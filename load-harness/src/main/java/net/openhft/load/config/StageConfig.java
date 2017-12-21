package net.openhft.load.config;

import java.nio.file.Path;
import java.util.List;

public final class StageConfig {
    private final Path inputPath;
    private final Path outputPath;
    private final List<Integer> stageIndices;
    private final List<Integer> cpus;

    public StageConfig(final Path inputPath, final Path outputPath,
                       final List<Integer> stageIndices, final List<Integer> cpus) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.stageIndices = stageIndices;
        this.cpus = cpus;
    }

    public Path getInputPath() {
        return inputPath;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public List<Integer> getStageIndices() {
        return stageIndices;
    }

    public List<Integer> getCpus() {
        return cpus;
    }
}