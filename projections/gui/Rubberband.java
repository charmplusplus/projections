package projections.gui;

import java.awt.*;

/** 
 * JMU: I took this from Java in a nutshell (I THINK!) and 
 * modified for image.<p>
 *
 * A abstract base class for rubberbands.<p>
 *
 * Rubberbands do their rubberbanding inside of a Component, 
 * which must be specified at construction time.<p>
 * 
 * Subclasses are responsible for implementing 
 * <em>void drawLast(Graphics g)</em> and 
 * <em>void drawNext(Graphics g)</em>.  
 *
 * drawLast() draws the appropriate geometric shape at the last 
 * rubberband location, while drawNext() draws the appropriate 
 * geometric shape at the next rubberband location.  All of the 
 * underlying support for rubberbanding is taken care of here, 
 * including handling XOR mode setting; extensions of Rubberband
 * need not concern themselves with anything but drawing the 
 * last and next geometric shapes.<p>
 *
 */
abstract public class Rubberband {
  protected Point anchor    = new Point(0,0); 
  protected Point stretched = new Point(0,0);
  protected Point last      = new Point(0,0); 
  protected Point end       = new Point(0,0);
  protected Point highlight = new Point(0,0);
  
  private Component component = null;
  private Image     image = null;
  private boolean   firstStretch = true;
  private boolean   firstHighlight = true;
  private Graphics  graphics = null ;
  
  abstract public void drawLast(Graphics g);
  abstract public void drawNext(Graphics g);
  abstract public void drawHighlight(Graphics g);
  abstract public void clearHighlight(Graphics g);
  
  public Rubberband(Component component) {
    this.component = component;
  }
  public Rubberband(Image image) {
    this.image = image;
  }
  public Point getAnchor   () { return anchor;    }
  public Point getStretched() { return stretched; }
  public Point getLast     () { return last;      }
  public Point getEnd      () { return end;       }
  public boolean getFirstStretch() { return firstStretch;       }
  
  public void highlight(Point p) {
    setGraphics();
    if (!firstHighlight) { clearHighlight(graphics); }
    highlight.x = p.x;
    highlight.y = p.y;
    if (graphics != null) { drawHighlight(graphics); }
    firstHighlight = false;
  }
  public void clearHighlight() {
    if (setGraphics() != null) { clearHighlight(graphics); }
    firstHighlight = true;
  }
  public void anchor(Point p) {
    firstStretch = true;
    anchor.x = p.x;
    anchor.y = p.y;
    
    stretched.x = last.x = anchor.x;
    stretched.y = last.y = anchor.y;
  }
  public void stretch(Point p) {
    last.x      = stretched.x;
    last.y      = stretched.y;
    stretched.x = p.x;
    stretched.y = p.y;
    
    if(setGraphics() != null) {
      if(firstStretch == true) firstStretch = false;
      else                     drawLast(graphics);
      drawNext(graphics);
    }
  }
  public void end(Point p) {
    last.x = end.x = p.x;
    last.y = end.y = p.y;
    if (setGraphics() != null) { drawLast(graphics); }
  }
  public Rectangle bounds() {
    return new Rectangle(stretched.x < anchor.x ? 
			 stretched.x : anchor.x,
			 stretched.y < anchor.y ? 
			 stretched.y : anchor.y,
			 Math.abs(stretched.x - anchor.x),
			 Math.abs(stretched.y - anchor.y));
  }
  
  public Rectangle lastBounds() {
    return new Rectangle(last.x < anchor.x ? last.x : anchor.x,
			 last.y < anchor.y ? last.y : anchor.y,
			 Math.abs(last.x - anchor.x),
			 Math.abs(last.y - anchor.y));
  }
  private Graphics setGraphics() {
    Color color = Color.black;
    if (component != null) { 
      if (graphics == null) { graphics = component.getGraphics(); }
      color = component.getBackground();
    }
    else if (image != null) {
      if (graphics == null) { graphics = image.getGraphics(); }
    }
    if(graphics != null) {
      graphics.setXORMode(color);
    }
    return graphics;
  }
}

