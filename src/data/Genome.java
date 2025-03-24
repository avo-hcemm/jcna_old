package data;


import java.io.File;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents a genome build. Stores information on chromosomes
 * @author Stefan Grabuschnig
 *
 */
public class Genome {
	
	private TreeMap<String, Chromosome> chromosomes;
	
	/**
	 * instantiates empty genome (no chromosomes yet)
	 */
	public Genome() {
		this.chromosomes = new TreeMap<String, Chromosome>();
	}
	
	/**
	 * adds chromosome information
	 * @param chromosomeName name of the chromosome
	 * @param chromosomeSize size of the chromosome (in base pairs)
	 * @param fastAPath path to a fasta file containing the sequence of the chromosome
	 */
	public void addChromosome(String chromosomeName, int chromosomeSize, String fastAPath) {
		this.chromosomes.put(chromosomeName, new Chromosome(chromosomeSize, fastAPath));
	}
	
	/**
	 * @param chromosomeName the name of the chromosome
	 * @return the size of the chromosome (in base pairs)
	 */
	public int getChromosomeSize(String chromosomeName) {
		return this.chromosomes.get(chromosomeName).getSize();
	}
	
	/**
	 * @param chromosomeName the name of the chromosome
	 * @return the path to the fasta file
	 */
	public File getFastA(String chromosomeName) {
		return this.chromosomes.get(chromosomeName).getFastA();
	}
	
	/**
	 * @param chromosomeName the name of the chromosome
	 * @return true if the genome oject contains a chromosome with the specified name
	 */
	public boolean contains(String chromosomeName) {
		if(chromosomeName != null)
			return this.chromosomes.containsKey(chromosomeName);
		return false;
	}
	
	/**
	 * @return the number of chromosomes
	 */
	public int getNumberOfChromosomes() {
		return this.chromosomes.size();
	}
	
	/**
	 * @return all chromosome names
	 */
	public Set<String> getChromosomeNames() {
		return this.chromosomes.keySet();
	}
	
	/**
	 * @return the genome size = sum of all chromosome sizes
	 */
	public long getGenomeLength() {
		long length = 0;
		for(Chromosome c : this.chromosomes.values())
			length += c.getSize();
		return length;
	}
	
	private class Chromosome{
		private int size;
		private File fastAFile;
		
		Chromosome(int size, String fastAPath) {
			this.size = size;
			this.fastAFile = new File(fastAPath);
			if(!fastAFile.exists()) {
				System.out.println("File " + fastAPath + " does not exist!");
				System.exit(0);
			}
		}
		
		public int getSize() {
			return this.size;
		}
		public File getFastA() {
			return this.fastAFile;
		}
	}
	
}