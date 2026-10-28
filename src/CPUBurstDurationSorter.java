import java.util.Comparator;

/**
 * SJF and SRTF: the process with the least time left in its current CPU burst comes first.
 * Ties go to the process that has been in the ready queue longest.
 */
public class CPUBurstDurationSorter implements Comparator<Process> {

    @Override
    public int compare(Process a, Process b) {
        int byBurst = Integer.compare(a.currentBurst().remaining, b.currentBurst().remaining);
        if (byBurst != 0) {
            return byBurst;
        }
        return Long.compare(a.readySeq, b.readySeq);
    }
}
