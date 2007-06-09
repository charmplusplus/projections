package projections.analysis;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import projections.misc.*;

/** 
 *  ProjectionsConfigurationReader.java
 *  by Chee Wai Lee.
 *  10/24/2005
 *
 *  ProjectionsConfigurationReader reads a simple line-based data file
 *  which describes visualization configurations (like total run time).
 *
 */
public class ProjectionsConfigurationReader
{
    private String baseName;
    private String configurationName;

    private boolean dirty;

    // Configuration Variables. They *must* begin with "RC_"
    // For convenience of coding, these are static. This will have to
    // be changed once multiple runs are supported generically in
    // Projections.
    public static Long RC_GLOBAL_END_TIME = new Long(-1);
    public static Long RC_POSE_REAL_TIME = new Long(-1);
    public static Long RC_POSE_VIRT_TIME = new Long(-1);    
    public static Boolean RC_OUTLIER_FILTERED = Boolean.valueOf(false);

    public ProjectionsConfigurationReader(String filename)
    {
	baseName = FileUtils.getBaseName(filename);
	String logDirectory = FileUtils.dirFromFile(filename);
	configurationName = baseName + ".projrc";
	dirty = false;
	try {
	    readfile();
	} catch (LogLoadException e) {
	    System.err.println(e.toString());
	    System.exit(-1);
	}
    }

    public void readfile() 
	throws LogLoadException
    {
	try {
	    BufferedReader InFile = 
		new BufferedReader(new InputStreamReader(new FileInputStream(configurationName)));
	    String Line;
	    while ((Line = InFile.readLine()) != null) {
		StringTokenizer st = new StringTokenizer(Line);
		String s1 = "";
		try {
		    s1 = st.nextToken();
		} catch (NoSuchElementException e) {
		    // empty line, just continue
		    break;
		}
		String tempStr = "";
		// All rc descriptors must start with this string
		if (!s1.startsWith("RC_")) {
		    System.err.println("Warning: Key [" + s1 + "] does not " +
				       "start with RC_ and is rejected.");
		    continue;
		}
		try {
		    Field rcField =
			this.getClass().getField(s1);
		    // The configuration variables must either support the
		    // valueOf(String) method or be String-compatible.
		    // Failure to support valueOf is caught by the exception
		    // NoSuchMethodException and is an internal error (i.e.
		    // a member of the development team used an incompatible
		    // type)
		    try {
			tempStr = st.nextToken();
		    } catch (NoSuchElementException e) {
			// no value, so assign the empty string.
			tempStr = "";
		    }
		    if (Class.forName("java.lang.String").equals(rcField.getType())) {
			rcField.set(this, tempStr);
		    } else {
			rcField.set(this,
				    rcField.getType().getMethod("valueOf", new Class[] {
					Class.forName("java.lang.String")
				    }
								).invoke(null,new Object[] {
								    tempStr
								}));
		    }
		} catch (NoSuchFieldException e) {
		    System.err.println("Warning: Key [" + s1 + "] is " +
				       "not supported on this version " +
				       "of Projections!");
		} catch (Exception e) {
		    // for ClassNotFoundException, NoSuchMethodException,
		    //     IllegalAccessException & SecurityException
		    System.err.println("Internal Error: Encountered when " +
				       "attempting to assign value [" +
				       tempStr + "] to configuration key [" +
				       s1 + "] Please report to " +
				       "developers!");
		    System.err.println(e.toString());
		    System.exit(-1);
		}
	    }
	    InFile.close();
	} catch (FileNotFoundException e) {
	    // no previous rc file. Create new file.
	    try {
		File newRc = new File(configurationName);
		newRc.createNewFile();
	    } catch (IOException ioException) {
		System.err.println("WARNING: Unable to write to rc file [" +
				   configurationName + "]. Reason: ");
		System.err.println(ioException.toString());
	    }
	} catch (IOException e) {
	    throw new LogLoadException (configurationName, 
					LogLoadException.READ);
	}
    }

    public void writeFile() 
	throws LogLoadException
    {
	if (dirty) {
	    try {
		PrintWriter writer =
		    new PrintWriter(new FileWriter(configurationName));
		try {
		    Field rcFields[] =
			this.getClass().getFields();
		    for (int field=0; field<rcFields.length; field++) {
			String fieldname = rcFields[field].getName();
			if (fieldname.startsWith("RC_")) {
			    writer.println(fieldname + " " + 
					   rcFields[field].get(this).toString());
			}
		    }
		} catch (Exception e) {
		    System.err.println("Internal Error: Cannot write " +
				       "configuration file. Please " +
				       "report to developers!");
		    System.err.println(e.toString());
		    System.exit(-1);
		}
		writer.close();
	    } catch (FileNotFoundException e) {
		throw new LogLoadException (configurationName, 
					    LogLoadException.OPEN);
	    } catch (IOException e) {
		throw new LogLoadException (configurationName, 
					    LogLoadException.WRITE);
	    }
	} else {
	    return;
	}
    }

    public void setValue(String key, Object value) {
	// check key for initial correctness
	if (!key.startsWith("RC_")) {
	    System.err.println("Internal Error: Request to set " +
			       "configuration option [" + key +
			       "] not supported! Please report to " +
			       "developers!");
	    System.exit(-1);
	} else {
	    try {
		Field rcField = 
		    this.getClass().getField(key);
		rcField.set(this, value);
		dirty = true;
	    } catch (NoSuchFieldException e) {
		System.err.println("Internal Error: Request to set " +
				   "configuration option [" + key +
				   "] not supported! Please report to " +
				   "developers!");
		System.err.println(e.toString());
		System.exit(-1);
	    } catch (SecurityException e) {
		System.err.println("Internal Error: Request to set " +
				   "configuration option [" + key +
				   "] not supported! Please report to " +
				   "developers!");
		System.err.println(e.toString());
		System.exit(-1);
	    } catch (IllegalArgumentException e) {
		System.err.println("Internal Error: Request to set " +
				   "configuration option [" + key +
				   "] not supported! Please report to " +
				   "developers!");
		System.err.println(e.toString());
		System.exit(-1);
	    } catch (IllegalAccessException e) {
		System.err.println("Internal Error: Request to set " +
				   "configuration option [" + key +
				   "] not supported! Please report to " +
				   "developers!");
		System.err.println(e.toString());
		System.exit(-1);
	    }
	}
    }

    public void close() 
    {
	try {
	    writeFile();
	} catch (LogLoadException e) {
	    System.err.println(e.toString());
	    System.exit(-1);
	}
    }
}

