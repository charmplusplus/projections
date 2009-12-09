package projections.Tools.Timeline;

/** The reader threads for timeline */
public class ThreadedFileReader extends Thread  {
	
	int pe;
//	int p;
	Data data;
	
	protected ThreadedFileReader(int pe, Data data){
		this.data = data;
		this.pe = pe;
	}
	
     public void run() {
     	 	data.getData(new Integer(pe));
     }

}
