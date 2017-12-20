package net.openhft.load.pretoucher;

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.load.QueueFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class FileCreatorMain {
    public static void main(String[] args) {
        final Path queuePath = Paths.get(args[0]);
        start(queuePath);
    }

    private static void start(final Path queuePath) {
        try (final SingleChronicleQueue queue = QueueFactory.builderFor(queuePath).build()) {
            while (!Thread.currentThread().isInterrupted()) {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));

                final int cycle = queue.cycle();

                final File file = queue.storeForCycle(cycle, queue.epoch(), false).file();
                if (file != null) {
                    final String[] tokens = file.getName().substring(0, file.getName().indexOf('.')).split("-");
                    final File nextCycleFile = new File(file.getParent(), tokens[0] + "-" + (Integer.parseInt(tokens[1]) + 1) + ".cq4");
                    if (!nextCycleFile.exists()) {
                        System.out.printf("Creating file %s%n", nextCycleFile.getAbsolutePath());
                        try (final RandomAccessFile rw = new RandomAccessFile(nextCycleFile, "rw")) {
                            rw.setLength(1024 * 1024 * 2);
                        } catch (IOException e) {
                            System.err.println("Failed to pre-create file: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public static void startOutOfBandFileCreator(final Path queueDir, final ExecutorService executorService) {
        final String javaHome = System.getenv("JAVA_HOME");
        final Path javaBin = Paths.get(javaHome, "bin", "java");
        final String classpath = System.getProperty("java.class.path");
        final String mainClass = FileCreatorMain.class.getName();

        final ProcessBuilder processBuilder = new ProcessBuilder(javaBin.toString(), "-cp", classpath, mainClass, queueDir.toString()).inheritIO();
        executorService.submit(() -> {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5L));
            try {
                final Process process = processBuilder.start();
                process.waitFor();
            } catch (Exception e) {
                System.err.println("Error launching file creator");
                e.printStackTrace();
            }
        });
    }

}