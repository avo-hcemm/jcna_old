package data;


/**
 * Represents a distinct region on the genome. Provides functionality to compare region sizes.
 * @author Stefan Grabuschnig
 *
 */
public class Region implements Comparable<Region>{
	
	private String name;
	private String chromosome;
	private int start;
	private int stop;
	
	/**
	 * @param chromosome name of the chromosome
	 * @param start start coordinate
	 * @param stop stop coordinate
	 */
	public Region(String chromosome, int start, int stop) {
		this.chromosome = chromosome;
		this.start = start;
		this.stop = stop;
		this.name = this.chromosome + "-" + this.start + "-" + this.stop;
	}
	
	/**
	 * @param name of the region
	 * @param chromosome name of the chromosome
	 * @param start start coordinate
	 * @param stop stop coordinate
	 */
	public Region(String name, String chromosome, int start, int stop) {
		this.name = name;
		this.chromosome = chromosome;
		this.start = start;
		this.stop = stop;
	}
	
	/**
	 * @return rhe name of the region if set
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return the name of the chromosome
	 */
	public String getChromosome() {
		return this.chromosome;
	}
	
	/**
	 * @return the start coordinate of the region
	 */
	public int getStart() {
		return this.start;
	}
	
	/**
	 * @return the stop coordinate of the region
	 */
	public int getStop() {
		return this.stop;
	}
	
	/**
	 * @return the size of the region (in base pairs)
	 */
	public int getSize() {
		return this.stop - this.start + 1;
	}

	@Override
	public int compareTo(Region r) {
		if(this.getSize() < r.getSize())
			return - 1;
		else if(this.getSize() > r.getSize())
			return 1;
		else
			return 0;
	}
}
