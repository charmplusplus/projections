package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import projections.gui.*;

/** A class that displays a color and selection chooser for entry methods */
class ChooseEntriesWindow extends JFrame 
{
	private Data data;
	private Map<Integer, String> entryNames;
	private Vector<Vector> tabledata;
	private Vector<String> columnNames;

	private JButton checkAll;
	private JButton uncheckAll;


	ChooseEntriesWindow(Data _data){
		data = _data;
		createLayout();
	}

	private void createLayout(){
		setTitle("Choose which entry methods are displayed and their colors");


		// create a table of the data
		columnNames = new Vector();
		columnNames.add(new String("Visible"));
		columnNames.add(new String("Entry Method"));
		columnNames.add(new String("ID"));
		columnNames.add(new String("Color"));


		tabledata  = new Vector();

		entryNames =  data.getEntryNames();
		entryNames.put(-1, "Overhead");
		entryNames.put(-2, "Idle");

		Iterator<Integer> iter = entryNames.keySet().iterator();
		while(iter.hasNext()){
			Integer id = iter.next();
			String name = entryNames.get(id);
			Vector tableRow = new Vector();

			Boolean b = data.entryIsVisibleID(id);

			ClickableColorBox1 c = new ClickableColorBox1(id, data.getEntryColor(id), data);

			tableRow.add(b);
			tableRow.add(name);
			tableRow.add(id);
			tableRow.add(c);

			tabledata.add(tableRow);
		}

		MyTableModel1 tableModel = new MyTableModel1(tabledata, columnNames, data, checkAll, uncheckAll); 

		JTable table = new JTable(tableModel);
		initColumnSizes(table);

		table.setDefaultRenderer(ClickableColorBox1.class, new ColorRenderer());
		table.setDefaultEditor(ClickableColorBox1.class, new ColorEditor());

		// put the table into a scrollpane
		JScrollPane scroller = new JScrollPane(table);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		checkAll = new JButton("Make All Visible");
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
}   


/// A class that incorporates an integer identifier and its corresponding paint
class ClickableColorBox1 extends ClickableColorBox {
	Data data;
	
	public ClickableColorBox1(int id, Color c, Data data_) {
		super(id, c);
		data=data_;
	}
	
	public void setColor(Color c){
		this.c = c;
		data.setColorForEntry(id, c);
	}
}

class MyTableModel1 extends MyTableModel implements ActionListener{
	Data data;
	JButton checkAll;
	JButton uncheckAll;

	public MyTableModel1(Vector<Vector> TD, Vector<String> CN, Data data_, JButton checkAll_, JButton uncheckAll_) {
		super(TD, CN);
		data = data_;
	}
	
	public boolean isCellEditable(int row, int col) {
		if (col == 0 || col == 3) {
			return true;
		} else {
			return false;
		}
	}

	public int getColumnCount() {
		return 4;
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
		} else {
//			System.out.println("setValueAt col = " + col);
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
		} else if(e.getSource() == uncheckAll) {
			Iterator<Vector> iter= tabledata.iterator();
			while(iter.hasNext()){
				Vector v = iter.next();
				Integer id = (Integer) v.get(2);
				// update the backing data for the table
				v.set(0,false);
				// Update the visualization (but don't redraw yet)
				data.makeEntryInvisibleID(id, false);
			}				
		} else {
			System.out.println("Action for object: " + e.getSource());
		}


		data.displayMustBeRedrawn();
		fireTableDataChanged();
	}
}
