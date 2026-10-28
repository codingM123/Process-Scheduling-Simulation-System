import java.util.ArrayList;

public class Process implements Cloneable {

	int processId; // Unique Process ID
	static int idCounter = 0;
	int priority;
	int arrivalTime;
	Burst[] bursts;
	int currentBurstIndex;
	int totalTimeInSystem;
	int totalWaitingTime;
	int burstRemainingTime;
	int tempBurstIndex;
	double estimatedTau;
	double smoothingFactor;
	int cpuBurstSumHolder;

	public Process(int priority, int arrivalTime, Burst[] bursts, int initialTau) {
		this.priority = priority;
		this.arrivalTime = arrivalTime;
		this.bursts = bursts;
		this.processId = idCounter++;
		this.estimatedTau = initialTau;
	}

	public Burst getActiveBurst() {
		return bursts[currentBurstIndex];
	}

	public void reduceIOBurst() {
		bursts[currentBurstIndex].applyWork(1);
	}

	public void reduceCPUBurst() {
		if (currentBurstIndex < bursts.length && bursts[currentBurstIndex].type) {
			if (bursts[currentBurstIndex].timeNeeded > 0) {
				bursts[currentBurstIndex].applyWork(1);
				if (bursts[currentBurstIndex].timeNeeded == 0) {
					currentBurstIndex++;
					if (currentBurstIndex < bursts.length && !bursts[currentBurstIndex].type) {
						updateTauAfterBurst();
					}
				}
			}
		}
	}

	public boolean isOnCPU() {
		return currentBurstIndex < bursts.length && bursts[currentBurstIndex].type;
	}

	public int computeRemainingCPUBurstTime() {
		System.out.println("Calculating remaining CPU time for Process " + processId);

		int total = 0;
		tempBurstIndex = currentBurstIndex;
		while (tempBurstIndex < bursts.length && bursts[tempBurstIndex].type) {
			total += bursts[tempBurstIndex].timeNeeded;
			tempBurstIndex++;
		}
		System.out.println("Total remaining CPU burst time for Process " + processId + ": " + total);
		cpuBurstSumHolder = total;
		return total;
	}

	public double calculateExponentialAverage() {
		System.out.println("Exponential Averaging for Process " + processId);
		return estimatedTau * smoothingFactor + (1 - smoothingFactor) * computeRemainingCPUBurstTime();
	}

	public void updateTauAfterBurst() {
		System.out.println("Previous Burst Sum: " + cpuBurstSumHolder);
		estimatedTau = estimatedTau * smoothingFactor + (1 - smoothingFactor) * cpuBurstSumHolder;
		System.out.println("Updated Tau for Process " + processId + ": " + estimatedTau);
	}

	public void resetBurstIndex() {
		currentBurstIndex = tempBurstIndex;
	}

	@Override
	public String toString() {
		return "Arrival Time: " + arrivalTime;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		Process clonedProcess = (Process) super.clone();
		clonedProcess.bursts = new Burst[bursts.length];

		for (int i = 0; i < bursts.length; i++) {
			clonedProcess.bursts[i] = (Burst) bursts[i].clone();
		}

		return clonedProcess;
	}
}
