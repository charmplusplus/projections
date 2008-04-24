package projections.gui.Timeline;

import java.awt.*;

import javax.swing.*;

import projections.gui.MainWindow;

public class UserEventObject extends JComponent implements Comparable
{

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
	private Color  color;
	public String Name;
	private Data data;
	
	private int pe;

	public UserEventObject(int pe, long t, int e, int event, int type) {
		setFocusable(false); // optimization for speed
		Type=type;
		BeginTime=EndTime=t;
		UserEventID=e;
		CharmEventID=event;
		color=MainWindow.runObject[myRun].getUserEventColor(UserEventID);
		if(color == null)
			color = Color.white;
		Name=MainWindow.runObject[myRun].getUserEventName(UserEventID);
		this.pe = pe;
	}

	
	/** Called by the layout manager to put this in the right place */
	public void setLocationAndSize(Data data, int actualDisplayWidth) {
		this.data = data;

		int leftCoord = data.timeToScreenPixel(BeginTime, actualDisplayWidth);
		int rightCoord = data.timeToScreenPixel(EndTime, actualDisplayWidth);

		if(EndTime > data.endTime())
			rightCoord = data.timeToScreenPixel(data.endTime(), actualDisplayWidth) - 5;

		if(BeginTime < data.beginTime())
			leftCoord = data.timeToScreenPixel(data.beginTime(), actualDisplayWidth) + 5;
		
		int width = rightCoord-leftCoord+1;
		
		/** The y coordinate of the top of the rectangle */
		int rectHeight = data.userEventRectHeight();
		double yTop = ((double)verticalDisplayPosition()+0.5)*data.singleTimelineHeight() - data.barheight()/2 - rectHeight;
		
		this.setBounds( leftCoord,  
						(int)yTop,
						width, 
						rectHeight );

	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if(data.showUserEvents()){
			g.setColor(color);
			g.fillRect(0, 0, getWidth(), getHeight());
		}		
		
	}

	public Color getColor() {
		return color;
	}

	public void shiftTimesBy(long shift) {
		BeginTime += shift;
		EndTime += shift;
	}

	public int compareTo(Object o) {
		UserEventObject ueo = (UserEventObject) o;
		if(pe != ueo.pe){
			return pe - ueo.pe;
		}
		else{
			return (int) (BeginTime - ueo.BeginTime);
		}	
	}

	/** The position in the ordering of PEs displayed */
	public int verticalDisplayPosition(){
		return data.whichTimelineVerticalPosition(pe);		
	}
	
	
}


