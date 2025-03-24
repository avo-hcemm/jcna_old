package data;

import java.util.ArrayList;

/** Represents a gene annotation
 * @author Stefan Grabuschnig
 *
 */
public class Gene {
	
	private final String refSeqID;
	private final String geneName;
	private final String product;
	
	private final int txStart;
	private final int txEnd;
	
	private final ArrayList<int[]> exons;
	
	/**
	 * @param refSeqID the refSeq ID
	 * @param geneName name of the gene
	 * @param product name of the gene product
	 * @param txStart genomic start coordinate
	 * @param txEnd genomic stop coordinate
	 * @param exons list of exon start stop coordinates
	 */
	public Gene(String refSeqID, String geneName, String product, int txStart, int txEnd, ArrayList<int[]> exons) {
		this.refSeqID = refSeqID;
		this.geneName = geneName;
		this.product = product;
		this.txStart = txStart;
		this.txEnd = txEnd;
		this.exons = exons;
	}

	/**
	 * @return the refSeq ID
	 */
	public String getRefSeqID() {
		return refSeqID;
	}

	/**
	 * @return the gene name
	 */
	public String getGeneName() {
		return geneName;
	}

	/**
	 * @return rhe gene product name
	 */
	public String getProduct() {
		return product;
	}

	/**
	 * @return the genomic start coordinate
	 */
	public int getTxStart() {
		return txStart;
	}

	/**
	 * @return the genomic stop coordinate
	 */
	public int getTxEnd() {
		return txEnd;
	}

	/**
	 * @return the list of exon start stop coordinates
	 */
	public ArrayList<int[]> getExons() {
		return exons;
	}
}
