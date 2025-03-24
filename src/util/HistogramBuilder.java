package util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.ui.RectangleInsets;

/**
 * Provides functionallity to create simple histograms
 * @author Stefan Grabuschnig
 *
 */
public class HistogramBuilder {
	/**
	 * @param values array of double values
	 * @param numBins number of bins in the histogram
	 * @param sumToOne scale the sum of the bar heights to one (approximated probability density)
	 * @return a JFreeChart histogram object
	 */
	public static JFreeChart createSizeHistogram(double[] values, int numBins, boolean sumToOne) {

		HistogramDataset dataset = new HistogramDataset();
		
		if(sumToOne)
			dataset.setType(HistogramType.SCALE_AREA_TO_1);
		else
			dataset.setType(HistogramType.FREQUENCY);
		
		dataset.addSeries("", values, numBins);
		String plotTitle = "";
		String xaxis = "Length (bp)";
		String yaxis = "Frequency (1)";
		PlotOrientation orientation = PlotOrientation.VERTICAL;

		JFreeChart chart = ChartFactory.createHistogram(plotTitle, xaxis, yaxis, dataset, orientation, false, false,
				false);

		chart.getPlot().setBackgroundPaint(Color.white);
		chart.getPlot().setOutlinePaint(Color.black);

		chart.setPadding(new RectangleInsets(20, 20, 20, 20));

		XYBarRenderer renderer = new XYBarRenderer();
		// renderer.setBarPainter(new StandardBarPainter());
		renderer.setGradientPaintTransformer(null);
		renderer.setShadowVisible(false);
		renderer.setDrawBarOutline(true);
		// renderer.setMaximumBarWidth(0.15);

		Stroke s = new BasicStroke(5);

		renderer.setSeriesOutlineStroke(0, s);
		renderer.setSeriesOutlinePaint(0, Color.black);
		renderer.setSeriesPaint(0, Color.white);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setOutlineVisible(false);

		plot.getDomainAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 48)); // 18
		plot.getRangeAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 48)); // 18

		plot.setShadowGenerator(null);
		plot.getDomainAxis().setLabelFont(new Font("Dialog", Font.PLAIN, 52));
		plot.getRangeAxis().setLabelFont(new Font("Dialog", Font.PLAIN, 52));
		plot.getDomainAxis().setLabelPaint(Color.BLACK);
		plot.getRangeAxis().setLabelPaint(Color.BLACK);

		plot.getDomainAxis().setAxisLineStroke(new BasicStroke(5));
		plot.getDomainAxis().setTickMarkStroke(new BasicStroke(5));
		plot.getDomainAxis().setAxisLinePaint(Color.BLACK);
		plot.getDomainAxis().setTickMarkPaint(Color.BLACK);

		plot.getRangeAxis().setAxisLineStroke(new BasicStroke(5));
		plot.getRangeAxis().setTickMarkStroke(new BasicStroke(5));
		plot.getRangeAxis().setAxisLinePaint(Color.BLACK);
		plot.getRangeAxis().setTickMarkPaint(Color.BLACK);

		plot.setRenderer(renderer);
		
		chart.setPadding(new RectangleInsets(20, 20, 20, 20));
		chart.setBorderVisible(true);
		chart.setBorderStroke(new BasicStroke((float) 4.0));
		chart.setBorderPaint(Color.BLACK);

		return chart;
	}

}
