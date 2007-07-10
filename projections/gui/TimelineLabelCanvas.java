package projections.gui;

import java.awt.*;

public class TimelineLabelCanvas extends Canvas
{  
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    private TimelineData data;
    private FontMetrics fm;
    private Image offscreen;
    
    public TimelineLabelCanvas(TimelineData data)
    {
	this.data = data;
	setBackground(MainWindow.runObject[myRun].background);
	setForeground(MainWindow.runObject[myRun].foreground);
    }   

    public void makeNewImage()
    {
	offscreen = null;
	
	if (data.lcw > 0 && data.tlh > 0) {
	    offscreen = createImage(data.lcw, data.tlh);
	    
	    if (offscreen == null)
		return;
	    
	    Graphics og = offscreen.getGraphics();
	    og.setClip(0, 0, data.lcw, data.tlh);
	    
	    Color oldColor = og.getColor();
	    og.setColor(MainWindow.runObject[myRun].background);
	    og.fillRect(0, 0, getSize().width, getSize().height);
	    //og.fillRect(0, 0, data.lcw, data.tlh);
	    og.setColor(oldColor);
	    
	    if (fm == null) {
		fm = og.getFontMetrics(og.getFont());
		data.lcw = Math.max(fm.stringWidth("PE 999999") + 15,
				    fm.stringWidth("(999,999)") + 20);
	    }
	    
	    og.setColor(MainWindow.runObject[myRun].foreground);
	    data.processorList.reset();
	    /*
	    NumberFormat df = NumberFormat.getInstance();
	    df.setMinimumFractionDigits(1);
	    df.setMaximumFractionDigits(1);
	    */
	    for (int p=0; p<data.numPs; p++) {
		String tmp = "PE "+data.processorList.nextElement();
		og.drawString(tmp, 10, data.tluh/2 + p*data.tluh);
		tmp = "(" + 
		    (int)(100 - data.idleUsage[p]) + "," +
		    (int)(data.processorUsage[p]) + ")";
		og.drawString(tmp, 15, data.tluh/2 + p*data.tluh + 
			      fm.getHeight() + 2);
	    }
	    og.dispose();
	    repaint();
	}   
    }   
 
    public void paint(Graphics g)
    {
	if (offscreen != null) {
	    int y = data.timelineWindow.getVSBValue();
	    g.drawImage(offscreen, 0,0, data.lcw, data.vph, 
			0,y, data.lcw, y + data.vph, null);
	}                        
    }   

    public void update(Graphics g)
    {
	paint(g);
    }   
}

