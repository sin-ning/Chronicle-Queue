package net.openhft.load.config;

import java.nio.file.Path;

public final class PublisherConfig {
    private final Path outputDir;
    private final int publishRateMegaBytesPerSecond;
    private final int stageCount;
    private final int cpu;
    private final boolean bursty;

    public PublisherConfig(final Path outputDir, final int publishRateMegaBytesPerSecond,
                           final int stageCount, final int cpu, final boolean bursty) {
        this.outputDir = outputDir;
        this.publishRateMegaBytesPerSecond = publishRateMegaBytesPerSecond;
        this.stageCount = stageCount;
        this.cpu = cpu;
        this.bursty = bursty;
    }

    public Path outputDir() {
        return outputDir;
    }

    public int getPublishRateMegaBytesPerSecond() {
        return publishRateMegaBytesPerSecond;
    }

    public int getCpu() {
        return cpu;
    }

    public boolean isBursty() {
        return bursty;
    }
}
