package projections.gui.count;

import java.io.*;
import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.*;

import projections.analysis.*;
import projections.gui.*;
import projections.gui.graph.*;

/** Joshua Mostkoff Unger, unger1@uiuc.edu
 *  Parallel Programming Laboratory
 * 
 *  CounterTable displays the performance counter data in a spreadsheet-like
 *  format.
 *  */
public class CounterTable extends AbstractTableModel 
{
  // ***********************************************************************
  // NOTES/TODO
  // ***********************************************************************

  // WHAT OTHER CHECKS SHOULD I DO TO MAKE SURE OUTPUTS MATCH?
  // MAKE ILLUSTRATION OF CLASS DATA MEMBER RELATIONSHIPS!!!
  // USE PROGRESS BAR!!!
  // CLOSE READERS!!! (AND IN OTHER FILES)
  // CHECK VALIDITY OF SUM/MAX/MIN/STDEV
  // DIALOG BOXES FOR SYSTEM.OUT.PRINTLN()
  // BOLDFACE TOTAL, MAKE STATIC?
  // CHANGE VARIABLE NAMES TO REFLECT NEW MEANING
  // ADD PERCENTAGE OF TIME!!!
  // MOVE SHEET TO NEW FILE

  // ***********************************************************************
  // LINKS
  // ***********************************************************************

  // http://java.sun.com/products/jdk/1.1/docs/api/packages.html
  // http://java.sun.com/products/jfc/swingdoc-api-1.1/index.html
  // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
  // http://java.sun.com/docs/books/tutorial/uiswing/misc/border.html
  // http://java.sun.com/docs/books/tutorial/2d/index.html

  // ***********************************************************************
  // PUBLIC METHODS
  // ***********************************************************************

  /** Constructor. */
  public CounterTable() { 
    leftJustify_.setHorizontalAlignment(JLabel.LEFT);
    rightJustify_.setHorizontalAlignment(JLabel.RIGHT);
    intFormat_.setGroupingUsed(true);
    doubleFormat_.setGroupingUsed(true);
    doubleFormat_.setMinimumFractionDigits(2);
    doubleFormat_.setMaximumFractionDigits(2);
  }

  /** Override AbstractTableModel, return current sheet's numCols. */
  public int getColumnCount() { 
    if (currSheet_ != null) { return currSheet_.numCols; } 
    else { return 0; }
  }

  /** Override AbstractTableModel, return current sheet's numRows. */
  public int getRowCount() {
    if (currSheet_ != null) { return currSheet_.numRows; }
    else { return 0; }
  }

  /** Override AbstractTableModel, return string for header of table. */
  public String getColumnName(int columnIndex) {
    if (currSheet_ != null) {
      if (columnIndex == 0) { return "Entry Point"; }
      else {
	int counter = (columnIndex-1) / EPValue.NUM_VALUES_PER_COUNTER;
	String code = currSheet_.counters[counter].counterCode;
	int pos = (columnIndex-1) % EPValue.NUM_VALUES_PER_COUNTER;
	switch (pos) {
	  case EPValue.AVG_CALLED: return EPValue.AVG_CALLED_STR+":"+code;
	  case EPValue.AVG_TIME:   return EPValue.AVG_TIME_STR+":"+code;
	  case EPValue.AVG_CVAL:   return code+":"+EPValue.AVG_CVAL_STR;
	  case EPValue.AVG_CSTDEV: return code+":"+EPValue.AVG_CSTDEV_STR;
	  case EPValue.MAX_CVAL:   return code+":"+EPValue.MAX_CVAL_STR;
	  case EPValue.MIN_CVAL:   return code+":"+EPValue.MIN_CVAL_STR;
	  default: return "ERROR";
	}
      }
    }
    return new Integer(columnIndex).toString(); 
  }

  /** Override AbstractTableModel, return class so know how to format screen */
  public Class getColumnClass(int columnIndex) {
    if (currSheet_ != null) {
      if (columnIndex == 0) { return java.lang.String.class; }
      else { return FormattedNumber.class; }
    }
    return java.lang.String.class;
  }

  /** Override AbstractTableModel, return value for display. */
  public Object getValueAt(int row, int col) { 
    if (currSheet_ != null) {
      if (col == 0) { return currSheet_.tableRows[row].name; }
      else {
	int counter = (col-1) / EPValue.NUM_VALUES_PER_COUNTER;
	CounterSummary s = currSheet_.tableRows[row].summary[counter];
	int pos = (col-1) % EPValue.NUM_VALUES_PER_COUNTER;
	switch (pos) {
	  case EPValue.AVG_CALLED: 
	    return new FormattedNumber(s.numCalled, doubleFormat_);
	  case EPValue.AVG_TIME:   
	    return new FormattedNumber(s.totTime, doubleFormat_);
	  case EPValue.AVG_CVAL:   
	    return new FormattedNumber(s.avgCount, doubleFormat_);
	  case EPValue.AVG_CSTDEV:
	    return new FormattedNumber(s.stdevCount, doubleFormat_);
	  case EPValue.MAX_CVAL:
	    return new FormattedNumber(s.maxCount, intFormat_);    
	  case EPValue.MIN_CVAL:   
	    return new FormattedNumber(s.minCount, intFormat_);    
	  default: return new Integer(-1);
	}
      }
    }
    else { return new Integer(-1); }
  }

  /** Return number of simulations (note not files) to display. */
  public int getNumSims() { return sheet_.size(); }

  /** Return name of simulation at index. */
  public String getRunName(int index) {
    Sheet sheet = (Sheet) sheet_.elementAt(index);
    if (sheet.merged) { return sheet.sheetName; }
    else { return getRunName(sheet.sheetName); }
  }

  /** Return tool tip for simulation at index. */
  public String getToolTip(int index) {
    try {
      Sheet sheet = (Sheet) sheet_.elementAt(index);
      return sheet.sheetName;
    }
    catch (Exception e) { ProjectionsFileChooser.handleException(null, e); }
    return null;
  }

  /** Given file manager, load files and update progress bar. */

    public FrameCallBack callback;
    public void loadFiles(
    ProjectionsFileMgr fileMgr, JProgressBar progress, JTable table, CallBack callback)
	throws IOException, Exception
    {
	this.callback = (FrameCallBack ) callback;
	loadFiles(fileMgr,progress,table);
    }




  public JTable table;
  public void loadFiles(
    ProjectionsFileMgr fileMgr, JProgressBar progress, JTable table)
    throws IOException, Exception
  {
    this.table = table;
    int i, j;
    int numFiles = fileMgr.getNumFiles();
    sheet_ = new Vector(fileMgr.getNumFiles());
    Sheet firstSheet = null;
    for (i=0; i<numFiles; i++) {
      Sheet sheet = new Sheet(fileMgr, i);
      sheet_.addElement(sheet);
      if (i == 0) { firstSheet = sheet; }
      else {
	if (!sheet.sameEPs(sheet)) {
	  throw new IOException("sts1: "+fileMgr.getStsFile(0)+"\n"+
				"sts2: "+fileMgr.getStsFile(i)+"\n");
	}
      }
    }

    // search through and try to find similar files
    // use a Vector of Vectors of Files to group similar files
    Vector similarCollection = new Vector();
    boolean[] inSimilar = new boolean[numFiles];
    for (i=0; i<numFiles; i++) { inSimilar[i] = false; }
    for (i=0; i<numFiles; i++) {
      if (!inSimilar[i]) {
	inSimilar[i] = true;
	Sheet searchSheet = (Sheet) sheet_.elementAt(i);
	Vector similar = new Vector();
	similar.add(searchSheet);
	similarCollection.add(similar);
	j=i+1;
	while (j<numFiles) {
	  // yes, this is a n^2 algo.  it will never need to scale.
	  if (!inSimilar[j]) {
	    Sheet sheet = (Sheet) sheet_.elementAt(j);
	    if (sheet.similarTo(searchSheet)) {
	      similar.add(sheet);
	      inSimilar[j] = true;
	    }
	  }
	  j++;
	}
      }
    }
    if (removeSingleEntries(similarCollection)) {
      userMergeSimilar(similarCollection);
    }else{
    	//System.out.println("No similar files ");
    	sortSheets();  // sorts based on numPEs
    	setSheet(0, table);
	callback.callBack();
    }
  }

  /** Return the list of counters and their colors for this sheet. */
  public Component getCounterPanel(int index) {
    Sheet sheet = (Sheet) sheet_.elementAt(index);
    if (sheet_ == null || sheet == null) { return null; }
    CounterListTable clTable = new CounterListTable(sheet.counters);
    JTable table = new JTable(clTable);
    table.setRowSelectionAllowed(false);
    clTable.configure(table);
    JPanel panel = new JPanel();
    panel.add(new JScrollPane(table));
    Dimension d = table.getPreferredSize();
    int numCounters = sheet.counters.length;
    // adjust because table doesn't want to display header
    d.height = Math.min(100, (d.height / numCounters ) * (numCounters + 1));
    panel.setPreferredSize(d);
    return panel;
  }

  /** Set the current display spreadsheet to the index. */
  public void setSheet(int index, JTable table) {
    // save sorted column here, also set columns correctly
    currSheet_ = (Sheet) sheet_.elementAt(index);
    super.fireTableStructureChanged();
    TableColumn column = null;
    column = table.getColumnModel().getColumn(0);
    column.setCellRenderer(leftJustify_);
    column.setPreferredWidth(250);
    for (int i=1; i<currSheet_.numCols; i++) {
      column = table.getColumnModel().getColumn(i);
      column.setCellRenderer(rightJustify_);
      column.setPreferredWidth(110);
      column.setHeaderRenderer(getCounterHeader(i));
    }
    // table.tableChanged(new TableModelEvent(
    // this, 0, currSheet_.numRows-1, TableModelEvent.ALL_COLUMNS));
  }

  /** Pop up a window that shows the stats across processors for this sim */
  public void createGraph(int[] selectedRows) {
    if (selectedRows.length == 0) {
      System.out.println("ERROR there are no selected rows");
    }
    else if (currSheet_ == null) { System.out.println("NO DATA IN TABLE"); }
    else {
      // allocate new info and sort it and pass it into the graph class
      int counterNum = 0;
      Sheet s = currSheet_;
      double[][] data = new double[s.numProcs][];
      for (int i=0; i<s.numProcs; i++) {
	data[i] = new double[selectedRows.length];
	for (int j=0; j<selectedRows.length; j++) {
	  DoubleCountData dData = (DoubleCountData) 
	    s.data[i].cResults[counterNum].countData[CountData.AVG_COUNT];
	  data[i][j] = dData.getValue(selectedRows[j]);
	}
      }
      DataSource2D source = new DataSource2D(currSheet_.sheetName, data);
      XAxis xAxis = new XAxisFixed("Processor", "#");
      YAxis yAxis = 
	new YAxisAuto(currSheet_.counters[0].counterCode, "#", source);
      Graph g = new Graph();
      g.setGraphType(Graph.LINE);
      g.setData(source, xAxis, yAxis);
      JFrame f = new JFrame();
      f.getContentPane().add(g);
      f.setSize(800,640);
      f.setVisible(true);
    }
  }

  /* Pop up a window displaying MFlops for all EPs and their significance. */
  public void calcMFlops() {
    if (currSheet_ == null) { System.out.println("NO DATA IN TABLE"); }
    else {
      int i;
      Sheet s = currSheet_;
      // find the correct counter to use
      boolean found = false;
      int counterIndex = -1;
//      System.out.println("s.counters.length "+s.counters.length);
      for (i=0; i<s.counters.length && !found; i++) {
	if (s.counters[i].counterCode.equals("GR_FLOPS")) {
	  found = true;
	  counterIndex = i;
	}
      }
      if (!found) { System.out.println("COULD NOT FIND FLOPS!!!");  return; }
      // loop through EPs and find out which ones have been called (non-null)
      Vector vector = new Vector();
      for (i=0; i<s.tableRows.length-1; i++) {
	if (s.tableRows[i].summary[counterIndex].numCalled > 0) {
	  vector.addElement(new Integer(i));
	}
      }
      int numEPs = vector.size();
      int[] indices = new int[numEPs];
      for (i=0; i<numEPs; i++) {
	indices[i] = ((Integer) vector.elementAt(i)).intValue();
      }
      vector.removeAllElements();
      // now have correct indices for non-zero flops, so loop through 
      // and calc total flops, prepare data for graph
      double[][] data = new double[numEPs][];
      double totalFlops = 0;
      double totalCalled = 0;
      for (i=0; i<numEPs; i++) {
	CounterSummary summary = s.tableRows[indices[i]].summary[counterIndex];
	data[i] = new double[2];
	data[i][0] = 
	  summary.numCalled*summary.avgCount*s.numProcs/summary.totTime*1e6;
	totalFlops += data[i][0]*summary.numCalled;
	totalCalled += summary.numCalled;
      }
      double calcMFlops = totalFlops / totalCalled;
      for (i=0; i<numEPs; i++) { data[i][1] = calcMFlops; }
      // set up graph
      String[] xAxisLabels = new String[numEPs];
      for (i=0; i<numEPs; i++) { 
	xAxisLabels[i] = (new Integer(indices[i])).toString();
      }
      DataSource2D source = new DataSource2D(
	currSheet_.sheetName+": Overall Flops/s = "+calcMFlops, data);
      XAxis xAxis = new MultiRunXAxis(xAxisLabels);
      YAxis yAxis = new YAxisAuto("Performance", "Flops/s", source);
      Graph g = new Graph();
      g.setGraphType(Graph.LINE);
      g.setData(source, xAxis, yAxis);
      JFrame f = new JFrame();
      f.getContentPane().add(g);
      f.setSize(800,640);
      f.setVisible(true);
    }
  }

  /* Pop up a window displaying MFlops for all EPs and their significance. */
  public void calcCacheMiss() {
    if (currSheet_ == null) { System.out.println("NO DATA IN TABLE"); }
    else {
      int i;
      Sheet s = currSheet_;
      // find the correct counters to use
      boolean foundLoad = false;   int indexLoad = -1;
      boolean foundStore = false;  int indexStore = -1;
      boolean foundL1 = false;     int indexL1 = -1;
      boolean foundL2 = false;     int indexL2 = -1;
      boolean foundTLB = false;    int indexTLB = -1;
      for (i=0; i<s.counters.length; i++) {
	String c = s.counters[i].counterCode;
	System.out.println("  Counter "+i+": "+c);
	if      (c.equals("LOAD"))     { foundLoad  = true;  indexLoad  = i;  }
	else if (c.equals("STORE"))    { foundStore = true;  indexStore = i; }
	else if (c.equals("L1_DMISS")) { foundL1    = true;  indexL1    = i; }
	else if (c.equals("L2_DMISS")) { foundL2    = true;  indexL2    = i; }
	else if (c.equals("TLB_MISS")) { foundTLB   = true;  indexTLB   = i; }
      }
      if (!(foundLoad && foundStore)) { 
	System.out.println("COULD NOT FIND LOADS/STORES!!!");  return; 
      }
      System.out.println(
	"indexL1 "+indexL1+" indexL2 "+indexL2+" indexTLB "+indexTLB);
      // loop through EPs and find out which ones have been called (non-null)
      Vector vector = new Vector();
      for (i=0; i<s.tableRows.length-1; i++) {
	if (s.tableRows[i].summary[indexLoad].numCalled > 0 &&
	    s.tableRows[i].summary[indexStore].numCalled > 0) 
	{
	  vector.addElement(new Integer(i));
	}
      }
      int numEPs = vector.size();
      int[] indices = new int[numEPs];
      for (i=0; i<numEPs; i++) {
	indices[i] = ((Integer) vector.elementAt(i)).intValue();
      }
      vector.removeAllElements();
      // figure out how many datasets there will be and set up for function
      int numSets = 0;
      int[] index = new int[3];
      if (foundL1)  { index[numSets] = indexL1;   numSets++; }
      if (foundL2)  { index[numSets] = indexL2;   numSets++; }
      if (foundTLB) { index[numSets] = indexTLB;  numSets++; }
      System.out.println("THERE ARE "+numSets+" SETS");
      // now have correct indices for non-zero flops, so loop through 
      // and calc total flops, prepare data for graph
      double[][] data = new double[numEPs][];
      for (i=0; i<numEPs; i++) { data[i] = new double[numSets*2]; }
      for (i=0; i<numSets; i++) {
	fillCacheData(data, indices, indexLoad, indexStore, index[i], i);
      }
      // set up graph
      String[] xAxisLabels = new String[numEPs];
      for (i=0; i<numEPs; i++) { 
	xAxisLabels[i] = (new Integer(indices[i])).toString();
      }
      String title = currSheet_.sheetName+":";
      numSets = 0;
      if (foundL1)  { 
	title = title.concat(" L1 Miss "+data[0][numSets*2+1]);  numSets++; 
      }
      if (foundL2)  { 
	title = title.concat(" L2 Miss "+data[0][numSets*2+1]);  numSets++;
      }
      if (foundTLB) { 
	title = title.concat(" TLB Miss "+data[0][numSets*2+1]);  numSets++; 
      }
      DataSource2D source = new DataSource2D(title, data);
      XAxis xAxis = new MultiRunXAxis(xAxisLabels);
      YAxis yAxis = new YAxisAuto("Cache Miss Rate", "%", source);
      Graph g = new Graph();
      g.setGraphType(Graph.LINE);
      g.setData(source, xAxis, yAxis);
      JFrame f = new JFrame();
      f.getContentPane().add(g);
      f.setSize(800,640);
      f.setVisible(true);
    }
  }

  /** Pop up a window that shows the stats across processors for this sim */
  public void multiRunAnalysis(int[] selectedRows) {
    if (selectedRows.length == 0) {
      System.out.println("ERROR there are no selected rows");
    }
    else {
      int i, j;
      // get counterIndex for GR_FLOPS
      // find the correct counter to use
      int[] counterIndex = new int[sheet_.size()];
      for (i=0; i<counterIndex.length; i++) {
	Sheet s = (Sheet) sheet_.elementAt(i);
	boolean found = false;
	
	for (j=0; j<s.counters.length && !found; j++) {
	  if (s.counters[j].counterCode.equals("GR_FLOPS")) {
	    found = true;
	    counterIndex[i] = j;
	  }
	}
	if (!found) { System.out.println("COULD NOT FIND FLOPS!!!");  return; }
      }
      // allocate new info and sort it and pass it into the graph class
      double[][] data = new double[sheet_.size()][];
      for (i=0; i<sheet_.size(); i++) {
	data[i] = new double[selectedRows.length];
	Sheet s = (Sheet) sheet_.elementAt(i);
	for (j=0; j<selectedRows.length; j++) {
	  data[i][j] =
	    s.tableRows[selectedRows[j]].summary[counterIndex[i]].avgCount;
	}
      }
      // set up graph
      String[] xAxisLabels = new String[sheet_.size()];
      for (i=0; i<sheet_.size(); i++) {
	xAxisLabels[i] = ((Sheet) sheet_.elementAt(i)).sheetName;
      }
      DataSource2D source = new DataSource2D("EPs Across Procs", data);
      XAxis xAxis = new MultiRunXAxis(xAxisLabels);
      YAxis yAxis = 
	new YAxisAuto("GR_FLOPS", "#", source);
      Graph g = new Graph();
      g.setGraphType(Graph.LINE);
      g.setData(source, xAxis, yAxis);
      JFrame f = new JFrame();
      f.getContentPane().add(g);
      f.setSize(800,640);
      f.setVisible(true);
    }
  }

  // ***********************************************************************
  // PRIVATE VARIABLES
  // ***********************************************************************

  // Vector of sheets (for each simulation)
  private Vector sheet_     = null;
  // pointer to currently selected sheet
  private Sheet  currSheet_ = null;
  // renderer to left justify object in cell (does not affect not header)
  private DefaultTableCellRenderer leftJustify_ = 
    new DefaultTableCellRenderer();
  // renderer to right justify object in cell (does not affect header)
  private DefaultTableCellRenderer rightJustify_ = 
    new DefaultTableCellRenderer();
  // set the ints to have commas in appropriate places
  private DecimalFormat intFormat_ = new DecimalFormat();
  // set the double to have commas and same sig figs
  private DecimalFormat doubleFormat_ = new DecimalFormat();
  // for waiting until the user chooses something
  private Wait wait_ = new Wait(false);
  // carries results of merge window, true if merge selected picked
  private boolean merge_;

  // ***********************************************************************
  // PRIVATE METHODS
  // ***********************************************************************

  /** Searches through the counter results and calculates cache miss ratio. */
  private void fillCacheData
  (
    double[][] data,    // the data that will eventually be graphed
    int[]      iEPs,    // index of candidate EPs
    int        iLoad,   // index of load counter
    int        iStore,  // index of store counter
    int        iCache,  // index of cache counter
    int        offset   // where to put in <data>, multiply by two
  )
  {
    int i;
    int ep;
    Sheet s = currSheet_;
    for (i=0; i<iEPs.length; i++) {
      ep = iEPs[i];
      CounterSummary sLoad  = s.tableRows[ep].summary[iLoad];
      CounterSummary sStore = s.tableRows[ep].summary[iStore];
      CounterSummary sCache = s.tableRows[ep].summary[iCache];
      double loadPerTime = sLoad.numCalled*sLoad.avgCount / sLoad.totTime;
      double storePerTime = sStore.numCalled*sStore.avgCount / sStore.totTime;
      double cachePerTime = sCache.numCalled*sCache.avgCount / sCache.totTime;
      double cacheMiss = 100.0 * cachePerTime / (loadPerTime + storePerTime);
      data[i][offset*2] = cacheMiss;
    }
    // calculate weighted average for across all EPs
    double total = 0.0;
    double totalTime = 0.0;
    for (i=0; i<iEPs.length; i++) {
      ep = iEPs[i];
      CounterSummary sCache = s.tableRows[ep].summary[iCache];
      total += sCache.totTime * data[i][offset*2];
      totalTime += sCache.totTime;
    }
    double overallCacheMiss = total/totalTime;
    System.out.println("CACHE MISS "+overallCacheMiss);
    for (i=0; i<iEPs.length; i++) { data[i][offset*2+1] = overallCacheMiss; }
  }

  /** Sorts the sheets so that lowest numPEs is first.
   *  Yes, this is bubblesort.
   *  Yes, bubblesort is very slow. */
  private void sortSheets() {
    Sheet sheet1, sheet2;
    for (int i=sheet_.size()-1; i>=0; i--) {
      for (int j=0; j<i; j++) {
	sheet1 = (Sheet) sheet_.elementAt(j);
	sheet2 = (Sheet) sheet_.elementAt(j+1);
	if (sheet1.data.length > sheet2.data.length) {
	  sheet_.setElementAt(sheet2, j);
	  sheet_.setElementAt(sheet1, j+1);
	}
      }
    }
  }

  /** Let the user choose if they want to merge similar files.
   *  After this function called, Sheet. may be rearranged.
   *  similarCollection is Vector of Vector of Sheet. */


   public Vector similarCollection;
   public JList[]  list;
   public JTextArea[] names;

  private void userMergeSimilar(Vector similarCollection) throws Exception {
    int numGroups = similarCollection.size();
    this.similarCollection = similarCollection;
    list = new JList[numGroups];  // each groups list (user selects)
    names = new JTextArea[numGroups];  // name of new group
    JPanel listPanel = createListPanel(similarCollection, list, names);
    callback.f.setVisible(false);
    JDialog dialog = createMergeDialog(listPanel, numGroups);

    // when the dialog box appears the main frame should disappear


    // pop up dialog
    //wait_.setValue(true);
    dialog.setVisible(true);
    //Thread th = new Thread() { public void run() { wait_.waitFor(false); } };
    //th.run();
    //dialog.setVisible(false);

  }

  /** Go through the similar Collection and construct a panel for each
   *  one allowing the user to select files that should be merged
   *  and add constructed panel to the listPanel */
  private JPanel createListPanel(
    Vector similarCollection, JList[] list, JTextArea[] names)
  {
    int i, j;
    Sheet sheet = null;
    Vector similar = null;
    int numGroups = similarCollection.size();

    JPanel listPanel = new JPanel(new GridLayout(numGroups, 1));
    for (i=0; i<numGroups; i++) {
      similar = (Vector) similarCollection.elementAt(i);
      String[] similarFileNames = new String[similar.size()];
      int[] selectAll = new int[similar.size()];
      JPanel panel = new JPanel(new BorderLayout());
      for (j=0; j<similar.size(); j++) {
	sheet = (Sheet) similar.elementAt(j);
	selectAll[j] = j;
	similarFileNames[j] = sheet.sheetName;
      }
      list[i] = new JList(similarFileNames);
      list[i].setSelectedIndices(selectAll);
      names[i] = new JTextArea((new Integer(sheet.numProcs)).toString());
      JPanel topPanel = new JPanel(new BorderLayout());
      JLabel label = new JLabel("numPE="+sheet.numProcs+", group name=");
      label.setPreferredSize(label.getMinimumSize());
      topPanel.add(label, BorderLayout.WEST);
      topPanel.add(names[i], BorderLayout.CENTER);
      panel.add(topPanel, BorderLayout.NORTH);
      panel.add(new JScrollPane(list[i]), BorderLayout.CENTER);
      panel.setBorder(BorderFactory.createEmptyBorder(15, 5, 15, 5));
      listPanel.add(panel);
    }
    return listPanel;
  }

  /** Construct the merge dialog and return it. */
  public JDialog dialog;
  private JDialog createMergeDialog(JPanel listPanel, int numPanels) {
    JTextArea instructions = new JTextArea(
      "Projections has found several performance analysis runs\n"+
      "  that were created using the same number of processors.\n"+
      "Should runs on the same number of processors be merged into\n"+
      "  one analysis window?  (Recommended to compare several counters\n"+
      "  as an effective single run.)\n"+
      "Select the runs to be merged and change the group name (the\n"+
      "  name that the merged run will be labeled) if desired.\n"+
      "Press \"Merge Selected\" when ready, or press\n"+
      "  \"Don't Merge Any\" if desired.\n"+
      "Use \"Control-Click\" to deselect single selections.\n"+
      "\n"+
      "NOTE: To make more than one group out of a collection,\n"+
      "  simply make a group out of some of its runs, and this\n"+
      "  dialog will pop-up again allowing further merges.\n");
    instructions.setEditable(false);

    // create dialog so user can choose
    dialog = new JDialog();
    dialog.setTitle(
      "Projections: Performance Counter Analysis (Merge Similar Files)");
    dialog.setSize(600, 240 + 125*numPanels);
    dialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
      	//System.exit(0);
      }
    });
    dialog.getContentPane().setLayout(new BorderLayout());
    dialog.getContentPane().add(instructions, BorderLayout.NORTH);
    dialog.getContentPane().add(listPanel, BorderLayout.CENTER);

    WaitButton no = new WaitButton("Don't Merge Any", wait_);
    WaitButton yes = new WaitButton("Merge Selected", wait_);
    no.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) { merge_ = false;
      sortSheets();  // sorts based on numPEs
    	setSheet(0, table);
	callback.callBack();
	dialog.setVisible(false);
     }
    });
    yes.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	int i, j;
    	Vector similar;
    	Sheet sheet;
      	merge_ = true;
	try{
	if (!merge_) { return; }
    	else {
      	// merge selected and recursively call this function
      	System.out.println("Merging files");
      	i=0;
      	while (i<similarCollection.size()) {
		similar = (Vector) similarCollection.elementAt(i);
		int[] selected = list[i].getSelectedIndices();
		if (selected.length == 0) { similarCollection.removeElement(similar); }
		else {
	  	// loop through the sheets and remove the selected files,
	  	// then create a new sheet out of them and add to the sheet vector
	  		Sheet[] toMerge = new Sheet[selected.length];
	  		for (j=0; j<selected.length; j++) {
	    			sheet = (Sheet) similar.elementAt(selected[j]);
				toMerge[j] = sheet;
	    			sheet_.removeElement(sheet);
	  		}
	  		for (j=0; j<toMerge.length; j++) {
	    			similar.removeElement(toMerge[j]);
	  		}
	  		sheet_.add(new Sheet(toMerge, names[i].getText()));
	  		i++;
		}
      	}
      // search through the similarCollection to find out if any similar stuff
      // actually exists.  if not return, otherwise call this function again
      	if (removeSingleEntries(similarCollection)) {
		userMergeSimilar(similarCollection);
      	}
	sortSheets();  // sorts based on numPEs
    	setSheet(0, table);
	callback.callBack();
	dialog.setVisible(false);

   	}
	}catch (Exception ex){ex.printStackTrace();}
	}
    });
    Panel buttons = new Panel(new FlowLayout());
    buttons.add(no, BorderLayout.WEST);
    buttons.add(yes, BorderLayout.EAST);
    dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
    return dialog;
  }

  /** Search through Vector of Vector of Sheets in similarCollection,
   *  and remove if 2nd Vector only has one Sheet.
   *  Return true if there are any similar entries in the collection,
   *  false otherwise. */
  private boolean removeSingleEntries(Vector similarCollection) {
    int i=0;
    boolean hasSimilar = false;
    while (i<similarCollection.size()) {
      Vector similar = (Vector) similarCollection.elementAt(i);
      // if only one file in this group, remove from consideration
      if (similar.size() <= 1) { similarCollection.removeElement(similar); }
      else { i++;  hasSimilar = true; }
    }
    return hasSimilar;
  }

  /** Return name of simulation given filename. */
  private String getRunName(String fileName) {
    try {
      File file = new File(fileName);
      String parentStr = file.getParent();
      File parent = new File(parentStr);
      String parentParentStr = parent.getParent();
      String retVal = "["+
	parentStr.substring(parentParentStr.length()+1, parentStr.length())+
	"]"+getStartString(file.getName());
      return retVal;
    }
    catch (Exception e) { ProjectionsFileChooser.handleException(null, e); }
    return null;
  }

  /** Return start string of name, assuming name in format of:
   *  startStr.count.sts. */
  private String getStartString(String name) throws IOException {
    int lastDotIndex = name.lastIndexOf(".");
    int nextDotIndex = name.lastIndexOf(".", lastDotIndex-1);
    if (!name.endsWith(".count") && !name.endsWith(".sts")) {
      throw new IOException(
	"Expect count files to end with .count or .sts:\n"+name);
    }
    // given that the sts has name like: "namd2.count.sts", find
    // all files of the name "namd2.x.count"
    String retVal = name.substring(0, nextDotIndex);
    return retVal;
  }

  /** Return header renderer for column at index col. */
  private ColorHeader getCounterHeader(int col) {
    if (currSheet_ != null) {
      if (col == 0) { return new ColorHeader(null, null); }
      else {
	int counter = (col-1) / EPValue.NUM_VALUES_PER_COUNTER;
	int pos = (col-1) % EPValue.NUM_VALUES_PER_COUNTER;
	String mod = null;
	switch (pos) {
	  case EPValue.AVG_CALLED: mod = EPValue.AVG_CALLED_STR;  break;
	  case EPValue.AVG_TIME:   mod = EPValue.AVG_TIME_STR;    break;
	  case EPValue.AVG_CVAL:   mod = EPValue.AVG_CVAL_STR;    break;
	  case EPValue.AVG_CSTDEV: mod = EPValue.AVG_CSTDEV_STR;  break;
	  case EPValue.MAX_CVAL:   mod = EPValue.MAX_CVAL_STR;    break;
	  case EPValue.MIN_CVAL:   mod = EPValue.MIN_CVAL_STR;    break;
	  default: mod = "ERROR";  break;
	}
	return new ColorHeader(
	  currSheet_.counters[counter].color,
	  currSheet_.counters[counter].counterCode+": "+mod);
      }
    }   
    else { return new ColorHeader(null, null); }
  } 

  // ***********************************************************************
  // PRIVATE CLASSES 
  // ***********************************************************************

  /** Each sheet represents a simulation's data. */
  private class Sheet {
    public int numCols  = 0;  // number of data columns in simulation 
    public int numRows  = 0;  // number of EPs in simulation
    public int numProcs = 0;  // number of processors for this simulation

    public String           sheetName = null;  // store name of this sheet
    public Counter[]        counters  = null;  // stores name/description
    public EPValue[]        tableRows = null;  // store row data
    public LogData[]        data      = null;  // to store data for log files

    public boolean          merged = false;    // true if composed of merger

    /** Constructor, read from file. */
    public Sheet(ProjectionsFileMgr fileMgr, int index)
      throws IOException
    {
      int i, j;

      sheetName = fileMgr.getStsFile(index).getCanonicalPath();
      // read sts file
      StsReader stsReader = 
	  new StsReader(fileMgr.getStsFile(index).getCanonicalPath());
      numProcs = stsReader.getProcessorCount();
      // read log file data
      File[] logFiles = fileMgr.getLogFiles(index);
      if (numProcs != logFiles.length) {
	throw new IOException("Sheet() "+sheetName+" expect numProcs "+
			    logFiles.length+" got "+numProcs);
      }
      data = new LogData[logFiles.length];
      boolean first = true;
      int numEP = -1;
      for (i=0; i<logFiles.length; i++) {
	int realIndex = getProcIndex(logFiles[i], i);
	data[realIndex] = new LogData(logFiles[i], i, this, first, sheetName);
	if (first) { numEP = data[realIndex].numEPs; }
	else {
	  if (numEP != data[realIndex].numEPs) {
	    throw new IOException("Sheet() "+sheetName+" expect numEPs "+numEP+
	      " proc "+realIndex+" has "+data[realIndex].numEPs);
	  }
	}
	first = false;
      }
      // now initialize the EPValue by summarizing over all processors
      tableRows = new EPValue[stsReader.getEntryCount()+1];
      numRows = stsReader.getEntryCount()+1;
      for (i=0; i<numRows-1; i++) {
	tableRows[i] = new EPValue(
	  stsReader.getEntryNames()[i][0], i, data, counters.length, true);
      }
      calcEPValueTotals();  // total each column in last row
    }

    /** Constructor.  Combine the data in the sheets array. */
    public Sheet(Sheet[] sheets, String name) throws Exception {
      merged = true;
      if (sheets.length == 1) {
	numCols   = sheets[0].numCols;
	numRows   = sheets[0].numRows;
	numProcs  = sheets[0].numProcs;
	counters  = sheets[0].counters;
	data      = sheets[0].data;
	tableRows = sheets[0].tableRows;
	
	sheetName = name;
	return;
      }

      int i, j, k;
      numCols = 1;
      numRows = sheets[0].numRows;
      numProcs = sheets[0].numProcs;
      sheetName = name;
      
      // calculate memory requirements
      int numCounters = 0;
      for (i=0; i<sheets.length; i++) { 
	numCounters += sheets[i].counters.length;
	if (sheets[i].numRows != numRows || sheets[i].numProcs != numProcs) {
	  throw new Exception(
	    "Sheet(Sheet[] sheets) expect "+numRows+" rows "+numProcs+
	    " procs\n"+"  ["+sheets[i].sheetName+"] has "+sheets[i].numRows+
	    " rows "+sheets[i].numProcs+" procs\n");
	}
      }
      counters = new Counter[numCounters];
      tableRows = new EPValue[numRows];
      data = new LogData[numProcs];
      for (i=0; i<data.length; i++) {
	data[i] = new LogData(
	  i, data.length, sheets[0].data[0].numEPs, numCounters);
      }

      // loop through and copy counters into new arrays
      int counterIndex = 0;
      for (i=0; i<sheets.length; i++) {
	for (j=0; j<sheets[i].counters.length; j++) {
	  counters[counterIndex] = sheets[i].counters[j];
	  counterIndex++;
	}
      }
      // loop through and copy log data
      for (i=0; i<numProcs; i++) {
	counterIndex = 0;
	for (j=0; j<sheets.length; j++) {
	  for (k=0; k<sheets[j].counters.length; k++) {
	    data[i].cResults[counterIndex] = sheets[j].data[i].cResults[k];
	    counterIndex++;
	  }
	}
      }
      // sum up new data into EPValues
      for (i=0; i<numRows-1; i++) {
	tableRows[i] = new EPValue(
	  sheets[0].tableRows[i].name, i, data, counters.length, false);
      }
      calcEPValueTotals();  // total each column in last row
    }

    /** Return true if the two sheets have same num of EPs and num Procs. */
    public boolean similarTo(Sheet sheet) {
      if (sheet.numRows != numRows) { return false; }
      if (sheet.numProcs != numProcs) { return false; }
      if (!sameEPs(sheet)) { return false; }
      return true;
    }

    /** Return true if the two Sheets have the same EP names. */
    public boolean sameEPs(Sheet sheet) {
      if (sheet.tableRows.length != tableRows.length) { return false; }
      for (int i=0; i<tableRows.length; i++) {
	if (!sheet.tableRows[i].name.equals(tableRows[i].name)) {
	  return false; 
	}
      }
      return true;
    }

    /** Assuming that counters and data are set, now sum them all up and 
     *  create the EPValues for the table rows. */
    private void calcEPValueTotals() {
      int i, j;
      numCols = 1 + counters.length * EPValue.NUM_VALUES_PER_COUNTER;
      // sum up the rows
      int totalIndex = numRows-1;
      tableRows[totalIndex] = new EPValue("ALL EPs", counters.length);
      for (i=0; i<totalIndex; i++) {
	for (j=0; j<counters.length; j++) {
	  tableRows[totalIndex].summary[j].numCalled += 
	    tableRows[i].summary[j].numCalled;
	  tableRows[totalIndex].summary[j].totTime += 
	    tableRows[i].summary[j].totTime;
	  tableRows[totalIndex].summary[j].avgCount +=
	    tableRows[i].summary[j].avgCount * 
	    tableRows[i].summary[j].numCalled;
	}
      }
    }

    /** Based on filename, return true index (foo is returned now, faking out
     *  this function).  This function exists because in file list, 128 can
     *  come before 2. */
    private int getProcIndex(File file, int foo) throws IOException { 
      String fileName = file.getName();
      int lastDotIndex = fileName.lastIndexOf(".");
      int nextDotIndex = fileName.lastIndexOf(".", lastDotIndex-1);
      if (!fileName.endsWith(".count")) {
	throw new IOException("Expect count files to end with .count:\n"+
			      file.getCanonicalPath());
      }
      // given that the sts has name like: "namd2.count.sts", find
      // all files of the name "namd2.x.count"
      String number = fileName.substring(nextDotIndex+1, lastDotIndex);
      Integer intNum = new Integer(number);
      return intNum.intValue();
    }
  }

  /** Represents each row in the table. */
  private class EPValue { 
    public final static int NUM_VALUES_PER_COUNTER = 3;
    public final static int LONG_NUM_VALUES_PER_COUNTER = 6;

    public final static int AVG_CALLED = 0;
    public final static int AVG_TIME   = 1;
    public final static int AVG_CVAL   = 2;
    public final static int AVG_CSTDEV = 3;
    public final static int MAX_CVAL   = 4;
    public final static int MIN_CVAL   = 5;
    
    public final static String AVG_CALLED_STR = "numCalled per proc (avg)";
    public final static String AVG_TIME_STR   = "totTime(us) per proc (avg)";
    public final static String AVG_CVAL_STR   = "per EP (avg over all procs)";
    public final static String AVG_CSTDEV_STR = "stDev per EP (avg over all procs)";
    public final static String MAX_CVAL_STR   = "maxCount over all procs";
    public final static String MIN_CVAL_STR   = "minCount over all procs";

    public String           name    = null;
    public CounterSummary[] summary = null;

    public EPValue(String n, int size) {
      name = n;
      summary = new CounterSummary[size];
      for (int i=0; i<size; i++) { summary[i] = new CounterSummary(); }
    }

    public EPValue(
      String n, int index, LogData[] procData, int numCounters, boolean addNum)
    {
      int i, j;
      name = (addNum) ? '['+(new Integer(index)).toString()+"] "+n : n;
      // DO NUMBER CHECK HERE???
      summary = new CounterSummary[numCounters];
      for (i=0; i<summary.length; i++) { summary[i] = new CounterSummary(); }
      int numProcs = procData[0].procTotal;
      for (j=0; j<summary.length; j++) {
	CounterSummary s = summary[j];
	for (i=0; i<numProcs; i++) {
	  CountData[] data = procData[i].cResults[j].countData;
	  s.numCalled += data[CountData.NUM_CALLED].getValue(index);
	  s.totTime += data[CountData.TOTAL_TIME].getValue(index);
	  double count = data[CountData.AVG_COUNT].getValue(index);
	  s.avgCount += count;
	  s.maxCount = Math.max(s.maxCount, (int) count);
	  s.minCount = Math.min(s.minCount, (int) count);
	}
	s.numCalled /= numProcs;
	s.totTime /= numProcs;
	s.avgCount /= numProcs;
      }
    }

  }

  /* used by the EPValue for each counter sum or avg across all procs. */
  private class CounterSummary {
    public double numCalled    = 0.0;
    public double totTime      = 0.0;
    public double avgCount     = 0.0;
    public double stdevCount   = 0.0;
    public int    maxCount     = Integer.MIN_VALUE;
    public int    minCount     = Integer.MAX_VALUE;
    
    public CounterSummary() { }
  }

  /* For sts data, the raw counts controlled by this class. */
  private class LogData { 
    public int procNum;
    public int procTotal;
    public int numEPs;

    public CounterResults[] cResults = null; 

    public LogData(
      File file, int index, Sheet sheet, boolean first, String stsName) 
      throws IOException 
    {
      String err = "File: "+file.getPath()+"\n  ERROR in LogData(): ";
      try {
	BufferedReader reader = 
	  new BufferedReader(new FileReader(file.getCanonicalPath()));
	String line = reader.readLine();
	ParseTokenizer t = new ParseTokenizer(new StringReader(line));
	t.parseNumbers();
	t.whitespaceChars(':', ':');
	t.whitespaceChars('/', '/');
	t.nextString("ver");
	double version = t.nextNumber(err+"version");
	procNum = (int) t.nextNumber(err+"procNum");
	if (procNum != index) { 
	  throw new IOException(err+"read index "+procNum+" expected "+index);
	}
	procTotal = (int) t.nextNumber(err+"procTotal");
	t.nextString("ep");
	numEPs = (int) t.nextNumber(err+"numEPs");
	t.nextString("counters");
	int numCounters = (int) t.nextNumber(err+"numCounters");
	if (first) { sheet.counters = new Counter[numCounters]; }
	else if (sheet.counters.length != numCounters) {
	  throw new IOException(
	    err+"expected "+cResults.length+" counters but got "+numCounters);
	}
	// check to make sure that was the last data
	if (t.nextToken()!=StreamTokenizer.TT_EOF) {
	  throw new IOException(err+"expected EOF but not found");
	}
	cResults = new CounterResults[numCounters]; 
	for (int i=0; i<numCounters; i++) {
	  cResults[i] = new CounterResults(
	    file, reader, numEPs, sheet.counters, i, first, stsName);
	}
      }
      catch (Exception e) { throw new IOException(err+"\n"+e.getMessage()); }
    }
    
    public LogData(int n, int tot, int numEPs, int numCResults) {
      procNum = n;
      procTotal = tot;
      this.numEPs = numEPs;
      cResults = new CounterResults[numCResults];
    }
  }

  /** For each counter, store the raw data for all EPs */
  private class CounterResults {
    public Counter     counter     = null;
    public CountData[] countData   = null;

    public CounterResults(
      File file, BufferedReader reader, int numEPs, Counter[] groupCounters, 
      int index, boolean first, String stsName) 
      throws IOException 
    {
      String err = 
	"File: "+file.getCanonicalPath()+"\n ERROR in CounterResults(): ";
      try {
	String line = reader.readLine();
	ParseTokenizer t = 
	  new ParseTokenizer(new StringReader(line));
	t.parseNumbers();
	t.whitespaceChars('[', '[');
	t.whitespaceChars(']', ']');
	t.wordChars('_', '_');
	String counterName = t.nextString("counterCode");
	// get description
	int firstIndex = line.indexOf('{');
	int secondIndex = line.indexOf('}');
	if (firstIndex == -1 || secondIndex == -1) {
	  throw new IOException(err+" counter "+counterName+
	    " require { and } on first line for description\n"+line);
	}
	String counterDescription = line.substring(firstIndex+1,secondIndex);
	if (first) { 
	  groupCounters[index] = 
	    new Counter(counterName, counterDescription, stsName); 
	}
	else if (!groupCounters[index].counterCode.equals(counterName) ||
		 !groupCounters[index].description.equals(counterDescription)) 
	{
	  throw new IOException(
	    err+"expected "+groupCounters[index].counterCode+" {"+
	    groupCounters[index].description+"}\ngot "+counterName+" {"+
	    counterDescription+"}");
	}
	counter = groupCounters[index];
	countData = new CountData[5];
	countData[CountData.NUM_CALLED] = new IntCountData(
	  reader.readLine(), counterName, CountData.NUM_CALLED, numEPs);
	countData[CountData.AVG_COUNT] = new DoubleCountData(
	  reader.readLine(), counterName, CountData.AVG_COUNT, numEPs);
	countData[CountData.TOTAL_TIME] = new IntCountData(
	  reader.readLine(), counterName, CountData.TOTAL_TIME, numEPs);
      }
      catch (Exception e) { throw new IOException(err+"\n"+e.getMessage()); }
    }
  }

  /** Each counter has different types of stats, store them here. */
  private abstract class CountData {
    public static final int NUM_CALLED = 0;
    public static final int TOTAL_TIME = 1;
    public static final int MAX_VALUE  = 2;
    public static final int MIN_VALUE  = 3;
    public static final int AVG_COUNT  = 4;

    public int type = -1;

    public CountData(int t) { type = t; }
    public abstract void setValue(int index, double num);
    public abstract double getValue(int index);
    public void readLine(String line, String code, int numEPs) 
      throws IOException 
    {
      String err = 
	"ERROR in CountData.readLine parser for line:\n  "+line+"\n";
      try {
	ParseTokenizer t = new ParseTokenizer(new StringReader(line));
	t.parseNumbers();
	t.whitespaceChars('[', '[');
	t.whitespaceChars(']', ']');
	t.wordChars('_', '_');
	t.wordChars('(', '(');
	t.wordChars(')', ')');
	t.checkNextString(code);
	String typeStr = null;
	switch (type) {
          case NUM_CALLED: typeStr = "num_called";     break;
          case TOTAL_TIME: typeStr = "total_time(us)";  break;
          case MAX_VALUE:  typeStr = "max_value";      break;
          case MIN_VALUE:  typeStr = "min_value";      break;
          case AVG_COUNT:  typeStr = "avg_count";      break;
          default:         typeStr = "UNKNOWN TYPE";   break;
	}
	t.checkNextString(typeStr);
	for (int i=0; i<numEPs; i++) { setValue(i, t.nextNumber("value")); }
	// check to make sure that was the last data
	if (t.nextToken()!=StreamTokenizer.TT_EOF) {
	  throw new IOException(err+"expected EOF but not found");
	}
      }
      catch (Exception e) { throw new IOException(err+"\n"+e.getMessage()); }
    }
  }

  /* Superclass of CountData for double type data. */
  private class DoubleCountData extends CountData {
    public double[] values = null;
    public DoubleCountData(String line, String code, int type, int numEPs) 
      throws IOException 
    {
      super(type);
      values = new double[numEPs];
      readLine(line, code, numEPs);
    }
    public void setValue(int index, double num) { values[index] = num; }
    public double getValue(int index) {
		if(index < values.length)
			return values[index];
		else
			return 0.0;
	}
  }

  /* Superclass of CountData for int type data. */
  private class IntCountData extends CountData {
    public int[] values = null;
    public IntCountData(String line, String code, int type, int numEPs) 
      throws IOException
    {
      super(type);
      values = new int[numEPs];
      readLine(line, code, numEPs);
    }
    public void setValue(int index, double num) { values[index] = (int) num; }
    public double getValue(int index) { return (double) values[index]; }
  }

  /** To enable the correct format for both int and scientific. */
  private class FormattedNumber extends Number {
    double       number;
    NumberFormat format;
    public FormattedNumber(double d, NumberFormat f) {
      number = d;
      format = f;
    }
    public byte byteValue() { return (byte) number; }
    public double doubleValue() { return number; }
    public float floatValue() { return (float) number; }
    public int intValue() { return (int) number; }
    public long longValue() { return (long) number; }
    public short shortValue() { return (short) number; }
    public String toString() { return format.format(number); }
  }
}








