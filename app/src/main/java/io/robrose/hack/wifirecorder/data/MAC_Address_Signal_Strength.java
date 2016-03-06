package io.robrose.hack.wifirecorder.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/* 
 *    A class to process and store MAC Addresses and their associated dBm values, using a hash map.
 * To do this, the method process() analyzes each line (each MAC Address identified in the CSV file)
 * and then uses  String manipulation to parse out relevant data.
 */
public class MAC_Address_Signal_Strength
{
	private static final String fileName = "Test_csv_MACAddresses.csv";
	
	public MAC_Address_Signal_Strength ()
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
				WifiPointsInterpreter nextFile = new WifiPointsInterpreter (sourceFile);
				nextFile.process(sourceFile);
			}
			else
			{
				WifiPointsInterpreter singleAction = new WifiPointsInterpreter (givenDirectory);
				singleAction.process(givenDirectory);
			}
		} catch (FileNotFoundException k)
		{
			k.printStackTrace();
		}

	}
	
	public MAC_Address_Signal_Strength (File givenDirectory)
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
							WifiPointsInterpreter nextFile = new WifiPointsInterpreter (setOfWifiSignals);
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
			WifiPointsInterpreter singleAction = new WifiPointsInterpreter (givenDirectory);
			singleAction.process(givenDirectory);
		}
	}
	
	public class WifiPointsInterpreter
	{
		private File csvFile;
		private HashMap<String, Double> wifiSignals;
		
		public WifiPointsInterpreter (File csvInputFile)
		{
			csvFile = csvInputFile;
			wifiSignals = new HashMap<String, Double>();
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
			try
			{
				String evaluation = new String ("");
				PrintWriter output = new PrintWriter (outputFile);
				Set<String> allKeys = wifiSignals.keySet();
				Iterator<String> allKeysIter = allKeys.iterator();
				output.println ("Example text");
				while (allKeysIter.hasNext())
				{
					String key = allKeysIter.next();
					double distance = wifiSignals.get(key).doubleValue();
					distance = (150.0)*Math.pow(10, (((-1*distance) - 113.0) / 40.0));
					output.println ("  " + key + " : signal recorded at " + (wifiSignals.get(key)) + " which corresponds to a distance of " + (float)distance + " feet.");
				}
				
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
			if (colC.indexOf (',') != (-1))
			{
				colC = colC.substring (0, colC.indexOf (','));
			}
			else
			{
				// no-op
			}
			
			double nextSignal = Double.parseDouble(colC);
			wifiSignals.put (colB, new Double (nextSignal));
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
			MAC_Address_Signal_Strength processor = new MAC_Address_Signal_Strength (inputFile);
		}
		else
		{
			MAC_Address_Signal_Strength processor = new MAC_Address_Signal_Strength ();
		}
		System.out.println ("Program terminated.");
	}
	
	public static void main (String[] argv)
	{
		execute();
	}
}