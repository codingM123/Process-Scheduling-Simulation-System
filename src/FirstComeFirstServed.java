import java.util.LinkedList;
import java.util.Queue;

public class FirstComeFirstServed {

	// Job queue - yahan naye processes enter karte hain
	Queue<Process> jobQueue;

	// Ready queue - yahan se processes CPU ke liye ready hote hain
	Queue<Process> readyQueue;

	// Device list - yeh IO devices ko handle karta hai (infinite IO maan ke chal rahe hain)
	LinkedList<Process> deviceList;

	// Degree of multiprogramming - kitne processes ek saath system me ho sakte hain
	int degreeOfMP;

	// Context switching time - jab ek process se dusre process par switch karte hain toh lagne wala time
	int contix;

	// Temporary backup of job queue - future use ke liye
	LinkedList<Process> tempList;

	// Constructor - queues ko initialize karta hai
	public FirstComeFirstServed(Queue<Process> jobQueue, Queue<Process> readyQueue, LinkedList<Process> deviceList,
			int degreeOfMP, int contix) {
		super();
		this.jobQueue = jobQueue;
		this.readyQueue = readyQueue;
		this.deviceList = deviceList;
		this.degreeOfMP = degreeOfMP;
		this.contix = contix;

		tempList = new LinkedList<Process>();
		for (Process p : jobQueue) {
			tempList.add(p);
		}
	}

	// Ye method FCFS scheduling ko simulate karta hai
	public void applyFCFS() {
		int currentTime = 0; // Time tracker

		int capicity = degreeOfMP; // Current capacity (based on degree of multiprogramming)

		Process runningState = null; // Jo process abhi CPU par chal raha hai

		int totalTimeInTheSystem = 0; // System me processes ka total time
		int totalTimeInTheReadyQueue = 0; // Ready queue ka waiting time
		int timeRunningStateIsNotWorking = 0; // Jab CPU idle tha
		int numberOfProcessesDoneSoFar = 0; // Kitne processes complete ho chuke hain
		int prevTime = 0;

		boolean displayInfo = false; // Info display flag

		Process prevRunningState = null; // Last process which was running
		int wait = 0;

		while (true) {
			// Jobs ko ready queue me shift karna (arrival time check karke)
			int temp = 0;
			for (int i = 0; i < capicity; i++) {
				if (jobQueue.size() != 0) {
					if (jobQueue.peek().arrivalTime <= currentTime) {
