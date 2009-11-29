package projections.Tools.Timeline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import projections.analysis.ObjectId;

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
	private Map<Integer, TimelineMessage> []eventIDToMessageMap;

	/** A Map for each PE, key = eventID value = EntryMethodObject */
	private Map<Integer, EntryMethodObject> []eventIDToEntryMethodMap;

	/** Map from a message to the the resulting entry methods entry object invocations */
	private Map<TimelineMessage, Set<EntryMethodObject>> messageToExecutingObjectsMap;

	/** Map from a message to its invoking entry method instance */
	private Map<TimelineMessage, EntryMethodObject> messageToSendingObjectsMap;

	/** Map from Chare Array element id's to the corresponding known entry method invocations */
	private Map<ObjectId, List<EntryMethodObject> > oidToEntryMethodObjectsMap;

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

			messageToSendingObjectsMap = new HashMap<TimelineMessage, EntryMethodObject>();
			messageToExecutingObjectsMap = new HashMap<TimelineMessage, Set<EntryMethodObject>>();

			oidToEntryMethodObjectsMap = new TreeMap();
		}		
	}

	/** Spawn a thread that will fill in the data structures. It is unlikely in the visualization that these 
	 * structures will be needed early on. Due to the mutual exclusion synchronization on this object,
	 *  all threads that eventually need the data will block until the data has been produced */
	public void create(boolean useSeparateThread){
		// Create the secondary structures for efficient accessing of messages
		init();
		secondaryWorkers = new ThreadMessageStructures(this);
		if(useSeparateThread){
			secondaryWorkers.setPriority(Thread.MIN_PRIORITY);
			secondaryWorkers.start();
		} else {
			generate(null);
		}
	}

	public void setEventIDToMessageMap(Map [] eventIDToMessageMap) {
		this.eventIDToMessageMap = eventIDToMessageMap;
	}

	public Map [] getEventIDToMessageMap() {
		return eventIDToMessageMap;
	}

	public Map<TimelineMessage, EntryMethodObject> getMessageToSendingObjectsMap() {
		return messageToSendingObjectsMap;
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
			Map<TimelineMessage, Set<EntryMethodObject>> messageToExecutingObjectsMap) {
		this.messageToExecutingObjectsMap = messageToExecutingObjectsMap;
	}

	public Map<TimelineMessage, Set<EntryMethodObject>> getMessageToExecutingObjectsMap() {
		return messageToExecutingObjectsMap;
	}

	public void setMessageToSendingObjectsMap(Map<TimelineMessage, EntryMethodObject> messageToSendingObjectsMap) {
		this.messageToSendingObjectsMap = messageToSendingObjectsMap;
	}

	public void kill() {
		if(secondaryWorkers != null){
			secondaryWorkers.stopThread();
			secondaryWorkers = null;
		}
		init();
	}

	public void generate(ThreadMessageStructures structures){

		// TODO These are computed anytime a new range or pe is loaded. Make faster by just adding in the new PEs portion
		/** Create a mapping from EventIDs on each pe to messages */
		Iterator<Integer> pe_iter = data.allEntryMethodObjects.keySet().iterator();
		while(pe_iter.hasNext()){

			if(structures!=null && structures.stop)
				return;
			
			Integer pe =  pe_iter.next();
			List<EntryMethodObject> objs = data.allEntryMethodObjects.get(pe);
			Iterator<EntryMethodObject> obj_iter = objs.iterator();
			while(obj_iter.hasNext()){  
				// For each EntryMethod Object
				EntryMethodObject obj = obj_iter.next();
				Iterator<TimelineMessage> msg_iter = obj.messages.iterator();
				while(msg_iter.hasNext()){
					// For each message sent by the object
					TimelineMessage msg = msg_iter.next();
					getEventIDToMessageMap()[pe.intValue()].put(new Integer(msg.EventID), msg);
				}
			}
		}


		/** Create a mapping from Entry Method EventIDs on each pe to EntryMethods */

		pe_iter = data.allEntryMethodObjects.keySet().iterator();
		while(pe_iter.hasNext()){

			if(structures!=null && structures.stop)
				return;
			
			Integer pe =  pe_iter.next();
			List<EntryMethodObject> objs = data.allEntryMethodObjects.get(pe);
			Iterator<EntryMethodObject> obj_iter = objs.iterator();
			while(obj_iter.hasNext()){
				EntryMethodObject obj = obj_iter.next();
				getEventIDToEntryMethodMap()[pe.intValue()].put(new Integer(obj.EventID), obj);
			}
		}


		/** Create a mapping from TimelineMessage objects to their creator EntryMethod's */

		pe_iter = data.allEntryMethodObjects.keySet().iterator();
		while(pe_iter.hasNext()){
			

			if(structures!=null && structures.stop)
				return;
			
			Integer pe =  pe_iter.next();
			List<EntryMethodObject> objs = data.allEntryMethodObjects.get(pe);
			Iterator<EntryMethodObject> obj_iter = objs.iterator();
			while(obj_iter.hasNext()){
				EntryMethodObject obj = obj_iter.next();

				// put all the messages created by obj into the map, listing obj as the creator
				Iterator<TimelineMessage> iter = obj.messages.iterator();
				while(iter.hasNext()){
					TimelineMessage msg = iter.next();
					getMessageToSendingObjectsMap().put(msg, obj);
				}
			}				
		}


		/** Create a mapping from TimelineMessage objects to a set of the resulting execution EntryMethod objects */

		pe_iter = data.allEntryMethodObjects.keySet().iterator();
		while(pe_iter.hasNext()){

			if(structures!=null && structures.stop)
				return;
			
			Integer pe =  pe_iter.next();
			List<EntryMethodObject> objs = data.allEntryMethodObjects.get(pe);
			Iterator<EntryMethodObject> obj_iter = objs.iterator();
			while(obj_iter.hasNext()){
				EntryMethodObject obj = obj_iter.next();

				TimelineMessage msg = obj.creationMessage();
				if(msg!=null){
					// for each EntryMethodObject, add its creation Message to the map
					if(getMessageToExecutingObjectsMap().containsKey(msg)){
						// add it to the TreeSet in the map
						Object o= getMessageToExecutingObjectsMap().get(msg);
						TreeSet<EntryMethodObject> ts = (TreeSet<EntryMethodObject>)o;
						ts.add(obj);
					} else {
						// create a new TreeSet and put it in the map
						TreeSet<EntryMethodObject> ts = new TreeSet<EntryMethodObject>();
						ts.add(obj);
						getMessageToExecutingObjectsMap().put(msg, ts);
					}
				}

			}
		}

		/** Create a mapping from Chare array element indices to their EntryMethodObject's */

		pe_iter = data.allEntryMethodObjects.keySet().iterator();
		while(pe_iter.hasNext()){

			if(structures!=null && structures.stop)
				return;
			
			Integer pe =  pe_iter.next();
			List<EntryMethodObject> objs = data.allEntryMethodObjects.get(pe);
			Iterator<EntryMethodObject> obj_iter = objs.iterator();
			while(obj_iter.hasNext()){
				EntryMethodObject obj = obj_iter.next();

				if(obj != null){
					ObjectId id = obj.getTid();

					if(getOidToEntryMethodObjectsMap().containsKey(id)){
						// add obj to the existing set
						TreeSet<EntryMethodObject> s = (TreeSet<EntryMethodObject>) getOidToEntryMethodObjectsMap().get(id);
						s.add(obj);
					} else {
						// create a set for the id
						TreeSet<EntryMethodObject> s = new TreeSet<EntryMethodObject>();
						s.add(obj);
						getOidToEntryMethodObjectsMap().put(id, s);
					}

				}
			}				
		}
	}

	
	public void clearAll(){
		synchronized(this){
			int pe = eventIDToMessageMap.length;
			for(int i=0;i<pe;i++)
				eventIDToMessageMap[i].clear();
			for(int i=0;i<pe;i++)
				eventIDToEntryMethodMap[i].clear();
			messageToSendingObjectsMap.clear();
			messageToExecutingObjectsMap.clear();
			oidToEntryMethodObjectsMap.clear();
		}		
	}
	
}
