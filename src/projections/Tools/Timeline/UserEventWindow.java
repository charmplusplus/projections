package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import projections.Tools.Timeline.RangeQueries.Query1D;
import projections.gui.FormattedNumber;
import projections.gui.count.TableSorter;

/** Joshua Mostkoff Unger
 *  Parallel Programming Laboratory
 * 
 *  UserEventWindow displays the UserEvents for the timeline 
 *  currently being viewed and manages their display.
 */
class UserEventWindow extends JFrame
{

	private Color BACKGROUND = Color.black;
	private Color FOREGROUND = Color.white;

	Data data;
	
	// set the ints to have commas in appropriate places
	private static DecimalFormat format_;
	private JCheckBox             checkbox_;  // when closing, set to false
	private Object[][]        events_ = null;
	private Integer[] pes;
	private JTabbedPane          tabbedPane_ = new JTabbedPane();
	private TableSorter[]        sorter_ = null;

	private DefaultTableCellRenderer rightJustify_ = 
		new DefaultTableCellRenderer();

	private class NameRenderer extends DefaultTableCellRenderer {

		private int index_;
		protected NameRenderer(int index) { index_ = index; }
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean selected, boolean focused, 
				int row, int column)
		{
			setHorizontalAlignment(JLabel.CENTER);
			setEnabled(table == null || table.isEnabled()); // see question above
			setForeground(((UserEventObject)events_[index_][sorter_[index_].mapRow(row)]).getColor(data));
			setBackground(BACKGROUND);
			super.getTableCellRendererComponent(
					table, value, selected, focused, row, column);
			return this;
		}
	}

	private class UserEventTable extends AbstractTableModel {
		private int index_;

		protected UserEventTable(int i) { index_ = i; }
		public int getColumnCount() {
			if (data.getNumNestedIDs() > 0)
				return 5;
			else
				return 4;
		}
		public int getRowCount() { 
			if (events_ != null && events_[index_] != null) { 
				return events_[index_].length; 
			}
			else { return 0; }
		}
		public String getColumnName(int columnIndex) { 
			switch (columnIndex) {
			case 0: return "User Event";
			case 1: return "Begin Time";
			case 2: return "End Time";
			case 3: return "Delta Time";
			case 4: return "Nested ID";
			default: return "ERROR";
			}
		}
		public Object getValueAt(int row, int col) { 
			if (events_ != null && events_[index_] != null) {
				switch (col) {
				case 0:  
					return ((UserEventObject)events_[index_][row]).getName();
				case 1:  return new FormattedNumber(((UserEventObject)events_[index_][row]).beginTime, format_);
				case 2:  return new FormattedNumber(((UserEventObject)events_[index_][row]).endTime, format_);
				case 3:  return new FormattedNumber(((UserEventObject)events_[index_][row]).endTime-
						((UserEventObject)events_[index_][row]).beginTime, format_);
				case 4:  return new FormattedNumber(((UserEventObject)events_[index_][row]).getNestedID(), format_);
				default: return "ERROR";
				}
			}
			else { return "ERROR"; }
		}
		public Class getColumnClass(int columnIndex) {
			if (columnIndex == 0) { return java.lang.String.class; }
			else { return projections.gui.FormattedNumber.class; }
		}
	}

	public void setData(Data data) { 
		this.data = data;
		
		if(data.numPs() <= 100){
			// TODO This file should be converted to use the treeset structure instead of these old arrays
			//  events_ is sorted already because it comes from a treeset
			events_ = new Object[data.numPs()][];
			pes = new Integer[data.numPs()];


			int pindex=0;
			for(Entry<Integer, Query1D<UserEventObject>> e : data.allUserEventObjects.entrySet()){
				Integer pe = e.getKey();
				Query1D<UserEventObject> userEventsForPe = e.getValue();
				events_[pindex] = userEventsForPe.toArray();	
				pes[pindex] = pe;
				pindex++;
			}

			// create the layout here
			super.getContentPane().removeAll();
			tabbedPane_.removeAll();
			if (events_.length > 1) { super.getContentPane().add(tabbedPane_); }
			sorter_ = new TableSorter[events_.length];

			for (int i=0; i<events_.length; i++) {
				UserEventTable userEvents = new UserEventTable(i);
				sorter_[i] = new TableSorter(userEvents);
				JTable table = new JTable(sorter_[i]);
				sorter_[i].addMouseListenerToHeaderInTable(table);
				table.setBackground(BACKGROUND);
				table.setForeground(FOREGROUND);
				TableColumn column = null;
				column = table.getColumnModel().getColumn(0);
				column.setPreferredWidth(150);
				column.setCellRenderer(new NameRenderer(i));
				for (int j=1; j<userEvents.getColumnCount(); j++) {
					column = table.getColumnModel().getColumn(j);
					column.setPreferredWidth(75);
					column.setCellRenderer(rightJustify_);
				}
				if (events_.length==1) { 
					super.getContentPane().add(new JScrollPane(table));
				}
				else {
					tabbedPane_.addTab(
							pes[i].toString(),
							new JScrollPane(table));
				}
			}
		} else {
			super.getContentPane().removeAll();
			super.getContentPane().add(new JLabel("<html><body><h1>ERROR: Can only load display when at most 100 PEs are loaded</h1></body></html>"));
		}
		super.getContentPane().invalidate();
		super.getContentPane().doLayout();
	}

	/** Constructor. */
	protected UserEventWindow(JCheckBox c) { 

		super("User Event Window");

		format_ = new DecimalFormat();
		format_.setGroupingUsed(true);

		rightJustify_.setHorizontalAlignment(JLabel.RIGHT);
		checkbox_ = c;
		setSize(480, 400);
		tabbedPane_ = new JTabbedPane();

		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(tabbedPane_, BorderLayout.CENTER);

		// define closing behavior
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) { 
				setVisible(false); 
				checkbox_.setSelected(false);
			}
		});
	}
}

