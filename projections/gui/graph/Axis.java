/**
 * Joshua Mostkoff Unger, unger1@uiuc.edu
 * Base class for X and Y axes.
 */

package projections.gui.graph;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.text.DecimalFormat;

public abstract class Axis
{
  public static final int DISCRETE = 1;
//  public static final int CONTINUOUS = 2;

  private static final int MAJOR_TICK_OFFSET = 4;
  private static final int MINOR_TICK_OFFSET = 2;
  private static final int MINOR_TICKS_PER_MAJOR = 4;
  private static final int DIST_BETWEEN_MINOR_TICKS = 10;
  private static final int DIST_BETWEEN_LABELS = 5;
  private static final Color MAJOR_TICK_COLOR = Color.white;
  private static final Color MINOR_TICK_COLOR = Color.lightGray;

  private DecimalFormat small_ = null;  // with decimals
  private DecimalFormat large_ = null;    // no decimals
  private double min_;
  private double max_;
  double scale_ = 1.0;
  
  /** Return the border from the axis to edge of tick. */
  public static int getTickSize() { return MAJOR_TICK_OFFSET; }

  /** Sets zoom for display purposes. */
  public void setZoom(double min, double max) {
    min_ = min;
    max_ = max;
    scale_ = getSize()/(max-min);
  }

  /** Convert from actual to axis coordinates. 
   *  This function in axis so can implement log graph. 
   *  Return normalized number from 0-1.0 (getMin()-getMax()) 
   *  Number can be outside these limits however.
   */
  public double coordToNormalScale(double c) {
    return (c-min_)/(max_-min_);
  }

  /** If axis wants to resize based on new data.  Default is to ignore. */
  public void newLimits(Graph g, double min, double max) { }

  /** Draw a major tick with center at posAlongAxis, axisPos (or vice versa
   *  depending on x or y axis, and width 2*size+1. */
  public abstract void drawTick(
    Graphics g, int posAlongAxis, int axisPos, int size);

  /** Draw the axis. */
  public abstract void drawAxis(Graphics g, int axisPos, int start, int size);

  /** Draw a string centered at x, y. */
  public abstract void drawLabel(
    Graphics g, FontMetrics f, String s, int x, int y);

  /** Return size of label in pixels. */
  public int getVerticalSize(FontMetrics fm, String label) {
    int length = label.length();
    int charHeight = fm.getHeight();
    int strHeight = charHeight*length;
    return strHeight;
  }

  /** Return size in pixels of s (if discrete data) 
   *  Inheriting class can use getVerticalSize for this
   *  if they want to make vertically spaced labels!
   **/
  public abstract int getDimDiscrete(FontMetrics fm, String s);

  /** Return size in pixels of s (if continuous data). */
  public abstract int getDimContinuous(FontMetrics fm, String s);

  /** Return max dimension to allocate for screen for the labels of axis. */
  public int getLabelDimension(FontMetrics fm, int size) {
    TickMetrics tm = new TickMetrics(size);
    boolean discrete = (getAxisType() == DISCRETE);
    int indicesPerTick = (discrete) ? (int)(tm.tickSpacing / tm.dataSize) : 0;
    int maxDim = 0;
    for (int i=0; i<tm.numTicks; i++) {
      int dim = (discrete) ? 
	getDimDiscrete(fm, getIndexName((int)(min_+0.5)+i*indicesPerTick)) :
	getDimContinuous(
	  fm, getValueName(min_+i*tm.tickSpacing/size*tm.actualSize));
      if (dim > maxDim) { maxDim = dim; }
    }
    return maxDim;
  }

  /** Common data between two functions. */
  private class TickMetrics {
    public double actualSize;
    public double dataSize;
    public int    numTicks;
    public double tickSpacing;

    TickMetrics(int size) {
      actualSize = getSize()*scale_;
      dataSize = size / actualSize;
      numTicks = (int)((size - dataSize) / DIST_BETWEEN_MINOR_TICKS);
      tickSpacing = (dataSize > DIST_BETWEEN_MINOR_TICKS) ? 
	dataSize : DIST_BETWEEN_MINOR_TICKS;
      numTicks = (int)(size / tickSpacing);
    }
  }

  /* draw the axis, ticks, and labels. */
  public void draw(Graphics g, FontMetrics fm, 
		   int labelPos, // offset from axis to draw label
		   int axisPos,  // pixel location of axis
		   int start,    // start pixel of axis
		   int size,     // size of axis
		   int dir)      // -1 for y axis, 1 for axis
  {
    drawAxis(g, axisPos, start, size);
    TickMetrics tm = new TickMetrics(size);

    // calc max label size
    int maxLabelSize = 0;
    int indicesPerTick = 0;
    if (getAxisType() == DISCRETE) {
      indicesPerTick = (int)(tm.tickSpacing / tm.dataSize);
      maxLabelSize = calcMaxLabelSize(fm, indicesPerTick, tm.numTicks);
    }
    // now calculate new num ticks per major
    int minor_ticks_per_major = MINOR_TICKS_PER_MAJOR;
    if (getAxisType() == DISCRETE) {
      minor_ticks_per_major = calcNumMinorTicks(
	fm, minor_ticks_per_major, maxLabelSize, tm.numTicks, tm.tickSpacing);
    }
      
    int tickIndex = 0;
    if (getAxisType() == DISCRETE) {
      start += (int)((Math.ceil(min_)-min_)/(max_-min_)*size+0.5);
    }
    for (int i=0; i<tm.numTicks; i++) {
      int pos = start + dir*(int)(i*tm.tickSpacing + 0.5);
      int tickSize = 0;
      if (tickIndex % (minor_ticks_per_major+1) == 0) {
	tickSize = MAJOR_TICK_OFFSET;
	g.setColor(MAJOR_TICK_COLOR);
	// draw major label
	int xLoc = (dir > 0) ? pos : labelPos;
	int yLoc = (dir > 0) ? labelPos : pos;
	if (getAxisType() == DISCRETE) {
	  drawLabel(g, fm, getIndexName(
	    (int)(Math.ceil(min_)+tickIndex*indicesPerTick)), xLoc, yLoc);
	}
	else {
	  drawLabel(g, fm, 
	    getValueName(min_+i*tm.tickSpacing/size*tm.actualSize),
	    xLoc, yLoc);
	}
      }
      else { 
	tickSize = MINOR_TICK_OFFSET; 
	g.setColor(MINOR_TICK_COLOR);
      }
      drawTick(g, pos, axisPos, tickSize);
      tickIndex++;
    }
  }

  /** Return number of minor ticks per major tick */
  private int calcNumMinorTicks(
    FontMetrics fm, int minor_ticks_per_major, int maxLabelSize, int numTicks, 
    double tickSpacing) 
  {
    while (tickSpacing*(minor_ticks_per_major+1)<maxLabelSize &&
	   minor_ticks_per_major < numTicks-1) 
    {
      minor_ticks_per_major++;
    }
    // adjust num ticks per major to fit the most ticks as possible
    boolean adjusted = false;
    while (tickSpacing*(minor_ticks_per_major+1) >=
	   (maxLabelSize+DIST_BETWEEN_LABELS) &&
	   minor_ticks_per_major > 0) 
    {
      adjusted = true;
      minor_ticks_per_major--;
    }
    if (adjusted && 
	tickSpacing*(minor_ticks_per_major+1) < 
	(maxLabelSize+DIST_BETWEEN_LABELS)) 
    { 
      minor_ticks_per_major++; 
    }
    return minor_ticks_per_major;
  }

  /** Return the max label size for possible indices */
  private int calcMaxLabelSize(
    FontMetrics fm, int indicesPerTick, int numTicks) 
  {
    int maxLabelSize = 0;
    for (int i=0; i<numTicks; i++) {
      int labelSize = getLabelSize(fm, getIndexName(
	(int)(min_+0.5)+i*indicesPerTick));
      if (labelSize > maxLabelSize) { maxLabelSize = labelSize; }
    }
    return maxLabelSize;
  }      

  /** Return size of label in pixels. */
  public abstract int getLabelSize(FontMetrics fm, String label);

  /**
   * Return type, either DISCRETE or CONTINUOUS.
   */
  public abstract int getAxisType();

  /**
   * Return a human-readable string to describe this axis.
   *  e.g., "CPU Utilization(%)", or "Queue Length"
   */
  public abstract String getTitle();

  /**
   * Return the minimum value on this axis.  This should almost
   * always be zero, which is the default implementation.
   */
  public abstract double getMin();

  /**
   * Return the maximum value on this axis.  
   * This must be larger than the value returned by getMin().
   *  e.g., 100.0
   */
  public abstract double getMax();
   
  /** Return the size of the axis (difference between the max and min coord. */
  public double getSize() { return getMax()-getMin(); }
  
  /**
   * Return the smallest reasonable difference between two axis values.
   *  The default is 1/100 the difference between getMax and getMin.
   *  e.g., 1.0 for discrete values.
   */
  public double getDifference() { return 0.01*(getMax()-getMin()); }

  /**
   * Return the human-readable name for this value.
   * e.g., "17%", "42.96"
   * This will only be valid for CONTINUOUS type Axes
   */
  public String getValueName(double val) { 
    if (large_ == null) { 
      large_ = new DecimalFormat(); 
      large_.setGroupingUsed(true);
      large_.setMinimumFractionDigits(0);
      large_.setMaximumFractionDigits(0);
    }
    if (small_ == null) { 
      small_ = new DecimalFormat(); 
      small_.setGroupingUsed(true);
    }
    // this won't be totally tested, so not sure if it will work
    // NEED TO DO A FORMAT HERE
    double value = Math.abs(val);
    if (value >= 100) { return large_.format(val); }
    else if (value >= 10) { 
      small_.setMinimumFractionDigits(1);
      small_.setMaximumFractionDigits(1);
      return small_.format(val);
    }
    else if (value >= 1) {
      small_.setMinimumFractionDigits(2);
      small_.setMaximumFractionDigits(2);
      return small_.format(val);
    }
    else {
      small_.setMinimumFractionDigits(3);
      small_.setMaximumFractionDigits(3);
      return small_.format(val);
    }
  }

   /**
    * Return the human-readable name of this index.
    *   Not all indices will necessarily have their name displayed.
    * This will only be valid for DISCRETE type Axes
    * e.g., "7", "10-11ms"
    */
   public String getIndexName(int index) { 
     if (large_ == null) { 
       large_ = new DecimalFormat(); 
       large_.setGroupingUsed(true);
       large_.setMinimumFractionDigits(0);
       large_.setMaximumFractionDigits(0);
     }
     return large_.format(index); 
   }

   /**
    * Return the minimum index for DISCRETE Axes.
    * Can be negative.
    */
   public abstract int getMinIndex();

   /**
    * Return the maximum index for DISCRETE Axes.
    * Can be negative, but should be greater than getMinIndex();
    */
   public abstract int getMaxIndex();

   /**
    *Return the number of indices for DISCRETE Axes.
    */
   public int getNumIndices() { return getMaxIndex()-getMinIndex()+1; }

   /** 
    * Return the units of this axis, like "ms" or "#" or even null if none
    */
   public abstract String getUnits();
}


