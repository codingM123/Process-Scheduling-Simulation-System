import java.util.Comparator;

/**
 * SJF with burst prediction. The scheduler can't know how long the next CPU burst will be, so it
 * predicts it from past bursts with an exponential average:
 *
 *     tau(n+1) = alpha * t(n) + (1 - alpha) * tau(n)
 *
 * where t(n) is the length of the burst that just finished and tau(n) was its prediction.
 * The process with the shortest prediction runs next (non-preemptive).
 */
public class ExponentialAveraging extends Scheduler {

    private final double alpha;

    public ExponentialAveraging(double alpha) {
        super("SJF + exp. averaging");
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha must be between 0 and 1");
        }
        this.alpha = alpha;
    }

    /** One step of the exponential average. */
    static double predictNext(double previousPrediction, int actualBurst, double alpha) {
        return alpha * actualBurst + (1 - alpha) * previousPrediction;
    }

    @Override
    protected Comparator<Process> readyOrder() {
        return new ProcessExpAverageSorter();
    }

    @Override
    protected void cpuBurstFinished(Process p, int actualLength) {
        p.tau = predictNext(p.tau, actualLength, alpha);
    }
}
