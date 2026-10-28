import java.util.Comparator;

public class ProcessPrioritySorter implements Comparator<Process> {

	@Override
	public int compare(Process first, Process second) {
		return Integer.compare(first.priority, second.priority);
	}
}
