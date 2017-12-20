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
                final int nextCycle = cycle + 1;

                final File file = queue.storeForCycle(nextCycle, queue.epoch(), true).file();
                if (!file.exists()) {
                    System.out.printf("Creating file %s%n", file.getAbsolutePath());
                    try(final RandomAccessFile rw = new RandomAccessFile(file, "rw")) {
                        rw.setLength(1024 * 1024 * 2);
                    } catch (IOException e) {
                        System.err.println("Failed to pre-create file: " + e.getMessage());
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