package projections.analysis;


public class ObjectId implements Comparable
{
  private int ID_SIZE = 4;

public int id[];
public ObjectId() {
     	 id = new int[ID_SIZE];
	 id[0] = id[1] = id[2] = id[3] = -1;
       }
public ObjectId(ObjectId d) {
	 if (d!=null) id = d.id.clone();
         else {
     	   id = new int[ID_SIZE];
	   id[0] = id[1] = id[2] = id[3] = -1;
	 }
       }
protected ObjectId(int d0, int d1, int d2, int aid) {
     	 id = new int[ID_SIZE];
	 id[0] = d0; id[1] = d1; id[2] = d2; id[3] = aid;
       }

public boolean equals(ObjectId comp) {
	System.out.println("EQUALS");
	return (id[0]==comp.id[0]) && (id[1]==comp.id[1]) && (id[2]==comp.id[2]) && (id[3] == comp.id[3]); 
}

public boolean equals(Object o) {
	ObjectId comp = (ObjectId) o;
	return (id[0]==comp.id[0]) && (id[1]==comp.id[1]) && (id[2]==comp.id[2]) && (id[3] == comp.id[3]); 
}

/** Needed for putting these as keys in a TreeSet */
public int compareTo(Object o) {
	ObjectId oid = (ObjectId)o;

	int d0 = id[0]-oid.id[0];
	int d1 = id[1]-oid.id[1];
	int d2 = id[2]-oid.id[2];
	int d3 = id[3]-oid.id[3];

	if(d0 != 0)
		return d0;

	if(d1 != 0)
		return d1;

	if(d2 != 0)
		return d2;

	return d3;
}

public String toString(){
	return "" + id[0] + ","+ id[1] + ","+ id[2] + ","+ id[3] ;
}

}


