package data;


/**
 * Represents a genomic repeat region.
 * @author Stefan Grabuschnig
 *
 */
public class Repeat {
	
	private final String repName;
	private final String repClass;
	private final String repFamily;
	
	private final int start;
	private final int end;
	
	/**
	 * @param repName repeat name from annotation database
	 * @param repClass repeat class from annotation database
	 * @param repFamily repeat family from annotation database
	 * @param start genomic start coordinate
	 * @param end genomic end coordinate
	 */
	public Repeat(String repName, String repClass, String repFamily, int start, int end) {
		this.repName = repName;
		this.repClass = repClass;
		this.repFamily = repFamily;
		this.start = start;
		this.end = end;
	}

	public String getRepName() {
		return repName;
	}

	public String getRepClass() {
		return repClass;
	}

	public String getRepFamily() {
		return repFamily;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}
}
