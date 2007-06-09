// UNUSED FILE -- says Isaac 

///**
// * DataSource-- provide the actual data to be graphed and graphs it.
// *
// * 
// * 
// * Joshua Mostkoff Unger, unger1@uiuc.edu, 07.10.2002
// */
//
//package projections.gui.graph;
//import java.awt.*;
//
//public class DataSourceXY extends DataSource
//{
//  private Coordinate[] coords_;
//  private Color        color_;
//  private Coordinate   ul_ = new Coordinate();
//  private Coordinate   lr_ = new Coordinate();
//
//  public DataSourceXY(String title, Coordinate[] c, Color color) {
//    super(title);
//    coords_ = c;
//    color_ = color;
//    calcDrawingData();
//  }
//
//  public void draw(Graphics g, Graph graph) {
//    g.setColor(color_);
//    graph.drawXYarray(coords_);
//  }
//
//  /** If any advance calc needed before drawing, do it here. */
//  public void calcDrawingData() {
//    ul_.x = +Double.MAX_VALUE;
//    ul_.y = -Double.MAX_VALUE;
//    lr_.x = -Double.MAX_VALUE;
//    lr_.y = +Double.MAX_VALUE;
//    for (int i=0; i<coords_.length; i++) {
//      if (coords_[i].x > lr_.x) { lr_.x = coords_[i].x; } 
//      if (coords_[i].x < ul_.x) { ul_.x = coords_[i].x; }
//      if (coords_[i].y > ul_.y) { ul_.y = coords_[i].y; } 
//      if (coords_[i].y < lr_.y) { lr_.y = coords_[i].y; }
//    }
//  }
//
//  /**
//   * Return the upper left and lower right (in cartesian coordinates) of 
//   * the boundaries of this dataSource.
//   */
//  public void getLimits(Coordinate ul, Coordinate lr) {
//    if (ul != null) { ul.x = ul_.x;  ul.y = ul_.y; }
//    if (lr != null) { lr.x = lr_.x;  lr.y = lr_.y; }
//  }
//}
//
