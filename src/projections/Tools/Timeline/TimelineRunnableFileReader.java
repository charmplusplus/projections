package projections.Tools.Timeline;

/** The reader threads for timeline */
class TimelineRunnableFileReader implements Runnable  {
	
	private int pe;
	private Data data;
	
	protected TimelineRunnableFileReader(int pe, Data data){
		this.data = data;
		this.pe = pe;
	}
	
     public void run() {
     	 	data.getData(Integer.valueOf(pe));
     }

}
