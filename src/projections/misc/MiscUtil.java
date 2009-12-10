package projections.misc;

class MiscUtil {
    /**
     *  The sort method returns the mapping resulting from the sorting of 
     *  "unsorted". A simple bubble sort algorithm is used since this 
     *  mapping feature is unlikely to be used in large data sets.
     *  As a side effect, the input array also becomes sorted.
     */ 
    protected static int[] sortAndMap(int unsorted[]) {
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

//    /**
//     *  This method returns only the sort mapping but will not modify the
//     *  input array. getSortMap is intended to work with small arrays, so
//     *  the cloning operation is not expected to be too expensive.
//     */
//    public static int[] getSortMap(int unsorted[]) {
//	int tempArray[] = (unsorted.clone());
//
//	return sortAndMap(tempArray);
//    }

    /**
     *  Actually applying a given map to an array of the correct size.
     *  Code will throw an ArrayIndexOutOfBoundsException if the sizes do
     *  not match.
     *
     *  The simplest solution is to clone the target array and then apply
     *  the values to the original array. This is alright since applyMap
     *  is intended to work with smaller arrays.
     *
     *  Uses on primitive arrays should first convert it to objects and
     *  then back again.
     */
    protected static void applyMap(Object targetArray[], int map[]) {
	if (targetArray.length != map.length) {
	    throw new ArrayIndexOutOfBoundsException("Sizes do not match!");
	}
	
	Object tempArray[] = (targetArray.clone());
	for (int i=0; i<map.length; i++) {
	    targetArray[i] = tempArray[map[i]];
	}
    }

//      public static void main(String args[]) {
// 	int mydata[] = {4, 6, 2, 3, 1, 8};
// 	String myObjectData[] = new String[6]; 
// 	// myObjectData is basically an string array that makes sense
// 	// if its indices are sorted according to mydata.
// 	myObjectData[0] = "than";
// 	myObjectData[1] = "your";
// 	myObjectData[2] = "Charm++\'s";
// 	myObjectData[3] = "better";
// 	myObjectData[4] = "My";
// 	myObjectData[5] = "Charm++";
// 	int result[];
// 	result = MiscUtil.getSortMap(mydata);
// 	for (int i=0; i<mydata.length; i++) {
// 	    System.out.print(result[i] + " ");
// 	}
// 	System.out.println();
// 	for (int i=0; i<mydata.length; i++) {
// 	    System.out.print(mydata[i] + " " );
// 	}
// 	System.out.println();
// 	MiscUtil.applyMap(myObjectData, result);
// 	for (int i=0; i<myObjectData.length; i++) {
// 	    System.out.print(myObjectData[i] + " ");
// 	}
// 	System.out.println();
//     }
}
