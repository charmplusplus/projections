package projections.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

/** A class that displays a color and selection chooser for entry methods */
public class ChooseEntryColorsWindow extends JFrame 
{
	private Map<Integer, String> entryNames;
	private Vector<Vector> tabledata;
	private Vector<String> columnNames;

	private int myRun = 0;

	ColorUpdateNotifier gw;
	
	public ChooseEntryColorsWindow(ColorUpdateNotifier gw){
		setTitle("Choose colors for each entry point");

		this.gw = gw;
		
		// create a table of the data
		columnNames = new Vector();
		columnNames.add(new String("Entry Method"));
		columnNames.add(new String("ID"));
		columnNames.add(new String("Color"));


		tabledata  = new Vector();

		entryNames = MainWindow.runObject[myRun].getSts().getPrettyEntryNames();
		entryNames.put(-1, "Overhead");
		entryNames.put(-2, "Idle");

		Iterator<Integer> iter = entryNames.keySet().iterator();
		while(iter.hasNext()){
			Integer id = iter.next();
			String name = entryNames.get(id);
			Vector tableRow = new Vector();

			Color epColor = MainWindow.runObject[myRun].getEntryColor(id);
			ClickableColorBox1 c = new ClickableColorBox1(id, epColor, myRun, gw);

			tableRow.add(name);
			tableRow.add(id);
			tableRow.add(c);

			tabledata.add(tableRow);
		}

		MyTableModel1 tableModel = new MyTableModel1(tabledata, columnNames); 

		JTable table = new JTable(tableModel);
		initColumnSizes(table);

		table.setDefaultRenderer(ClickableColorBox1.class, new ColorRenderer());
		table.setDefaultEditor(ClickableColorBox1.class, new ColorEditor());

		// put the table into a scrollpane
		JScrollPane scroller = new JScrollPane(table);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// put the scrollpane into our guiRoot
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
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
		column.setPreferredWidth(680);

		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(50);

	}   
}

/// A class that incorporates an integer identifier and its corresponding paint
class ClickableColorBox1 extends ClickableColorBox {
	public int myRun;
	public ColorUpdateNotifier gw;
	
	public ClickableColorBox1(int id, Color c, int myRun_, ColorUpdateNotifier gw_) {
		super(id, c);
		myRun = myRun_;
		gw=gw_;
	}
	public void setColor(Color c){
		this.c = c;
		MainWindow.runObject[myRun].setEntryColor(id, c);
		gw.colorsHaveChanged();
	}
}


class MyTableModel1 extends MyTableModel implements ActionListener{
	
	public MyTableModel1(Vector<Vector> TD, Vector<String> CN) {
		super(TD, CN);
	}

	public boolean isCellEditable(int row, int col) {
		if (col == 2) {
			return true;
		} else {
			return false;
		}
	}

	public int getColumnCount() {
		return 3;
	}

	public void setValueAt(Object value, int row, int col) {
		tabledata.get(row).set(col,value);
		fireTableCellUpdated(row, col);
	}

	public void actionPerformed(ActionEvent e) {
		System.out.println("Action for object: " + e.getSource());
		System.out.println("Update/refresh the plot here");
		fireTableDataChanged();
	}

} 
