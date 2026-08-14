package projections.gui;

import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class MyTableModel extends AbstractTableModel implements ActionListener {
    final private EntryMethodVisibility data;

    final private boolean displayVisibilityCheckboxes;
    final private List<List> tabledata;
    final private List<String> columnNames;

    public MyTableModel(List<List> TD, List<String> CN, EntryMethodVisibility data_, boolean checkboxesVisible) {
        tabledata = TD;
        columnNames = CN;
        data = data_;
        if (data != null)
            data.displayMustBeRedrawn();
        displayVisibilityCheckboxes = checkboxesVisible;
    }

    /** Decided by what the cell holds rather than by where it sits: the
     *  columns are not the same for every tool. Only the visibility box and
     *  the color swatch are editable -- the id column used to be as well,
     *  which let a user type over an entry method's id to no effect. */
    public boolean isCellEditable(int row, int col) {
        Object value = getValueAt(row, col);
        return (displayVisibilityCheckboxes && value instanceof Boolean)
                || value instanceof ClickableColorBox;
    }

    public int getColumnCount() {
        return columnNames.size();
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

    public void setValueAt(Object value, int row, int col) {
        if (col == 0 && displayVisibilityCheckboxes) {
            Integer id = (Integer) tabledata.get(row).get(2);
            if ((Boolean)value) {
                // remove from list of disabled entry methods
                data.makeEntryVisibleID(id);
            } else {
                // add to list of disabled entry methods
                data.makeEntryInvisibleID(id);
            }
            data.displayMustBeRedrawn();
        }

        tabledata.get(row).set(col, value);
        fireTableCellUpdated(row, col);
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println("Action for object: " + e.getSource());
        fireTableDataChanged();
        data.displayMustBeRedrawn();
    }
}