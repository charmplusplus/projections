package projections.analysis;


public class ObjectId
{
public int id[];
public ObjectId() {
     	 id = new int[3];
	 id[0] = id[1] = id[2] = 0;
       }
public ObjectId(ObjectId d) {
	 if (d!=null) id = (int[])d.id.clone();
         else {
     	   id = new int[3];
	   id[0] = id[1] = id[2] = 0;
	 }
       }
public ObjectId(int d0, int d1, int d2) {
     	 id = new int[3];
	 id[0] = d0; id[1] = d1; id[2] = d2;
       }
}
