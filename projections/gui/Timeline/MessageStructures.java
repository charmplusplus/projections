package projections.gui.Timeline;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
	
	// TODO update each of these first two members to make them not require space proportional to PE.
	
	/** A Map for each PE, key = eventID value = Message */
	private Map []eventIDToMessageMap;
	
	/** A Map for each PE, key = eventID value = EntryMethodObject */
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
		init();
	}

	
	/** Delete all old references by initializing each data structure */
	private void init(){
		synchronized(this){
			int pe = data.numPEs();

			eventIDToMessageMap = new HashMap[pe];
			for(int i=0;i<pe;i++)
				eventIDToMessageMap[i] = new HashMap();

			eventIDToEntryMethodMap = new HashMap[pe];
			for(int i=0;i<pe;i++)
				eventIDToEntryMethodMap[i] = new HashMap();

			messageToSendingObjectsMap = new HashMap();
			messageToExecutingObjectsMap = new HashMap();

			oidToEntryMethodObjectsMap = new TreeMap();
		}		
	}
	
	/** Spawn a thread that will fill in the data structures. It is unlikely in the visualization that these 
	 * structures will be needed early on. Due to the mutual exclusion synchronization on this object,
	 *  all threads that eventually need the data will block until the data has been produced */
	public void create(){
		// Create the secondary structures for efficient accessing of messages
		init();
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

	public void kill() {
		if(secondaryWorkers != null){
			secondaryWorkers.stop();
			secondaryWorkers = null;
		}
		init();
	}
			
}
