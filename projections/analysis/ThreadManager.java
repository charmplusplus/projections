package projections.analysis;

import java.awt.Component;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ProgressMonitor;


/** This class manages a pool of worker threads, updating a progress bar as threads complete. 
 * 
 * The class must be provided with a list of threads. 
 * 
 * */
public class ThreadManager {

	/** A copy of the list of threads */
	private LinkedList<Thread> threadsToRun;
	public int numInitialThreads;

	private String description;

	public int numConcurrentThreads;

	private Component guiRootForProgressBar;

	public ThreadManager(String description, List<Thread> threads, Component guiRoot){
		this.threadsToRun = new LinkedList<Thread>();
		this.threadsToRun.addAll(threads);
		this.description = description;
		this.numInitialThreads = threads.size();
		this.guiRootForProgressBar = guiRoot;

		int numProcs = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of processors available to the Java Virtual Machine: " + numProcs);		
		numConcurrentThreads = numProcs*2;
	}
	

	public void runThreads(){

		ProgressMonitor progressBar = new ProgressMonitor(guiRootForProgressBar, description,"", 0, numInitialThreads);
		progressBar.setMillisToPopup(10);
		progressBar.setMillisToDecideToPopup(10);
		progressBar.setProgress(0);

		int totalToLoad = threadsToRun.size();
		progressBar.setMaximum(totalToLoad);

		if(totalToLoad < numConcurrentThreads){
			numConcurrentThreads = totalToLoad;
		}

		Iterator<Thread> iter;
		
		// execute reader threads, a few at a time, until all have executed
		LinkedList<Thread> spawnedReaders = new LinkedList<Thread>();
		while(threadsToRun.size() > 0 || spawnedReaders.size() > 0){
			
			//------------------------------------
			// spawn as many threads as needed to keep 
			// numConcurrentThreads running at once
			Thread r;
			while(threadsToRun.size()>0 && spawnedReaders.size()<numConcurrentThreads){
				r =  (Thread) ( threadsToRun).removeFirst(); // retrieve and remove from list
				spawnedReaders.add(r);
				r.start(); // will cause the run method to be executed
			}

			//------------------------------------
			// update the progress bar
			int doneCount = totalToLoad-threadsToRun.size()-spawnedReaders.size();
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
			int waitMillis = 50;
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
