package projections.streaming;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.*;


@SuppressWarnings("serial")
public class StartupDialogBox extends JFrame implements ActionListener {

	JFrame dialogBox;
	JButton connectButton;
	JTextField portTextField;
	JTextField hostnameTextField;
	JTextField stsFilenameTextField;
	JComboBox handlerComboBox;
	
	StartupDialogBox(){
		dialogBox = new JFrame();

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
		stsFilenameTextField = new JTextField("");
		stsFilenameRowPane.add(stsFilenameLabel);
		stsFilenameRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
		stsFilenameRowPane.add(stsFilenameTextField);
		
		
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
			
			System.out.println("User supplied the following connection information:");
			System.out.println("hostname: " + hostname);	
			System.out.println("port: " + port);	
			System.out.println("CCS Handler: " + ccsHandler);
			
			if( ccsHandler.equals("CkPerfSumDetail compressed") ){
				new MultiSeriesHandler(hostname, port, ccsHandler, stsFilename, false, false);
			} else {
				new SingleSeriesHandler(hostname, port, ccsHandler);
			}
			// close window
			this.setVisible(false);
			this.dispose();
		}
	}
	
	
	
}
