import java.util.ArrayList;
import java.util.List;

/** What happened in one simulation: per-process times, what the CPU did in every time unit, and an event log. */
public class SimulationResult {

    final String algorithm;
    final List<Process> processes;
    final List<String> timeline = new ArrayList<>(); // CPU activity per time unit: a process, "CS" or "idle"
    final List<String> events = new ArrayList<>();
    int totalTime;
    int busyTime;          // time units spent running processes
    int contextSwitches;

    SimulationResult(String algorithm, List<Process> processes) {
        this.algorithm = algorithm;
        this.processes = processes;
    }

    void record(String cpuActivity) {
        timeline.add(cpuActivity);
    }

    void log(int time, String event) {
        events.add("t=" + time + "  " + event);
    }

    double averageWaitingTime() {
        double sum = 0;
        for (Process p : processes) {
            sum += p.waitingTime;
        }
        return sum / processes.size();
    }

    double averageTurnaroundTime() {
        double sum = 0;
        for (Process p : processes) {
            sum += p.turnaroundTime();
        }
        return sum / processes.size();
    }

    double averageResponseTime() {
        double sum = 0;
        for (Process p : processes) {
            sum += p.responseTime();
        }
        return sum / processes.size();
    }

    /** Share of time the CPU spent running processes (idle time and context switches don't count). */
    double cpuUtilization() {
        return totalTime == 0 ? 0 : 100.0 * busyTime / totalTime;
    }

    /** Processes finished per 100 time units. */
    double throughput() {
        return totalTime == 0 ? 0 : 100.0 * processes.size() / totalTime;
    }

    /** Gantt chart of the CPU, for example  | P1 | P2 | CS | P3 |  with the times underneath. */
    String ganttChart() {
        if (timeline.isEmpty()) {
            return "";
        }
        StringBuilder bars = new StringBuilder("|");
        StringBuilder times = new StringBuilder("0");
        int start = 0;
        for (int t = 1; t <= timeline.size(); t++) {
            if (t < timeline.size() && timeline.get(t).equals(timeline.get(start))) {
                continue;
            }
            String label = timeline.get(start);
            String end = String.valueOf(t);
            int width = Math.max(label.length() + 2, end.length() + 1);
            bars.append(centered(label, width)).append('|');
            padTo(times, bars.length() - 1);
            times.append(end);
            start = t;
        }
        return bars + System.lineSeparator() + times;
    }

    private static String centered(String text, int width) {
        int left = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        padTo(sb, left);
        sb.append(text);
        padTo(sb, width);
        return sb.toString();
    }

    private static void padTo(StringBuilder sb, int length) {
        while (sb.length() < length) {
            sb.append(' ');
        }
    }
}
