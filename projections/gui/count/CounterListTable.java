// UNUSED FILE

//package projections.gui.count;
//
//import javax.swing.*;
//import javax.swing.table.*;
//import java.awt.*;
//import java.awt.event.*;
//
//public class CounterListTable extends AbstractTableModel 
//{
//  public CounterListTable(Counter[] counters) { counters_ = counters; }
//
//  private class ColorRenderer extends DefaultTableCellRenderer {
//    public ColorRenderer() { }
//    public Component getTableCellRendererComponent(
//      JTable t, Object v, boolean i, boolean h, int r, int c)
//    {
//      setBackground(counters_[r].color);
//      return super.getTableCellRendererComponent(t, v, i, h, r, c);
//    }
//  }
//
//  public void configure(JTable table) {
//    TableColumn column = null;
//    column = table.getColumnModel().getColumn(0);
//    column.setPreferredWidth(50);
//    column.setCellRenderer(new ColorRenderer());
//    column = table.getColumnModel().getColumn(1);
//    column.setPreferredWidth(350);
//  }
//
//  public int getColumnCount() { return 2; }
//  public int getRowCount() { return counters_.length; }
//  public String getColumnName(int columnIndex) { 
//    switch (columnIndex) {
//    case 0:   return "Counter";
//    case 1:   return "Description";
//    default:  return "ERROR";
//    }
//  }
//  public Object getValueAt(int row, int col) { 
//    switch (col) {
//    case 0:   return counters_[row].counterCode;
//    case 1:   return counters_[row].description;
//    default:  return "ERROR";
//    }
//  }
//
//  private Counter[] counters_ = null;
//}
//
//
//
//
//
//
//
//
