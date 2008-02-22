package projections.gui.Timeline;

/** The reader threads for timeline */
public class ThreadedFileReader extends Thread  {
	
	EntryMethodObject[][] tloArray;
	int pnum;
	int p;
	Data data;
	
	public ThreadedFileReader(int pnum, int p, Data data){
		this.data = data;
		this.pnum = pnum;
		this.p = p;
	}
	
     public void run() {
    		data.tloArray[p] = data.getData(pnum, p);
     }

}
