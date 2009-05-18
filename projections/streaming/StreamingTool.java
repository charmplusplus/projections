package projections.streaming;

/** A tool that uses CCS to get data from a running parallel Charm++ program. */
public class StreamingTool {

	public StreamingTool(){
		System.out.println("Streaming Tool");	
		new StartupDialogBox();
	}

	public static void main(String args[]){
		new MultiSeriesHandler("order.cs.uiuc.edu", 1234, "CkPerfSumDetail compressed");
	}

} 
	