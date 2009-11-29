package projections.Tools.Timeline;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import projections.analysis.NoiseMiner.NoiseResultButton;
import projections.gui.MainWindow;


/** A class that analyzes the user supplied data ranges, and displays a table of the results */
public class UserSuppliedAnalyzer extends JFrame {
	Data data;
	
	
	UserSuppliedAnalyzer(Data data){
		this.data = data;
		createLayout();
	}
	

	void createLayout(){
		setTitle("Analysis of user supplied values(timesteps)");

		TreeMap parameterMinTime = new TreeMap();
		TreeMap parameterMaxTime = new TreeMap();
		
		
		/* TreeMap<Integer,LinkedList<EntryMethodObject> > */ 
		Iterator iter = data.allEntryMethodObjects.values().iterator();

		while(iter.hasNext()){
			List objs = (List) iter.next();
			Iterator objIter = objs.iterator();
			while(objIter.hasNext()){
				EntryMethodObject obj = (EntryMethodObject) objIter.next();
				Integer param = obj.userSuppliedData;
				long start = obj.getBeginTime();
				long end = obj.getEndTime();
				
				if(param != null){
					if(parameterMinTime.containsKey(param)){
						// update the minimum seen for the user supplied parameter param
						Integer oldval = (Integer) parameterMinTime.get(param);
						if(start < oldval){
							parameterMinTime.put(param, new Integer((int) start));
						}

						// update the maximum seen for the user supplied parameter param
						oldval = (Integer) parameterMaxTime.get(param);
						if(end > oldval){
							parameterMaxTime.put(param, new Integer((int) end));
						}						
					}
					else {
						// first time we see the values, just insert them
						parameterMinTime.put(param, new Integer((int) start));
						parameterMaxTime.put(param, new Integer((int) end));
					}
				}

			}

		}


		
		// create a table of the data
		Vector columnNames = new Vector();
		columnNames.add(new String("User Supplied Value(timestep)"));
		columnNames.add(new String("Earliest Begin Time"));
		columnNames.add(new String("Latest End Time"));
		columnNames.add(new String("Duration"));
		columnNames.add(new String("Last Begin to This Begin"));
		columnNames.add(new String("Last End to This End"));
		
		Vector data  = new Vector();

		
		Iterator i = parameterMinTime.keySet().iterator();
		Integer prevMin=null;
		Integer prevMax=null;
		while(i.hasNext()){
			Integer param = (Integer) i.next();
			Integer min = (Integer) parameterMinTime.get(param);
			Integer max = (Integer) parameterMaxTime.get(param);			
			Vector row = new Vector();
			row.add(param);
			row.add(min);
			row.add(max);
			row.add(max-min);
		
			if(prevMin==null)
				row.add("");
			else
				row.add(min-prevMin);
			
			if(prevMax==null)
				row.add("");
			else
				row.add(max-prevMax);
			
			prevMin = min;
			prevMax = max;
			data.add(row);	
		}
		
		// If we didn't find any values, put some other text in the table
		
		if(parameterMinTime.size() == 0){
			JLabel msg = new JLabel("<html><body>No User Supplied Values Found in the currently loaded timeline.<br> Try selecting a different time range, or add calls to <br><font color=blue><tt>traceUserSuppliedData(int value)</tt></font> to the program.</body></html>");
			JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
			
			Vector row = new Vector();
			row.add(new String("No data found"));
			row.add(new String(""));
			row.add(new String(""));
			row.add(new String(""));
			row.add(new String(""));
			row.add(new String(""));
			data.add(row);
		}
				
		DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);

		JTable table = new JTable(tableModel);

		// put the table into a scrollpane

		JScrollPane scroller = new JScrollPane(table);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// put the scrollpane into our guiRoot

		this.setContentPane(scroller);
		
		// Display it all

		pack();
		setSize(800,400);
		setVisible(true);
		
	}
	
	

}
