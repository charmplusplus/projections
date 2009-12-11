package projections.analysis;

import java.util.Random;

public class KMeansClustering {

    // clusterMap and distanceFromClusterMean are meant to be output arrays    
    public static void kMeans(double in_data[][], int numClusters,
			      int clusterMap[], 
			      double distanceFromClusterMean[]) {

	double data[][] = normalize(in_data);
    
	int numProcs = data.length;
	int numMetrics = data[0].length;

	double centroids[][];
	double newCentroidVectors[][];
	int clusterCounts[];
	boolean clusterChange[];

	// initialization
	centroids = new double[numClusters][numMetrics];
	newCentroidVectors = new double[numClusters][numMetrics];
	clusterCounts = new int[numClusters];
	clusterChange = new boolean[numClusters];
	for (int p=0; p<numProcs; p++) {
	    clusterMap[p] = -1; // initialize with out-of-bound values
	}

	// set up initial centroid placement
	setCentroidRandom(centroids, numClusters, data);

	// initially, everything has changed.
	for (int k=0; k<numClusters; k++) {
	    clusterChange[k] = true;
	}

	// Main Algorithm Loop
	while (!isConverged(clusterChange)) {
	    // printMean(centroids);
	    
	    // reset cluster changes, counts and new centroid vectors
	    for (int k=0; k<numClusters; k++) {
		clusterChange[k] = false;
		clusterCounts[k] = 0;
		for (int metric=0; metric<numMetrics; metric++) {
		    newCentroidVectors[k][metric] = 0.0;
		}
	    }
	    
	    // for each data point, compute distance from centroids
	    //   and pick the centroid closest to it.
	    for (int p=0; p<numProcs; p++) {
		double minDist = Double.MAX_VALUE;
		int currentK = -1;
		// compute distance from centroids
		for (int k=0; k<numClusters; k++) {
		    double tempDist = 0.0;
		    for (int metric=0; metric<numMetrics; metric++) {
			tempDist += 
			    Math.pow(data[p][metric] - 
				     centroids[k][metric], 2.0);
		    }
		    tempDist = Math.sqrt(tempDist);
		    if (tempDist < minDist) {
			minDist = tempDist;
			currentK = k;
		    }
		}
		// found closest centroid from p, record the data
		clusterCounts[currentK]++;
		// since changes happen in pairs, we can get away with
		//   just recording change on one side.
		if (currentK != clusterMap[p]) {
		    clusterChange[currentK] = true;
		}
		clusterMap[p] = currentK;
		distanceFromClusterMean[p] = minDist;
		for (int metric=0; metric<numMetrics; metric++) {
		    newCentroidVectors[currentK][metric] += data[p][metric];
		}
	    }
	    
	    // Compute new centroids from new centroid vectors
	    for (int k=0; k<numClusters; k++) {
		for (int metric=0; metric<numMetrics; metric++) {
		    if (clusterCounts[k] > 0) {
			centroids[k][metric] = 
			    (newCentroidVectors[k][metric]/clusterCounts[k]);
		    }
		}
	    }
	}
	outputResults(clusterMap, numClusters, distanceFromClusterMean, false);
    }
    
    public static boolean isConverged(boolean clusterChange[]) {
	for (int k=0; k<clusterChange.length; k++) {
	    if (clusterChange[k]) {
		return false;
	    }
	}
	return true;
    }

    public static void setCentroidDiagonal(double centroid[][], 
					   int numClusters,
					   double data[][]) {
	// Place initial mean values O(ep * p)
	double minVal;
	double maxVal;
	double interval;
	
	int numProcs = data.length;
	int numEPs = data[0].length;

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
	    interval = ((maxVal - minVal + 1))/numClusters;
	    for (int k=0; k<numClusters; k++) {
		centroid[k][ep] = (k+1)*interval - interval/2 + minVal;
	    }
	}
    }

    public static void setCentroidRandom(double centroid[][],
					 int numClusters,
					 double data[][]) {
	int numProcs = data.length;

	int pickedCentroids[] = new int[numClusters];
	for (int k=0; k<numClusters; k++) {
	    pickedCentroids[k] = -1;
	}
	Random rand = new Random(11337);
	int numPicked = 0;
	while (numPicked < numClusters) {
	    int randomNum = rand.nextInt(numProcs);
	    boolean duplicate = false;
	    for (int k=0; k<=numPicked; k++) {
		if (pickedCentroids[k] == randomNum) {
		    duplicate = true;
		    break;
		}
	    }
	    if (!duplicate) {
		pickedCentroids[numPicked] = randomNum;
		System.out.println("Centroid " + randomNum + " picked");
		for (int act=0; act<centroid[numPicked].length; act++) {
		    centroid[numPicked][act] = data[randomNum][act];
		}
		numPicked++;
	    }
	}
    }

    // Absolute-value-biased normalization:
    //    (ie. normalized_value = value - min).
    // The regular min-max normalization removes an important absolute-value 
    //    bias required to recognize performance problems from the same 
    //    metric domain.
    public static double[][] normalize(double data[][]) {
	double normalized[][] = new double[data.length][data[0].length];
	// assume same length when normalizing.
	for (int metric=0; metric<data[0].length; metric++) {
	    double min = Double.MAX_VALUE;
	    // pass #1 - find min
	    for (int p=0; p<data.length; p++) {
		if (data[p][metric] < min) {
		    min = data[p][metric];
		}
	    }
	    // pass #2 - normalize
	    for (int p=0; p<data.length; p++) {
		normalized[p][metric] = (data[p][metric] - min);
	    }
	}
	return normalized;
    }

    public static double[][] normalizeRelative(double data[][]) {
	double normalized[][] = new double[data.length][data[0].length];
	// assume same length when normalizing.
	for (int metric=0; metric<data[0].length; metric++) {
	    double min = Double.MAX_VALUE;
	    double max = Double.MIN_VALUE;
	    // pass #1 - find min and max
	    for (int p=0; p<data.length; p++) {
		if (data[p][metric] < min) {
		    min = data[p][metric];
		}
		if (data[p][metric] > max) {
		    max = data[p][metric];
		}
	    }
	    // pass #2 - normalize
	    for (int p=0; p<data.length; p++) {
		if ((max - min) > 0) {
		    normalized[p][metric] = 
			(data[p][metric] - min)/(max - min);
		} else {
		    normalized[p][metric] = 0;
		}
	    }
	}
	return normalized;
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
    
    public static void outputResults(int clusterMap[], int numClusters,
				     double distanceFromClusterMean[],
				     boolean outputDetails) {
	// Calculate Quality Measure = Sum of Cluster Radii
	//   the smaller the better given a fixed number of non-empty clusters.
	int numPoints = distanceFromClusterMean.length;

	int numNonEmpty = 0;
	double quality = 0.0;
	double clusterMax[] = new double[numPoints];
	int clusterCounts[] = new int[numClusters];
	boolean clusterHasStuff[] = new boolean[numClusters]; 
	for (int k=0; k<numClusters; k++) {
	    clusterCounts[k] = 0;
	    clusterHasStuff[k] = false;
	}

	for (int p=0; p<numPoints; p++) {
	    int k = clusterMap[p];
	    clusterHasStuff[k] = true;
	    clusterCounts[k]++;
	    if (distanceFromClusterMean[p] > clusterMax[k]) {
		clusterMax[k] = distanceFromClusterMean[p];
	    }
	}

	for (int k=0; k<numClusters; k++) {
	    if (clusterHasStuff[k]) {
		quality += clusterMax[k];
		numNonEmpty++;
	    }
	}
	
	// Output
	System.out.println("Cluster Results:");
	System.out.println("----------------");
	System.out.println("Chosen Num Clusters: " + numClusters);
	System.out.println("Num Non Empty: " + numNonEmpty);
	System.out.print("Cluster Counts: ");
	for (int k=0; k<numClusters; k++) {
	    if (clusterHasStuff[k]) {
		System.out.print("["+k+"] " + clusterCounts[k] + ", ");
	    }
	}
	System.out.println();
	System.out.println("Average Cluster Quality: " + 
			   quality/numNonEmpty);
	System.out.println("================================");
     
	if (outputDetails) {
	    for (int k=0; k<numClusters; k++) {
		System.out.println("["+ k + "]:");
		for (int p=0; p<clusterMap.length; p++) {
		    if (clusterMap[p] == k) {
			System.out.println(p);
		    }
		}
	    }
	}
    }

    public static void main(String args[]) {
	int numClusters = 5;
	double data[][] = { {0,5}, {1,1}, {1,2}, {1,4}, {2,1},
			  {2,7}, {3,6}, {3,8}, {4,9}, {5,2},
			  {5,7}, {6,1}, {7,2}, {9,4}, {9,5} };
	int clusterMap[] = new int[data.length];
	double distanceFromClusterMean[] = new double[data.length];
	KMeansClustering.kMeans(data, numClusters, clusterMap, 
				distanceFromClusterMean);
	outputResults(clusterMap, numClusters, distanceFromClusterMean, true);
    }
}
