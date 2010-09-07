package projections.gui;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JOptionPane;

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
 *  This class should be inherited by all projections tools that present
 *  some kind of main window and show a dialog box requiring the user to
 *  input a range of processors and a time interval.
 *
 */

public abstract class GenericGraphWindow 
extends ProjectionsWindow 
implements PopUpAble, ColorUpdateNotifier
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	// inheritable GUI objects
	private GraphPanel graphPanel;
	protected Graph graphCanvas;


	// all child classes should implement this function and 
	// call it to initialize the graph data
	protected abstract void setGraphSpecificData();

	// Graph specific data, so that implementation can be changed if required
	private DataSource dataSource;
	protected XAxis xAxis;
	private YAxis yAxis;

	// assuming all projections graph windows need a menu bar and a file menu
	protected JMenuBar menuBar  = new JMenuBar();
	private JMenu    fileMenu = new JMenu("File");


	private JMenuItem mSaveScreenshot;
	private JMenuItem mWhiteBG;
	private JMenuItem mBlackBG;
	protected JMenuItem mChooseColors;
	private JMenuItem mSaveColors;
	private JMenuItem mLoadColors;


	/** Provides the color mapping for the graph */
	private GenericGraphColorer colorer;

	GenericGraphWindow gw;
	
	// constructor 
	public GenericGraphWindow(String title, 
			MainWindow mainWindow) {
		super(title, mainWindow);
		menuBar.add(fileMenu);
		gw = this;
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
				           this);
		
		
		
		// Color Scheme Menu
		JMenu mColors = new JMenu("Color Scheme");
		mWhiteBG = new JMenuItem("White background");
		mBlackBG = new JMenuItem("Black background");
		mChooseColors = new JMenuItem("Choose Entry Colors");
		mSaveColors = new JMenuItem("Save Colors To File");
		mLoadColors = new JMenuItem("Load Colors From File");	
		
		mWhiteBG.addActionListener(new MenuHandler());
		mBlackBG.addActionListener(new MenuHandler());
		mChooseColors.addActionListener(new MenuHandler());
		mSaveColors.addActionListener(new MenuHandler());
		mLoadColors.addActionListener(new MenuHandler());

		mColors.add(mWhiteBG);
		mColors.add(mBlackBG);
		mColors.addSeparator();
		mColors.add(mChooseColors);
		mColors.addSeparator();
		mColors.add(mSaveColors);
		mColors.add(mLoadColors);
		menuBar.add(mColors);
		
		
		// Screenshot Menu
		JMenu saveMenu = new JMenu("Save To Image");
		mSaveScreenshot = new JMenuItem("Save Plot as JPG or PNG");
		mSaveScreenshot.addActionListener(new MenuHandler());
		saveMenu.add(mSaveScreenshot);
		menuBar.add(saveMenu);
		
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
	protected void setDataSource(String title, double data[][], GenericGraphColorer colorer, GenericGraphWindow parent) {
		dataSource = new DataSource2D(title, data, parent);
		this.colorer = colorer;
		dataSource.setColors(colorer.getColorMap());
		if (yAxis != null) {
			yAxis = new YAxisAuto(yAxis.getTitle(),yAxis.getUnits(),dataSource);
		}
	}
	
	protected void setDataSource(String title, double data[][], GenericGraphWindow parent) {
		setDataSource( title,  data, new GenericGraphDefaultColors(),  parent);
	}
	
	// refresh graph
	protected void refreshGraph(){    
		if(graphCanvas!=null){
			// Colors can be changed, so we must update them
			dataSource.setColors(colorer.getColorMap());
			graphCanvas.setData(dataSource,xAxis,yAxis);
			graphCanvas.repaint();
		}
	}
	
	
	/** Recieve notification that colors have been changed */
	public void colorsHaveChanged(){
		refreshGraph();
	}
	
	
	public class MenuHandler implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			
			if (e.getSource() == mWhiteBG) {
				MainWindow.runObject[myRun].background = Color.white;
				MainWindow.runObject[myRun].foreground = Color.black;
				graphCanvas.repaint();
			} else if (e.getSource() == mBlackBG){
				MainWindow.runObject[myRun].background = Color.black;
				MainWindow.runObject[myRun].foreground = Color.white;
				graphCanvas.repaint();
			} else if(e.getSource() == mSaveScreenshot){
				JPanelToImage.saveToFileChooserSelection(graphCanvas, "Save Plot To File", "./ProjectionsPlot.png");
			} else if (e.getSource() == mChooseColors){
				new ChooseEntriesWindow(gw);
			} else if (e.getSource() == mLoadColors){
				try {
					MainWindow.runObject[myRun].loadColors();
					gw.colorsHaveChanged();
					JOptionPane.showMessageDialog(null, "The colors have successfully been loaded.", "Colors Loaded", JOptionPane.INFORMATION_MESSAGE);
				}
				catch (Exception error)
					{JOptionPane.showMessageDialog(null, error.getMessage() + "\nPlease set your colors and save them.", "Error", JOptionPane.ERROR_MESSAGE);
					refreshGraph();
					}
			} else if (e.getSource() == mSaveColors){
				MainWindow.runObject[myRun].saveColors();
			}
		}

	}
	
	
	
	
	
}

