package projections.misc;

import projections.analysis.*;
import projections.gui.*;

import java.io.*;

/**
 *
 *  Written by Chee Wai Lee
 *  3/28/2002
 *
 *  MultiRunData is the main class encapsulating raw data read from multiple
 *  summary or log files.
 */

public class MultiRunData {

    // IO reader objects
    public GenericStsReader stsReaders[];
    // sumReaders dim 1 - indexed by set ID
    // sumReaders dim 2 - indexed by processor index
    public GenericSummaryReader sumReaders[][];

    public MultiRunData() {
    }

    /**
     *
     *  initialize is essentially a request to read a brand new set of
     *  data. This could be the initial data set or changes in datasets.
     *
     */
    public void initialize(String NbaseName, String NdataSetPathnames[],
			   boolean NisDefault, int NlogType) 
	throws IOException
    {
	if (NisDefault) {
	    NdataSetPathnames = getSubDirectories(NdataSetPathnames[0]);
	}
	if (NlogType == MultiRunController.SUMMARY) {
	    stsReaders = new GenericStsReader[NdataSetPathnames.length];
	    sumReaders = new GenericSummaryReader[NdataSetPathnames.length][];

	    for (int i=0; i<stsReaders.length; i++) {
		stsReaders[i] = 
		    new GenericStsReader(getSumStsFilename(NbaseName,
							   NdataSetPathnames[i]),
					 MainWindow.CUR_VERSION);
		sumReaders[i] = new GenericSummaryReader[stsReaders[i].numPe];
		for (int j=0; j<sumReaders[i].length; j++) {
		    sumReaders[i][j] = 
			new GenericSummaryReader(getSumFilename(NbaseName,
								NdataSetPathnames[i],
								j),
						 MainWindow.CUR_VERSION);
		}
	    }
	}
    }

    /**
     *
     *  addDataSets adds additional log data sets to the one currently
     *  loaded into AccumulatedData. This allows scaling data to be
     *  viewed and compared incrementally.
     *
     *  [[[[not to be implemented in first phase of project]]]]
     */
    public void addDataSets(String dataSetPathnames[]) 
	throws IOException
    {
    }

    /**
     *
     *  dropDataSets allows the user to drop log data sets from the
     *  ones already loaded into AccumulatedData. This aids in selective
     *  comparisons at analysis time and data generation.
     *
     *  [[[[not to be implemented in first phase of project]]]]
     */
    public void dropDataSets(int logSetIDs[]) {

    }

    private String[] getSubDirectories(String NrootPathName) 
	throws IOException
    {
	File rootPath = new File(NrootPathName);
	File subDirListing[];
	String retPathNames[];

	if (!rootPath.isDirectory()) {
	    throw new IOException("Invalid root path - " +
					  NrootPathName);
	}
	subDirListing = rootPath.listFiles(new FileFilter() 
	    {
		public boolean accept(File path) {
		    return path.isDirectory();
		}
	    });		
	if (subDirListing.length > 0) {
	    retPathNames = new String[subDirListing.length];
	    for (int i=0; i<subDirListing.length; i++) {
		retPathNames[i] = subDirListing[i].getAbsolutePath();
	    }
	} else {
	    throw new IOException("Default root has no " + 
					  "subdirectories");
	}
	return retPathNames;
    }

    private String getSumStsFilename(String NbaseName, String NlogSetPathName)
    {
	if (!NlogSetPathName.endsWith(File.separator)) {
	    NlogSetPathName += File.separator;
	}
	return NlogSetPathName + NbaseName + ".sum.sts";
    }

    private File getSumStsFile(String NbaseName, String NlogSetPathName) 
	throws IOException
    {
	String sumStsPathName = getSumStsFilename(NbaseName, NlogSetPathName);
	File sumStsFile = new File(sumStsPathName);
	if (!sumStsFile.isFile()) {
	    throw new IOException("Invalid sts file - " +
				  sumStsPathName);
	}
	return sumStsFile;
    }

    private String getSumFilename(String NbaseName, String NlogSetPathName,
				  int pe) 
    {
	if (!NlogSetPathName.endsWith(File.separator)) {
	    NlogSetPathName += File.separator;
	}
	return NlogSetPathName + NbaseName + "." + pe + ".sum";
    }

    private File getSumFile(String NbaseName, String NlogSetPathName, int pe) 
	throws IOException
    {
	String sumPathName = getSumFilename(NbaseName, NlogSetPathName, pe);
	File sumFile = new File(sumPathName);
	if (!sumFile.isFile()) {
	    throw new IOException("Invalid summary file - " +
				  sumPathName);
	}
	return sumFile;
    }
}

