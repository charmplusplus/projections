package projections.analysis;

import java.awt.Component;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ProgressMonitor;


/** This class manages a pool of worker threads, updating a progress bar as threads complete. 
 * 
 * The class must be provided with a list of Runnable objects that will be assigned to threads by me. 
 * 
 * */
public class ThreadManager {

	/** A copy of the list of threads */
	private LinkedList<Runnable> runableObjects;
	public int numInitialThreads;

	private boolean showProgress;

	private String description;

	public int numConcurrentThreads;

	private Component guiRootForProgressBar;

	public ThreadManager(String description, List<Runnable> runableObjects, Component guiRoot, boolean showProgress){
		this.runableObjects = new LinkedList<Runnable>();
		this.runableObjects.addAll(runableObjects);
		this.description = description;
		this.numInitialThreads = runableObjects.size();
		this.guiRootForProgressBar = guiRoot;
		this.showProgress = showProgress;

		int numProcs = Runtime.getRuntime().availableProcessors()*1;
		numConcurrentThreads = numProcs;

	}


	public void runThreads(){

		Date startReadingTime  = new Date();
		
		
		ProgressMonitor progressBar=null;
		if(showProgress){
			progressBar = new ProgressMonitor(guiRootForProgressBar, description,"", 0, numInitialThreads);
			progressBar.setMillisToPopup(0);
			progressBar.setMillisToDecideToPopup(0);
			progressBar.setProgress(0);
		}

		int totalToLoad = runableObjects.size();
		if(showProgress){
			progressBar.setMaximum(totalToLoad);
		}

		if(totalToLoad < numConcurrentThreads){
			numConcurrentThreads = totalToLoad;
		}

		// execute reader threads, a few at a time, until all have executed
		LinkedList<Thread> spawnedThreads = new LinkedList<Thread>();
		while(runableObjects.size() > 0 || spawnedThreads.size() > 0){

			//------------------------------------
			// spawn as many threads as needed to keep 
			// numConcurrentThreads running at once
			while(runableObjects.size()>0 && spawnedThreads.size()<numConcurrentThreads){
				Thread t = new Thread(runableObjects.removeFirst()); // retrieve and remove from list
				spawnedThreads.add(t);
				t.start(); // will cause the run method to be executed
			}

			//------------------------------------
			// update the progress bar
			if(showProgress){
				int doneCount = totalToLoad-runableObjects.size()-spawnedThreads.size();
				if (!progressBar.isCanceled()) {
					progressBar.setNote(doneCount+ " of " + totalToLoad);
					progressBar.setProgress(doneCount);
				} else {
					// user cancelled this operation
					// Wait on all spawned threads to finish
					Iterator<Thread> iter = spawnedThreads.iterator();
					while(iter.hasNext()){
						Thread t = iter.next();
						try {
							t.join(); 
							// All actions in a thread happen-before this thread successfully returns from a join on that thread.
							// See http://java.sun.com/javase/6/docs/api/java/util/concurrent/package-summary.html
						}
						catch (InterruptedException e) {
							throw new RuntimeException("Thread was interrupted. This should not ever occur");
						}
					}
					break;
				}
			}


			//------------------------------------
			// wait on the threads to complete
			Iterator<Thread> iter = spawnedThreads.iterator();
			if(iter.hasNext()){
				Thread t = iter.next();
				try {
					t.join(10);
					if(! t.isAlive()) {
						// Thread Finished
						iter.remove();
					}
				}
				catch (InterruptedException e) {
					throw new RuntimeException("Thread was interrupted. This should not ever occur");
				}
			}

		}
		
		Date endReadingTime  = new Date();
		System.out.println("Time to read " + numInitialThreads +  " input files(using " + numConcurrentThreads + " concurrent threads): " + ((double)(endReadingTime.getTime() - startReadingTime.getTime())/1000.0) + "sec");

		if(showProgress){
			progressBar.close();
		}
	}


}
