package projections.gui;
import java.awt.event.*;
import javax.swing.*;

/**
 *  ProjectionsWindow
 *  written by Chee Wai Lee
 *  6/28/2002
 *  changed by Sindhura Bandhakavi
 *  8/1/2002
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

public abstract class ProjectionsWindow 
    extends JFrame
{
    protected RangeDialog dialog;
    protected boolean isDialogCancelled = true;
    abstract void showDialog();

// variables to be set by RangeDialog
    protected OrderedIntList validPEs;
    protected long startTime;
    protected long endTime;

// functions for RangeDialog to work
    protected void dialogCancelled(boolean state)         { isDialogCancelled = state;}
    protected void setProcessorRange(OrderedIntList proc) { validPEs = proc; }
    protected void setStartTime(long time)                { startTime = time; }
    protected void setEndTime(long time)                  { endTime = time; }

// constructor 
    public ProjectionsWindow(){
  	  addWindowListener(new WindowAdapter(){
                 public void windowClosing(WindowEvent e){
			  close();
                 }
          });
    }

// close the window	
    public void close(){
		dispose();
    }

}

