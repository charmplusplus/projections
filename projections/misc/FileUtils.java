package projections.misc;

import java.io.*;

import projections.analysis.*;
import projections.gui.*;

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

    public static final int NUM_TYPES = 5;
    public static final int LOG = 0;
    public static final int SUMMARY = 1;
    public static final int COUNTER = 2;
    public static final int SUMDETAIL = 3;
    public static final int DOP = 4;

    private static boolean hasSum;
    private static boolean hasSumDetail;
    private static boolean hasSumAccumulated;
    private static boolean hasLog;
    private static boolean hasPoseDop;    

    private static OrderedIntList validPEs[];
    private static String validPEStrings[];

    public static String getBaseName(String filename) {
	String baseName = null;
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
	if (index != -1) {
	    return filename.substring(0,index);
	}
	return(".");	// present directory
    }

    public static void detectFiles(StsReader sts, String baseName) {
	// determine if any of the data files exist.
	// We assume they are automatically valid and this is reflected
	// in the validPEs. 
	// **FIXME** This is expensive with large numbers of processors!
	//           Use rc-type information to do this once per dataset
	//           lifetime.

	hasLog = false;
	hasSum = false;
	hasSumDetail = false;
	hasSumAccumulated = false;
	hasPoseDop = false;

	validPEs = new OrderedIntList[NUM_TYPES];
	validPEStrings = new String[NUM_TYPES];
	for (int i=0; i<NUM_TYPES; i++) {
	    validPEs[i] = new OrderedIntList();
	}

	for (int i=0;i<sts.getProcessorCount();i++) {
	    if ((new File(getSumName(baseName, i))).isFile()) {
		hasSum = true;
		validPEs[SUMMARY].insert(i);
	    }
	    if ((new File(getSumDetailName(baseName, i))).isFile()) {
		hasSumDetail = true;
		validPEs[SUMDETAIL].insert(i);
	    }
	    if ((new File(getLogName(baseName, i))).isFile()) {
		hasLog = true;
		validPEs[LOG].insert(i);
	    }
	    if ((new File(getPoseDopName(baseName, i))).isFile()) {
		hasPoseDop = true;
		validPEs[DOP].insert(i);
	    }
	}
	for (int type=0; type<NUM_TYPES; type++) {
	    validPEStrings[type] = validPEs[type].listToString();
	}
	if ((new File(getSumAccumulatedName(baseName))).isFile()) {
	    hasSumAccumulated = true;
	}
    }
    
    public static boolean hasLogFiles() {
	return hasLog;
    }   

    public static boolean hasSumFiles() {
	return hasSum;
    }
   
    public static boolean hasSumAccumulatedFile() {
	return hasSumAccumulated;
    }

    public static boolean hasSumDetailFiles() {
	return hasSumDetail;
    }

    public static boolean hasPoseDopFiles() {
	return hasPoseDop;
    }

    public static String getLogName(String baseName, int pnum) {
	return baseName+"."+pnum+".log";
    }   

    public static String getSumName(String baseName, int pnum) {
	return baseName+"."+pnum+".sum";
    }   
    
    public static String getSumAccumulatedName(String baseName) {
	return baseName+".sum";
    }

    public static String getSumDetailName(String baseName, int pnum) {
	return baseName + "." + pnum + ".sumd";
    }

    public static String getPoseDopName(String baseName, int pnum) {
	return baseName + "." + pnum + ".poselog";
    }

    public static OrderedIntList getValidProcessorList(int type) {
	switch (type) {
	case LOG:
	    if (!hasLog) {
		System.err.println("Warning: No log files.");
	    }
	    break;
	case SUMMARY:
	    if (!hasSum) {
		System.err.println("Warning: No summary files.");
	    }
	    break;
	case SUMDETAIL:
	    if (!hasSumDetail) {
		System.err.println("Warning: No summary detail files.");
	    }
	    break;
	case DOP:
	    if (!hasPoseDop) {
		System.err.println("Warning: No poselog files found.");
	    }
	    break;
	}
	return validPEs[type];
    }

    public static String getValidProcessorString(int type) {
	switch (type) {
	case LOG:
	    if (!hasLog) {
		System.err.println("Warning: No log files.");
	    }
	    break;
	case SUMMARY:
	    if (!hasSum) {
		System.err.println("Warning: No summary files.");
	    }
	    break;
	case SUMDETAIL:
	    if (!hasSumDetail) {
		System.err.println("Warning: No summary detail files.");
	    }
	    break;
	case DOP:
	    if (!hasPoseDop) {
		System.err.println("Warning: No poselog files found.");
	    }
	    break;
	}
	return validPEStrings[type];
    }

}
