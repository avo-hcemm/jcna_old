package data;


import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a distinct individual in the data base (patient or animal).
 * @author Stefan Grabuschnig
 *
 */
public class Individual {
	private String id;
	private Label label;
	private HashMap<String,Alignment> alignments;
	
	/**
	 * @param id name or id of the individual
	 * @param label classification of the individual (i.e. healthy or disease)
	 */
	public Individual(String id, Label label) {
		this.label = label;
		this.id = id;
		this.alignments = new HashMap<String,Alignment>(20);
	}
	
	/**
	 * assigns an alignment (sam/bam) to the individual
	 * @param alignment an alignment object
	 */
	
	public void addAlignment(Alignment alignment) {
		this.alignments.put(alignment.getID(), alignment);
	}
	
	/**
	 * @return the name or id
	 */
	public String getID() {
		return this.id;
	}
	
	/**
	 * @return the classification label
	 */
	public Label getLabel() {
		return this.label;
	}
	
	/**
	 * @return list of all allignments assigned to the respective individual
	 */
	
	public ArrayList<Alignment> getAlignments() {
		ArrayList<Alignment> alignments = new ArrayList<Alignment>(this.alignments.size());
		alignments.addAll(this.alignments.values());
		return alignments;
	}

}
