package projections.gui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.StringTokenizer;
import java.lang.Integer;


public class Util
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

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
    /* Modified by Chao Mei for adding support for sublevel menus */
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
		else if (items[i] instanceof JCheckBoxMenuItem && 
			 target instanceof ItemListener)
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
                /* Support for sub-level menu */
                else if (items[i] instanceof String []) {
                    String[] subMenuStr = (String [])items[i];
                    JMenu subMenu = new JMenu(subMenuStr[0]);
                    m.add(subMenu);

                    for(int index=1; index<subMenuStr.length; index++){
                        JMenuItem mi = new JMenuItem(subMenuStr[index]);
			if (target instanceof ActionListener)
			    {
				mi.addActionListener((ActionListener)target);
			    }
			subMenu.add(mi);
                    }
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
    public static void saveColors(Color[] colors, String graphType, String filePath) throws IOException {
	
	// **sharon** somehow MainWindow.runObject[myRun].getLogDirectory() returns null
	//File filename = new File(MainWindow.runObject[myRun].getLogDirectory() +
	//			 File.separator +
	//			 "color.map");
	
	// loop twice: once to save to pathPath, once to save to color.map
	
	File filename = new File("bin/color.map");
	int again;
	if(filePath == null) {again = 0;}
	else                {again = 1;}
	
	do{
	
	boolean fileExists = filename.exists();
	RandomAccessFile accessFile = new RandomAccessFile(filename, "rw");

	@SuppressWarnings("unused")
	String tempString = new String();
	
	//If more graphs are created put them here
	int numOfGraph=1;
	String[] typeArray = new String[numOfGraph];
	typeArray[0] = "Timeline Graph";
	
	
	if(!fileExists){
		//Output graph names and default color
		for(int i=0; i<numOfGraph;i++){
			accessFile.writeBytes(typeArray[i] + "\n");
			for(int j=0; j<colors.length; j++){
				accessFile.writeBytes(j + " ");
	    			accessFile.writeBytes(String.valueOf(MainWindow.runObject[myRun].getEntryColor(j).getRed()));
	    			accessFile.writeBytes(" ");
	    			accessFile.writeBytes(String.valueOf(MainWindow.runObject[myRun].getEntryColor(j).getGreen()));
	    			accessFile.writeBytes(" ");
	    			accessFile.writeBytes(String.valueOf(MainWindow.runObject[myRun].getEntryColor(j).getBlue()));
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
	    			accessFile.writeBytes(String.valueOf(colors[j].getRed()));
	    			accessFile.writeBytes(" ");
	    			accessFile.writeBytes(String.valueOf(colors[j].getGreen()));
	    			accessFile.writeBytes(" ");
	    			accessFile.writeBytes(String.valueOf(colors[j].getBlue()));
	    			accessFile.writeBytes(";     ");
			}
		}
	}
	
	if (filePath != null) { filename = new File(filePath); }
	again--;
	
	accessFile.close();
	
	}while(again >= 0);
	
	
	
    }

    /**
     *  New color map routines are a little messed up. Old routines
     *  restored by Chee Wai 3/3/2005
     */
    public static void saveColors(Color[] colors, String Useless) 
	throws IOException 
    {
        FileOutputStream fileStream = 
            new FileOutputStream(MainWindow.runObject[myRun].getLogDirectory() +
                                 File.separator +
                                 "color.map");
        ObjectOutputStream objStream =
            new ObjectOutputStream(fileStream);
        for (int i=0; i<colors.length; i++) {
            objStream.writeObject(colors[i]);
        }
        objStream.close();
    }

    public static void restoreColors(Color[] colors, String Useless)
        throws IOException, ClassNotFoundException
    {
        FileInputStream fileStream =
            new FileInputStream(MainWindow.runObject[myRun].getLogDirectory() +
                                File.separator +
                                "color.map");
        ObjectInputStream objStream =
            new ObjectInputStream(fileStream);
        for (int i=0; i<colors.length; i++) {
            colors[i] = (Color)objStream.readObject();
        }
        objStream.close();
    }

     /**
     *	Modified by Sharon Ma 03/01/03
     *	Changed color.map to a readable format
     */
    public static void restoreColors(Color[] colors, String graphType, String filePath) throws IOException{
	File filename;
	if(filePath == null){
		filename = new File("bin/color.map");}
	else{
		filename = new File(filePath);}
	
	boolean fileExists = filename.exists();
	RandomAccessFile accessFile = new RandomAccessFile(filename, "rw");
	String tempString = new String();
	
	if(fileExists){
		tempString = accessFile.readLine();
		while(!(tempString.compareTo("null") == 0)){
			if(tempString.compareTo(graphType) == 0){
			StringTokenizer tokenizer = 
				new StringTokenizer(accessFile.readLine(), " ;");
			for (int i=0; i<colors.length; i++){
				tempString = tokenizer.nextToken();
				colors[i] = new Color(Integer.parseInt(tokenizer.nextToken()),
							Integer.parseInt(tokenizer.nextToken()),
							Integer.parseInt(tokenizer.nextToken()));
			}
			tempString = "null";
			}
			else{tempString = accessFile.readLine();}
		}
	}
	else{
		for (int i=0; i<colors.length; i++) {
			colors[i] = MainWindow.runObject[myRun].getEntryColor(i);
	   	}
	}
	
	// update the restored color setting to bin/color.map
	saveColors(colors, graphType, null);
	
	accessFile.close();
    }
}
