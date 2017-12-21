package net.openhft.load;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.load.config.ConfigParser;
import net.openhft.load.config.StageConfig;
import net.openhft.load.pretoucher.FileCreatorMain;
import net.openhft.load.pretoucher.PretoucherMain;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StageMain {

    private static final int UNSET_SOURCE = -1;

    public static void main(String[] args) throws IOException {
//        MlockAll.doMlockall();
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: <program> [resource-name] [stage-index]");
        }
        final ConfigParser configParser = new ConfigParser(args[0]);

        final StageConfig stageConfig = configParser.getStageConfig(Integer.parseInt(args[1]));
        final ExecutorService service = Executors.newCachedThreadPool();
        startPretoucher(configParser, stageConfig, service);
        final List<Integer> cpuList = new ArrayList<>(stageConfig.getCpus());
        for (Integer index : stageConfig.getStageIndices()) {
            service.submit(() -> {
                final Stage stage = new Stage(createOutput(stageConfig.getOutputPath(), index + 1), index);
                final MethodReader reader = createReader(stageConfig.getInputPath(), stage);
                Thread.currentThread().setName("load.stage-consumer-" + index);
                AffinityHelper.setAffinity(cpuList.remove(0));
                boolean warnOnce = false;
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        reader.readOne();
                    }
                    catch (Exception e)
                    {
                        if (!warnOnce)
                        {
                            e.printStackTrace();
                            warnOnce = true;
                        }
                    }
                }
            });
        }
    }

    private static void startPretoucher(final ConfigParser configParser, final StageConfig stageConfig, final ExecutorService service) {
        if (configParser.isPretouchOutOfBand()) {
            PretoucherMain.startOutOfBandPretoucher(stageConfig.getOutputPath(), service);
            FileCreatorMain.startOutOfBandFileCreator(stageConfig.getOutputPath(), service);
        } else {
            service.submit(new PretoucherTask(outputQueue(stageConfig.getOutputPath(), UNSET_SOURCE),
                    configParser.getPretouchIntervalMillis()));
        }
    }

    private static MethodReader createReader(final Path path, final MethodDefinition impl) {
        final SingleChronicleQueue queue = outputQueue(path, UNSET_SOURCE);
        return queue.createTailer().methodReader(impl);
    }

    private static MethodDefinition createOutput(final Path path, final Integer index) {
        final SingleChronicleQueue queue = outputQueue(path, index);
        return new GarbageFreeMethodPublisher(queue::acquireAppender);
    }

    @NotNull
    private static SingleChronicleQueue outputQueue(final Path path, final int index) {
        final SingleChronicleQueueBuilder builder = QueueFactory.builderFor(path);
        if (index != UNSET_SOURCE) {
            builder.sourceId(index);
        }
        return builder.build();
    }
}