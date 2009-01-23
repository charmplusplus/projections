package projections.gui;

import java.io.*;
import java.awt.*;


/**
 *  ColorManager.java
 *  8/2/2006 - replaces ColorSaver.java in analysis directory
 *
 *  Manages Color Manager for possibly multiple instances of Analysis-type
 *  objects/classes. Static and performs services for these Analysis-type
 *  objects.
 */

public class ColorManager
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

    private static String filename = null;

    public static void setDefaultLocation(String fname) {
	filename = fname;
    }

    public static Color[][] initializeColors() {
	Color retColors[][] = new Color[Analysis.NUM_ACTIVITIES][];
	for (int i=0; i<retColors.length; i++) {
	    retColors[i] = 
		createColorMap(MainWindow.runObject[myRun].getNumActivity(i));
	}
	return retColors;
    }

    public static Color[][] initializeColors(String filename)
	throws IOException
    {
	ObjectInputStream in =
	    new ObjectInputStream(new FileInputStream(filename));
	Color retColors[][] = new Color[Analysis.NUM_ACTIVITIES][];
	String names[] = null;
	Color tempColors[][] = null;
	int index = 0;
	try {
	    names = (String[])(in.readObject());
	    tempColors = (Color[][])(in.readObject());
	    if (names.length != tempColors.length) {
		System.err.println("WARNING: Color file corrupted. " +
				   "Number of names do not match number " +
				   "of sets of colors.");
		return null;
	    }
	    for (int i=0; i<tempColors.length; i++) {
		index = MainWindow.runObject[myRun].stringToActivity(names[i]);
		if (index != -1) {
		    retColors[index] = tempColors[i];
		}
	    }
	    for (int i=0; i<retColors.length; i++) {
		if (retColors[i] == null) {
		    retColors[i] = 
			createColorMap(MainWindow.runObject[myRun].getNumActivity(i));
		}
	    }
	} catch (ClassCastException e) {
	    // Assume that the color file is deprecated and uses the old
	    // single array format, so back-off to load that instead whilst
	    // constructing the rest of the array from scratch.
	    in.close();
	    int activity = Analysis.PROJECTIONS;
	    retColors[activity] =
		new Color[MainWindow.runObject[myRun].getNumActivity(activity)];
	    loadActivityColors(activity,retColors[activity]);
	    for (int i=0; i<retColors.length; i++) {
		if (retColors[i] == null) {
		    retColors[i] = 
			createColorMap(MainWindow.runObject[myRun].getNumActivity(i));
		}
	    }
	} catch (Exception e) {
	    System.err.println("WARNING: Failed to read saved color object");
	    System.err.println(e);
	    return null;
	}
	in.close();
	return retColors;
    }

    /**
     *  This is the code used by tools to load colors of a particular type.
     *  In the event the color module used is an old version, the method
     *  will catch the exception and fallback to the previous code which
     *  expects a 1D array instead of a 2D array.
     *
     *  Silly java handler-thing detected. We have to copy the individual
     *  colors into the original arrays or risk losing proper management
     *  of all the pointer structures.
     */
    public static void loadActivityColors(int type, Color[] origColors) 
	throws IOException
    {
	ObjectInputStream in =
	    new ObjectInputStream(new FileInputStream(filename));
	Color retColors[][] = null;
	try {
	    retColors = (Color[][])(in.readObject());
	} catch (ClassCastException e) {
	    // attempt a fall-back version for backward compatibility
	    retColors[type] = loadActivityColorsFallback(type);
	} catch (Exception e) {
	    System.err.println("WARNING: Failed to read saved color object");
	    System.err.println(e);
	}
	in.close();
	if ((type >= 0) && (type < Analysis.NUM_ACTIVITIES)) {
	    // Copy the individual colors. This is done so that every
	    // tool can see those same colors.
	    if (origColors.length == retColors[type].length) {
		for (int i=0; i<origColors.length; i++) {
		    origColors[i] = retColors[type][i];
		}
	    } else {
		System.err.println("WARNING: Current color array length of " +
				   origColors.length + " does not match " +
				   "stored color array length of " +
				   retColors[type].length + ". Load " +
				   "request rejected.");
	    }
	} else {
	    System.err.println("WARNING - Internal Error: Activity type " +
			       type + " unknown when requesting load " +
			       "colors. Please inform developers");
	}
    }

    /**
     *  This is stand-in code for current tool codes to work while
     *  in preparation for a move to an activity-based
     *  array of arrays of colors.
     *
     *  This should not be required except if a different color set is
     *  required.
     */
    public static Color[] loadActivityColorsFallback(int type) 
	throws IOException
    {
	Color retColors[] = null;
	if (type == Analysis.PROJECTIONS) {
	    ObjectInputStream in =
		new ObjectInputStream(new FileInputStream(filename));
	    try {
		retColors = (Color[])(in.readObject());
	    } catch (ClassCastException e) {
		System.err.println("WARNING: Unexpected object format when " +
				   "attempting to read color file");
		System.err.println(e);
		return null;
	    } catch (Exception e) {
		System.err.println("WARNING: Failed to read saved color " +
				   "object");
		System.err.println(e);
		return null;
	    }
	    in.close();
	} else {
	    retColors = createColorMap(MainWindow.runObject[myRun].getNumActivity(type));
	}
	return retColors;
    }

    public static void saveColors(Color colors[][]) 
    {
	try {
	    ObjectOutputStream out =
		new ObjectOutputStream(new FileOutputStream(filename));
	    out.writeObject(colors);
	    out.close();
	} catch (Exception e) {
	    System.err.println("WARNING: Failed to save color file to " +
			       filename);
	    System.err.println(e);
	}
    }

    /** ************** COLOR CREATION ROUTINES **************** */

    public static Color[] createGrayscaleColorMap(int numColors) {
	Color[] colors = new Color[numColors];
	float H = (float)1.0;
	float S = (float)0.0;
	float B = (float)0.9; // initial white value would be bad.
	float delta = (float)(0.8/numColors); // extreme black is also avoided
	// as long as S==0, H does not matter, so scale according to B
	for (int i=0; i<numColors; i++) {
	    colors[i] = Color.getHSBColor(H, S, B);
	    B -= delta;
	    if (B < 0.1) {
		B = (float)0.1;
	    }
	}
	return colors;
    }

    public static Color[] createColorMap(int numColors) {
	Color[] colors = new Color[numColors];
	float H = (float)1.0;
	float S = (float)1.0;
	float B = (float)1.0;
	float delta = (float)(1.0/numColors);
	for(int i=0; i<numColors; i++) {
	    colors[i] = Color.getHSBColor(H, S, B);
	    H -= delta;
	    if(H < 0.0) { H = (float)1.0; }
	}
	return colors;
    }

//    /**
//     *  Wrapper version for using a default weight assignment.
//     */
//    public static Color[] createColorMap(int numEPs, int epMap[]) {
//	int numSignificant = epMap.length;
//	int[] weights = new int[numSignificant];
//	
//	if (numSignificant > 0) {
//	    // default assignment of weights using an accelerating increment
//	    // method (acceleration = 2; initial value = 5)
//	    int acceleration = 2;
//	    int increment = 7;
//	    weights[numSignificant-1] = 5;
//	    for (int ep=numSignificant-2; ep>=0; ep--) {
//		weights[ep] = weights[ep+1] + increment;
//		increment += acceleration;
//	    }
//	}
//	return createColorMap(numEPs, epMap, weights);
//    }

//    /**
//     *  A more advanced version of color assignment that takes a map
//     *  of significant entry methods in sorted order and assigns more
//     *  distinctly different (hue) colors to more significant entry
//     *  methods. Significance is assigned by the tool requesting the
//     *  color map. This scheme is still arbitrary.
//     *
//     *  numEPs give a total of color assignments required.
//     */
//    public static Color[] createColorMap(int numEPs, int epMap[], 
//					 int weights[]) {
//	Color[] colors = new Color[numEPs];
//
//	int numSignificant = epMap.length;
//	// no significant values, so return uniform color map
//	if (numSignificant == 0) {
//	    return createColorMap(numEPs);
//	}
//	int total = 0;
//	for (int ep=0; ep<numSignificant; ep++) {
//	    total += weights[ep];
//	}
//	// a linear distribution segment of the remaining color space should 
//	// not be larger than the smallest final hue segment assigned to a
//	// significant ep. Formula: x >= 1.0/kc+1.0 where x is the hue space
//	// allocated to significant eps, k is the % weight assigned to the
//	// smallest significant ep and c is the number of insignificant eps.
//	// x should be at least 66% of the hue space or it wouldn't make a
//	// difference (when a small number of significant elements are
//	// presented).
//	double k = weights[numSignificant-1]/(double)total;
//	int c = numEPs-numSignificant;
//	double x = 1.0/(k*c + 1.0);
//	if (x < 0.67) {
//	    x = 0.67;
//	}
//
//	double currentHue = 1.0;
//	double saturation = 1.0;
//	double brightness = 1.0;
//	// assign colors to significant eps
//	for (int ep=0; ep<numSignificant; ep++) {
//	    colors[epMap[ep]] = Color.getHSBColor((float)currentHue, 
//						  (float)saturation,
//						  (float)brightness);
//	    currentHue -= (weights[ep]/(double)total)*x;
//	}
//	// assign colors to all other eps
//	double delta = currentHue/c;
//	for (int ep=0; ep<numEPs; ep++) {
//	    // needs assignment
//	    if (colors[ep] == null) {
//		colors[ep] = Color.getHSBColor((float)currentHue, 
//					       (float)saturation,
//					       (float)brightness);
//		currentHue -= delta;
//	    }
//	}
//	return colors;
//    }


}

