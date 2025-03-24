package util;

import java.util.ArrayList;

import data.Alignment;

public class Statistics {

	public static float[] calcMean(String chromosome, int start, int stop,ArrayList<Alignment> alignments,float[][] data){
		int size = (stop - start + 1);
		float[] mean = new float[size];
		
		for (int i = 0; i < alignments.size(); i++) {
			float[] normalizedCoverage = alignments.get(i).getNormalizedCoverage(chromosome);

			for (int p = start - 1; p < stop; p++)
				data[i][p - start + 1] = normalizedCoverage[p];

			for (int p = 0; p < mean.length; p++)
				mean[p] += data[i][p];
		}

		for (int p = 0; p < mean.length; p++)
			mean[p] /= alignments.size();
		
		return mean;
	}
	
	public static float[] calcStdev(float[] mean,int start, int stop,ArrayList<Alignment> alignments,float[][] data) {
		int size = (stop - start + 1);
		float[] variance = new float[size];
		
		for (int i = 0; i < data.length; i++)
			for (int p = 0; p < variance.length; p++)
				variance[p] += (data[i][p] - mean[p]) * (data[i][p] - mean[p]);

		for (int p = 0; p < variance.length; p++)
			variance[p] = (float) Math.sqrt(variance[p] / alignments.size());
		
		return variance;
	}
}
