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

			yield();
			
			// TODO These are computed anytime a new range or pe is loaded. Make faster by just adding in the new PEs portion

			/** Create a mapping from EventIDs on each pe to messages */
			Iterator pe_iter = data.allEntryMethodObjects.keySet().iterator();
			while(pe_iter.hasNext()){
				Integer pe =  (Integer) pe_iter.next();
				LinkedList objs = (LinkedList)data.allEntryMethodObjects.get(pe);
				Iterator obj_iter = objs.iterator();
				while(obj_iter.hasNext()){  
					// For each EntryMethod Object
					EntryMethodObject obj = (EntryMethodObject) obj_iter.next();
					Iterator msg_iter = obj.messages.iterator();
					while(msg_iter.hasNext()){
						// For each message sent by the object
						TimelineMessage msg = (TimelineMessage) msg_iter.next();
						messageStructures.getEventIDToMessageMap()[pe.intValue()].put(new Integer(msg.EventID), msg);
					}
				}
			}


//			progressBar.setProgress(1);
//			progressBar.setNote("Creating Map 2");
			yield();

			/** Create a mapping from Entry Method EventIDs on each pe to EntryMethods */
		
			pe_iter = data.allEntryMethodObjects.keySet().iterator();
			while(pe_iter.hasNext()){
				Integer pe =  (Integer) pe_iter.next();
				LinkedList objs = (LinkedList)data.allEntryMethodObjects.get(pe);
				Iterator obj_iter = objs.iterator();
				while(obj_iter.hasNext()){
					EntryMethodObject obj = (EntryMethodObject) obj_iter.next();
					messageStructures.getEventIDToEntryMethodMap()[pe.intValue()].put(new Integer(obj.EventID), obj);
				}
			}

//			progressBar.setProgress(2);
//			progressBar.setNote("Creating Map 3");
			yield();	


			/** Create a mapping from TimelineMessage objects to their creator EntryMethod's */

			pe_iter = data.allEntryMethodObjects.keySet().iterator();
			while(pe_iter.hasNext()){
				Integer pe =  (Integer) pe_iter.next();
				LinkedList objs = (LinkedList)data.allEntryMethodObjects.get(pe);
				Iterator obj_iter = objs.iterator();
				while(obj_iter.hasNext()){
					EntryMethodObject obj = (EntryMethodObject) obj_iter.next();

					// put all the messages created by obj into the map, listing obj as the creator
					Iterator iter = obj.messages.iterator();
					while(iter.hasNext()){
						TimelineMessage msg = (TimelineMessage) iter.next();
						messageStructures.getMessageToSendingObjectsMap().put(msg, obj);
					}
				}				
			}
		

//			progressBar.setProgress(3);
//			progressBar.setNote("Creating Map 4");

			yield();

			/** Create a mapping from TimelineMessage objects to a set of the resulting execution EntryMethod objects */
		
			pe_iter = data.allEntryMethodObjects.keySet().iterator();
			while(pe_iter.hasNext()){
				Integer pe =  (Integer) pe_iter.next();
				LinkedList objs = (LinkedList)data.allEntryMethodObjects.get(pe);
				Iterator obj_iter = objs.iterator();
				while(obj_iter.hasNext()){
					EntryMethodObject obj = (EntryMethodObject) obj_iter.next();

					TimelineMessage msg = obj.creationMessage();
					if(msg!=null){
						// for each EntryMethodObject, add its creation Message to the map
						if(messageStructures.getMessageToExecutingObjectsMap().containsKey(msg)){
							// add it to the TreeSet in the map
							Object o= messageStructures.getMessageToExecutingObjectsMap().get(msg);
							TreeSet ts = (TreeSet)o;
							ts.add(obj);
						} else {
							// create a new TreeSet and put it in the map
							TreeSet ts = new TreeSet();
							ts.add(obj);
							messageStructures.getMessageToExecutingObjectsMap().put(msg, ts);
						}
					}
								
				}
			}


//			progressBar.setProgress(4);
//			progressBar.setNote("Creating Map 5");
			yield();

			/** Create a mapping from Chare array element indices to their EntryMethodObject's */

			pe_iter = data.allEntryMethodObjects.keySet().iterator();
			while(pe_iter.hasNext()){
				Integer pe =  (Integer) pe_iter.next();
				LinkedList objs = (LinkedList)data.allEntryMethodObjects.get(pe);
				Iterator obj_iter = objs.iterator();
				while(obj_iter.hasNext()){
					EntryMethodObject obj = (EntryMethodObject) obj_iter.next();

					if(obj != null){
						ObjectId id = obj.getTid();

						if(messageStructures.getOidToEntryMethodObjectsMap().containsKey(id)){
							// add obj to the existing set
							TreeSet s = (TreeSet) messageStructures.getOidToEntryMethodObjectsMap().get(id);
							s.add(obj);
						} else {
							// create a set for the id
							TreeSet s = new TreeSet();
							s.add(obj);
							messageStructures.getOidToEntryMethodObjectsMap().put(id, s);
						}

					}
				}				
			}

//			progressBar.close();

		}
    	 
     }

}
