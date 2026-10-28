import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a random workload. The same seed always gives the same processes, so every run can be repeated.
 *
 * Each process gets an arrival time, a priority and a list of bursts that alternate between CPU and I/O,
 * starting and ending with a CPU burst: CPU, I/O, CPU, I/O, CPU. The number of CPU bursts is drawn from
 * a Gaussian or a binomial distribution around the chosen average.
 */
public class WorkloadGenerator {

    public enum Distribution { GAUSSIAN, BINOMIAL }

    int processes = 20;
    int averageCpuBursts = 3;
    int minCpu = 2;
    int maxCpu = 12;
    int minIo = 5;
    int maxIo = 20;
    int minPriority = 1;
    int maxPriority = 5;
    int minArrival = 0;
    int maxArrival = 40;
    double initialTau = 6;
    Distribution distribution = Distribution.GAUSSIAN;
    long seed = 42;

    public List<Process> generate() {
        validate();
        Random rng = new Random(seed);
        List<Process> list = new ArrayList<>();
        for (int pid = 1; pid <= processes; pid++) {
            int arrival = between(rng, minArrival, maxArrival);
            int priority = between(rng, minPriority, maxPriority);
            list.add(new Process(pid, arrival, priority, makeBursts(rng), initialTau));
        }
        return list;
    }

    /** CPU, I/O, CPU, ... , CPU for one process. */
    private Burst[] makeBursts(Random rng) {
        int cpuBursts = cpuBurstCount(rng);
        Burst[] bursts = new Burst[2 * cpuBursts - 1];
        for (int i = 0; i < bursts.length; i++) {
            boolean cpu = i % 2 == 0;
            bursts[i] = cpu
                    ? new Burst(true, between(rng, minCpu, maxCpu))
                    : new Burst(false, between(rng, minIo, maxIo));
        }
        return bursts;
    }

    /** Number of CPU bursts for one process: around the average, between 1 and twice the average. */
    private int cpuBurstCount(Random rng) {
        int max = 2 * averageCpuBursts;
        int count;
        if (distribution == Distribution.GAUSSIAN) {
            double spread = Math.max(1.0, averageCpuBursts / 2.0);
            count = (int) Math.round(averageCpuBursts + rng.nextGaussian() * spread);
        } else {
            count = 0; // binomial: max trials with probability 1/2 each, so the mean is the average
            for (int i = 0; i < max; i++) {
                if (rng.nextBoolean()) {
                    count++;
                }
            }
        }
        return Math.max(1, Math.min(max, count));
    }

    private static int between(Random rng, int min, int max) {
        return min + rng.nextInt(max - min + 1);
    }

    private void validate() {
        require(processes >= 1, "number of processes must be at least 1");
        require(averageCpuBursts >= 1, "average CPU bursts per process must be at least 1");
        require(minCpu >= 1 && minCpu <= maxCpu, "CPU burst range must start at 1 or more, with min <= max");
        require(minIo >= 1 && minIo <= maxIo, "I/O burst range must start at 1 or more, with min <= max");
        require(minPriority <= maxPriority, "priority range needs min <= max");
        require(minArrival >= 0 && minArrival <= maxArrival, "arrival range must start at 0 or later, with min <= max");
        require(initialTau > 0, "initial tau must be positive");
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new IllegalArgumentException(message);
        }
    }

    String describe() {
        return processes + " processes (seed " + seed + "), about " + averageCpuBursts + " CPU bursts each ("
                + distribution.name().toLowerCase() + "), CPU " + minCpu + "-" + maxCpu + ", I/O " + minIo + "-"
                + maxIo + ", arrivals " + minArrival + "-" + maxArrival + ", priorities " + minPriority + "-"
                + maxPriority;
    }
}
