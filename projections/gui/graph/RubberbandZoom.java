package projections.gui.graph;

import java.awt.*;
import projections.gui.Rubberband;

/** JMU: I took this from  Java in a nutshell (I THINK!) and then modified */

public class RubberbandZoom extends Rubberband {
  public RubberbandZoom(Component component) { 
    super(component); 
  }

  public void drawHighlight(Graphics graphics) { }
  public void clearHighlight(Graphics graphics) { }
     
  public void drawLast(Graphics graphics) {
    Rectangle rect = lastBounds();
    graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
  }
  public void drawNext(Graphics graphics) {
    Rectangle rect = bounds();
    graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
  }
}
