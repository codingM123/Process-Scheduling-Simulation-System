import java.util.Comparator;

/**
 * SJF with prediction: the process with the shortest predicted next CPU burst (tau) comes first.
 * Ties go to the process that has been in the ready queue longest.
 */
public class ProcessExpAverageSorter implements Comparator<Process> {

    @Override
    public int compare(Process a, Process b) {
        int byPrediction = Double.compare(a.tau, b.tau);
        if (byPrediction != 0) {
            return byPrediction;
        }
        return Long.compare(a.readySeq, b.readySeq);
    }
}
