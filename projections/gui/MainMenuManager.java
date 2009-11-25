package projections.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;


import projections.ExtremaTool.OutlierAnalysisWindow;
import projections.Overview.OverviewWindow;
import projections.TimeProfile.TimeProfileWindow;
import projections.analysis.ProjMain;
import projections.gui.LogFileViewer.LogFileViewerWindow;
import projections.gui.Timeline.TimelineWindow;
import projections.gui.TimelineRendered.RenderedPE;
import projections.streaming.StreamingTool;

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

public class MainMenuManager
implements ActionListener, ItemListener
{
	private JMenuBar menubar;

	private JMenu fileMenu;
	private JMenu preferencesMenu;
	private JMenu toolMenu;

	private JMenuItem graphMenuItem;


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

	// The menu items for the preferences menu
	private JMenuItem changeBGColorMenuItem;
	private JMenuItem changeFGColorMenuItem;
	private JMenuItem useGrayscaleColorsMenuItem;	
	private JMenuItem useStandardColorsMenuItem;

	// The menu items for each tool in the tool menu
	private JMenuItem timelinesMenuItem;
	private JMenuItem renderedTimelinesMenuItem;
	private JMenuItem usageProfileMenuItem;
	private JMenuItem communicationMenuItem;
	private JMenuItem communicationVsTimeMenuItem;
	private JMenuItem callTableMenuItem;
	private JMenuItem viewLogFilesMenuItem;
	private JMenuItem histogramsMenuItem;
	private JMenuItem overviewMenuItem;
	private JMenuItem userEventsMenuItem;
	private JMenuItem outlierAnalysisMenuItem;
	private JMenuItem animationMenuItem;
	private JMenuItem timeProfileGraphMenuItem;
	private JMenuItem multirunAnalysisMenuItem;
	private JMenuItem functionToolMenuItem;
	private JMenuItem AMPIUsageProfileMenuItem;
	private JMenuItem noiseMinerMenuItem;
	private JMenuItem streamingMenuItem;


	public MainMenuManager(JFrame parent) {
		this.parent = (MainWindow)parent;
		createMenus();
	}

	void stateChanged(int state) {
		switch (state) {
		case NO_DATA :
			
			fileOpenMenuItem.setEnabled(true);
			fileCloseMenuItem.setEnabled(false);
			fileCloseAllMenuItem.setEnabled(false);
			fileQuitMenuItem.setEnabled(true);

			changeBGColorMenuItem.setEnabled(true);
			changeFGColorMenuItem.setEnabled(false);
			useGrayscaleColorsMenuItem.setEnabled(false);	
			useStandardColorsMenuItem.setEnabled(false);
			
			graphMenuItem.setEnabled(false);
			renderedTimelinesMenuItem.setEnabled(false);
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
			userEventsMenuItem.setEnabled(false);
			outlierAnalysisMenuItem.setEnabled(false);
			multirunAnalysisMenuItem.setEnabled(true);
			functionToolMenuItem.setEnabled(false);
			AMPIUsageProfileMenuItem.setEnabled(false);
			noiseMinerMenuItem.setEnabled(false);


			break;
		case OPENED_SUMMARY:

			fileOpenMenuItem.setEnabled(true);
			fileCloseMenuItem.setEnabled(true);
			fileCloseAllMenuItem.setEnabled(true);
			fileQuitMenuItem.setEnabled(true);

			changeBGColorMenuItem.setEnabled(true);
			changeFGColorMenuItem.setEnabled(true);
			useGrayscaleColorsMenuItem.setEnabled(false);	
			useStandardColorsMenuItem.setEnabled(false);
			
			graphMenuItem.setEnabled(true);
			renderedTimelinesMenuItem.setEnabled(false);
			timelinesMenuItem.setEnabled(false);
			usageProfileMenuItem.setEnabled(true);
			communicationMenuItem.setEnabled(false);
			communicationVsTimeMenuItem.setEnabled(false);
			callTableMenuItem.setEnabled(false);
			viewLogFilesMenuItem.setEnabled(false);
			histogramsMenuItem.setEnabled(false);
			overviewMenuItem.setEnabled(true);
			animationMenuItem.setEnabled(true);
			timeProfileGraphMenuItem.setEnabled(false);
			userEventsMenuItem.setEnabled(false);
			outlierAnalysisMenuItem.setEnabled(false);
			multirunAnalysisMenuItem.setEnabled(true);
			functionToolMenuItem.setEnabled(false);
			AMPIUsageProfileMenuItem.setEnabled(true);
			noiseMinerMenuItem.setEnabled(true);

			break;
		case OPENED_FILES :

			fileOpenMenuItem.setEnabled(true);
			fileCloseMenuItem.setEnabled(true);
			fileCloseAllMenuItem.setEnabled(true);
			fileQuitMenuItem.setEnabled(true);

			changeBGColorMenuItem.setEnabled(true);
			changeFGColorMenuItem.setEnabled(true);
			useGrayscaleColorsMenuItem.setEnabled(false);	
			useStandardColorsMenuItem.setEnabled(false);	

			graphMenuItem.setEnabled(true);
			renderedTimelinesMenuItem.setEnabled(true);
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
			userEventsMenuItem.setEnabled(true);
			outlierAnalysisMenuItem.setEnabled(true);
			multirunAnalysisMenuItem.setEnabled(true);
			functionToolMenuItem.setEnabled(true);
			AMPIUsageProfileMenuItem.setEnabled(true);
			noiseMinerMenuItem.setEnabled(true);

			break;
		}
	}


	/** Create a default set of menus. Some tools replace the menu bar with their own. */
	private void createMenus() {
		menubar = new JMenuBar();

		// FILE MENU	
		fileMenu = new JMenu("File");		
		fileOpenMenuItem = new JMenuItem("Open File(s)");
		fileCloseMenuItem= new JMenuItem("Close current data");
		fileCloseAllMenuItem= new JMenuItem("Close all data");
		fileQuitMenuItem= new JMenuItem("Quit");	

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


		// PREFERENCES MENU
		preferencesMenu = new JMenu("Preferences");

		changeBGColorMenuItem = new JMenuItem("Change Background Color");	
		changeFGColorMenuItem = new JMenuItem("Change Foreground Color");	
		useGrayscaleColorsMenuItem = new JMenuItem("Use Default Grayscale Colors");	
		useStandardColorsMenuItem = new JMenuItem("Use Standard Colors");	

		changeBGColorMenuItem.addActionListener(this);
		changeFGColorMenuItem.addActionListener(this);
		useGrayscaleColorsMenuItem.addActionListener(this);
		useStandardColorsMenuItem.addActionListener(this);

		preferencesMenu.add(changeBGColorMenuItem);
		preferencesMenu.add(changeFGColorMenuItem);
		preferencesMenu.add(useGrayscaleColorsMenuItem);
		preferencesMenu.add(useStandardColorsMenuItem);

		menubar.add(preferencesMenu);



		// TOOLS MENU
		toolMenu = new JMenu("Tools");

		graphMenuItem = new JMenuItem("Graphs");
		timelinesMenuItem = new JMenuItem("Timelines");
		renderedTimelinesMenuItem = new JMenuItem("Timelines - rendered to image");
		usageProfileMenuItem = new JMenuItem("Usage Profile");
		communicationMenuItem = new JMenuItem("Communication");
		communicationVsTimeMenuItem = new JMenuItem("Communication vs Time");
		callTableMenuItem = new JMenuItem("Call Table");
		viewLogFilesMenuItem = new JMenuItem("View Log Files");
		histogramsMenuItem = new JMenuItem("Histograms");
		overviewMenuItem = new JMenuItem("Overview");
		animationMenuItem = new JMenuItem("Animation");
		timeProfileGraphMenuItem = new JMenuItem("Time Profile Graph");
		userEventsMenuItem = new JMenuItem("User Events");
		outlierAnalysisMenuItem = new JMenuItem("Extrema Analysis");
		multirunAnalysisMenuItem = new JMenuItem("Multirun Analysis");
		functionToolMenuItem = new JMenuItem("Function Tool");
		AMPIUsageProfileMenuItem = new JMenuItem("AMPI Usage Profile");
		noiseMinerMenuItem = new JMenuItem("Noise Miner");
		streamingMenuItem = new JMenuItem("Streaming CCS Tool");
		
		graphMenuItem.addActionListener(this);
		timelinesMenuItem.addActionListener(this);
		renderedTimelinesMenuItem.addActionListener(this);
		usageProfileMenuItem.addActionListener(this);
		communicationMenuItem.addActionListener(this);
		communicationVsTimeMenuItem.addActionListener(this);
		callTableMenuItem.addActionListener(this);
		viewLogFilesMenuItem.addActionListener(this);
		histogramsMenuItem.addActionListener(this);
		overviewMenuItem.addActionListener(this);
		animationMenuItem.addActionListener(this);
		timeProfileGraphMenuItem.addActionListener(this);
		userEventsMenuItem.addActionListener(this);
		outlierAnalysisMenuItem.addActionListener(this);
		multirunAnalysisMenuItem.addActionListener(this);
		functionToolMenuItem.addActionListener(this);
		AMPIUsageProfileMenuItem.addActionListener(this);
		noiseMinerMenuItem.addActionListener(this);
		streamingMenuItem.addActionListener(this);

		toolMenu.add(graphMenuItem);
		toolMenu.add(timelinesMenuItem);
		toolMenu.add(renderedTimelinesMenuItem);
		toolMenu.add(usageProfileMenuItem);
		toolMenu.add(communicationMenuItem);
		toolMenu.add(communicationVsTimeMenuItem);
		toolMenu.add(callTableMenuItem);
		toolMenu.add(viewLogFilesMenuItem);
		toolMenu.add(histogramsMenuItem);
		toolMenu.add(overviewMenuItem);
		toolMenu.add(animationMenuItem);
		toolMenu.add(timeProfileGraphMenuItem);
		toolMenu.add(userEventsMenuItem);
		toolMenu.add(outlierAnalysisMenuItem);
		toolMenu.add(multirunAnalysisMenuItem);
		toolMenu.add(functionToolMenuItem);
		toolMenu.add(AMPIUsageProfileMenuItem);
		toolMenu.add(noiseMinerMenuItem);
		toolMenu.add(streamingMenuItem);
		
		menubar.add(toolMenu);

		parent.setJMenuBar(menubar);

		stateChanged(NO_DATA);
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem mi = (JMenuItem)e.getSource();
			String arg = mi.getText();

			if(mi == fileOpenMenuItem)
				parent.showOpenFileDialog();
			
			else if (mi == fileCloseMenuItem) 
				parent.closeCurrent();
			
			else if (mi == fileCloseAllMenuItem) 
				parent.closeAll();
			
			else if (mi == fileQuitMenuItem) 
				ProjMain.shutdown(0);
			
			else if (mi == changeBGColorMenuItem)
				parent.changeBackground();
			
			else if (mi == changeFGColorMenuItem)
				parent.changeForeground();
			
			else if (mi == useGrayscaleColorsMenuItem)
				parent.setGrayscale();
			
			else if (mi == useStandardColorsMenuItem)
				parent.setFullColor();
			
			else if (mi == graphMenuItem)
				parent.openTool(new GraphWindow(parent) );
			
			else if (mi == timelinesMenuItem)
				parent.openTool(new TimelineWindow(parent) );

			else if (mi == renderedTimelinesMenuItem)
				parent.openTool(new RenderedPE(parent) );
			
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
			
			else if (mi == timeProfileGraphMenuItem)	
				parent.openTool(new TimeProfileWindow(parent) );
			
			else if (mi == userEventsMenuItem)	
				parent.openTool(new UserEventsWindow(parent) );
			
			else if (mi == outlierAnalysisMenuItem)	
				parent.openTool(new OutlierAnalysisWindow(parent) );
			
			else if (mi == multirunAnalysisMenuItem)	
				parent.openTool(new MultiRunWindow(parent) );
			
			else if (mi == functionToolMenuItem)	
				parent.openTool(new FunctionTool(parent) );
			
			else if (mi == AMPIUsageProfileMenuItem)	
				parent.openTool(new AmpiProfileWindow(parent) );
			
			else if (mi == noiseMinerMenuItem)	
				parent.openTool(new NoiseMinerWindow(parent) );
			
			else if (mi == streamingMenuItem)	
				new StreamingTool();
			
			else 
				System.out.println("ERROR: unknown menu item was selected" + mi);
			
		}
	}

	public void itemStateChanged(ItemEvent e) {
	}

	// Interface methods to MainWindow
	public void fileOpened() {
		stateChanged(OPENED_FILES);
	}

	public void lastFileClosed() {
		stateChanged(NO_DATA);
	}

	public void summaryOnly() {
		stateChanged(OPENED_SUMMARY);
	}

}
