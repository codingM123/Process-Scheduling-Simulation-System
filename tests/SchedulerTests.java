import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Checks the simulator against worked examples from operating systems textbooks, then checks
 * invariants that must hold for any workload. Run with: make test
 */
public class SchedulerTests {

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        textbookFcfs();
        textbookSjf();
        textbookSrtf();
        textbookPriority();
        textbookRoundRobin();
        exponentialAveraging();
        contextSwitchCostsTime();
        ioOverlapsWithCpu();
        degreeOfMultiprogrammingLimitsAdmission();
        workloadIsNeverModified();
        generatorIsRepeatable();
        invariantsOnRandomWorkloads();

        System.out.println();
        System.out.println((checks - failures) + "/" + checks + " checks passed");
        if (failures > 0) {
            System.exit(1);
        }
    }

    // ---- worked textbook examples (context switch time 0, every process in memory) ----

    static void textbookFcfs() {
        List<Process> w = Arrays.asList(one(1, 0, 0, 24), one(2, 0, 0, 3), one(3, 0, 0, 3));
        SimulationResult r = new FirstComeFirstServed().run(w, 3, 0);
        check("FCFS: average waiting time is 17", near(r.averageWaitingTime(), 17));
        check("FCFS: average turnaround time is 27", near(r.averageTurnaroundTime(), 27));
        check("FCFS: order P1, P2, P3", order(r).equals("P1 P2 P3"));
    }

    static void textbookSjf() {
        List<Process> w = Arrays.asList(one(1, 0, 0, 6), one(2, 0, 0, 8), one(3, 0, 0, 7), one(4, 0, 0, 3));
        SimulationResult r = new ShortestJobScheduler().run(w, 4, 0);
        check("SJF: average waiting time is 7", near(r.averageWaitingTime(), 7));
        check("SJF: order P4, P1, P3, P2", order(r).equals("P4 P1 P3 P2"));
    }

    static void textbookSrtf() {
        List<Process> w = Arrays.asList(one(1, 0, 0, 8), one(2, 1, 0, 4), one(3, 2, 0, 9), one(4, 3, 0, 5));
        SimulationResult r = new PreemptiveShortestJob().run(w, 4, 0);
        check("SRTF: average waiting time is 6.5", near(r.averageWaitingTime(), 6.5));
        check("SRTF: P2 preempts P1 at time 1", order(r).equals("P1 P2 P4 P1 P3"));
        check("SRTF: completion times 17, 5, 26, 10", completions(r).equals("17 5 26 10"));
    }

    static void textbookPriority() {
        List<Process> w = Arrays.asList(one(1, 0, 3, 10), one(2, 0, 1, 1), one(3, 0, 4, 2), one(4, 0, 5, 1),
                one(5, 0, 2, 5));
        SimulationResult r = new PriorityScheduling().run(w, 5, 0);
        check("Priority: average waiting time is 8.2", near(r.averageWaitingTime(), 8.2));
        check("Priority: order P2, P5, P1, P3, P4", order(r).equals("P2 P5 P1 P3 P4"));
    }

    static void textbookRoundRobin() {
        List<Process> w = Arrays.asList(one(1, 0, 0, 24), one(2, 0, 0, 3), one(3, 0, 0, 3));
        SimulationResult r = new RoundRobinScheduler(4).run(w, 3, 0);
        check("Round Robin q=4: average waiting time is 17/3", near(r.averageWaitingTime(), 17.0 / 3));
        check("Round Robin q=4: order P1, P2, P3, P1", order(r).equals("P1 P2 P3 P1"));
        check("Round Robin q=4: 3 context switches", r.contextSwitches == 3);
    }

    /** The exponential-averaging example: alpha 1/2, tau0 10, bursts 6 4 6 4 13 13 13. */
    static void exponentialAveraging() {
        int[] bursts = { 6, 4, 6, 4, 13, 13, 13 };
        double[] expected = { 8, 6, 6, 5, 9, 11, 12 };
        double tau = 10;
        boolean ok = true;
        for (int i = 0; i < bursts.length; i++) {
            tau = ExponentialAveraging.predictNext(tau, bursts[i], 0.5);
            ok &= near(tau, expected[i]);
        }
        check("Exponential averaging: predictions 8 6 6 5 9 11 12", ok);

        // In a simulation the prediction is updated after every CPU burst: 10 -> 8 -> 6
        Burst[] b = { new Burst(true, 6), new Burst(false, 2), new Burst(true, 4) };
        SimulationResult r = new ExponentialAveraging(0.5).run(Arrays.asList(new Process(1, 0, 0, b, 10)), 1, 0);
        check("Exponential averaging: tau updated after each CPU burst", near(r.processes.get(0).tau, 6));
    }

    // ---- the parts textbooks usually leave out ----

    static void contextSwitchCostsTime() {
        List<Process> w = Arrays.asList(one(1, 0, 0, 2), one(2, 0, 0, 2));
        SimulationResult r = new FirstComeFirstServed().run(w, 2, 1);
        check("Context switch: one switch costing 1 unit", r.contextSwitches == 1 && r.totalTime == 5);
        check("Context switch: P2 waits 2 + 1 units", r.processes.get(1).waitingTime == 3);
        check("Context switch: CPU busy 80% of the time", near(r.cpuUtilization(), 80));
        check("Context switch: shown in the Gantt chart", r.ganttChart().contains("| CS |"));
    }

    static void ioOverlapsWithCpu() {
        Burst[] withIo = { new Burst(true, 2), new Burst(false, 3), new Burst(true, 1) };
        List<Process> w = Arrays.asList(new Process(1, 0, 0, withIo, 5), one(2, 0, 0, 4));
        SimulationResult r = new FirstComeFirstServed().run(w, 2, 0);
        check("I/O: P2 runs while P1 does I/O", order(r).equals("P1 P2 P1"));
        check("I/O: P1 finishes at 7, P2 at 6", completions(r).equals("7 6"));
        check("I/O: CPU never idle", near(r.cpuUtilization(), 100));
    }

    static void degreeOfMultiprogrammingLimitsAdmission() {
        List<Process> w = Arrays.asList(one(1, 0, 0, 3), one(2, 0, 0, 3));
        SimulationResult r = new FirstComeFirstServed().run(w, 1, 0);
        Process p2 = r.processes.get(1);
        check("Multiprogramming 1: P2 admitted only when P1 finishes", p2.admittedTime == 3);
        check("Multiprogramming 1: P2 waited 3 units in the job queue", p2.waitingTime == 3);
        check("Multiprogramming 1: P2 finishes at 6", p2.completionTime == 6);
    }

    static void workloadIsNeverModified() {
        WorkloadGenerator g = new WorkloadGenerator();
        List<Process> w = g.generate();
        String before = describe(w);
        for (Scheduler s : allSchedulers(4, 0.5)) {
            s.run(w, 5, 1);
        }
        check("Workload: unchanged after all six algorithms ran", describe(w).equals(before));
    }

    static void generatorIsRepeatable() {
        WorkloadGenerator a = new WorkloadGenerator();
        WorkloadGenerator b = new WorkloadGenerator();
        check("Generator: same seed gives the same workload", describe(a.generate()).equals(describe(b.generate())));
        b.seed = 7;
        check("Generator: another seed gives another workload", !describe(a.generate()).equals(describe(b.generate())));

        boolean shapeOk = true;
        for (WorkloadGenerator.Distribution d : WorkloadGenerator.Distribution.values()) {
            a.distribution = d;
            for (Process p : a.generate()) {
                for (int i = 0; i < p.bursts.length; i++) {
                    Burst burst = p.bursts[i];
                    boolean shouldBeCpu = i % 2 == 0;
                    boolean inRange = burst.isCpu
                            ? burst.duration >= a.minCpu && burst.duration <= a.maxCpu
                            : burst.duration >= a.minIo && burst.duration <= a.maxIo;
                    shapeOk &= burst.isCpu == shouldBeCpu && inRange;
                }
                shapeOk &= p.bursts.length % 2 == 1; // starts and ends with CPU
                shapeOk &= p.arrivalTime >= a.minArrival && p.arrivalTime <= a.maxArrival;
            }
        }
        check("Generator: CPU and I/O alternate, all lengths in range", shapeOk);
    }

    /** 300 random workloads x 6 algorithms; every run must satisfy these rules. */
    static void invariantsOnRandomWorkloads() {
        Random rng = new Random(2024);
        String[] rules = {
            "every process finishes",
            "no process runs before it arrives",
            "turnaround = CPU time + I/O time + waiting time",
            "CPU time line covers every time unit",
            "busy time = total CPU demand",
            "context-switch time = switches x switch cost",
        };
        String[] firstFailure = new String[rules.length];
        int runs = 0;
        for (int round = 0; round < 300; round++) {
            WorkloadGenerator g = new WorkloadGenerator();
            g.seed = rng.nextLong();
            g.processes = 1 + rng.nextInt(15);
            g.averageCpuBursts = 1 + rng.nextInt(4);
            g.maxArrival = rng.nextInt(30);
            g.distribution = rng.nextBoolean() ? WorkloadGenerator.Distribution.GAUSSIAN
                    : WorkloadGenerator.Distribution.BINOMIAL;
            List<Process> w = g.generate();
            int degree = 1 + rng.nextInt(8);
            int cs = rng.nextInt(3);
            for (Scheduler s : allSchedulers(1 + rng.nextInt(6), rng.nextDouble())) {
                SimulationResult r = s.run(w, degree, cs);
                runs++;
                int demand = 0;
                for (Process p : r.processes) {
                    demand += p.totalCpuTime();
                    if (p.completionTime < 0) note(firstFailure, 0, s, p);
                    if (p.firstRunTime < p.arrivalTime) note(firstFailure, 1, s, p);
                    if (p.turnaroundTime() != p.totalCpuTime() + p.totalIoTime() + p.waitingTime) note(firstFailure, 2, s, p);
                }
                if (r.timeline.size() != r.totalTime) note(firstFailure, 3, s, null);
                if (r.busyTime != demand) note(firstFailure, 4, s, null);
                int switchUnits = 0;
                for (String unit : r.timeline) {
                    if (unit.equals("CS")) switchUnits++;
                }
                if (switchUnits != r.contextSwitches * cs) note(firstFailure, 5, s, null);
            }
        }
        for (int i = 0; i < rules.length; i++) {
            check("Random workloads (" + runs + " runs): " + rules[i]
                    + (firstFailure[i] == null ? "" : " - broken by " + firstFailure[i]), firstFailure[i] == null);
        }
    }

    // ---- helpers ----

    static List<Scheduler> allSchedulers(int quantum, double alpha) {
        return Arrays.asList(new FirstComeFirstServed(), new ShortestJobScheduler(), new PreemptiveShortestJob(),
                new PriorityScheduling(), new RoundRobinScheduler(quantum), new ExponentialAveraging(alpha));
    }

    static Process one(int pid, int arrival, int priority, int burst) {
        return Process.withOneBurst(pid, arrival, priority, burst, 5);
    }

    /** Processes in the order they used the CPU, e.g. "P1 P2 P1". */
    static String order(SimulationResult r) {
        List<String> runs = new ArrayList<>();
        for (String unit : r.timeline) {
            if (unit.startsWith("P") && (runs.isEmpty() || !runs.get(runs.size() - 1).equals(unit))) {
                runs.add(unit);
            }
        }
        return String.join(" ", runs);
    }

    static String completions(SimulationResult r) {
        List<String> times = new ArrayList<>();
        for (Process p : r.processes) {
            times.add(String.valueOf(p.completionTime));
        }
        return String.join(" ", times);
    }

    static String describe(List<Process> w) {
        StringBuilder sb = new StringBuilder();
        for (Process p : w) {
            sb.append(p.name()).append(' ').append(p.arrivalTime).append(' ').append(p.priority).append(' ')
                    .append(p.burstIndex).append(' ').append(p.completionTime).append(' ');
            for (Burst b : p.bursts) {
                sb.append(b).append('/').append(b.remaining).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    static void note(String[] firstFailure, int rule, Scheduler s, Process p) {
        if (firstFailure[rule] == null) {
            firstFailure[rule] = s.name() + (p == null ? "" : " on " + p.name());
        }
    }

    static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    static void check(String name, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "[pass] " : "[FAIL] ") + name);
    }
}
