import java.util.Comparator;

/** First in, first out: the process that entered the ready queue first comes first (FCFS, Round Robin). */
public class ReadyOrderSorter implements Comparator<Process> {

    @Override
    public int compare(Process a, Process b) {
        return Long.compare(a.readySeq, b.readySeq);
    }
}
