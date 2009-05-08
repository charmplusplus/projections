package projections.streaming;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.time.Year;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import projections.ccs.*;
import projections.gui.graph.DataSource;
import projections.gui.graph.DataSource1D;
import projections.gui.graph.Graph;
import projections.gui.graph.XAxis;
import projections.gui.graph.XAxisFixed;
import projections.gui.graph.YAxis;
import projections.gui.graph.YAxisFixed;

/** A tool that will eventually use CCS to get data from a running parallel Charm++ program. */

public class StreamingTool {

	
	public static void main(String args[]){

		System.out.println("Streaming Tool");
		
		new StreamingDataHandler();

	}

	

}
