package projections.Tools.UserStatsOverTime;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Paint;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Iterator;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.*;

import projections.gui.ProjectionsWindow;
import projections.gui.GenericGraphColorer;
import projections.gui.MainWindow;
import projections.gui.StatDialog;
import projections.gui.ColorManager;
import projections.gui.JPanelToImage;
import projections.misc.LogEntryData;
import projections.analysis.ProjDefs;
import projections.analysis.GenericLogReader;
import projections.analysis.EndOfLogSuccess;
import projections.analysis.TimedProgressThreadExecutor;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;

/* UserStatsOverTime Tool by Joshua Lew 7/6/16. This tool reads in the userstats and plots
Them over time. The tool is highly customizable. The User can customize every line plot by stat, PEs,
aggregating method, X value type, Y Axis, Color, line/points, etc.  */

public class UserStatsTimeWindow extends ProjectionsWindow
implements ItemListener, ActionListener
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	// Sent External code commented out and may be implemented later

	private UserStatsTimeWindow      thisWindow;

	//    private EntrySelectionDialog entryDialog;

	private JPanel	   mainPanel;
	private JTable 		table;
	private StatTableModel tableModel;

	private JPanel	   graphPanel;
	private JPanel         controlPanel;

	private JButton	   setRanges;
	private JButton	   saveImage;
	//    private JButton	   epSelection;

	private StatDialog 	statDialog;
	private NumberAxis 	domainAxis, range1Axis, range2Axis;
	private XYPlot		plot;

	private SortedSet<Integer> processorList;

	private Vector<XYLineAndShapeRenderer> renderers;

	/*This Vector holds all our desired data. Each element of the Vector is a different
	plot. XYSeriesCollection is necessary because we can change Y Axis on the XYSeriesCollection level, but
	not on the XYSeries level  */
	private Vector<XYSeriesCollection> graphedData;
	private Vector<Double> 	globalSum;
	private Vector<Double>	maxValue;
	private Vector<Double>	minValue;
	private Vector<Integer>	numCalls;
	private Vector<Vector<Object>> tableData;

	private final Vector<String> columnNames;

	private String[]	statNames;
	private int 		numStats;
	private int 		curRow;				//current row in table. starts at 0 and increases after every new line plotted.
	private int 		curStat;			//Stat about to be graphed. Only this index stat will be stored in data 
	private int 		numColumns;

	private long		startTime;
	private long		endTime;

	private boolean 	isValid;
// format for output
	private DecimalFormat  _format;


	public UserStatsTimeWindow(MainWindow mainWindow) {
		super("User Stats over Time Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
		// the following data are statically known and can be initialized
		// here
		numStats = MainWindow.runObject[myRun].getNumUserDefinedStats();
		statNames = MainWindow.runObject[myRun].getUserStatNames();
		thisWindow = this;
		_format = new DecimalFormat("###,###.###");

		//Create Column Names
		columnNames = new Vector<String>();
		columnNames.add(new String("Stat Name"));
		columnNames.add(new String("PEs"));
		columnNames.add(new String("Average Value"));
		columnNames.add(new String("Max Value"));
		columnNames.add(new String("Min Value"));
		columnNames.add(new String("Visible?"));
		numColumns = 6;

		//Initialize global vectors to hold overall data
		globalSum = new Vector<Double>();
		maxValue = new Vector<Double>();
		minValue = new Vector<Double>();
		numCalls = new Vector<Integer>();
		tableData = new Vector<Vector<Object>>();
		graphedData = new Vector<XYSeriesCollection>();
		renderers = new Vector<XYLineAndShapeRenderer>();

		//initialize curRow, which indicates which row the next new Stat plot would occupy
		curRow = 0;

		//Initialize plot and Axis
		domainAxis = new NumberAxis("Time (microseconds)");
      		domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		domainAxis.setAutoRangeIncludesZero(false);
       		range1Axis = new NumberAxis("Value");
       		range2Axis = new NumberAxis("Value");
		plot = new XYPlot();
		plot.setDomainAxis(domainAxis);
		plot.setRangeAxis(0,range1Axis);
		plot.setRangeAxis(1,range2Axis);
		createLayout();
		pack();
		showDialog();
	}

	//Create initial layout of window

	private void createLayout() {

		mainPanel = new JPanel();
		setLayout(mainPanel);
		mainPanel.setLayout(new java.awt.BorderLayout());
		// control panel items
		setRanges = new JButton("Add New Stat");
		setRanges.addActionListener(this);

		saveImage = new JButton("Save to Image");
		saveImage.addActionListener(this);


		graphPanel = new JPanel();
		tableModel = new StatTableModel(columnNames, 0);

		table = new JTable(tableModel);

		//Create the 2 tabs
		JTabbedPane tabbedPane = new JTabbedPane();
		JScrollPane resultTable = new JScrollPane(table);
		tabbedPane.addTab("Line Graph", graphPanel);
		tabbedPane.addTab("Stat Info", resultTable);


		controlPanel = new JPanel();
		controlPanel.add(setRanges);

		controlPanel.add(saveImage);
		mainPanel.add(tabbedPane, BorderLayout.CENTER);
		mainPanel.add(controlPanel,BorderLayout.SOUTH);
		table.revalidate();
		table.repaint();

	}

	//Show dialog box and read in appropriate Data
	public void showDialog() {
		if (statDialog == null) {
			statDialog = new StatDialog(this, "Add Stat", statNames,false);
		}
		statDialog.displayDialog();
		if (!statDialog.isCancelled()) {
			//Read in values from dialog box
			processorList = new TreeSet<Integer>(statDialog.getSelectedProcessors());
			startTime = statDialog.getStartTime();
			endTime = statDialog.getEndTime();

			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					//Get the data and check for success
					isValid = getData();

					return null;
				}
				public void done() {

					//if data read correctly, add results to table
					if(isValid) {
						setNewGraphData();
						addResultsToTable(getResultsTable());
						curRow=curRow+1;
					}

					table.getModel().addTableModelListener(new TableModelListener() {

						public void tableChanged(TableModelEvent e){
							TableModel model = (TableModel)e.getSource();
							if(model.getRowCount()==0) return;
							for(int row = e.getFirstRow(); row<=e.getLastRow();row++){
								if(row<0) continue;
								if(model.getValueAt(row,numColumns -1).equals(new Boolean(true))) {
									/*If dataset at row is marked Visible, Set the dataset
								   	For that row to be the XYSeriesCollection meant for that row */
									plot.setDataset(row,graphedData.get(row));
									updateData(row);
								}
								else {
									/*If dataset at that row is marked not Visible, set the dataset
									fot that row to be null. Note that this doesn't affect the 
									data at all, just which data is plotted*/
									plot.setDataset(row,null);
									plotData();
								}
							}
						}
					});
					thisWindow.setVisible(true);
				}
			};
			worker.execute();
		}
	}

	//Add Row and refresh tabel
	private void addResultsToTable(Vector data){

		tableModel.addRow(data);
		table = new JTable(tableModel);
		table.revalidate();
		table.repaint();

	}

	//Get new row for table
	public Vector<Object> getResultsTable(){

		String pes = "";
		for(Integer pe : processorList) {
			if( pe.equals(processorList.last()))
				pes+=pe.toString();
			else pes+=pe.toString() + ", ";

		}
		Vector<Object> row = new Vector<Object>();

		row.add(new String("" +statNames[curStat]  ));
		row.add(pes);
		row.add(new String("" + globalSum.get(curRow)/ numCalls.get(curRow) ));
		row.add(new String("" + maxValue.get(curRow) ));
		row.add(new String("" + minValue.get(curRow)));
		row.add(Boolean.TRUE);


		return row;
	}

	//Set new Renderer for new line plot
	protected void setNewGraphData(){
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(statDialog.getLineSet(),statDialog.getPointSet());
		String renderColor = statDialog.getColor();

		if(renderColor.equals("Red"))
			renderer.setSeriesPaint(0,Color.RED);
		else if(renderColor.equals("Blue"))
			renderer.setSeriesPaint(0,Color.BLUE);
		else if(renderColor.equals("Green"))
			renderer.setSeriesPaint(0,Color.GREEN);
		else if(renderColor.equals("Black"))
			renderer.setSeriesPaint(0,Color.BLACK);
		else if(renderColor.equals("Cyan"))
			renderer.setSeriesPaint(0,Color.CYAN);
		else if(renderColor.equals("Yellow"))
			renderer.setSeriesPaint(0,Color.YELLOW);
		else if(renderColor.equals("Pink"))
			renderer.setSeriesPaint(0,Color.PINK);
		else if(renderColor.equals("Magenta"))
			renderer.setSeriesPaint(0,Color.MAGENTA);
		else if(renderColor.equals("Gray"))
			renderer.setSeriesPaint(0,Color.GRAY);
		renderers.add(renderer);

		updateData(curRow);
	}

	//Add new row of data into dataSet
	public void updateData(int plotRow) {
		plot.setDataset(plotRow,graphedData.get(plotRow));
		plot.setRenderer(plotRow,renderers.get(plotRow));
		if(statDialog.getYValue().equals("Left"))
			plot.mapDatasetToRangeAxis(plotRow,0);
		else plot.mapDatasetToRangeAxis(plotRow,1);

		plotData();

	}

	//refreash plot
	public void plotData() {

		JFreeChart chart = new JFreeChart("Stats (Click and drag to zoom in) ",plot);
		ChartPanel chartpanel = new ChartPanel(chart);
		graphPanel.removeAll();
		graphPanel.setLayout(new java.awt.BorderLayout());
		graphPanel.add(chartpanel,BorderLayout.CENTER);
		graphPanel.validate();
		graphPanel.repaint();
		pack();
	}

	//gather appropriate data based off user input in Stat Dialog
	private boolean getData() {
		double sum= 0;
		double  max= 0;
		double  min=0;
		int nCalls=0;
		//Get User choices
		String xType = statDialog.getXValue();
		String xAgg = statDialog.getAggregate();
		curStat = statDialog.getStatIndex();
		//Get name of series for Legend based off stat name and PE's used
		String seriesName = statNames[curStat] + ": ";
		for(Integer pe : processorList) {
			if( pe.equals(processorList.last()))
				seriesName+=pe.toString();
			else seriesName+=pe.toString() + ", ";
		}
		XYSeries data = new XYSeries(seriesName,true);
		int numPes = 0;
		//Loop through every PE
		for(Integer pe : processorList) {
			numPes+=1;
			int pointIndex = 0;
			GenericLogReader reader = new GenericLogReader( pe,
					MainWindow.runObject[myRun].getVersion());
			LogEntryData logData;

			// Skip to the first begin.
			try {
				logData = reader.nextEventOfType(ProjDefs.USER_STAT);

				//Loop until first entry in desired time fram
				while (logData.time < startTime) {
					logData = reader.nextEventOfType(ProjDefs.USER_STAT);
				}
				int statIndex = 0;
				double time = 0;
				while (true) {

					//find the statIndex for the current LogEntry
					statIndex = MainWindow.runObject[myRun].getUserDefinedStatIndex(logData.userEventID);
					//If this is the stat we are looking for, store the data.
					if(curStat==statIndex){
						//Determine what time to store based off user choices
						if(xType.equals("User Specified"))
							time = logData.userTime;
						else if (xType.equals("Ordered"))
							time = pointIndex;
						else time = logData.time;

						//If this is the first PE, just store the point
						if(numPes==1) {
							data.add(time,logData.stat);
						}

						//Otherwise, we have to store according to our aggregating rules
						else {
							//Make sure this PE doesn't have more points than the 1st PE did.
							if(pointIndex>=data.getItemCount()) {
								System.out.println("Error: Uneven number of points per PE. Can not aggregate across PEs. Load 1 PE per plot");
								return false;
							}
							//Aggregate data appropriately
							if(xAgg.equals("Min")) {
								if(logData.stat < data.getY(pointIndex).doubleValue())
									data.updateByIndex(pointIndex,logData.stat);
							}
							else if (xAgg.equals("Max")) {
								if(logData.stat > data.getY(pointIndex).doubleValue())
									data.updateByIndex(pointIndex,logData.stat);
							}

							else data.updateByIndex(pointIndex, data.getY(pointIndex).doubleValue() + logData.stat);
						}
						//increase global stats, such as total calls, total sum, avg, max, min
						nCalls=nCalls+1;
						sum = sum + logData.stat;
						if(nCalls==1){
							min = logData.stat;
							max = logData.stat;
						}
						else if (logData.stat<min)
							min = logData.stat;
						else if (logData.stat>max)
							max =logData.stat;
						/*Increment pointIndex, which indicates which numbered point we are on.
						Useful for determining item count and for storing data as Ordered. */
						pointIndex+=1;
					}

					logData = reader.nextEventOfType(ProjDefs.USER_STAT);
					//Stop once we are over end time frame
					if (logData.time > endTime) {
						break;
					}
				}

			}catch (EndOfLogSuccess e) {
			// do nothing
			} catch (IOException e) {
				System.out.println("Exception while reading log file " + pe);
			}


			try {
				reader.close();
			} catch (IOException e1) {
				System.err.println("Error: could not close log file reader for processor " + pe );
			}

			//Make sure that this PE doesn't have LESS points than the 1st PE did.
			if(pointIndex< data.getItemCount()) {
				System.out.println("Error: Uneven number of points per PE. Can not aggregate across PEs. Load 1 PE per plot");
				return false;
			}
		}
		//Add all our values to the overall Vectors.
		globalSum.add(sum);
		maxValue.add(max);
		minValue.add(min);
		numCalls.add(nCalls);
		//If Aggregating average, divide every Point by numPES to get the average
		if(xAgg.equals("Average")) {
			int numItems = data.getItemCount();
			for(int i = 0; i<numItems; i++) {
				data.updateByIndex(i, data.getY(i).doubleValue()  / numPes);
			}
		}

		//Put our data into a XySeriesCollection and add it to the graphedData Vector
		graphedData.add(new XYSeriesCollection(data));
		return true;

   	 }
	//Handle the button actions appropriately
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton b = (JButton)e.getSource();
			if (b == setRanges) {
				showDialog();
			}
			else if (b == saveImage) {
				JPanelToImage.saveToFileChooserSelection(graphPanel, "Save Plot To File", "./ProjectionsPlot.png");
			}

		} else if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem)e.getSource()).getText();
			if (arg.equals("Close")) {
				close();
			} else if(arg.equals("Set Range")) {
				showDialog();
			}
		}
	}

	//Change cursor depending on situation
	public void itemStateChanged(ItemEvent ae){
		if(ae.getSource() instanceof Checkbox){
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			Checkbox cb = (Checkbox)ae.getSource();
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	/*Class that defines our table. Ensures only the proper columns are ediable
	and allows us to read in the last column as a boolean*/
	public class StatTableModel extends DefaultTableModel {

		public StatTableModel(Vector columnNames, int rowCount){
			super(columnNames,rowCount);
		}

		public StatTableModel(Vector data, Vector columnNames){
			super(data,columnNames);
		}
		//Only the last column (Visible?)  should be editable
		public boolean isCellEditable(int row, int column){

			if(column == (numColumns -1) )
				return true;
			else return false;
		}
		//Every class is a string excpet for the last column.
		public Class<?> getColumnClass(int columnIndex) {

			if(columnIndex == (numColumns -1))
				return Boolean.class;
			else return String.class;
		}

	}

}
