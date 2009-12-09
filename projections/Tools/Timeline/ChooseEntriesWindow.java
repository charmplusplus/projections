package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class ChooseEntriesWindow extends JFrame 
{
	Data data;
	Hashtable<Integer, String> entryNames;
	Vector<Vector> tabledata;
	Vector<String> columnNames;

	JButton checkAll;
	JButton uncheckAll;
	
	
	ChooseEntriesWindow(Data _data){
		data = _data;
		createLayout();
	}

	private void createLayout(){
		setTitle("Choose which entry methods are displayed");
		

		// create a table of the data
		columnNames = new Vector();
		columnNames.add(new String("Visible"));
		columnNames.add(new String("Entry Method"));
		columnNames.add(new String("ID"));
		columnNames.add(new String("Color"));


		tabledata  = new Vector();

		entryNames =  data.getEntryNames();

		Iterator<Integer> iter = entryNames.keySet().iterator();
		while(iter.hasNext()){
			Integer id = iter.next();
			String name = entryNames.get(id);
			Vector tableRow = new Vector();
			
			Boolean b = data.entryIsVisibleID(id);

			Color c = data.getEntryColor(id);
			
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
		
		// put the table into a scrollpane
		JScrollPane scroller = new JScrollPane(table);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		checkAll = new JButton("Show All");
		uncheckAll = new JButton("Hide All");
		checkAll.addActionListener(tableModel);
		uncheckAll.addActionListener(tableModel);
		buttonPanel.add(checkAll);
		buttonPanel.add(uncheckAll);
		
		// put the scrollpane into our guiRoot
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		
		p.add(buttonPanel, BorderLayout.NORTH);
		p.add(scroller, BorderLayout.CENTER);

		
		this.setContentPane(p);

		// Display it all

		pack();
		setSize(800,400);
		setVisible(true);

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


	class MyTableModel extends AbstractTableModel implements ActionListener{

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
				Integer id = (Integer) tabledata.get(row).get(2);

				if(newValue){
					// remove from list of disabled entry methods
					data.makeEntryVisibleID(id);
				} else {
					// add to list of disabled entry methods
					data.makeEntryInvisibleID(id);
				}				
			}

			tabledata.get(row).set(col,value);
			fireTableCellUpdated(row, col);

		}

			

		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == checkAll){
				Iterator<Vector> iter= tabledata.iterator();
				while(iter.hasNext()){
					Vector v = iter.next();
					Integer id = (Integer) v.get(2);
					// update the backing data for the table
					v.set(0,true);				
					// Update the visualization (but don't redraw yet)
					data.makeEntryVisibleID(id, false);
				}
			} else {
				Iterator<Vector> iter= tabledata.iterator();
				while(iter.hasNext()){
					Vector v = iter.next();
					Integer id = (Integer) v.get(2);
					// update the backing data for the table
					v.set(0,false);
					// Update the visualization (but don't redraw yet)
					data.makeEntryInvisibleID(id, false);
				}				
			}
			data.displayMustBeRedrawn();
			fireTableDataChanged();
			
		}

		
	}    
	
	

	
    /// A simple color renderer
	public class ColorRenderer extends JLabel
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
