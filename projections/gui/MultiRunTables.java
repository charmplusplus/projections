package projections.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import projections.misc.*;
import projections.analysis.*;
import projections.gui.count.*;

/**
 *  Responsible for displaying multiple-run projections data in a 2D tabulated
 *  form. Each category is placed in a separate tab.
 *
 *  There are no current plans to move data entries between tabs (ie. change
 *  categories) nor allow the user to add new categories. These can, however,
 *  be incorporated in future, more general, versions.
 */
public class MultiRunTables 
    extends JTabbedPane
{
    private MultiRunDataAnalyzer analysisModule;

    /**
     *  Default constructor. Creates an empty tabbed pane. The calling
     *  data supplier (currently hardcoded as MultiRun's analysis class.
     *  It can, and should be, generalized to an inheritable abstract/super
     *  class) is then expected to fill it in using makeTable.
     */
    public MultiRunTables(int defaultDataType,
			  MultiRunDataAnalyzer analysisModule) {
	this.analysisModule = analysisModule;
	// based on the analysisModule supplied, create all the tabs
	// necessary for its categorization scheme.
	String categories[] = analysisModule.getCategoryNames();
	for (int category=0; category<categories.length; category++) {
	    makeTable(categories[category], null, Color.black,
		      defaultDataType, category);
	}
    }

    /**
     *  This method sets the types of each table and forces a repaint.
     */
    public void setType(int dataType) {
	int numTables = getTabCount();
	for (int tableIdx=0; tableIdx<numTables; tableIdx++) {
	    JTable table =
		(JTable)((JScrollPane)getComponentAt(tableIdx)).getViewport().getView();
	    MultiRunTableModel model =
		(MultiRunTableModel)((TableSorter)table.getModel()).getModel();
	    model.setType(dataType);
	    table.repaint();
	}
    }

    /**
     *  Convenience method for creating a new table in a new tab.
     */
    public void makeTable(String name, String toolTip, Color color,
			  int dataType, int category) {
	MultiRunTableModel tableModel =
	    new MultiRunTableModel(name, analysisModule, dataType, category);
	// use Josh's modified Model manipulator for sorting columns.
	TableSorter sorter = new TableSorter(tableModel);
	JTable table = new JTable(sorter);
	sorter.addMouseListenerToHeaderInTable(table);
	initColumnSizes(table);
	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	JScrollPane scrollPane = new JScrollPane(table);
	// make sure we do not become too small
	table.setPreferredScrollableViewportSize(new Dimension(500,500));
	addTab(name, null, scrollPane, toolTip);
	setForegroundAt(indexOfComponent(scrollPane), color);
    }
    
    /**
     *  Convenience method for making each column as long as it's header
     *  needs to be.
     *
     *  Code adapted from TableRenderDemo (pg 734 to 735 of "The JFC Swing
     *  Tutorial - A Guide to Constructing GUIs").
     */
    private void initColumnSizes(JTable table) {
	TableColumn column;
	TableCellRenderer defaultHeaderRenderer =
	    table.getTableHeader().getDefaultRenderer();
	Component comp;
	int headerWidth;
	for (int col=0; col<table.getColumnCount(); col++) {
	    column = table.getColumnModel().getColumn(col);
	    column.setHeaderRenderer(defaultHeaderRenderer);
	    comp = 
		column.getHeaderRenderer().getTableCellRendererComponent(null,
									 column.getHeaderValue(),
									 false,
									 false,
									 0, 0);
	    headerWidth = comp.getPreferredSize().width;
									    
	    column.setPreferredWidth(headerWidth);
	}
    }
}
