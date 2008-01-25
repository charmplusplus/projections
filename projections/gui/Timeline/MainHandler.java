package projections.gui.Timeline;

/** An interface for anything that will be used to hold a MainPanel.
 * 
 *  This is used to handle/intercept events such as clicking on TimelineEntryMethodObjects.
 * 
 * @author idooley2
 *
 */
public interface MainHandler {

	/** A processor has been added, and now this object must update its displays appropriately
	 * 
	 * @note Called when a new processor is added in Data.addProcessor(int p)
	 */
	void notifyProcessorListHasChanged();
	
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
	
	/** Display a warning dialog
	 * @note This is used when trying to draw a line to a message sent from a time outside the range of the visualization
	 */
	void displayWarning(String message);
	
}