package data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import config.Config;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;

/**
 * Provides functionality to obtain repeat masks from an annotation database
 * @author Stefan Grabuschnig
 *
 */
public class RepeatMasker {
	private static final DataBase db = new DataBase(Config.dbDriver, Config.dbURL, Config.dbUser, Config.dbPassword);

	/**
	 * @param genome the considered genome
	 * @param chromosome name of the chromosome
	 * @return the repeat mask for all annotated repeats on chromosome
	 */
	public static int[] getRepeatMask(Genome genome, String chromosome) {
		int[] mask = null;
		File file = new File("rmsk" + File.separator + chromosome + ".rmsk");

		try {
			if (file.exists()) {
				mask = RepeatMasker.uncompressIntArray(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
			} else {
				System.out.print("Generating repeat mask...");
				mask = new int[genome.getChromosomeSize(chromosome)];

				db.connect();

				String query = "SELECT genoStart, genoEnd FROM " + Config.dbSchema + ".rmsk" + " WHERE genoName = '"
						+ chromosome + "';";

				ResultSet rs = db.performQuery(query);

				while (rs.next()) {
					for (int i = rs.getInt("genoStart"); i < rs.getInt("genoEnd"); i++)
						mask[i] = 1;
				}
				rs.close();
				db.disconnect();

				file.getParentFile().mkdirs();
				Files.write(Paths.get(file.getAbsolutePath()), RepeatMasker.compress(mask));
				System.out.println("finished.");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}

		return mask;
	}

	/**
	 * @param genome the considered genome
	 * @param chromosome name of the chromosome
	 * @param repeatFamily name of the repeat family
	 * @return the repeat mask for the specified repeat family. It is a vector containing "1" where the specific repeat family is detected
	 */
	public static int[] getRepeatFamilyMask(Genome genome, String chromosome, String repeatFamily) {
		int[] mask = new int[genome.getChromosomeSize(chromosome)];
		String query2 = "SELECT genoStart, genoEnd FROM " + Config.dbSchema + ".rmsk" + " WHERE genoName = '"
				+ chromosome + "' AND rmsk.repFamily = '" + repeatFamily + "';";
				// adding extra printout function
		//System.out.println(" query2 is: " + query2);
		db.connect();
		ResultSet rs = db.performQuery(query2);

		try {
			while (rs.next()) {
				for (int i = rs.getInt("genoStart"); i < rs.getInt("genoEnd"); i++)
					mask[i] = 1;
			}
			rs.close();
			db.disconnect();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return mask;
	}

	/**
	 * @return complete list of all annotated repeat families in the database
	 */
	public static LinkedList<String> getRepeatFamilies() {
		ResultSet rs;
		LinkedList<String> repeatFamilies = new LinkedList<String>();

		db.connect();
		// adding extra printout function
		System.out.println("UCSC database for human genome is: " + Config.dbSchema);
		String query = "SELECT DISTINCT rmsk.repFamily FROM " + Config.dbSchema + ".rmsk;";
		//System.out.println(" query is: " + query);
		rs = db.performQuery(query);
		try {
			while (rs.next())
				repeatFamilies.add(rs.getString("rmsk.repFamily"));

			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		db.disconnect();
		return repeatFamilies;
	}

	private static int[] uncompressIntArray(byte[] bytes) {
		int[] intArray = null;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			BufferedInputStream bis = new BufferedInputStream(bais);
			LZ4FrameInputStream lz4is = new LZ4FrameInputStream(bis);
			ObjectInputStream ois = new ObjectInputStream(lz4is);
			intArray = (int[]) ois.readObject();
			ois.close();
		} catch (IOException e) {
			System.out.println("Error while uncompressing int array");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Error while uncompressing int array");
			e.printStackTrace();
		}
		return intArray;
	}

	private static byte[] compress(int[] intArray) {
		byte[] bytesOut = null;

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			LZ4FrameOutputStream lz4os = new LZ4FrameOutputStream(baos);
			BufferedOutputStream bos = new BufferedOutputStream(lz4os);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(intArray);
			oos.flush();
			oos.close();
			lz4os.close();
			bytesOut = baos.toByteArray();
			baos.close();
		} catch (IOException e) {
			System.out.println("Error while compressing int array");
			e.printStackTrace();
		}
		return bytesOut;
	}
}
