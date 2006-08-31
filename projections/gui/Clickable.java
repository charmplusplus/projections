package projections.gui;

import java.awt.event.*;

public interface Clickable extends ResponsiveToMouse {
    public void toolClickResponse(MouseEvent e, int xVal, int yVal);
}
