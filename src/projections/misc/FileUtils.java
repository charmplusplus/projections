package projections.misc;

import java.io.File;
import java.util.TreeMap;

import projections.analysis.ProjMain;
import projections.gui.OrderedIntList;

/**
 *  FileUtils.java
 *
 *  Provides log files to the log readers.
 *
 *	Note: Originally this class just handled the names of the files, but it is in transition to supply File objects.
 *
 */

public class FileUtils {

	private OrderedIntList validPEs[];
	private String validPEStrings[];
	private boolean hasFiles[];
	
	private String filename;
	private String baseName;
	
	
	public String getBaseName(){
		return baseName;
	}
	
	/** The file for the log for each specified pe */
	private TreeMap<Integer, File> logFiles;
	
	public FileUtils(String filename){
		this.filename = filename;
		System.out.println("FileUtils created with filename: " + filename);	
	
		// Extract base name
		if (filename.endsWith(".sum.sts")) {
			baseName = filename.substring(0, filename.length()-8);
		} else if (filename.endsWith(".sts")) {
			baseName = filename.substring(0, filename.length()-4); 
		} else {
			System.err.println("Invalid sts filename! Exiting ...");
			System.exit(-1);
		}
		System.out.println("FileUtils extracted basename: " + baseName);	
		
		
		detectFiles();
	
	}
	
	
	public String getProjRCName(){
	    return baseName + ".projrc";
	}

	public String dirFromFile() {
		// pre condition - filename is a full path name
		int index = baseName.lastIndexOf(File.separator);
		if (index > -1) {
			return baseName.substring(0,index);
		}
		return("./");	// present directory
	}

	
	public String withoutDir() {
		// pre condition - filename is a full path name
		int index = baseName.lastIndexOf(File.separator);
		if (index != -1) {
			return baseName.substring(index,baseName.length());
		}
		return(baseName);
	}

	
	private void detectFiles() {
		// determine if any of the data files exist.
		// We assume they are automatically valid and this is reflected
		// in the validPEs. 
		hasFiles = new boolean[ProjMain.NUM_TYPES];
		validPEs = new OrderedIntList[ProjMain.NUM_TYPES];
		validPEStrings = new String[ProjMain.NUM_TYPES];


		// Scan for log files and record what we find
		logFiles = new TreeMap<Integer, File>();
		
		
		for (int type=0; type<ProjMain.NUM_TYPES; type++) {
			validPEs[type] = new OrderedIntList();

			detectFiles(type);
			validPEStrings[type] = validPEs[type].listToString();
		}
		
		
		System.out.println("Found " + logFiles.size() + " log files");
		
	}

	/** Scan through all files in the directory, looking for things that might be log files. */
	private void detectFiles(int type) {
		File testFile = null;
		// special condition for SUMACC (and any future, single-file
		// log types) only
		if (type == ProjMain.SUMACC) {
			testFile = new File(getSumAccumulatedName(baseName));
			if (testFile.isFile() &&
					testFile.length() > 0 &&
					testFile.canRead()) {
				hasFiles[type] = true;
			}
			return;
		}
		
		findFilesInDirectory(new File( dirFromFile() ), type);
			
	}
	
	
	/** a recursive function to find log files in the directory or subdirectories */
	private void findFilesInDirectory(File myDir, int type){
		if(! myDir.isDirectory()){
			System.err.println("Internal Error: Path [" + myDir.getAbsolutePath() + "] " +
					"supplied for file detection is not a " +
			"directory! Please report to developers!");
			System.exit(-1);
		}
		
		File prefix = new File( baseName );
		String prefix_s = prefix.getName();
		
//		System.out.println("FileUtils.dirFromFile(baseName) = " + FileUtils.dirFromFile(baseName) );
		File files[] = myDir.listFiles();
		for (int i=0; i<files.length; i++) {
			File f = files[i];
			String filename = f.getName();
			String extension = getTypeExtension(type);

//			System.out.println("Examining " + filename);
			
			if(filename.startsWith(prefix_s)){
				
//				System.out.println("File "+ filename + " does start with " + prefix_s);
				
				if(f.isDirectory()){
					
					String[] splits = filename.split("\\.");
					int numSplits = splits.length;
					if(numSplits > 1 && splits[numSplits-2].equals("projdir")  ){
						
						if(type == ProjMain.LOG){
							System.out.println("Looking for logs in subdirectory: " + f.getAbsolutePath());
						}
						
						// Look inside the directory
						findFilesInDirectory(f, type);
					}
					
				} else if(f.isFile()) {
					String[] splits = filename.split("\\.");
					int numSplits = splits.length;
					if(numSplits > 1){
						if(splits[numSplits-1].equals(extension) ){
							int pe = Integer.parseInt(splits[numSplits-2]);
							validPEs[type].insert(pe);
							hasFiles[type] = true;
							logFiles.put(pe, f);
							//						System.out.println("Found " + extension + " for pe " + pe);
						} else if(splits[numSplits-2].equals(extension)  &&  splits[numSplits-1].equals("gz") ){
							int pe = Integer.parseInt(splits[numSplits-3]);
							validPEs[type].insert(pe);
							hasFiles[type] = true;
							logFiles.put(pe, f);
							//						System.out.println("Found " + extension + ".gz for pe " + pe);
						} else {
							// The file does not appear to match the desired names
						}
					}
				}
			} else {
//				System.out.println("File "+ filename + " does not start with " + prefix_s);
			}

		}
	}

    public boolean hasLogFiles() {
	return hasFiles[ProjMain.LOG];
    }   

    public boolean hasSumFiles() {
	return hasFiles[ProjMain.SUMMARY];
    }
   
    public boolean hasSumAccumulatedFile() {
	return hasFiles[ProjMain.SUMACC];
    }

    public boolean hasSumDetailFiles() {
	return hasFiles[ProjMain.SUMDETAIL];
    }

    public boolean hasPoseDopFiles() {
	return hasFiles[ProjMain.DOP];
    }

    /** @TODO: Make this private so that the names don't leak out of here to be used in bad ways */
    public String getCanonicalFileName(int pnum, int type){
    	return getBaseName() + "." + pnum + "." + getTypeExtension(type);
    }


    private String getSumAccumulatedName(String baseName) {
    	return baseName+".sum";
    }

    private String getTypeExtension(int type) {
	String fileExt = null;
	switch (type) {
	case ProjMain.SUMMARY:
	    fileExt = "sum";
	    break;
	case ProjMain.SUMDETAIL:
	    fileExt = "sumd";
	    break;
	case ProjMain.LOG:
	    fileExt = "log";
	    break;
	case ProjMain.DOP:
	    fileExt = "poselog";
	    break;
	default:
	    System.err.println("Internal Error: Unknown file type " +
			       "index " + type);
	    System.exit(-1);
	}
	return fileExt;
    }

    public OrderedIntList getValidProcessorList(int type) {
	String errorMsg = "";
	switch (type) {
	case ProjMain.LOG:
	    errorMsg = "Warning: No log files.";
	    break;
	case ProjMain.SUMMARY:
	    errorMsg = "Warning: No summary files.";
	    break;
	case ProjMain.SUMDETAIL:
	    errorMsg = "Warning: No summary detail files.";
	    break;
	case ProjMain.DOP:
	    errorMsg = "Warning: No poselog files found.";
	    break;
	default:
	    System.err.println("Internal Error: Unsupported log type " +
			       "index " + type + " for valid processor " +
			       "info.");
	    System.exit(-1);

	}
	if (!hasFiles[type]) {
	    System.err.println(errorMsg);
	}
	return validPEs[type];
    }

    public String getValidProcessorString(int type) {
	return getValidProcessorList(type).listToString();
    }

    /** Return a File for the log for a PE */
    public File getLogFile(int pe){
    	return logFiles.get(pe);
    }
    
    
}
