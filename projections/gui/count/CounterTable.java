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
  // WHAT OTHER CHECKS SHOULD I DO TO MAKE SURE OUTPUTS MATCH?
  // FIX getProxIndex!!!
  // (EVERYWHERE) SEARCH FOR LENGTH AND REPLACE BY NUMBER???
  // MAKE ILLUSTRATION OF CLASS DATA MEMBER RELATIONSHIPS!!!
  // USE PROGRESS BAR!!!
  // CLOSE READERS!!! (AND IN OTHER FILES)
  // CHECK VALIDITY OF SUM/MAX/MIN/STDEV
  // COLOR CODE THE COUNTER DATA
  // CHECK FLOPS VS GR_FLOPS FOR MFLOPS RATING
  // DIALOG BOXES FOR SYSTEM.OUT.PRINTLN()

  // http://java.sun.com/products/jdk/1.1/docs/api/packages.html
  // http://java.sun.com/products/jfc/swingdoc-api-1.1/index.html
  // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
  // http://java.sun.com/docs/books/tutorial/uiswing/misc/border.html
  // http://java.sun.com/docs/books/tutorial/2d/index.html

  public CounterTable() { 
    leftJustify_.setHorizontalAlignment(JLabel.LEFT);
    rightJustify_.setHorizontalAlignment(JLabel.RIGHT);
    intFormat_.setGroupingUsed(true);
    doubleFormat_.setGroupingUsed(true);
    doubleFormat_.setMinimumFractionDigits(2);
    doubleFormat_.setMaximumFractionDigits(2);
  }

  public int getColumnCount() { 
    if (currSheet_ != null) { return currSheet_.numCols; } 
    else { return 0; }
  }
  public int getRowCount() {
    if (currSheet_ != null) { return currSheet_.numRows; } 
    else { return 0; }
  }
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
  public Class getColumnClass(int columnIndex) {
    if (currSheet_ != null) {
      if (columnIndex == 0) { return java.lang.String.class; }
      else { return FormattedNumber.class; }
    }
    return java.lang.String.class;
  }
  public Object getValueAt(int row, int col) { 
    if (currSheet_ != null) {
      if (col == 0) { return currSheet_.tableRows[row].name; }
      else {
	int counter = (col-1) / EPValue.NUM_VALUES_PER_COUNTER;
	CounterSummary s = currSheet_.tableRows[row].summary[counter];


	int pos = (col-1) % EPValue.NUM_VALUES_PER_COUNTER;
	switch (pos) {
	  case EPValue.AVG_CALLED: 
	    return new FormattedNumber(s.avgNumCalled, doubleFormat_);
	  case EPValue.AVG_TIME:   
	    return new FormattedNumber(s.avgTime, doubleFormat_);
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

  /** Given file manager, load files and update progress bar. */
  public void loadFiles(
    ProjectionsFileMgr fileMgr, JProgressBar progress, JTable table) 
    throws IOException
  {
    try {
      int i, j;
      int numFiles = fileMgr.getNumFiles();
      sheet_ = new Sheet[numFiles];
      for (i=0; i<numFiles; i++) { sheet_[i] = new Sheet(fileMgr, i); }
      setSheet(0, table);
    }
    catch (IOException e) {
      throw new IOException("ERROR in CounterTable.loadFiles()\n"+
			    e.getMessage());
    }
  }

  /** Return the list of counters and their colors for this sheet */
  public Component getCounterPanel(int index) {
    if (sheet_ == null || sheet_[index] == null) { return null; }
    CounterListTable clTable = new CounterListTable(sheet_[index].counters);
    JTable table = new JTable(clTable);
    table.setRowSelectionAllowed(false);
    clTable.configure(table);
    JPanel panel = new JPanel();
    panel.add(new JScrollPane(table));
    Dimension d = table.getPreferredSize();
    int numCounters = sheet_[index].counters.length;
    // adjust because table doesn't want to display header
    d.height = Math.min(100, (d.height / numCounters ) * (numCounters + 1));  
    panel.setPreferredSize(d);
    return panel;
  }

  public void setSheet(int index, JTable table) {
    // save sorted column here, also set columns correctly
    currSheet_ = sheet_[index];
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

  public void createGraph(int[] selectedRows) {
    if (selectedRows.length == 0) {
      System.out.println("ERROR there are no selected rows");
    }
    else if (currSheet_ == null) { System.out.println("NO DATA IN TABLE"); }
    else {
      // allocate new info and sort it and pass it into the graph class
      // for (int i=0; i<selectedRows.length; i++) {
      // System.out.println(
      // "  Graph for "+currSheet_.tableRows[selectedRows[i]].name);
      // }
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
      DataSource2D source = new DataSource2D(currSheet_.fileName, data);
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

  public void calcMFlops() {
    if (currSheet_ == null) { System.out.println("NO DATA IN TABLE"); }
    else {
      int i;
      Sheet s = currSheet_;
      // find the correct counter to use
      boolean found = false;
      int counterIndex = -1;
      for (i=0; i<s.counters.length || !found; i++) {
	if (s.counters[i].counterCode.equals("GR_FLOPS")) {
	  found = true;
	  counterIndex = i;
	}
      }
      if (!found) { System.out.println("COULD NOT FIND FLOPS!!!");  return; }
      // loop through EPs and find out which ones have been called (non-null)
      Vector vector = new Vector();
      for (i=0; i<s.tableRows.length-1; i++) {
	if (s.tableRows[i].summary[counterIndex].avgNumCalled > 0) {
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
      String[] xAxisLabels = new String[numEPs];
      double totalFlops = 0;
      double totalCalled = 0;
      for (i=0; i<numEPs; i++) {
	xAxisLabels[i] = (new Integer(indices[i])).toString();
	String name = s.tableRows[indices[i]].name;
	CounterSummary summary = s.tableRows[indices[i]].summary[counterIndex];
	data[i] = new double[2];
	data[i][0] = 
	  summary.avgNumCalled*summary.avgCount*s.numProcs/summary.avgTime*1e6;
	totalFlops += data[i][0]*summary.avgNumCalled;
	totalCalled += summary.avgNumCalled;
	System.out.print("Label "+i+"="+xAxisLabels[i]+" "+
			 name.substring(1,Math.min(name.length(),25)));
	System.out.println("  time/proc="+summary.avgTime+
			   " numCalled/proc="+summary.avgNumCalled+
			   " Flops/proc="+summary.avgCount+
			   " Flops/sec="+data[i][0]);
      }
      double calcMFlops = totalFlops / totalCalled;
      System.out.println("OVERALL FLOPS="+calcMFlops);
      for (i=0; i<numEPs; i++) { data[i][1] = calcMFlops; }
      // set up graph
      DataSource2D source = new DataSource2D(
	currSheet_.fileName+": Overall Flops/s = "+calcMFlops, data);
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

  private Sheet[] sheet_     = null;
  private Sheet   currSheet_ = null;
  private DefaultTableCellRenderer leftJustify_ = 
    new DefaultTableCellRenderer();
  private DefaultTableCellRenderer rightJustify_ = 
    new DefaultTableCellRenderer();
  private DecimalFormat intFormat_ = new DecimalFormat();
  private DecimalFormat doubleFormat_ = new DecimalFormat();
  private static final LineBorder border_ = (LineBorder)
	BorderUIResource.LineBorderUIResource.createGrayLineBorder();
  
  /** Each sheet represents a simulation. */
  private class Sheet {
    public int numCols  = 0;  // number of data columns in simulation 
    public int numRows  = 0;  // number of EPs in simulation
    public int numProcs = 0;  // number of processors for this simulation

    public String           fileName  = null;  // just store name of file
    public GenericStsReader stsReader = null;  // to read and hold data for sts
    public Counter[]        counters  = null;  // stores name/description
    public EPValue[]       tableRows = null;  // store row data
    public LogData[]        data      = null;  // to store data for log files
    
    public Sheet(ProjectionsFileMgr fileMgr, int index) 
      throws IOException 
    { 
      int i, j;
      fileName = fileMgr.getStsFile(index).getCanonicalPath();
      // read sts file
      stsReader = new GenericStsReader(
	fileMgr.getStsFile(index).getCanonicalPath(), 1.0);
      // read log file data
      File[] logFiles = fileMgr.getLogFiles(index);
      data = new LogData[logFiles.length]; 
      boolean first = true;
      for (i=0; i<logFiles.length; i++) {
	int realIndex = getProcIndex(logFiles[i], i);
	data[realIndex] = new LogData(logFiles[i], i, this, first);
	first = false;
      }
      // now initialize the EPValue by summarizing over all processors
      tableRows = new EPValue[stsReader.entryCount+1];
      numRows = stsReader.entryCount+1;
      for (i=0; i<numRows-1; i++) {
	tableRows[i] = 
	  new EPValue(stsReader.entryList[i].name, i, data, counters.length);
      }
      numCols = 1 + counters.length * EPValue.NUM_VALUES_PER_COUNTER;
      numProcs = stsReader.numPe;
      // sum up the rows
      int totalIndex = numRows-1;
      tableRows[totalIndex] = new EPValue("TOTAL", counters.length);
      for (i=0; i<totalIndex; i++) {
	for (j=0; j<counters.length; j++) {
	  tableRows[totalIndex].summary[j].avgNumCalled += 
	    tableRows[i].summary[j].avgNumCalled;
	  tableRows[totalIndex].summary[j].avgTime += 
	    tableRows[i].summary[j].avgTime;
	  tableRows[totalIndex].summary[j].avgCount +=
	    tableRows[i].summary[j].avgCount;
	}
      }
    }

    private int getProcIndex(File file, int foo) { return foo; }
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

    public EPValue(String n, int index, LogData[] procData, int numCounters) 
    {
      int i, j;
      name = '['+(new Integer(index)).toString()+"] "+n;
      // DO NUMBER CHECK HERE???
      summary = new CounterSummary[numCounters];
      for (i=0; i<summary.length; i++) { summary[i] = new CounterSummary(); }
      int numProcs = procData[0].procTotal;
      for (j=0; j<summary.length; j++) {
	CounterSummary s = summary[j];
	for (i=0; i<numProcs; i++) {
	  CountData[] data = procData[i].cResults[j].countData;
	  s.avgNumCalled += data[CountData.NUM_CALLED].getValue(index);
	  s.avgTime += data[CountData.TOTAL_TIME].getValue(index);
	  double count = data[CountData.AVG_COUNT].getValue(index);
	  s.avgCount += count;
	  s.maxCount = Math.max(s.maxCount, (int) count);
	  s.minCount = Math.min(s.minCount, (int) count);
	}
	s.avgNumCalled /= numProcs;
	s.avgTime /= numProcs;
	s.avgCount /= numProcs;
      }
    }

  }

  private class CounterSummary {
    public double avgNumCalled = 0;
    public double avgTime      = 0;
    public double avgCount     = 0.0;
    public double stdevCount   = 0.0;
    public int    maxCount     = Integer.MIN_VALUE;
    public int    minCount     = Integer.MAX_VALUE;
    
    public CounterSummary() { }
  }

  private class LogData { 
    public int procNum;
    public int procTotal;
    public int numEPs;

    public CounterResults[] cResults = null; 

    public LogData(File file, int index, Sheet sheet, boolean first) 
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
	  cResults[i] = 
	    new CounterResults(file, reader, numEPs, sheet.counters, i, first);
	}
      }
      catch (Exception e) { throw new IOException(err+"\n"+e.getMessage()); }
    }
  }

  private class CounterResults {
    public Counter     counter     = null;
    public CountData[] countData   = null;

    public CounterResults(File file, BufferedReader reader, int numEPs, 
			  Counter[] groupCounters, int index, boolean first) 
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
	  groupCounters[index] = new Counter(counterName, counterDescription); 
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

  /** Store the raw data from each file */
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
    public double getValue(int index) { return values[index]; }
  }

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








