import java.util.Comparator;

/**
 * Round Robin: processes take turns in FIFO order. A process that uses up its time quantum
 * goes to the back of the ready queue, if anyone else is waiting.
 */
public class RoundRobinScheduler extends Scheduler {

    private final int quantum;

    public RoundRobinScheduler(int quantum) {
        super("Round Robin (q=" + quantum + ")");
        if (quantum < 1) {
            throw new IllegalArgumentException("time quantum must be at least 1");
        }
        this.quantum = quantum;
    }

    @Override
    protected Comparator<Process> readyOrder() {
        return new ReadyOrderSorter();
    }

    @Override
    protected int timeQuantum() {
        return quantum;
    }
}
