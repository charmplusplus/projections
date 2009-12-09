package projections.misc;

import javax.swing.table.AbstractTableModel;

import projections.analysis.MultiRunDataAnalyzer;

/**
 *  This class serves as a table model manipulator. It acquires its data
 *  from the analysis module for MultiRuns.
 */
public class MultiRunTableModel
    extends AbstractTableModel
{

	private static final long serialVersionUID = 1L;

//	protected String tableName;
    private MultiRunDataAnalyzer analysisModule;
    // This is the category type the associated JTable is supposed to
    // visualize.
    private int category;
    private int dataType;

    public MultiRunTableModel( MultiRunDataAnalyzer analysisModule,
			      int dataType, int category) {
//	this.tableName = tableName;
	this.analysisModule = analysisModule;
	this.dataType = dataType;
	this.category = category;
    }

    /**
     *  This method serves as an interface to MultiRunTables, giving it
     *  the ability to change the type of the information.
     */
    public void setType(int dataType) {
	this.dataType = dataType;
    }

    // **** required interface implementations for AbstractTableModel ****

    /**
     *  This method is closely tied to the display table. The analysisModule
     *  is expected to ask the display table for the currently displayed
     *  table (hence in the MultiRun case, the category) and to supply data 
     *  accordingly.
     */
    public int getRowCount() {
	return analysisModule.getNumRows(dataType, category);
    }

    /**
     *  This method is closely tied to the display table. The analysisModule
     *  is expected to ask the display table for the currently displayed
     *  table (hence in the MultiRun case, the category) and to supply data 
     *  accordingly.
     */
    public int getColumnCount() {
	return analysisModule.getNumColumns();
    }

    /**
     *  For populating the header row of the table.
     */
    public String getColumnName(int columnIndex) {
	return analysisModule.getColumnName( columnIndex);
    }

    /**
     *  This method is closely tied to the display table. The analysisModule
     *  is expected to ask the display table for the currently displayed
     *  table (hence in the MultiRun case, the category) and to supply data 
     *  accordingly.
     */
    public Object getValueAt(int row, int column) {
	return analysisModule.getTableValueAt(dataType, category, 
					      row, column);
    }

    /**
     *  To allow the table to be more intelligent in recognizing the
     *  format of a value.
     *
     *  In particular, it allows the Sorter to recognize numbers and
     *  causes right-indentation to be enforced for numbers.
     */
    public Class getColumnClass(int column) {
	return getValueAt(0, column).getClass();
    }
}
