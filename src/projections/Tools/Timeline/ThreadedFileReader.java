package projections.Tools.Timeline;

/** The reader threads for timeline */
class ThreadedFileReader implements Runnable  {
	
	private int pe;
	private Data data;
	
	protected ThreadedFileReader(int pe, Data data){
		this.data = data;
		this.pe = pe;
	}
	
     public void run() {
     	 	data.getData(Integer.valueOf(pe));
     }

}
