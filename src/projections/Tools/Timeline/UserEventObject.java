package projections.Tools.Timeline;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import projections.Tools.Timeline.RangeQueries.Range1D;
import projections.gui.MainWindow;
import projections.misc.MiscUtil;

public class UserEventObject implements Comparable, Range1D, ActionListener, MainPanel.SpecialMouseHandler
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	public enum Type {
		SINGLE, // if this just marks one point in time
		PAIR // if this has a begin and end point
	}
	
	protected Type    type;         // should be SINGLE or PAIR
	public long   beginTime;    // Begin Time
	public long   endTime;      // End Time
	public int    userEventID;  // The user supplied value used to distinguish different types of user events
	public int    charmEventID; // for matching with end time
	
	private int pe;
	private int nestedID = -1; // Nested thread ID, e.g. virtual AMPI ranks

	private String note = null;
	
	private final static String popupChangeColor = "Change Color";

	/** If displaying nested user events in multiple rows, use this value to determine the row in which we draw this event */
	private int nestedRow;
	
	public UserEventObject(int pe, long t, int e, int event, Type type) {
		this.type=type;
		this.beginTime=endTime=t;
		this.userEventID=e;
		this.charmEventID=event;
		this.pe = pe;
	}
	
	/** Create a user event that is a note */
	public UserEventObject(int pe, long t, String note) {
		this.beginTime=endTime=t;
		this.pe = pe;
		this.note = note;
		this.userEventID=-1;
	}

	/** Create a user event that has as nested ID (such as a virtual AMPI rank) */
	public UserEventObject(int pe, long t, int e, int event, Type type, int nestedID) {
		this.type=type;
		this.beginTime=endTime=t;
		this.userEventID=e;
		this.charmEventID=event;
		this.pe = pe;
		this.nestedID = nestedID;
	}
	
	
	public UserEventObject(int pe, long t, int e, int event, Type type, String note) {
		this.type=type;
		this.beginTime=endTime=t;
		this.userEventID=e;
		this.charmEventID=event;
		this.pe = pe;
		this.note = note;
	}

	public String getName(){
		String name = "";
		
		boolean addNewline = false;
		
		String userEventName = MainWindow.runObject[myRun].getUserEventName(userEventID);
		if(userEventName != null){
			name += userEventName;
			if (nestedID >= 0)
				name += "(VP " + nestedID + ")";
			addNewline = true;
		}

		if(note != null){
			if(addNewline)
				name += "\n";	
			name += note;
		}
		
		return name;
	}		
	
	protected Color getColor(Data data){	
		Color c = MainWindow.runObject[myRun].getUserEventColor(userEventID);
		if(c != null)
			return c;
		else 
			return data.getForegroundColor();
	}
	
	public int getNestedID() {
		return nestedID;
	}

	protected void paintMe(Graphics2D g, int actualDisplayWidth, Data data, final int baseBottomCoord) {

		if(data.userEventIsHiddenID(userEventID)){
			return;
		}

		
		int leftCoord = data.timeToScreenPixel(beginTime, actualDisplayWidth);
		int rightCoord = data.timeToScreenPixel(endTime, actualDisplayWidth);

		if(endTime > data.endTime())
			rightCoord = data.timeToScreenPixelRight(data.endTime(), actualDisplayWidth);

		if(beginTime < data.startTime())
			leftCoord = data.timeToScreenPixelLeft(data.startTime(), actualDisplayWidth);
		
		int width = rightCoord-leftCoord+1;

		if(width < 1)
			width = 1;

		int height = data.singleUserEventRectHeight();
		int topCoord = baseBottomCoord - (1+nestedRow) * height;
		int bottomCoord = topCoord+height-1;
		
		Color c = getColor(data);
		g.setColor(c);
		g.fillRect(leftCoord, topCoord, width, height);

		// Paint the left/right edges of the rectangle lighter/darker to help differentiate between adjacent same-colored objects
		if(width > 1)
		{	
			g.setColor(c.brighter());
			g.drawLine(leftCoord, topCoord, leftCoord, bottomCoord);
			g.setColor(c.darker());
			g.drawLine(rightCoord, topCoord, rightCoord, bottomCoord);
		}

		final int leftpad = 3;
		final int rightpad = 3;
		final int toppad = 1;
		final int bottompad = 1;
		final int fontsize = height - toppad - bottompad;

		// Draw the name of the user event
		if(fontsize >= 9 && getName() != null){
			g.setFont(data.labelFont);
			FontMetrics fm = g.getFontMetrics();
			int stringWidth = fm.stringWidth(getName());		

			if(stringWidth < width - leftpad - rightpad){
				g.setColor(Color.black);
				g.drawString(getName(), leftCoord+leftpad, topCoord+toppad + fontsize);

//				g.setPaintMode();
			}
		}


	}



	/** Dynamically generate the tooltip mouseover text when needed */
	public String getToolTipText(){
		if(note == null) 
			return "<html><body><p><i>User Traced Event:</i> <b>" + getName() + "</b></p><p><i>Duration:</i> " + (endTime-beginTime) + " us</p><p><i>event:</i> " + userEventID + "</p><p><i>occurred on PE:</i> " + pe + "</p></html></body>";
		else if(endTime - beginTime <= 0)
			return "<html><body><p><i>User Supplied Note:</i></p><p></p>" + note + "</html></body>";
		else
			return "<html><body><p><i>User Supplied Note:</i></p><p></p>" + note + "<p><i>Duration</i>: " + (endTime-beginTime) + "us</p></html></body>";
	}
	
	
	@Override
	public void shiftTimesBy(long shift) {
		beginTime += shift;
		endTime += shift;
	}

	@SuppressWarnings("ucd")
	public int compareTo(Object o) {
		UserEventObject ueo = (UserEventObject) o;
		if(pe != ueo.pe){
			return pe - ueo.pe;
		}
		else if (beginTime != ueo.beginTime) {
			return MiscUtil.sign(beginTime - ueo.beginTime);	
		} else if (endTime != ueo.endTime) {
			return MiscUtil.sign(ueo.endTime - endTime);
		} else if (this != ueo) {
			return MiscUtil.sign(this.userEventID - ueo.userEventID);
		} else {
			System.err.println("ERROR: compareTo not working correctly for class UserEventObject");
			return 0;
		}
	
	}

	public void setNestedRow(int row) {
		nestedRow = row;
	}

	public int getNestedRow() {
		return nestedRow;
	}

	Data dataForLastClick;
	

	public void mouseClicked(MouseEvent evt, JPanel parent, Data data) {
		dataForLastClick = data;
		if (evt.getModifiers()==MouseEvent.BUTTON1_MASK) {
			// Left Click
		} else {	
			// non-left click: display popup menu
			JPopupMenu popup = new JPopupMenu();

			JMenuItem menuItem = new JMenuItem(popupChangeColor);
			menuItem.addActionListener(this);
			popup.add(menuItem);

			popup.show(parent, evt.getX(), evt.getY());			
		}
		
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
//
//	public void mousePressed(MouseEvent e) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public void mouseReleased(MouseEvent e) {
//		// TODO Auto-generated method stub
//		
//	}
//
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem) e.getSource()).getText();
			
			if (arg.equals(popupChangeColor)){
				Color c = JColorChooser.showDialog(null, "Choose color for " + getName(), getColor(dataForLastClick)); 
				if(c !=null){
					MainWindow.runObject[myRun].setUserEventColor(userEventID, c);
					dataForLastClick.displayMustBeRepainted();
				}

			}

		}
	}

	@Override
	public long lowerBound() {
		return beginTime;
	}

	@Override
	public long upperBound() {
		return endTime;
	}

	@Override
	public void mouseMoved(MouseEvent evt) {
		// TODO Auto-generated method stub
		
	}

}


