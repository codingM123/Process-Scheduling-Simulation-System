/**
 * A simulated process: an arrival time, a priority and a list of CPU and I/O bursts.
 * It also holds the bookkeeping that a scheduler fills in while the process runs.
 */
public class Process {

    final int pid;
    final int arrivalTime;
    final int priority;          // lower number = higher priority
    final Burst[] bursts;

    int burstIndex;              // which burst the process is on
    double tau;                  // predicted length of the next CPU burst (exponential averaging)

    // Filled in during a simulation
    long readySeq;               // when it last entered the ready queue (keeps FIFO order)
    int admittedTime = -1;       // when it left the job queue
    int firstRunTime = -1;       // first time it got the CPU
    int completionTime = -1;
    int waitingTime;             // time in the system spent neither running nor doing I/O

    public Process(int pid, int arrivalTime, int priority, Burst[] bursts, double initialTau) {
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("arrival time cannot be negative");
        }
        if (bursts == null || bursts.length == 0) {
            throw new IllegalArgumentException("a process needs at least one burst");
        }
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.priority = priority;
        this.bursts = bursts;
        this.tau = initialTau;
    }

    /** A process with a single CPU burst, as in most textbook examples. */
    static Process withOneBurst(int pid, int arrivalTime, int priority, int cpuBurst, double initialTau) {
        return new Process(pid, arrivalTime, priority, new Burst[] { new Burst(true, cpuBurst) }, initialTau);
    }

    Burst currentBurst() {
        return bursts[burstIndex];
    }

    void nextBurst() {
        burstIndex++;
    }

    boolean isFinished() {
        return burstIndex >= bursts.length;
    }

    int totalCpuTime() {
        int sum = 0;
        for (Burst b : bursts) {
            if (b.isCpu) {
                sum += b.duration;
            }
        }
        return sum;
    }

    int totalIoTime() {
        int sum = 0;
        for (Burst b : bursts) {
            if (!b.isCpu) {
                sum += b.duration;
            }
        }
        return sum;
    }

    int turnaroundTime() {
        return completionTime - arrivalTime;
    }

    int responseTime() {
        return firstRunTime - arrivalTime;
    }

    String name() {
        return "P" + pid;
    }

    String burstList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bursts.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(bursts[i]);
        }
        return sb.toString();
    }

    /** Deep copy, so every algorithm runs on the same untouched workload. */
    Process copy() {
        Burst[] copied = new Burst[bursts.length];
        for (int i = 0; i < bursts.length; i++) {
            copied[i] = bursts[i].copy();
        }
        Process p = new Process(pid, arrivalTime, priority, copied, tau);
        p.burstIndex = burstIndex;
        return p;
    }
}
