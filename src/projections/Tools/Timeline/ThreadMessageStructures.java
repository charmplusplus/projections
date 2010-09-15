package projections.Tools.Timeline;



/**  A thread that will fill in the MessageStructures data .
 * 
 *   This thread synchronizes with the messageStructures object, so that other methods
 *    that syncrhonize with it will block until this thread has created the data structures completely.
 *   
 *  @author idooley2
 * */
class ThreadMessageStructures extends Thread {

	volatile boolean stop = false;
	
	protected void stopThread(){
		synchronized(this){
			stop = true;
		}
	}
	
	private MessageStructures messageStructures;
	
	protected ThreadMessageStructures(MessageStructures messageStructures){
		this.messageStructures = messageStructures;
	}
	
	public void run() {
		synchronized(messageStructures){
			messageStructures.generate(this);
		}
    	 
     }

}
