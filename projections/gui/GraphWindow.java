package projections.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.SwingWorker;

import projections.misc.PrintUtils;

public class GraphWindow extends ProjectionsWindow
implements ActionListener
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	GraphDisplayPanel      displayPanel; 
	GraphControlPanel      controlPanel;
	GraphLegendPanel       legendPanel;
	GraphData data;

	GraphWindow thisWindow;


	IntervalChooserPanel intervalPanel;

	OrderedIntList processorList;
	public static boolean dumpNow = false;
	public static int dumpCount = 0;

	public GraphWindow(MainWindow parentWindow)
	{
		super(parentWindow);

		thisWindow = this;
		setBackground(Color.lightGray);

		createLayout();
		createMenus();
		pack();

		setTitle("Projections Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts");

		processorList = MainWindow.runObject[myRun].getValidProcessorList();

		showDialog();
	}   

	public void refreshDisplay() {
		if (displayPanel != null) {
			displayPanel.refreshDisplay();
		}
		if (legendPanel != null) {
			legendPanel.repaint();
		}
	}


	public void actionPerformed(ActionEvent evt)
	{
		if (evt.getSource() instanceof MenuItem) {
			MenuItem mi = (MenuItem)evt.getSource();
			String arg = mi.getLabel();
			if(arg.equals("Close"))
				close();
			else if(arg.equals("Print Graph"))
				PrintGraph();   
			else if(arg.equals("Set Interval Size"))
				showDialog();
			else if(arg.equals("Dump Raw Data")) {
				try {
					File dumpFile = new File("GraphDump."+dumpCount+".out");
					while (dumpFile.exists()) {
						dumpCount++;
						dumpFile = 
							new File("GraphDump."+dumpCount+".out");
					}
					MainWindow.dataDump = 
						new PrintWriter(new FileWriter(dumpFile));
					dumpNow = true;
					refreshDisplay();
				} catch (IOException e) {
					System.err.println("Failure to handle dump data " +
							"GraphDump."+dumpCount+".out");
					System.exit(-1);
				}
			}
		}
	}   

	private void createLayout()
	{   
		Panel p = new Panel();
		getContentPane().add("Center", p);
		p.setBackground(Color.gray);

		displayPanel = new GraphDisplayPanel();
		controlPanel = new GraphControlPanel();
		legendPanel  = new GraphLegendPanel (this);

		GridBagLayout      gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		p.setLayout(gbl);

		gbc.fill   = GridBagConstraints.BOTH;
		Util.gblAdd(p, displayPanel, gbc, 0,0, 1,1, 2,1, 2,2,2,2);
		Util.gblAdd(p, legendPanel,  gbc, 1,0, 1,1, 1,1, 2,2,2,2);
		Util.gblAdd(p, controlPanel, gbc, 0,1, 2,1, 1,0, 2,2,2,2);
	}   

	private void createMenus()
	{
		MenuBar mbar = new MenuBar();

		mbar.add(Util.makeMenu("File", new Object[]
		                                          {
				"Print Graph",
				null,
				"Close"
		                                          },
		                                          this));                   
		mbar.add(Util.makeMenu("Tools", new Object[]
		                                           {
				"Set Interval Size",
				"Dump Raw Data",
				"Timeline"
		                                           },
		                                           this));
		Menu helpMenu = new Menu("Help");
		mbar.add(Util.makeMenu(helpMenu, new Object[]
		                                            {
				"Index",
				"About"
		                                            },
		                                            this)); 
		mbar.setHelpMenu(helpMenu);
		setMenuBar(mbar);                                                     
	}   


	private void PrintGraph()
	{
		PrinterJob pjob = PrinterJob.getPrinterJob();
		PageFormat format = new PageFormat();
		format = pjob.pageDialog(format);
		pjob.setPrintable(new PrintUtils(displayPanel), format);
		if (pjob.printDialog() == false) {
			// user cancelled.
			return;
		}
		try {
			pjob.print();
		} catch (PrinterException e) {
			System.err.println("Printer failure.");
		}
	}   

	void setChildDatas()
	{
		controlPanel.setGraphData(data);
		displayPanel.setGraphData(data);
		legendPanel.setGraphData(data);

		data.graphWindow  = this;
		data.controlPanel = controlPanel;
		data.displayPanel = displayPanel;
		data.legendPanel  = legendPanel;
	}   

	public void showDialog()
	{
		if (dialog == null) {
			intervalPanel = new IntervalChooserPanel();    	
			dialog = new RangeDialog(this, "Select Range", intervalPanel, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()) {

			final long intervalsize = intervalPanel.getIntervalSize();
			final int intervalStart = (int)intervalPanel.getStartInterval();
			final int intervalEnd = (int)intervalPanel.getEndInterval();
			processorList = dialog.getSelectedProcessors();

			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			final SwingWorker worker = new SwingWorker() {
				public Object doInBackground() {
					MainWindow.runObject[myRun].LoadGraphData(intervalsize, 
							intervalStart, intervalEnd,
							true, processorList);
					// got rid of the old optimization that new data 
					// is only created if the start and end points of 
					// the range is modified. This will, of course, 
					// have to be restored, but in a far more elegant 
					// way than currently allowed.
					data = new GraphData(intervalsize, 
							intervalStart, intervalEnd,
							processorList);
					return null;
				}
				public void done() {
					thisWindow.setChildDatas();
					/* also need to close and free legendPanel */
					if (legendPanel!=null) 
						legendPanel.closeAttributesWindow();

					controlPanel.setXMode(data.xmode);
					controlPanel.setYMode(data.ymode);

					legendPanel.UpdateLegend();
					displayPanel.setAllBounds();
					displayPanel.UpdateDisplay(); 

					thisWindow.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					thisWindow.setVisible(true);
				}
			};
			worker.execute();

		} else {
			return;
		}
	}



}
