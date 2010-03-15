package projections.Tools.Timeline;


import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.ScrollPaneLayout;
import javax.swing.border.Border;

/**
 * A layout manager that correctly lays out a JScrollPane for our timeline view
 * 
 * This inherits most of its functionality from javax.swing.ScrollPaneLayout, 
 * but it overrides a few parts.
 * 
 * The main view and column header view has a width which is the 
 * available width scaled by a factor specified in the member 'data'.
 * This configuration causes the container to scale to its parents
 * size, while suporting a user specified zoom level.
 * 
 * Scrollbars are always visible
 * 
 * @author idooley2
 * 
 */
class TimelineScrollPaneLayout extends ScrollPaneLayout
implements LayoutManager, ScrollPaneConstants
{

	private Data data;

	protected TimelineScrollPaneLayout(Data data){
		this.data = data;
		vsbPolicy = VERTICAL_SCROLLBAR_ALWAYS;
		hsbPolicy = HORIZONTAL_SCROLLBAR_ALWAYS;
	}

	public void syncWithScrollPane(JScrollPane sp) {
		super.syncWithScrollPane(sp);
		vsbPolicy = VERTICAL_SCROLLBAR_ALWAYS;
		hsbPolicy = HORIZONTAL_SCROLLBAR_ALWAYS;
	}


	/**
	 * Adjusts the <code>Rectangle</code> <code>available</code> to account 
	 * for the vertical scroll bar's width
	 */
	private void adjustForVSB(Rectangle available,
			Rectangle vsbR, Insets vpbInsets) {
		int vsbWidth = Math.max(0, Math.min(vsb.getPreferredSize().width, available.width));
		available.width -= vsbWidth;
		vsbR.width = vsbWidth;
		vsbR.x = available.x + available.width + vpbInsets.right;
	}

	/**
	 * Adjusts the <code>Rectangle</code> <code>available</code> to account 
	 * for the vertical scroll bar's width
	 */
	private void adjustForHSB(Rectangle available,
			Rectangle hsbR, Insets vpbInsets) {
		int hsbHeight = Math.max(0, Math.min(available.height, hsb.getPreferredSize().height));
		available.height -= hsbHeight;
		hsbR.y = available.y + available.height + vpbInsets.bottom;
		hsbR.height = hsbHeight;	
	}


	/** 
	 * Lays out the scrollpane. Similar to our superclass's method
	 * with some notable exceptions: The column header and main view
	 * are scaled based upon the scale factor found in 'data'
	 * 
	 * @see javax.swing.ScrollPaneLayout.layoutContainer()
	 *
	 */
	public void layoutContainer(Container parent) 
	{

		// Get the old view position to be used later to keep the scrollbar where we want it
		
		Point originalViewPosition = viewport.getViewPosition();		
		int originalScaledWidth = viewport.getView().getWidth();
		int originalViewWidth = viewport.getWidth();
		
		super.layoutContainer(parent);
		
		Rectangle availR;

		JScrollPane scrollPane;

		scrollPane = (JScrollPane)parent;


		availR = parent.getBounds();

		availR.x = availR.y = 0;

		Insets insets = parent.getInsets();
		availR.x = insets.left;
		availR.y = insets.top;
		availR.width -= insets.left + insets.right;
		availR.height -= insets.top + insets.bottom;


		/* If there's a visible column header remove the space it 
		 * needs from the top of availR.  The column header is treated 
		 * as if it were fixed height, arbitrary width.
		 */

		Rectangle colHeadR = new Rectangle(0, availR.y, 0, 0);

		if ((colHead != null) && (colHead.isVisible())) {
			int colHeadHeight = Math.min(availR.height,
					colHead.getPreferredSize().height);
			colHeadR.height = colHeadHeight; 
			availR.y += colHeadHeight;
			availR.height -= colHeadHeight;
		}

		/* If there's a visible row header remove the space it needs
		 * from the left or right of availR.  The row header is treated 
		 * as if it were fixed width, arbitrary height.
		 */

		Rectangle rowHeadR = new Rectangle(0, 0, 0, 0);

		if ((rowHead != null) && (rowHead.isVisible())) {
			int rowHeadWidth = Math.min(availR.width,
					rowHead.getPreferredSize().width);
			rowHeadR.width = rowHeadWidth;
			availR.width -= rowHeadWidth;

			rowHeadR.x = availR.x;
			availR.x += rowHeadWidth;

		}

		/* If there's a JScrollPane.viewportBorder, remove the
		 * space it occupies for availR.
		 */

		Border viewportBorder = scrollPane.getViewportBorder();
		Insets vpbInsets;
		if (viewportBorder != null) {
			vpbInsets = viewportBorder.getBorderInsets(parent);
			availR.x += vpbInsets.left;
			availR.y += vpbInsets.top;
			availR.width -= vpbInsets.left + vpbInsets.right;
			availR.height -= vpbInsets.top + vpbInsets.bottom;
		}
		else {
			vpbInsets = new Insets(0,0,0,0);
		}

		Rectangle vsbR = new Rectangle(0, availR.y - vpbInsets.top, 0, 0);

		if (vsb != null) {
			adjustForVSB(availR, vsbR, vpbInsets);
		}

		Rectangle hsbR = new Rectangle(availR.x - vpbInsets.left, 0, 0, 0);

		if (hsb != null) {
			adjustForHSB(availR, hsbR, vpbInsets);
		}


		int scaledWidth = data.scaledScreenWidth(availR.width);

		
		if (viewport != null) {
			Component c = viewport.getView();

			viewport.setBounds(availR);

			int requiredScreenHeight = data.screenHeight();
			if(scaledWidth != c.getWidth()) {
				c.setBounds(0,0,scaledWidth,requiredScreenHeight);
			}

		}


		vsbR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
		hsbR.width = availR.width + vpbInsets.left + vpbInsets.right;
		rowHeadR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
		rowHeadR.y = availR.y - vpbInsets.top;
		colHeadR.width = availR.width + vpbInsets.left + vpbInsets.right;
		colHeadR.x = availR.x - vpbInsets.left;




		if (rowHead != null) {
			int rowHeadR_x = rowHeadR.x;
			int rowHeadR_y = rowHeadR.y;
			int rowHeadR_width = rowHeadR.width;
			int rowHeadR_height = rowHeadR.height;
			rowHead.setBounds(rowHeadR_x,rowHeadR_y,rowHeadR_width,rowHeadR_height);
		}

		if (colHead != null) {
			int colHeadR_x = colHeadR.x;
			int colHeadR_y = colHeadR.y;
			int colHeadR_width = colHeadR.width;
			int colHeadR_height = colHeadR.height;

			// Set the size of the viewport

			Point goodPosition = colHead.getViewPosition();

			colHead.setBounds(colHeadR_x, colHeadR_y, colHeadR_width, colHeadR_height);

			// Size the top column header as well

			Component c = colHead.getView();
			c.setBounds(0,0, scaledWidth, colHeadR_height);

			colHead.setViewPosition(goodPosition);
//				
//			colHead.repaint();

		}

		if (vsb != null) {
			vsb.setVisible(true);
			vsb.setBounds(vsbR);
		}

		if (hsb != null) {
			hsb.setVisible(true);
			hsb.setBounds(hsbR);
		}

		if (lowerLeft != null) {
			lowerLeft.setBounds(rowHeadR.x ,
					hsbR.y,
					rowHeadR.width ,
					hsbR.height);
		}

		if (lowerRight != null) {
			lowerRight.setBounds(vsbR.x ,
					hsbR.y,
					vsbR.width ,
					hsbR.height);
		}

		if (upperLeft != null) {
			upperLeft.setBounds(rowHeadR.x,
					colHeadR.y,
					rowHeadR.width,
					colHeadR.height);
		}

		if (upperRight != null) {
			upperRight.setBounds(vsbR.x ,
					colHeadR.y,
					vsbR.width,
					colHeadR.height);
		}


		
		
		/** See if we want a specific location for the view 
		 * 
		 * This would occur when we click the zoom button
		 */
		if(viewport != null){

			boolean doMoveView = false;

			int newViewPositionCenter=0;

			if(data.hasNewPreferredView()){
				// The desired view time should be at the center of the window
				newViewPositionCenter = data.getNewPreferredViewCenter(scaledWidth);

				doMoveView = true;
			} else if(data.keepViewCentered()){
				// Try to keep the center of the visible area at the same portion of the timeline, even though its width may have changed

				int oldViewPositionCenter = originalViewPosition.x + originalViewWidth / 2 ;
				
				double relativePosition = ((double)(oldViewPositionCenter-data.offset()))/((double)(originalScaledWidth-2*data.offset()));

				newViewPositionCenter = data.offset()+(int)(relativePosition* (scaledWidth-2*data.offset()));
				
				data.keepViewCentered(false);

				doMoveView = true;
			}


			// We should move the view position only if we have a good reason to
			// If we do it all the time our scrollbar could get stuck there
			if(doMoveView){
				int newViewPositionLeft = newViewPositionCenter - availR.width / 2;

				// If we were at the far left or right, make sure we stay in range
				if(newViewPositionLeft < 0)
					newViewPositionLeft = 0;
				if(newViewPositionLeft > scaledWidth - availR.width)
					newViewPositionLeft = scaledWidth - availR.width;

				Point p = viewport.getViewPosition();
				p.x = newViewPositionLeft;
				viewport.setViewPosition(p);

				data.resetPreferredView();


			} 
		}
		
		
		
	} // end layoutContainer()

} // end class
