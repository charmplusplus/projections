package projections.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import projections.Tools.Timeline.Data;
import projections.analysis.Analysis;

/** A class that displays a color and selection chooser for entry methods */
public class ChooseEntriesWindow extends JFrame
{
	private EntryMethodVisibility data;
	private Map<Integer, String> entryNames;
	/** Per entry method counts, when the tool supplies meaningful ones. Empty
	 *  otherwise, which is what keeps the list in entry method id order. */
	private Map<Integer, Integer> entryCounts = new HashMap<Integer, Integer>();
	/** Counts below this are shown greyed: present in the range, but too few
	 *  to be anything on a chart. Derived from the largest count, since what
	 *  counts as negligible depends on the run. */
	private int dimBelowCount = 0;
	/** Fixed when the window is built, so the rows never disagree with the
	 *  columns even if the list is rebuilt later. */
	private boolean showCountColumn = false;
	/** A thousandth of the busiest entry method is the line. */
	private static final int DIM_RATIO = 1000;
	private List<List> tabledata;
	private List<String> columnNames;
	private boolean displayVisibilityCheckboxes;
	private ColorUpdateNotifier gw;
	private int myRun = 0;

	private JButton checkAll;
	private JButton uncheckAll;
	private JCheckBox displayAllEntryMethods;
	private JTextField searchField;
	private TableRowSorter<MyTableModel> sorter;

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
		entryCounts.clear();
		dimBelowCount = 0;
		boolean useCounts = data.sortEntriesByCount();
		// asked for once: some tools scan their whole display to answer this
		int[] entriesInRange = data.getEntriesArray();
		int largest = 0;
		for (int i = 0; i < entriesInRange.length; i++) {
			if (MainWindow.runObject[myRun].getSts().getEntryNames().containsKey(i) && entriesInRange[i]!=0) {
				entryNames.put(i, MainWindow.runObject[myRun].getSts().getEntryNames().get(i) +
						"::" +
						MainWindow.runObject[myRun].getSts().entryChares.get(i));
				if (useCounts) {
					entryCounts.put(i, entriesInRange[i]);
					if (entriesInRange[i] > largest) {
						largest = entriesInRange[i];
					}
				}
			}
		}
		dimBelowCount = largest / DIM_RATIO;
		addIdleOverhead();
	}

	private void allEntryMethods() {
		entryNames =  MainWindow.runObject[myRun].getSts().getPrettyEntryNames();
		// nothing outside the range has a count, so this list stays in id order
		entryCounts.clear();
		dimBelowCount = 0;
		addIdleOverhead();
	}
	
	private void addIdleOverhead() {
		if (data!= null && data.handleIdleOverhead() || data==null) {
			entryNames.put(Analysis.OVERHEAD_ENTRY_POINT, "Overhead");
			entryNames.put(Analysis.IDLE_ENTRY_POINT, "Idle");
		}
	}

	/** The entry methods in the order they should be listed: busiest first
	 *  when the tool gave counts, otherwise by entry method id as always. */
	private List<Integer> displayOrder() {
		List<Integer> ids = new ArrayList<Integer>(entryNames.keySet());
		if (entryCounts.isEmpty()) {
			return ids;
		}
		Collections.sort(ids, new Comparator<Integer>() {
			public int compare(Integer a, Integer b) {
				int countA = entryCounts.containsKey(a) ? entryCounts.get(a) : 0;
				int countB = entryCounts.containsKey(b) ? entryCounts.get(b) : 0;
				if (countA != countB) {
					return (countA < countB) ? 1 : -1;
				}
				return a.compareTo(b);
			}
		});
		return ids;
	}

	private void makeTableData() {
		tabledata.clear();
		Iterator<Integer> iter = displayOrder().iterator();
		while(iter.hasNext()){
			Integer id = iter.next();
			String name = entryNames.get(id);
			List tableRow = new ArrayList(5);

			if (displayVisibilityCheckboxes) {
				Boolean b = data.entryIsVisibleID(id);
				tableRow.add(b);
			}

			ClickableColorBox c = new ClickableColorBox(id, MainWindow.runObject[myRun].getEntryColor(id), myRun, gw);

			tableRow.add(name);
			tableRow.add(id);
			// after the id, so the id stays the third element that the table
			// model and "Hide All" read the entry method out of
			if (showCountColumn) {
				tableRow.add(entryCounts.containsKey(id) ? entryCounts.get(id) : Integer.valueOf(0));
			}
			tableRow.add(c);

			tabledata.add(tableRow);
		}
	}

	private void createLayout(){
		setTitle("Choose which entry methods are displayed and their colors");


		tabledata = new ArrayList<List>();

		// loaded first: whether there are counts to show decides the columns
		if (data!=null && data.hasEntryList())
			onlyEntryMethodsInRange();
		else
			allEntryMethods();

		showCountColumn = !entryCounts.isEmpty();

		// create a table of the data
		columnNames = new ArrayList<String>(5);
		if (displayVisibilityCheckboxes)
			columnNames.add("Visible");
		columnNames.add("Entry Method");
		columnNames.add("ID");
		if (showCountColumn)
			columnNames.add("Count");
		columnNames.add("Color");

		makeTableData();

		final MyTableModel tableModel = new MyTableModel(tabledata, columnNames, data, displayVisibilityCheckboxes); 

		JTable table = new JTable(tableModel);
		initColumnSizes(table);

		table.setDefaultRenderer(ClickableColorBox.class, new ColorRenderer());
		table.setDefaultEditor(ClickableColorBox.class, new ColorEditor());

		if (showCountColumn) {
			int nameColumn = displayVisibilityCheckboxes ? 1 : 0;
			SmallCountRenderer renderer = new SmallCountRenderer(table);
			table.getColumnModel().getColumn(nameColumn).setCellRenderer(renderer);
			table.getColumnModel().getColumn(nameColumn+2).setCellRenderer(renderer);
		}

		// row sorter used only for the search filter; column-click sorting
		// stays off (the color column is not comparable)
		sorter = new TableRowSorter<MyTableModel>(tableModel);
		for (int c=0; c<tableModel.getColumnCount(); c++) {
			sorter.setSortable(c, false);
		}
		table.setRowSorter(sorter);

		searchField = new JTextField();
		searchField.setToolTipText("Type part of an entry method name (or ID) to filter the list");
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

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		topPanel.add(searchPanel, BorderLayout.SOUTH);

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

			topPanel.add(buttonPanel, BorderLayout.NORTH);
		}

		p.add(topPanel, BorderLayout.NORTH);
		p.add(scroller, BorderLayout.CENTER);

		// Color choices take effect as they are made; this just dismisses
		// the window, which otherwise has no obvious way to be closed.
		JButton done = new JButton("Done");
		done.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		JPanel donePanel = new JPanel();
		donePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		donePanel.add(done);
		p.add(donePanel, BorderLayout.SOUTH);

		this.setContentPane(p);

		// Display it all

		pack();
		setSize(800,400);
		setVisible(true);
	}

	/** Show only rows whose entry method name (or ID) contains the search text */
	private void updateFilter() {
		String text = searchField.getText().trim();
		if (text.isEmpty()) {
			sorter.setRowFilter(null);
			return;
		}
		int nameColumn = displayVisibilityCheckboxes ? 1 : 0;
		sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text),
				nameColumn, nameColumn+1));
	}

	public void changeVisibility(boolean visible, MyTableModel tableModel) {
		Iterator<List> iter= tabledata.iterator();
		while(iter.hasNext()) {
			List v = iter.next();
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
		// by name rather than by position: the count column is only there for
		// some tools, and it shifts everything after it
		for (int c=0; c<columnNames.size(); c++) {
			String name = columnNames.get(c);
			TableColumn column = table.getColumnModel().getColumn(c);
			if (name.equals("Visible")) {
				column.setPreferredWidth(70);
			} else if (name.equals("Entry Method")) {
				column.setPreferredWidth(680);
			} else if (name.equals("ID")) {
				column.setPreferredWidth(50);
			} else if (name.equals("Count")) {
				column.setPreferredWidth(90);
			}
		}
	}

	/** Greys the entry methods whose counts are negligible next to the biggest
	 *  one, and gives the counts thousands separators. They sort to the bottom
	 *  of the list anyway; this says why they are down there. */
	private class SmallCountRenderer extends DefaultTableCellRenderer {
		private final JTable owner;
		private final DecimalFormat format = new DecimalFormat("###,###");

		SmallCountRenderer(JTable owner) {
			this.owner = owner;
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
			setHorizontalAlignment((value instanceof Integer) ? RIGHT : LEFT);
			if (value instanceof Integer) {
				setText(format.format(value));
			}
			if (!isSelected) {
				// set every time, never only for the grey ones: this renderer
				// is one reused component whose setForeground sticks, so a
				// single grey row would otherwise grey every row after it
				setForeground((countOfRow(row) < dimBelowCount)
						? Color.gray : table.getForeground());
			}
			return c;
		}

		/** The count behind a displayed row, which the search filter can have
		 *  moved away from its position in the model. */
		private int countOfRow(int row) {
			int modelRow = owner.convertRowIndexToModel(row);
			if (modelRow < 0 || modelRow >= tabledata.size()) {
				return Integer.MAX_VALUE;
			}
			Object count = tabledata.get(modelRow).get(displayVisibilityCheckboxes ? 3 : 2);
			return (count instanceof Integer) ? (Integer)count : Integer.MAX_VALUE;
		}
	}
}
