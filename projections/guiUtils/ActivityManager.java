package projections.guiUtils;

import java.awt.*;

import java.util.*;

import projections.gui.*;

/**
 *  ActivityManager.java
 *  Chee Wai Lee - 7/7/2004
 *
 *  Presents a unified approach to entity rendering in Projections.
 *  Features include:
 *
 *  1) 2-level categorization/grouping capabilities.
 *  2) Default activity properties are names and color.
 *  3) The user may assign more properties that can be accessed.
 *
 */

public class ActivityManager {

    // Hashtable to map a category name + local activity index pair
    // to an internal Vector index.
    Hashtable activityMap;

    // category information. There's no current need for efficiency
    // since there should not be many categories. If there is, then
    // a Hashtable should be used for the forward-mapping process.
    // eg. Hashtable categoryMap;
    Vector categoryNames;
    Vector categoryActivities; // Vector of Vector of internal indices

    // built-in activity attributes.
    Vector activityNames;
    Vector activityColors;

    // user-definited activity attributes (Vector of Vectors)
    Vector activityProperties;
  
    // cached values of color (for efficiency) indexed by category and
    // local activity index.
    Color cachedColors[][];

    boolean initialized = false;

    public ActivityManager() {
	activityMap = new Hashtable();

	categoryNames = new Vector();
	categoryActivities = new Vector();

	activityNames = new Vector();
	activityColors = new Vector();
	activityProperties = new Vector();
    }

    // The triple should uniquely identify any activity in Projections.
    public void registerActivity(String category, int index, String name) {
	if (!initialized) {
	    if (!categoryNames.contains(category)) {
		categoryNames.add(category);
		categoryActivities.add(new Vector());
	    }
	    int internalIndex = activityNames.size();
	    activityMap.put(category+"|"+index, 
			    new Integer(internalIndex));
	    activityNames.add(name);
	    int categoryIndex = categoryNames.indexOf(category);
	    ((Vector)categoryActivities.get(categoryIndex)).add(new Integer(internalIndex));
	} else {
	    System.err.println("Warning! Internal inconsistency - " +
			       "Projections attempting to register " +
			       "activities to an already initialized set.");
	}
    } 

    public Color[] getColors(String category) {
	int categoryIndex =
	    categoryNames.indexOf(category);
	return cachedColors[categoryIndex];
    }

    public void registrationDone() {
	resetColors(); // set default colors
	// construct color cache.
	cachedColors = new Color[categoryNames.size()][];
	for (int cat=0; cat<categoryNames.size(); cat++) {
	    Vector activityList = (Vector)categoryActivities.get(cat);
	    cachedColors[cat] = new Color[activityList.size()];
	    for (int i=0; i<activityList.size(); i++) {
		int index = ((Integer)activityList.get(i)).intValue();
		cachedColors[cat][index] = 
		    (Color)activityColors.get(index);
	    }
	}
	initialized = true;
    }

    public void setColors(String category, Color inColors[]) {
	int categoryIndex =
	    categoryNames.indexOf(category);
	cachedColors[categoryIndex] = inColors;
    }

    public void resetColors() {
	Color tmpColors[] =
	    Analysis.createColorMap(activityNames.size());
	for (int i=0; i<activityNames.size(); i++) {
	    activityColors.set(i, tmpColors[i]);
	}
    }
    
    // This is the accessor that acquires the number of activities managed.
    public int getNumActivities() {
	if (initialized) {
	    return activityNames.size();
	} else {
	    return 0;
	}
    }
}
