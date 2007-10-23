package projections.analysis;


public class ObjectId
{
  private int ID_SIZE = 4;

public int id[];
public ObjectId() {
     	 id = new int[ID_SIZE];
	 id[0] = id[1] = id[2] = id[3] = -1;
       }
public ObjectId(ObjectId d) {
	 if (d!=null) id = (int[])d.id.clone();
         else {
     	   id = new int[ID_SIZE];
	   id[0] = id[1] = id[2] = id[3] = -1;
	 }
       }
public ObjectId(int d0, int d1, int d2, int aid) {
     	 id = new int[ID_SIZE];
	 id[0] = d0; id[1] = d1; id[2] = d2; id[3] = aid;
       }
public boolean compare(ObjectId comp) {
  return (id[0]==comp.id[0]) && (id[1]==comp.id[1]) && 
    (id[2]==comp.id[2]) && (id[3] == comp.id[3]); 
}
}

