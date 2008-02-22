package projections.gui.Timeline;

import java.util.Map;
import java.util.Vector;

/** this class is used to hold some data structures that are initialized by a separate thread 
 * 
 * @note anyone using the data members in this class should synchronize on this object.
 *              This will cause threads to wait while the data structures are first being computed
 * 
 * @author idooley2
 * 
 * */
public class MessageStructures {
	
	Data data;
	
	/** Some associative containers to make lookups fast */
	private Map []eventIDToMessageMap;
	private Map []eventIDToEntryMethodMap;
	
	/** Map from a message to the the resulting entry methods entry object invocations */
	private Map messageToExecutingObjectsMap;
	
	/** Map from a message to its invoking entry method instance */
	private Map messageToSendingObjectsMap;
	
	/** Map from Chare Array element id's to the corresponding known entry method invocations */
	private Map oidToEntryMethodObjectsMap;
	
	/** A worker thread that creates those data structures */
	private ThreadMessageStructures secondaryWorkers;
	
	public MessageStructures(Data data){
		this.data = data;
	}

	/** Spawn a thread that will fill in the data structures. It is unlikely in the visualization that these 
	 * structures will be needed early on. Due to the mutual exclusion synchronization on this object,
	 *  all threads that eventually need the data will block until the data has been produced */
	public void create(){
		// Create the secondary structures for efficient accessing of messages

		secondaryWorkers = new ThreadMessageStructures(this);
		secondaryWorkers.setPriority(Thread.MIN_PRIORITY);
		secondaryWorkers.start();
	
	}

	public void setEventIDToMessageMap(Map [] eventIDToMessageMap) {
		this.eventIDToMessageMap = eventIDToMessageMap;
	}

	public Map [] getEventIDToMessageMap() {
		return eventIDToMessageMap;
	}

	public Map getMessageToSendingObjectsMap() {
		return messageToSendingObjectsMap;
	}

	public Map getOidToEntryMethonObjectsMap() {
		return getOidToEntryMethodObjectsMap();
	}

	public void setOidToEntryMethodObjectsMap(Map oidToEntryMethodObjectsMap) {
		this.oidToEntryMethodObjectsMap = oidToEntryMethodObjectsMap;
	}

	public Map getOidToEntryMethodObjectsMap() {
		return oidToEntryMethodObjectsMap;
	}

	public void setEventIDToEntryMethodMap(Map [] eventIDToEntryMethodMap) {
		this.eventIDToEntryMethodMap = eventIDToEntryMethodMap;
	}

	public Map [] getEventIDToEntryMethodMap() {
		return eventIDToEntryMethodMap;
	}

	public void setMessageToExecutingObjectsMap(
			Map messageToExecutingObjectsMap) {
		this.messageToExecutingObjectsMap = messageToExecutingObjectsMap;
	}

	public Map getMessageToExecutingObjectsMap() {
		return messageToExecutingObjectsMap;
	}

	public void setMessageToSendingObjectsMap(Map messageToSendingObjectsMap) {
		this.messageToSendingObjectsMap = messageToSendingObjectsMap;
	}
			
}
