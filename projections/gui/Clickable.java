package projections.gui;

import java.awt.event.*;

public interface Clickable extends ResponsiveToMouse {
    public void toolClickResponse(MouseEvent e, int xVal, int yVal);
    public void toolMouseMovedResponse(MouseEvent e, int xVal, int yVal);
}
