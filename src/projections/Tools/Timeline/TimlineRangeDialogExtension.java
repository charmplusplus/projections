package projections.Tools.Timeline;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import projections.gui.JIntTextField;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.TimeTextField;

/** A JPanel that can be used to extend the standard RangeDialog dialog box by adding GUI components for filtering out data. */
class TimlineRangeDialogExtension extends RangeDialogExtensionPanel implements ItemListener, ActionListener
{

	// Additional GUI objects
	protected JCheckBox dialogEnableEntryFiltering;
	protected TimeTextField dialogMinEntryFiltering;

	protected JCheckBox dialogEnableMsgFiltering;
	protected JCheckBox dialogEnableUserEventFiltering;

	protected JCheckBox dialogEnableTopTimes;
	protected JIntTextField dialogAmountTopTimes;

	protected JButton dialogAdjustRanges;

	protected RangeDialog parent;

	private int myRun = 0;

	
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
		    
		dialogEnableMsgFiltering = new JCheckBox("Filter out messages");
		    
		dialogEnableUserEventFiltering = new JCheckBox("Filter out user events");

		dialogAdjustRanges = new JButton("Adjust ranges to show useful information");
		dialogAdjustRanges.addActionListener(this);

		//create JPanel for filtering longest methods
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.LINE_AXIS));
		dialogEnableTopTimes = new JCheckBox();
		p2.add(dialogEnableTopTimes);
		p2.add(new JLabel("Highlight the top "));
		dialogAmountTopTimes = new JIntTextField(10,5);
		dialogAmountTopTimes.setEditable(false);
		p2.add(dialogAmountTopTimes);
		p2.add(new JLabel(" longest idle and entry times"));
		p2.add(Box.createHorizontalStrut(200)); // Add some empty space so that the textbox isn't huge
		p2.add(Box.createHorizontalGlue());
		dialogEnableTopTimes.addItemListener(this);
		    
		// Put the various rows into the panel
		p.add(p1);
		p.add(new LeftAlignedPanel(dialogEnableMsgFiltering));
		p.add(new LeftAlignedPanel(dialogEnableUserEventFiltering));
		p.add(p2);
		p.add(new LeftAlignedPanel(dialogAdjustRanges));


	}
	
	
	
	public void itemStateChanged(ItemEvent evt) {
		if(evt.getSource() == dialogEnableEntryFiltering)
		{
			dialogMinEntryFiltering.setEditable(dialogEnableEntryFiltering.isSelected());
		}
		if(evt.getSource() == dialogEnableTopTimes)
		{
			dialogAmountTopTimes.setEditable(dialogEnableTopTimes.isSelected());
		}
		return;
	}

	public void actionPerformed(ActionEvent event){
		if (event.getSource() == dialogAdjustRanges)
		{
			long adjustedTime = MainWindow.runObject[myRun].findEarliestBeginEventTime(parent.getSelectedProcessors(), MainWindow.runObject[myRun].getValidProcessorList());
			if (parent.getStartTime() < adjustedTime) parent.setStartTime(adjustedTime);
			adjustedTime = MainWindow.runObject[myRun].findLatestEndEventTime(parent.getSelectedProcessors(), MainWindow.runObject[myRun].getValidProcessorList());
			if (parent.getEndTime() > adjustedTime) parent.setEndTime(adjustedTime);
		}
	}

	public void setParentDialogBox(RangeDialog parent) {
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
