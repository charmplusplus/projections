package projections.gui.Timeline;

import javax.swing.*;

import projections.gui.JTimeTextField;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.RangeDialogNew;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.*;

/** A JPanel that can be used to extend the standard RangeDialog dialog box by providing the granularity at which the time range should be discretized. */
public class TimlineRangeDialogExtension extends RangeDialogExtensionPanel implements ItemListener
{

	// Additional GUI objects
	public JCheckBox dialogEnableEntryFiltering;
	public JTimeTextField dialogMinEntryFiltering;

	public JCheckBox dialogEnableIdleFiltering;
	public JCheckBox dialogEnableMsgFiltering;
	

	// A reference to the parent dialog box that I'm extending
	RangeDialogNew parent;

	// dialog parameter variables
	public long intervalSize;

	/** Create a panel of input items specific to the timeline tool */
	public TimlineRangeDialogExtension() {

			JPanel p = this;
		    p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
		    
		    // Create a JPanel for filtering out small entry methods
		    JPanel p1 = new JPanel();
		    p1.setLayout(new BoxLayout(p1, BoxLayout.LINE_AXIS));
		    dialogEnableEntryFiltering = new JCheckBox();
		    p1.add(dialogEnableEntryFiltering);
		    p1.add(new JLabel("Filter out entries shorter than"));
		    dialogMinEntryFiltering = new JTimeTextField("30us", 7);
			dialogMinEntryFiltering.setEditable(false);
		    p1.add(dialogMinEntryFiltering);
		    p1.add(Box.createHorizontalStrut(200)); // Add some empty space so that the textbox isn't huge
		    p1.add(Box.createHorizontalGlue());
		    dialogEnableEntryFiltering.addItemListener(this);

		    // Create a JPanel for filtering out idle time
		    JPanel p2 = new JPanel();
		    p2.setLayout(new BorderLayout());
		    dialogEnableIdleFiltering = new JCheckBox();
		    p2.add(dialogEnableIdleFiltering, BorderLayout.WEST);
		    p2.add(new JLabel("Filter out idle time regions"), BorderLayout.CENTER);
		    
		    // Create a JPanel for filtering out messages
		    JPanel p3 = new JPanel();
		    p3.setLayout(new BorderLayout());
		    dialogEnableMsgFiltering = new JCheckBox();
		    p3.add(dialogEnableMsgFiltering, BorderLayout.WEST);
		    p3.add(new JLabel("Filter out messages"), BorderLayout.CENTER);
		    
		    // Put the various rows into the panel
		    p.add(p1);
		    p.add(p2);
		    p.add(p3);

	}
	
	
	
	public void itemStateChanged(ItemEvent evt) {
		if(evt.getSource() == dialogEnableEntryFiltering){
			dialogMinEntryFiltering.setEditable(dialogEnableEntryFiltering.isSelected());
			return;
		}
	}
	

	public void setParentDialogBox(RangeDialogNew parent) {
		this.parent = parent;	
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
