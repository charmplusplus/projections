package projections.Tools.Timeline;



/**  A thread that will fill in the MessageStructures data .
 * 
 *   This thread synchronizes with the messageStructures object, so that other methods
 *    that syncrhonize with it will block until this thread has created the data structures completely.
 *   
 *  @author idooley2
 * */
public class ThreadMessageStructures extends Thread {

	volatile boolean stop = false;
	
	public void stopThread(){
		stop = true;
	}
	
	MessageStructures messageStructures;
	
	public ThreadMessageStructures(MessageStructures messageStructures){
		this.messageStructures = messageStructures;
	}
	
	public void run() {
		synchronized(messageStructures){
			messageStructures.generate(this);
		}
    	 
     }

}
