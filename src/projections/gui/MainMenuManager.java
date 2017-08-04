package projections.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.Toolkit;

import java.util.logging.Level;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import projections.Tools.MessageSizeEvolution.MessageSizeEvolutionWindow;
import projections.Tools.PerformanceCounters.PerfWindow;
import projections.Tools.CommunicationOverTime.CommTimeWindow;
import projections.Tools.CommunicationPerPE.CommWindow;
import projections.Tools.Extrema.ExtremaWindow;
import projections.Tools.Histogram.HistogramWindow;
import projections.Tools.LogFileViewer.LogFileViewerWindow;
import projections.Tools.MemoryUsage.MemoryUsageWindow;
import projections.Tools.NoiseMiner.NoiseMinerWindow;
import projections.Tools.Overview.OverviewWindow;
import projections.Tools.EntryMethodProfile.MethodProfileWindow;
import projections.Tools.Streaming.StreamingTool;
import projections.Tools.TimeProfile.TimeProfileWindow;
import projections.Tools.Timeline.TimelineWindow;
import projections.Tools.UserEvents.UserEventsWindow;
import projections.Tools.UserStatsOverTime.UserStatsTimeWindow;
import projections.Tools.UserStatsPerPE.UserStatsProcWindow;
import projections.analysis.ProjMain;

/* ***************************************************
 * MainMenuManager.java
 * Chee Wai Lee - 11/7/2002
 * Isaac - 1/22/2009
 * 
 * This is the class controlling the menus for the MainWindow
 * in projections. It will implement the state machine and
 * communication interface with MainWindow.
 * 
 * This class has been simplified to use no self-reflection.
 * This simpler version can be succesfully analyzed by fancy
 * tools in Eclipse.
 *
 * ***************************************************/

class MainMenuManager
implements ActionListener, ItemListener
{
	private JMenuBar menubar;

	private JMenu fileMenu;
	private JMenu toolMenu;
	private JMenu debugMenu;
	private JMenu phaseRangeMenu;


	//    private static final int NUM_STATES = 4;
	private static final int NO_DATA = 0;
	private static final int OPENED_FILES = 1;
	private static final int OPENED_SUMMARY = 2;
	//    private static final int ADD_POSE = 3;

	private MainWindow parent;

	// The menu items for the file menu
	private JMenuItem fileOpenMenuItem;
	private JMenuItem fileCloseMenuItem;
	private JMenuItem fileCloseAllMenuItem;
	private JMenuItem fileQuitMenuItem;


	// The menu items for each tool in the tool menu
	private JMenuItem timelinesMenuItem;
	//private JMenuItem renderedTimelinesMenuItem;
	private JMenuItem usageProfileMenuItem;
	private JMenuItem communicationMenuItem;
	private JMenuItem communicationVsTimeMenuItem;
	private JMenuItem callTableMenuItem;
	private JMenuItem viewLogFilesMenuItem;
	private JMenuItem histogramsMenuItem;
	private JMenuItem overviewMenuItem;
	private JMenuItem userEventsMenuItem;
	//Menu Items for the User Stat Tools
	private JMenuItem userStatsTimeMenuItem;
	private JMenuItem userStatsProcMenuItem;

	private JMenuItem outlierAnalysisMenuItem;
	private JMenuItem animationMenuItem;
	private JMenuItem timeProfileGraphMenuItem;
	private JMenuItem perfCounterMenuItem;
	private JMenuItem multirunAnalysisMenuItem;
	private JMenuItem noiseMinerMenuItem;
	private JMenuItem streamingMenuItem;
	private JMenuItem memoryUsageMenuItem;
	private JMenuItem methodProfileMenuItem;
	private JMenuItem messageSizeEvolutionMenuItem;

	private JCheckBoxMenuItem perfLogMenuItem;

	//The menu items for the phase/range menu
	private JMenuItem addNewPhase;
	private JMenuItem listPhases;
	private JMenuItem listRanges;

	protected MainMenuManager(JFrame parent) {
		this.parent = (MainWindow)parent;
		createMenus();
	}

	private void stateChanged(int state, int sumDetail) {
		switch (state) {
		case NO_DATA :
			
			fileOpenMenuItem.setEnabled(true);
			fileCloseMenuItem.setEnabled(false);
			fileCloseAllMenuItem.setEnabled(false);
			fileQuitMenuItem.setEnabled(true);

			//renderedTimelinesMenuItem.setEnabled(false);
			timelinesMenuItem.setEnabled(false);
			usageProfileMenuItem.setEnabled(false);
			communicationMenuItem.setEnabled(false);
			communicationVsTimeMenuItem.setEnabled(false);
			callTableMenuItem.setEnabled(false);
			viewLogFilesMenuItem.setEnabled(false);
			histogramsMenuItem.setEnabled(false);
			overviewMenuItem.setEnabled(false);
			animationMenuItem.setEnabled(false);
			timeProfileGraphMenuItem.setEnabled(false);
			perfCounterMenuItem.setEnabled(false);
			userEventsMenuItem.setEnabled(false);
			userStatsTimeMenuItem.setEnabled(false);
			userStatsProcMenuItem.setEnabled(false);
			outlierAnalysisMenuItem.setEnabled(false);
			multirunAnalysisMenuItem.setEnabled(true);
			noiseMinerMenuItem.setEnabled(false);
			memoryUsageMenuItem.setEnabled(false);
			methodProfileMenuItem.setEnabled(false);
			messageSizeEvolutionMenuItem.setEnabled(false);

			addNewPhase.setEnabled(false);
			listPhases.setEnabled(false);
			listRanges.setEnabled(false);

			break;
		case OPENED_SUMMARY:

			fileOpenMenuItem.setEnabled(true);
			fileCloseMenuItem.setEnabled(true);
			fileCloseAllMenuItem.setEnabled(true);
			fileQuitMenuItem.setEnabled(true);

			//renderedTimelinesMenuItem.setEnabled(false);
			timelinesMenuItem.setEnabled(false);
			usageProfileMenuItem.setEnabled(true);
			communicationMenuItem.setEnabled(false);
			communicationVsTimeMenuItem.setEnabled(false);
			callTableMenuItem.setEnabled(false);
			viewLogFilesMenuItem.setEnabled(false);
			histogramsMenuItem.setEnabled(false);
			overviewMenuItem.setEnabled(true);
			animationMenuItem.setEnabled(true);
                        if(sumDetail==1)
			    timeProfileGraphMenuItem.setEnabled(true);
			else
                            timeProfileGraphMenuItem.setEnabled(false);
			perfCounterMenuItem.setEnabled(false);
			userEventsMenuItem.setEnabled(false);
			userStatsTimeMenuItem.setEnabled(false);
			userStatsProcMenuItem.setEnabled(false);
			outlierAnalysisMenuItem.setEnabled(false);
			multirunAnalysisMenuItem.setEnabled(true);
			noiseMinerMenuItem.setEnabled(true);
			memoryUsageMenuItem.setEnabled(true);
			methodProfileMenuItem.setEnabled(false);
			messageSizeEvolutionMenuItem.setEnabled(false);

			if(sumDetail == 1)
				addNewPhase.setEnabled(true);
			else
				addNewPhase.setEnabled(false);
			listPhases.setEnabled(true);
			listRanges.setEnabled(true);

			break;
		case OPENED_FILES :

			fileOpenMenuItem.setEnabled(true);
			fileCloseMenuItem.setEnabled(true);
			fileCloseAllMenuItem.setEnabled(true);
			fileQuitMenuItem.setEnabled(true);

			//renderedTimelinesMenuItem.setEnabled(true);
			timelinesMenuItem.setEnabled(true);
			usageProfileMenuItem.setEnabled(true);
			communicationMenuItem.setEnabled(true);
			communicationVsTimeMenuItem.setEnabled(true);
			callTableMenuItem.setEnabled(true);
			viewLogFilesMenuItem.setEnabled(true);
			histogramsMenuItem.setEnabled(true);
			overviewMenuItem.setEnabled(true);
			animationMenuItem.setEnabled(true);
			timeProfileGraphMenuItem.setEnabled(true);
			if (MainWindow.runObject[0].getSts().getNumPerfCounts() > 0) {
				perfCounterMenuItem.setEnabled(true);
			}
			else {
				perfCounterMenuItem.setEnabled(false);
			}
			userEventsMenuItem.setEnabled(true);
			if (MainWindow.runObject[0].getSts().getNumUserDefinedStats() > 0) {
				userStatsTimeMenuItem.setEnabled(true);
				userStatsProcMenuItem.setEnabled(true);
			}
			else {
				userStatsTimeMenuItem.setEnabled(false);
				userStatsProcMenuItem.setEnabled(false);
			}
			outlierAnalysisMenuItem.setEnabled(true);
			multirunAnalysisMenuItem.setEnabled(true);
			noiseMinerMenuItem.setEnabled(true);
			memoryUsageMenuItem.setEnabled(true);
			methodProfileMenuItem.setEnabled(true);
			messageSizeEvolutionMenuItem.setEnabled(true);

			addNewPhase.setEnabled(true);
			listPhases.setEnabled(true);
			listRanges.setEnabled(true);

			break;
		}
	}


	/** Create a default set of menus. Some tools replace the menu bar with their own. */
	private void createMenus() {
		menubar = new JMenuBar();

//    	System.out.println("MainMenuManager Create Menus");
		
		// FILE MENU	
		fileMenu = new JMenu("File");		
		fileOpenMenuItem = new JMenuItem("Open File(s)");
		fileOpenMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		fileCloseMenuItem= new JMenuItem("Close current data");
		fileCloseAllMenuItem= new JMenuItem("Close all data");
		fileQuitMenuItem= new JMenuItem("Quit");	
		fileQuitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		fileMenu.addActionListener(this);
		fileOpenMenuItem.addActionListener(this);
		fileCloseMenuItem.addActionListener(this);
		fileCloseAllMenuItem.addActionListener(this);
		fileQuitMenuItem.addActionListener(this);

		fileMenu.add(fileMenu);
		fileMenu.add(fileOpenMenuItem);
		fileMenu.add(fileCloseMenuItem);
		fileMenu.add(fileCloseAllMenuItem);
		fileMenu.add(fileQuitMenuItem);

		menubar.add(fileMenu);

		// TOOLS MENU
		toolMenu = new JMenu("Tools");

		timelinesMenuItem = new JMenuItem("Timelines");
		//renderedTimelinesMenuItem = new JMenuItem("Timelines - rendered to image");
		usageProfileMenuItem = new JMenuItem("Usage Profile");
		communicationMenuItem = new JMenuItem("Communication Per Processor");
		communicationVsTimeMenuItem = new JMenuItem("Communication Over Time");
		callTableMenuItem = new JMenuItem("Call Table");
		viewLogFilesMenuItem = new JMenuItem("View Log Files");
		histogramsMenuItem = new JMenuItem("Histograms");
		overviewMenuItem = new JMenuItem("Overview");
		animationMenuItem = new JMenuItem("Animation");
		timeProfileGraphMenuItem = new JMenuItem("Time Profile");
		perfCounterMenuItem = new JMenuItem("Performance Counters");
		userEventsMenuItem = new JMenuItem("User Events");
		userStatsTimeMenuItem = new JMenuItem("User Stats Over Time");
		userStatsProcMenuItem = new JMenuItem("User Stats Per Processor");
		outlierAnalysisMenuItem = new JMenuItem("Extrema Analysis");
		multirunAnalysisMenuItem = new JMenuItem("Multirun Analysis");
		noiseMinerMenuItem = new JMenuItem("Noise Miner");
		streamingMenuItem = new JMenuItem("Streaming CCS");
		memoryUsageMenuItem = new JMenuItem("Memory Usage");
		methodProfileMenuItem = new JMenuItem("Entry Method Profile");
		messageSizeEvolutionMenuItem = new JMenuItem("Message Size Evolution");

		timelinesMenuItem.addActionListener(this);
		//renderedTimelinesMenuItem.addActionListener(this);
		usageProfileMenuItem.addActionListener(this);
		communicationMenuItem.addActionListener(this);
		communicationVsTimeMenuItem.addActionListener(this);
		callTableMenuItem.addActionListener(this);
		viewLogFilesMenuItem.addActionListener(this);
		histogramsMenuItem.addActionListener(this);
		overviewMenuItem.addActionListener(this);
		animationMenuItem.addActionListener(this);
		timeProfileGraphMenuItem.addActionListener(this);
		perfCounterMenuItem.addActionListener(this);
		userEventsMenuItem.addActionListener(this);
		userStatsTimeMenuItem.addActionListener(this);
		userStatsProcMenuItem.addActionListener(this);
		outlierAnalysisMenuItem.addActionListener(this);
		multirunAnalysisMenuItem.addActionListener(this);
		noiseMinerMenuItem.addActionListener(this);
		streamingMenuItem.addActionListener(this);
		memoryUsageMenuItem.addActionListener(this);
		methodProfileMenuItem.addActionListener(this);
		messageSizeEvolutionMenuItem.addActionListener(this);

		toolMenu.add(timelinesMenuItem);
		//toolMenu.add(renderedTimelinesMenuItem);
		toolMenu.add(usageProfileMenuItem);
		toolMenu.add(communicationMenuItem);
		toolMenu.add(communicationVsTimeMenuItem);
		toolMenu.add(callTableMenuItem);
		toolMenu.add(viewLogFilesMenuItem);
		toolMenu.add(histogramsMenuItem);
		toolMenu.add(overviewMenuItem);
		toolMenu.add(animationMenuItem);
		toolMenu.add(timeProfileGraphMenuItem);
		toolMenu.add(perfCounterMenuItem);
		toolMenu.add(userEventsMenuItem);
		toolMenu.add(userStatsTimeMenuItem);
		toolMenu.add(userStatsProcMenuItem);
		toolMenu.add(outlierAnalysisMenuItem);
		toolMenu.add(multirunAnalysisMenuItem);
		toolMenu.add(noiseMinerMenuItem);
		toolMenu.add(streamingMenuItem);
		toolMenu.add(memoryUsageMenuItem);
		toolMenu.add(methodProfileMenuItem);
		toolMenu.add(messageSizeEvolutionMenuItem);

		menubar.add(toolMenu);

		

		// DEBUG/Logging MENU
		debugMenu = new JMenu("Debug");
		debugMenu.setToolTipText("For debugging or optimizing the projections tools");
		
		perfLogMenuItem = new JCheckBoxMenuItem("Enable Logging of Performance Measurements");
		perfLogMenuItem.addActionListener(this);
		debugMenu.add(perfLogMenuItem);
		menubar.add(debugMenu);

		// Phase MENU
		phaseRangeMenu = new JMenu("Phase & Range Info");
		phaseRangeMenu.setToolTipText("To add new phase configs");

		addNewPhase = new JMenuItem("Add new Phase Config");
		addNewPhase.addActionListener(this);
		listPhases = new JMenuItem("List all Phase Configs");
		listPhases.addActionListener(this);
		listRanges = new JMenuItem("List all Range Entries");
		listRanges.addActionListener(this);

		phaseRangeMenu.add(addNewPhase);
		phaseRangeMenu.add(listPhases);
		phaseRangeMenu.add(listRanges);
		menubar.add(phaseRangeMenu);
		
		parent.setJMenuBar(menubar);

		stateChanged(NO_DATA, 0);
	}

	
	

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem mi = (JMenuItem)e.getSource();
//			String arg = mi.getText();

			if(mi == fileOpenMenuItem)
				parent.showOpenFileDialog();
			
			else if (mi == fileCloseMenuItem) 
				parent.closeCurrent();
			
			else if (mi == fileCloseAllMenuItem) 
				parent.closeAll();
			
			else if (mi == fileQuitMenuItem) 
				ProjMain.shutdown(0);
			
			else if (mi == perfLogMenuItem){
				if(perfLogMenuItem.isSelected()){
					MainWindow.performanceLogger.setLevel(Level.ALL);
				} else {
					MainWindow.performanceLogger.setLevel(Level.OFF);
				}
			}
			
			else if (mi == timelinesMenuItem)
				parent.openTool(new TimelineWindow(parent) );

			//else if (mi == renderedTimelinesMenuItem)
			//	parent.openTool(new TimelineRenderedWindow(parent) );
			
			else if (mi == usageProfileMenuItem)
				parent.openTool(new ProfileWindow(parent) ); 
			
			else if (mi == communicationMenuItem)	
				parent.openTool(new CommWindow(parent) );
			
			else if (mi == communicationVsTimeMenuItem)	
				parent.openTool(new CommTimeWindow(parent) );
			
			else if (mi == callTableMenuItem)	
				parent.openTool(new CallTableWindow(parent) ); 
			
			else if (mi == viewLogFilesMenuItem)	
				parent.openTool(new LogFileViewerWindow(parent) );
			
			else if (mi == histogramsMenuItem)	
				parent.openTool(new HistogramWindow(parent) );
			
			else if (mi == overviewMenuItem)	
				parent.openTool(new OverviewWindow(parent) );
			
			else if (mi == animationMenuItem)	
				parent.openTool(new AnimationWindow(parent) );
			
			else if (mi == timeProfileGraphMenuItem)	{
                                System.out.println("timeProfileGraphMenuItem is being created\n");
				parent.openTool(new TimeProfileWindow(parent) );
			}

			else if (mi == perfCounterMenuItem)
				parent.openTool(new PerfWindow(parent) );

			else if (mi == userEventsMenuItem)	
				parent.openTool(new UserEventsWindow(parent) );
			
			else if (mi == userStatsTimeMenuItem)
				parent.openTool(new UserStatsTimeWindow(parent) );
	
			else if (mi == userStatsProcMenuItem)
				parent.openTool(new UserStatsProcWindow(parent) );

			else if (mi == outlierAnalysisMenuItem)	
				parent.openTool(new ExtremaWindow(parent) );
			
			else if (mi == multirunAnalysisMenuItem)	
				parent.openTool(new MultiRunWindow(parent) );

			else if (mi == noiseMinerMenuItem)	
				parent.openTool(new NoiseMinerWindow(parent) );
			
			else if (mi == memoryUsageMenuItem)
				parent.openTool(new MemoryUsageWindow(parent) );
			
			else if (mi == streamingMenuItem)	
				new StreamingTool();
			
			else if (mi == methodProfileMenuItem)
				parent.openTool(new MethodProfileWindow(parent) );

			else if(mi == messageSizeEvolutionMenuItem)
				parent.openTool(new MessageSizeEvolutionWindow(parent));

			else if(mi == addNewPhase)
				parent.openTool(new PhaseWindow(parent, -1));

			else if(mi == listPhases)
				parent.openTool(new PhaseListWindow(parent));

			else if(mi == listRanges)
				parent.openTool(new RangeListWindow(parent));

			else 
				System.out.println("ERROR: unknown menu item was selected" + mi);
			
		}
	}

	public void itemStateChanged(ItemEvent e) {
	}

	// Interface methods to MainWindow
	protected void fileOpened() {
		stateChanged(OPENED_FILES, 0);
	}

	protected void lastFileClosed() {
		stateChanged(NO_DATA, 0);
	}

	protected void summaryOnly(int sumDetail) {
		stateChanged(OPENED_SUMMARY, sumDetail);
	}

}
