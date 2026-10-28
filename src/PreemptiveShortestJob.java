import java.util.Comparator;

/**
 * Shortest Remaining Time First, the preemptive version of SJF: when a process with a shorter
 * remaining CPU burst becomes ready, it takes the CPU from the running process.
 */
public class PreemptiveShortestJob extends Scheduler {

    public PreemptiveShortestJob() {
        super("SRTF");
    }

    @Override
    protected Comparator<Process> readyOrder() {
        return new CPUBurstDurationSorter();
    }

    @Override
    protected boolean preempts(Process candidate, Process running) {
        return candidate.currentBurst().remaining < running.currentBurst().remaining;
    }
}
