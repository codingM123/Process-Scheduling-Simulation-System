import java.util.Comparator;

public class ProcessExpAverageSorter implements Comparator<Object> {

	@Override
	public int compare(Object first, Object second) {
		Process processOne = (Process) first;
		Process processTwo = (Process) second;
		return Double.compare(processOne.getExpAveraging(), processTwo.getExpAveraging());
	}
}
