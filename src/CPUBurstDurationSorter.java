import java.util.Comparator;

public class CPUBurstDurationSorter implements Comparator<Process> {

	@Override
	public int compare(Process first, Process second) {
		return Integer.compare(first.getTimeOfCurrentCPUBurst(), second.getTimeOfCurrentCPUBurst());
	}
}
