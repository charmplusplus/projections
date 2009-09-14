package projections.gui.Timeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import projections.gui.MainWindow;
import projections.gui.Timeline.ChooseUserEventsWindow.ColorRenderer;

public class MemoryColorRangeChooser extends JFrame implements ActionListener 
{
	Data data;
	Hashtable<Integer, String> entryNames;
	Vector<Vector> tabledata;
	Vector<String> columnNames;

	JButton checkAll;
	JButton uncheckAll;
	
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
