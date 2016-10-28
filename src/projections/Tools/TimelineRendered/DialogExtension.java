package projections.Tools.TimelineRendered;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import projections.gui.RangeDialog;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.TimeTextField;


class DialogExtension extends RangeDialogExtensionPanel implements ItemListener {

	// Additional GUI objects
	private JCheckBox dialogEnableEntryFiltering;
	private TimeTextField dialogMinEntryFiltering;

	private JCheckBox dialogEnableIdleFiltering;
	private JCheckBox dialogEnableMsgFiltering;
	private JCheckBox dialogEnableUserEventFiltering;
	
	protected JTextField dialogWidth;

	
	private class LeftAlignedPanel extends JPanel {
		private LeftAlignedPanel(JComponent c){
		    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		    add(c);
		    add(Box.createHorizontalGlue());
		}
	}
	
	/** Create a panel of input items specific to the timeline tool */
	public DialogExtension() {

			JPanel p = this;
		    p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
		    
		    // Create a JPanel for filtering out small entry methods
		    JPanel p1 = new JPanel();
		    p1.setLayout(new BoxLayout(p1, BoxLayout.LINE_AXIS));
		    dialogEnableEntryFiltering = new JCheckBox();
		    p1.add(dialogEnableEntryFiltering);
		    p1.add(new JLabel("Filter out entries shorter than "));
		    dialogMinEntryFiltering = new TimeTextField("30us", 7);
			dialogMinEntryFiltering.setEditable(false);
		    p1.add(dialogMinEntryFiltering);
		    p1.add(Box.createHorizontalStrut(200)); // Add some empty space so that the textbox isn't huge
		    p1.add(Box.createHorizontalGlue());
		    dialogEnableEntryFiltering.addItemListener(this);

		    dialogEnableIdleFiltering = new JCheckBox("Filter out idle time regions");
		    
		    dialogEnableMsgFiltering = new JCheckBox("Filter out messages");
		    
		    dialogEnableUserEventFiltering = new JCheckBox("Filter out user events");
		    
		    
		    
		    JPanel p2 = new JPanel();
		    p2.setLayout(new BoxLayout(p2, BoxLayout.LINE_AXIS));
		    p2.add(new JLabel("Width of rendered image: "));
		    dialogWidth = new JTextField("2000");
		    p2.add(dialogWidth);
		    p2.add(new JLabel(" pixels"));
		    p2.add(Box.createHorizontalStrut(200)); // Add some empty space so that the textbox isn't huge
		    p2.add(Box.createHorizontalGlue());

		    
		    // Put the various rows into the panel
		    p.add(p1);
		    p.add(new LeftAlignedPanel(dialogEnableIdleFiltering));
		    p.add(new LeftAlignedPanel(dialogEnableMsgFiltering));
		    p.add(new LeftAlignedPanel(dialogEnableUserEventFiltering));
		    p.add(p2);


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
