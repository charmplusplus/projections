package projections.Tools.Timeline;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;

import javax.swing.JPanel;
import javax.swing.Scrollable;

import projections.Tools.Timeline.Data.ViewType;
import projections.gui.Util;

/** The class that draws the top time axis on the top of the timeline window */
class AxisPanel extends JPanel implements Scrollable, MouseListener, MouseMotionListener 
{

	/** Desired height of the whole JPanel */
	private int totalHeight() {
		if(data.getViewType() == ViewType.VIEW_MINIMAL)
			return axispos()+largeTickHalfLength;
		else
			return 14+axispos()+largeTickHalfLength;
	}
	
	/** Distance from top to the horizontal line **/
	private int axispos() {
		if(data.getViewType() == ViewType.VIEW_MINIMAL)
			return 2 + largeTickHalfLength + textpos();
		else
			return 5 + largeTickHalfLength + textpos();
	}
	
	/** Distance from top to the bottom of the text label */
	private int axisLabelPositionY(){
		if(data.getViewType() == ViewType.VIEW_MINIMAL)
			return 0;
		else
			return 15;
	}
		
	/** Distance from top to the baseline for the timestamps */
	private int textpos() {
		if(data.getViewType() == ViewType.VIEW_MINIMAL)
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

	protected AxisPanel(Data data)
	{
		this.data = data;
		format_.setGroupingUsed(true);

	
		addComponentListener(new MyListener());
		
		addMouseListener(this);
		addMouseMotionListener(this);
	
	}   


	

	/** Paint the axis in its panel */

	public void paintComponent(Graphics g)
	{
		synchronized(data){

			g.setFont(data.axisFont);
			FontMetrics fm = g.getFontMetrics();

			g.setColor(data.getBackgroundColor());
			Rectangle clipBounds = g.getClipBounds();
			g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

			int maxx = getWidth();

			// Determine the left and right pixel coordinates where we will be drawing the timeline
			int xLeft = data.getOffset();
			int xRight = maxx-data.getOffset();

			// Draw horizontal line
			g.setColor(data.getForegroundColor());
			g.drawLine(xLeft, axispos(), xRight, axispos());


			// Draw the big tick marks
			for(int i=0;i<numBigTicks()+1;i++){

				double timeForTick = (startTimePretty()+i*smallTickTimeIncrement()*smallTicksPerBigTick());
				int pixelForTickX = data.timeToScreenPixel(timeForTick);

				if(pixelForTickX >= xLeft && pixelForTickX <= xRight){
					String label = format_.format(timeForTick);	
					//				String label = U.humanReadableString(timeForTick);	
					g.drawLine(pixelForTickX, axispos()-largeTickHalfLength, pixelForTickX, axispos() + largeTickHalfLength);
					g.drawString(label, pixelForTickX - fm.stringWidth(label)/2, textpos());
				}
			}


			// Draw the small tick marks
			double timeForFirstBigTick = startTimePretty();
			for(int i=0;i<numSmallTicks()+1;i++){

				double timeForTick = timeForFirstBigTick + i*smallTickTimeIncrement();
				int pixelForTickX = data.timeToScreenPixel(timeForTick);
				if(pixelForTickX >= xLeft && pixelForTickX <= xRight){
					g.drawLine(pixelForTickX, axispos()-smallTickHalfLength, pixelForTickX, axispos()+smallTickHalfLength);			
				}
			}


			// Draw the label for the axis
			g.drawString(axisLabel(), getWidth()/2 - fm.stringWidth(axisLabel())/2, axisLabelPositionY());


			// Draw the overlay
			if(data.selectionValid()){
				g.setColor(data.getForegroundColor());
				g.drawLine(data.leftSelection(),0, data.leftSelection(), getHeight()-1);
				g.drawLine(data.rightSelection(),0, data.rightSelection(), getHeight()-1);
			}

			if(data.highlightValid()){
				// Draw vertical line
				g.setColor(data.getForegroundColor());
				g.drawLine(data.getHighlight(),0, data.getHighlight(), getHeight()-1);
			}

		}

	}


	/** Round the left offset to a multiple of the pretty timeIncrement() */
	private long startTimePretty() {
		return (long)(((long)data.startTime()/(long)smallTickTimeIncrement())*smallTickTimeIncrement());
	}

	private String axisLabel(){
		return "Time In Microseconds";
	}
	

	/** Number of microseconds per tickmark */
	private int smallTickTimeIncrement(){
		int actualDisplayWidth = getWidth();
		double pixelPerMicrosecond =  (double) data.lineWidth(actualDisplayWidth) / data.totalTime();
		double pixelsPerSmallTick = 5.0;
		int microsecondPerTick = Util.getBestIncrement( (int) Math.ceil(pixelsPerSmallTick / pixelPerMicrosecond ) );
		return microsecondPerTick;
	}
	
	
	/** The number of ticks we can display on the timeline in the given sized window */
	private int numSmallIntervals(){
		return (int) (data.totalTime() / smallTickTimeIncrement() + 1);
	}

	private int numSmallTicks(){
		return 1+numSmallIntervals();
	}
	
	private int numBigIntervals(){
		return numSmallIntervals() / smallTicksPerBigTick();
	}
	
	private int numBigTicks(){
		return 1 + numBigIntervals();
	}
	
	/** number of pixels per tick mark */
	private double pixelsPerTickMark(){
		return  ((double) data.lineWidth(getWidth())) / ((double)numSmallIntervals());
	}
	
	/** The number of tickmarks between the labeled big ticks */
	private int smallTicksPerBigTick() {
		return Util.getBestIncrement((int)(Math.ceil(data.maxLabelLen() / pixelsPerTickMark())));
	}
	

	@Override
	public void mouseDragged(MouseEvent e) {
		data.setSelection2(e.getPoint().x);
		data.setHighlight(e.getPoint().x);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		data.setHighlight(e.getPoint().x);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		data.invalidateSelection();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		data.setSelection1(e.getPoint().x);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		data.setSelection2(e.getPoint().x);
		data.setHighlight(e.getPoint().x);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		data.setHighlight(e.getPoint().x);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		data.removeHighlight();
	}


	private class MyListener implements ComponentListener {

		public void componentHidden(ComponentEvent e) {
		}

		public void componentMoved(ComponentEvent e) {
		}

		public void componentResized(ComponentEvent e) {
			data.invalidateSelection();
			repaint();
		}

		public void componentShown(ComponentEvent e) {
		}
	
	}


	

	/** Get the preferred size. The Width provided should be ignored */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(getWidth(),totalHeight());
	}
	
	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction){
		return data.singleTimelineHeight();
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction){
		return 5*data.singleTimelineHeight();
	}

	@Override
	public Dimension getPreferredScrollableViewportSize(){
		return getPreferredSize();
	}

	@Override
	public boolean getScrollableTracksViewportWidth(){
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight(){
		return false;
	}


}
