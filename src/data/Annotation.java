package data;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import config.Config;

/**
 * Represents gene and repeat annotation for a distinct genomic region
 * @author Stefan Grabuschnig
 *
 */
public class Annotation {

	private ArrayList<Gene> genes;
	private ArrayList<Repeat> repeats;
	private ArrayList<Region> dnaseSensitiveSites;
	private String chromosome;
	private int start;
	private int stop;

	/**
	 * @param chromosome name of the chromosome
	 * @param start genomic start coordinate of the region	
	 * @param stop genomic stop coordinate of the region
	 */
	public Annotation(String chromosome, int start, int stop) {
		this.genes = new ArrayList<Gene>(10);
		this.repeats = new ArrayList<Repeat>(10);
		this.dnaseSensitiveSites = new ArrayList<Region>(10);
		this.chromosome = chromosome;
		this.start = start;
		this.stop = stop;
		this.retrieveAnnotation();
	}

	private void retrieveAnnotation() {
		DataBase db = new DataBase(Config.dbDriver, Config.dbURL, Config.dbUser, Config.dbPassword);
		db.connect();
		
		String query = "SELECT refGene.name, refGene.name2, refGene.txStart, refGene.txEnd, refGene.exonStarts, refGene.exonEnds, refLink.product FROM "
				+ Config.dbSchema + ".refGene INNER JOIN  hgFixed.refLink ON refGene.name = refLink.mrnaAcc"
				+ " WHERE chrom = 'chr" + this.chromosome + "' AND (" + this.start
				+ " BETWEEN refGene.txStart AND refGene.txEnd OR " + this.stop
				+ " BETWEEN refGene.txStart AND refGene.txEnd OR refGene.txStart BETWEEN " + this.start + " AND "
				+ this.stop + "  OR refGene.txEnd BETWEEN " + this.start + " AND " + this.stop + ");";

		String query2 = "SELECT  repName, repClass, repFamily, genoStart, genoEnd FROM " + Config.dbSchema + ".rmsk"
				+ " WHERE genoName = 'chr" + this.chromosome + "' AND (" + this.start
				+ " BETWEEN genoStart AND genoEnd OR " + this.stop
				+ " BETWEEN genoStart AND genoEnd OR genoStart BETWEEN " + this.start + " AND " + this.stop
				+ "  OR genoEnd BETWEEN " + this.start + " AND " + this.stop + ");";
		
		String query3 = "SELECT  chrom, chromStart, chromEnd FROM " + Config.dbSchema + ".wgEncodeRegDnaseClustered"
				+ " WHERE chrom = 'chr" + this.chromosome + "' AND (" + this.start
				+ " BETWEEN chromStart AND chromEnd OR " + this.stop
				+ " BETWEEN chromStart AND chromEnd OR chromStart BETWEEN " + this.start + " AND " + this.stop
				+ "  OR chromEnd BETWEEN " + this.start + " AND " + this.stop + ");";
		
		ResultSet rs = db.performQuery(query);

		try {
			while (rs.next()) {
				Blob exonStarts = rs.getBlob("refGene.exonStarts");
				Blob exonEnds = rs.getBlob("refGene.exonEnds");

				String starts = new String(exonStarts.getBytes(1l, (int) exonStarts.length()));
				String ends = new String(exonEnds.getBytes(1l, (int) exonEnds.length()));

				StringTokenizer tokenizer = new StringTokenizer(starts, ",");
				StringTokenizer tokenizer2 = new StringTokenizer(ends, ",");

				ArrayList<int[]> exons = new ArrayList<int[]>(10);

				while (tokenizer.hasMoreTokens()) {
					int[] exon = new int[2];
					exon[0] = Integer.parseInt(tokenizer.nextToken());
					exon[1] = Integer.parseInt(tokenizer2.nextToken());
					exons.add(exon);
				}

				this.genes.add(new Gene(rs.getString("refGene.name"), rs.getString("refGene.name2"),
						rs.getString("refLink.product"), rs.getInt("refGene.txStart"), rs.getInt("refGene.txEnd"),
						exons));
			}
			
			rs.close();
			rs = db.performQuery(query2);

			while (rs.next()) {
				this.repeats.add(new Repeat(rs.getString("repName"), rs.getString("repClass"),
						rs.getString("repFamily"), rs.getInt("genoStart"), rs.getInt("genoEnd")));
			}
			
			rs.close();
			rs = db.performQuery(query3);

			while (rs.next()) {
				this.dnaseSensitiveSites.add(new Region(rs.getString("chrom"), rs.getInt("chromStart"), rs.getInt("chromEnd")));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		db.disconnect();
	}

	/** 
	 * @return list of annotated genes
	 */
	public ArrayList<Gene> getGenes() {
		return this.genes;
	}

	/**
	 * @return list of annotated repeat regions
	 */
	public ArrayList<Repeat> getRepeats() {
		return this.repeats;
	}
	
	/**
	 * @return list of DNAse sensitive sites
	 */
	public ArrayList<Region> getDNAseSensitiveSites() {
		return this.dnaseSensitiveSites;
	}
}
