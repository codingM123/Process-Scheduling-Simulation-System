import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The simulation that every algorithm shares. Time moves in whole units, and each unit has five steps:
 *
 *   1. admit     arrived jobs move from the job queue to the ready queue, as long as fewer than
 *                "degree of multiprogramming" processes are in the system
 *   2. preempt   a preemptive algorithm, or an expired Round Robin quantum, takes the CPU back
 *   3. dispatch  if the CPU is free, the head of the ready queue gets it; moving the CPU from one
 *                process to another costs the context-switch time
 *   4. run       the CPU does one unit of work, and every process in the device queue does one unit of I/O
 *   5. move on   a finished CPU burst sends the process to I/O, back to the ready queue or out of the
 *                system; a finished I/O burst sends it back to the ready queue
 *
 * A subclass only decides the order of the ready queue and when to preempt.
 */
public abstract class Scheduler {

    private static final int TIME_LIMIT = 10_000_000; // guards against a simulation that never ends

    private final String name;

    protected Scheduler(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** Order of the ready queue: the head is the next process to run. */
    protected abstract Comparator<Process> readyOrder();

    /** Preemptive algorithms return true when the candidate should take the CPU from the running process. */
    protected boolean preempts(Process candidate, Process running) {
        return false;
    }

    /** Round Robin returns its time quantum; 0 means no time slicing. */
    protected int timeQuantum() {
        return 0;
    }

    /** Called when a CPU burst finishes, with the burst's real length. */
    protected void cpuBurstFinished(Process p, int actualLength) {
    }

    /**
     * Runs the whole workload. The workload itself is never changed: every process is copied first,
     * so all algorithms can run on the same workload, even at the same time.
     */
    public SimulationResult run(List<Process> workload, int degreeOfMultiprogramming, int contextSwitchTime) {
        if (degreeOfMultiprogramming < 1) {
            throw new IllegalArgumentException("degree of multiprogramming must be at least 1");
        }
        if (contextSwitchTime < 0) {
            throw new IllegalArgumentException("context switch time cannot be negative");
        }
        Run run = new Run(workload, degreeOfMultiprogramming, contextSwitchTime);
        while (!run.allFinished()) {
            run.admit();
            run.preempt();
            run.dispatch();
            run.runOneUnit();
            run.moveOn();
            if (run.time > TIME_LIMIT) {
                throw new IllegalStateException(name + ": simulation did not finish");
            }
        }
        run.result.totalTime = run.time;
        return run.result;
    }

    /** The state of one simulation. */
    private final class Run {

        final PriorityQueue<Process> jobQueue = new PriorityQueue<>(new ProcessArrivalSorter());
        final PriorityQueue<Process> readyQueue = new PriorityQueue<>(readyOrder());
        final List<Process> deviceQueue = new ArrayList<>(); // unlimited I/O devices: all I/O runs in parallel
        final SimulationResult result;
        final int degree;
        final int switchTime;

        int time;
        int inSystem;          // admitted and not finished yet
        int finished;
        long nextSeq;          // stamps the order processes enter the ready queue
        Process running;
        Process switchingIn;   // process being loaded during a context switch
        int switchLeft;
        Process lastOnCpu;
        int quantumUsed;

        Run(List<Process> workload, int degree, int switchTime) {
            List<Process> copies = new ArrayList<>();
            for (Process p : workload) {
                Process copy = p.copy();
                copies.add(copy);
                jobQueue.add(copy);
            }
            this.result = new SimulationResult(name, copies);
            this.degree = degree;
            this.switchTime = switchTime;
        }

        boolean allFinished() {
            return finished == result.processes.size();
        }

        void admit() {
            while (!jobQueue.isEmpty() && jobQueue.peek().arrivalTime <= time && inSystem < degree) {
                Process p = jobQueue.poll();
                p.admittedTime = time;
                p.waitingTime += time - p.arrivalTime; // time spent in the job queue waiting to be admitted
                inSystem++;
                result.log(time, p.name() + " admitted");
                enter(p);
            }
        }

        void preempt() {
            if (running == null) {
                return;
            }
            boolean quantumOver = timeQuantum() > 0 && quantumUsed >= timeQuantum();
            if (readyQueue.isEmpty()) {
                if (quantumOver) {
                    quantumUsed = 0; // nobody else is waiting, so it starts a new slice
                }
                return;
            }
            Process candidate = readyQueue.peek();
            if (quantumOver) {
                result.log(time, running.name() + " used up its quantum");
            } else if (preempts(candidate, running)) {
                result.log(time, candidate.name() + " preempts " + running.name());
            } else {
                return;
            }
            toReadyQueue(running);
            running = null;
        }

        void dispatch() {
            if (running != null || switchingIn != null || readyQueue.isEmpty()) {
                return;
            }
            Process next = readyQueue.poll();
            quantumUsed = 0;
            boolean switching = lastOnCpu != null && lastOnCpu != next;
            if (switching) {
                result.contextSwitches++;
            }
            if (switching && switchTime > 0) {
                switchingIn = next;
                switchLeft = switchTime;
                result.log(time, "context switch " + lastOnCpu.name() + " -> " + next.name());
            } else {
                running = next;
                result.log(time, next.name() + " runs");
            }
        }

        void runOneUnit() {
            if (switchingIn != null) {
                result.record("CS");
                switchingIn.waitingTime++;
                switchLeft--;
                if (switchLeft == 0) {
                    running = switchingIn;
                    switchingIn = null;
                    result.log(time + 1, running.name() + " runs");
                }
            } else if (running != null) {
                if (running.firstRunTime < 0) {
                    running.firstRunTime = time;
                }
                running.currentBurst().work();
                result.record(running.name());
                result.busyTime++;
                lastOnCpu = running;
                quantumUsed++;
            } else {
                result.record("idle");
            }
            for (Process p : readyQueue) {
                p.waitingTime++;
            }
            for (Process p : deviceQueue) {
                p.currentBurst().work();
            }
            time++;
        }

        void moveOn() {
            if (running != null && running.currentBurst().isDone()) {
                Process p = running;
                running = null;
                cpuBurstFinished(p, p.currentBurst().duration);
                p.nextBurst();
                goToNextStop(p);
            }
            List<Process> doneWithIo = new ArrayList<>();
            Iterator<Process> it = deviceQueue.iterator();
            while (it.hasNext()) {
                Process p = it.next();
                if (p.currentBurst().isDone()) {
                    it.remove();
                    doneWithIo.add(p);
                }
            }
            for (Process p : doneWithIo) {
                result.log(time, p.name() + " finished I/O");
                p.nextBurst();
                goToNextStop(p);
            }
        }

        /** After a burst ends: leave the system, or go wherever the next burst belongs. */
        private void goToNextStop(Process p) {
            if (p.isFinished()) {
                p.completionTime = time;
                inSystem--;
                finished++;
                result.log(time, p.name() + " finished");
            } else {
                enter(p);
            }
        }

        /** CPU bursts wait in the ready queue, I/O bursts go to the device queue. */
        private void enter(Process p) {
            if (p.currentBurst().isCpu) {
                toReadyQueue(p);
            } else {
                deviceQueue.add(p);
                result.log(time, p.name() + " starts I/O");
            }
        }

        private void toReadyQueue(Process p) {
            p.readySeq = nextSeq++;
            readyQueue.add(p);
        }
    }
}
