package projections.gui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.StringTokenizer;
import java.lang.Integer;


public class Util
{
    public static void gblAdd(Container target, Component c,
			      GridBagConstraints gbc,
			      int x, int y, int dx, int dy, int wx, int wy)
    {
	gbc.gridx = x;
	gbc.gridy = y;
	gbc.gridwidth = dx;
	gbc.gridheight = dy;
	gbc.weightx = wx;
	gbc.weighty = wy;
	target.add(c, gbc);
    }

    public static void gblAdd(Container target, Component c,
			      GridBagConstraints gbc,
			      int x, int y, int dx, int dy, int wx, int wy,
			      int I1, int I2, int I3, int I4)
    {
	Insets oldInsets = gbc.insets;
	gbc.gridx = x;
	gbc.gridy = y;
	gbc.gridwidth = dx;
	gbc.gridheight = dy;
	gbc.weightx = wx;
	gbc.weighty = wy;
	gbc.insets = new Insets(I1, I2, I3, I4);
	target.add(c, gbc);
	gbc.insets = oldInsets;
    }

    public static int getBestIncrement(int initialIncrement)
    {
	int[] indices = {1, 2, 5, 25};
	int best = -1;
	for (int i=0; i<indices.length; i++) {
	    int t=0;
	    int sum=0;
	    while ((sum = (int)(indices[i] * Math.pow(10,t)))
		   < initialIncrement)
		t++;
	    if((sum-initialIncrement) < (best-initialIncrement) || best < 0)
		best = sum;
	}
	return best;
    }

    // do not remove yet - still being used by various windows
    // remove only after the whole of projections is converted to swing.
    // Chee Wai - 10/29/2002
    public static Menu makeMenu(Object parent, Object[] items, Object target)
    {
	  Menu m = null;
	  if (parent instanceof Menu)
		 m = (Menu)parent;
	  else if (parent instanceof String)
		 m = new Menu((String)parent);
	  else
		 return null;

	  for (int i = 0; i < items.length; i++)
	  {
		 if (items[i] instanceof String)
		 {
			MenuItem mi = new MenuItem((String)items[i]);
			if (target instanceof ActionListener)
			   mi.addActionListener((ActionListener)target);
			m.add(mi);
		 }
		 else if (items[i] instanceof CheckboxMenuItem && target instanceof ItemListener)
		 {
			CheckboxMenuItem cmi = (CheckboxMenuItem)items[i];
			cmi.addItemListener((ItemListener)target);
			m.add(cmi);
		 }
		 else if (items[i] instanceof MenuItem)
		 {
			MenuItem mi = (MenuItem)items[i];
			if (target instanceof ActionListener)
			   mi.addActionListener((ActionListener)target);
			m.add(mi);
		 }
		 else if (items[i] == null)
			m.addSeparator();
	  }

	  return m;
   }

    /* Swing version of the above function */
    public static JMenu makeJMenu(Object parent, Object[] items,
				  boolean[] enabled, Object target)
    {
	JMenu m = null;
	if (parent instanceof JMenu)
	    m = (JMenu)parent;
	else if (parent instanceof String)
	    m = new JMenu((String)parent);
	else
	    return null;

	for (int i = 0; i < items.length; i++)
	    {
		if (items[i] instanceof String)
		    {
			JMenuItem mi = new JMenuItem((String)items[i]);
			if (target instanceof ActionListener)
			    {
				mi.addActionListener((ActionListener)target);
			    }
			m.add(mi);
		    }
		else if (items[i] instanceof JCheckBoxMenuItem && target instanceof ItemListener)
		    {
			JCheckBoxMenuItem cmi = (JCheckBoxMenuItem)items[i];
			cmi.addItemListener((ItemListener)target);
			m.add(cmi);
		    }
		else if (items[i] instanceof JRadioButtonMenuItem)
		    {
			JRadioButtonMenuItem cmi = (JRadioButtonMenuItem)items[i];
			cmi.addActionListener((ActionListener)target);
			m.add(cmi);
		    }
		else if (items[i] instanceof JMenuItem)
		    {
			JMenuItem mi = (JMenuItem)items[i];
			if (target instanceof ActionListener)
			    mi.addActionListener((ActionListener)target);
			m.add(mi);
		    }
		else if (items[i] == null)
		    {
			m.addSeparator();
		    }
	    }

	return m;
    }

   public static void waitForImage(Component component, Image image)
   {
	  MediaTracker tracker = new MediaTracker(component);
	  try
	  {
		 tracker.addImage(image, 0);
		 tracker.waitForID(0);
	  }
	  catch(InterruptedException e)
	  {
		 e.printStackTrace();
	  }
   }
   public static void wallPaper(Component component, Graphics  g, Image image)
   {
	  Dimension compsize = component.getSize();
	  Util.waitForImage(component, image);

	  int patchW = image.getWidth(component);
	  int patchH = image.getHeight(component);

	  for(int r=0; r < compsize.width; r += patchW)
	  {
		 for(int c=0; c < compsize.height; c += patchH)
			g.drawImage(image, r, c, component);
	  }
   }

    /**
     *  Added by Chee Wai Lee
     *  4/24/2002
     *
     *  For the purpose of applying sorting maps to various types of arrays.
     */
    public static boolean[] applyMap(boolean[] dataArray, int[] map) {
	if (map == null) {
	    return dataArray;
	} else {
	    boolean returnArray[] = new boolean[dataArray.length];
	    for (int i=0; i<returnArray.length; i++) {
		returnArray[i] = dataArray[map[i]];
	    }
	    return returnArray;
	}
    }

    public static Color[] applyMap(Color[] dataArray, int[] map) {
	if (map == null) {
	    return dataArray;
	} else {
	    Color returnArray[] = new Color[dataArray.length];
	    for (int i=0; i<returnArray.length; i++) {
		returnArray[i] = dataArray[map[i]];
	    }
	    return returnArray;
	}
    }

    public static String[] applyMap(String[] dataArray, int[] map) {
	if (map == null) {
	    return dataArray;
	} else {
	    String returnArray[] = new String[dataArray.length];
	    for (int i=0; i<returnArray.length; i++) {
		returnArray[i] = dataArray[map[i]];
	    }
	    return returnArray;
	}
    }

    public static double[][] applyMap(double[][] dataArray, int[] map) {
	if (map == null) {
	    return dataArray;
	} else {
	    double returnArray[][] = new double[dataArray.length][];
	    for (int config=0; config<returnArray.length; config++) {
		returnArray[config] = new double[dataArray[config].length];
		for (int ep=0; ep<returnArray[config].length; ep++) {
		    returnArray[config][ep] = dataArray[config][map[ep]];
		}
	    }
	    return returnArray;
	}
    }

    /**
     *   pre-condition: filter1 and filter2 are of the same length
     */
    public static boolean[] orFilters(boolean[] filter1, boolean[] filter2) {
	boolean newFilter[] = new boolean[filter1.length];

	for (int i=0; i<newFilter.length; i++) {
	    newFilter[i] = (filter1[i] || filter2[i]);
	}
	return newFilter;
    }

    /**
     *   pre-condition: filter1 and filter2 are of the same length
     */
    public static boolean[] andFilters(boolean[] filter1, boolean[] filter2) {
	boolean newFilter[] = new boolean[filter1.length];

	for (int i=0; i<newFilter.length; i++) {
	    newFilter[i] = (filter1[i] && filter2[i]);
	}
	return newFilter;
    }

    /**
     *	Modified by Sharon Ma 03/01/03
     *	Changed color.map to a readable format
     *  Separate saved data for each graph view
     */
    public static void saveColors(Color[] colors, String graphType) throws IOException {
	
	File filename = new File(Analysis.getLogDirectory() +
				 File.separator +
				 "color.map");
	boolean fileExists = filename.exists();
	RandomAccessFile accessFile = new RandomAccessFile(filename, "rw");
	String tempString = new String();
	
	//If more graphs are created put them here
	int numOfGraph=6;
	String[] typeArray = new String[numOfGraph];
	typeArray[0] = "Graph Graph";
	typeArray[1] = "Timeline Graph";
	typeArray[2] = "Usage Profile Graph";
	typeArray[3] = "Animation Graph";
	typeArray[4] = "Histogram Graph";
	typeArray[5] = "Overview Graph";
	
	if(!fileExists){
		//Output graph names and default color
		for(int i=0; i<numOfGraph;i++){
			accessFile.writeBytes(typeArray[i] + "\n");
			for(int j=0; j<colors.length; j++){
				accessFile.writeBytes(j + " ");
	    			accessFile.writeBytes(tempString.valueOf(Analysis.getEntryColor(j).getRed()));
	    			accessFile.writeBytes(" ");
	    			accessFile.writeBytes(tempString.valueOf(Analysis.getEntryColor(j).getGreen()));
	    			accessFile.writeBytes(" ");
	    			accessFile.writeBytes(tempString.valueOf(Analysis.getEntryColor(j).getBlue()));
	    			accessFile.writeBytes(";     ");
			}
			accessFile.writeBytes("                    ");
			accessFile.writeBytes("\n");
			accessFile.writeBytes("\n");
		}
	}
	
	long tempLong = 0;
	accessFile.seek(tempLong);
	int lineNumber;
	
	for(int i=0; i<numOfGraph; i++){
		if(graphType.compareTo(typeArray[i]) == 0){
			lineNumber = 3*i+1;
			//Go to the correct line number to output
			for(int k=0; k<lineNumber; k++)
				tempString = accessFile.readLine(); 
			
			for (int j=0; j<colors.length; j++) {
	    			accessFile.writeBytes(j + " ");
	    			accessFile.writeBytes(tempString.valueOf(colors[j].getRed()));
	    			accessFile.writeBytes(" ");
	    			accessFile.writeBytes(tempString.valueOf(colors[j].getGreen()));
	    			accessFile.writeBytes(" ");
	    			accessFile.writeBytes(tempString.valueOf(colors[j].getBlue()));
	    			accessFile.writeBytes(";     ");
			}
		}
	}
	accessFile.close();
    }

     /**
     *	Modified by Sharon Ma 03/01/03
     *	Changed color.map to a readable format
     */
    public static void restoreColors(Color[] colors, String graphType) throws IOException{
	File filename = new File(Analysis.getLogDirectory() +
				 File.separator +
				 "color.map");
	boolean fileExists = filename.exists();
	RandomAccessFile accessFile = new RandomAccessFile(filename, "rw");
	String tempString = new String();
	
	Integer tempInteger = new Integer(0);
	if(fileExists){
		tempString = accessFile.readLine();
		while(!(tempString.compareTo("null") == 0)){
			if(tempString.compareTo(graphType) == 0){
			StringTokenizer tokenizer = 
				new StringTokenizer(accessFile.readLine(), " ;");
			for (int i=0; i<colors.length; i++){
				tempString = tokenizer.nextToken();
				colors[i] = new Color(tempInteger.parseInt(tokenizer.nextToken()),
							tempInteger.parseInt(tokenizer.nextToken()),
							tempInteger.parseInt(tokenizer.nextToken()));
			}
			tempString = "null";
			}
			else{tempString = accessFile.readLine();}
		}
	}
	else{
		for (int i=0; i<colors.length; i++) {
			colors[i] = Analysis.getEntryColor(i);
	   	}
	}
	accessFile.close();
    }
}
