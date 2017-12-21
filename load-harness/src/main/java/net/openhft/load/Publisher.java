package net.openhft.load;

import net.openhft.load.config.PublisherConfig;
import net.openhft.load.config.StageConfig;
import net.openhft.load.messages.EightyByteMessage;
import net.openhft.load.messages.Sizer;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public final class Publisher {
    private final PublisherConfig config;
    private final EightyByteMessage message = new EightyByteMessage();
    private final MethodDefinition methodDefinition;
    private final long[] stagePublishBitmasks = new long[16];
    private final boolean bursty;
    private int messagesPerSec;
    private boolean warnOnce;
    private long publishMaskCount = 0;

    public Publisher(final PublisherConfig config, final MethodDefinition methodDefinition,
                     final List<StageConfig> stageConfigs) throws Exception {
        this.config = config;
        this.methodDefinition = methodDefinition;
        final Random random = new Random(1511514053000L);
        for (int i = 0; i < stagePublishBitmasks.length; i++) {
            for (StageConfig stageConfig : stageConfigs) {
                final List<Integer> stageIndices = stageConfig.getStageIndices();
                final int index = stageIndices.get(random.nextInt(stageIndices.size()));
                stagePublishBitmasks[i] |= 1 << index;
            }
        }
        this.bursty = config.isBursty();
    }

    void init() {
        final int bytesPerSec = config.getPublishRateMegaBytesPerSecond() * 1024 * 1024;
        messagesPerSec = bytesPerSec / Sizer.size(message);
    }

    public void startPublishing() {
        Thread.currentThread().setName("load.publisher");
        AffinityHelper.setAffinity(config.getCpu());
        final long startPublishingAt = System.currentTimeMillis();
        long nanoInterval = ((long) (TimeUnit.SECONDS.toNanos(1L) * 0.8) / messagesPerSec) - 30;

        boolean loggedException = false;
        while (!Thread.currentThread().isInterrupted()) {
            final long startNanos = System.nanoTime();
            message.batchStartNanos = startNanos;
            message.batchStartMillis = System.currentTimeMillis();
            if (bursty) {
                try {
                    for (int i = 0; i < messagesPerSec; i++) {
                        message.stagesToPublishBitMask = stagePublishBitmasks[(int) (publishMaskCount & 15)];
                        message.publishNanos = System.nanoTime();
                        publishMaskCount++;
                        methodDefinition.onEightyByteMessage(message);

                    }

                } catch (Exception e) {
                    if (!loggedException) {
                        e.printStackTrace();
                        loggedException = true;
                    }
                }
            } else {
                try {
                    for (int i = 0; i < messagesPerSec; i++) {
                        message.stagesToPublishBitMask = stagePublishBitmasks[(int) (publishMaskCount & 15)];
                        message.publishNanos = System.nanoTime();
                        publishMaskCount++;
                        methodDefinition.onEightyByteMessage(message);
                        while ((System.nanoTime()) < message.publishNanos + nanoInterval) {
                            // spin
                        }
                    }

                } catch (Exception e) {
                    if (!loggedException) {
                        e.printStackTrace();
                        loggedException = true;
                    }
                }
            }

            final long endOfSecond = startNanos + TimeUnit.SECONDS.toNanos(1L);
            boolean slept = false;
            while (System.nanoTime() < endOfSecond) {
                // spin
                slept = true;
            }
            if (!slept && bursty) {
                nanoInterval *= 0.9;
            }

            if (!warnOnce && !slept && System.currentTimeMillis() > startPublishingAt + 30_000L) {
                System.err.println("Unable to publish at requested rate");
                warnOnce = true;
            }
        }
    }

    private static long nanosToMicros(final long nanos) {
        return TimeUnit.NANOSECONDS.toMicros(nanos);
    }
}