package projections.gui.count;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import projections.gui.*;

/** Joshua Mostkoff Unger
 *  Parallel Programming Laboratory
 *
 *  CounterFrame manages the input/output for the CounterTable data. */
public class CounterFrame extends JFrame
{
  TableSorter        sorter_      = null;
  JTable             jTable_      = null;
  CounterTable       cTable_      = new CounterTable();
  ProjectionsFileMgr fileMgr_     = null;
  JProgressBar       progress_    = new JProgressBar();
  JTabbedPane        tabbedPane_  = new JTabbedPane();
  
  /** Constructor. */
  public CounterFrame() {
    super("Performance Counter Analysis");
    // set up table and tabs

    sorter_ = new TableSorter(cTable_);
    jTable_ = new JTable(sorter_);
    jTable_.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    sorter_.addMouseListenerToHeaderInTable(jTable_);
    jTable_.setColumnSelectionAllowed(false);
    JPanel mainPanel = new JPanel(new BorderLayout());
    tabbedPane_.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
	cTable_.setSheet(tabbedPane_.getSelectedIndex(), jTable_);
	// sorter_.tableChanged(new TableModelEvent(sorter_));
	sortByColumn(1);
      }
    });
    mainPanel.add(tabbedPane_, BorderLayout.NORTH);
    mainPanel.add(new JScrollPane(jTable_), BorderLayout.CENTER);
    // set up bottom stuff
    JPanel panel = new JPanel(new FlowLayout());
    progress_.setBorderPainted(true);
    panel.add(progress_);
    JButton createGraph = (JButton) panel.add(new JButton("EP Proc Graph"));
    createGraph.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
	int[] selectedRows = jTable_.getSelectedRows();
	cTable_.createGraph(sorter_.mapRows(selectedRows));
      }
    });
    JButton calcMFlops = (JButton) panel.add(new JButton("Calc MFlops/s"));
    calcMFlops.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) { cTable_.calcMFlops(); }
    });
    JButton calcCacheMiss = 
      (JButton) panel.add(new JButton("Calc Cache Miss"));
    calcCacheMiss.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) { cTable_.calcCacheMiss(); }
    });
    JButton multiRunAnalysis = 
      (JButton) panel.add(new JButton("Multi Run Analysis"));
    multiRunAnalysis.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) { 
	int[] selectedRows = jTable_.getSelectedRows();
	cTable_.multiRunAnalysis(sorter_.mapRows(selectedRows)); 
      }
    });
    // create window
    super.getContentPane().setLayout(new BorderLayout());
    super.getContentPane().add(createMenu(), BorderLayout.NORTH);
    super.getContentPane().add(mainPanel, BorderLayout.CENTER);
    super.getContentPane().add(panel, BorderLayout.SOUTH);
    // define closing behavior
    super.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    super.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
      	//System.exit(0);
	setVisible(false);
      }
    });

  }

  /** Set the fileMgr */
  public void setFileMgr(ProjectionsFileMgr fileMgr) { fileMgr_ = fileMgr; }

  /** Tell the CounterTable to start loading its data. */
  public void loadFiles()
    throws Exception
  {

    if (fileMgr_ == null) {
      throw new Exception("ERROR in CounterFrame.loadFiles():\n"+
			  "      Must call setFileMgr before load files!\n");
    }
    FrameCallBack callback = new FrameCallBack(this);
    cTable_.loadFiles(fileMgr_, progress_, jTable_,callback);


    
  }

  public void sortByColumn(int index) {
    if (cTable_.getColumnCount() > index) {
      sorter_.sortByColumn(index, false);
    }
  }

  /** Create and return menu for the CounterFrame. */
  private JMenuBar createMenu() {
    // file
    JMenuBar menuBar = new JMenuBar();
    JMenu file = (JMenu) menuBar.add(new JMenu("File"));
    file.setMnemonic('f');
    file.setMenuLocation(0,0);
    // file-open
    JMenuItem menuItem = (JMenuItem) file.add(
      new JMenuItem("Open Simulation(s)"));
    menuItem.setMnemonic('o');
    menuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {

      }
    });
    // file-quit
    menuItem = (JMenuItem) file.add(new JMenuItem("Quit"));
    menuItem.setMnemonic('q');
    menuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
      //System.exit(0);
      setVisible(false);
      }
    });
    return menuBar;
  }
}
