package projections.Tools.Histogram;



import java.io.IOException;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntry;




/** The reader threads for Histogram tool. */
class ThreadedFileReader implements Runnable  {

	private int pe;
	private long startTime;
	private long endTime;
	private int myRun = 0;

	private int timeNumBins;
	private long timeBinSize;
	private long timeMinBinSize;
	private int msgNumBins;
	private long msgBinSize;
	private long msgMinBinSize;
	private int idleNumBins;
	private long idleBinSize;
	private long idleMinBinSize;

	private double [][][] outputCounts;

    	private double [][] executionTime;
	/** Construct a file reading thread that will generate histogram data for one PE. */
	protected ThreadedFileReader(double[][][] outputCounts, int pe, long startTime, long endTime, int timeNumBins, long timeBinSize, long timeMinBinSize, int msgNumBins, long msgBinSize, long msgMinBinSize)
	{
		this.pe = pe;
		this.startTime = startTime;
		this.endTime = endTime;
		this.timeNumBins = timeNumBins;
		this.timeBinSize = timeBinSize;
		this.timeMinBinSize = timeMinBinSize;
		this.msgNumBins = msgNumBins;
		this.msgBinSize = msgBinSize;
		this.msgMinBinSize = msgMinBinSize;
		//this.idleNumBins = idleNumBins; //If this function ever needs to be used again, uncomment this and add to arguments
		//this.idleBinSize = idleBinSize;
		//this.idleMinBinSize = idleMinBinSize;
		this.outputCounts = outputCounts;	
	}

    	protected ThreadedFileReader(double[][][] outputCounts, int pe, long startTime, long endTime, int timeNumBins, long timeBinSize, long timeMinBinSize, int msgNumBins, long msgBinSize, long msgMinBinSize, int idleNumBins, long idleBinSize, long idleMinBinSize, double[][] executionTime)
	{
		this.pe = pe;
		this.startTime = startTime;
		this.endTime = endTime;
		this.timeNumBins = timeNumBins;
		this.timeBinSize = timeBinSize;
		this.timeMinBinSize = timeMinBinSize;
		this.msgNumBins = msgNumBins;
		this.msgBinSize = msgBinSize;
		this.msgMinBinSize = msgMinBinSize;
		this.idleNumBins = idleNumBins;
		this.idleBinSize = idleBinSize;
		this.idleMinBinSize = idleMinBinSize;
		this.outputCounts = outputCounts;
        	this.executionTime = executionTime;
    	}



	public void run() 
	{ 
		double [][][] myCounts = getCounts();
        	double [][] exec_time = getTotalExecTime(); 
		// in synchronized manner accumulate into global counts:
		synchronized (outputCounts)
		{
			for(int i=0; i< outputCounts.length; i++)
			{
				for(int j=0; j<outputCounts[i].length; j++)
				{
					for(int k=0; k<outputCounts[i][j].length; k++)
					{
						outputCounts[i][j][k] += myCounts[i][j][k];
					}
				}
			}
		}
        	synchronized (executionTime)
        	{
            		for(int i=0; i< executionTime[0].length; i++)
            		{
                		executionTime[0][i] += exec_time[0][i];
                		executionTime[3][i] += exec_time[3][i];
            		}
            		for(int i=0; i< executionTime[1].length; i++)
            		{
                		if(executionTime[1][i] < exec_time[1][i])
                   		executionTime[1][i] = exec_time[1][i];
            		}
            		for(int i=0; i< executionTime[2].length; i++)
            		{
                		if(executionTime[2][i] < exec_time[2][i])
                   		executionTime[2][i] = exec_time[2][i];
            		}
            
        	}
		myCounts = null;
	}



	private double[][][] getCounts()
	{
		// Variables for use with the analysis
		long executionTime;
		long adjustedTime;
		long adjustedSize;
		long totalIdleTime = 0;
		long totalStart = 0;
		long totalStop = 0;
		long idleStart = 0;

		int numEPs = MainWindow.runObject[myRun].getNumUserEntries()+1;
        	/* YH Sun added */
		double[][][] countData = new double[HistogramWindow.NUM_TYPES][][];
        	double _sun_execution_time[] = new double[numEPs];
        	for(int _i=0; _i<numEPs; _i++)
            	_sun_execution_time[_i] = 0;

		// we create an extra bin to hold overflows.
		countData[HistogramWindow.TYPE_TIME] = new double[timeNumBins+1][numEPs];
		countData[HistogramWindow.TYPE_ACCTIME] = new double[timeNumBins+1][numEPs];
		countData[HistogramWindow.TYPE_MSG_SIZE] = new double[msgNumBins+1][numEPs];
		countData[HistogramWindow.TYPE_IDLE_PERC] = new double[idleNumBins+1][numEPs];

		int curPeCount = 0;

		curPeCount++;
		GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
		try
		{
			int nestingLevel = 0;
			boolean logEnd = false;
			LogEntry prevBegin = null;
			LogEntry prevIdleBegin = null;
			
			while (true) 
			{ // EndOfLogException will terminate loop when end of log file is reached

				LogEntry logdata = reader.nextEvent(); // Scan through all events, hopefully there are no missing BEGIN_PROCESSING, or our nesting will be broken
				
				switch (logdata.type) 
				{
				case ProjDefs.BEGIN_PROCESSING:
					if (logEnd == true) break;
					nestingLevel++;
					if(nestingLevel == 1){
						prevBegin = logdata;
					}
					if (logdata.time >= endTime)//stop and end before this processing session
					{
						logEnd = true;
						//since we're ending here we also need to calculate idle percentages
						int idleTargetBin = (int)((totalIdleTime*100)/(endTime-totalStart));
						idleTargetBin = (int)(idleTargetBin - idleMinBinSize);
						if (idleTargetBin >= 0)
						{
							idleTargetBin = (int)(idleTargetBin/idleBinSize);
							if (idleTargetBin >= idleNumBins)
							{
								idleTargetBin = idleNumBins;
							}
							countData[HistogramWindow.TYPE_IDLE_PERC][idleTargetBin][0] += 1.0;
							break;
						}
						
					}
					break;

                		case ProjDefs.END_PROCESSING:
					if (logEnd == true) break;
					nestingLevel--;
					if(nestingLevel == 0)
					{
						if(logdata.time >= startTime && logdata.time <= endTime)//startTime <= endProcessing <= endTime
						{
							if (prevBegin.time <= startTime)//beginProcessing <= startTime <= endProcessing <= endTime
							{
								prevBegin.time = startTime;//change prevBin.time to be startTime
							}
							executionTime = logdata.time - prevBegin.time;
							adjustedTime = executionTime - timeMinBinSize;
							// respect user threshold
							if (adjustedTime >= 0)
							{
								int targetBin = (int)(adjustedTime/timeBinSize);
								if (targetBin >= timeNumBins)
								{
									targetBin = timeNumBins;
								}
								countData[HistogramWindow.TYPE_TIME][targetBin][logdata.entry] += 1.0;
								countData[HistogramWindow.TYPE_ACCTIME][targetBin][logdata.entry] += executionTime;
							}
                            				_sun_execution_time[logdata.entry] += executionTime;
						}
						else if (logdata.time >= endTime)//startTime <= endTime <= endProcessing
						{
							if (startTime >= prevBegin.time)//beginProcessing <= startTime <= endTime <= endProcessing
							{
								prevBegin.time = startTime;
							}
							executionTime = endTime - prevBegin.time;
							adjustedTime = executionTime - timeMinBinSize;
							// respect user threshold
							if (adjustedTime >= 0)
							{
								int targetBin = (int)(adjustedTime/timeBinSize);
								if (targetBin >= timeNumBins)
								{
									targetBin = timeNumBins;
								}
								countData[HistogramWindow.TYPE_TIME][targetBin][logdata.entry] += 1.0;
								countData[HistogramWindow.TYPE_ACCTIME][targetBin][logdata.entry] += executionTime;
							}
                            				_sun_execution_time[logdata.entry] += executionTime;
							logEnd = true;
							//since we're ending here we also have to calculate idle percentages
							int idleTargetBin = (int)((totalIdleTime*100)/(endTime-totalStart));
							idleTargetBin = (int)(idleTargetBin - idleMinBinSize);
							if (idleTargetBin >= 0)
							{
								idleTargetBin = (int)(idleTargetBin/idleBinSize);
								if (idleTargetBin >= idleNumBins)
								{
									idleTargetBin = idleNumBins;
								}
								countData[HistogramWindow.TYPE_IDLE_PERC][idleTargetBin][0] += 1.0;
								break;
							}
						}
						prevBegin = null;	
					} 
					else if(nestingLevel < 0)
					{
						nestingLevel = 0; // Reset to 0 because we didn't get to see an appropriate matching BEGIN_PROCESSING.
						prevBegin = null;
					}
					break;
                
				case ProjDefs.BEGIN_IDLE:
					if (logEnd == true) break;
					prevIdleBegin = logdata;
					idleStart = prevIdleBegin.time;
					if (logdata.time >= endTime)
					{
						logEnd = true;
						//stop before we begin this idle session
						int idleTargetBin = (int)((totalIdleTime*100)/(endTime-totalStart));
						idleTargetBin = (int)(idleTargetBin - idleMinBinSize);
						if (idleTargetBin >= 0)
						{
							idleTargetBin = (int)(idleTargetBin/idleBinSize);
							if (idleTargetBin >= idleNumBins)
							{
								idleTargetBin = idleNumBins;
							}
							countData[HistogramWindow.TYPE_IDLE_PERC][idleTargetBin][0] += 1.0;
							break;
						}
					}
					break;
                

				case ProjDefs.END_IDLE:
					if (logEnd == true) break;
                    			if (logdata.time >= startTime && logdata.time <= endTime)//startTime <= endIdle <= endTime
					{
						if (idleStart <= startTime)//idleStart <= startTime <= endIdle <= endTime
						{
							prevIdleBegin.time = startTime;
						}
						executionTime = logdata.time - prevIdleBegin.time;
                        			adjustedTime = executionTime - timeMinBinSize;
                        			// respect user threshold
                        			if (adjustedTime >= 0)
						{
                            				int targetBin = (int)(adjustedTime/timeBinSize);
                            				if (targetBin >= timeNumBins)
							{
                                				targetBin = timeNumBins;
                            				}
                            				countData[HistogramWindow.TYPE_TIME][targetBin][numEPs-1] += 1.0;
                            				countData[HistogramWindow.TYPE_ACCTIME][targetBin][numEPs-1] += executionTime;
						}
                        			_sun_execution_time[logdata.entry] += executionTime;
                        			//System.out.println("idle time is " + executionTime );
						//now consider idle percentage calculations
						if (idleStart <= startTime)//beginIdle <= startTime <= endIdle <= endTime
						{
							idleStart = startTime;
							totalStart = startTime;
						}
						totalIdleTime += (logdata.time - idleStart);//startTime <= beginIdle <= endIdle <= endTime
					}
					else if (logdata.time >= endTime)//startTime <= endTime <= endIdle
					{
						if (startTime >= idleStart)//beginIdle <= startTime <= endTime <= endIdle
						{
							idleStart = startTime;
							totalStart = startTime;
						}
						executionTime = endTime - idleStart;
						adjustedTime = executionTime - timeMinBinSize;
						// respect user threshold
						if (adjustedTime >= 0)
						{
							int targetBin = (int)(adjustedTime/timeBinSize);
							if (targetBin >= timeNumBins)
							{
								targetBin = timeNumBins;
							}
							countData[HistogramWindow.TYPE_TIME][targetBin][numEPs-1] += 1.0;
							countData[HistogramWindow.TYPE_ACCTIME][targetBin][numEPs-1] += executionTime;
							}
                            			_sun_execution_time[logdata.entry] += executionTime;
						logEnd = true;
						totalIdleTime += (endTime - idleStart);
						int idleTargetBin = (int)((totalIdleTime*100)/(endTime - totalStart));
						idleTargetBin = (int)(idleTargetBin - idleMinBinSize);
						if (idleTargetBin >= 0)
						{
							idleTargetBin = (int)(idleTargetBin/idleBinSize);
							if (idleTargetBin >= idleNumBins)
							{
								idleTargetBin = idleNumBins;
							}
							countData[HistogramWindow.TYPE_IDLE_PERC][idleTargetBin][0] += 1.0;
							break;
						}
					}
					else //(startTime >= logdata.time)//endIdle <= startTime <= endTime
					{
						//nothing is added to idle times because OST hasnt started yet
					}
					prevIdleBegin = null;
					break;	

				case ProjDefs.CREATION:
					//if (logEnd == true) break;
					if (logdata.time < startTime || logdata.time > endTime) {
						break;
					}
					// respect the user threshold.
					adjustedSize = logdata.msglen - msgMinBinSize;
					if (adjustedSize >= 0)
					{
						int targetBin = (int)(adjustedSize/msgBinSize);
						if (targetBin >= msgNumBins)
						{
							targetBin = msgNumBins;
						}
						countData[HistogramWindow.TYPE_MSG_SIZE][targetBin][logdata.entry]+=1.0;
					}
					break;

				case ProjDefs.END_COMPUTATION:
					if (logEnd == true) break;
					if (logdata.time >= endTime)
					{
						//just change totalStop to optionalEndTime since processor was not idle at this line
						int idleTargetBin = (int)((totalIdleTime*100)/(endTime-totalStart));
						idleTargetBin = (int)(idleTargetBin - idleMinBinSize);
						if (idleTargetBin >= 0)
						{
							idleTargetBin = (int)(idleTargetBin/idleBinSize);
							if (idleTargetBin >= idleNumBins)
							{
								idleTargetBin = idleNumBins;
							}
							countData[HistogramWindow.TYPE_IDLE_PERC][idleTargetBin][0] += 1.0;
						}
					}
					else if (logdata.time <= startTime)
					{
						//we were never able to start, so totalIdleTime is zero
						int idleTargetBin = (int)(0-idleMinBinSize);
						if (idleTargetBin >= 0)
						{
							idleTargetBin = (int)(idleTargetBin/idleBinSize);
							if (idleTargetBin >= idleNumBins)
							{
								idleTargetBin = idleNumBins;
							}
							countData[HistogramWindow.TYPE_IDLE_PERC][idleTargetBin][0] += 1.0;
						}
					}
					else
					{
						int idleTargetBin = (int)((totalIdleTime*100)/(logdata.time-totalStart));
						idleTargetBin = (int)(idleTargetBin - idleMinBinSize);
						if (idleTargetBin >= 0)
						{
							idleTargetBin = (int)(idleTargetBin/idleBinSize);
							if (idleTargetBin >= idleNumBins)
							{
								idleTargetBin = idleNumBins;
							}
							countData[HistogramWindow.TYPE_IDLE_PERC][idleTargetBin][0] += 1.0;
						}
					}
					break;

				case ProjDefs.BEGIN_COMPUTATION:
					if (logEnd == true) break;
					totalStart = logdata.time;
					if (logdata.time >= endTime)
					{
						logEnd = true;
						//since processor is starting after endtime, its total idle time must be zero
						int idleTargetBin = (int)(0 - idleMinBinSize);
						if (idleTargetBin >= 0)
						{
							idleTargetBin = (int)(idleTargetBin/idleBinSize);
							if (idleTargetBin >= idleNumBins)
							{
								idleTargetBin = idleNumBins;
							}
							countData[HistogramWindow.TYPE_IDLE_PERC][idleTargetBin][0] += 1.0;
							break;
						}
					}
					else if (logdata.time <= startTime)
					{
						totalStart = startTime;
					}					
					break;
				}//end switch			
			}
		} 
		catch(EndOfLogSuccess e)
		{
			// successfully reached end of log file
		} 
		catch(Exception e) 
		{
			System.err.println("Exception " + e);
			e.printStackTrace();
			System.exit(-1);
		}

		try
		{
			reader.close();
		} 
		catch (IOException e1)
		{
			System.err.println("Error: could not close log file reader for processor " + pe );
		}

		return countData;
	}

    	private double[][] getTotalExecTime()
	{
		// Variables for use with the analysis
		long executionTime;

		int numEPs = MainWindow.runObject[myRun].getNumUserEntries()+1;
        	/* YH Sun added */
        	double _sun_execution_time[][] = new double[4][numEPs];

		int curPeCount = 0;

		curPeCount++;
		GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
		try
		{
			int nestingLevel = 0;
			LogEntry prevBegin = null;
			LogEntry prevIdleBegin = null;
			
			while (true)
			{ // EndOfLogException will terminate loop when end of log file is reached

				LogEntry logdata = reader.nextEvent(); // Scan through all events, hopefully there are no missing BEGIN_PROCESSING, or our nesting will be broken

				switch (logdata.type) {
				case ProjDefs.BEGIN_PROCESSING:
					nestingLevel++;
					if(nestingLevel == 1)
					{
						prevBegin = logdata;
					}
					break;

				case ProjDefs.END_PROCESSING:
					nestingLevel--;
					if(nestingLevel == 0)
					{
						if(logdata.time >= startTime && logdata.time <= endTime)
						{
							executionTime = logdata.time - prevBegin.time;
                            				_sun_execution_time[0][logdata.entry] += executionTime;
                            				if(_sun_execution_time[1][logdata.entry] < executionTime)
                                			_sun_execution_time[1][logdata.entry] = executionTime;
							if(_sun_execution_time[2][logdata.entry] > executionTime)
                                			_sun_execution_time[2][logdata.entry] = executionTime;
                            				_sun_execution_time[3][logdata.entry]++;
						}
						prevBegin = null;	
					} 
					else if(nestingLevel < 0)
					{
						nestingLevel = 0; // Reset to 0 because we didn't get to see an appropriate matching BEGIN_PROCESSING.
						prevBegin = null;
					}
					break;

                		case ProjDefs.BEGIN_IDLE:
					prevIdleBegin = logdata;
					break;

                		case ProjDefs.END_IDLE:
					if (prevIdleBegin == null) break;
                    			if(logdata.time >= startTime && logdata.time <= endTime)
					{
                        			executionTime = logdata.time - prevIdleBegin.time;
                        			_sun_execution_time[0][numEPs-1] += executionTime;
                        			if(_sun_execution_time[1][numEPs-1] < executionTime)
                            			_sun_execution_time[1][numEPs-1] = executionTime;
                        			if(_sun_execution_time[2][numEPs-1] > executionTime)
                            			_sun_execution_time[2][numEPs-1] = executionTime;
                        			_sun_execution_time[3][numEPs-1]++;
                        			//System.out.println("idle time is " + executionTime );
                    			}
                    			prevIdleBegin = null;
					break;


				case ProjDefs.CREATION:
					if (logdata.time > endTime)
					{
						break;
                    			}
					break;
				}
			}
		} 
		catch(EndOfLogSuccess e)
		{
			// successfully reached end of log file
		}
		catch(Exception e)
		{
			System.err.println("Exception " + e);
			e.printStackTrace();
			System.exit(-1);
		}

		try
		{
			reader.close();
		} 
		catch (IOException e1) 
		{
			System.err.println("Error: could not close log file reader for processor " + pe );
		}
	    
		return _sun_execution_time;
	}

}





