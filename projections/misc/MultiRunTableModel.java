package projections.misc;

import javax.swing.table.*;

import projections.analysis.*;

/**
 *  This class serves as a table model manipulator. It acquires its data
 *  from the analysis module for MultiRuns.
 */
public class MultiRunTableModel
    extends AbstractTableModel
{
    private String tableName;
    private MultiRunDataAnalyzer analysisModule;
    // This is the category type the associated JTable is supposed to
    // visualize.
    private int category;
    private int dataType;

    public MultiRunTableModel(String tableName,
			      MultiRunDataAnalyzer analysisModule,
			      int dataType, int category) {
	this.tableName = tableName;
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
	return analysisModule.getNumColumns(dataType, category);
    }

    /**
     *  This method is closely tied to the display table. The analysisModule
     *  is expected to ask the display table for the currently displayed
     *  table (hence in the MultiRun case, the category) and to supply data 
     *  accordingly.
     */
    public Object getValueAt(int row, int column) {
	return new Double(analysisModule.getTableValueAt(dataType, category, 
							 row, column));
    }
}
