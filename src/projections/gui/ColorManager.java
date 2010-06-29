package projections.gui;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeMap;
import java.util.Vector;

import projections.Tools.Timeline.Data;


/**
 *  Manages colors for an Analysis object.
 */

public class ColorManager
{

	private static String filename = null;
	private Analysis a;

	public ColorManager(String fname, Analysis a) {
		filename = fname;
		this.a = a;
	}

	protected Color[][] initializeColors() throws Exception {
		Exception exception = null;
		Color[][] colorToReturn = null;
		File f = new File(filename);
		
		if(f.exists()){
			// First try to read colors from a file
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(new FileInputStream(f));
			} catch (FileNotFoundException e1) {
				exception = e1;
			} catch (IOException e1) {
				exception = e1;
			}
			
			
			try {
				Object o1 = in.readObject();

				// Check to see if this is the expected format
				if(o1 instanceof String[]) {
					// This format uses an array of entry point names and corresponding colors

					Object o2 = in.readObject();
					if(o2 instanceof Color[]){
						String names[] = (String[]) o1;
						Color inputColors[] = (Color[]) o2;

						Color retColors[][] = new Color[Analysis.NUM_ACTIVITIES][];

						// First fill in some default color maps
						for (int i=0; i<Analysis.NUM_ACTIVITIES; i++) {
							retColors[i] = createColorMap(a.getNumActivity(i));
						}

						// Now fill in any values found in the file
						for (int i=0; i<inputColors.length; i++) {
							// Overwrite with any stored colors found in the file
							String epName = names[i];
							int k = a.getEntryIDByName(epName);
							if(k>=0){
//								System.out.println("Using color found in file for " + epName);
								retColors[Analysis.PROJECTIONS][k] = inputColors[i];
							}
						}
						

						in.close();
						colorToReturn = retColors;
					}	
					else {
						exception = new Exception("Read object for color has failed.");
					}
					

				} else if (o1 instanceof Color[][]) {
					// Else, it may be an old format that is just a color array for each type of activity

					Color retColors[][] = (Color[][]) o1;

					// Fill in any null activity color maps
					for (int i=0; i<retColors.length; i++) {
						if (retColors[i] == null) {
							retColors[i] = createColorMap(a.getNumActivity(i));
						}
					}
					in.close();
					colorToReturn = retColors;
				} else if (o1 instanceof Color[]) {
					// Else, it may be an ancient format that is just a color array

					Color retColors[][] = new Color[Analysis.NUM_ACTIVITIES][];
					retColors[Analysis.PROJECTIONS] = (Color[])o1;
					
					// Fill in any null activity color maps
					for (int i=0; i<retColors.length; i++) {
						if (retColors[i] == null) {
							retColors[i] = createColorMap(a.getNumActivity(i));
						}
					}

					in.close();
					colorToReturn = retColors;
				}

				else {
					exception = new Exception("Read object has failed.");
				}
				
				
			} catch (IOException e) {
				exception = e;
			} catch (ClassNotFoundException e) {
				exception = e;
			}

		}
		else {
			exception = new FileNotFoundException("The file, " + filename + ", was not found.");
		}
		
		if (colorToReturn != null) {
			return colorToReturn;
		}
		else {
			throw exception;
		}
	
		// Otherwise, just generate the new colors
		
	
	}


	public Color[][] defaultColorMap() {
		Color retColors[][] = new Color[Analysis.NUM_ACTIVITIES][];
		for (int i=0; i<retColors.length; i++) {
			retColors[i] = createColorMap(a.getNumActivity(i));
		}
		return retColors;
	}

	
	protected void saveColorsData(Data d) {
		saveColors(d.entryColorsMapping);
	}
	
/** Write out the colors to the file. Make sure that this output format is readable 
 *  by initializeColors() above.
 */
	protected void saveColors(TreeMap<Integer,Color> overrideMapping) 
	{
		try {
			System.out.println("Saving colors in new format");
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));

			int numEPs = a.getEntryCount();
			String[]  o1 = new String[numEPs];
			Color[] o2 = new Color[numEPs];
		    for(int i=0; i<numEPs; i++){
		    	o1[i] = a.getEntryNameByIndex(i);
		    	o2[i] = a.getEPColorMap()[i];
		    	if (overrideMapping!=null && overrideMapping.containsKey(i))
		    		o2[i] = overrideMapping.get(i);
		    }
			
			out.writeObject(o1);
			out.writeObject(o2);

			out.close();
		} catch (Exception e) {
			System.err.println("WARNING: Failed to save color file to " + filename);
			System.err.println(e);
		}
	}
	
//	protected void saveColorsOldVersion(Color colors[][]) 
//	{
//		try {
//			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
//			out.writeObject(colors);
//			out.close();
//		} catch (Exception e) {
//			System.err.println("WARNING: Failed to save color file to " + filename);
//			System.err.println(e);
//		}
//	}

	
	
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
	
	public static Color[] createComplementaryColorMap(int numUserEntries) {
		if (numUserEntries>=2) {
			float[] hLookup = new float[numUserEntries];
			hLookup[0] = (float)0.0;
			hLookup[1] = (float)180.0;
			Color[] complementaryColors = new Color[numUserEntries];
			float S = (float)1.0;
			float B = (float)1.0;
			
			for (int i=1; i<(numUserEntries/2); i++) {
				hLookup[i*2] = (hLookup[i]/2);
				if (((i*2)+1)<numUserEntries) {
					hLookup[(i*2)+1] = (hLookup[i]+360)/2;
				}
			}
			
			for (int i=0; i<numUserEntries; i++) {
				//When i is a multiple of 15 or 16, it's hue will be reddish... make sure the two look different enough
				//from each other
				if((i+1)%15==0)
					complementaryColors[i] = Color.getHSBColor((hLookup[i])/360, (float)1.0, (float)1.0);
				else if ((i+1)%16==0)
					complementaryColors[i] = Color.getHSBColor((hLookup[i])/360, (float)0.25, (float)1.0);
				else				
					complementaryColors[i] = Color.getHSBColor((hLookup[i])/360, S, B);
				
				//Modify S and B in a cyclic way
				if ((i+1)%4==0)
					S-=(0.14);
				if ((i+3)%4==0)
					B-=(0.19);
				
				//Modify S and B to be in proper range
				if ((B+S)<1.3) {
					B=(float)(1.0/B);
					S=(float)(1.0/S);
				}
				if (B<=0)
					B=-B;
				if (B>1)
					B-=(int)B;
				if (B<=.4)
					B = (float)1.0;
				
				if (S<=0)
					S=-S;
				if (S>1)
					S-=(int)S;
				if (S<=.4)
					S = (float)1.0;
			}
			return complementaryColors;
		}
		return new Color[0];
	}

	public static Color[] entryColorsByFrequency(Color[] complementaryMap, Vector<Integer> freqVector) {
		Color [] colors = new Color[freqVector.size()];
		for (int i=0; i< freqVector.size(); i++) {
			colors[freqVector.get(i)] = complementaryMap[i];
		}
		return colors;
	}
}
