package projections.gui;
import java.awt.Paint;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import projections.gui.graph.DataSource;
import projections.gui.graph.DataSource2D;
import projections.gui.graph.Graph;
import projections.gui.graph.GraphPanel;
import projections.gui.graph.XAxis;
import projections.gui.graph.XAxisDiscrete;
import projections.gui.graph.XAxisDiscreteOrdered;
import projections.gui.graph.XAxisFixed;
import projections.gui.graph.YAxis;
import projections.gui.graph.YAxisAuto;
import projections.gui.graph.YAxisFixed;

/**
 *  GenericGraphWindow
 *  written by Sindhura Bandhakavi
 *  8/2/2002
 *  modified by Chee Wai Lee
 *  12/15/2003 - to use the (hopefully) cleaner windows framework.
 *
 *  This class should be inherited by all projections tools that present
 *  some kind of main window and show a dialog box requiring the user to
 *  input a range of processors and a time interval.
 *
 *  NOTE that it is *still* an abstract class despite implementing a whole
 *  lot more functionality.
 *
 */

public abstract class GenericGraphWindow 
extends ProjectionsWindow 
implements PopUpAble
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	static int myRun = 0;

	// inheritable GUI objects
	protected GraphPanel graphPanel;
	protected Graph graphCanvas;


	// all child classes should implement this function and 
	// call it to initialize the graph data
	protected abstract void setGraphSpecificData();

	// Graph specific data, so that implementation can be changed if required
	protected DataSource dataSource;
	protected XAxis xAxis;
	protected YAxis yAxis;

	// assuming all projections graph windows need a menu bar and a file menu
	protected JMenuBar menuBar  = new JMenuBar();
	protected JMenu    fileMenu = new JMenu("File");


	// constructor 
	public GenericGraphWindow(String title, 
			MainWindow mainWindow) {
		super(title, mainWindow);
		menuBar.add(fileMenu);
	}

	// create a standard fileMenu to be inherited by subclasses
	// make it return menuBar instead of being void
	protected void createMenus(){
		fileMenu = Util.makeJMenu(fileMenu, 
				new Object[]
				           {
				"Set Range",
				"Close"
				           },
				           null,
				           this);
		setJMenuBar(menuBar);
	}

	public abstract String[] getPopup(int xVal, int yVal);

	// create a standard layout which can be called from child class or 
	// overridden by it
	// returns a Main Panel with vertical box layout and graphPanel attached
	protected JPanel getMainPanel(){
		JPanel mainPanel = new JPanel();
		graphCanvas = new Graph();
		graphPanel = new GraphPanel(graphCanvas);
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
		mainPanel.add(graphPanel);
		return mainPanel;
	}


	protected void setXAxis(String title,String units){
		xAxis = new XAxisFixed(title,units);
	}

	//  This is used for an X Axis that has discrete, non-contigious values.
	//  Typically used for getting a subset of the processor list that is
	//  sorted in increasing processor number.
	//
	protected void setXAxis(String title,OrderedIntList discreteList){
		xAxis = new XAxisDiscreteOrdered(title,discreteList);	
	}

	// This is a more general discrete XAxis than the above. 
	//
	// It is typically used for displaying a list of processors
	// pre-sorted in some arbitrary significance order. **NOTE**
	// Requires that discreteList be a LinkedList of Integers.
	//
	// It can also be used in cases (currently unexploited) where groups
	// of processors pre-sorted in some significance order are supplied.
	// **NOTE** Requires that discreteList be a LinkedList of Strings.
	//
	// XAxisDiscrete will take responsibility for determining the
	// appropriate behavior given the arbitrary LinkedList.
	//
	protected void setXAxis(String title, LinkedList discreteList) {
		xAxis = new XAxisDiscrete(title, discreteList);
	}

	protected void setXAxis(String title, String units, double startValue, double multiplier) {
		xAxis = new XAxisFixed(title,units);	
		((XAxisFixed)xAxis).setLimits(startValue,multiplier);
	}

	protected void setYAxis(String title, String units){
		if(dataSource != null)
			yAxis = new YAxisAuto(title,units,dataSource);
		else
			// create a dummy YAxis storing the title and units
			yAxis = new YAxisFixed(title,units,0);	
	}

	// This should be the correct way of setting a data source with a partial
	// set of colors
	protected void setDataSource(String title, double data[][], 
			Paint colorMap[],
			GenericGraphWindow parent) {
		dataSource = new DataSource2D(title, data, parent);
		dataSource.setColors(colorMap);
		if (yAxis != null) {
			yAxis = 
				new YAxisAuto(yAxis.getTitle(),yAxis.getUnits(),dataSource);
		}
	}

	protected void setDataSource(String title, double [][] data, GenericGraphWindow parent){
		dataSource = new DataSource2D(title,data, parent);
		dataSource.setColors(MainWindow.runObject[myRun].getColorMap());
		if(yAxis != null)
			yAxis = new YAxisAuto(yAxis.getTitle(),yAxis.getUnits(),dataSource);
	}
	// refresh graph
	protected void refreshGraph(){    
		if(graphCanvas!=null){
			graphCanvas.setData(dataSource,xAxis,yAxis);
			graphCanvas.repaint();
		}
	}
}

