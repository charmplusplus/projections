package projections.analysis;

import java.io.*;
import java.util.*;
import java.awt.*;

public class ColorSaver
{
    private static String filename = null;

    public static void setLocation(String fname) {
	filename = fname;
    }

    public static Color[] loadColors() 
	throws IOException
    {
	ObjectInputStream in =
	    new ObjectInputStream(new FileInputStream(filename));
	Color retColors[] = null;
	try {
	    retColors = (Color[])(in.readObject());
	} catch (Exception e) {
	    System.err.println("Failed to find array class for Colors");
	}
	in.close();
	return retColors;
    }

    public static void save(Color colors[]) 
	throws IOException
    {
	ObjectOutputStream out =
	    new ObjectOutputStream(new FileOutputStream(filename));
	out.writeObject(colors);
	out.close();
    }
}
