package projections.gui.Timeline;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import projections.gui.MainWindow;

public class UserEventObject extends JComponent implements Comparable, MouseListener
{

	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	static int myRun = 0;

	public static final int SINGLE=1;   // if this just marks one point in time
	public static final int PAIR=2;  // if this has a begin and end point

	public int    Type;         // should be SINGLE or PAIR
	public long   BeginTime;    // Begin Time
	public long   EndTime;      // End Time
	public int    UserEventID;  // The user supplied value used to distinguish different types of user events
	public int    CharmEventID; // for matching with end time

	private Data data;
	
	private int pe;

	/** If displaying nested user events in multiple rows, use this value to determine the row in which we draw this event */
	private int nestedRow;
	
	public UserEventObject(int pe, long t, int e, int event, int type) {
		setFocusable(false); // optimization for speed
		this.Type=type;
		this.BeginTime=EndTime=t;
		this.UserEventID=e;
		this.CharmEventID=event;
		this.pe = pe;
		
		setToolTipText("<html><body><p>" + getName() + "</p><p>Duration: " + (EndTime-BeginTime) + " us</p></html></body>");
	
		addMouseListener(this);
	}

	public String getName(){
		return MainWindow.runObject[myRun].getUserEventName(UserEventID);
	}
	
	public Color getColor(){	
		Color c = MainWindow.runObject[myRun].getUserEventColor(UserEventID);
		if(c != null)
			return c;
		else 
			return Color.white;
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
		
//		/** The y coordinate of the top of the rectangle */
//		int rectHeight = data.userEventRectHeight();
//		int yTop = data.userEventLocationTop(pe);
//			
		
		// Do the layout to account for multiple rows
		
		int heightPerRow = data.userEventRectHeight() / data.getNumUserEventRows();
		int bottom = data.userEventLocationBottom(pe) +   - heightPerRow * ( this.nestedRow );
		
		int top = bottom - heightPerRow;
				
		this.setBounds( leftCoord, 
						top,
						width,
						heightPerRow );

	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if(data.showUserEvents()){
			g.setColor(getColor());
		
//			int height = getHeight() / data.getNumUserEventRows();
//			
//			int bottom =  getHeight() - height * ( this.nestedRow );
//			int top = bottom - height;
//			int top = 0 + height * (data.getNumUserEventRows() - this.nestedRow - 1);
//			System.out.println("height="+height+ " top=" + top + " getHeight()=" + getHeight() + " getNumUserEventRows="+data.getNumUserEventRows());
			
			g.fillRect(0, 0, getWidth(), getHeight());
						
			// Draw the name of the user event
			if(getName() != null){
				int leftpad = 3;
				int rightpad = 3;
				int toppad = 1;
				int bottompad = 1;
				int fontsize = getHeight() - toppad - bottompad;

				g.setFont(data.labelFont);
				FontMetrics fm = g.getFontMetrics();
				int stringWidth = fm.stringWidth(getName());		

				if( fontsize >=9 && stringWidth < getWidth() - leftpad - rightpad){
					g.setColor(Color.black);
					g.drawString(getName(), leftpad, toppad + fontsize);
					
					g.setPaintMode();
				}
			}		
			
		}		
		
		
		
		
		
		
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


	public void setNestedRow(int row) {
		nestedRow = row;
	}

	public void mouseClicked(MouseEvent e) {
		System.out.println("Mouse Clicked on user event object");
		 Color c = JColorChooser.showDialog(null, "Choose color for " + getName(), getColor()); 
		 MainWindow.runObject[myRun].setUserEventColor(UserEventID, c);
		 data.displayMustBeRepainted();
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
}


