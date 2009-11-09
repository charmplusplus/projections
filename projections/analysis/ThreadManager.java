package projections.analysis;

import java.awt.Component;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ProgressMonitor;


/** This class manages a pool of worker threads, updating a progress bar as thrads complete. 
 * 
 * The class must be provided with a list of threads 
 * 
 * */
public class ThreadManager {

	/** A copy of the list of threads */
	private LinkedList threads;
	public int numInitialThreads;

	private String description;

	public int numConcurrentThreads;

	private Component parentWindow;

	public ThreadManager(String description, List threads, Component guiRoot){
		this.threads = new LinkedList();
		this.threads.addAll(threads);
		this.description = description;
		this.numInitialThreads = threads.size();
		this.parentWindow = guiRoot;
		this.numConcurrentThreads = 20;
	}


	public void runThreads(){

		ProgressMonitor progressBar = new ProgressMonitor(parentWindow, description,"", 0, numInitialThreads);
		progressBar.setMillisToPopup(10);
		progressBar.setMillisToDecideToPopup(10);
		progressBar.setProgress(0);

		int totalToLoad = threads.size();
		progressBar.setMaximum(totalToLoad);

		if(totalToLoad < numConcurrentThreads){
			numConcurrentThreads = totalToLoad;
		}

		Iterator iter;
		
		// execute reader threads, a few at a time, until all have executed
		LinkedList spawnedReaders = new LinkedList();
		while(threads.size() > 0 || spawnedReaders.size() > 0){

			//------------------------------------
			// spawn some threads
			Thread r;
			while(threads.size()>0 && spawnedReaders.size()<numConcurrentThreads){
				r =  (Thread) ( threads).removeFirst(); // retrieve and remove from list
				spawnedReaders.add(r);
				r.start(); // will cause the run method to be executed
			}

			//------------------------------------
			// update the progress bar
			int doneCount = totalToLoad-threads.size()-spawnedReaders.size();
			if (!progressBar.isCanceled()) {
				progressBar.setNote(doneCount+ " of " + totalToLoad);
				progressBar.setProgress(doneCount);
			} else {
				// user cancelled this operation
				// Wait on all spawned threads to finish
				iter = spawnedReaders.iterator();
				while(iter.hasNext()){
					r = (Thread) iter.next();
					try {
						r.join();
					}
					catch (InterruptedException e) {
						throw new RuntimeException("Thread was interrupted. This should not ever occur");
					}
				}
				break;
			}

			
			//------------------------------------
			// wait on the threads to complete
			iter = spawnedReaders.iterator();
			int waitMillis = 200; // wait for 1000 ms for the first thread, and 1 ms for each additional thread
			while(iter.hasNext()){
				r = (Thread) iter.next();
				try {
					r.join(waitMillis);
					waitMillis = 1;
					if(! r.isAlive()) {
						// Thread Finished
						iter.remove();
					} 
				}
				catch (InterruptedException e) {
					throw new RuntimeException("Thread was interrupted. This should not ever occur");
				}
			}

		}

		progressBar.close();

	}


}
