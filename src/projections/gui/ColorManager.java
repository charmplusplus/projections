package projections.gui;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


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

	protected Color[][] initializeColors() {
		
		File f = new File(filename);
		
		if(f.exists()){
			// First try to read colors from a file
			
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(new FileInputStream(f));
			} catch (FileNotFoundException e1) {
				System.err.println("Could not open file, even though it exists: " + filename);
				return defaultColorMap();
			} catch (IOException e1) {
				System.err.println("Could not read from color file: " + filename);
				return defaultColorMap();
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
						return retColors;
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
					return retColors;
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
					return retColors;
				}

									
				
				
			} catch (IOException e) {
				System.out.println("WARNING: Could not read colors from file (IOException): " + filename);
				return defaultColorMap();
			} catch (ClassNotFoundException e) {
				System.out.println("WARNING: Could not read colors from file (ClassNotFoundException): " + filename);
				return defaultColorMap();
			}

		}
	
		// Otherwise, just generate the new colors
		return defaultColorMap();
	
	}


	private Color[][] defaultColorMap() {
		Color retColors[][] = new Color[Analysis.NUM_ACTIVITIES][];
		for (int i=0; i<retColors.length; i++) {
			retColors[i] = createColorMap(a.getNumActivity(i));
		}
		return retColors;	
	}

	
/** Write out the colors to the file. Make sure that this output format is readable 
 *  by initializeColors() above.
 */
	protected void saveColors() 
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
	
}