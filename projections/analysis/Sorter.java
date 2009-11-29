//package projections.analysis;
//
///**
// *  Written by Chee Wai Lee
// *  4/25/2002
// *
// *  Sorter is a static class that provides methods to sort numeric data
// *  (using the simplest Quicksort variant) but produce a sort map instead
// *  of the actual sorted array.
// *
// *  ***CURRENT IMP***
// *  For now, it is hard coded to do reverse sorting (with the assumption
// *  that bigger is more significant). It should be written to register an 
// *  object that implements the java.util.Comparator interface.
// */
//
//public class Sorter
//{
//  
//
//    /** 
//     *  Invariant: Every element to the left of the pivot is smaller than
//     *             the pivot element.
//     */
//    private static void qSort(int[] map, double[] array, int low, int high) {
//	// test termination condition
//	if (low >= high) {
//	    return;
//	}
//
//	// pick first element as pivot
//	int pivot = low;
//
//	// partition the data
//	for (int i=low+1; i<=high; i++) {
//	    if (i < pivot) {
//		// seeking to swap with smaller or equal elements
//		// if I am not wrong, this never happens!
//		if (array[i] <= array[pivot]) {
//		    swap(map, array, i, pivot);
//		    pivot = i;
//		}
//	    } else {
//		// seeking to swap with larger elements
//		if (array[i] > array[pivot]) {
//		    if (i == pivot + 1) {
//			// if smaller element is next to pivot, do a swap
//			swap(map, array, i, pivot);
//			pivot = i;
//		    } else {
//			// if i > pivot + 1, 
//			// do a double swap with next larger element
//			swap(map, array, pivot, pivot+1);
//			swap(map, array, i, pivot);
//			pivot = pivot+1;
//		    }
//		}
//	    }
//	}
//
//	// recurse
//	qSort(map, array, low, pivot-1);
//	qSort(map, array, pivot+1, high);
//    }
//
//    private static void swap(int[] map, double[] array, int idx1, int idx2) {
//	int mapTemp;
//	double arrayTemp;
//
//	mapTemp = map[idx1];
//	map[idx1] = map[idx2];
//	map[idx2] = mapTemp;
//
//	arrayTemp = array[idx1];
//	array[idx1] = array[idx2];
//	array[idx2] = arrayTemp;
//    }
//}

