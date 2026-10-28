import java.util.Comparator;

/** First-Come, First-Served: processes run in the order they became ready, and are never preempted. */
public class FirstComeFirstServed extends Scheduler {

    public FirstComeFirstServed() {
        super("FCFS");
    }

    @Override
    protected Comparator<Process> readyOrder() {
        return new ReadyOrderSorter();
    }
}
