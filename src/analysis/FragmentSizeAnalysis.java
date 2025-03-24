package analysis;

import java.util.ArrayList;
import java.util.TreeSet;

import org.jfree.chart.JFreeChart;

import data.Alignment;
import data.Region;
import util.HistogramBuilder;
import util.SVGBuilder;

/**
 * provides functionality to analyze and plot mapped fragment sizes 
 * @author Stefan Grabuschnig
 *
 */
public class FragmentSizeAnalysis {
	
	/**
	 * @param regions list of regions
	 * @param alignments list of alignments
	 * @return a histogram for sizes of fragment mapped to specified regions
	 */
	public static JFreeChart analyzeFragmentSize(ArrayList<Region> regions, ArrayList<Alignment> alignments) {

		TreeSet<String> chromosomes = new TreeSet<String>();
		ArrayList<Integer> sizes = new ArrayList<Integer>(1000000);

		for (Region region : regions)
			chromosomes.add(region.getChromosome());

		for (String chromosome : chromosomes)
			for (Alignment alignment : alignments) {
				int[] fragments = alignment.getFragments(chromosome);

				for (int i = 0; i < fragments.length; i += 2)
					for (Region region : regions)
						if (region.getChromosome().equals(chromosome))
							if ((fragments[i] >= region.getStart() && fragments[i] <= region.getStop())
									|| (fragments[i + 1] >= region.getStart() && fragments[i + 1] <= region.getStop())
									|| (fragments[i] <= region.getStart() && fragments[i + 1] >= region.getStop())) {
								sizes.add(Integer.valueOf(fragments[i + 1] - fragments[i] + 1));
								break;
							}
			}

		double[] values = new double[sizes.size()];

		for (int i = 0; i < values.length; i++)
			values[i] = sizes.get(i).doubleValue();

		return HistogramBuilder.createSizeHistogram(values, 100, true);
	}
	
	/**
	 * Creates a histogram of fragment sizes mapped to a set of alignments. Saves it as SVG
	 * @param title name of the svg file
	 * @param alignments list of alignments
	 */
	public static void plotFragmentSize(String title, ArrayList<Alignment> alignments) {		
		ArrayList<Integer> sizes = new ArrayList<Integer>(500000000);

		for (String chromosome : alignments.get(0).getGenome().getChromosomeNames())
			for (Alignment alignment : alignments) {
				int[] fragments = alignment.getFragments(chromosome);

				for (int i = 0; i < fragments.length; i += 2)
					sizes.add(Integer.valueOf(fragments[i + 1] - fragments[i] + 1));
			}

		System.out.println(sizes.size());

		double[] values = new double[sizes.size()];

		System.out.println("prepare values");

		for (int i = 0; i < values.length; i++)
			values[i] = sizes.get(i).doubleValue();

		System.out.println("Try to build histograms");
		SVGBuilder.saveSVG(HistogramBuilder.createSizeHistogram(values, 100, true), title);

	}
	
	/**
	 * Creates a histogram of fragment sizes mapped to distinct regions in a set of alignments. Saves it as SVG
	 * @param title name of the svg
	 * @param regions list of regions
	 * @param alignments list of alignments
	 */
	public static void plotFragmentSize(String title, ArrayList<Region> regions, ArrayList<Alignment> alignments) {
		SVGBuilder.saveSVG(FragmentSizeAnalysis.analyzeFragmentSize(regions, alignments), title);
	}
}
