package projections.gui.Timeline;

/** The reader threads for timeline */
public class ThreadedFileReader extends Thread  {
	
	EntryMethodObject[][] tloArray;
	int pe;
	int p;
	Data data;
	
	public ThreadedFileReader(int pe, int p, Data data){
		this.data = data;
		this.pe = pe;
		this.p = p;
	}
	
     public void run() {
     	 	data.tloArray[p] = data.getData(pe, p);
     }

}
