package projections.SamplePlots;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

public class MyActionHandler implements ActionListener {
	XYPlot plot;

		public MyActionHandler(XYPlot plot) {
			this.plot = plot;
		}

		public void actionPerformed(ActionEvent e) {
			System.out.println("Button was clicked");
			
			// create data
	        XYSeries s = new XYSeries("All Event Types", true, false);
	        for(int i=0;i<20;i++){
	        	s.add(i,Math.random());
	        }	

	        // Create a dataset
	        DefaultTableXYDataset dataset = new DefaultTableXYDataset();
	        dataset.addSeries(s);
			
			plot.setDataset(dataset);
			
		}
		
	
	
}

