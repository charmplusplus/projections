package projections.gui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import projections.analysis.NoiseMiner;
import projections.analysis.NoiseMiner.NoiseResultButton;


/**
 *  @class NoiseMinerWindow
 *  @author Isaac Dooley
 */

class NoiseMinerWindow extends ProjectionsWindow
implements ItemListener
{

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

	NoiseMiner			noiseMiner;

	public String buttonColumnTitle;
	public int numColumns;
	

	public NoiseMinerWindow(MainWindow parentWindow) {
		super(parentWindow);
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
			dialog = new RangeDialog(this, "Select Time Range & Processors", null, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()) {

			final OrderedIntList validPEs = dialog.getSelectedProcessors();
			final long startTime = dialog.getStartTime();
			final long endTime = dialog.getEndTime();
			
			final SwingWorker worker = new SwingWorker() {
				public Object doInBackground() {
					noiseMiner = new NoiseMiner(startTime, endTime, validPEs);
					noiseMiner.gatherData(thisWindow);
					mainText.setText(noiseMiner.getText());
					addResultsToTable(noiseMiner.getResultsTable());
					addDataToHistogram(noiseMiner.histogramToDisplay);
					return null;
				}
				
				public void done() {
//					System.out.println("displayDialog finished()");
				}
			};
			worker.execute();
		}
	}
	
	
	
	private void addDataToHistogram(long[] data) {
			
		/* Generate a nice looking histogram plot */

        DefaultTableXYDataset dataset = new DefaultTableXYDataset();
        XYSeries s = new XYSeries("All Event Types", true, false);
    
        for(int i=0;i<data.length;i++){
        	if(data[i] > 0){
        		s.add(i, data[i]);
        		System.out.println("data["+i+"]="+data[i]);
        	}
        }
        
        dataset.addSeries(s);

        NumberAxis domainAxis = new NumberAxis("Event Duration (not in Microseconds)");
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

//        LogAxis rangeAxis = new LogAxis("Number of Events");
//        rangeAxis.setLowerBound(1.0);
//        rangeAxis.setUpperBound(1000000);
        
        NumberAxis rangeAxis = new NumberAxis("Number of Events");

        StackedXYBarRenderer renderer = new StackedXYBarRenderer();

        renderer.setDrawBarOutline(true);
        
        XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);

        JFreeChart chart = new JFreeChart("Event Durations Histogram (Click & Drag to zoom)", plot);

		ChartPanel chartpanel = new ChartPanel(chart);

		chartJPanel.removeAll();
		chartJPanel.setLayout(new java.awt.BorderLayout());
		chartJPanel.add(chartpanel, BorderLayout.CENTER);
		chartJPanel.validate();
		chartJPanel.repaint();
		
	}

	private void addResultsToTable(Vector data){

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
