package projections.gui;

import javax.swing.JPanel;

/** An interface for any class that can provide an extra bit of GUI input contained within the range dialog box displayed to the user */

public abstract class RangeDialogExtensionPanel extends JPanel {

	/** Record a reference to the parent dialog box into which I will be inserted. */
	public abstract void setParentDialogBox(RangeDialogNew parent);

	/** After the parent dialog box has setup its fields, the inserted box can derive any values it needs */
	public abstract void setInitialFields();

	/** If any input is changed in the parent dialog box, this will be called */
	public abstract void updateFields();
	
	/** Is the input currently valid? 
	 *  This is called only after the parent's input has been validated. 
	 *  If some field is not valid, requestFocus should be called on the gui component.
	 * 
	 * */
	public abstract boolean isInputValid();
	
}
