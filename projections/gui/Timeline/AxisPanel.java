package projections.gui.Timeline;

import java.text.DecimalFormat;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import javax.swing.*;

import projections.gui.Util;

import java.awt.FontMetrics;

public class AxisPanel extends JPanel 
{

	/** Desired height of the whole JPanel */
	private int totalHeight() {
		if(data.useMinimalView())
			return axispos()+largeTickHalfLength;
		else
			return 14+10+axispos()+largeTickHalfLength;
	}
	
	/** Distance from top to the horizontal line **/
	private int axispos() {
		if(data.useMinimalView())
			return 2 + largeTickHalfLength + textpos();
		else
			return 5 + largeTickHalfLength + textpos();
	}
	
	/** Distance from top to the bottom of the text label */
	public int axisLabelPositionY(){
		if(data.useMinimalView())
			return 0;
		else
			return 15;
	}
		
	/** Distance from top to the baseline for the timestamps */
	private int textpos() {
		if(data.useMinimalView())
			return data.axisFont.getSize()+axisLabelPositionY();
		else
			return 5+data.axisFont.getSize()+axisLabelPositionY();
	}
	
	/** The distance the small tick marks extend from the horizontal line */
	private int smallTickHalfLength = 2;

	/** The distance the large tick marks extend from the horizontal line */
	private int largeTickHalfLength = 5;

	private Data  data;

	private DecimalFormat format_= new DecimalFormat();

	public AxisPanel(Data data)
	{
		this.data = data;
		format_.setGroupingUsed(true);
	}   


	/** Get the preferred size. The Width provided should be ignored */
	public Dimension getPreferredSize() {
		int preferredWidth = 200;
		int preferredHeight = totalHeight();
		return new Dimension(preferredWidth, preferredHeight);
	}

	/** Paint the axis in its panel */

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		g.setFont(data.axisFont);
		FontMetrics fm = g.getFontMetrics();
		
		g.setColor(data.getBackgroundColor());
		Rectangle clipBounds = g.getClipBounds();
		g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

		int maxx = getWidth();

		g.setColor(data.getForegroundColor());
		g.drawLine(data.offset(), axispos(), maxx-data.offset(), axispos());

		// Draw the tick marks and timestamps
		for (int x=0; x<numIntervals(); x++) {
			
			/** the coordinate in the panel */
			int curx = data.leftOffset() + (int)(x*pixelsPerTickMark()) ;

			if (curx > maxx) {
				break;
			}
			
			if (x % labelIncrement() == 0) {  
				String tmp = format_.format(data.screenToTime(curx));				
				g.drawLine(curx, axispos()-largeTickHalfLength, curx, axispos() + largeTickHalfLength);
				g.drawString(tmp, curx - fm.stringWidth(tmp)/2, textpos());
			} else {
				g.drawLine(curx, axispos()-smallTickHalfLength, curx, axispos()+smallTickHalfLength);
			}
			
		}
		
		
		// Draw the label for the axis
		g.drawString(axisLabel(), getWidth()/2 - fm.stringWidth(axisLabel())/2, axisLabelPositionY());

	}


	public String axisLabel(){
		return "Time In Microseconds";
	}
	
	
	/** The number of ticks we can display on the timeline in the given sized window */
	public int numIntervals(){
		return (int) Math.ceil(data.totalTime() / timeIncrement(getWidth())) + 1;
	}   

	/** The number of tickmarks between the labeled big ticks */
	public int labelIncrement() {
		return (int) Util.getBestIncrement((int)(Math.ceil(data.maxLabelLen() / pixelsPerTickMark())));
	}
	
	/** Number of microseconds per tickmark */
	public int timeIncrement(int actualDisplayWidth){
		return Util.getBestIncrement( (int) Math.ceil(5 / ( (double) data.lineWidth(actualDisplayWidth) / data.totalTime() )  ) );
	}

	/** number of pixels per tick mark */
	public double pixelsPerTickMark(){
		return  ((double) data.lineWidth(getWidth())) / ((double)numIntervals());
	}

}
