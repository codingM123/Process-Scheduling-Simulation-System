import java.util.Comparator;

/**
 * Priority scheduling: the lowest priority number comes first.
 * Ties go to the process that has been in the ready queue longest.
 */
public class ProcessPrioritySorter implements Comparator<Process> {

    @Override
    public int compare(Process a, Process b) {
        if (a.priority != b.priority) {
            return Integer.compare(a.priority, b.priority);
        }
        return Long.compare(a.readySeq, b.readySeq);
    }
}
