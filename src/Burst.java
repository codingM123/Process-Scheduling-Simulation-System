/**
  Represents a CPU or I/O burst in a process lifecycle.
  Each burst has a type, required time, and current progress.
 */
public class Burst implements Cloneable {

	// 'true' indicates CPU burst, 'false' for I/O burst
	boolean type;
	int timeNeeded;  // Total time required for this burst
	int timeWorked;  // Time already executed on this burst

	/**
	 * Initializes a burst with its type and required time.
	 * @param type - burst type (CPU or I/O)
	 * @param timeNeeded - total time required for completion
	 * @param timeWorked - initial progress on this burst (usually 0)
	 */
	public Burst(boolean type, int timeNeeded, int timeWorked) {
		super();
		this.type = type;
		this.timeNeeded = timeNeeded;
		this.timeWorked = timeWorked;
	}

	/**
	 * Simulates work done on the burst for a given time unit.
	 * Decreases remaining time and updates work progress.
	 * @param timeUnit - time units to apply as work
	 */
	public void applyWork(int timeUnit) {
		timeNeeded -= timeUnit;
		timeWorked += timeUnit;
	}

	/**
	 * Creates a duplicate of this Burst object.
	 * Useful for deep-copying bursts in scheduling simulations.
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
