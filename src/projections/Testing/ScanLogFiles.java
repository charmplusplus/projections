package projections.Testing;


import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingWorker;

import projections.analysis.ProjMain;
import projections.analysis.ThreadManager;
import projections.gui.OrderedIntList;
import projections.gui.RangeDialog;


/** A tool to help determine how long the file reading ought to take. Why does it take so long for time profile? */

public class ScanLogFiles implements ActionListener 
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;
	
	
	public ScanLogFiles() {
	}

    public static void main(String args[]){
    	ProjMain.startup(args);
    	
    	// Add a new menu to the main window
    	JMenuBar menuBar = ProjMain.mainWindow.getJMenuBar();
    	
    	System.out.println("ScanLogFiles Create Menus");
    			
    	JMenu m = new JMenu("Experimental Tools:");
    	JMenuItem mi = new JMenuItem("Scan Log Files");
    	m.add(mi);
    	mi.addActionListener(new ScanLogFiles());
    	menuBar.add(m);
    
    }

    
    
	
	public void showDialog() {

		RangeDialog dialog = new RangeDialog(null, "Select Range", null, false);
		dialog.displayDialog();
		if (!dialog.isCancelled()){
			final OrderedIntList processorList = dialog.getSelectedProcessors();

			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {

						double fakeResult[] = new double[1];
					
						// Create a list of worker threads
						LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();

						int pIdx=0;		
						while (processorList.hasMoreElements()) {
							int nextPe = processorList.nextElement();
							readyReaders.add( new ThreadedFileReader(nextPe, myRun, fakeResult) );
							pIdx++;
						}

						// Determine a component to show the progress bar with
						Component guiRootForProgressBar = null;
					
						// Pass this list of threads to a class that manages/runs the threads nicely
						ThreadManager threadManager = new ThreadManager("Scanning Logs in Parallel", readyReaders, guiRootForProgressBar, true);
						threadManager.runThreads();
						return null;
				}
			};
			worker.execute();
		}
	}

	public void actionPerformed(ActionEvent e) {
		showDialog();
	}

}
