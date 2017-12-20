package net.openhft.load;

import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

import java.nio.file.Path;
import java.time.ZoneId;

public final class QueueFactory {
    private QueueFactory() {}

    public static SingleChronicleQueueBuilder builderFor(final Path path) {
        final SingleChronicleQueueBuilder builder = SingleChronicleQueueBuilder.binary(path);
        builder.rollTime(RollTimeCalculator.getNextRollWindow(), ZoneId.of("UTC"));
        builder.rollCycle(RollCycles.HOURLY);
        return builder;
    }
}
