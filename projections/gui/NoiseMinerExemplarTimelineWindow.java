package projections.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import projections.analysis.TimelineEvent;
import projections.analysis.NoiseMiner.EventWindow;
import projections.analysis.NoiseMiner.NoiseResult;
import projections.gui.Timeline.NonScrollingPanel;

public class NoiseMinerExemplarTimelineWindow extends JFrame {

	JLabel explanation; //< The textual explanation at the top of the window
	
	public NoiseMinerExemplarTimelineWindow(NoiseResult nr)  {
		
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEmptyBorder(8,4,4,0));
		wrapper.setLayout(new BorderLayout());
		wrapper.setBackground(Color.white);
		
		JPanel contents = new JPanel();
		contents.setBorder(BorderFactory.createEmptyBorder(15,0,0,0));
		contents.setBackground(Color.white);
		
		EventWindow ew =nr.ew;
 		int numEventsInWindow = ew.size();

 		int maxTimelinesPerWindow = 36;
 		int numMiniTimelines = Math.min(numEventsInWindow, maxTimelinesPerWindow);

 		int numRows;
		int numCols;
		
		/** If we only have a few timelines to display, lets use a smaller grid */
		if(numMiniTimelines<6){
			numRows = numMiniTimelines;
			numCols = 1;
		}
		else {
			numCols = 4;
			numRows = (numMiniTimelines+numCols-1) / numCols;
 		}
		
// 		System.out.println("Laying out grid for "+numMiniTimelines+ " mini-timelines to be of size "+numRows+"x"+numCols);
		/** Set the actual layout for the content pane */
		contents.setLayout(new GridLayout(numRows,numCols));
		
		int eventsSoFar=0;
		for (Iterator itr = ew.occurrences.iterator(); itr.hasNext();){

			TimelineEvent e =  (TimelineEvent) itr.next();
			
			//System.out.println(""+((e.BeginTime+e.EndTime )/ 2));
			
			
			if(eventsSoFar<numMiniTimelines){
				
				long eDuration = e.EndTime-e.BeginTime;
				
				// Pad the start time and end time to give some context
				double padding = 0.7;
				long startTime  = e.BeginTime-((long)(eDuration*padding));
				long endTime    = e.EndTime+((long)(eDuration*padding));

				int PE = e.SrcPe;
//				System.out.println("PE="+PE+"  Event time = "+e.BeginTime+"-"+e.EndTime + " displaying "+startTime+"-"+endTime);

	
				NonScrollingPanel tfp = new NonScrollingPanel(startTime, endTime, PE, Color.white, Color.black, true);
				contents.add(tfp);

				eventsSoFar ++;
			} else {
				break;
			}
		}	

		
		explanation = new JLabel("<html><body bgcolor=white>Below are " + numMiniTimelines + "(out of "+nr.occurrences+") mini-timelines that show a selected set of exemplar regions where extraordinarily long events occurred. In the center of each is the stretched event. Each stretched event was approximately "+ nr.duration +" longer than other entry methods of the same type(but not necessarily on the same object). Each timeline does not have the same scale, so direct comparisons are meaningless.</body></html>");
		explanation.setFont(new Font("Serif", Font.PLAIN, 16));
		explanation.setBackground(Color.white);
		explanation.setForeground(Color.black);
		
		wrapper.add(contents,BorderLayout.CENTER);
		wrapper.add(explanation, BorderLayout.NORTH);
		
		super.setContentPane(wrapper);
        super.pack();                               // Layout components.
        super.setTitle("NoiseMiner Exemplars");
		super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        super.setLocationRelativeTo(null);          // Center window.
        
	}
	
}
