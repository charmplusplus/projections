package projections.gui;
import projections.gui.graph.*;
import java.awt.Color;
import javax.swing.*;

/**
 *  GenericGraphWindow
 *  written by Sindhura Bandhakavi
 *  8/2/2002
 *
 *  This class should be inherited by all projections tools that present
 *  some kind of main window and show a dialog box requiring the user to
 *  input a range of processors and a time interval.
 *
 */

public abstract class GenericGraphWindow 
    extends ProjectionsWindow 
{
    static final Color BACKGROUND = Color.white;

// inheritable GUI objects
   protected GraphPanel graphPanel;
   protected Graph graphCanvas;

// all child classes should implement this function and call it to initialize the graph data
   protected abstract void setGraphSpecificData();

// Graph specific data, so that implementation can be changed if required
   protected DataSource dataSource;
   protected XAxisFixed xAxis;
   protected YAxis yAxis;

// assuming all projections graph windows need a menu bar and a file menu
    protected JMenuBar menuBar  = new JMenuBar();
    protected JMenu    fileMenu = new JMenu("File");

// constructor 
    public GenericGraphWindow(String title){
	  super();
	  setTitle(title);
	  setBackground(BACKGROUND);
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
	
	public String[] getPopup(int xVal, int yVal){
		//System.out.println("GenericGraphWindow.getPopup()");
		return null;
	};

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

    // set Graph Specific data
    protected void setBarGraphType(int type) {
	if (graphCanvas != null) {
	    graphCanvas.setBarGraphType(type);
	} else {
	    // issue warning.
	    System.err.println("Warning: The graph canvas has not yet been " +
			       "initialized! Ignoring request.");
	}
    }

    protected void setXAxis(String title,String units){
	xAxis = new XAxisFixed(title,units);	
    }
    
    protected void setXAxis(String title, String units, double startValue, 
			    double multiplier) {
	xAxis = new XAxisFixed(title,units);	
	xAxis.setLimits(startValue,multiplier);
    }

    protected void setYAxis(String title, String units){
	if(dataSource != null)
	    yAxis = new YAxisAuto(title,units,dataSource);
	else
	    // create a dummy YAxis storing the title and units
	    yAxis = new YAxisFixed(title,units,0);	
    }

    // whenever datasource changes, yaxis needs to be changed too
    protected void setDataSource(String title, int [] data){
	dataSource = new DataSource1D(title,data);
	if(yAxis != null)
	    yAxis = 
		new YAxisAuto(yAxis.getTitle(),yAxis.getUnits(),dataSource);
    }

    protected void setDataSource(String title, double [][] data){
	dataSource = new DataSource2D(title,data);
	if(yAxis != null)
	    yAxis = 
		new YAxisAuto(yAxis.getTitle(),yAxis.getUnits(),dataSource);
    }

    protected void setDataSource(String title, int [] data, GenericGraphWindow parent){
	dataSource = new DataSource1D(title,data,parent);
	if(yAxis != null)
	    yAxis = 
		new YAxisAuto(yAxis.getTitle(),yAxis.getUnits(),dataSource);
    }

    protected void setDataSource(String title, double [][] data, GenericGraphWindow parent){
	dataSource = new DataSource2D(title,data, parent);
	if(yAxis != null)
	    yAxis = 
		new YAxisAuto(yAxis.getTitle(),yAxis.getUnits(),dataSource);
    }
    // refresh graph
    protected void refreshGraph(){    
	graphCanvas.setData(dataSource,xAxis,yAxis);
	graphCanvas.repaint();
    }
}

