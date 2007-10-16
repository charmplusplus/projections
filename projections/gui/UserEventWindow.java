package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import projections.analysis.UserEvent;
import java.text.DecimalFormat;
import projections.gui.count.TableSorter;
import projections.gui.FormattedNumber;

/** Joshua Mostkoff Unger
 *  Parallel Programming Laboratory
 * 
 *  UserEventWindow displays the UserEvents for the timeline 
 *  currently being viewed and manages their display.
 */
public class UserEventWindow extends JFrame
{
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
Color BACKGROUND = Color.black;
  private Color FOREGROUND = Color.white;

  // set the ints to have commas in appropriate places
  static DecimalFormat format_ = null;
  Checkbox             checkbox_;  // when closing, set to false
  UserEvent[][]        events_ = null;
  private JTabbedPane          tabbedPane_ = new JTabbedPane();
  TableSorter[]        sorter_ = null;

  private DefaultTableCellRenderer rightJustify_ = 
    new DefaultTableCellRenderer();

  private class NameRenderer extends DefaultTableCellRenderer {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int index_;
    public NameRenderer(int index) { index_ = index; }
    public Component getTableCellRendererComponent(
      JTable table, Object value, boolean selected, boolean focused, 
      int row, int column)
      {
	setHorizontalAlignment(JLabel.CENTER);
	setEnabled(table == null || table.isEnabled()); // see question above
	setForeground(events_[index_][sorter_[index_].mapRow(row)].color);
	setBackground(BACKGROUND);
        super.getTableCellRendererComponent(
	  table, value, selected, focused, row, column);
	return this;
      }
    }

  private class UserEventTable extends AbstractTableModel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int index_ = -1;

    public UserEventTable(int i) { index_ = i; }
    public int getColumnCount() { return 4; }
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
      default: return "ERROR";
      }
    }
    public Object getValueAt(int row, int col) { 
      if (events_ != null && events_[index_] != null) {
	switch (col) {
	  case 0:  
	    return events_[index_][row].Name;
	  case 1:  return new FormattedNumber((int)events_[index_][row].BeginTime, format_);
	  case 2:  return new FormattedNumber((int)events_[index_][row].EndTime, format_);
	  case 3:  return new FormattedNumber((int)events_[index_][row].EndTime-
					      (int)events_[index_][row].BeginTime, format_);
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
  
  public void setData(TimelineData data) { 
    events_ = data.userEventsArray;
    // create the layout here
    data.processorList.reset();
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
	  new Integer(data.processorList.nextElement()).toString(),
	  new JScrollPane(table));
      }
    }
    super.getContentPane().invalidate();
    super.getContentPane().doLayout();
  }

  /** Constructor. */
  public UserEventWindow(Checkbox c) { 
    super("User Event Window");
    if (format_ == null) {
      format_ = new DecimalFormat();
      format_.setGroupingUsed(true);
    }
    rightJustify_.setHorizontalAlignment(JLabel.RIGHT);
    checkbox_ = c;
    super.setSize(480, 400);
    tabbedPane_ = new JTabbedPane();
    // define closing behavior
    super.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    super.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { 
	setVisible(false); 
	checkbox_.setState(false);
      }
    });
  }
}

