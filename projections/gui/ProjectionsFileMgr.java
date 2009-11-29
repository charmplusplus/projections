package projections.gui;

import java.io.File;
import java.io.FilenameFilter;

/** Joshua Mostkoff Unger, unger1@uiuc.edu
 *  Parallel Programming Laboratory
 * 
 *  ProjectionsFileMgr is given a list of sts fileNames, and it looks through
 *  the directory and tries to find the associated projection log files for 
 *  each sts file. */
public class ProjectionsFileMgr {
  // WHAT IF sts NOT IN CORRECT POSITION
  // WHAT IF NO ASSOCIATED LOG FILES?!?

  /** Constructor. */
  public ProjectionsFileMgr(String[] fileNames) 
  { 
    stsFiles_ = new File[fileNames.length];
    logFiles_ = new File[fileNames.length][];
    for (int i=0; i<fileNames.length; i++) {
      stsFiles_[i] = new File(fileNames[i]);
      logFiles_[i] = findFiles(fileNames[i]);
    }
  }

//  /** Constructor. files is a Vector of "File". */
//  public ProjectionsFileMgr(Vector files) 
//    throws IOException 
//  { 
//    stsFiles_ = new File[files.size()];
//    logFiles_ = new File[files.size()][];
//    for (int i=0; i<files.size(); i++) {
//      stsFiles_[i] = (File) files.elementAt(i);
//      logFiles_[i] = findFiles(stsFiles_[i].getCanonicalPath());
//    }
//  }

//  /** Print out the files that were set. */
//  public void printSts() {
//    for (int i=0; i<stsFiles_.length; i++) { 
//      //System.out.println(stsFiles_[i].getCanonicalPath());
//      for (int j=0; j<logFiles_[i].length; j++) {
//	if (logFiles_[i] != null) {
//	  //System.out.println("  "+logFiles_[i][j].getCanonicalPath());
//	}
//      }
//    }
//  }

//  /** Return the sts file at index or null if no index. */
//  public File getStsFile(int index) {
//    if (stsFiles_ != null) { return stsFiles_[index]; }
//    else { return null; }
//  }

//  /** Return the number of sts files or 0 if no files. */
//  public int getNumFiles() {
//    if (stsFiles_ != null) { return stsFiles_.length; }
//    else { return 0; }
//  }

//  /** Return pointer to files that are associated with the stsFile at
//   *  index <idx>.  Can return null.  */
//  public File[] getLogFiles(int idx) {
//    if (logFiles_ != null) { return logFiles_[idx]; }
//    else { return null; }
//  }

  /** Filter files in the directory and only get those logs associated with
   *  the STS file. */
  private File[] findFiles(String stsPathName)
  {
    File stsFile = new File(stsPathName);
    File stsDir = new File(stsFile.getParent());
    if (!stsDir.isDirectory()) { return null; }
    // calculate where to break up sts file name to do pattern matching
    String stsFileName = stsFile.getName();
    int lastDotIndex = stsFileName.lastIndexOf(".");
    int nextDotIndex = stsFileName.lastIndexOf(".", lastDotIndex-1);
    if(nextDotIndex != -1){
    	base_ = stsFileName.substring(0, nextDotIndex);
	    if (stsPathName.endsWith(".sts")) {
      // given that the sts has name like: "namd2.count.sts", find
      // all files of the name "namd2.x.count"
      	extention_ = stsFileName.substring(nextDotIndex+1, lastDotIndex);
    	}
    	else {
      // assuming that the sts has name like: "namd2.sts.count", find
      // all files of the name "namd2.x.count"
      	extention_ = stsFileName.substring(lastDotIndex+1, stsFileName.length());
    	}
    }else{
    	base_ = stsFileName.substring(0,lastDotIndex-1);
	extention_ = ".log";
    }
    String[] logFiles = stsDir.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
	return name.startsWith(base_) &&
	       name.endsWith(extention_) &&
	       name.indexOf(".sts") == -1;  // doesn't contain ".sts"
      }
    });
    File[] returnVal = new File[logFiles.length];
    for (int i=0; i<returnVal.length; i++) {
      returnVal[i] = new File(stsDir, logFiles[i]);
    }
    return returnVal;
  }

  private File[]   stsFiles_ = null;  // array of sts files (passed in)
  private File[][] logFiles_ = null;  // for each sts file, the associated logs

  String base_ = null;       // use temporarily while processing filenames
  String extention_ = null;  // use temporarily while processing filenames
}

