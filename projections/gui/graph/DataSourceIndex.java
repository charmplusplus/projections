// UNUSED FILE

///**
// * DataSource2D-- provide simple 2D data to be graphed.
// * 
// * 2D data means that there are indexed X axis, with multiple values for
// * the y axis.
// * 
//**/
//
//package projections.gui.graph;
//import projections.gui.*;
//import java.awt.*;
//
//// x axis is indexed and y axis is double
//// can display as line or bar
//public class DataSourceIndex extends DataSource
//{
//  public static final int BAR = 1;
//  public static final int LINE = 2;
//
//  private int          type_;      // BAR or LINE
//  // only one of the datas will be valid
//  private double[][]     data2D_;    // data to be graphed in multiple per index
//  private double[]       data1D_;    // data to be graphed one per index 
//  private int[]          dataInt_;   // data to be graphed one per index
//  private int            dataSize_;  // size of non-null array above
//  private Coordinate[][] coords_;    // size [getNumDataSets][dataSize]
//  private double         maxY_ = -Double.MAX_VALUE;
//  private double         minY_ = +Double.MAX_VALUE;
//  private int            maxX_;
//
//  // assume starts at x = 0.0 and goes until x = data.length-1
//  public DataSourceIndex(String title, double[][] data, int type) {
//    super(title);
//    type_ = type;
//    data2D_=data;
//    dataSize_ = data2D_.length;
//    coords_ = initCoords(data2D_[0].length, dataSize_);
//    maxX_ = data.length-1;
//    calcDrawingData();
//  }
//
//  // assume starts at x = 0.0 and goes until x = data.length-1
//  public DataSourceIndex(String title, double[] data, int type) {
//    super(title);
//    type_ = type;
//    data1D_=data;
//    dataSize_ = data1D_.length;
//    coords_ = initCoords(1, dataSize_);
//    maxX_ = data.length-1;
//    calcDrawingData();
//  }
//
//  // assume starts at x = 0.0 and goes until x = data.length-1
//  public DataSourceIndex(String title, int[] data, int type) {
//    super(title);
//    type_ = type;
//    dataInt_=data;
//    dataSize_ = dataInt_.length;
//    coords_ = initCoords(1, dataSize_);
//    maxX_ = data.length-1;
//    calcDrawingData();
//  }
//
//  /** memory allocation */
//  private Coordinate[][] initCoords(int x, int y) {
//    Coordinate[][] c = new Coordinate[x][y];
//    for (int i=0; i<x; i++) {
//      for (int j=0; j<y; j++) {
//	c[i][j] = new Coordinate(j, 0);
//      }
//    }
//    return c;
//  }
//  
//  /** If any advance calc needed before drawing, do it here. */
//  public void calcDrawingData() {
//    int i, j;
//    if (data1D_ != null)  { 
//      for (i=0; i<dataSize_; i++) { coords_[0][i].y = data1D_[i]; }
//    }
//    if (data2D_ != null)  { 
//      for (i=0; i<dataSize_; i++) {
//	for (j=0; j<data2D_[i].length; j++) {
//	  coords_[j][i].y = data2D_[i][j];
//	}
//      }
//    }
//    if (dataInt_ != null) { 
//      for (i=0; i<dataSize_; i++) { coords_[0][i].y = dataInt_[i]; }
//    }
//
//    maxY_ = -Double.MIN_VALUE;
//    minY_ = +Double.MAX_VALUE;
//    for (i=0; i<coords_.length; i++) {
//      for (j=0; j<dataSize_; j++) {
//	if (coords_[i][j].y > maxY_) { maxY_ = coords_[i][j].y; }
//	if (coords_[i][j].y < minY_) { minY_ = coords_[i][j].y; }
//      }
//    }
//  }
//
//  // fill the data passed in the values for index index
//  public void getValues(int index, double[] data) {
//    if (data1D_ != null) { data[0] = data1D_[index]; }
//    if (data2D_ != null) {
//      for (int i=0; i<data2D_[index].length; i++) {
//	data[i] = data2D_[index][i];
//      }
//    }
//    if (dataInt_ != null) { data[0] = dataInt_[index]; }
//  }
//
//  // get set type LINE or BAR
//  public int getType() { return type_; }
//  public void setType(int type) { type_ = type; }
//
//  // return the number of indices
//  public int getNumIndices() { 
//    return dataSize_;
//  }
//
//  // if this is a 2d DataSource, return the number of data sets
//  public int getNumDataSets() {
//    if (data2D_ != null) { return data2D_[0].length; }
//    else { return 1; }
//  }
//
//  public void getLimits(Coordinate ul_, Coordinate lr_) {
//    if (ul_ != null) { ul_.x = 0.0;    ul_.y = maxY_; }
//    if (lr_ != null) { lr_.x = maxX_;  lr_.y = minY_; }
//  }
//
//  public void draw(Graphics g, Graph graph) {
//    g.setColor(Color.white);
//    int numSets = getNumDataSets();
//    for (int i=0; i<numSets; i++) {
//      // set color here
//      graph.drawXYarray(coords_[i]);
//    }
//  }
//}
//
//
//
//
//
//
//
