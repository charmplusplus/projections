package projections.analysis;

import java.util.TreeMap;

import projections.gui.OrderedIntList;

public class LogIndex extends ProjDefs {
	
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;
	
	public TreeMap peToIndex;
	
	private String filename;
	
	private LogLoader logloader;
	
	public LogIndex(String filename, LogLoader logloader){
		this.filename = filename;
		this.logloader = logloader;
	}
	
	
	/** 
	 * Create an index of each logfile, saving it into a resulting "*.index" file.
	 * 
	 * The index contains up to 50 BEGIN_PROCESSING timestamp records and 
	 * corresponding file offsets in a sorted data structure.
	 * 
	 * @note The index is created as follows:
	 *  We seek into a small fixed number of locations in the file.
	 *  Then we advance until we find the next BEGIN_PROCESSING record.
	 *  Then we add the file offset and timestamp to the index structure.
	 *  Finally we save the index structure to a file. 
	 *  
	 * */
	public void createTimeIndexes(OrderedIntList validPEs){
//		// Load any existing indexes
//		this.loadIndex();
//
//		long BeginTime=0;
//		long EndTime = logloader.determineEndTime(validPEs);
//
//		long 	         fileLength;
//		String           Line;
//		RandomAccessFile InFile;
//		StringTokenizer  st;
//		int nPe=validPEs.size();
//
//		ProgressMonitor progressBar =
//			new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, "Creating an index to speed up future accesses to the trace files",
//					"", 0, nPe);
//		validPEs.reset();
//		int count = 1;
//		while (validPEs.hasMoreElements()) {
//			int pe = validPEs.nextElement();
//
//			if (!progressBar.isCanceled()) {
//				progressBar.setNote("[" + pe + "] " + count + 
//						" of " + nPe + " files");
//				progressBar.setProgress(count);
//			} else {
//				System.err.println("Fatal error - Something bad happened while creating time indexes to the trace files!");
//				System.exit(-1);
//			}
//			count++;
//
//			
//			// Do we already have an index for the pe ?
//			if(this.peToIndex.containsKey(pe)){
//				System.out.println("Index file already contains index for pe " + pe);
//			} else {
//				// Generate an index for the log file for the pe
//				TreeMap index = new TreeMap();
//
//				int dummyInt = 0;
//				int type = 0;
//				try {
//
//					String logFilename = MainWindow.runObject[myRun].getLogName(pe);
//					InFile = new RandomAccessFile(new File(logFilename), "r");
//					fileLength = InFile.length();
//
//					if(fileLength > 1024*256){
//						long num_samples = 40; 
//						long Time;
//
//						// For each sample
//						for(int i=0;i<num_samples;i++) { 
//							long offset = fileLength/num_samples * i;
//							InFile.seek(offset);
//							// Chomp away until we get to a new line
//							while (InFile.readByte() != '\n'){}
//
//							// Keep reading in lines until we hit a BEGIN_PROCESSING
//							while (true){
//								offset = InFile.getFilePointer();
//								Line = InFile.readLine();
//								if (Line == null) {
//									throw new EOFException();
//								}
//								st = new StringTokenizer(Line);
//
//								type=Integer.parseInt(st.nextToken());
//
//								if(type == LogLoader.BEGIN_PROCESSING){
//
//									dummyInt = Integer.parseInt(st.nextToken());
//									dummyInt = Integer.parseInt(st.nextToken());
//
//									Time = Long.parseLong(st.nextToken());
////									System.out.println("Found a BEGIN at offset "+offset+" at timestamp "+Time);
//									index.put(Time, offset);
//									break;
//								}
//							}	
//							// Now index contains a map from timestamps to offsets for pe
//							
//							
//						}
//
//					
//					InFile.close ();
//
//				}
//			} catch (EOFException e) {
//
//			} catch (IOException E) {
////				System.err.println("Couldn't read log file " + inFilename);
//			}
//
//			saveIndex(index);
//		}
//
//		progressBar.close();
//		}

	}
	

	
	
	private void saveIndex(TreeMap index){
//		try {
//			FileOutputStream fout = new FileOutputStream(outFilename);
//			ObjectOutputStream oos = new ObjectOutputStream(fout);
//			oos.writeObject(index);
//			oos.close();
//		}
//		catch (Exception e) { e.printStackTrace(); }
	}
	

	private void loadIndex(){
//		System.out.println("Attempting to open index file " + filename);
//		
//		try {
//			FileInputStream fin = new FileInputStream(filename);
//			ObjectInputStream ois = new ObjectInputStream(fin);
//
//			Object t = ois.readObject();
//			if(t instanceof TreeMap){
//				peToIndex= (TreeMap)t;
//			}
//
//			ois.close();
//		}
//		catch (Exception e) { 
//			// Do nothing
//		}

	}
	
	
	
	/** 
	 *  Lookup the offset in index file to a BEGIN_PROCESSING event before given timestamp
	 * */
	public long lookupIndexOffset(int pe, long timestamp){

//		if(peToIndex.containsKey(pe)){
//			TreeMap tm=(TreeMap)peToIndex.get(pe);
//	
//			// Get a reference to the head of the map(which will contain 
//			// keys less than the desired timestamp)
//			// Then look at the last key in that set
//			Long foundTimestamp = (Long) tm.headMap(timestamp).lastKey();
//			Long foundOffset= (Long) tm.get(foundTimestamp);
//
//			return foundOffset.longValue();
//
//		}
		
		return -1;	
		
			
	}
	
}
	
	