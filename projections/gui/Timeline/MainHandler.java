package projections.gui.Timeline;


/** An interface for anything that will be used to hold a MainPanel.
 * 
 *  This is used to handle/intercept events such as clicking on TimelineEntryMethodObjects.
 * 
 * @author idooley2
 *
 */
public interface MainHandler {

	/** Called to insert another PE into the visualization
	 * 
	 * @note Called by mouse handler in mainpanel when a message dependency is required and a processor involved is not yet loaded
	 * 
	 * @param PE The processor id to add
	 */
	void addProcessor(int PE);
	
	/** Called by the main panel once it has loaded the objects. 
	 * 
	 * @note Used by Window: Allows a User Event Table to be constructed */
	void setData(Data data);

	
	/** Called by the data object when the scale or size of the canvas changes in size
	 *  or when any changes necessitate a repaint
	 *  
	 *  @note Any implementer should call repaint on its panels(main,axis,etc.)
	 *  @note Any implementer should also call revalidate if doRevalidate is true
	 */
	void refreshDisplay(boolean doRevalidate);
	
}