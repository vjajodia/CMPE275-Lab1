package crimewatch.obs.madis;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import crimewatch.router.client.MessageClient;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class MesonetProcessor {

	/** file name prefix for general data */
	public static final String sGeneralTag = "madis-mesonet";

	public MesonetProcessor(File catalogF, File outputDir) {
		// TODO read catalog
		// TODO read data

		// TODO if needed, save catalog w/ new stations
	}

	public MesonetProcessor(){}

	/**
	 * assumption: data is received once every 15 minutes or so.
	 * 
	 * This will perform batch processing.
	 * 
	 * @param args
	 */
	public void main(String[] args, MessageClient mc, List<String> otherNodes) {
		if (args.length != 3) {
//			System.out.println("Usage: dataSource catalogFile outputDir");
//			System.exit(2);
		}

		File dataSource = new File("/Users/mulumoodi/Documents/CMPE-275-ClimateWatch/mesonetParser/sample_data/20050621_0800.gz");
		File catF = new File("./catalog.csv");
		File outdir = new File("./output");

		String name = dataSource.getName().substring(0, dataSource.getName().lastIndexOf("."));;
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd_HHmm");
        DateTime dt = formatter.parseDateTime(name);

		// filters
		Date startDate = null;
		Date endDate = null;
		Rectangle region = null;
		Set<String> stationIDs = null;

		System.out.println("\n\nSource: " + dataSource + "\nCatalog: " + catF.getAbsolutePath() + "\nOutput: "
				+ outdir.getAbsolutePath());

		long startTime = System.currentTimeMillis();
		try {
			Catalog cat = new Catalog();
			if (!cat.load(catF)) {
				System.out.println("-- new catalog file created");
			}

			MesonetReader rawReader = new MesonetReader();

			/**
			 * note use readFile() to perform the same steps
			 */

			if (!dataSource.exists())
				return;
			if (dataSource.isFile()) {
				List<Station> stations = rawReader.extractCatalog(dataSource);
				if (stations != null) {
					for (Station s : stations)
						cat.addStation(s);
				}


				List<MesonetData> data = rawReader.extract(dataSource, startDate, endDate, region, stationIDs);
				System.out.println("processed " + data.size() + " entries");

				// now do something with the data
				// 1. send to the cluster or cloud



				int i=0;
				for (MesonetData d : data) {
					// TODO do something other than print!
					StringBuilder retString = new StringBuilder();
					
					System.out.println("Date: " + dt.toString() + "Obs: " + d.getStationID() + " T = " + d.getTemperature() + ", WS = "
							+ d.getWindSpeed() + ", WD = " + d.getWindDir() + ", RH = " + d.getRelHumidity());
					retString.append("Date: " + dt.toString() + "Obs: " + d.getStationID() + " T = " + d.getTemperature() + ", WS = "
							+ d.getWindSpeed() + ", WD = " + d.getWindDir() + ", RH = " + d.getRelHumidity());
					//mc.postMessage(retString.toString());
					
						mc = new MessageClient(otherNodes.get(i%otherNodes.size()),4568);
					//mc.commConnection.bs.connect(otherNodes.get(i%otherNodes.size()),4568);
					mc.postMessage(retString.toString());
						i++;
						mc.postMessage(retString.toString());
						try        
						{
						    Thread.sleep(1000);
						} 
						catch(InterruptedException ex) 
						{
						    Thread.currentThread().interrupt();
						}
											
				}
			} else {
				FileFilter filter = new FileFilter() {
					public boolean accept(File pathname) {
						return (pathname.isFile() && !pathname.getName().startsWith(".")
								&& !pathname.getName().endsWith(".gz"));
					}
				};

				// TODO walk through accepted files and process
				System.out.println("TODO: process files");

			}

			// save catalog
			cat.save(catF);

			long stopTime = System.currentTimeMillis();
			System.out.println(
					"MADIS Mesonet - total processing time is " + ((stopTime - startTime) / 1000.0) + " seconds");
		} catch (Throwable t) {
			System.out.println(
					"Unable to process mesowest data in " + dataSource.getAbsolutePath() + ": " + t.getMessage());
		}
	}
}
