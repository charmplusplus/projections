package projections.Tools.EntryMethodProfile;

import java.util.Comparator;

/**
 * Used to sort data array by index rather than use a more expensive data structure
 */
public class ArrayIndexComparator implements Comparator<Integer> {
    private final double[] array;
    ArrayIndexComparator(double[] array) {
        this.array = array;
    }

    Integer[] createIndexArray()
    {
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
        {
            indexes[i] = i;
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2)
    {
        return (int)(array[index2] - array[index1]);
    }
}
