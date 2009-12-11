package projections.Tools.Timeline;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import projections.gui.RangeDialog;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.TimeTextField;

/** A JPanel that can be used to extend the standard RangeDialog dialog box by providing the granularity at which the time range should be discretized. */
class TimlineRangeDialogExtension extends RangeDialogExtensionPanel implements ItemListener
{

	// Additional GUI objects
	protected JCheckBox dialogEnableEntryFiltering;
	protected TimeTextField dialogMinEntryFiltering;

	protected JCheckBox dialogEnableIdleFiltering;
	protected JCheckBox dialogEnableMsgFiltering;
	protected JCheckBox dialogEnableUserEventFiltering;

	
	private class LeftAlignedPanel extends JPanel {
		private LeftAlignedPanel(JComponent c){
		    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		    add(c);
		    add(Box.createHorizontalGlue());
		}
	}
	
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
		    dialogMinEntryFiltering = new TimeTextField("30us", 7);
			dialogMinEntryFiltering.setEditable(false);
		    p1.add(dialogMinEntryFiltering);
		    p1.add(Box.createHorizontalStrut(200)); // Add some empty space so that the textbox isn't huge
		    p1.add(Box.createHorizontalGlue());
		    dialogEnableEntryFiltering.addItemListener(this);

		    dialogEnableIdleFiltering = new JCheckBox("Filter out idle time regions");
		    
		    dialogEnableMsgFiltering = new JCheckBox("Filter out messages");
		    
		    dialogEnableUserEventFiltering = new JCheckBox("Filter out user events");
		    
		    // Put the various rows into the panel
		    p.add(p1);
		    p.add(new LeftAlignedPanel(dialogEnableIdleFiltering));
		    p.add(new LeftAlignedPanel(dialogEnableMsgFiltering));
		    p.add(new LeftAlignedPanel(dialogEnableUserEventFiltering));


	}
	
	
	
	public void itemStateChanged(ItemEvent evt) {
		if(evt.getSource() == dialogEnableEntryFiltering){
			dialogMinEntryFiltering.setEditable(dialogEnableEntryFiltering.isSelected());
			return;
		}
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
