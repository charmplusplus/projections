package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.AbstractCellEditor;
import javax.swing.table.TableCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class ChooseEntriesWindow extends JFrame 
{
	private Data data;
	private Hashtable<Integer, String> entryNames;
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

		Iterator<Integer> iter = entryNames.keySet().iterator();
		while(iter.hasNext()){
			Integer id = iter.next();
			String name = entryNames.get(id);
			Vector tableRow = new Vector();

			Boolean b = data.entryIsVisibleID(id);

			ClickableColorBox c = new ClickableColorBox(id, data.getEntryColor(id));

			tableRow.add(b);
			tableRow.add(name);
			tableRow.add(id);
			tableRow.add(c);

			tabledata.add(tableRow);
		}

		MyTableModel tableModel = new MyTableModel(); 

		JTable table = new JTable(tableModel);
		initColumnSizes(table);

		table.setDefaultRenderer(ClickableColorBox.class, new ColorRenderer());
		table.setDefaultEditor(ClickableColorBox.class, new ColorEditor());

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


	private class MyTableModel extends AbstractTableModel implements ActionListener{

		public boolean isCellEditable(int row, int col) {
			if (col == 0 || col == 3) {
				return true;
			} else {
				return false;
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
			} else {
//				System.out.println("setValueAt col = " + col);
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


	/// A class that incorporates an integer identifier and its corresponding paint
	private class ClickableColorBox {
		public int id;
		public Color c;
		public ClickableColorBox(int id, Color c){
			this.id = id;
			this.c = c;
		}
		public void setColor(Color c){
			this.c = c;
			data.setColorForEntry(id, c);
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
			if(color instanceof Color){
				setBackground((Color) color);
			}else if(color instanceof ClickableColorBox){
				setBackground(((ClickableColorBox) color).c);
			}

			return this;
		}
	}



	public class ColorEditor extends AbstractCellEditor
	implements TableCellEditor,
	ActionListener {
		ClickableColorBox currentColorBox;
		JButton button;
		JColorChooser colorChooser;
		JDialog dialog;
		protected static final String EDIT = "edit";

		public ColorEditor() {
			button = new JButton();
			button.setActionCommand(EDIT);
			button.addActionListener(this);
			button.setBorderPainted(false);

			//Set up the dialog that the button brings up.
			colorChooser = new JColorChooser();
			dialog = JColorChooser.createDialog(button,
					"Pick a Color",
					true,  //modal
					colorChooser,
					this,  //OK button handler
					null); //no CANCEL button handler
		}

		public void actionPerformed(ActionEvent e) {
			if (EDIT.equals(e.getActionCommand())) {
				//The user has clicked the cell, so
				//bring up the dialog.
				button.setBackground(currentColorBox.c);
				colorChooser.setColor(currentColorBox.c);
				dialog.setVisible(true);

				fireEditingStopped(); //Make the renderer reappear.

			} else { //User pressed dialog's "OK" button.
				currentColorBox.setColor(colorChooser.getColor());
			}
		}

		//Implement the one CellEditor method that AbstractCellEditor doesn't.
		public Object getCellEditorValue() {
			return currentColorBox;
		}

		//Implement the one method defined by TableCellEditor.
		public Component getTableCellEditorComponent(JTable table,
				Object value,
				boolean isSelected,
				int row,
				int column) {
			
			currentColorBox = (ClickableColorBox)value;
			
			return button;
		}
	}




}
