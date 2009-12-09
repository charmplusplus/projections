package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MemoryColorRangeChooser extends JFrame implements ActionListener 
{
	Data data;
	
	JTextField minField;
	JTextField maxField;

	
	MemoryColorRangeChooser(Data _data){
		data = _data;
		createLayout();
	}

	void createLayout(){
		setTitle("Specify a range for use in coloring the entry methods:");
		
		JPanel rangeChooserPanel = new JPanel();
		rangeChooserPanel.setLayout(new FlowLayout());
		JLabel minLabel = new JLabel("Minimum MB:");
		JLabel maxLabel = new JLabel("Maximum MB:");
		minField = new JTextField(7);
		maxField = new JTextField(7);
		minField.setText("" + data.minMemMB());
		maxField.setText("" + data.maxMemMB());
				
		
		JButton okButton = new JButton("Continue");
		okButton.addActionListener(this);

		rangeChooserPanel.add(minLabel);
		rangeChooserPanel.add(minField);
		rangeChooserPanel.add(maxLabel);
		rangeChooserPanel.add(maxField);
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(rangeChooserPanel, BorderLayout.CENTER);
		p.add(okButton, BorderLayout.SOUTH);

		this.setContentPane(p);

		// Display it all

		pack();
//		setSize(400,200);
		setVisible(true);
		setLocation(400,400);
	}

	public void actionPerformed(ActionEvent e) {
		// parse the user input
		long userMin = Long.parseLong(minField.getText());
		long userMax = Long.parseLong(maxField.getText());
		
		// update the colors:
		data.setMemColorRange(userMin*1024*1024, userMax*1024*1024);
		data.finalizeColorByMemoryUsage();
		this.dispose();
	}

}
