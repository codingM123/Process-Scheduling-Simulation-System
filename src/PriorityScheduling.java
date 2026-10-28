import java.util.Comparator;

/**
 * Preemptive priority scheduling: a lower number means a higher priority. When a higher-priority
 * process becomes ready, it takes the CPU from the running process.
 */
public class PriorityScheduling extends Scheduler {

    public PriorityScheduling() {
        super("Priority");
    }

    @Override
    protected Comparator<Process> readyOrder() {
        return new ProcessPrioritySorter();
    }

    @Override
    protected boolean preempts(Process candidate, Process running) {
        return candidate.priority < running.priority;
    }
}
