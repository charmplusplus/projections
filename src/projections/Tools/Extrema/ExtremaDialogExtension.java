package projections.Tools.Extrema;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import projections.gui.Analysis;
import projections.gui.JIntTextField;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.RangeDialogExtensionPanel;
import projections.gui.Util;

/** The dialog input panel for the  OutlierAnalysisWindow */

class ExtremaDialogExtension extends RangeDialogExtensionPanel
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private int myRun = 0;

	// GUI components
	private JPanel outlierPanel;
	private JComboBox attributeList;
	private JComboBox activityList;
	private JIntTextField thresholdField;
	private JIntTextField kField;
	private JTextArea errorText;

	private JLabel attributeLabel;
	private JLabel activityLabel;
	private JLabel thresholdPost;
	protected JLabel thresholdLabel;
	
	
	public ExtremaDialogExtension(String[] attributes) {

		// initialize default threshold to display the top 10% deviants
		// for # processors 256 or less. The top 20 otherwise.
		int threshold;
		if (MainWindow.runObject[myRun].getNumProcessors() <= 256) {
			threshold = (int)Math.ceil(0.1*MainWindow.runObject[myRun].getNumProcessors());
		} else {
			threshold = 20;
		}
		
			
		GridBagLayout gbl      = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2,2,2,2);

		outlierPanel = this;
		outlierPanel.setLayout(gbl);

		attributeList = new JComboBox(attributes);
		attributeList.setSelectedIndex(0);
		attributeLabel = new JLabel("Attribute: ", JLabel.RIGHT);

		activityList = new JComboBox(Analysis.NAMES);
		activityList.setSelectedIndex(0);
		activityLabel = new JLabel("Activity: ", JLabel.RIGHT);
		
		thresholdLabel = new JLabel("Extrema Threshold: ", JLabel.RIGHT);
		thresholdField = new JIntTextField(threshold, 8);
		thresholdPost = new JLabel("Processors", JLabel.LEFT);
		

		JLabel kLabel = new JLabel("Number of Clusters: ", JLabel.RIGHT);
		// initialize k-means choice of k to be default to 5
		kField = new JIntTextField(5, 3);

		errorText = new JTextArea();
		errorText.setRows(3);
		errorText.setEditable(false);
		errorText.setLineWrap(true);
		errorText.setWrapStyleWord(true);
		errorText.setForeground(Color.red);
		JScrollPane scrollText = 
			new JScrollPane(errorText,
					JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		Util.gblAdd(outlierPanel, attributeLabel, gbc, 0,0, 1,1, 0,1);
		Util.gblAdd(outlierPanel, attributeList,  gbc, 1,0, 2,1, 0,1);
		Util.gblAdd(outlierPanel, activityLabel,  gbc, 0,1, 1,1, 0,1);
		Util.gblAdd(outlierPanel, activityList,   gbc, 1,1, 2,1, 0,1);
		Util.gblAdd(outlierPanel, thresholdLabel, gbc, 0,2, 1,1, 0,1);
		Util.gblAdd(outlierPanel, thresholdField, gbc, 1,2, 1,1, 0,1);
		Util.gblAdd(outlierPanel, thresholdPost,  gbc, 2,2, 1,1, 0,1);
		Util.gblAdd(outlierPanel, kLabel,         gbc, 0,3, 1,1, 0,1);
		Util.gblAdd(outlierPanel, kField,         gbc, 1,3, 2,1, 0,1);
		Util.gblAdd(outlierPanel, scrollText,     gbc, 0,4, 3,3, 0,1);

	}
		

	public boolean isInputValid() {
		
		// don't check how many PEs are in the list because it takes too long.
				
		if ((thresholdField.getValue() < 0) ) {
			thresholdField.setForeground(Color.red);
			return false;
		}

		
		if ( activityList.getSelectedIndex() == Analysis.FUNCTIONS && attributeList.getSelectedIndex() >= 1 ) {
			attributeLabel.setForeground(Color.red);
			activityLabel.setForeground(Color.red);
			errorText.setForeground(Color.red);
			errorText.setText("ERROR: Attribute " +	attributeList.getSelectedItem() +
					" is incompatible with Activity " + activityList.getSelectedItem() );
			return false;
		}

		
		// Everything is valid, so clear all warning messages
		errorText.setText("");
		attributeLabel.setForeground(Color.black);
		activityLabel.setForeground(Color.black);
		thresholdField.setForeground(Color.black);
		return true;
		
	}

	
	public void setInitialFields() {
		// do nothing
	}

	
	public void setParentDialogBox(RangeDialog parent) {

		attributeList.addActionListener(parent);
		attributeList.addKeyListener(parent);
		attributeList.addFocusListener(parent);

		activityList.addActionListener(parent);
		activityList.addKeyListener(parent);
		activityList.addFocusListener(parent);

		thresholdField.addActionListener(parent);	
		thresholdField.addKeyListener(parent);	
		thresholdField.addFocusListener(parent);	
		
		kField.addActionListener(parent);	
		kField.addKeyListener(parent);	
		kField.addFocusListener(parent);

	}

	public void updateFields() {
//	
//		System.out.println("updateFields currentActivity=" + getCurrentActivity() );
//		System.out.println("updateFields currentAttribute=" + getCurrentAttribute() );
//		System.out.println("updateFields k=" + getK() );
//		
	}
	


	public int getThreshold(){
		return thresholdField.getValue();
	}

	int getCurrentAttribute(){
		return attributeList.getSelectedIndex();
	}


	int getCurrentActivity(){
		return activityList.getSelectedIndex();
	}

	int getK(){
		return kField.getValue(); 
	}

	
}
