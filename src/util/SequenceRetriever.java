package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import data.Genome;
import data.Region;

/**
 * Provides functionallity to retrieve the sequences of genomic regions from fasta files.
 * @author Stefan Grabuschnig
 *
 */
public class SequenceRetriever {
	/**
	 * @param regions list of regions
	 * @param genome the respective genome
	 */
	public static void retrieveSequences(ArrayList<Region> regions, Genome genome) {

		try {
			BufferedWriter bwr = new BufferedWriter(new FileWriter("sequences.fa"));
			int n = 0;
			for (Region r : regions) {
				n++;
				bwr.write(">seq" + n);
				bwr.newLine();
				File fastAFile = genome.getFastA(r.getChromosome());

				if (fastAFile.exists()) {

					BufferedReader br = new BufferedReader(new FileReader(fastAFile));

					int headerLength;
					int lineLength;

					// print header
					String line = br.readLine();
					headerLength = line.length() + 1;

					// get line Length
					line = br.readLine();
					lineLength = line.length();
					br.close();

					int sequenceStart = headerLength + r.getStart() + (r.getStart() / lineLength);
					int sequenceEnd = headerLength + r.getStop() + (r.getStop() / lineLength);
					char[] seq = new char[sequenceEnd - sequenceStart];

					// read sequence from position
					br = new BufferedReader(new FileReader(fastAFile));
					br.skip(headerLength + r.getStart() + (r.getStart() / lineLength));
					if (br.read(seq) > 0) {
						String sequence = new String(seq);
						sequence = sequence.replaceAll("\n", "");
						bwr.write(sequence);
						bwr.newLine();
					}
					br.close();
				}
			}
			bwr.flush();
			bwr.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
