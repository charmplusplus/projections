package projections.analysis;

public class KMeansClustering {

    // clusterMap and clusterDistance are meant to be output arrays    
    public static void kMeans(double data[][], int numSamples,
			      int clusterMap[], double clusterDistance[]) {
	double mean[][];
	double oldMean[][];
	double distance[];
	int clusterCounts[];
	double clusterMinBound[][];
    
	int numProcs = data.length;
	int numEPs = data[0].length;
	
	// initialization O(p + ep)
	mean = new double[numSamples][numEPs];
	oldMean = new double[numSamples][numEPs];
	distance = new double[numSamples];
	clusterCounts = new int[numSamples];
	clusterMinBound = new double[numSamples][numEPs];
	
	// Place initial mean values O(ep * p)
	double minVal;
	double maxVal;
	double interval;
	
	for (int ep=0; ep<numEPs; ep++) {
	    minVal = Double.MAX_VALUE;
	    maxVal = Double.MIN_VALUE;
	    for (int p=0; p<numProcs; p++) {
		if (data[p][ep] < minVal) {
		    minVal = data[p][ep];
		}
		if (data[p][ep] > maxVal) {
		    maxVal = data[p][ep];
		}
	    }
	    interval = ((double)(maxVal - minVal + 1))/numSamples;
	    for (int k=0; k<numSamples; k++) {
		mean[k][ep] = (k+1)*interval - interval/2 + minVal;
	    }
	}
	
	// Main Algorithm Loop
	while (checkMean(mean, oldMean)) {
	    // printMean(mean);
	    
	    // assign samples
	    double minDist;
	    int minSample;
	    double tempDist;
	    
	    for (int k=0; k<numSamples; k++) {
		clusterCounts[k] = 0;
	    }
	    
	    for (int p=0; p<numProcs; p++) {
		minDist = Double.MAX_VALUE;
		minSample = 0;
		// compute distance from means
		for (int k=0; k<numSamples; k++) {
		    tempDist = 0.0;
		    for (int ep=0; ep<numEPs; ep++) {
			tempDist += Math.pow(data[p][ep] - mean[k][ep], 2.0);
		    }
		    tempDist = Math.sqrt(tempDist);
		    if (tempDist < minDist) {
			minDist = tempDist;
			minSample = k;
		    }
		}
		clusterCounts[minSample]++;
		clusterMap[p] = minSample;
		clusterDistance[p] = minDist;
	    }
	    
	    // Recompute mean
	    // get bounds, store min in clusterMinBound, abuse mean to store
	    // the max bounds.
	    for (int k=0; k<numSamples; k++) {
		for (int ep=0; ep<numEPs; ep++) {
		    clusterMinBound[k][ep] = Double.MAX_VALUE;
		    mean[k][ep] = Double.MIN_VALUE;
		}
	    }
	    for (int p=0; p<numProcs; p++) {
		for (int ep=0; ep<numEPs; ep++) {
		    if (data[p][ep] < clusterMinBound[clusterMap[p]][ep]) {
			clusterMinBound[clusterMap[p]][ep] = 
			    data[p][ep];
		    }
		    if (data[p][ep] > mean[clusterMap[p]][ep]) {
			mean[clusterMap[p]][ep] = data[p][ep];
		    }
		}
	    }
	    for (int k=0; k<numSamples; k++) {
		for (int ep=0; ep<numEPs; ep++) {
		    if (clusterCounts[k] > 0) {
			mean[k][ep] = 
			    (mean[k][ep] - clusterMinBound[k][ep])/2 +
			    clusterMinBound[k][ep];
		    } else {
			// have to rewrite because we blew the old value away
			mean[k][ep] = oldMean[k][ep];
		    }
		}
	    }
	    // outputResults(clusterMap, numSamples);
	}
    }
    
    // Checks for change and at the same time, update oldMeans as a 
    // side-effect
    public static boolean checkMean(double mean[][], double oldMean[][]) {
	boolean returnVal = false;
	// compare the contents of mean and oldMean
	for (int k=0; k<mean.length; k++) {
	    for (int ep=0; ep<mean[k].length; ep++) {
		if (mean[k][ep] != oldMean[k][ep]) {
		    returnVal = true;
		    oldMean[k][ep] = mean[k][ep];
		}
	    }
	}
	return returnVal;
    }
    
    public static void printMean(double mean[][]) {
	System.out.println("Mean:");
	System.out.println("-----");
	for (int k=0; k<mean.length; k++) {
	    System.out.print("[" + k + "]; (");
	    for (int ep=0; ep<mean[k].length; ep++) {
		System.out.print(" " + mean[k][ep]);
	    }
	    System.out.println(" )");
	}
    }
    
    public static void outputResults(int clusterMap[], int numSamples) {
	System.out.println("Cluster Map:");
	System.out.println("------------");
	for (int k=0; k<numSamples; k++) {
	    System.out.print("["+ k + "]: ");
	    for (int p=0; p<clusterMap.length; p++) {
		if (clusterMap[p] == k) {
		    System.out.print(p + " ");
		}
	    }
	    System.out.println();
	}
    }

    public static void main(String args[]) {
	int numSamples = 5;
	double data[][] = { {0,5}, {1,1}, {1,2}, {1,4}, {2,1},
			  {2,7}, {3,6}, {3,8}, {4,9}, {5,2},
			  {5,7}, {6,1}, {7,2}, {9,4}, {9,5} };
	int clusterMap[] = new int[data.length];
	double clusterDistance[] = new double[data.length];
	KMeansClustering.kMeans(data, numSamples, clusterMap, 
				clusterDistance);
	outputResults(clusterMap, numSamples);
    }
}
