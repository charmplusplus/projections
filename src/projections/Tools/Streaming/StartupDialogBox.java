package projections.Tools.Streaming;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


@SuppressWarnings("serial") class StartupDialogBox extends JFrame implements ActionListener, ItemListener {

	private JButton connectButton;
	private JTextField portTextField;
	private JTextField hostnameTextField;
	private JTextField stsFilenameTextField;
	private JComboBox handlerComboBox;
	private JCheckBox saveRepliesCheckBox;
	private JCheckBox loadRepliesCheckBox;
	
	StartupDialogBox(){

		JPanel handlerRowPane = new JPanel();
		handlerRowPane.setLayout(new BoxLayout(handlerRowPane, BoxLayout.LINE_AXIS));
		JLabel handlerLabel = new JLabel("Choose CCS Handler:");
		Vector<String> handlerStrings = new Vector<String>();
		handlerStrings.add("CkPerfSummaryCcsClientCB");
		handlerStrings.add("CkPerfSummaryCcsClientCB uchar");
		handlerStrings.add("CkPerfSumDetail compressed");
		
		handlerComboBox = new JComboBox(handlerStrings);
		handlerComboBox.setEditable(false);
		handlerComboBox.setMaximumRowCount(handlerStrings.size());
		handlerComboBox.setSelectedIndex(2); // nothing selected at first	
		handlerRowPane.add(handlerLabel);
		handlerRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
		handlerRowPane.add(handlerComboBox);

		JPanel hostnameRowPane = new JPanel();
		hostnameRowPane.setLayout(new BoxLayout(hostnameRowPane, BoxLayout.LINE_AXIS));
		JLabel hostLabel = new JLabel("CCS Server Hostname:");
		hostnameTextField = new JTextField("localhost");
		hostnameRowPane.add(hostLabel);
		hostnameRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
		hostnameRowPane.add(hostnameTextField);

		
		JPanel portRowPane = new JPanel();
		portRowPane.setLayout(new BoxLayout(portRowPane, BoxLayout.LINE_AXIS));
		JLabel portLabel = new JLabel("CCS Port Number:");
		portTextField = new JTextField("1234");
		portRowPane.add(portLabel);
		portRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
		portRowPane.add(portTextField);

		
		JPanel stsFilenameRowPane = new JPanel();	
		stsFilenameRowPane.setLayout(new BoxLayout(stsFilenameRowPane, BoxLayout.LINE_AXIS));
		JLabel stsFilenameLabel = new JLabel("STS File containing Entry Point Names:");
		stsFilenameTextField = new JTextField("/tmp/namd2.sts");
		stsFilenameRowPane.add(stsFilenameLabel);
		stsFilenameRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
		stsFilenameRowPane.add(stsFilenameTextField);
		
		
		

		JPanel saveLoadRowPane = new JPanel();	
		saveLoadRowPane.setLayout(new BoxLayout(saveLoadRowPane, BoxLayout.LINE_AXIS));
		JLabel saveToFileLabel = new JLabel("Save CCS Replies To File:");
		JLabel loadFromFileLabel = new JLabel("Load CCS Replies From File:");
		saveRepliesCheckBox = new JCheckBox();
		loadRepliesCheckBox = new JCheckBox();		
		
		saveRepliesCheckBox.addItemListener(this);
		loadRepliesCheckBox.addItemListener(this);
		
		saveLoadRowPane.add(saveToFileLabel);
		saveLoadRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
		saveLoadRowPane.add(saveRepliesCheckBox);
		saveLoadRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
		saveLoadRowPane.add(loadFromFileLabel);
		saveLoadRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
		saveLoadRowPane.add(loadRepliesCheckBox);
		saveLoadRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
		
		
		JPanel buttonRowPane = new JPanel();
		buttonRowPane.setLayout(new BoxLayout(buttonRowPane, BoxLayout.LINE_AXIS));
		buttonRowPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonRowPane.add(Box.createHorizontalGlue());
		connectButton = new JButton("Connect");
		connectButton.addActionListener(this);
		buttonRowPane.add(connectButton);
		

		
		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
		listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		this.setContentPane(listPane);

		listPane.add(handlerRowPane);
		listPane.add(Box.createRigidArea(new Dimension(0, 20)));
		listPane.add(hostnameRowPane);
		listPane.add(Box.createRigidArea(new Dimension(0, 5)));
		listPane.add(portRowPane);
		listPane.add(Box.createRigidArea(new Dimension(0, 5)));
		listPane.add(stsFilenameRowPane);
		listPane.add(Box.createRigidArea(new Dimension(0, 5)));
		listPane.add(saveLoadRowPane);		
		listPane.add(Box.createRigidArea(new Dimension(0, 20)));
		listPane.add(buttonRowPane);

		
		// Display it all
		pack();
		setVisible(true);
		
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == connectButton){
			
			String hostname = hostnameTextField.getText();	
			String portString = portTextField.getText();
			String stsFilename = stsFilenameTextField.getText();
			int port = new Integer(portString);
			String ccsHandler = (String) handlerComboBox.getSelectedItem();

			boolean saveReplies = saveRepliesCheckBox.isSelected();
			boolean loadReplies = loadRepliesCheckBox.isSelected();
			
			
			System.out.println("User supplied the following connection information:");
			System.out.println("hostname: " + hostname);	
			System.out.println("port: " + port);	
			System.out.println("CCS Handler: " + ccsHandler);
			
			if( ccsHandler.equals("CkPerfSumDetail compressed") ){
				new MultiSeriesHandler(hostname, port, ccsHandler, stsFilename, saveReplies, loadReplies);
			} else {
				new SingleSeriesHandler(hostname, port, ccsHandler);
			}
			// close window
			this.setVisible(false);
			this.dispose();
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == saveRepliesCheckBox){
			if(saveRepliesCheckBox.isSelected()){
				loadRepliesCheckBox.setSelected(false);
			}
		} else if(e.getSource() == loadRepliesCheckBox){
			if(loadRepliesCheckBox.isSelected()){
				saveRepliesCheckBox.setSelected(false);
			}
		}
	}
	
	
	
}
