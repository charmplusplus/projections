package projections.gui;
import projections.gui.count.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import projections.gui.FormattedNumber;
import java.text.DecimalFormat;

/** Sindhura Bandhakavi
*   07-15-2002
*
* EPFrame displays the longest Entry Points in the current run
* allows sorting of data with respect to the columns (using TableSorter)
* allows saving of the data in a file  */

/* based on Josh's UserEventWindow class */

public class EntryPointWindow extends JFrame implements ActionListener
{

	// structure for storing each row
	private class RowData{
		Object[] value;		// actual value of the object
		Color color;		// color in which the object must be drawn
		
		public RowData(){
			value = new Object[numCols];
		}
	}

	private DefaultTableCellRenderer rightJustify_ =
    		new DefaultTableCellRenderer();

  	private class NameRenderer extends DefaultTableCellRenderer {
		public NameRenderer() {}
    		public Component getTableCellRendererComponent(
      		JTable table, Object value, boolean selected, boolean focused,
      		int row, int column)
      		{
        		setHorizontalAlignment(JLabel.CENTER);
        		setEnabled(table == null || table.isEnabled());
		        if(data!=null && data[row]!=null)
				setForeground(data[row].color);	
		        setBackground(BACKGROUND);
		        super.getTableCellRendererComponent(
		          table, value, selected, focused, row, column);
		        return this;
		 }
    	}

  	private class EPTable extends AbstractTableModel{
		public EPTable() {}
		public int getColumnCount() { return numCols; }
		public int getRowCount() {
			if(data != null)
				return data.length; 
			return 0;
		}

		 public String getColumnName(int columnIndex) {
      			switch (columnIndex) {
			      case 0: return "Processor Number";
			      case 1: return "EntryPoint Name";
			      case 2: return "Begin Time";
			      case 3: return "End Time";
			      case 4: return "Delta Time";
			      default: return "ERROR";
		        }
    		}	

		/* for the proper working of the sorter */		
		public Class getColumnClass(int columnIndex) {
      			if (columnIndex == 1) { return java.lang.String.class; }
			      else { return projections.gui.FormattedNumber.class; }
		}

		public Object getValueAt(int row, int col) {
				return data[row].value[col];
   		}
	}


	private void growTable() {
	
	    numRows *= 2;
	    RowData[] temp = new RowData[numRows];
	    for(int i=0; i< temp.length; i++)
		temp[i] = new RowData();

	    for(int i=0; i < data.length; i++)
	    {
	        for(int j=0; j<numCols; j++)
		{	
               		temp[i].value[j] = data[i].value[j];
               		temp[i].color = data[i].color;
		}
	    }
	    data = temp;

  	}

	/* save data in the chosen file when "save" button is pressed */
      	public void actionPerformed(ActionEvent ae) {
				int returnVal = fileChooser.showSaveDialog(this);
			   try{
				if(returnVal == JFileChooser.APPROVE_OPTION) {
                                        fileChooser.approveSelection();
					FileWriter outputFile = new FileWriter(fileChooser.getSelectedFile());
					// save data into the selected file
					for(int i=0; i<currRow; i++){
					// use sorter,instead of 'epTable' or 'data' so that the file is stored as sorted
					   	for(int j=0;j<numCols;j++)		
							outputFile.write(sorter.getValueAt(i,j).toString() + "\t");
					  	outputFile.write("\n");
					}
					outputFile.close();
				JOptionPane.showMessageDialog(this,"File Saved","Information",JOptionPane.INFORMATION_MESSAGE);
				}
			   }catch(Exception e){
				System.out.println("Exception: "+e);
				JOptionPane.showMessageDialog(this,"Error While Saving File" + e,"Error",JOptionPane.ERROR_MESSAGE);
			   }
	}

	/** Constructor */
	public EntryPointWindow(){
		super("Longest EntryPoints");
    	    	if (format == null) {
      			format = new DecimalFormat();
      			format.setGroupingUsed(true);
    		}
	
		JButton saveButton = new JButton("Save");		
		    saveButton.addActionListener(this); 

		for(int i=0; i<data.length; i++)
			data[i] = new RowData();		// explicitly call constructor to initialize value[] in RowData

		fileChooser =  new JFileChooser();

		// set up the table
		epTable = new EPTable();
		sorter = new TableSorter(epTable);
    		jTable = new JTable(sorter);
    		sorter.addMouseListenerToHeaderInTable(jTable);
    		jTable.setColumnSelectionAllowed(false);		

	        jTable.setBackground(BACKGROUND);
                jTable.setForeground(FOREGROUND);
		
		TableColumn column = null;
      		column = jTable.getColumnModel().getColumn(0);
      		column.setPreferredWidth(5);
      		column = jTable.getColumnModel().getColumn(1);
      		column.setPreferredWidth(200);
      		column.setCellRenderer(new NameRenderer());
      		for (int j=2; j<epTable.getColumnCount(); j++) {
        		column = jTable.getColumnModel().getColumn(j);
        		column.setPreferredWidth(75);
        		column.setCellRenderer(rightJustify_);
      		}

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(jTable), BorderLayout.CENTER);
		getContentPane().add(saveButton, BorderLayout.SOUTH);	
		
		addWindowListener(new WindowAdapter() {
      			public void windowClosing(WindowEvent e) { 
				setVisible(false);
			 }
    		});
	}

	public void clearTableData()
	{
		data = new RowData[numRows];
		for(int i=0; i<data.length; i++)
			data[i] = new RowData();
		currRow = 0;

                sorter.tableChanged(new TableModelEvent(
      epTable, 0, epTable.getRowCount()-1, TableModelEvent.ALL_COLUMNS));
	}

	/* Parent Window uses this function to enter data into the row */

	public void writeToTable(int pe, String epName, long startTime, long endTime, Color color)
	{
		// if table size is not enough, reallocate the table
		if(currRow == numRows)
                        growTable();

                data[currRow].value[0] = new Integer(pe);
                data[currRow].value[1] = epName;
                data[currRow].value[2] = new FormattedNumber(startTime,format);		// create a new object to be stored in the Object[][] data
                data[currRow].value[3] = new FormattedNumber(endTime,format);
                data[currRow].value[4] = new FormattedNumber((endTime - startTime),format);

		data[currRow].color = color;

		// currRow keeps track the row to be filled	
                currRow++;

                sorter.tableChanged(new TableModelEvent(
      epTable, 0, epTable.getRowCount()-1, TableModelEvent.ALL_COLUMNS));
	}


	// Member Variables


	private TableSorter        sorter      = null;
  	private JTable             jTable      = null;
	private EPTable		   epTable;
 	private DecimalFormat format = null;
	private final JFileChooser fileChooser;

	// variables for table

  	Color BACKGROUND = Color.black;
  	private Color FOREGROUND = Color.white;
	private int 		   currRow     = 0;
        private int 		   numRows     = 100;
        int 		   numCols     = 5;
	RowData[] 	   data	       = new RowData[numRows];	 

}
