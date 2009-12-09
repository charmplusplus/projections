package projections.analysis;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import projections.gui.MainWindow;
import projections.misc.FileUtils;

/** A class that provides the directory paths to the GenericLogReader class, load balancing between them. */
public class GenericLogReaderBalancer {
	int myRun = 0;
	
	Map<String, Integer> directories; //< Number of open files for each directory
	
	public GenericLogReaderBalancer(){
		directories = new TreeMap();
	}
	
	public void init(){
		directories.put(MainWindow.runObject[myRun].getLogDirectory(), new Integer(0));
//		directories.put("/expand/home/idooley2/ProjectionsLogs/W256_MIN_TOPO_8192VN/", new Integer(0));
	}
	
	public String acquireLogName(int pe){
		int minOpenFound = Integer.MAX_VALUE;
		String minOpenBaseName = "";
		synchronized(directories){
			Iterator<String> iter = directories.keySet().iterator();
			while(iter.hasNext()){
				String baseName = iter.next();
				Integer numOpen = directories.get(baseName);
				if(numOpen < minOpenFound){
					minOpenBaseName = baseName;
					minOpenFound = numOpen;
				}
			}
			directories.put(minOpenBaseName, new Integer(minOpenFound+1));
		}
		minOpenBaseName += MainWindow.runObject[myRun].getLogWithoutExtensionOrDirectory();
		return FileUtils.getCanonicalFileName(minOpenBaseName, pe, ProjMain.LOG);
	}

	public void releaseLogName(String logName){
		synchronized(directories){
			String dir = FileUtils.dirFromFile(logName);
			Integer count = directories.get(dir);
			directories.put(dir, count-1);	
		}
//		System.out.println("Releasing " + logName);
	}
	
}
