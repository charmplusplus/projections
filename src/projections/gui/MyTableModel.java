package projections.gui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

//This class will be further implemented in EntryColorsWindow.java and ChooseEntriesWindow.java
public class MyTableModel extends AbstractTableModel implements ActionListener{
	protected Vector<Vector> tabledata;
	protected Vector<String> columnNames;
	
	public MyTableModel(Vector<Vector> TD, Vector<String> CN) {
		tabledata=TD;
		columnNames=CN;
	}
	
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	public int getRowCount() {
		return tabledata.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		return tabledata.get(rowIndex).get(columnIndex);
	}

	public String getColumnName(int col) {
		return columnNames.get(col);
	}
	
	//this function will never be called because it will be over-ridden in its children classes
	public int getColumnCount() {
		return -1;
	}
	
	//this will never be called either
	public void actionPerformed(ActionEvent e) {}
}
