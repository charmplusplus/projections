package projections.gui;

import projections.gui.*;

import java.awt.*;
import javax.swing.*;

/**
 *  ProjectionsWindow
 *  written by Chee Wai Lee
 *  6/28/2002
 *
 *  This class should be inherited by all projections tools that present
 *  some kind of main window and show a dialog box requiring the user to
 *  input a range of processors and a time interval.
 *
 *  The author would need to implement the following methods to support
 *  an interface to work with the basic RangeDialog object which presents
 *  to the user a dialog interface.
 *
 *  The author would also need to implement any extra data setting fields
 *  for the specific projections tool to interface correctly with the
 *  corresponding dialog (which should be a subclass of RangeDialog).
 *
 */
// changed the ProjectionsWindow to extend JFrame instead of Frame - Sindhura
public abstract class ProjectionsWindow 
    extends JFrame
{
    RangeDialog dialog;
    boolean isDialogCancelled = true;

    abstract void showDialog();
    void dialogCancelled(boolean state){ isDialogCancelled = state;}

    public abstract void setProcessorRange(OrderedIntList validPEs);
    public abstract void setStartTime(long time);
    public abstract void setEndTime(long time);
}

