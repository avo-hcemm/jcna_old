package data;


/**
 * Represents a paired-end sequencing read pair
 * @author Stefan Grabuschnig
 *
 */
public class ReadPair {
	private Read first = null;
	private Read second = null;
	private boolean firstSet = false; // it is FALSE if the first read of the pair has not been set yet
	private boolean secondSet = false; // it is FALSE if the second read of the set has not been set yet
	private boolean complete = false; // it is FALSE if the reads of the pair have not been set yet

	/**
	 * Instantiates an empty read pair
	 */
	public ReadPair() {
	}
	/**
	 * sets first read in pair
	 * @param start genomic start coordinate
	 * @param end genomic stop coordinate
	 * @param mateStart start coordinate of the mate read
	 */
	public void setFirst(int start, int end, int mateStart) {
		if (this.complete) {
			System.out.println("ERROR: Insertion into complete set!");
		} else if (this.firstSet) {
			System.out.println("ERROR: First already set!");
		} else if (!this.secondSet) {
			this.first = new Read(start, end, mateStart);
			this.firstSet = true;
		} else if (this.second.getMateStart() == start) {
			this.first = new Read(start, end, mateStart);
			this.firstSet = true;
			this.complete = true;
		} else {
			System.out.println("ERROR: First at wrong position!");
		}
	}

	/**
	 * sets second read in pair
	 * @param start genomic start coordinate
	 * @param end genomic stop coordinate
	 * @param mateStart start coordinate of the mate read
	 */
	public void setSecond(int start, int end, int mateStart) {
		if (this.complete) {
			System.out.println("ERROR: Insertion into complete set!");
		} else if (this.secondSet) {
			System.out.println("ERROR: Second already set!");
		} else if (!this.firstSet) {
			this.second = new Read(start, end, mateStart);
			this.secondSet = true;
		} else if (this.first.getMateStart() == start) {
			this.second = new Read(start, end, mateStart);
			this.secondSet = true;
			this.complete = true;
		} else {
			System.out.println("ERROR: Second at wrong position!");
		}
	}

	/**
	 * @return true if both reads are set (complete pair)
	 */
	public boolean isComplete() {
		return this.complete;
	}

	/**
	 * @return length of the DNA fragment from which both reads supposedly originated
	 */ 
	public int getLength() {
		if (this.complete)
			return this.getFragmentEnd() - this.getFragmentStart() + 1;
		else
			System.out.println("ERROR: Incomplete read pair getLength()!");
		return -1;
	}

	/**
	 * @return start coordinate of the DNA fragment from which both reads supposedly originated
	 */
	public int getFragmentStart() {
		if (this.complete) {
			if (this.first.getStart() < this.second.getStart())
				return this.first.getStart();
			else {
				return this.second.getStart();
			}
		} else
			System.out.println("ERROR: Incomplete read pair getFragmentStart()!");
		return -1;
	}

	/**
	 * @return stop coordinate of the DNA fragment from which both reads supposedly originated
	 */
	public int getFragmentEnd() {
		if (this.complete) {
			if (this.first.getEnd() < this.second.getEnd())
				return this.second.getEnd();
			else {
				return this.first.getEnd();
			}
		} else
			System.out.println("ERROR: Incomplete read pair getFragmentEnnd()!");
		return -1;
	}

	private class Read {
		private int start;
		private int end;
		private int mateStart;

		private Read(int start, int end, int mateStart) {
			this.start = start;
			this.end = end;
			this.mateStart = mateStart;
		}

		private int getStart() {
			return this.start;
		}

		private int getEnd() {
			return this.end;
		}

		private int getMateStart() {
			return this.mateStart;
		}
	}

}
