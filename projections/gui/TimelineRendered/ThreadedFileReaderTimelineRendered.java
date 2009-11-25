package projections.gui.TimelineRendered;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import projections.analysis.IntervalData;
import projections.analysis.LogReader;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.Timeline.Data;
import projections.gui.Timeline.MainHandler;
import projections.gui.Timeline.MainPanel;
import projections.gui.Timeline.SaveImage;

/** The reader threads for Time Profile tool. This class ought to be generalized for all the other tools needing similar functionality. */
public class ThreadedFileReaderTimelineRendered extends Thread implements MainHandler {
	public int PE;
	Color background;
	Color foreground;
	long startTime, endTime;
	private BufferedImage image;
	int width;

	public ThreadedFileReaderTimelineRendered(int pe, long startTime, long endTime, Color backgroundColor, Color foregroundColor, int width){
		this.PE = pe;
		this.startTime = startTime;
		this.endTime = endTime;
		this.background = backgroundColor;
		this.foreground = foregroundColor;
		this.width = width;
	}

	public void run() { 

		OrderedIntList validPEs = new OrderedIntList();
		validPEs.insert(PE);

		// setup the Data for this panel 
		Data data = new Data(null);
		data.setProcessorList(validPEs);
		data.setRange(startTime, endTime);
//		data.setUseMinimalMargins(true);
//		data.setFontSizes(12, 10, true);
//		data.showIdle(false);
//		data.showPacks(true);
		data.setCompactView(true);
		
		if(background != null && foreground != null)
			data.setColors(background,foreground);

		data.setHandler(this);

		// create a MainPanel for it	
		MainPanel displayPanel = new MainPanel(data, this);
		displayPanel.loadTimelineObjects(false, null, false);

		displayPanel.setSize(width,data.singleTimelineHeight());
		displayPanel.revalidate();
		displayPanel.doLayout();

		SaveImage si = new SaveImage();
		image = si.generateImage(displayPanel);
		
		System.out.println("Created image for PE " + PE);
		
		data = null;
	}

	
	public void displayResults(){
		JLabel lbl = new JLabel(new ImageIcon(image));
		JFrame f = new JFrame();
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(lbl, BorderLayout.CENTER);
		f.pack();
		f.setVisible(true);
	}
	
	public void displayWarning(String message) {
		// do nothing
	}

	public void notifyProcessorListHasChanged() {
		// do nothing
	}

	public void refreshDisplay(boolean doRevalidate) {
		// do nothing
	}

	public void setData(Data data) {
		// do nothing
	}

	public BufferedImage getImage() {
		return image;
	}


}





