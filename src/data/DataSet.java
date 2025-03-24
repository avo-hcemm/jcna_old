package data;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Represents a data set of genomic alignments in sam/bam format. Provides functionality to select distinct subsets of the dataset
 * @author Stefan Grabuschnig
 *
 */
public class DataSet {
	private Genome genome;
	private HashMap<String, Label> labels;
	private HashMap<String, Individual> individuals;
	private HashMap<String, Alignment> alignments;
	private TreeSet<String> times;

	/**
	 * @param taskXML an xml file defining the respective genome and all alignment files in the data set
	 */
	public DataSet(String taskXML) {
		this.labels = new HashMap<String, Label>(10);
		this.individuals = new HashMap<String, Individual>(1000);
		this.alignments = new HashMap<String, Alignment>(10000);
		this.times = new TreeSet<String>();
		this.genome = new Genome();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;
		Document doc = null;

		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(new File(taskXML));
			doc.getDocumentElement().normalize();

			// Initialize genome information
			NodeList nodeList = doc.getElementsByTagName("chromosome");

			for (int i = 0; i < nodeList.getLength(); i++) {
				Element element = (Element) nodeList.item(i);
				if (this.genome.contains(element.getElementsByTagName("name").item(0).getTextContent())) {
					System.out.println(
							"Error: Chromosome " + element.getElementsByTagName("name").item(0).getTextContent()
									+ " defined more than once!");
					System.exit(0);
				}
				this.genome.addChromosome(element.getElementsByTagName("name").item(0).getTextContent(),
						Integer.parseInt(element.getElementsByTagName("size").item(0).getTextContent()),
						element.getElementsByTagName("fasta").item(0).getTextContent());
			}

			// Setup Alignments
			nodeList = doc.getElementsByTagName("alignment");
			this.alignments = new HashMap<String, Alignment>(nodeList.getLength());

			for (int i = 0; i < nodeList.getLength(); i++) {
				Element element = (Element) nodeList.item(i);
				String labelID = element.getElementsByTagName("label").item(0).getTextContent();
				String individualID = element.getElementsByTagName("individual").item(0).getTextContent();
				String alignmentID = element.getElementsByTagName("id").item(0).getTextContent();
				String infectionState = element.getElementsByTagName("infectionState").item(0).getTextContent();
				String time = element.getElementsByTagName("time").item(0).getTextContent();
				String bamFilePath = element.getElementsByTagName("path").item(0).getTextContent();

				Label label = null;
				Individual individual = null;

				// check if label exists --> else new label
				if (!this.labels.containsKey(labelID))
					this.labels.put(labelID, new Label(labelID));
				label = this.labels.get(labelID);

				// check if individual exists --> else new animal
				if (!this.individuals.containsKey(individualID)) {
					this.individuals.put(individualID, new Individual(individualID, label));
					label.addIndividual(this.individuals.get(individualID));
				}
				individual = this.individuals.get(individualID);

				// check if animal is already at other label --> Error
				if (!individual.getLabel().getName().equals(labelID)) {
					System.out.println("Error: Individual " + individualID + " assigned to label "
							+ individual.getLabel().getName() + " and to label " + labelID + "!");
					System.exit(0);
				}

				// check if alignmentID exists --> Yes --> Error
				if (this.alignments.containsKey(alignmentID)) {
					System.out.println("Error: AlignmentID " + alignmentID + " defined more than once!");
					System.exit(0);
				}

				this.alignments.put(alignmentID,
						new Alignment(alignmentID, individual, bamFilePath, time, infectionState, this.genome));
				individual.addAlignment(this.alignments.get(alignmentID));

				// add time to list of times
				this.times.add(time);
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (SAXException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	/**
	 * @return the genome
	 */
	public Genome getGenome() {
		return this.genome;
	}

	/**
	 * @param infectionState the name of an infection state
	 * @return list of allignmens featuring the respective infection state
	 */
	public ArrayList<Alignment> getAlignmentsWithInfectionState(String infectionState) {
		ArrayList<Alignment> alignments = new ArrayList<Alignment>(this.alignments.size());
		for (Alignment alignment : this.alignments.values())
			if (alignment.getInfectionState().equals(infectionState))
				alignments.add(alignment);

		return alignments;
	}

	/**
	 * @return all names of assigned times
	 */
	public SortedSet<String> getTimes() {
		return this.times;
	}

	/**
	 * @param infectionState the name of an infection state
	 * @return all individuals with at least one alignment featuring the respective infection state
	 */
	public SortedSet<String> getIndividualsWithInfectionState(String infectionState) {
		TreeSet<String> individuals = new TreeSet<String>();
		for (Alignment alignment : this.alignments.values())
			if (alignment.getInfectionState().equals(infectionState))
				individuals.add(alignment.getIndividual().getID());
		return individuals;
	}

	/**
	 * @param label name of the classification label
	 * @return list of all individuals assigned to the respective label
	 */
	public SortedSet<String> getIndividualsFromLabel(String label) {
		TreeSet<String> individuals = new TreeSet<String>();

		for (Individual individual : this.labels.get(label).getIndividuals())
			individuals.add(individual.getID());
		return individuals;
	}

	/**
	 * @param label name of the classification label
	 * @return list of alignments assigned to the respective label
	 */
	public ArrayList<Alignment> getAlignmentsFromLabel(String label) {
		ArrayList<Alignment> alignments = new ArrayList<Alignment>(this.alignments.size());

		for (Individual individual : this.labels.get(label).getIndividuals())
			alignments.addAll(individual.getAlignments());
		return alignments;
	}
	
	/**
	 * @param label name of the classification label
	 * @param time name of the time
	 * @return list of alignments assigned to the respective label also featuring the specified time
	 */
	public ArrayList<Alignment> getAlignmentsFromLabelTime(String label, String time) {
		ArrayList<Alignment> alignments = new ArrayList<Alignment>(this.alignments.size());
		
		for (Individual individual : this.labels.get(label).getIndividuals())
			for(Alignment alignment : individual.getAlignments())
				if(alignment.getTime().equals(time))
					alignments.add(alignment);
		return alignments;
	}
	
	/**
	 * @param individual name of an individual
	 * @return list of alignments assigned to the specified individual
	 */
	public ArrayList<Alignment> getAlignmentsFromIndividual(String individual) {
		return this.individuals.get(individual).getAlignments();
	}
	
	/**
	 * @param individual name of an individual
	 * @param time name of the time
	 * @return list of alignments assigned to the specified individual also featuring the specified time
	 */
	public Alignment getAlignmentsFromIndividualTime(String individual, String time) {
		for(Alignment a : this.individuals.get(individual).getAlignments())
			if(a.getTime().equals(time))
				return a;
		return null;
	}
	
	/**
	 * @param id name of the alignments
	 * @return distinct alignment with the specified name
	 */
	public Alignment getAlignment(String id) {
		return this.alignments.get(id);
	}
	
	/**
	 * @return list of all alignments in the data set
	 */
	public ArrayList<Alignment> getAll() {
		ArrayList<Alignment> all = new ArrayList<Alignment>(this.alignments.size());
		all.addAll(this.alignments.values());
		return all;
	}
	
	/**
	 * @param label name of the classification label
	 * @param excluded set of excluded individual names
	 * @return list of alignments featuring the specified label which are not assigned to the excluded individuals
	 */
	public ArrayList<Alignment> getAlignmentsFromLabelExcludeIndividuals(String label, SortedSet<String> excluded) {
		ArrayList<Alignment> alignments = new ArrayList<Alignment>(this.alignments.size());
		
		for (Individual individual : this.labels.get(label).getIndividuals())
			if(!excluded.contains(individual.getID()))
				alignments.addAll(individual.getAlignments());
		return alignments;
	}
	
	/**
	 * @return a hash map (name, Individual) of all individual-objects in the data set
	 */
	public HashMap<String, Individual> getIndividuals() {
		return this.individuals;
	}
}
