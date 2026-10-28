import java.util.Comparator;

/** Order of the job queue: earliest arrival first, then lowest process id. */
public class ProcessArrivalSorter implements Comparator<Process> {

    @Override
    public int compare(Process a, Process b) {
        if (a.arrivalTime != b.arrivalTime) {
            return Integer.compare(a.arrivalTime, b.arrivalTime);
        }
        return Integer.compare(a.pid, b.pid);
    }
}
