package projections.analysis;

import java.awt.*;
import projections.gui.*;

public class UserEvent extends Component
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

  public static final int SINGLE=1;   // if this just marks one point in time
  public static final int PAIR=2;  // if this has a begin and end point

  public int    Type;
  public long   BeginTime;
  public long   EndTime;
  public int    UserEventID;  
  public int    CharmEventID; // for matching with end time
  public Color  color;
  public String Name;
  public TimelineData data;
  public int width;

  public UserEvent(long t, int e, int event, int type) {
    Type=type;
    BeginTime=EndTime=t;
    UserEventID=e;
    CharmEventID=event;
    color=MainWindow.runObject[myRun].getUserEventColor(UserEventID);
    Name=MainWindow.runObject[myRun].getUserEventName(UserEventID);
  }

  public void setBounds(int ylocation, TimelineData data) {
    this.data = data;
    long BT, ET;
    
    if(EndTime > data.endTime)
      ET = data.endTime - data.beginTime;
    else
      ET = EndTime - data.beginTime;
    
    if(BeginTime < data.beginTime) 
      BT = 0;
    else BT = BeginTime - data.beginTime;
    
    int BTS  = data.offset + (int)(BT*data.pixelIncrement/data.timeIncrement);
    int ETS  = data.offset + (int)(ET*data.pixelIncrement/data.timeIncrement);
    int LENS = ETS - BTS + 1; 
    if(LENS < 1) LENS = 1;
    
    if(EndTime > data.endTime) LENS += 5;
    if(BeginTime < data.beginTime)
      {
	BTS  -= 5;
	LENS += 5;
      }
    
    super.setBounds(BTS,  data.tluh/2 + ylocation*data.tluh - data.barheight/2,
		    LENS, 5);
    
    width = LENS;
  }
 
  public void paint(Graphics g) {
    g.setColor(color);
    g.fillRect(0, 0, width, 5);
  }
}


