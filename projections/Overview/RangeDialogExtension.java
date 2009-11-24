package projections.Overview;

import javax.swing.*;

import projections.gui.JTimeTextField;
import projections.gui.MainWindow;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.RangeDialog;
import projections.gui.Util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.*;

/** A JPanel that can be used to extend the standard RangeDialog dialog box by providing the granularity at which the time range should be discretized. */
public class RangeDialogExtension extends RangeDialogExtensionPanel implements ItemListener
{
	
	int myRun = 0;

	// Additional GUI objects

	private ButtonGroup modeGroup;
	private JRadioButton utilizationMode;
	private JRadioButton epMode;

	// A reference to the parent dialog box that I'm extending
	RangeDialog parent;


	/** Create a panel of input items specific to the timeline tool */
	public RangeDialogExtension() {

			// create mode panel
			modeGroup = new ButtonGroup();
			utilizationMode = new JRadioButton("Utilization", true);
			utilizationMode.addItemListener(this);
			epMode = new JRadioButton("Entry Method", false);
			epMode.addItemListener(this);
			modeGroup.add(utilizationMode);
			modeGroup.add(epMode);
			
			if (!MainWindow.runObject[myRun].hasLogData()) {
				epMode.setEnabled(false);
			}
		   

			GridBagLayout      gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			this.setLayout(gbl);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			Util.gblAdd(this, new JLabel("Color By:"), gbc, 0,0, 1,1, 1,1);
			Util.gblAdd(this, utilizationMode, gbc, 1,0, 1,1, 1,1);
			Util.gblAdd(this, epMode, gbc, 2,0, 1,1, 1,1);

	}
	
	
	
	public void itemStateChanged(ItemEvent evt) {
	}
	

	public void setParentDialogBox(RangeDialog parent) {
		this.parent = parent;	
	}

	public boolean isModeEP(){
		return epMode.isSelected();
	}

	public boolean isModeUtilization(){
		return utilizationMode.isSelected();
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
