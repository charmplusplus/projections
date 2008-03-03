package projections.gui.Timeline;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import projections.analysis.ObjectId;


/**  A thread that will fill in the MessageStructures data .
 * 
 *   This thread synchronizes with the messageStructures object, so that other methods
 *    that syncrhonize with it will block until this thread has created the data structures completely.
 *   
 *  @author idooley2
 * */
public class ThreadMessageStructures extends Thread {

	Data data;
	MessageStructures messageStructures;
	
	public ThreadMessageStructures(MessageStructures messageStructures){
		this.messageStructures = messageStructures;
		this.data = messageStructures.data;
	}
	
	public void run() {
		synchronized(messageStructures){
			messageStructures.generate();
			
		}
    	 
     }

}
