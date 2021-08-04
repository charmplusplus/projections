package projections.analysis;

import java.awt.Component;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.SortedSet;

import javax.swing.ProgressMonitor;

import projections.gui.MainWindow;
import projections.misc.LogEntry;

public class CallTable extends ProjDefs
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private int myRun = 0;

	private int numPe;		     //Number of processors
	private int numEPs;	   	     //Number of entry methods
	private double[][] byteSum;	     //Array for EP message byte sum to be stored
	private double[][] msgCount;     //Array for EP message count to be stored
	private double[][] sumSquares;   //Array for EP sum of byte counts squared
	private int[][] minStats;	     //Array for EP min stats to be stored
	private int[][] maxStats;	     //Array for EP max stats to be stored
	private double[][] varStats;     //Array for EP variance stats to be stored
	private boolean[] exists;	     //Array to remember if sourceEP sent any messages
	private long startTime;	     //Interval begin
	private long endTime;	     //Interval end
	private SortedSet<Integer> peList;   //List of processors
	private DecimalFormat _format;   //Format for output

	public CallTable(long startInterval, long endInterval, 
			SortedSet<Integer> processorList)
	{
		//Initialize class variables
		peList = processorList;
		numPe = peList.size();
		numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		startTime = startInterval;
		endTime = endInterval;
		byteSum = new double[numEPs][numEPs];
		msgCount = new double[numEPs][numEPs];
		sumSquares = new double[numEPs][numEPs];
		minStats = new int[numEPs][numEPs];
		maxStats = new int[numEPs][numEPs];
		varStats = new double[numEPs][numEPs];
		exists = new boolean[numEPs];
		_format = new DecimalFormat("###,###.###");
	}

	public void GatherData(Component parent)
	{
		GenericLogReader reader;
		LogEntry logdata;

		int currPeIndex = 0;
		int sourceEP;

		ProgressMonitor progressBar =
			new ProgressMonitor(parent, "Generating Call Table",
					"", 0, numPe);

		for (Integer pe : peList){
			if (!progressBar.isCanceled()) {
				progressBar.setNote("[PE: " + pe + " ] Reading data.");
				progressBar.setProgress(currPeIndex+1);
			}
			else {
				progressBar.close();
				break;
			}
			reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());

			try
			{
				logdata = reader.nextEventOnOrAfter(startTime);
				//Now we have entered into the time interval

				Deque<Integer> creationStack = new ArrayDeque<Integer>();
				while ( (logdata.time<endTime)&&(logdata.type!=BEGIN_PROCESSING) ) {
					//Account for any Creations encountered after a BP but before an EP
					// Basically, the start interval begins inside a Processing Block
					if (logdata.type == CREATION) {
						creationStack.push(logdata.entry);
						creationStack.push(logdata.msglen);
					}
					if (logdata.type == END_PROCESSING) {
						sourceEP = logdata.entry;
						while(creationStack.size() != 0) {
							int msglen = creationStack.pop();
							int destEP = creationStack.pop();
							if ( (minStats[sourceEP][destEP]>msglen) ||
									(minStats[sourceEP][destEP]==0) )
								minStats[sourceEP][destEP] = msglen;
							if (maxStats[sourceEP][destEP]<msglen)
								maxStats[sourceEP][destEP] = msglen;
							msgCount[sourceEP][destEP]++;
							byteSum[sourceEP][destEP]+=msglen;
							sumSquares[sourceEP][destEP]+=(double)msglen*(double)msglen;
							if (!exists[sourceEP])
								exists[sourceEP]=true;
						}
						logdata = reader.nextEvent();
						break;
					}
					logdata = reader.nextEvent();
				}

				while (logdata.time < endTime) {
					//Go through each log file and for each Creation
					//  in a Processing Block, update arrays
					if (logdata.type == BEGIN_PROCESSING) {
						//Starting new entry method
						sourceEP = logdata.entry;
						logdata = reader.nextEvent();
						while ( (logdata.type != END_PROCESSING) &&
								(logdata.type != END_COMPUTATION) &&
								(logdata.time < endTime) ) {
							if (logdata.type == CREATION) {
								int msglen = logdata.msglen;
								int destEP = logdata.entry;
								if ( (minStats[sourceEP][destEP]>msglen) ||
										(minStats[sourceEP][destEP]==0) )
									minStats[sourceEP][destEP] = msglen;
								if (maxStats[sourceEP][destEP]<msglen)
									maxStats[sourceEP][destEP] = msglen;
								msgCount[sourceEP][destEP]++;
								byteSum[sourceEP][destEP]+=msglen;
								sumSquares[sourceEP][destEP]+=(double)msglen*(double)msglen;
								if (!exists[sourceEP])
									exists[sourceEP]=true;
							}
							logdata = reader.nextEvent();
						}
					}
					logdata = reader.nextEvent();
				}
			} catch (EndOfLogSuccess e) {
				// Reached end of the log file successfully
			} catch (IOException e) {
				System.err.println("Error reading log data for processor " + pe);
				System.err.println(e);
			}
		
			
			try {
				reader.close();
			} catch (IOException e1) {
				System.err.println("Error: could not close log file reader for processor " + pe );
			}
		    
			
			currPeIndex++;
		}

		// Update Variance Array
		for (int srcEP=0; srcEP<numEPs; srcEP++) {
			if (exists[srcEP]) {
				for (int destEP=0; destEP<numEPs; destEP++) {
					if (msgCount[srcEP][destEP]==1)
						varStats[srcEP][destEP] = 0;
					else if (msgCount[srcEP][destEP]>1) {
						double mean = byteSum[srcEP][destEP]/msgCount[srcEP][destEP];
						//Variance = (sumofsquares - 2*mean*sum + mean*mean*count) / (count-1)
						varStats[srcEP][destEP] = ((sumSquares[srcEP][destEP]
						                                              - 2.0*mean*byteSum[srcEP][destEP]
						                                                                        + mean*mean*msgCount[srcEP][destEP])
						                                                                        /(msgCount[srcEP][destEP]-1.0));
					}
				}
			}
		}

		progressBar.close();
	}

	public String[][] getCallTableText(boolean epDetailToggle, boolean statsToggle)
	{
		// length is number of lines
		int length = 0;
		for (int sourceEP=0; sourceEP<numEPs; sourceEP++) {
			if (exists[sourceEP]) {
				length++;
				for (int destEP=0; destEP<numEPs; destEP++) {
					if (msgCount[sourceEP][destEP] > 0)
						length+=2;
				}
				length+=2; //for 2 line spaces between source EPs
			}
		}

		String[][] text = new String[length][1];
		int lengthCounter = 0;

		for (int sourceEP=0; sourceEP<numEPs; sourceEP++) {
			if (exists[sourceEP]) {

				if (epDetailToggle==true) { //need ep detail
					text[lengthCounter][0] = MainWindow.runObject[myRun].getEntryFullNameByIndex(sourceEP);
				}
				else { //don't need ep detail
					String s = MainWindow.runObject[myRun].getEntryNameByIndex(sourceEP);
					int parenthIndex = s.indexOf('(');
					if (parenthIndex != -1) //s has parenthesis
						s = s.substring(0, parenthIndex);
					text[lengthCounter][0] = s;
				}

				lengthCounter++;

				for (int destEP=0; destEP<numEPs; destEP++) {
					if (msgCount[sourceEP][destEP] > 0) {

						if (epDetailToggle==true) { //need ep detail
							text[lengthCounter][0] = "        " + MainWindow.runObject[myRun].getEntryFullNameByIndex(destEP);
						}
						else { //don't need ep detail
							String s = MainWindow.runObject[myRun].getEntryNameByIndex(destEP);
							int parenthIndex = s.indexOf('(');
							if (parenthIndex != -1) //s has parenthesis
								s = s.substring(0, parenthIndex);
							text[lengthCounter][0] = "        " + s;
						}

						lengthCounter++;

						if (statsToggle==true) { //stats needed
							text[lengthCounter][0] = "                " + 
							"Msg's Rec'd=" + _format.format(msgCount[sourceEP][destEP]) + 
							"  Bytes Rec'd=" + _format.format(byteSum[sourceEP][destEP]) +
							"  Min=" + _format.format(minStats[sourceEP][destEP]) +
							"  Max=" + _format.format(maxStats[sourceEP][destEP]) +
							"  Mean=" +
							_format.format(byteSum[sourceEP][destEP]/msgCount[sourceEP][destEP]) +
							"  Variance=" + _format.format(varStats[sourceEP][destEP]);
						}
						else { //stats not needed
							text[lengthCounter][0] = "";
						}

						lengthCounter++;
					}
				}
				text[lengthCounter][0] = "";
				lengthCounter++;
				text[lengthCounter][0] = "";
				lengthCounter++;
			}
		}

		return text;
	}

//	public void PrintToFile()
//	{
//		try
//		{
//			//Write arrays to file
//			BufferedWriter output = new BufferedWriter(new FileWriter("calltable"));
//			output.write("CALL TABLE FOR " + MainWindow.runObject[myRun].getFilename() + ".sts -\n");
//			for (int sourceEP=0; sourceEP<numEPs; sourceEP++) {
//				if (exists[sourceEP]) {
//					output.write("\n\n");
//					output.write(MainWindow.runObject[myRun].getEntryFullNameByIndex(sourceEP) + "[EPid #" +
//							sourceEP + "]\n");
//					for (int destEP=0; destEP<numEPs; destEP++) {
//						if (msgCount[sourceEP][destEP] > 0) {
//							output.write("    " + MainWindow.runObject[myRun].getEntryFullNameByIndex(destEP) + "[EPid #" + destEP + "] - " +
//									msgCount[sourceEP][destEP] + " messages, " +
//									byteSum[sourceEP][destEP] + " bytes\n");
//						}
//					}
//				}
//			}
//			output.close();
//		}
//		catch (IOException e)
//		{
//			// ignore
//		}
//	}
}
