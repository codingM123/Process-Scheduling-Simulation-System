import java.util.Comparator;

/** Shortest Job First, non-preemptive: the process with the shortest next CPU burst runs next. */
public class ShortestJobScheduler extends Scheduler {

    public ShortestJobScheduler() {
        super("SJF");
    }

    @Override
    protected Comparator<Process> readyOrder() {
        return new CPUBurstDurationSorter();
    }
}
