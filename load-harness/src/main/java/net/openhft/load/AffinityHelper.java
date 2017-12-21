package net.openhft.load;

import net.openhft.affinity.Affinity;

public final class AffinityHelper {
    public static void setAffinity(final int cpu) {
        if (cpu < 0) {
            return;
        }
        System.out.printf("Setting affinity for %s to %d%n", Thread.currentThread().getName(), cpu);
        Affinity.setAffinity(cpu);
    }
}