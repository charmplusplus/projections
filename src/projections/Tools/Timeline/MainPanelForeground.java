package projections.Tools.Timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;


/** This class displays lines for each message send.
 * 
 *  This panel should be added as the front most object in the MainPanel.
 * 
 *  This class does not handle any events, so hopefully the mouse events will make it through to the other objects.
 *  
 *  The bounds and position of this object is set by the custom layout manager for the MainPanel(see MainLayout).
 *
 */

class MainPanelForeground extends JPanel {

	private Data data;
		
	protected MainPanelForeground(Data data){
		this.data = data;
		setOpaque(false);
	}
	

	/** Paint the panel, filling the entire panel's width */
	public void paintComponent(Graphics g) {
		// Let UI delegate paint first 
		// (including background filling, if I'm opaque)
	//	super.paintComponent(g); 
		// paint the message send lines	
//		paintMessageSendLines(g, data.getMessageColor(), data.getBackgroundColor(), data.drawMessagesForTheseObjects);
	//	paintMessageSendLines(g, data.getMessageAltColor(), data.getBackgroundColor(), data.drawMessagesForTheseObjectsAlt);	
	}


	
	private void paintMessageSendLines(Graphics g, Color c, Color bgColor, Set drawMessagesForObjects){
		Graphics2D g2d = (Graphics2D) g;
		// paint the message send lines
		if (drawMessagesForObjects.size()>0) {
			Iterator iter = drawMessagesForObjects.iterator();
			while(iter.hasNext()){
				Object o = iter.next();
				if(o instanceof EntryMethodObject){
					EntryMethodObject obj = (EntryMethodObject)o;
					if(obj.creationMessage() != null){
						int pCreation = obj.pCreation;
						int pExecution = obj.pe;
						
						// Message Creation point
						int x1 = data.timeToScreenPixel(obj.creationMessage().Time);			
						double y1 = data.messageSendLocationY(pCreation);
						// Message executed (entry method starts) 
						int x2 =  data.timeToScreenPixel(obj.getBeginTime());
						double y2 = data.messageRecvLocationY(pExecution);

						// Draw thick background Then thin foreground
						g2d.setPaint(bgColor);
						g2d.setStroke(new BasicStroke(4.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);
						g2d.setPaint(c);
						g2d.setStroke(new BasicStroke(2.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);
					}
				}
			}
		}

	}
	
}
