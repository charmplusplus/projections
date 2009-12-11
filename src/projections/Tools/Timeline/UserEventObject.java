package projections.Tools.Timeline;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import projections.gui.MainWindow;

public class UserEventObject extends JComponent implements Comparable, MouseListener,  ActionListener
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	public static final int SINGLE=1;   // if this just marks one point in time
	public static final int PAIR=2;  // if this has a begin and end point

	protected int    Type;         // should be SINGLE or PAIR
	public long   BeginTime;    // Begin Time
	public long   EndTime;      // End Time
	public int    UserEventID;  // The user supplied value used to distinguish different types of user events
	public int    CharmEventID; // for matching with end time

	private Data data;
	
	private int pe;

	private String note = null;
	
	private final static String popupChangeColor = "Change Color";

	/** If displaying nested user events in multiple rows, use this value to determine the row in which we draw this event */
	private int nestedRow;
	
	public UserEventObject(int pe, long t, int e, int event, int type) {
		setFocusable(false); // optimization for speed
		this.Type=type;
		this.BeginTime=EndTime=t;
		this.UserEventID=e;
		this.CharmEventID=event;
		this.pe = pe;
		setOpaque(false);
		addMouseListener(this);
	}
	
	/** Create a user event that is a note */
	public UserEventObject(int pe, long t, String note) {
		setFocusable(false); // optimization for speed		
		this.BeginTime=EndTime=t;
		this.pe = pe;
		this.note = note;
		this.UserEventID=-1;
		setOpaque(false);
		addMouseListener(this);
	}


	
	
	public UserEventObject(int pe, long t, int e, int event, int type, String note) {
		setFocusable(false); // optimization for speed
		this.Type=type;
		this.BeginTime=EndTime=t;
		this.UserEventID=e;
		this.CharmEventID=event;
		this.pe = pe;
		this.note = note;

		addMouseListener(this);
	}

	public String getName(){
		String name = "";
		
		boolean addNewline = false;
		
		String userEventName = MainWindow.runObject[myRun].getUserEventName(UserEventID);
		if(userEventName != null){
			name += userEventName;
			addNewline = true;
		}

		if(note != null){
			if(addNewline)
				name += "\n";	
			name += note;
		}
		
		return name;
	}		
	
	public Color getColor(){	
		Color c = MainWindow.runObject[myRun].getUserEventColor(UserEventID);
		if(c != null)
			return c;
		else 
			return data.getForegroundColor();
	}
	
	
	/** Called by the layout manager to put this in the right place */
	protected void setLocationAndSize(Data data, int actualDisplayWidth) {
		this.data = data;

		if(data.userEventIsHiddenID(UserEventID) || (data.userSuppliedNotesHidden() && UserEventID==-1)){
			setBounds( 0, 0, 0, 0 );			
			return;
		}	
		
		int left = data.timeToScreenPixel(BeginTime, actualDisplayWidth);
		int rightCoord = data.timeToScreenPixel(EndTime, actualDisplayWidth);

		if(EndTime > data.endTime())
			rightCoord = data.timeToScreenPixel(data.endTime(), actualDisplayWidth) - 5;

		if(BeginTime < data.startTime())
			left = data.timeToScreenPixel(data.startTime(), actualDisplayWidth) + 5;
		
		int width = rightCoord-left+1;
		
		
		// Do the layout to account for multiple rows
		
		int heightPerRow = data.userEventRectHeight() / data.getNumUserEventRows();
		int bottom = data.userEventLocationBottom(pe);

		if(data.drawNestedUserEventRows){
			 bottom -= heightPerRow * ( nestedRow );
		}
		
		int top = bottom - heightPerRow;
		int height = heightPerRow;
		
		// Use a very large height if this is meant to span all PE timelines
		if(getName().contains("***")){
			top = 3;
			height = data.screenHeight()-top;
		}
		
		if(note == null)
			setToolTipText("<html><body><p><i>User Traced Event:</i> <b>" + getName() + "</b></p><p><i>Duration:</i> " + (EndTime-BeginTime) + " us</p><p><i>event:</i> " + UserEventID + "</p><p><i>occurred on PE:</i> " + pe + "</p></html></body>");
		else
			setToolTipText("<html><body><p><i>User Supplied Note:</i></p><p></p>" + note + "</html></body>");
			
		setBounds( left, top, width, height );
				
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if(data.userEventIsHiddenID(UserEventID)){
			return;
		}
		
		
		if(data.showUserEvents()){
			Color c = getColor();
			g.setColor(c);
		
			g.fillRect(0, 0, getWidth(), getHeight());
						
			
			// Paint the left/right edges of the rectangle lighter/darker to help differentiate between adjacent same-colored objects
			if(getWidth() > 1)
			{	
				g.setColor(c.brighter());
				g.drawLine(0, 0, 0, getHeight());
				g.setColor(c.darker());
				g.drawLine(getWidth()-1, 0, getWidth()-1, getHeight()-1);
			}
			
			
			
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

	protected void shiftTimesBy(long shift) {
		BeginTime += shift;
		EndTime += shift;
	}

	public int compareTo(Object o) {
		UserEventObject ueo = (UserEventObject) o;
		if(pe != ueo.pe){
			return pe - ueo.pe;
		}
		else if (BeginTime != ueo.BeginTime) {
			return (int) (BeginTime - ueo.BeginTime);
		} else if (EndTime != ueo.EndTime) {
			return (int) (ueo.EndTime - EndTime);
		} else if (this != ueo) {
			return this.UserEventID - ueo.UserEventID;
		} else {
			System.err.println("ERROR: compareTo not working correctly for class UserEventObject");
			return 0;
		}
	
	}



	public void setNestedRow(int row) {
		nestedRow = row;
	}

	public void mouseClicked(MouseEvent evt) {
		
		if (evt.getModifiers()==MouseEvent.BUTTON1_MASK) {
			// Left Click
		} else {	
			// non-left click: display popup menu
			JPopupMenu popup = new JPopupMenu();

			JMenuItem menuItem = new JMenuItem(popupChangeColor);
			menuItem.addActionListener(this);
			popup.add(menuItem);

			popup.show(this, evt.getX(), evt.getY());			
		}
		
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

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem) e.getSource()).getText();
			
			if (arg.equals(popupChangeColor)){
				Color c = JColorChooser.showDialog(null, "Choose color for " + getName(), getColor()); 
				if(c !=null){
					MainWindow.runObject[myRun].setUserEventColor(UserEventID, c);
					data.displayMustBeRepainted();
				}

			}

		}
	}
	
	
}


