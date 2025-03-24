package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import data.Region;

/**
 * Provided functionallity to import predefined regions from a textfile
 * @author Stefan Grabuschnig
 *
 */
public class RegionImporter {

	/**
	 * @param fileName path to a textfile
	 * @return list of regions
	 */
	public static ArrayList<Region> importRegions(String fileName) {
		ArrayList<Region> regions = new ArrayList<Region>(100);
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = null;

			while ((line = br.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(line, "\t");
				regions.add(new Region(tokenizer.nextToken(), tokenizer.nextToken(),
						Integer.parseInt(tokenizer.nextToken()) - 1, Integer.parseInt(tokenizer.nextToken()) - 1));
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return regions;
	}
}
