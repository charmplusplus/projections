package projections.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import projections.Tools.Timeline.Data;

/** A class that displays a color and selection chooser for entry methods */
public class ChooseEntriesWindow extends JFrame
{
	private EntryMethodVisibility data;
	private Map<Integer, String> entryNames;
	private Vector<Vector> tabledata;
	private Vector<String> columnNames;
	private boolean displayVisibilityCheckboxes;
	private ColorUpdateNotifier gw;
	private int myRun = 0;

	private JButton checkAll;
	private JButton uncheckAll;
	private JCheckBox displayAllEntryMethods;

	public ChooseEntriesWindow(ColorUpdateNotifier _gw) {
		data = null;
		displayVisibilityCheckboxes = false;
		gw = _gw;
		createLayout();
	}

	public ChooseEntriesWindow(EntryMethodVisibility _data, boolean checkboxesVisible, ColorUpdateNotifier _gw){
		data = _data;
		displayVisibilityCheckboxes = checkboxesVisible;
		gw = _gw;
		createLayout();
	}

	private void onlyEntryMethodsInRange() {
		entryNames = new TreeMap<Integer, String>();
		for (int i = 0; i < data.getEntriesArray().length; i++) {
			if (MainWindow.runObject[myRun].getSts().getEntryNames().containsKey(i) && data.getEntriesArray()[i]!=0)
				entryNames.put(i, MainWindow.runObject[myRun].getSts().getEntryNames().get(i) + 
						"::" + 
						MainWindow.runObject[myRun].getSts().entryChareNames.get(i));
		}
		addIdleOverhead();
	}

	private void allEntryMethods() {
		entryNames =  MainWindow.runObject[myRun].getSts().getPrettyEntryNames();
		addIdleOverhead();
	}
	
	private void addIdleOverhead() {
		if (data.handleIdleOverhead()) {
			entryNames.put(Analysis.isOverhead, "Overhead");
			entryNames.put(Analysis.isIdle, "Idle");
		}
	}

	private void makeTableData() {
		tabledata.clear();
		Iterator<Integer> iter = entryNames.keySet().iterator();
		while(iter.hasNext()){
			Integer id = iter.next();
			String name = entryNames.get(id);
			Vector tableRow = new Vector();

			if (displayVisibilityCheckboxes) {
				Boolean b = data.entryIsVisibleID(id);
				tableRow.add(b);
			}

			ClickableColorBox c = new ClickableColorBox(id, MainWindow.runObject[myRun].getEntryColor(id), myRun, gw);

			tableRow.add(name);
			tableRow.add(id);
			tableRow.add(c);

			tabledata.add(tableRow);
		}
	}

	private void createLayout(){
		setTitle("Choose which entry methods are displayed and their colors");


		// create a table of the data
		columnNames = new Vector();
		if (displayVisibilityCheckboxes)
			columnNames.add(new String("Visible"));
		columnNames.add(new String("Entry Method"));
		columnNames.add(new String("ID"));
		columnNames.add(new String("Color"));

		tabledata  = new Vector();

		if (data!=null && data.hasEntryList())
			onlyEntryMethodsInRange();
		else
			allEntryMethods();

		makeTableData();

		final MyTableModel tableModel = new MyTableModel(tabledata, columnNames, data, displayVisibilityCheckboxes); 

		JTable table = new JTable(tableModel);
		initColumnSizes(table);

		table.setDefaultRenderer(ClickableColorBox.class, new ColorRenderer());
		table.setDefaultEditor(ClickableColorBox.class, new ColorEditor());

		// put the table into a scrollpane
		JScrollPane scroller = new JScrollPane(table);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// put the scrollpane into our guiRoot
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());

		if (displayVisibilityCheckboxes) {
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout());
			checkAll = new JButton("Make All Visible");
			uncheckAll = new JButton("Hide All");
			checkAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					changeVisibility(true, tableModel);
					tableModel.fireTableDataChanged();
					data.displayMustBeRedrawn();
				}
			});
			uncheckAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					changeVisibility(false, tableModel);
					tableModel.fireTableDataChanged();
					data.displayMustBeRedrawn();
				}
			});
			buttonPanel.add(checkAll);
			buttonPanel.add(uncheckAll);
			if (data!=null && data instanceof Data) {
				displayAllEntryMethods = new JCheckBox("Show All Entry Methods");
				displayAllEntryMethods.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if (ItemEvent.DESELECTED==e.getStateChange() && data!=null && data.hasEntryList())
							onlyEntryMethodsInRange();
						else if (data!=null && data.hasEntryList())
							allEntryMethods();
						makeTableData();
						tableModel.fireTableDataChanged();
						data.displayMustBeRedrawn();
					}
				});
				buttonPanel.add(displayAllEntryMethods);
			}

			p.add(buttonPanel, BorderLayout.NORTH);
		}

		p.add(scroller, BorderLayout.CENTER);

		this.setContentPane(p);

		// Display it all

		pack();
		setSize(800,400);
		setVisible(true);
	}

	public void changeVisibility(boolean visible, MyTableModel tableModel) {
		Iterator<Vector> iter= tabledata.iterator();
		while(iter.hasNext()) {
			Vector v = iter.next();
			Integer id = (Integer) v.get(2);
			if (visible)
				data.makeEntryVisibleID(id);
			else
				data.makeEntryInvisibleID(id);
		}
		for (int i=0; i<tabledata.size(); i++) {
			tabledata.get(i).set(0,visible);
		}
		tableModel.fireTableDataChanged();
		data.displayMustBeRedrawn();
	}

	private void initColumnSizes(JTable table) {
		TableColumn column = null;

		if (displayVisibilityCheckboxes) {
			column = table.getColumnModel().getColumn(0);
			column.setPreferredWidth(70);

			column = table.getColumnModel().getColumn(1);
			column.setPreferredWidth(680);

			column = table.getColumnModel().getColumn(2);
			column.setPreferredWidth(50);
		}
		else {
			column = table.getColumnModel().getColumn(0);
			column.setPreferredWidth(680);

			column = table.getColumnModel().getColumn(1);
			column.setPreferredWidth(50);
		}

	}
}


/// A class that incorporates an integer identifier and its corresponding paint
class ClickableColorBox {
	public int myRun;
	public ColorUpdateNotifier gw;
	int id;
	Color c;

	public ClickableColorBox(int id_, Color c_, int myRun_, ColorUpdateNotifier gw_) {
		id = id_;
		c = c_;
		myRun = myRun_;
		gw=gw_;
	}

	public void setColor(Color c){
		this.c = c;
		MainWindow.runObject[myRun].setEntryColor(id, c);
		gw.colorsHaveChanged();
	}
}

class MyTableModel extends AbstractTableModel implements ActionListener {
	EntryMethodVisibility data;
	JCheckBox displayAllEntryMethods;
	boolean displayVisibilityCheckboxes;
	Vector<Vector> tabledata;
	Vector<String> columnNames;

	public MyTableModel(Vector<Vector> TD, Vector<String> CN, EntryMethodVisibility data_, boolean checkboxesVisible) {
		tabledata=TD;
		columnNames=CN;
		data = data_;
		if (data!=null)
			data.displayMustBeRedrawn();
		displayVisibilityCheckboxes = checkboxesVisible;
	}

	public boolean isCellEditable(int row, int col) {
		if (displayVisibilityCheckboxes && (col == 0 || col == 3)) {
			return true;
		} else if (col == 2) {
			return true;
		} else {
			return false;
		}
	}

	public int getColumnCount() {
		if (displayVisibilityCheckboxes)
			return 4;
		else
			return 3;
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
		if(col==0 && displayVisibilityCheckboxes){
			Boolean newValue = (Boolean) value;
			Integer id = (Integer) tabledata.get(row).get(2);

			if(newValue){
				// remove from list of disabled entry methods
				data.makeEntryVisibleID(id);
			} else {
				// add to list of disabled entry methods
				data.makeEntryInvisibleID(id);
			}
			data.displayMustBeRedrawn();
		} else {
//			System.out.println("setValueAt col = " + col);
		}

		tabledata.get(row).set(col,value);
		fireTableCellUpdated(row, col);
	}

	public void actionPerformed(ActionEvent e) {
		System.out.println("Action for object: " + e.getSource());
		fireTableDataChanged();
		data.displayMustBeRedrawn();
	}
}
