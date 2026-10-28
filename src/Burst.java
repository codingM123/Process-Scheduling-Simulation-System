/**
 * One burst in a process's life: either CPU work or an I/O operation.
 */
public class Burst {

    final boolean isCpu;   // true = CPU burst, false = I/O burst
    final int duration;    // total length in time units
    int remaining;         // time units still left to do

    public Burst(boolean isCpu, int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("a burst must last at least 1 time unit");
        }
        this.isCpu = isCpu;
        this.duration = duration;
        this.remaining = duration;
    }

    /** Does one time unit of work on this burst. */
    void work() {
        remaining--;
    }

    boolean isDone() {
        return remaining == 0;
    }

    Burst copy() {
        Burst b = new Burst(isCpu, duration);
        b.remaining = remaining;
        return b;
    }

    @Override
    public String toString() {
        return (isCpu ? "CPU(" : "IO(") + duration + ")";
    }
}
