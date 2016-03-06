package io.robrose.hack.wifirecorder.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class BigDataAnalysis {

	private static final String fileName = "training_data.csv";
	
	private static TreeMap<String, TreeMap<String, Stats>> allData;
	
	/* A private inner class to store the average signal strength, the standard deviation, and the position stats 
	 * (latitude, longitude, altitude) for each *general* location that has been recorded so far.
	 */
	private static class Stats
	{
		ArrayList<Integer> signal_data_points;
		int nLRSF;
		float avg_signal_strength; // average signal strength
		float std_dev; // standard deviation
		float latitude;
		float longitude;
		float altitude;
		
		public Stats ()
		{
			signal_data_points = new ArrayList<Integer>();
			nLRSF = 0;
			avg_signal_strength = 0;
			std_dev = 0;
			latitude = 0;
			longitude = 0;
			altitude = 0;
		}
		
		public void add (int nextSignal, float nextLat, float nextLong, float nextAlt)
		{
			nLRSF++;
			signal_data_points.add(new Integer(nextSignal));
			avg_signal_strength += nextSignal; // to improve efficiency, keep track of the sum as we enter them into the ArrayList.
			latitude += nextLat;
			longitude += nextLong;
			altitude += nextAlt;
		}
		
		/* 
		 * Will finally, after all data has been aggregated using the add() function, calculate the mean and standard deviation for this object, which pertains to just ONE AND ONLY ONE
		 * PAIRING OF a general location, expressed vaguely by a String, and a router point, expressed by a MAC Address (which is really just another String).
		 */
		public void calculate ()
		{
			avg_signal_strength /= nLRSF; // now, we only need one step to find the ACTUAL mean. =)
			for (int i = 0; i < nLRSF; i ++)
			{
				std_dev += ((signal_data_points.get(i) - avg_signal_strength)*(signal_data_points.get(i) - avg_signal_strength));
			}
			std_dev /= nLRSF;
			std_dev = (float) Math.sqrt((double) std_dev);
			latitude /= nLRSF;
			longitude /= nLRSF;
			altitude /= nLRSF;
		}
		
		public String toString ()
		{
			return ("" + avg_signal_strength + "," + std_dev + "," + nLRSF + "");
		}
	}
	
	public BigDataAnalysis ()
	{
		File config = new File ("config.txt");
		try
		{
			Scanner directoryFinder = new Scanner (config);
			String line  = directoryFinder.nextLine();
			File givenDirectory = new File (line);
			directoryFinder.close();
			if (givenDirectory.isDirectory())
			{
				File swDirectory = givenDirectory;
				File sourceFile = new File (swDirectory.toString() + "\\" + fileName);
				System.out.println ("Now checking " + sourceFile.getName());
				WifiDataConnectionsInterpreter nextFile = new WifiDataConnectionsInterpreter (sourceFile);
				nextFile.process(sourceFile);
			}
			else
			{
				WifiDataConnectionsInterpreter singleAction = new WifiDataConnectionsInterpreter (givenDirectory);
				singleAction.process(givenDirectory);
			}
		} catch (FileNotFoundException k)
		{
			k.printStackTrace();
		}

	}
	
	public BigDataAnalysis (File givenDirectory)
	{
		if (givenDirectory.isDirectory())
		{
			File swDirectory = givenDirectory;
			File[] subFiles = swDirectory.listFiles();
			ArrayList<String> errors = new ArrayList<String>();
			if (subFiles == null)
			{
				System.out.println ("ERR: Given directory does not exist on this file system.");
			}
			else
			{
				boolean failures = false;
				for (int ind = 0; ind < subFiles.length; ind ++)
				{
					if (subFiles[ind].getName().indexOf (".csv") != (-1))
					{
						try
						{
							File setOfWifiSignals = subFiles[ind];
							System.out.println ("Now checking " + setOfWifiSignals.getName());
							WifiDataConnectionsInterpreter nextFile = new WifiDataConnectionsInterpreter (setOfWifiSignals);
							nextFile.process(setOfWifiSignals);
						}
						catch (UnsupportedOperationException k)
						{
							failures = true;
							errors.add ((subFiles[ind].getName() + " ERR: Unsupported Operation."));
						}
						catch (StringIndexOutOfBoundsException k)
						{
							failures = true;
							errors.add ((subFiles[ind].getName() + " ERR: CSV Formatting is different."));
						}
					}
				}
				if (! failures)
				{
					errors.add (("No failed documents exist.\nAll input documents were processed without error."));
				}
			}
			File outputFile = new File (swDirectory.getParent() + "\\outputFiles\\failed_csv_document_log.txt");
			try
			{
				PrintWriter output = new PrintWriter (outputFile);
				for (int index = 0; index < errors.size(); index ++)
				{
					output.println (errors.get(index));
				}
				output.close();
			}
			catch (FileNotFoundException k)
			{
				System.out.println ("ERR: File Not Found. Internal Error.");
				k.printStackTrace();
			}
		}
		else
		{
			WifiDataConnectionsInterpreter singleAction = new WifiDataConnectionsInterpreter (givenDirectory);
			singleAction.process(givenDirectory);
		}
	}
	
	public class WifiDataConnectionsInterpreter
	{
		private File csvFile;
		private TreeMap<String, TreeMap<String, Stats>> allConnections;
		
		public WifiDataConnectionsInterpreter (File csvInputFile)
		{
			csvFile = csvInputFile;
			allConnections = new TreeMap<String, TreeMap<String, Stats>>();
		}
		
		public void process (File nextFile)
		{
			/* 
			 * Note: we expect these dBm values to range from -100 to -10, and we'll interpret them as doubles.
			 * 
			 */
			try
			{
				Scanner allInputs = new Scanner (nextFile);
				String nextEntry = allInputs.nextLine();
				while (allInputs.hasNextLine())
				{
					nextEntry = allInputs.nextLine();
					analyzeOneSignal (nextEntry);
				}
				allInputs.close();
			}
			catch (FileNotFoundException k)
			{
				System.out.println ("404'd -- File Not Found");
				k.printStackTrace();
			}
			File outputFile = new File (nextFile.getParent() + "\\" + nextFile.getName().substring(0, nextFile.getName().indexOf('.')) + "_output.txt");
			File outputCSVFile = new File (nextFile.getParent() + "\\" + nextFile.getName().substring(0, nextFile.getName().indexOf('.')) + "_outputCSV.csv");
			try
			{
//				String evaluation = new String ("");
				PrintWriter output = new PrintWriter (outputFile);
				PrintWriter outputCSV = new PrintWriter (outputCSVFile);

				Set<String> allKeys = allConnections.keySet();
				Iterator<String> allKeysIter = allKeys.iterator();
				output.println ("Example text");
				while (allKeysIter.hasNext())
				{
					String location = allKeysIter.next();
					output.println ("Now checking the outer key (Location) " + location);
					TreeMap<String, Stats> nextTree = allConnections.get(location);
					Set<String> nextTreeKeySet = nextTree.keySet();
					if (nextTreeKeySet.isEmpty())
					{
						output.println ("\t  Error has occurred. The program thinks that his MAC Address has no matching location =(");
					}
					Iterator<String> nextTreeIter = nextTreeKeySet.iterator();
					while (nextTreeIter.hasNext())
					{
						String MAC = nextTreeIter.next();
						nextTree.get(MAC).calculate();
						output.println ("\t  In location " + location + ", identifying MAC = " + MAC + ", data : " + nextTree.get(MAC).toString());
						outputCSV.println (location + "," + MAC + "," + nextTree.get(MAC).toString());
					}
				}
				outputCSV.close();
				output.close();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				System.out.println ("EXCEPTION - output File processing. This should not have happened. :(");
			}
		}
		
		/*
		 * Precondition: entry is a non-null, non-empty String that obeys the expected format for our CSV document (Location, MAC Address, signal strength in dBm)
		 * Postcondition: Will analyze a given String entry in the table of MAC Addresses that have been identified
		 */
		private void analyzeOneSignal (String entry)
		{
			String colA = entry.substring (0, entry.indexOf (','));
			
			String colB = entry.substring (1 + entry.indexOf (','));
			colB = colB.substring (0, colB.indexOf (','));
			
			String colC = entry.substring (1 + entry.indexOf (','));
			colC = colC.substring (1 + colC.indexOf (','));
			colC = colC.substring (0, colC.indexOf (','));

			String colD = entry.substring (1 + entry.indexOf (','));
			colD = colD.substring (1 + colD.indexOf (','));
			colD = colD.substring (1 + colD.indexOf (','));
			colD = colD.substring (0, colD.indexOf (','));
			
			String colE = entry.substring (1 + entry.indexOf (','));
			colE = colE.substring (1 + colE.indexOf (','));
			colE = colE.substring (1 + colE.indexOf (','));
			colE = colE.substring (1 + colE.indexOf (','));
			colE = colE.substring (0, colE.indexOf (','));
			
			String colF = entry.substring (1 + entry.indexOf (','));
			colF = colF.substring (1 + colF.indexOf (','));
			colF = colF.substring (1 + colF.indexOf (','));
			colF = colF.substring (1 + colF.indexOf (','));
			colF = colF.substring (1 + colF.indexOf (','));
			colF = colF.substring (0, colF.indexOf (','));
			
			String colG = entry.substring (1 + entry.indexOf (','));
			colG = colG.substring (1 + colG.indexOf (','));
			colG = colG.substring (1 + colG.indexOf (','));
			colG = colG.substring (1 + colG.indexOf (','));
			colG = colG.substring (1 + colG.indexOf (','));
			colG = colG.substring (1 + colG.indexOf (','));
			colG = colG.substring (0, colG.indexOf (','));
			
			String colH = entry.substring (1 + entry.indexOf (','));
			colH = colH.substring (1 + colH.indexOf (','));
			colH = colH.substring (1 + colH.indexOf (','));
			colH = colH.substring (1 + colH.indexOf (','));
			colH = colH.substring (1 + colH.indexOf (','));
			colH = colH.substring (1 + colH.indexOf (','));
			colH = colH.substring (1 + colH.indexOf (','));
			colH = colH.substring (0, colH.indexOf (','));
			
			String colI = entry.substring (1 + entry.indexOf (','));
			colI = colI.substring (1 + colI.indexOf (','));
			colI = colI.substring (1 + colI.indexOf (','));
			colI = colI.substring (1 + colI.indexOf (','));
			colI = colI.substring (1 + colI.indexOf (','));
			colI = colI.substring (1 + colI.indexOf (','));
			colI = colI.substring (1 + colI.indexOf (','));
			colI = colI.substring (1 + colI.indexOf (','));
			colI = colI.substring (0, colI.indexOf (','));
			/*
			if (colC.indexOf (',') != (-1))
			{
				colC = colC.substring (0, colC.indexOf (','));
			}
			else
			{
				// no-op
			}
			*/
			int nextSignal = Integer.parseInt(colE);
			float nextLatitude = (float)(Double.parseDouble(colG));
			float nextLongitude = (float)(Double.parseDouble(colH));
			float nextAltitude = (float)(Double.parseDouble(colI));
			Set<String> outerKeySet = allConnections.keySet();
			if (outerKeySet.contains (colC)) // if we've seen this MAC Address before..
			{
				TreeMap<String, Stats> existingTree = allConnections.get(colC);
				Set<String> allMACAddrses = existingTree.keySet();
				if (allMACAddrses.contains (colD)) // then, if we've seen this location before...
				{
					// then, use the add function on the existing Stat object.
//					System.out.println ("This should be printing.");
					allConnections.get(colC).get(colD).add(nextSignal, nextLatitude, nextLongitude, nextAltitude);
				}
				else
				{
					// then, generate a new Stat object.
					Stats next = new Stats ();
					next.add(nextSignal, nextLatitude, nextLongitude, nextAltitude);
					existingTree.put(colD, next);
				}
			}
			else
			{
				// then, make a new TreeMap inside this TreeMap. TreeMap some more.
				if (colC.equals(""))
				{
					
				}
				else
				{
					allConnections.put(colC, new TreeMap<String, Stats>());
				}
			}
		}
	}
	
	public static void execute ()
	{
		System.out.println ("Please type the name of the .CSV File, OR the directory of the default .CSV File to be processed, or the empty String to use the directory in the config file:");
		Scanner input = new Scanner (System.in);
		String fileName = input.nextLine();
		input.close();
		if (! fileName.equals (""))
		{
			File inputFile = new File (fileName);
			BigDataAnalysis processor = new BigDataAnalysis (inputFile);
		}
		else
		{
			BigDataAnalysis processor = new BigDataAnalysis ();
		}
		System.out.println ("Program terminated.");
	}
	
	public static void main (String[] argv)
	{
		execute();
	}

}