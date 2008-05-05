package projections.gui;
import projections.analysis.*;
import projections.analysis.NoiseMiner.Histogram;
import projections.analysis.NoiseMiner.NoiseResultButton;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 *  @class NoiseMinerWindow
 *  @author Isaac Dooley
 */

public class NoiseMinerWindow extends ProjectionsWindow
implements ItemListener
{

	private static final long serialVersionUID = 1L;

	NoiseMinerWindow      thisWindow;    

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	private JPanel	         mainPanel;
	private JPanel           controlPanel;

	private DefaultTableModel tableModel;
	private JTable table;

	private JPanel chartJPanel;

	private final Vector columnNames;

	private JButton              setRanges;

	JTextArea   mainText;
	private JScrollPane	mainTextScroller;

	public OrderedIntList        validPEs;
	public long                  startTime;
	public long                  endTime;

	NoiseMiner			noiseMiner;

	public String buttonColumnTitle;
	public int numColumns;
	
	protected void windowInit() {
	}

	public NoiseMinerWindow(MainWindow parentWindow, Integer myWindowID) {
		super(parentWindow, myWindowID);
		thisWindow = this;

		setBackground(Color.lightGray);
		setTitle("Projections Computational Noise Miner :  " + MainWindow.runObject[myRun].getFilename() + ".sts");

		buttonColumnTitle = new String("Exemplar Timelines");

		columnNames = new Vector();
		columnNames.add(new String("Noise Duration"));
		columnNames.add(new String("Seen on Processors"));
		columnNames.add(new String("Occurrences"));
		columnNames.add(new String("Periodicity")); 
		columnNames.add(new String("Likely Source of Noise"));
		columnNames.add(new String(buttonColumnTitle)); // buttons go here
		numColumns = 6;

		mainText = new JTextArea("", 4, 30); // height, width
		mainTextScroller = new JScrollPane(mainText);
		mainTextScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		mainPanel = new JPanel();
		setLayout(mainPanel);
		CreateLayout();
		pack();
		showDialog();
		setSize(1000,500);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() instanceof JButton) {
			JButton b = (JButton)e.getSource();
			if(b == setRanges)
				showDialog();
		} 
	}   

	public void showDialog() {
		if (dialog == null) {
			dialog = new RangeDialog(this, "select Range");
		}
		else {
			setDialogData();
		}
		dialog.displayDialog();
		if (!dialog.isCancelled()) {
			getDialogData();
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					noiseMiner = new NoiseMiner(startTime, endTime, validPEs);
					noiseMiner.gatherData(thisWindow);
					mainText.setText(noiseMiner.getText());
					addResultsToTable(noiseMiner.getResultsTable());
					addDataToHistogram(noiseMiner.getNumBins(), noiseMiner.binWidth().ms(), noiseMiner.perPEHistograms);
					return null;
				}
				
				public void finished() {
//					System.out.println("displayDialog finished()");
				}
			};
			worker.start();
		}
	}
	
	
	
	private void addDataToHistogram(int nbins, double binwidth_ms, Hashtable perPEHistograms) {
		int numPEs = perPEHistograms.size();
		if(numPEs > 0){
		

			double[] series1 = new double[nbins+1];
			for(int i=0;i<nbins;i++)
				series1[i] = 0.0;
			
			/* Consolidate the data for the histogram plot */
			
			Iterator keyIter = perPEHistograms.keySet().iterator();
			while(keyIter.hasNext()){
				Integer pe = (Integer) keyIter.next();
				NoiseMiner.Histogram[] hists = (Histogram[]) perPEHistograms.get(pe);
				for(int event=0;event<hists.length;event++){
					NoiseMiner.Histogram h = hists[event];
					for(int bin=0;bin<nbins;bin++){
						series1[bin] += h.getBin_count(bin);
					}	
				}
			}
			
			
			/* Generate a nice looking histogram plot */
			
			HistogramDataset dataset = new HistogramDataset();

			dataset.addSeries("All Events", series1, nbins+1, 0.0, binwidth_ms*nbins);
			JFreeChart chart = ChartFactory.createHistogram(
					"Histogram of Event Durations", 
					null, 
					null, 
					dataset, 
					PlotOrientation.VERTICAL, 
					true, 
					true, 
					false
			);
			XYPlot plot = (XYPlot) chart.getPlot();
			plot.setForegroundAlpha(0.8f);
			XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
			renderer.setDrawBarOutline(false);

			ChartPanel chartpanel = new ChartPanel(chart);

			chartJPanel.removeAll();
			chartJPanel.setLayout(new java.awt.BorderLayout());
			chartJPanel.add(chartpanel, BorderLayout.CENTER);
			chartJPanel.validate();
			chartJPanel.repaint();
		}
		
	}

	void addResultsToTable(Vector data){

		tableModel = new DefaultTableModel(data, columnNames);
		table.setModel(tableModel);
		
		ButtonColumn bc = new ButtonColumn();
		table.getColumn(buttonColumnTitle).setCellRenderer(bc);
		table.getColumn(buttonColumnTitle).setCellEditor(bc);


		table.revalidate();
		table.repaint();
	}


	private void CreateLayout()
	{  
		JPanel noiseMinerResultPanel = new JPanel();

		DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);

		table = new JTable(tableModel);

		JScrollPane resultTable = new JScrollPane(table);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Results", resultTable);
//		tabbedPane.addTab("Text Summary", mainTextScroller);
		
	
		chartJPanel = new JPanel();
		tabbedPane.addTab("Histogram Plot", chartJPanel);

		mainPanel.setLayout(new java.awt.BorderLayout());

		// control panel items
		setRanges = new JButton("Select New Range");
		setRanges.addActionListener(this);
		controlPanel = new JPanel();

		controlPanel.add(setRanges);

		// Add the result gui
		mainPanel.add(tabbedPane, BorderLayout.CENTER);
		mainPanel.add(controlPanel, BorderLayout.SOUTH);
	}

	public void itemStateChanged(ItemEvent ae){
	}


	public void getDialogData() {
		validPEs = dialog.getValidProcessors();
		startTime = dialog.getStartTime();
		endTime = dialog.getEndTime();
	}

	public void setDialogData() {
		dialog.setValidProcessors(validPEs);
		dialog.setStartTime(startTime);
		dialog.setEndTime(endTime);
		super.setDialogData();	
	}

	public void showWindow() {
	}

	

	/** A class that renders and handles events for the JButtons in our table */
	class ButtonColumn extends AbstractCellEditor
	implements TableCellRenderer, TableCellEditor, ActionListener
	{

		/** A button which is shown when the clicking occurs on the object. The table thinks I'm editing that cell, but really I'm just displaying a similar JButton */
		JButton editButton;
		/** A reference to the original object which is stored while editing(displaying editButton) */
		Object editObject=null;

		public ButtonColumn()
		{
			super();
			editButton = new JButton();
			editButton.setFocusPainted( false );
			editButton.addActionListener( this );
		}

		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if(value instanceof JButton){
				JButton button = (JButton)value;
				if (hasFocus)
				{
					button.setForeground(table.getForeground());
					button.setBackground(UIManager.getColor("Button.background"));
				}
				else if (isSelected)
				{
					button.setForeground(table.getSelectionForeground());
					button.setBackground(table.getSelectionBackground());
				}
				else
				{
					button.setForeground(table.getForeground());
					button.setBackground(UIManager.getColor("Button.background"));
				}
				return button;
			} else {
				System.out.println("getTableCellRendererComponent() on a non-JButton");
				return null;
			}

		}

		public Component getTableCellEditorComponent(
				JTable table, Object value, boolean isSelected, int row, int column)
		{
			editObject = value;

			if(value instanceof JButton){
				JButton button = (JButton)value;

				String text = button.getText();
				editButton.setText( text );
				return editButton;
			} else {
				System.out.println("getTableCellEditorComponent() on a non-JButton");
				return null;
			}

		}

		public Object getCellEditorValue()
		{
			return editObject;
		}

		public void actionPerformed(ActionEvent e)
		{
			fireEditingStopped();
			
			// Find the column that holds the button
			int whichColumnHasButtons=-1;
			for(int i=0;i<numColumns;i++){
				if(table.getColumnName(i).compareTo(buttonColumnTitle)==0)
					whichColumnHasButtons = i;
			}

			// Get the edited cell's data 
			Object o = table.getValueAt(table.getSelectedRow(),whichColumnHasButtons);

			// Do the action associated with the button click
			if(o instanceof NoiseResultButton){
				// Display a window containing nice mini-timelines
				((NoiseResultButton)o).display();
			}
		}
	}


}
