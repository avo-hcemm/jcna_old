package analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import data.Alignment;
import data.Genome;
import data.RepeatMasker;

/**
 * Provides functionality to analyze the composition in terms of repeat families of sam/bam alignment data
 * @author Stefan Grabuschnig
 *
 */
public class CompositionAnalysis_2 {
	/**
	 * @param alignments list of alignments
	 * @param repeatFamilies names of considered repeat families
	 * @return a hashmap containing compositions of all alignments as double arrays
	 */
	public static LinkedHashMap<String, ArrayList<double[]>> calculateCompositions(
			LinkedHashMap<String, ArrayList<Alignment>> alignments, LinkedList<String> repeatFamilies) {

		Genome genome = alignments.get(alignments.keySet().iterator().next()).get(0).getGenome();

		// Key: it is a sample
		// al: it is a list containing double[] elements corresponding to each key.
		// double[]: of the size of the repeat families 
		// results: each sample has a list of double[] for each alignment contained in the sample. 
		LinkedHashMap<String, ArrayList<double[]>> results = new LinkedHashMap<String, ArrayList<double[]>>();
		for (String key : alignments.keySet()) {
			ArrayList<double[]> al = new ArrayList<double[]>(alignments.get(key).size());
			for (int i = 0; i < alignments.get(key).size(); i++)
				al.add(new double[repeatFamilies.size() + 2]);
			results.put(key, al);
		}

		// Loop over all chromosomes
		// check for the chromosome names
		for (String chromosome : genome.getChromosomeNames()) {
			if (chromosome.equals("chrY") || chromosome.equals("chrM"))
				continue;

			// create repeat masks
			// repeatMasks[i][j] is a 2D jagged array containing i repeat families per row. Each repeat family has a mask of length j that can be different
			int[][] repeatMasks = new int[repeatFamilies.size() + 1][]; // repeat families + other repeats

			for (int i = 0; i < repeatFamilies.size(); i++) {
				int[] mask = RepeatMasker.getRepeatFamilyMask(genome, chromosome, repeatFamilies.get(i));
				repeatMasks[i] = mask;
			}

			// Other repeats
			//initialization of the repeatMaskOther vector: the initial repeatMaskOther vector contains "1" for all the repeat families, 
			//then it checks where the annotated repeat families are and fill these positions with "0"
			int[] repeatMaskOther = RepeatMasker.getRepeatMask(genome, chromosome);
			for (int i = 0; i < repeatMasks.length - 1; i++)
				for (int pos = 0; pos < repeatMaskOther.length; pos++)
					if (repeatMasks[i][pos] == 1)
						repeatMaskOther[pos] = 0;

			repeatMasks[repeatMasks.length - 1] = repeatMaskOther;

			// Loop over samples
			// each alignmentID is stored in the alignments list under 'key'
			for (String key : alignments.keySet()) {
				// looping through each sample or AlignmentID
				for (int alignmentIndex = 0; alignmentIndex < alignments.get(key).size(); alignmentIndex++) {
					// The absoluteCoverage vector stores the count data (only fragments if the flag is True) for that specified chromosome
					// identified by the alignment 'alignmentindex'
					int[] absoluteCoverage = alignments.get(key).get(alignmentIndex).getAbsoluteCoverage(chromosome);
					//the absolute coverage is an integer array corresponding to each repeat family
					for (int pos = 0; pos < absoluteCoverage.length; pos++) {
						int match = 0;
						// checking for overlapping repeat families at the coordinate position 'pos'
						for (int[] mask : repeatMasks)
							match += mask[pos];
						//if there is there is no REPEAT FAMILY of interest in that particular position 'pos'
						//it simply records the absolute coverage in the 'OTHER REPEATS' class
						if (match == 0 )
							results.get(key).get(alignmentIndex)[repeatFamilies.size() + 1] += absoluteCoverage[pos];
						//if there is there is 1 REPEAT FAMILY of interest in that particular position 'pos'
						else if (match == 1)
							for (int i = 0; i < repeatMasks.length; i++)
								if (repeatMasks[i][pos] == 1)
								results.get(key).get(alignmentIndex)[i] += absoluteCoverage[pos];
						else
						// if there is overlap, it devides the absolute coverage by the counting of matches
							for (int j = 0; j < repeatMasks.length; j++)
								if (repeatMasks[j][pos] == 1)
									results.get(key).get(alignmentIndex)[i] += (double) 0.0;
									// results.get(key).get(alignmentIndex)[i] += (absoluteCoverage[pos] / (double) match);
					}
				}
			}
		}

		for (ArrayList<double[]> res : results.values()) {
			for (double[] r : res) {
				double sum = 0.0d;
				for (int i = 0; i < r.length; i++)
					sum += r[i];

				for (int i = 0; i < r.length; i++)
					r[i] /= sum;
			}

		}
		return results;
	}

	/**
	 * @param repeatFamilies names of considered repeat families
	 * @param genome the respective genome
	 * @return a double array containing the genomic abundances of the specified repeat families
	 */
	public static double[] calculateRepeatAbundances(LinkedList<String> repeatFamilies, Genome genome) {
		double[] repeatAbundances = new double[repeatFamilies.size() + 2];

		for (String chromosome : genome.getChromosomeNames()) {
			if (chromosome.equals("chrY") || chromosome.equals("chrM"))
				continue;

			// create repeat masks
			int[][] repeatMasks = new int[repeatFamilies.size() + 1][]; // repeat families + other repeats

			for (int i = 0; i < repeatFamilies.size(); i++) {
				int[] mask = RepeatMasker.getRepeatFamilyMask(genome, chromosome, repeatFamilies.get(i));
				repeatMasks[i] = mask;
			}

			// Other repeats
			int[] repeatMaskOther = RepeatMasker.getRepeatMask(genome, chromosome);
			for (int i = 0; i < repeatMasks.length - 1; i++)
				for (int pos = 0; pos < repeatMaskOther.length; pos++)
					if (repeatMasks[i][pos] == 1)
						repeatMaskOther[pos] = 0;

			repeatMasks[repeatMasks.length - 1] = repeatMaskOther;

			for (int pos = 0; pos < genome.getChromosomeSize(chromosome); pos++) {
				int match = 0;
				for (int[] mask : repeatMasks)
					match += mask[pos];
				if (match == 0)
					repeatAbundances[repeatAbundances.length - 1] += 1.0d;
				else
					for (int i = 0; i < repeatMasks.length; i++)
						if (repeatMasks[i][pos] == 1)
							repeatAbundances[i] += (1.0d / (double) match);
			}
		}

		double sum = 0.0d;
		for (int i = 0; i < repeatAbundances.length; i++)
			sum += repeatAbundances[i];
		for (int i = 0; i < repeatAbundances.length; i++)
			repeatAbundances[i] /= sum;

		return repeatAbundances;
	}
	
	/**
	 * Calculates coverage levels relative to a selected reference from compositions
	 * @param compositions list of compositions as double arrays
	 * @param repeatAbundances double array containing the genomic abundances of the considered repeat families
	 * @param referenceIndex index of the reference repeat familiy (or non repetetive)
	 * @return reference based coverages for all specified compositions
	 */
	// this method puts into relation the calculated coverages from the previous two methods (calculateRepeatAbundances)
	public static ArrayList<double[]> calculateReferenceBasedCoverageLevels(ArrayList<double[]> compositions, double[] repeatAbundances, int referenceIndex) {
		ArrayList<double[]> referenceBasedCoverageLevels = new ArrayList<double[]>(compositions.size());
		
		for(double[] composition : compositions) {
			double[] referenceBasedCoverage = new double[composition.length];
			
			for(int i = 0; i < composition.length; i++)
				referenceBasedCoverage[i] = (composition[i]/repeatAbundances[i])/(composition[referenceIndex]/repeatAbundances[referenceIndex]);
			
			referenceBasedCoverageLevels.add(referenceBasedCoverage);
		}
		return referenceBasedCoverageLevels;
	}

	/**
	 * @param alignments list of alignments
	 * @return the contents of mitochondrial DNA for all alignments
	 */
	public static double[] evaluateMitoContent(ArrayList<Alignment> alignments) {
		
		double[] mitoContents = new double[alignments.size()];
		
		for(int i = 0; i < alignments.size(); i++)
			mitoContents[i] = CompositionAnalysis_2.calculatMitoContent(alignments.get(i));

		return mitoContents;
	}

	/**
	 * @param alignment an alignment
	 * @return the content of mitochondrial DNA in the specified alignment
	 */
	private static double calculatMitoContent(Alignment alignment) {

		double mappedNtsTotal = 0;
		double mappedNtsMito = 0;

		for (String chromosome : alignment.getGenome().getChromosomeNames())
			if (chromosome.equals("chrY"))
				continue;
			else {
				double mappedNts = 0;
				int[] absoluteCoverage = alignment.getAbsoluteCoverage(chromosome);

				for (int pos = 0; pos < absoluteCoverage.length; pos++)
					mappedNts += absoluteCoverage[pos];

				if (chromosome.equals("chrM"))
					mappedNtsMito = mappedNts;

				mappedNtsTotal += mappedNts;
			}

		return mappedNtsMito / mappedNtsTotal;
	}
	
	//
	/**
	 * Created a bar-chart illustration of the average composition of the data
	 * @deprecated
	 * @param data labeled sets of compositions
	 * @param repeatFamilies names of the repeat families
	 * @param repeatAbundances double array containing the abundances of the considered repeat families
	 * @param monochrome false for usage of colors
	 */
	// public static void createChart(LinkedHashMap<String, double[]> data, LinkedList<String> repeatFamilies,
	// 		double[] repeatAbundances, boolean monochrome) {

	// 	Paint[] paintings = null;
	// 	if (monochrome) {
	// 		paintings = new Paint[repeatFamilies.size() + 2];
	// 		for (int i = 0; i < paintings.length; i++)
	// 			paintings[i] = new Color(255 / (paintings.length - 1) * i, 255 / (paintings.length - 1) * i,
	// 					255 / (paintings.length - 1) * i);
	// 	}

	// 	DefaultCategoryDataset dataset = new DefaultCategoryDataset();

	// 	// Abundance Bar
	// 	dataset.addValue(repeatAbundances[repeatAbundances.length - 1] * 100, "Non repetetive", "Genome");
	// 	dataset.addValue(repeatAbundances[repeatAbundances.length - 2] * 100, "Other Repeats", "Genome");

	// 	for (int i = repeatFamilies.size() - 1; i >= 0; i--)
	// 		dataset.addValue(repeatAbundances[i] * 100, repeatFamilies.get(i), "Genome");

	// 	// CNA composition bars

	// 	for (String key : data.keySet()) {
	// 		dataset.addValue(data.get(key)[data.get(key).length - 1] * 100, "Non repetetive", "CNA " + key);
	// 		dataset.addValue(data.get(key)[data.get(key).length - 2] * 100, "Other Repeats", "CNA " + key);

	// 		for (int i = repeatFamilies.size() - 1; i >= 0; i--)
	// 			dataset.addValue(data.get(key)[i] * 100, repeatFamilies.get(i), "CNA " + key);
	// 	}

	// 	final JFreeChart chart = ChartFactory.createStackedBarChart("", // chart title
	// 			"Category", // domain axis label
	// 			"Coverage (%)", // range axis label
	// 			dataset, // data
	// 			PlotOrientation.VERTICAL, // the plot orientation
	// 			true, // legend
	// 			false, // tooltips
	// 			false // urls
	// 	);

	// 	chart.getPlot().setBackgroundPaint(Color.white);
	// 	chart.getPlot().setOutlinePaint(Color.black);
	// 	chart.getLegend().setItemFont(new Font("Dialog", Font.PLAIN, 34)); // 22
	// 	chart.getCategoryPlot().getRangeAxis().setLowerBound(0.0);
	// 	chart.getCategoryPlot().getRangeAxis().setUpperBound(100.0);
	// 	chart.getCategoryPlot().getDomainAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 30)); // 18
	// 	chart.getCategoryPlot().getRangeAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 30)); // 18

	// 	chart.setPadding(new RectangleInsets(20, 20, 20, 20));

	// 	StackedBarRenderer renderer = new StackedBarRenderer();
	// 	renderer.setBarPainter(new StandardBarPainter());
	// 	renderer.setGradientPaintTransformer(null);
	// 	renderer.setBaseLegendShape(new Rectangle(30, 30));
	// 	renderer.setShadowVisible(false);
	// 	renderer.setDrawBarOutline(true);
	// 	renderer.setMaximumBarWidth(0.15);

	// 	Stroke s = new BasicStroke(4);

	// 	for (int i = 0; i < repeatFamilies.size() + 2; i++) {
	// 		renderer.setSeriesOutlineStroke(i, s);
	// 		renderer.setSeriesOutlinePaint(i, Color.black);
	// 	}

	// 	if (paintings != null) {
	// 		renderer.setSeriesPaint(0, paintings[0]);
	// 		renderer.setSeriesPaint(1, paintings[1]);

	// 		for (int i = 0; i < repeatFamilies.size(); i++)
	// 			renderer.setSeriesPaint(i + 2, paintings[i + 2]);
	// 	}

	// 	CategoryPlot plot = (CategoryPlot) chart.getPlot();
	// 	plot.setOutlineVisible(false);

	// 	LegendTitle legend = chart.getLegend();
	// 	legend.setPosition(RectangleEdge.RIGHT);
	// 	legend.setBorder(0, 0, 0, 0);

	// 	plot.setShadowGenerator(null);
	// 	plot.getDomainAxis().setLabelFont(new Font("Dialog", Font.PLAIN, 32));
	// 	plot.getRangeAxis().setLabelFont(new Font("Dialog", Font.PLAIN, 32));
	// 	plot.getDomainAxis().setLabelPaint(Color.BLACK);
	// 	plot.getRangeAxis().setLabelPaint(Color.BLACK);

	// 	plot.getDomainAxis().setAxisLineStroke(new BasicStroke(4));
	// 	plot.getDomainAxis().setTickMarkStroke(new BasicStroke(4));
	// 	plot.getDomainAxis().setAxisLinePaint(Color.BLACK);
	// 	plot.getDomainAxis().setTickMarkPaint(Color.BLACK);

	// 	plot.getRangeAxis().setAxisLineStroke(new BasicStroke(4));
	// 	plot.getRangeAxis().setTickMarkStroke(new BasicStroke(4));
	// 	plot.getRangeAxis().setAxisLinePaint(Color.BLACK);
	// 	plot.getRangeAxis().setTickMarkPaint(Color.BLACK);

	// 	plot.setRenderer(renderer);

	// 	BufferedOutputStream bos;
	// 	try {
	// 		bos = new BufferedOutputStream(new FileOutputStream("CNAcomposition.png"));
	// 		ImageIO.write(chart.createBufferedImage(1920, 1080), "PNG", bos);
	// 		bos.close();
	// 	} catch (FileNotFoundException e) {
	// 		System.out.println("Error while saving plot");
	// 		e.printStackTrace();
	// 		System.exit(0);
	// 	} catch (IOException e) {
	// 		System.out.println("Error while saving plot");
	// 		e.printStackTrace();
	// 		System.exit(0);
	// 	}
	// }

	/**
	 * reads composition data from a textfile
	 * @deprecated
	 * @param label label of the data (must be equal to filename)
	 * @return list of double arrays containing the composition data
	 */
	// public static ArrayList<double[]> readCompositionsFile(String label) {
	// 	ArrayList<double[]> compositions = new ArrayList<double[]>(100);
	// 	BufferedReader br;
	// 	try {
	// 		br = new BufferedReader(new FileReader("Compositions_" + label + ".txt"));

	// 		String line = null;
	// 		int count = 0;
	// 		int numEl = 0;
	// 		while ((line = br.readLine()) != null) {
	// 			count++;
	// 			StringTokenizer tokenizer = new StringTokenizer(line, "\t");
	// 			if (count == 1) {
	// 				numEl = tokenizer.countTokens();
	// 				continue;
	// 			}

	// 			double[] comp = new double[numEl];
	// 			int i = 0;
	// 			while (tokenizer.hasMoreTokens()) {
	// 				comp[i] = Double.parseDouble(tokenizer.nextToken());
	// 				i++;
	// 			}
	// 			compositions.add(comp);
	// 		}

	// 	} catch (FileNotFoundException e) {
	// 		e.printStackTrace();
	// 		System.exit(0);
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 		System.exit(0);
	// 	}

	// 	return compositions;
	// }
	
	/**
	 * Reads repeat abundances from a textfile RepeatAbundances.txt
	 * @deprecated
	 * @return double array containing repeat abundances
	 */
	// public static double[] readRepeatAbundanceFile() {
	// 	double[] repeatAbundances = null;
	// 	BufferedReader br;
	// 	try {
	// 		br = new BufferedReader(new FileReader("RepeatAbundances.txt"));

	// 		String line = null;
	// 		br.readLine();
	// 		if ((line = br.readLine()) != null) {
	// 			StringTokenizer tokenizer = new StringTokenizer(line, "\t");
	// 			repeatAbundances = new double[tokenizer.countTokens()];

	// 			int i = 0;
	// 			while (tokenizer.hasMoreTokens()) {
	// 				repeatAbundances[i] = Double.parseDouble(tokenizer.nextToken());
	// 				i++;
	// 			}
	// 		}

	// 	} catch (FileNotFoundException e) {
	// 		e.printStackTrace();
	// 		System.exit(0);
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 		System.exit(0);
	// 	}
	// 	return repeatAbundances;
	// }
}
