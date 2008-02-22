package projections.gui.Timeline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ProgressMonitor;

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

//			ProgressMonitor progressBar = new ProgressMonitor(data.guiRoot(), "Creating auxiliary data structures to speed up visualization", "", 0, 4);
//			progressBar.setProgress(0);
//			progressBar.setNote("Creating Map 1");

			/** Create a mapping from EventIDs on each pe to messages */
			messageStructures.setEventIDToMessageMap(new HashMap[data.numPEs()]);

			if(data.processorList!=null){
				int i=0;
				data.processorList.reset();	
				while(data.processorList.hasMoreElements()){
					int p = data.processorList.nextElement();

					messageStructures.getEventIDToMessageMap()[p] = new HashMap();
					if(data.mesgVector[p] != null){
//						System.out.println("Message vector size = "+mesgVector[p].size());

						// scan through mesgVector[p] and add each TimelineMessage entry to the map
						Iterator iter = data.mesgVector[p].iterator();
						while(iter.hasNext()){
							TimelineMessage msg = (TimelineMessage) iter.next();
							if(msg!=null)
								messageStructures.getEventIDToMessageMap()[p].put(new Integer(msg.EventID), msg);
						}
					} else {
						System.out.println("Message vector is empty");
					}
					i++;
				}

			}


//			progressBar.setProgress(1);
//			progressBar.setNote("Creating Map 2");
			yield();

			/** Create a mapping from Entry Method EventIDs on each pe to EntryMethods */
			messageStructures.setEventIDToEntryMethodMap(new HashMap[data.numPEs()]);


			if(data.processorList!=null){
				data.processorList.reset();	
				int i=0;
				while(data.processorList.hasMoreElements()){
					int p = data.processorList.nextElement();

					messageStructures.getEventIDToEntryMethodMap()[p] = new HashMap();
					if(data.tloArray[i]!=null){
						for(int j=0;j<data.tloArray[i].length;j++){
							EntryMethodObject obj=data.tloArray[i][j];
							if(obj!=null)
								messageStructures.getEventIDToEntryMethodMap()[p].put(new Integer(obj.EventID), obj);
						}
					}		
					i++;
				}
			}

//			progressBar.setProgress(2);
//			progressBar.setNote("Creating Map 3");
			yield();


			/** Create a mapping from TimelineMessage objects to their creator EntryMethod's */
			messageStructures.setMessageToSendingObjectsMap(new HashMap());
			for(int i=0;i<data.tloArray.length;i++){
				if(data.tloArray[i]!=null){
					for(int j=0;j<data.tloArray[i].length;j++){
						EntryMethodObject obj=data.tloArray[i][j];

						if(obj != null){
							// put all the messages created by obj into the map, listing obj as the creator
							Iterator iter = obj.messages.iterator();
							while(iter.hasNext()){
								TimelineMessage msg = (TimelineMessage) iter.next();
								messageStructures.getMessageToSendingObjectsMap().put(msg, obj);
							}
						}
					}				
				}
			}

//			progressBar.setProgress(3);
//			progressBar.setNote("Creating Map 4");
			yield();

			/** Create a mapping from TimelineMessage objects to a set of the resulting execution EntryMethod objects */
			messageStructures.setMessageToExecutingObjectsMap(new HashMap());
			for(int i=0;i<data.tloArray.length;i++){
				if(data.tloArray[i]!=null){
					for(int j=0;j<data.tloArray[i].length;j++){

						EntryMethodObject obj=data.tloArray[i][j];
						if(obj!=null){


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
				}
			}


//			progressBar.setProgress(4);
//			progressBar.setNote("Creating Map 5");
			yield();

			/** Create a mapping from Chare array element indices to their EntryMethodObject's */
			messageStructures.setOidToEntryMethodObjectsMap(new TreeMap());
			for(int i=0;i<data.tloArray.length;i++){
				if(data.tloArray[i]!=null){
					for(int j=0;j<data.tloArray[i].length;j++){
						EntryMethodObject obj=data.tloArray[i][j];

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
			}

//			System.out.println("oidToEntryMethonObjectsMap contains " + oidToEntryMethonObjectsMap.size() + " unique chare array indices");

//			progressBar.close();

		}
    	 
     }

}
