package projections.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
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
			ClickableColorBox c = new ClickableColorBox(id, epColor);

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


	private class MyTableModel extends AbstractTableModel implements ActionListener{

		public boolean isCellEditable(int row, int col) {
			if (col == 2) {
				return true;
			} else {
				return false;
			}
		}

		public Class getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}


		public int getColumnCount() {
			return 3;
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
		
			tabledata.get(row).set(col,value);
			fireTableCellUpdated(row, col);

		}


		public void actionPerformed(ActionEvent e) {
			System.out.println("Action for object: " + e.getSource());
			System.out.println("Update/refresh the plot here");
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
			MainWindow.runObject[myRun].setEntryColor(id, c);
			gw.colorsHaveChanged();
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
