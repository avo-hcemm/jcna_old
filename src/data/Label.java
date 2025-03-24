package data;


import java.util.Collection;
import java.util.HashMap;


/**
 * Represents a classification label (i.e. healthy or disease)
 * @author Stefan Grabuschnig
 *
 */
public class Label {
	private String name;
	private HashMap<String, Individual> individuals;
	
	/**
	 * @param name name of the label
	 */
	public Label(String name) {
		this.name = name;
		this.individuals = new HashMap<String, Individual>(100);
	}
	
	/**
	 * assigns an individual to the label
	 * @param individual the respective individual
	 */

	public void addIndividual(Individual individual) {
		this.individuals.put(individual.getID(), individual);
	}

	/**
	 * @param individualID id of an individual
	 * @return true if the respective individual is assigned to the label
	 */
	
	public boolean containsIndividual(String individualID) {
		return this.individuals.containsKey(individualID);
	}
	
	/**
	 * @return the name of the label
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return list of individuals assigned to the label
	 */
	
	public Collection<Individual> getIndividuals() {
		return this.individuals.values();
	}
	
}
