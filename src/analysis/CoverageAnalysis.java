	/*package cna.analysis;*/
package analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import config.Config;
import data.Alignment;
import data.Annotation;
import data.Genome;
import data.Gene;
import data.Region;
import data.RepeatMasker;
import util.Statistics;
import data.Repeat;
import parallel.ParallelArrayAdder;
import util.HistogramBuilder;
import util.SVGBuilder;

/**
 * Provides functionality to analyze coverage data of alignments and create
 * coverage plots
 * 
 * @author Stefan Grabuschnig
 *
 */
public class CoverageAnalysis {

	/**
	 * evaluates and prints coverage parameters for the average coverage of a set of
	 * alignments. Distinguishes between covered und uncovered (noisy) regions based
	 * on a threshold
	 * 
	 * @param alignments
	 *            list of alignments
	 * @param threshold
	 *            threshold for coverage interpretation
	 * @param minSize
	 *            minimum size for a region classified as covered
	 */
	public static void evaluateGenomeCoverage(ArrayList<Alignment> alignments, double threshold, int minSize) {
		ArrayList<Region> regions = CoverageAnalysis.getCoveredRegions(alignments, threshold, minSize,
				Integer.MAX_VALUE);

		double genomeSize = 0;
		double covered = 0;
		for (String chromosome : alignments.get(0).getGenome().getChromosomeNames())
			if (!(chromosome.equals("Y") || chromosome.equals("M")))
				genomeSize += alignments.get(0).getGenome().getChromosomeSize(chromosome);

		for (Region r : regions)
			covered += r.getSize();

		Collections.sort(regions);

		System.out.println("Regions: " + regions.size());
		System.out.println("Largest region: " + regions.get(regions.size() - 1).getSize());
		System.out.println("Genomesize: " + genomeSize);
		System.out.println("Covered: " + covered);
		System.out.println("Percentage: " + (100.0 * covered / genomeSize));
	}

	/**
	 * Creates a histogram of the sizes of region classified as covered (average
	 * coverage of alignments)
	 * 
	 * @param alignments
	 *            list of alignments
	 * @param threshold
	 *            threshold for coverage interpretation
	 * @param minSize
	 *            minimum size for a region classified as covered
	 * @param maxSize
	 *            maximum size for a region classified as covered (sets the scope of
	 *            the histogram)
	 */
	public static void evaluateRegionSizes(ArrayList<Alignment> alignments, double threshold, int minSize,
			int maxSize) {

		ArrayList<Region> regions = CoverageAnalysis.getCoveredRegions(alignments, threshold, minSize, maxSize);

		double[] values = new double[regions.size()];

		for (int i = 0; i < regions.size(); i++)
			values[i] = regions.get(i).getSize();

		SVGBuilder.saveSVG(HistogramBuilder.createSizeHistogram(values, 100, false), "RegionSizes");
	}

	/**
	 * Creates histograms for the sizes of regions classified as covered (average
	 * coverage of alignments) belonging to specified repeat families (one histogram
	 * per family)
	 * 
	 * @param repeatFamilies
	 *            names of the considered repeat families
	 * @param alignments
	 *            list of alignments
	 * @param threshold
	 *            threshold for coverage interpretation
	 * @param minSize
	 *            minimum size for a region classified as covered
	 * @param maxSize
	 *            maximum size for a region classified as covered (sets the scope of
	 *            the histogram)
	 */
	public static void evaluateRegionSizesForRepeatFamilies(LinkedList<String> repeatFamilies,
			ArrayList<Alignment> alignments, double threshold, int minSize, int maxSize) {

		Genome genome = alignments.get(0).getGenome();
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] sortedSizes = new ArrayList[repeatFamilies.size() + 1];
		ArrayList<Integer> sizesNonRepetetive = new ArrayList<Integer>(10000);

		DecimalFormat format = new DecimalFormat(".00");
		double genomeSize = 0;
		double covered = 0;
		double numRegions = 0;

		for (int i = 0; i < repeatFamilies.size(); i++)
			sortedSizes[i] = new ArrayList<Integer>(10000);
		sortedSizes[sortedSizes.length - 1] = new ArrayList<Integer>(10000);

		for (String chromosome : genome.getChromosomeNames()) {
			if (chromosome.equals("Y") || chromosome.equals("M"))
				continue;

			System.out.println("Chromosome: " + chromosome);

			genomeSize += genome.getChromosomeSize(chromosome);

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

			// sort region sizes
			ArrayList<Region> regions = CoverageAnalysis.getCoveredRegionsForChromosome(alignments, chromosome,
					threshold, minSize, maxSize);

			numRegions += regions.size();

			for (Region r : regions) {
				covered += r.getSize();
				boolean nonRepetetive = true;

				for (int i = 0; i < repeatMasks.length; i++)
					for (int pos = r.getStart(); pos <= r.getStop(); pos++)
						if (repeatMasks[i][pos] == 1) {
							sortedSizes[i].add(r.getSize());
							nonRepetetive = false;
							break;
						}

				if (nonRepetetive)
					sizesNonRepetetive.add(r.getSize());
			}
		}

		try {
			BufferedWriter bwr = new BufferedWriter(new FileWriter("regionSizeReport.txt"));

			bwr.write("Regions: " + numRegions);
			bwr.newLine();
			bwr.write("Genomesize: " + genomeSize);
			bwr.newLine();
			bwr.write("Covered: " + covered);
			bwr.newLine();
			bwr.write("Percentage: " + format.format((100.0 * covered / genomeSize)));
			bwr.newLine();
			bwr.newLine();

			// Repeat families
			for (int i = 0; i < repeatFamilies.size(); i++) {
				ArrayList<Integer> sizes = sortedSizes[i];
				Collections.sort(sizes);
				double averageSize = 0;
				double stdDev = 0;

				for (Integer size : sizes)
					averageSize += size;

				if (sizes.size() > 0)
					averageSize /= sizes.size();

				for (Integer size : sizes)
					stdDev += (size.doubleValue() - averageSize) * (size.doubleValue() - averageSize);

				if (sizes.size() > 1)
					stdDev = Math.sqrt(stdDev / (sizes.size() - 1));

				bwr.write("Repeat family: " + repeatFamilies.get(i));
				bwr.newLine();
				bwr.write("Number of regions: " + sizes.size());
				bwr.newLine();
				bwr.write("Region size: " + format.format(averageSize) + " +- " + format.format(stdDev));
				bwr.newLine();
				if (sizes.size() > 0) {
					bwr.write("Largest region: " + sizes.get(sizes.size() - 1));
					bwr.newLine();
				}
				bwr.newLine();
			}

			// Other repeats
			ArrayList<Integer> sizesOtherRepeats = sortedSizes[sortedSizes.length - 1];
			Collections.sort(sizesOtherRepeats);
			double averageSize = 0;
			double stdDev = 0;

			for (Integer size : sizesOtherRepeats)
				averageSize += size;

			if (sizesOtherRepeats.size() > 0)
				averageSize /= sizesOtherRepeats.size();

			for (Integer size : sizesOtherRepeats)
				stdDev += (size.doubleValue() - averageSize) * (size.doubleValue() - averageSize);

			if (sizesOtherRepeats.size() > 1)
				stdDev = Math.sqrt(stdDev / (sizesOtherRepeats.size() - 1));

			bwr.write("Repeat family: Other repeats");
			bwr.newLine();
			bwr.write("Number of regions: " + sizesOtherRepeats.size());
			bwr.newLine();
			bwr.write("Region size: " + format.format(averageSize) + " +- " + format.format(stdDev));
			bwr.newLine();
			if (sizesOtherRepeats.size() > 0) {
				bwr.write("Largest region: " + sizesOtherRepeats.get(sizesOtherRepeats.size() - 1));
				bwr.newLine();
			}
			bwr.newLine();

			// Non repetetive
			Collections.sort(sizesNonRepetetive);
			averageSize = 0;
			stdDev = 0;

			for (Integer size : sizesNonRepetetive)
				averageSize += size;

			if (sizesNonRepetetive.size() > 0)
				averageSize /= sizesNonRepetetive.size();

			for (Integer size : sizesNonRepetetive)
				stdDev += (size.doubleValue() - averageSize) * (size.doubleValue() - averageSize);

			if (sizesNonRepetetive.size() > 1)
				stdDev = Math.sqrt(stdDev / (sizesNonRepetetive.size() - 1));

			bwr.write("Repeat family: Non repetetive");
			bwr.newLine();
			bwr.write("Number of regions: " + sizesNonRepetetive.size());
			bwr.newLine();
			bwr.write("Region size: " + format.format(averageSize) + " +- " + format.format(stdDev));
			bwr.newLine();
			if (sizesNonRepetetive.size() > 0) {
				bwr.write("Largest region: " + sizesNonRepetetive.get(sizesNonRepetetive.size() - 1));
				bwr.newLine();
			}
			bwr.newLine();

			bwr.flush();
			bwr.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Creating histograms");

		for (int i = 0; i < repeatFamilies.size(); i++) {
			ArrayList<Integer> sizes = sortedSizes[i];
			ArrayList<Integer> sizesTrimmed = new ArrayList<Integer>(sizes.size());

			for (Integer integer : sizes)
				if (integer <= 2000)
					sizesTrimmed.add(integer);

			if (sizesTrimmed.size() > 0) {
				double[] values = new double[sizesTrimmed.size()];

				for (int d = 0; d < sizesTrimmed.size(); d++)
					values[d] = sizesTrimmed.get(d).doubleValue();

				SVGBuilder.saveSVG(HistogramBuilder.createSizeHistogram(values, 100, false), repeatFamilies.get(i));
			}
		}

		// Other repeats
		ArrayList<Integer> sizesOtherRepeats = sortedSizes[sortedSizes.length - 1];
		ArrayList<Integer> sizesTrimmed = new ArrayList<Integer>(sizesOtherRepeats.size());

		for (Integer integer : sizesOtherRepeats)
			if (integer <= 2000)
				sizesTrimmed.add(integer);

		if (sizesTrimmed.size() > 0) {
			double[] values = new double[sizesTrimmed.size()];

			for (int d = 0; d < sizesTrimmed.size(); d++)
				values[d] = sizesTrimmed.get(d).doubleValue();

			SVGBuilder.saveSVG(HistogramBuilder.createSizeHistogram(values, 100, false), "Other_repeats");
		}

		// Non repetetive
		if (sizesNonRepetetive.size() > 0) {
			sizesTrimmed = new ArrayList<Integer>(sizesOtherRepeats.size());

			for (Integer integer : sizesNonRepetetive)
				if (integer <= 2000)
					sizesTrimmed.add(integer);

			double[] values = new double[sizesTrimmed.size()];

			for (int d = 0; d < sizesTrimmed.size(); d++)
				values[d] = sizesTrimmed.get(d).doubleValue();

			SVGBuilder.saveSVG(HistogramBuilder.createSizeHistogram(values, 100, false), "Non_repetetive");
		}
	}

	/**
	 * @param alignments
	 *            list of alignments
	 * @param threshold
	 *            threshold for coverage interpretation
	 * @param minSize
	 *            minSize minimum size for a region classified as covered
	 * @param maxSize
	 *            maximum size for a region classified as covered
	 * @return a hashmap of regions interpreted as covered (average coverage of
	 *         alignments) assigned to repeat families, non repetitive regions, or
	 *         mixed (multiple repeat families).
	 */
	public static HashMap<String, ArrayList<Region>> getSortedRegions(ArrayList<Alignment> alignments, double threshold,
			int minSize, int maxSize) {
		ArrayList<Region> regions = CoverageAnalysis.getCoveredRegions(alignments, threshold, minSize, maxSize);
		HashMap<String, ArrayList<Region>> sortedRegions = new HashMap<String, ArrayList<Region>>(100);

		sortedRegions.put("mixed", new ArrayList<Region>(regions.size()));
		sortedRegions.put("nonRepeat", new ArrayList<Region>(regions.size()));

		for (Region r : regions) {
			Annotation annotation = new Annotation(r.getChromosome(), r.getStart(), r.getStop());
			TreeSet<String> repFams = new TreeSet<String>();

			for (Repeat rep : annotation.getRepeats())
				repFams.add(rep.getRepFamily());

			if (repFams.size() == 1) {
				if (!sortedRegions.containsKey(repFams.first()))
					sortedRegions.put(repFams.first(), new ArrayList<Region>(regions.size()));
				sortedRegions.get(repFams.first()).add(r);
			} else if (repFams.size() > 1)
				sortedRegions.get("mixed").add(r);
			else
				sortedRegions.get("nonRepeat").add(r);
		}
		return sortedRegions;
	}
	

	/**
	 * Creates a figure of histograms for the sizes of regions interpreted as
	 * covered (average coverage of alignments) belonging to full length intact L1
	 * repeats, L1 repeats, centr repeats and simple repeats
	 * 
	 * @param alignments
	 *            list of alignments
	 * @param threshold
	 *            threshold for coverage interpretation
	 * @param minSize
	 *            minimum size for a region classified as covered
	 * @param maxSize
	 *            maximum size for a region classified as covered (scope of
	 *            histograms)
	 */
	public static void evaluateSortedRegions(ArrayList<Alignment> alignments, double threshold, int minSize,
			int maxSize) {

		HashMap<String, ArrayList<Region>> sortedRegions = CoverageAnalysis.getSortedRegions(alignments, threshold,
				minSize, maxSize);

		for (String repFam : sortedRegions.keySet())
			System.out.println(repFam + " " + sortedRegions.get(repFam).size());

//		ArrayList<Region> L1TaRegions = RegionImporter.importRegions("L1.txt");
		
		System.out.println("Try to build histograms for Alu, L1, centr and Simple_repeat.");
		
		if(sortedRegions.containsKey("Alu")) {
			System.out.println("Sorted regions contain Alu coverage");
			SVGBuilder.saveSVG(FragmentSizeAnalysis.analyzeFragmentSize(sortedRegions.get("Alu"), alignments), "Alu coverage");
		}
		else if(sortedRegions.containsKey("L1")) {
			System.out.println("Sorted regions contain L1 coverage");
			SVGBuilder.saveSVG(FragmentSizeAnalysis.analyzeFragmentSize(sortedRegions.get("L1"), alignments), "L1 coverage");
		}
		else if(sortedRegions.containsKey("centr")) {
			System.out.println("Sorted regions contain centr coverage");
			SVGBuilder.saveSVG(FragmentSizeAnalysis.analyzeFragmentSize(sortedRegions.get("centr"), alignments), "L1 coverage");
		}
		else if(sortedRegions.containsKey("Simple_repeat")) {
			System.out.println("Sorted regions contain Simple_repeat coverage");
			SVGBuilder.saveSVG(FragmentSizeAnalysis.analyzeFragmentSize(sortedRegions.get("Simple_repeat"), alignments), "Simple_repeat coverage");
		}
		
	}

	/**
	 * @param alignments list of alignments
	 * @param threshold threshold for coverage interpretation
	 * @param minSize minimum size for a region classified as covered
	 * @param maxSize maximum size for a region classified as covered
	 * @return a list of regions interpreted as covered (average coverage of alignments)
	 */
	private static ArrayList<Region> getCoveredRegions(ArrayList<Alignment> alignments, double threshold, int minSize,
			int maxSize) {
		Genome genome = alignments.get(0).getGenome();
		System.out.println("Starting region size evaluation...");
		ArrayList<Region> regions = new ArrayList<Region>(1000000);

		double coverageTotal = 0;
		double coverageRegions = 0;

		for (String chromosome : genome.getChromosomeNames()) {
			if (chromosome.equals("Y") || chromosome.equals("M")) // exclude Y chromosome and mitochondrium
				continue;
			System.out.println("Chromosome: " + chromosome);

			float[] meanNormalizedCoverage = new float[genome.getChromosomeSize(chromosome)];

			for (Alignment alignment : alignments) {
				try {
					ParallelArrayAdder.add(meanNormalizedCoverage, alignment.getNormalizedCoverage(chromosome));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// divide through number of samples
			for (int i = 0; i < meanNormalizedCoverage.length; i++)
				meanNormalizedCoverage[i] /= alignments.size();

			// scan coverage
			int regionStart = -1;

			for (int i = 0; i < meanNormalizedCoverage.length; i++) {
				if (regionStart < 0 && meanNormalizedCoverage[i] >= threshold)
					regionStart = i;
				else if (regionStart >= 0 && meanNormalizedCoverage[i] < threshold) {
					if ((i - regionStart) >= minSize && (i - regionStart) <= maxSize)
						regions.add(new Region(chromosome, regionStart, i - 1));
					regionStart = -1;
				}
				if (regionStart >= 0)
					coverageRegions += meanNormalizedCoverage[i];
				coverageTotal += meanNormalizedCoverage[i];
			}
		}
		System.out.println((100.0 * coverageRegions / coverageTotal) + "% of total coverage attributed to regions.");

		return regions;
	}
	
	/**
	 * @param alignments list of alignments
	 * @param chromosome name of the chromosome
	 * @param threshold threshold for coverage interpretation
	 * @param minSize minimum size for a region classified as covered
	 * @param maxSize maximum size for a region classified as covered
	 * @return a list of regions interpreted as covered (average coverage of alignments) for a specified chromosome
	 */
	private static ArrayList<Region> getCoveredRegionsForChromosome(ArrayList<Alignment> alignments, String chromosome,
			double threshold, int minSize, int maxSize) {
		Genome genome = alignments.get(0).getGenome();
		ArrayList<Region> regions = new ArrayList<Region>(1000000);

		float[] meanNormalizedCoverage = new float[genome.getChromosomeSize(chromosome)];

		for (Alignment alignment : alignments) {
			try {
				ParallelArrayAdder.add(meanNormalizedCoverage, alignment.getNormalizedCoverage(chromosome));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// divide through number of samples
		for (int i = 0; i < meanNormalizedCoverage.length; i++)
			meanNormalizedCoverage[i] /= alignments.size();

		// scan coverage
		int regionStart = -1;

		for (int i = 0; i < meanNormalizedCoverage.length; i++) {
			if (regionStart < 0 && meanNormalizedCoverage[i] >= threshold)
				regionStart = i;
			else if (regionStart >= 0 && meanNormalizedCoverage[i] < threshold) {
				if ((i - regionStart) >= minSize && (i - regionStart) <= maxSize)
					regions.add(new Region(chromosome, regionStart, i - 1));
				regionStart = -1;
			}
		}
		return regions;
	}

	/**
	 * @param chromosome name of the chromosome
	 * @param start start coordinate of the region
	 * @param stop stop coordinate of the region
	 * @param alignments list of alignments
	 * @return a JFreeChart of a coverage plot for the normalized average coverage of the specified region
	 */
	public static JFreeChart plotRegion(String chromosome, int start, int stop, ArrayList<Alignment> alignments) { // one
		System.out.println("plotRegion:");																	// based
		if (start > stop) { // wrong orientation
			int x = start;
			start = stop;
			stop = x;
		}

		Annotation annotation = new Annotation(chromosome, start - 1, stop - 1); // zero based

		float[][] data = new float[alignments.size()][stop - start + 1];
		float[] mean = new float[stop - start + 1];
		float[] variance = new float[stop - start + 1];

		for (int i = 0; i < alignments.size(); i++) {
			float[] normalizedCoverage = alignments.get(i).getNormalizedCoverage(chromosome);

			for (int p = start - 1; p < stop; p++)
				data[i][p - start + 1] = normalizedCoverage[p];

			for (int p = 0; p < mean.length; p++)
				mean[p] += data[i][p];
		}

		for (int p = 0; p < mean.length; p++)
			mean[p] /= alignments.size();

		for (int i = 0; i < data.length; i++)
			for (int p = 0; p < variance.length; p++)
				variance[p] += (data[i][p] - mean[p]) * (data[i][p] - mean[p]);

		for (int p = 0; p < variance.length; p++)
			variance[p] = (float) Math.sqrt(variance[p] / alignments.size());

		// prepare dataset
		YIntervalSeries series = new YIntervalSeries("normalized coverage");
		YIntervalSeriesCollection seriesCollection = new YIntervalSeriesCollection();

		for (int p = 0; p < variance.length; p++)
			series.add(start + p, mean[p], mean[p] - variance[p], mean[p] + variance[p]);

		seriesCollection.addSeries(series);

		JFreeChart chart = ChartFactory.createXYLineChart("", "Position (bp)", "Coverage (1)", seriesCollection,
				PlotOrientation.VERTICAL, true, false, false);

		Font font = new Font("Dialog", Font.PLAIN, 52); // 35

		// chart.setTitle(new TextTitle((chromosome + ": " + start + " - " + stop), new
		// Font("Dialog", Font.BOLD, 38)));
		chart.getPlot().setBackgroundPaint(Color.white);
		chart.getPlot().setOutlinePaint(Color.black);
		chart.getLegend().setItemFont(new Font("Dialog", Font.PLAIN, 48)); // 28
		chart.getLegend().setPosition(RectangleEdge.RIGHT);

		DeviationRenderer renderer = new DeviationRenderer(true, false);

		renderer.setSeriesStroke(0, new BasicStroke((float) 6.0)); // 5.0
		renderer.setSeriesPaint(0, Color.BLUE);
		renderer.setSeriesFillPaint(0, Color.BLUE);

		renderer.setAlpha(0.15f);

		chart.getXYPlot().setRenderer(renderer);

		chart.getXYPlot().getDomainAxis().setLabelFont(font);
		chart.getXYPlot().getRangeAxis().setLabelFont(font);

		chart.getXYPlot().getRangeAxis().setAxisLineStroke(new BasicStroke((float) 4.0)); // 2.0
		chart.getXYPlot().getRangeAxis().setAxisLinePaint(Color.BLACK);
		chart.getXYPlot().getRangeAxis().setTickMarkStroke(new BasicStroke((float) 4.0));
		chart.getXYPlot().getRangeAxis().setTickMarkPaint(Color.BLACK);

		chart.getXYPlot().getDomainAxis().setAxisLineStroke(new BasicStroke((float) 4.0));
		chart.getXYPlot().getDomainAxis().setAxisLinePaint(Color.BLACK);
		chart.getXYPlot().getDomainAxis().setTickMarkStroke(new BasicStroke((float) 4.0));
		chart.getXYPlot().getDomainAxis().setTickMarkPaint(Color.BLACK);

		chart.getXYPlot().getDomainAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 48)); // 25
		chart.getXYPlot().getRangeAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 48)); // 18

		chart.getXYPlot().getRangeAxis().setRange(0, chart.getXYPlot().getRangeAxis().getRange().getUpperBound());

		chart.getXYPlot().setOutlineVisible(false);

		double yMax = chart.getXYPlot().getRangeAxis().getRange().getUpperBound();
		double offset = 0.1 * yMax;
		double y = yMax;

		LegendItemCollection legend = new LegendItemCollection();

		legend.add(new LegendItem("normalized coverage", Color.BLUE));

		if (annotation.getRepeats().size() > 0) {
			y -= offset;
			XYLineAnnotation repeatLine = new XYLineAnnotation(start, y, stop, y,
					new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0),
					Color.BLACK);
			chart.getXYPlot().addAnnotation(repeatLine);

			legend.add(new LegendItem("RepeatMasker", Color.BLACK));

			if (Config.plotDNAseClusters)
				legend.add(new LegendItem("DNAse clusters", Color.CYAN));
		}

		// draw Repeats
		for (Repeat r : annotation.getRepeats()) {
			XYLineAnnotation repeatAnnotation = new XYLineAnnotation(r.getStart() + 1, y, r.getEnd(), y,
					new BasicStroke(10.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL), Color.BLACK);
			chart.getXYPlot().addAnnotation(repeatAnnotation);
		}

		// draw DNAse hypersensitive sites
		if (Config.plotDNAseClusters) {
			if (annotation.getDNAseSensitiveSites().size() > 0)
				y -= offset;

			for (Region r : annotation.getDNAseSensitiveSites()) {
				XYLineAnnotation dnaseSensitiveSiteAnnotation = new XYLineAnnotation(r.getStart() + 1, y, r.getStop(),
						y, new BasicStroke(10.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL), Color.CYAN);
				chart.getXYPlot().addAnnotation(dnaseSensitiveSiteAnnotation);
			}
		}

		// draw genes
		for (Gene g : annotation.getGenes()) {
			y -= offset;

			int geneStart;
			int geneEnd;

			if (g.getTxStart() < g.getTxEnd()) {
				geneStart = g.getTxStart() + 1;
				geneEnd = g.getTxEnd();
			} else {
				geneStart = g.getTxEnd();
				geneEnd = g.getTxStart() + 1;
			}

			if (geneStart < start)
				geneStart = start;

			if (geneEnd > stop)
				geneEnd = stop;

			XYLineAnnotation geneLine = new XYLineAnnotation(geneStart, y, geneEnd, y,
					new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0),
					Color.GREEN);
			chart.getXYPlot().addAnnotation(geneLine);
			legend.add(new LegendItem(g.getGeneName(), Color.GREEN));

			// draw exons
			for (int[] exon : g.getExons()) {
				XYLineAnnotation repeatAnnotation = new XYLineAnnotation(exon[0] + 1, y, exon[1], y,
						new BasicStroke(10.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL), Color.GREEN);
				chart.getXYPlot().addAnnotation(repeatAnnotation);
			}
		}

		chart.getXYPlot().setFixedLegendItems(legend);
		chart.setPadding(new RectangleInsets(20, 20, 20, 20));
		chart.setBorderVisible(true);
		chart.setBorderStroke(new BasicStroke((float) 4.0));
		chart.setBorderPaint(Color.BLACK);

		if (!Config.showLegend)
			chart.removeLegend();

		return chart;
	}
	
	/**
	 * @param chromosome name of the chromosome
	 * @param start start coordinate of the region
	 * @param stop stop coordinate of the region
	 * @param alignments list of alignments
	 * @return a JFreeChart of a coverage plot containing the multiple normalized average coverage of the specified region
	 */
	public static JFreeChart plotRegion(String chromosome, int start, int stop,String cohort1, ArrayList<Alignment> alignments1, String cohort2,ArrayList<Alignment> alignments2) { // one
		System.out.println("plotRegion:");																	// based
		if (start > stop) { // wrong orientation
			int x = start;
			start = stop;
			stop = x;
		}

		Annotation annotation = new Annotation(chromosome, start - 1, stop - 1); // zero based

		int size = (stop - start + 1);
		float[][] data1 = new float[alignments1.size()][size];
		float[] mean1 = new float[size];
		float[] variance1 = new float[size];
		mean1 = Statistics.calcMean(chromosome, start, stop,alignments1,data1);
		variance1 = Statistics.calcStdev(mean1, start, stop, alignments1, data1);
		float[][] data2 = new float[alignments2.size()][size];
		float[] mean2 = new float[size];
		float[] variance2 = new float[size];
		mean2 = Statistics.calcMean(chromosome, start, stop,alignments2,data2);
		variance2 = Statistics.calcStdev(mean2, start, stop, alignments2, data2);

		// prepare dataset
		YIntervalSeriesCollection seriesCollection = new YIntervalSeriesCollection();
		
		YIntervalSeries series1 = new YIntervalSeries("normalized coverage for cohort "+cohort1);
		for (int p = 0; p < variance1.length; p++)
			series1.add(start + p, mean1[p], mean1[p] - variance1[p], mean1[p] + variance1[p]);
		YIntervalSeries series2 = new YIntervalSeries("normalized coverage for cohort "+cohort2);
		for (int p = 0; p < variance2.length; p++)
			series2.add(start + p, mean2[p], mean2[p] - variance2[p], mean2[p] + variance2[p]);

		seriesCollection.addSeries(series1);
		seriesCollection.addSeries(series2);

		JFreeChart chart = ChartFactory.createXYLineChart("", "Position (bp)", "Normalized Coverage (1 & 2)", seriesCollection,
				PlotOrientation.VERTICAL, true, false, false);

		Font font = new Font("Dialog", Font.PLAIN, 52); // 35

		chart.setTitle(new TextTitle((chromosome + ": " + start + " - " + stop)));
		// Font("Dialog", Font.BOLD, 38)));
		chart.getPlot().setBackgroundPaint(Color.white);
		chart.getPlot().setOutlinePaint(Color.black);
		chart.getLegend().setItemFont(new Font("Dialog", Font.PLAIN, 48)); // 28
		chart.getLegend().setPosition(RectangleEdge.RIGHT);

		DeviationRenderer renderer = new DeviationRenderer(true, false);

		renderer.setSeriesStroke(0, new BasicStroke((float) 6.0)); // 5.0
		renderer.setSeriesPaint(0, Color.BLUE);
		renderer.setSeriesFillPaint(0, Color.BLUE);
		
		renderer.setSeriesStroke(1, new BasicStroke((float) 6.0)); // 5.0
		renderer.setSeriesPaint(1, Color.RED);
		renderer.setSeriesFillPaint(1, Color.RED);

		renderer.setAlpha(0.15f);

		chart.getXYPlot().setRenderer(renderer);

		chart.getXYPlot().getDomainAxis().setLabelFont(font);
		chart.getXYPlot().getRangeAxis().setLabelFont(font);

		chart.getXYPlot().getRangeAxis().setAxisLineStroke(new BasicStroke((float) 4.0)); // 2.0
		chart.getXYPlot().getRangeAxis().setAxisLinePaint(Color.BLACK);
		chart.getXYPlot().getRangeAxis().setTickMarkStroke(new BasicStroke((float) 4.0));
		chart.getXYPlot().getRangeAxis().setTickMarkPaint(Color.BLACK);

		chart.getXYPlot().getDomainAxis().setAxisLineStroke(new BasicStroke((float) 4.0));
		chart.getXYPlot().getDomainAxis().setAxisLinePaint(Color.BLACK);
		chart.getXYPlot().getDomainAxis().setTickMarkStroke(new BasicStroke((float) 4.0));
		chart.getXYPlot().getDomainAxis().setTickMarkPaint(Color.BLACK);

		chart.getXYPlot().getDomainAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 48)); // 25
		chart.getXYPlot().getRangeAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 48)); // 18

//		chart.getXYPlot().getRangeAxis().setRange(0, chart.getXYPlot().getRangeAxis().getRange().getUpperBound());


		chart.getXYPlot().setOutlineVisible(false);
		
		double maxCoverage = 0.0;
		for (int p = 0; p < variance1.length; p++) {
		    maxCoverage = Math.max(maxCoverage, mean1[p] + variance1[p]);
		}
		for (int p = 0; p < variance2.length; p++) {
		    maxCoverage = Math.max(maxCoverage, mean2[p] + variance2[p]);
		}
		System.out.println("maxcoverage: "+maxCoverage);
		// Ensure maxCoverage is positive before setting the range
		double minY = 1.0;  // Prevents an invalid range
		// Add some padding (e.g., 10% of maxCoverage)
//		System.out.println("range: "+Math.max(minY, maxCoverage * 1.1));
		chart.getXYPlot().getRangeAxis().setRange(0, Math.max(minY, maxCoverage * 1.1));

		double yMax = chart.getXYPlot().getRangeAxis().getRange().getUpperBound();
		double offset = 0.1 * yMax;
		double y = yMax;

		LegendItemCollection legend = new LegendItemCollection();

		legend.add(new LegendItem("normalized coverage 1", Color.BLUE));
		legend.add(new LegendItem("normalized coverage 2", Color.RED));

		if (annotation.getRepeats().size() > 0) {
			y -= offset;
			XYLineAnnotation repeatLine = new XYLineAnnotation(start, y, stop, y,
					new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0),
					Color.BLACK);
			chart.getXYPlot().addAnnotation(repeatLine);

			legend.add(new LegendItem("RepeatMasker", Color.BLACK));

			if (Config.plotDNAseClusters)
				legend.add(new LegendItem("DNAse clusters", Color.CYAN));
		}

		// draw Repeats
		for (Repeat r : annotation.getRepeats()) {
			XYLineAnnotation repeatAnnotation = new XYLineAnnotation(r.getStart() + 1, y, r.getEnd(), y,
					new BasicStroke(10.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL), Color.BLACK);
			chart.getXYPlot().addAnnotation(repeatAnnotation);
		}

		// draw DNAse hypersensitive sites
		if (Config.plotDNAseClusters) {
			if (annotation.getDNAseSensitiveSites().size() > 0)
				y -= offset;

			for (Region r : annotation.getDNAseSensitiveSites()) {
				XYLineAnnotation dnaseSensitiveSiteAnnotation = new XYLineAnnotation(r.getStart() + 1, y, r.getStop(),
						y, new BasicStroke(10.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL), Color.CYAN);
				chart.getXYPlot().addAnnotation(dnaseSensitiveSiteAnnotation);
			}
		}

		// draw genes
		for (Gene g : annotation.getGenes()) {
			y -= offset;

			int geneStart;
			int geneEnd;

			if (g.getTxStart() < g.getTxEnd()) {
				geneStart = g.getTxStart() + 1;
				geneEnd = g.getTxEnd();
			} else {
				geneStart = g.getTxEnd();
				geneEnd = g.getTxStart() + 1;
			}

			if (geneStart < start)
				geneStart = start;

			if (geneEnd > stop)
				geneEnd = stop;

			XYLineAnnotation geneLine = new XYLineAnnotation(geneStart, y, geneEnd, y,
					new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0),
					Color.GREEN);
			chart.getXYPlot().addAnnotation(geneLine);
			legend.add(new LegendItem(g.getGeneName(), Color.GREEN));

			// draw exons
			for (int[] exon : g.getExons()) {
				XYLineAnnotation repeatAnnotation = new XYLineAnnotation(exon[0] + 1, y, exon[1], y,
						new BasicStroke(10.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL), Color.GREEN);
				chart.getXYPlot().addAnnotation(repeatAnnotation);
			}
		}

		chart.getXYPlot().setFixedLegendItems(legend);
		chart.setPadding(new RectangleInsets(20, 20, 20, 20));
		chart.setBorderVisible(true);
		chart.setBorderStroke(new BasicStroke((float) 4.0));
		chart.setBorderPaint(Color.BLACK);

		if (!Config.showLegend)
			chart.removeLegend();

		return chart;
	}

	/**
	 * batch coverage plot for a set of regions
	 * @param regions list of regions
	 * @param alignments list of alignments
	 */
	public static void plotAll(ArrayList<Region> regions, ArrayList<Alignment> alignments,String cohort) {
		System.out.println("plotAll:");
		for (Region r : regions)
			SVGBuilder.saveSVG(
					CoverageAnalysis.plotRegion(r.getChromosome(), r.getStart() + 1, r.getStop() + 1, alignments),
					r.getName(), cohort);
	}
	
	/**
	 * batch coverage plot for a set of regions
	 * @param regions list of regions
	 * @param alignments list of alignments
	 */
	public static void plotMultipleCohorts(ArrayList<Region> regions, ArrayList<Alignment> alignments1,String cohort1,ArrayList<Alignment> alignments2,String cohort2) {
		System.out.println("plotMutipleCohorts:");
		for (Region r : regions)
			SVGBuilder.saveSVG(CoverageAnalysis.plotRegion(r.getChromosome(), r.getStart() + 1, r.getStop() + 1, cohort1,alignments1,cohort2,alignments2),r.getName());
	}

	/**
	 * combined coverage plot for two regions
	 * @param name name for svg file
	 * @param r1 region
	 * @param r2 region
	 * @param alignments list of alignments
	 */
	public static void plotFigure(String name, Region r1, Region r2, ArrayList<Alignment> alignments) {

		SVGBuilder.saveSVG(
				CoverageAnalysis.plotRegion(r1.getChromosome(), r1.getStart() + 1, r1.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r2.getChromosome(), r2.getStart() + 1, r2.getStop() + 1, alignments), name);
	}

	/**
	 * combined coverage plot for four regions
	 * @param name name for svg file
	 * @param r1 region
	 * @param r2 region
	 * @param r3 region
	 * @param r4 region
	 * @param alignments list of alignments
	 */
	public static void plotFigure(String name, Region r1, Region r2, Region r3, Region r4,
			ArrayList<Alignment> alignments) {
		SVGBuilder.saveSVG(
				CoverageAnalysis.plotRegion(r1.getChromosome(), r1.getStart() + 1, r1.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r2.getChromosome(), r2.getStart() + 1, r2.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r3.getChromosome(), r3.getStart() + 1, r3.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r4.getChromosome(), r4.getStart() + 1, r4.getStop() + 1, alignments), name);

	}

	/**
	 * combined coverage plot for eight regions
	 * @param name name for svg file
	 * @param r1 region
	 * @param r2 region
	 * @param r3 region
	 * @param r4 region
	 * @param r5 region
	 * @param r6 region
	 * @param r7 region
	 * @param r8 region
	 * @param alignments list of alignments
	 */
	public static void plotFigure(String name, Region r1, Region r2, Region r3, Region r4, Region r5, Region r6,
			Region r7, Region r8, ArrayList<Alignment> alignments) {
		SVGBuilder.saveSVG(CoverageAnalysis.plotRegion(r1.getChromosome(), r1.getStart(), r1.getStop(), alignments),
				CoverageAnalysis.plotRegion(r2.getChromosome(), r2.getStart() + 1, r2.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r3.getChromosome(), r3.getStart() + 1, r3.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r4.getChromosome(), r4.getStart() + 1, r4.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r5.getChromosome(), r5.getStart() + 1, r5.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r6.getChromosome(), r6.getStart() + 1, r6.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r7.getChromosome(), r7.getStart() + 1, r7.getStop() + 1, alignments),
				CoverageAnalysis.plotRegion(r8.getChromosome(), r8.getStart() + 1, r8.getStop() + 1, alignments), name);

	}
}
