package net.openhft.load.pretoucher;

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.load.QueueFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class PretoucherMain {
    public static void main(String[] args) {
        start(Paths.get(args[0]));
    }

    private static void start(final Path path) {
        try (final SingleChronicleQueue queue = QueueFactory.builderFor(path).build()) {
            System.out.printf("Starting pre-touch in %s%n", path);
            while (!Thread.currentThread().isInterrupted()) {
                queue.acquireAppender().pretouch();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
        }
    }

    public static void startOutOfBandPretoucher(final Path queueDir, final ExecutorService executorService) {
        final String javaHome = System.getenv("JAVA_HOME");
        final Path javaBin = Paths.get(javaHome, "bin", "java");
        final String classpath = System.getProperty("java.class.path");
        final String mainClass = PretoucherMain.class.getName();

        final ProcessBuilder processBuilder = new ProcessBuilder(javaBin.toString(), "-cp", classpath, mainClass, queueDir.toString()).inheritIO();
        executorService.submit(() -> {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5L));
            try {
                final Process process = processBuilder.start();
                process.waitFor();
            } catch (Exception e) {
                System.err.println("Error launching pretoucher");
                e.printStackTrace();
            }
        });
    }
}