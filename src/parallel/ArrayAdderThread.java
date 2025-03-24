package parallel;


import java.util.concurrent.Callable;

/**
 * Thread adding a sub-partition of large float arrays
 * @author Stefan Grabuschnig
 *
 */
public class ArrayAdderThread implements Callable<Boolean> {
	private float[] target;
	private float[] addend;
	private int positionStart;
	private int positionEnd;

	/**
	 * @param target target array for addition
	 * @param addend array added to target
	 * @param positionStart start position of the partition
	 * @param positionEnd end of the partition
	 */
	public ArrayAdderThread(float[] target, float[] addend, int positionStart, int positionEnd) {
		this.target = target;
		this.addend = addend;
		this.positionStart = positionStart;
		this.positionEnd = positionEnd;
	}

	@Override
	public Boolean call() throws Exception {
		for (int position = positionStart; position <= positionEnd; position++)
			this.target[position] += this.addend[position];

		return null;
	}
}
