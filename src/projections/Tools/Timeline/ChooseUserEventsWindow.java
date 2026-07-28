package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

class ChooseUserEventsWindow extends JFrame
{
	private Data data;
	private Map<Integer, String> names;
	private Vector<Vector> tabledata;
	private Vector<String> columnNames;
	private JTextField searchField;
	private TableRowSorter<MyTableModel> sorter;

	ChooseUserEventsWindow(Data _data){
		data = _data;
		createLayout();
	}

	private void createLayout(){
		setTitle("Choose which user events are displayed");

		// create a table of the data
		columnNames = new Vector();
		columnNames.add(new String("Visible"));
		columnNames.add(new String("User Event Name"));
		columnNames.add(new String("ID"));
		columnNames.add(new String("Color"));


		tabledata  = new Vector();

		names =  data.getUserEventNames();

		// Add special purpose row for user supplied notes
		{
			Boolean b = true;
			Vector tableRow = new Vector();
			tableRow.add(b);
			tableRow.add("Unspecified");
			tableRow.add("");
			tableRow.add(Color.white);
			tabledata.add(tableRow);
		}
		
	
		// Add rows in the table for each user event id
		Iterator<Integer> iter = names.keySet().iterator();
		while(iter.hasNext()){
			Integer id = iter.next();
			String name = names.get(id);
			Vector tableRow = new Vector();
			
			Boolean b = data.entryIsVisibleID(id);

			Color c = data.getUserEventColor(id);
			
			tableRow.add(b);
			tableRow.add(name);
			tableRow.add(id);
			tableRow.add(c);
			
			tabledata.add(tableRow);
		}

		MyTableModel tableModel = new MyTableModel();

		JTable table = new JTable(tableModel);
		initColumnSizes(table);

		table.setDefaultRenderer(Color.class, new ColorRenderer());

		// row sorter used only for the search filter; column-click sorting
		// stays off (the color column is not comparable)
		sorter = new TableRowSorter<MyTableModel>(tableModel);
		for (int c=0; c<tableModel.getColumnCount(); c++) {
			sorter.setSortable(c, false);
		}
		table.setRowSorter(sorter);

		searchField = new JTextField();
		searchField.setToolTipText("Type part of a user event name (or ID) to filter the list");
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { updateFilter(); }
			public void removeUpdate(DocumentEvent e) { updateFilter(); }
			public void changedUpdate(DocumentEvent e) { updateFilter(); }
		});
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BorderLayout());
		searchPanel.add(new JLabel(" Search: "), BorderLayout.WEST);
		searchPanel.add(searchField, BorderLayout.CENTER);

		// put the table into a scrollpane
		JScrollPane scroller = new JScrollPane(table);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// put the scrollpane into our guiRoot
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(searchPanel, BorderLayout.NORTH);
		p.add(scroller, BorderLayout.CENTER);
		this.setContentPane(p);

		// Display it all

		pack();
		setSize(800,400);
		setVisible(true);

	}


	/** Show only rows whose user event name (or ID) contains the search text */
	private void updateFilter() {
		String text = searchField.getText().trim();
		if (text.isEmpty()) {
			sorter.setRowFilter(null);
			return;
		}
		sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1, 2));
	}

	private void initColumnSizes(JTable table) {
		TableColumn column = null;

		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(70);

		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(680);

		column = table.getColumnModel().getColumn(2);
		column.setPreferredWidth(50);

	}
	

	private class MyTableModel extends AbstractTableModel {

		public boolean isCellEditable(int row, int col) {
			if (col >= 1) {
				return false;
			} else {
				return true;
			}
		}

		public Class getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}


		public int getColumnCount() {
			return 4;
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
			if(col==0){
				Boolean newValue = (Boolean) value;

				if(row == 0){
//					// Show/Hide the user supplied notes
//					if(newValue){
//						data.makeUserSuppliedNotesVisible();
//					} else {
//						data.makeUserSuppliedNotesInvisible();
//					}		
				} else { 
					// Show/Hide the normal user events
					Integer id = (Integer) tabledata.get(row).get(2);
					if(newValue){
						data.makeUserEventVisibleID(id);// remove from list of disabled entry methods
					} else {
						data.makeUserEventInvisibleID(id);// add to list of disabled entry methods
					}		
				}

			}
			
			tabledata.get(row).set(col,value);
			fireTableCellUpdated(row, col);

		}

	}    

	
    /// A simple color renderer
	private class ColorRenderer extends JLabel
	implements TableCellRenderer {	
		private ColorRenderer() {
			setOpaque(true);
		}
		public Component getTableCellRendererComponent(
				JTable table, Object color,
				boolean isSelected, boolean hasFocus,
				int row, int column) {
			setBackground((Color) color);
			return this;
		}
	}
	
	
	
	

}
