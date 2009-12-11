package projections.misc;

import java.io.File;

import projections.analysis.ProjMain;
import projections.gui.OrderedIntList;

/**
 *  FileUtils.java
 *  7/21/2006
 *
 *  Static class for handling all things related to supported files (NOTE:
 *  *not* data within files!)
 *
 *  **CW** Further refactoring efforts should ensure that FileUtils performs
 *  tasks only for global static information. Run Objects should take care
 *  of run-specific file information.
 */

public class FileUtils {

	private static OrderedIntList validPEs[];
	private static String validPEStrings[];
	private static boolean hasFiles[];

	
	public static String getBaseName(String filename) {
		String baseName = null;
		System.out.println("filename = " + filename);
		if (filename.endsWith(".sum.sts")) {
			baseName = filename.substring(0, filename.length()-8);
		} else if (filename.endsWith(".sts")) {
			baseName = filename.substring(0, filename.length()-4); 
		} else {
			System.err.println("Invalid sts filename! Exiting ...");
			System.exit(-1);
		}
		return baseName;
	}

	public static String dirFromFile(String filename) {
		// pre condition - filename is a full path name
		int index = filename.lastIndexOf(File.separator);
		if (index > -1) {
			return filename.substring(0,index);
		}
		return("./");	// present directory
	}

	
	public static String withoutDir(String filename) {
		// pre condition - filename is a full path name
		int index = filename.lastIndexOf(File.separator);
		if (index != -1) {
			return filename.substring(index,filename.length());
		}
		return(filename);
	}

	
	public static void detectFiles(String baseName) {
		// determine if any of the data files exist.
		// We assume they are automatically valid and this is reflected
		// in the validPEs. 
		hasFiles = new boolean[ProjMain.NUM_TYPES];
		validPEs = new OrderedIntList[ProjMain.NUM_TYPES];
		validPEStrings = new String[ProjMain.NUM_TYPES];

		for (int type=0; type<ProjMain.NUM_TYPES; type++) {
			validPEs[type] = new OrderedIntList();

			detectFiles(baseName, type);
			validPEStrings[type] = validPEs[type].listToString();
		}
	}

	/** Scan through all files in the directory, looking for things that might be log files. */
	private static void detectFiles(String baseName, int type) {
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

		File prefix = new File( baseName );
		String prefix_s = prefix.getName();
		
		testFile = new File( FileUtils.dirFromFile(baseName) );
		if (!testFile.isDirectory()) {
			System.err.println("Internal Error: Path [" + baseName + "] " +
					"supplied for file detection is not a " +
			"directory! Please report to developers!");
			System.exit(-1);
		}
		String files[] = testFile.list();
		for (int file=0; file<files.length; file++) {
			String filename = files[file];
			String extension = getTypeExtension(type);

			if(filename.startsWith(prefix_s)){
				String[] splits = filename.split("\\.");
				int numSplits = splits.length;

				if(splits[numSplits-1].equals(extension) ){
					int pe = Integer.parseInt(splits[numSplits-2]);
					validPEs[type].insert(pe);
					hasFiles[type] = true;
//					System.out.println("Found " + extension + " for pe " + pe);
				} else if(splits[numSplits-2].equals(extension)  &&  splits[numSplits-1].equals("gz") ){
					int pe = Integer.parseInt(splits[numSplits-3]);
					validPEs[type].insert(pe);
					hasFiles[type] = true;
	//				System.out.println("Found " + extension + ".gz for pe " + pe);
				} else {
					// The file does not appear to match the desired names
				}

			}	


		}
	}

    public static boolean hasLogFiles() {
	return hasFiles[ProjMain.LOG];
    }   

    public static boolean hasSumFiles() {
	return hasFiles[ProjMain.SUMMARY];
    }
   
    public static boolean hasSumAccumulatedFile() {
	return hasFiles[ProjMain.SUMACC];
    }

    public static boolean hasSumDetailFiles() {
	return hasFiles[ProjMain.SUMDETAIL];
    }

    public static boolean hasPoseDopFiles() {
	return hasFiles[ProjMain.DOP];
    }

    public static String getCanonicalFileName(String baseName, int pnum, int type){
    	return baseName + "." + pnum + "." + getTypeExtension(type);
    }


    private static String getSumAccumulatedName(String baseName) {
    	return baseName+".sum";
    }

    private static String getTypeExtension(int type) {
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

    public static OrderedIntList getValidProcessorList(int type) {
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

    public static String getValidProcessorString(int type) {
	return getValidProcessorList(type).listToString();
    }

}
