package projections.analysis;

public class ObjectId implements Comparable
{
    private final static int ID_SIZE = 6;
    public int id[];

    public ObjectId() {
        id = new int[ID_SIZE];
        id[0] = id[1] = id[2] = id[3] = id[4] = id[5] = -1;
    }
    public ObjectId(ObjectId d) {
        if (d!=null) id = d.id.clone();
        else {
            id = new int[ID_SIZE];
            id[0] = id[1] = id[2] = id[3] = id[4] = id[5] = -1;
        }
    }

    protected ObjectId(int[] data) {
        if (data.length > ID_SIZE)
            throw new IndexOutOfBoundsException("Attempted to assign " + data.length + "elements to ID of size " + ID_SIZE);
        id = new int[ID_SIZE];
        for (int i = 0; i < data.length; i++) {
            id[i] = data[i];
        }
    }

    public boolean equals(ObjectId comp) {
        System.out.println("EQUALS");
        return (id[0] == comp.id[0]) && (id[1] == comp.id[1]) && (id[2] == comp.id[2]) &&
               (id[3] == comp.id[3]) && (id[4] == comp.id[4]) && (id[5] == comp.id[5]);
    }

    public boolean equals(Object o) {
        ObjectId comp = (ObjectId) o;
        return this.equals(comp);
    }

    /** Needed for putting these as keys in a TreeSet */
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
