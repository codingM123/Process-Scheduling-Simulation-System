import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Process Scheduling Simulator.
 *
 * Generates a workload of processes with CPU and I/O bursts, runs six CPU scheduling algorithms on
 * the same workload, and compares their waiting, turnaround and response times, CPU utilization,
 * throughput and number of context switches.
 */
public class Main {

    static final String USAGE = String.join(System.lineSeparator(),
            "Usage: java -cp out Main [options]",
            "",
            "  --example            run the classic 4-process textbook example, with Gantt charts",
            "  --interactive        type in the workload settings instead of using the defaults",
            "  --processes N        number of processes (default 20)",
            "  --cpu-bursts N       average number of CPU bursts per process (default 3)",
            "  --seed N             random seed; the same seed gives the same workload (default 42)",
            "  --quantum N          Round Robin time quantum (default 4)",
            "  --context-switch N   time units per context switch (default 1, or 0 with --example)",
            "  --mp N               degree of multiprogramming: most processes in memory at once (default: all)",
            "  --alpha X            exponential averaging weight, 0 to 1 (default 0.5)",
            "  --tau X              initial CPU burst prediction (default 6)",
            "  --binomial           draw each process's number of CPU bursts from a binomial distribution (default Gaussian)",
            "  --gantt              print a Gantt chart for each algorithm",
            "  --trace              print every scheduling event",
            "  --help               show this help");

    /** Settings from the command line. Null means "use the default". */
    static final class Options {
        final WorkloadGenerator generator = new WorkloadGenerator();
        int quantum = 4;
        double alpha = 0.5;
        Integer contextSwitch;
        Integer degreeOfMultiprogramming;
        boolean example;
        boolean interactive;
        boolean gantt;
        boolean trace;
        boolean help;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Locale.setDefault(Locale.US); // decimal points, not commas, in the tables
        Options o;
        try {
            o = parse(args);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage(), true);
            return;
        }
        if (o.help) {
            System.out.println(USAGE);
            return;
        }
        try {
            if (o.interactive) {
                askUser(o, new Scanner(System.in));
            }
            run(o);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage(), false);
        }
    }

    static void run(Options o) throws InterruptedException, ExecutionException {
        List<Process> workload = o.example ? classicExample(o.generator.initialTau) : o.generator.generate();
        int degree = o.degreeOfMultiprogramming != null ? o.degreeOfMultiprogramming : workload.size();
        int contextSwitch = o.contextSwitch != null ? o.contextSwitch : (o.example ? 0 : 1);
        if (degree < 1) {
            throw new IllegalArgumentException("degree of multiprogramming must be at least 1");
        }
        if (contextSwitch < 0) {
            throw new IllegalArgumentException("context switch time cannot be negative");
        }

        List<Scheduler> schedulers = Arrays.asList(
                new FirstComeFirstServed(),
                new ShortestJobScheduler(),
                new PreemptiveShortestJob(),
                new PriorityScheduling(),
                new RoundRobinScheduler(o.quantum),
                new ExponentialAveraging(o.alpha));

        System.out.println("Process Scheduling Simulator");
        System.out.println("Workload : " + (o.example
                ? "classic textbook example (4 processes, one CPU burst each)"
                : o.generator.describe()));
        System.out.printf("Settings : degree of multiprogramming %d, context switch %d, quantum %d, alpha %.2f, initial tau %.1f%n",
                degree, contextSwitch, o.quantum, o.alpha, o.generator.initialTau);
        System.out.println();
        printWorkload(workload);

        List<SimulationResult> results = runAll(schedulers, workload, degree, contextSwitch);
        printComparison(results);
        if (o.gantt || o.example) {
            printGanttCharts(results);
        }
        if (o.trace) {
            printEvents(results);
        }
    }

    /**
     * Runs every algorithm on its own thread. Each one copies the workload before changing anything,
     * so the threads share no mutable data and need no locks.
     */
    static List<SimulationResult> runAll(List<Scheduler> schedulers, List<Process> workload, int degree,
            int contextSwitch) throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(schedulers.size());
        try {
            List<Future<SimulationResult>> futures = new ArrayList<>();
            for (Scheduler s : schedulers) {
                futures.add(pool.submit(() -> s.run(workload, degree, contextSwitch)));
            }
            List<SimulationResult> results = new ArrayList<>();
            for (Future<SimulationResult> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdown();
        }
    }

    /** The 4-process example used in most OS textbooks to compare SJF and SRTF. */
    static List<Process> classicExample(double initialTau) {
        return Arrays.asList(
                Process.withOneBurst(1, 0, 3, 8, initialTau),
                Process.withOneBurst(2, 1, 1, 4, initialTau),
                Process.withOneBurst(3, 2, 4, 9, initialTau),
                Process.withOneBurst(4, 3, 2, 5, initialTau));
    }

    static void printWorkload(List<Process> workload) {
        System.out.println("PID   Arrival  Priority  Bursts");
        for (Process p : workload) {
            System.out.printf("%-5s %7d  %8d  %s%n", p.name(), p.arrivalTime, p.priority, p.burstList());
        }
        System.out.println();
    }

    static void printComparison(List<SimulationResult> results) {
        System.out.printf("%-22s %9s %15s %13s %9s %11s %17s%n", "Algorithm", "Avg wait", "Avg turnaround",
                "Avg response", "CPU busy", "Throughput", "Context switches");
        for (SimulationResult r : results) {
            System.out.printf("%-22s %9.2f %15.2f %13.2f %8.1f%% %11.2f %17d%n", r.algorithm,
                    r.averageWaitingTime(), r.averageTurnaroundTime(), r.averageResponseTime(),
                    r.cpuUtilization(), r.throughput(), r.contextSwitches);
        }
        System.out.println("Throughput = processes finished per 100 time units.");
        System.out.println();

        SimulationResult fcfs = results.get(0);
        SimulationResult bestWait = results.get(0);
        SimulationResult bestResponse = results.get(0);
        for (SimulationResult r : results) {
            if (r.averageWaitingTime() < bestWait.averageWaitingTime()) {
                bestWait = r;
            }
            if (r.averageResponseTime() < bestResponse.averageResponseTime()) {
                bestResponse = r;
            }
        }
        if (bestWait != fcfs && fcfs.averageWaitingTime() > 0) {
            double saving = 100 * (1 - bestWait.averageWaitingTime() / fcfs.averageWaitingTime());
            System.out.printf("Lowest average waiting time : %s, %.0f%% lower than FCFS%n", bestWait.algorithm, saving);
        } else {
            System.out.printf("Lowest average waiting time : %s%n", bestWait.algorithm);
        }
        System.out.printf("Fastest average response    : %s%n", bestResponse.algorithm);
        System.out.println();
    }

    static void printGanttCharts(List<SimulationResult> results) {
        System.out.println("Gantt charts (CS = context switch)");
        for (SimulationResult r : results) {
            System.out.println();
            System.out.println(r.algorithm);
            System.out.println(r.ganttChart());
        }
        System.out.println();
    }

    static void printEvents(List<SimulationResult> results) {
        for (SimulationResult r : results) {
            System.out.println("Events: " + r.algorithm);
            for (String e : r.events) {
                System.out.println("  " + e);
            }
            System.out.println();
        }
    }

    static Options parse(String[] args) {
        Options o = new Options();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--example":        o.example = true; break;
                case "--interactive":    o.interactive = true; break;
                case "--gantt":          o.gantt = true; break;
                case "--trace":          o.trace = true; break;
                case "--binomial":       o.generator.distribution = WorkloadGenerator.Distribution.BINOMIAL; break;
                case "--help":
                case "-h":               o.help = true; break;
                case "--processes":      o.generator.processes = intValue(args, ++i, a); break;
                case "--cpu-bursts":     o.generator.averageCpuBursts = intValue(args, ++i, a); break;
                case "--seed":           o.generator.seed = intValue(args, ++i, a); break;
                case "--quantum":        o.quantum = intValue(args, ++i, a); break;
                case "--context-switch": o.contextSwitch = intValue(args, ++i, a); break;
                case "--mp":             o.degreeOfMultiprogramming = intValue(args, ++i, a); break;
                case "--alpha":          o.alpha = doubleValue(args, ++i, a); break;
                case "--tau":            o.generator.initialTau = doubleValue(args, ++i, a); break;
                default:
                    throw new IllegalArgumentException("unknown option " + a);
            }
        }
        return o;
    }

    private static int intValue(String[] args, int i, String option) {
        try {
            return Integer.parseInt(args[i]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException(option + " needs a whole number");
        }
    }

    private static double doubleValue(String[] args, int i, String option) {
        try {
            return Double.parseDouble(args[i]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException(option + " needs a number");
        }
    }

    /** Asks for every workload setting, like the first version of this program did. */
    static void askUser(Options o, Scanner in) {
        WorkloadGenerator g = o.generator;
        try {
            g.processes = askInt(in, "Number of processes");
            g.averageCpuBursts = askInt(in, "Average CPU bursts per process");
            g.minCpu = askInt(in, "CPU burst length, min");
            g.maxCpu = askInt(in, "CPU burst length, max");
            g.minIo = askInt(in, "I/O burst length, min");
            g.maxIo = askInt(in, "I/O burst length, max");
            g.minPriority = askInt(in, "Priority, min (lower = more important)");
            g.maxPriority = askInt(in, "Priority, max");
            g.minArrival = askInt(in, "Arrival time, min");
            g.maxArrival = askInt(in, "Arrival time, max");
            o.degreeOfMultiprogramming = askInt(in, "Degree of multiprogramming");
            o.contextSwitch = askInt(in, "Context switch time");
            o.quantum = askInt(in, "Round Robin time quantum");
            g.initialTau = askDouble(in, "Initial tau (CPU burst prediction)");
            o.alpha = askDouble(in, "Alpha for exponential averaging (0 to 1)");
            g.distribution = askInt(in, "Number of CPU bursts: 0 = Gaussian, 1 = binomial") == 1
                    ? WorkloadGenerator.Distribution.BINOMIAL
                    : WorkloadGenerator.Distribution.GAUSSIAN;
            g.seed = askInt(in, "Random seed");
            System.out.println();
        } catch (InputMismatchException e) {
            throw new IllegalArgumentException("please type a number");
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("input ended before all settings were entered");
        }
    }

    private static int askInt(Scanner in, String prompt) {
        System.out.print(prompt + ": ");
        return in.nextInt();
    }

    private static double askDouble(Scanner in, String prompt) {
        System.out.print(prompt + ": ");
        return in.nextDouble();
    }

    private static void fail(String message, boolean showUsage) {
        System.err.println("error: " + message);
        if (showUsage) {
            System.err.println();
            System.err.println(USAGE);
        }
        System.exit(2);
    }
}
