package projections.misc;

public class MiscUtil {
    /**
     *  The sort method returns the mapping resulting from the sorting of 
     *  "unsorted". A simple bubble sort algorithm is used since this 
     *  mapping feature is unlikely to be used in large data sets.
     *  As a side effect, the input array also becomes sorted.
     */ 
    public static int[] sort(int unsorted[]) {
	int map[];
	int length;
	int temp;

	length = unsorted.length;

	// fill the mapping array
	map = new int[length];
	for (int i=0; i<length; i++) {
	    map[i] = i;
	}

	// bubble sort with mapping adjustment on swap
	for (int i=0; i<length; i++) {
	    for (int j=0; j<length-i-1; j++) {
		if (unsorted[j] > unsorted[j+1]) {
		    // swap data
		    temp = unsorted[j];
		    unsorted[j] = unsorted[j+1];
		    unsorted[j+1] = temp;
		    // adjust mapping
		    temp = map[j]; // j's initial id
		    map[j] = map[j+1]; // exchange with j+1's id
		    map[j+1] = temp;
		}
	    }
	}

	return map;
    }

    /**
     *  This method returns only the sort mapping but will not modify the
     *  input array.
     */
    public static int[] getSortMap(int unsorted[]) {
	int tempArray[] = (int [])(unsorted.clone());

	return sort(tempArray);
    }

    public static void main(String args[]) {
	int mydata[] = {4, 6, 2, 3, 1, 8};
	int result[];
	// result = MiscUtil.sort(mydata);
	result = MiscUtil.getSortMap(mydata);
	for (int i=0; i<mydata.length; i++) {
	    System.out.print(result[i] + " ");
	}
	System.out.println();
	for (int i=0; i<mydata.length; i++) {
	    System.out.print(mydata[i] + " " );
	}
	System.out.println();
    }
}
