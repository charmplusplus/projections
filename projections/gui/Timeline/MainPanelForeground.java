package projections.gui.Timeline;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Iterator;
import java.util.Set;

import javax.swing.*;


/** This class displays lines for each message send.
 * 
 *  This panel should be added as the front most object in the MainPanel.
 * 
 *  This class does not handle any events, so hopefully the mouse events will make it through to the other objects.
 *  
 *  The bounds and position of this object is set by the custom layout manager for the MainPanel(see MainLayout).
 *
 */

public class MainPanelForeground extends JPanel {

	private static final long serialVersionUID = 1L;
	
	Data data;
		
	public MainPanelForeground(Data data){
		this.data = data;
	}
	

	/** Paint the panel, filling the entire panel's width */
	protected void paintComponent(Graphics g) {
		// paint the message send lines	
		paintMessageSendLines(g, data.getMessageColor(), data.drawMessagesForTheseObjects);
		paintMessageSendLines(g, data.getMessageAltColor(), data.drawMessagesForTheseObjectsAlt);
	}


	
	public void paintMessageSendLines(Graphics g, Color c, Set drawMessagesForObjects){
		// paint the message send lines
		if (drawMessagesForObjects.size()>0) {
			g.setColor(c);
			Iterator iter = drawMessagesForObjects.iterator();
			while(iter.hasNext()){
				Object o = iter.next();
				if(o instanceof EntryMethodObject){
					EntryMethodObject obj = (EntryMethodObject)o;
					if(obj.creationMessage() != null){
						int pCreation = obj.pCreation;
						int pExecution = obj.pCurrent;
						// Find the index for the PEs in the list of displayed PEs
//						int startpe_index=data.whichTimelineVerticalPosition(pCreation);
//						int endpe_index=data.whichTimelineVerticalPosition(pExecution);
						
//						data.dumpPEOrder();
						
						// Message Creation point
						int x1 = data.timeToScreenPixelLeft(obj.creationMessage().Time, getWidth());			
						double y1 = data.messageSendLocationY(pCreation);
						// Message executed (entry method starts) 
						int x2 =  data.timeToScreenPixel(obj.getBeginTime(), getWidth());
						double y2 = data.messageSendLocationY(pExecution);
						// I like painting a line :)
						g.drawLine(x1,(int)y1,x2,(int)y2);
					}
				}
			}
		}

	}
	
}
