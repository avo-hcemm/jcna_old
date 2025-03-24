package parallel;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import config.Config;

/**
 * Provides functionality for parallelized addition of large float arrays. Number of threads specified in config
 * @author Stefan Grabuschnig
 *
 */
public class ParallelArrayAdder {
	
	protected static final ExecutorService executor = Executors.newCachedThreadPool();
	
	/**
	 * Performs parallelized addition of two float arrays
	 * @param target target array for addition
	 * @param addend array added to target
	 * @throws InterruptedException interrupted exception
	 */
	public static void add(float[] target, float[] addend) throws InterruptedException {
		// partition search space and prepare threads for current normalizedCoverage
		ArrayList<Callable<Boolean>> threads = new ArrayList<Callable<Boolean>>(Config.numThreads);

		int positionStart = 0;
		int partitionLength = target.length / Config.numThreads;

		for (int i = 0; i < Config.numThreads - 1; i++) {
			threads.add(new ArrayAdderThread(target, addend, positionStart, positionStart + partitionLength - 1));
			positionStart += partitionLength;
		}
		threads.add(new ArrayAdderThread(target, addend, positionStart, target.length - 1));

		// execute Threads!
		ParallelArrayAdder.executor.invokeAll(threads);
	}
}
