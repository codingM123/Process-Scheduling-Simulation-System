import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

	public static void main(String[] args) throws CloneNotSupportedException {

		Scanner scan = new Scanner(System.in);

		// Pehle basic inputs le rahe hain user se
		System.out.println("Enter the context switch:");
		int context = scan.nextInt();

		System.out.println("Enter total number of processes:");
		int numberOfTotalProcesses = scan.nextInt();

		System.out.println("Enter degree of Multiprogramming:");
		int degreeOfMP = scan.nextInt();

		System.out.println("Enter number of bursts per process:");
		int numberOfBursts = scan.nextInt();

		System.out.println("CPU burst time range:");
		System.out.print("Min: ");
		int minCPU = scan.nextInt();
		System.out.print("Max: ");
		int maxCPU = scan.nextInt();

		System.out.println("IO burst time range:");
		System.out.print("Min: ");
		int minIO = scan.nextInt();
		System.out.print("Max: ");
		int maxIO = scan.nextInt();

		System.out.println("Priority range:");
		System.out.print("Min: ");
		int minPriority = scan.nextInt();
		System.out.print("Max: ");
		int maxPriority = scan.nextInt();

		System.out.println("Initial arrival time range:");
		System.out.print("Min: ");
		int minArrival = scan.nextInt();
		System.out.print("Max: ");
		int maxArrival = scan.nextInt();

		System.out.println("Enter initial Tau value:");
		int initialTau = scan.nextInt();

		System.out.println("Enter alpha value:");
		double alpha = scan.nextDouble();

		System.out.println("Select random burst generation type:");
		System.out.println("(0) for Gaussian / (any other) for Binomial:");
		int type = scan.nextInt();

		boolean randomType = (type == 0);

		// Ab hum process queue bana rahe hain (sorted by arrival time)
		Queue<Process> jobQueue = new PriorityQueue<>(numberOfTotalProcesses, new SortByArraivalTime());

		// Randomly processes generate karenge according to user inputs
		generateJobs(jobQueue, numberOfTotalProcesses, randomType, numberOfBursts, minCPU, maxCPU, minIO, maxIO,
				minPriority, maxPriority, minArrival, maxArrival, initialTau);

		// 6 clones bana rahe hain jobQueue ke - taaki har scheduling algorithm ko alag se apply kar sakein
		Queue<Process> jobQueueInst1 = new PriorityQueue<>(numberOfTotalProcesses, new SortByArraivalTime());
		for (Process p : jobQueue) jobQueueInst1.add((Process) p.clone());

		Queue<Process> jobQueueInst2 = new PriorityQueue<>(numberOfTotalProcesses, new SortByArraivalTime());
		for (Process p : jobQueue) jobQueueInst2.add((Process) p.clone());

		Queue<Process> jobQueueInst3 = new PriorityQueue<>(numberOfTotalProcesses, new SortByArraivalTime());
		for (Process p : jobQueue) jobQueueInst3.add((Process) p.clone());

		Queue<Process> jobQueueInst4 = new PriorityQueue<>(numberOfTotalProcesses, new SortByArraivalTime());
		for (Process p : jobQueue) jobQueueInst4.add((Process) p.clone());

		Queue<Process> jobQueueInst5 = new PriorityQueue<>(numberOfTotalProcesses, new SortByArraivalTime());
		for (Process p : jobQueue) jobQueueInst5.add((Process) p.clone());

		Queue<Process> jobQueueInst6 = new PriorityQueue<>(numberOfTotalProcesses, new SortByArraivalTime());
		for (Process p : jobQueue) jobQueueInst6.add((Process) p.clone());

		// Yahan har algorithm ke object create kar rahe hain with their respective queues:
		FirstComeFirstServed FCFSObject = new FirstComeFirstServed(jobQueueInst1, new LinkedList<>(),
				new LinkedList<>(), degreeOfMP, context);

		ShortestJobFirst SJFObject = new ShortestJobFirst(jobQueueInst2,
				new PriorityQueue<>(numberOfTotalProcesses, new SortByShortestCPUBurst()), new LinkedList<>(), degreeOfMP);

		ShortestRemainingTimeFirst SRJFObject = new ShortestRemainingTimeFirst(jobQueueInst3,
				new PriorityQueue<>(numberOfTotalProcesses, new SortByShortestCPUBurst()), new LinkedList<>(), degreeOfMP);

		Priority priority = new Priority(jobQueueInst4,
				new PriorityQueue<>(numberOfTotalProcesses, new SortByPriority()), new LinkedList<>(), degreeOfMP);

		RoundRobin RR = new RoundRobin(jobQueueInst5,
				new PriorityQueue<>(numberOfTotalProcesses, new SortByArraivalTime()), new LinkedList<>(), degreeOfMP);

		ExponentialAveraging expAveraging = new ExponentialAveraging(jobQueueInst6,
				new PriorityQueue<>(numberOfTotalProcesses, new SortByExpAveraging()), new LinkedList<>(), degreeOfMP,
				alpha, context);

		// Before applying algorithms, processes ka initial status dekhte hain
		System.out.println("Initial status of processes before applying any algorithm:");
		System.out.println("________________");
		while (!jobQueue.isEmpty()) {
			Process p = jobQueue.poll();
			System.out.println("pID: " + p.pID + "\tArrival Time: " + p.arrivalTime + "\tPriority: " + p.priority
					+ "\tBursts: " + returnBursts(p));
		}

		System.out.println("Press Enter to execute Exponential Averaging Scheduling:");
		scan.nextLine();
		scan.nextLine();
		
		// Yahan par hum Exponential Averaging algorithm ko run kar rahe hain (baaki bhi run kar sakte ho)
		expAveraging.applyExpAverage();
	}

	// Ye method process ke CPU/IO bursts ko display format me laata hai
	public static String returnBursts(Process p) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < p.content.length; i++) {
			if (p.content[i].type) {
				str.append("CPU(").append(p.content[i].timeNeeded).append(")");
			} else {
				str.append("IO(").append(p.content[i].timeNeeded).append(")");
			}
			if (i != p.content.length - 1) {
				str.append(", ");
			}
		}
		return str.toString();
	}

	// Ye method random processes generate karta hai with bursts and other properties
	public static void generateJobs(Queue<Process> jobQueue, int numberOfTotalProcesses, boolean typeOfRandomGenerator,
			int numberOfBursts, int minCPU, int maxCPU, int minIO, int maxIO, int minPriority, int maxPriority,
			int minArrival, int maxArrival, int initialTau) {

		for (int i = 0; i < numberOfTotalProcesses; i++) {
			int numberOfCpuBursts;
			if (typeOfRandomGenerator)
				numberOfCpuBursts = randomGaussianInt(0.75 * numberOfBursts, numberOfBursts / 2, numberOfBursts);
			else
				numberOfCpuBursts = randomBinomialInt(0.75 * numberOfBursts, numberOfBursts / 2, numberOfBursts);

			int numberOfIoBursts = numberOfBursts - numberOfCpuBursts;

			Burst[] content = new Burst[numberOfBursts];
			content[0] = new Burst(true, ThreadLocalRandom.current().nextInt(minCPU, maxCPU + 1), 0);
			content[numberOfBursts - 1] = new Burst(true, ThreadLocalRandom.current().nextInt(minCPU, maxCPU + 1), 0);
			numberOfCpuBursts -= 2;

			for (int j = 1; j < numberOfBursts - 1; j++) {
				if (content[j - 1].type) {
					if (numberOfCpuBursts + 1 > numberOfIoBursts) {
						if (numberOfCpuBursts != 0 && (ThreadLocalRandom.current().nextInt(0, 2) == 0 || numberOfIoBursts == 0)) {
							content[j] = new Burst(true, ThreadLocalRandom.current().nextInt(minCPU, maxCPU + 1), 0);
							numberOfCpuBursts--;
						} else {
							content[j] = new Burst(false, ThreadLocalRandom.current().nextInt(minIO, maxIO + 1), 0);
							numberOfIoBursts--;
						}
					} else {
						content[j] = new Burst(false, ThreadLocalRandom.current().nextInt(minIO, maxIO + 1), 0);
						numberOfIoBursts--;
					}
				} else {
					content[j] = new Burst(true, ThreadLocalRandom.current().nextInt(minCPU, maxCPU + 1), 0);
					numberOfCpuBursts--;
				}
			}

			jobQueue.add(new Process(ThreadLocalRandom.current().nextInt(minPriority, maxPriority + 1),
					ThreadLocalRandom.current().nextInt(minArrival, maxArrival + 1), content, initialTau));
		}
	}

	// Binomial random generator
	public static int randomBinomialInt(double mean, int min, int max) {
		if (max < min || mean < min || mean > max) throw new IllegalArgumentException
