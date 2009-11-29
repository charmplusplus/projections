package projections.Tools.TimelineRendered;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import projections.Tools.Timeline.Data;
import projections.Tools.Timeline.MainHandler;
import projections.analysis.ThreadManager;
import projections.gui.JPanelToImage;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.ProjectionsWindow;
import projections.gui.RangeDialog;
import projections.gui.Util;

public class TimelineRenderedWindow extends ProjectionsWindow implements MainHandler {

	Color backgroundColor;
	Color foregroundColor;
	int width;
	int height;
	
	JMenuItem mSave;
	
	JPanel combinedTimelinesPanel;
	
	public TimelineRenderedWindow(MainWindow parentWindow) {
		super(parentWindow);
		createMenus();
		
		showDialog();
	}

	
	  protected void createMenus(){
	        JMenuBar mbar = new JMenuBar();
	        mbar.add(Util.makeJMenu("File", new Object[]
	            {
	                "Select Processors",
	                null,
	                                    "Close"
	            },
	                                null, this));
	        
	        
	        
	        JMenu saveMenu = new JMenu("Save To Image");
			mSave = new JMenuItem("Save as JPG or PNG");
			menuHandler mh = new menuHandler();
			mSave.addActionListener(mh);
			saveMenu.add(mSave);

			mbar.add(saveMenu);
	        
	        setJMenuBar(mbar);
	    }

	
	public class menuHandler implements ActionListener  {

		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == mSave){
				JPanelToImage.saveToFileChooserSelection(combinedTimelinesPanel, "Save Timeline Image", "./TimelineScreenshot.png");
			}
			
		}
		
	}
	  
	
	
	/** Resize my panels(required by interface, called by data object) */
	// The data object has been instructed to change the display width
	// The scale factor or window size has likely changed.
	// Do not call data.setScreenWidth() in here
	public void refreshDisplay(boolean doRevalidate){
		// do nothing
	}


	/** Required by interface MainHandler. This one does nothing */
	public void notifyProcessorListHasChanged() {
		// Do nothing
	}


	public void displayWarning(String message) {
		// Do nothing
	}


	protected void showDialog() {

		if (dialog == null) {
			dialog = new RangeDialog(this, "Select Range", null, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()){

			OrderedIntList processorList = dialog.getSelectedProcessors();
			long startTime = dialog.getStartTime();
			long endTime = dialog.getEndTime();
			backgroundColor = Color.white;
			foregroundColor = Color.black;
			width = 1000;
			
			final Date time1  = new Date();

			// Create a list of worker threads
			final LinkedList<Thread> readyReaders = new LinkedList<Thread>();

			processorList.reset();
			int pIdx=0;		
			while (processorList.hasMoreElements()) {
				int nextPe = processorList.nextElement();
				readyReaders.add( new ThreadedFileReaderTimelineRendered(nextPe, startTime, endTime, backgroundColor, foregroundColor, width) );
				pIdx++;
			}

			// Determine a component to show the progress bar with
			Component guiRootForProgressBar = null;

			// Pass this list of threads to a class that manages/runs the threads nicely
			final ThreadManager threadManager = new ThreadManager("Rendering Timelines in Parallel", readyReaders, guiRootForProgressBar, true);


			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					threadManager.runThreads();
					return null;
				}

				public void done() {
					
					combinedTimelinesPanel = new JPanel();
					combinedTimelinesPanel.setLayout(new BoxLayout(combinedTimelinesPanel, BoxLayout.PAGE_AXIS));

					// Merge resulting images together.
					Iterator<Thread> iter = readyReaders.iterator();
					while(iter.hasNext()){
						ThreadedFileReaderTimelineRendered r = (ThreadedFileReaderTimelineRendered) iter.next();
						BufferedImage i = r.getImage();
						JLabel l = new JLabel(new ImageIcon(i));
						l.setToolTipText("PE " + r.PE);
						combinedTimelinesPanel.add(l);
						width = i.getWidth();
						height = i.getHeight();
					}
					
					int totalHeight = readyReaders.size() * height;
					combinedTimelinesPanel.setPreferredSize(new Dimension(width, totalHeight));


					// put it in a scrolling pane
					JScrollPane scrollpane = new JScrollPane(combinedTimelinesPanel);
					scrollpane.getVerticalScrollBar();
					scrollpane.getHorizontalScrollBar();
					scrollpane.setPreferredSize(new Dimension(width+JScrollBar.WIDTH,
							totalHeight + JScrollBar.HEIGHT));

					setLayout(scrollpane);
					pack();
					setVisible(true);
					
					Date time4  = new Date();

					double totalTime = ((time4.getTime() - time1.getTime())/1000.0);
					System.out.println("Time to render " + threadManager.numInitialThreads +  
							" input PE Timelines (using " + threadManager.numConcurrentThreads + " concurrent threads): " + 
							totalTime + "sec");	

				}
			};
			worker.execute();   

		}
	}


	public void setData(Data data) {
		//		do nothing		
	}		


}