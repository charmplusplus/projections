package projections.Tools.Overview;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;

import projections.gui.RangeDialog;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.Util;

/** A JPanel that can be used to extend the standard RangeDialog dialog box by providing the granularity at which the time range should be discretized. */
class OverviewDialogExtension extends RangeDialogExtensionPanel implements ItemListener
{

	// Additional GUI objects

	protected JCheckBox cbGenerateImage;
	
	// A reference to the parent dialog box that I'm extending
//	RangeDialog parent;


	/** Create a panel of input items specific to the timeline tool */
	public OverviewDialogExtension() {

			// create mode panel
			GridBagLayout      gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			this.setLayout(gbl);

			cbGenerateImage = new JCheckBox("Save a Screenshot Once Loaded");
			cbGenerateImage.setSelected(false);
			
			Util.gblAdd(this, cbGenerateImage, gbc, 0,0, 1,1, 1,1);

	}
	
	
	
	public void itemStateChanged(ItemEvent evt) {
	}
	

	public void setParentDialogBox(RangeDialog parent) {
//		this.parent = parent;	
	}

	public boolean isInputValid() {
			return true;
	}

	public void setInitialFields(){
		// do nothing
	}

	/** derive all the values from the integer in sizeField */	
	public void updateFields(){
		// do nothing
	}
	



}
