package projections.analysis;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public final class ObjectId implements Comparable
{
    private final static int ID_SIZE = 6;
    public final int id[];

    private static ConcurrentHashMap<ObjectId, ObjectId> instances = new ConcurrentHashMap<>();

    private ObjectId(final int[] data) {
        if (data.length > ID_SIZE)
            throw new IndexOutOfBoundsException("Attempted to assign " + data.length + "elements to ID of size " + ID_SIZE);
        id = new int[ID_SIZE];
        for (int i = 0; i < ID_SIZE; i++) {
            id[i] = data[i];
        }
    }

    public static ObjectId createObjectId() {
        final int[] dummy = new int[ID_SIZE];
        dummy[0] = dummy[1] = dummy[2] = dummy[3] = dummy[4] = dummy[5] = -1;
        return createObjectId(dummy);
    }

    public static ObjectId createObjectId(ObjectId d) {
        if (d == null)
            return createObjectId();
        else return d;
    }

    protected static ObjectId createObjectId(final int[] data) {
        ObjectId candidate = new ObjectId(data);
        ObjectId canonical = instances.putIfAbsent(candidate, candidate);
        if (canonical == null)
            canonical = candidate;
        return canonical;
    }

    public boolean equals(ObjectId comp) {
        return Arrays.equals(id, comp.id);
    }

    public boolean equals(Object o) {
        ObjectId comp = (ObjectId) o;
        return this.equals(comp);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(id);
    }

    /** Needed for putting these as keys in a TreeSet */
    @Override
    @SuppressWarnings("ucd")
    public int compareTo(Object o) {
        ObjectId oid = (ObjectId)o;

        int d0 = id[0]-oid.id[0];
        int d1 = id[1]-oid.id[1];
        int d2 = id[2]-oid.id[2];
        int d3 = id[3]-oid.id[3];
        int d4 = id[4]-oid.id[4];
        int d5 = id[5]-oid.id[5];

        if(d0 != 0)
            return d0;

        if(d1 != 0)
            return d1;

        if(d2 != 0)
            return d2;

        if(d3 != 0)
            return d3;

        if(d4 != 0)
            return d4;

        return d5;
    }

    @Override
    public String toString(){
        return "" + id[0] + ","+ id[1] + ","+ id[2] + ","+ id[3] + "," + id[4] + "," + id[5];
    }
}
