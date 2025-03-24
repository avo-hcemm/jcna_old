package analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
//additional libraries
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;


import data.Alignment;
import data.DataSet;
import data.Region;
import data.RepeatMasker;

//date
import java.time.LocalDateTime;  
import java.time.format.DateTimeFormatter;  

/**
 * Main class for performing analyses. Define your desired analysis in the body of the main method using the functionality in the CompositionAnalysis, CoverageAnalysis and FragmentsSizeAnalysis classes.
 * Provides some functionality to store data.
 * @author Stefan Grabuschnig
 */
public class CNA_Analysis {

	public static void main(String[] args) {
		// __________________________________________________________________________________________________________________________________________________
		// Load dataset

		DataSet dataSet = new DataSet("master/human_covid.xml");

		// __________________________________________________________________________________________________________________________________________________
		// Define the set of genomic repeat families
		LinkedList<String> repeatFamilies = new LinkedList<String>();
		LinkedList<String> tempList = new LinkedList<String>();

	
		Scanner sc = new Scanner( System.in );

		// Ask the user
		System.out.print( "Would you like to analyse all the annotated repeat families? (y/n): \n" );
		String answer = sc.nextLine();
		if(answer.equalsIgnoreCase("y"))
		{
			repeatFamilies = RepeatMasker.getRepeatFamilies();
		}
		else
		{
			// store the annotated repeat families in a temporary List
			tempList = RepeatMasker.getRepeatFamilies();
			// Ask the user
			System.out.print( "Type the repeat families one by one on a new line. Type 'stop' at the end: \n" );
			//  Use the Scanner to read a line of text from the user.
			String input = sc.nextLine();
			// add the user input to the repeat families followed by 'stop'
			while(!input.equalsIgnoreCase("stop"))
			{
					//checking that the inserted repeat families from the user input are typed correctly!
			boolean isExists = tempList.contains(input);
		        if(isExists)
		        {
		            //System.out.println("The family " + input+ " exists in the annnotation database");
		            //checking that the inserted repeat family has not already been added!
		            if(repeatFamilies.contains(input))
		            {
		            	System.out.println("The family " + input+ " is already added. Please add a new one or type 'stop'");
		            	input = sc.nextLine();
		            }
		            else
		            {
						repeatFamilies.add(input);
						input = sc.nextLine();
		            }
		        }
		        else
		        {
		            System.out.println("The family " + input+ " does not exist in the annnotation database. Please, try it again.");
		            input = sc.nextLine();
		        }
					//
			}
		}
		
		System.out.println("Summary of the repeat families: ");
		for(String rf : repeatFamilies)
			System.out.print(rf + "\t ");
		System.out.println();

		// __________________________________________________________________________________________________________________________________________________
		// Define data for analysis

		LinkedHashMap<String, ArrayList<Alignment>> data = new LinkedHashMap<String, ArrayList<Alignment>>();

		// human
		ArrayList<Alignment> healthy = dataSet.getAlignmentsFromLabel("healthy");
		ArrayList<Alignment> cases = dataSet.getAlignmentsFromLabel("covid");

		data.put("healthy", healthy);	
		data.put("covid", cases);

		// __________________________________________________________________________________________________________________________________________________
		// Define analysis to perform

		CoverageAnalysis coverageAnalysis = new CoverageAnalysis();
//		coverageAnalysis.evaluateGenomeCoverage(cases, 100.0, 100);
//		coverageAnalysis.evaluateSortedRegions(cases, 100.0, 100, 200);
		
		/*		Plot my biomqrker regions		*/
		ArrayList<Region> regions = new ArrayList<Region>(10);
//		Region reg_12_2 = new Region("chr12",97430942,97431147); //max600
//		Region reg_12_1 = new Region("chr12",27222463,27222820);
//		Region reg_8_1 = new Region("chr8",2218482,2218741);
//		Region reg_3_1 = new Region("chr3",198015839,198015966);
//		Region reg_2_1 = new Region("chr2",86884426,86884805);
//		Region reg_14_1 = new Region("chr14",22801708,22802172);
		Region reg_x_1 = new Region("chrX",32678185,32678531);//max2000
		Region reg_3_2 = new Region("chr3",42452376,42452649);
		Region reg_12_3 = new Region("chr12",47326181,47327280);
		Region reg_2_2 = new Region("chr2",232799457,232799824);
		Region reg_2_3 = new Region("chr2",28846734,28847170);
		Region reg_17_1 = new Region("chr17",35451390,35451973);
		
//		regions.add(reg_16_1);
//		regions.add(reg_12_2);
//		regions.add(reg_12_1);
//		regions.add(reg_8_1);
//		regions.add(reg_3_1);
//		regions.add(reg_2_1);
//		regions.add(reg_14_1);
		regions.add(reg_12_3);
		regions.add(reg_x_1);
		regions.add(reg_17_1);
		regions.add(reg_2_3);
		regions.add(reg_2_2);
		regions.add(reg_3_2);
		
//		coverageAnalysis.plotAll(regions, cases);
//		coverageAnalysis.plotAll(regions, healthy, "healthy");
		coverageAnalysis.plotMultipleCohorts(regions, cases,"disease-severe",healthy, "healthy");
		
		/**		***		**/
		//it calculates the abundance of these repeat regions
		// the linkdHashMap compositions contains the compositions of all the dogs relatively to the repeat families
		LinkedHashMap<String, ArrayList<double[]>> compositions = CompositionAnalysis.calculateCompositions(data,repeatFamilies);
		double[] repeatAbundances = CompositionAnalysis.calculateRepeatAbundances(repeatFamilies, dataSet.getGenome());
		
		ArrayList<double[]> referenceBasedCoverageLevels_covid = CompositionAnalysis.calculateReferenceBasedCoverageLevels(compositions.get("covid"), repeatAbundances, repeatAbundances.length - 1);
		/*ArrayList<double[]> referenceBasedCoverageLevelsPanc = CompositionAnalysis.calculateReferenceBasedCoverageLevels(compositions.get("pancreatitis"), repeatAbundances, repeatAbundances.length - 1);*/

		// __________________________________________________________________________________________________________________________________________________
		// store data
		
		//filepath
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatDateTime = LocalDateTime.now().format(formatter);
        String[] pathNameDate1 = formatDateTime.split(" ",2);
        String pathNameDate2 = pathNameDate1[0]+"_"+pathNameDate1[1];
        String[] pathNameDate3 = pathNameDate2.split(":",3);
        String finalPathName = pathNameDate3[0]+"-"+pathNameDate3[1]+"-"+pathNameDate3[2];

		//compositions of repeat families
		//it loads the data for healthy and disease group separately
		saveData(compositions.get("covid"), repeatFamilies, finalPathName,"Compositions_disease-severe");
		/*saveData(compositions.get("pancreatitis"), repeatFamilies, "Compositions_canPanc");*/
		
		//repeat abundances
		//the genomic abundance of the repeat families
		saveData(repeatAbundances, repeatFamilies, finalPathName, "RepeatAbundancesHumans");
/*		saveData(repeatAbundances, repeatFamilies, "RepeatAbundancesDogs");*/
		
		//reference based coverages
		// the relative abundance of the cfDNA samples relatively to the genomic reference for the repeat families
		saveData(referenceBasedCoverageLevels_covid, repeatFamilies, finalPathName, "ReferenceBasedCoverageLevels_covid");
/*		saveData(referenceBasedCoverageLevelsPanc, repeatFamilies, "ReferenceBasedCoverageLevels_canpPanc");*/

	}

	/**
	 * Saves double array data (i.e. compositions or reference based coverage levels) to a textfile.
	 * @param data list of douable arrays containing the data
	 * @param repeatFamilies list of repeat families (header)
	 * @param fileName name of the file
	 */
	public static void saveData(ArrayList<double[]> data, LinkedList<String> repeatFamilies, String pathName, String fileName) {
		Path path = Paths.get("healthy/"+pathName+"/");
		try {
			// changing directory with current date
			if(Files.exists(path))
			{
			System.out.println("The file '"+fileName+"' is in the output folder: "+path);
			}
			else 
			{
  			Files.createDirectory(path);
  			System.out.println("New path created: "+path);
  			System.out.println("The file '"+fileName+"' is in the output folder: "+path);
			}
			BufferedWriter bwr = new BufferedWriter(new FileWriter(path +"/"+ fileName + ".txt"));
			// data header
			for (int i = 0; i < repeatFamilies.size(); i++)
				bwr.write(repeatFamilies.get(i) + "\t");

			bwr.write("Other repeats\t");
			bwr.write("Non repetetive");
			bwr.newLine();

			// data
			for (double[] vector : data) {
				for (int i = 0; i < vector.length - 1; i++)
					bwr.write(vector[i] + "\t");
				bwr.write("" + vector[vector.length - 1]);
				bwr.newLine();
			}

			bwr.flush();
			bwr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Saves a single double array (i.e. repeat abundances) to a textfile.
	 * @param vector double array
	 * @param repeatFamilies list of repeat families (header)
	 * @param fileName name of the file
	 */
	public static void saveData(double[] vector, LinkedList<String> repeatFamilies, String pathName, String fileName) {
		Path path = Paths.get("healthy/"+pathName+"/");
		try {
			// changing directory with current date
			if(Files.exists(path))
			{
			System.out.println("The file '"+fileName+"' is in the output folder: "+path);
			}
			else 
			{
  			Files.createDirectory(path);
  			System.out.println("New path created: "+path);
  			System.out.println("The file '"+fileName+"' is in the output folder: "+path);
			}
			BufferedWriter bwr = new BufferedWriter(new FileWriter(path +"/"+ fileName + ".txt"));
			// data header
			for (int i = 0; i < repeatFamilies.size(); i++)
				bwr.write(repeatFamilies.get(i) + "\t");

			bwr.write("Other repeats\t");
			bwr.write("Non repetetive");
			bwr.newLine();

			// data
			for (int i = 0; i < vector.length - 1; i++)
				bwr.write(vector[i] + "\t");
			bwr.write("" + vector[vector.length - 1]);
			bwr.newLine();

			bwr.flush();
			bwr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param values list of double arrays
	 * @return arithmetic mean of double arrays
	 */
	public static double[] calculateMean(ArrayList<double[]> values) {
		double[] mean = new double[values.get(0).length];

		for (double[] value : values)
			for (int i = 0; i < value.length; i++)
				mean[i] += value[i];

		for (int i = 0; i < mean.length; i++)
			mean[i] /= values.size();

		return mean;
	}
	
	/**
	 * @deprecated
	 * Creates a textfile with a table of average compositions for different labels
	 * @param data Hashmap of labeled average compositions
	 * @param repeatFams names of considered repeat families
	 * @param repeatAbundances double array with genomic abundances of repeat families
	 */
	/*public static void createTable(LinkedHashMap<String, double[]> data, LinkedList<String> repeatFams,
			double[] repeatAbundances) {

		LinkedList<String> repeatFamilies = new LinkedList<String>();
		repeatFamilies.addAll(repeatFams);
		repeatFamilies.add("Other Repeats");
		repeatFamilies.add("Non repetetive");

		// headline

		DecimalFormat fsc = new DecimalFormat("0.00E0");
		DecimalFormat f = new DecimalFormat("0.00");

		String output = "Repeat Family:\tGenome:";
		for (String key : data.keySet())
			output += "\t" + key + ":" + "\tOverrep:";
		output += System.lineSeparator();

		for (int i = 0; i < repeatAbundances.length; i++) {
			output += repeatFamilies.get(i) + ":";

			if (100.0d * repeatAbundances[i] < 0.01)
				output += "\t" + fsc.format(100.0d * repeatAbundances[i]) + "%";
			else
				output += "\t" + f.format(100.0d * repeatAbundances[i]) + "%";

			for (String key : data.keySet())
				if (100.0d * data.get(key)[i] < 0.01)
					output += "\t" + fsc.format(100.0d * data.get(key)[i]) + "%\t"
							+ f.format(100.0d * data.get(key)[i] / repeatAbundances[i]) + "%";
				else
					output += "\t" + f.format(100.0d * data.get(key)[i]) + "%\t"
							+ f.format(100.0d * data.get(key)[i] / repeatAbundances[i]) + "%";
			output += System.lineSeparator();
		}

		output = output.replaceAll("\\.", ",");

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("CNAcomposition_canPanc.txt"));
			writer.write(output);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Can't write output to textfile");
			e.printStackTrace();
		}*/
	}
// }
