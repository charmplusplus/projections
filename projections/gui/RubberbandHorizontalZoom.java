package projections.gui;

import java.awt.*;

/** JMU: I took this from  Java in a nutshell (I THINK!) and then modified */

public class RubberbandHorizontalZoom extends Rubberband {
  int height = 10;
  public RubberbandHorizontalZoom(Component component) { 
    super(component); 
    height = component.getHeight();
  }
  public RubberbandHorizontalZoom(Image image) { 
    super(image); 
    height = image.getHeight(null);
    if (height == -1) { height = 1000; System.out.println("HEIGHT -1"); }
  }
  public void drawLast(Graphics graphics) {
    Rectangle rect = lastBounds();
    graphics.setColor(Color.yellow);
    graphics.drawRect(rect.x, -1, rect.width, height+1);
  }
  public void drawNext(Graphics graphics) {
    Rectangle rect = bounds();
    Rectangle lastRect = lastBounds();
    graphics.setColor(Color.yellow);
    graphics.drawRect(rect.x, -1, rect.width, height+1);
  }
}
     
