package projections.analysis;

import java.io.*;
import java.util.*;
import java.lang.*;
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
    private String logDirectory;
    private String configurationName;

    // Variables recognized and stored in a hash table when a 
    // configuration file is read.
    private Hashtable configData;

    private boolean dirty;

    // Configuration Variables. They *must* begin with "RC_"
    public static Long RC_GLOBAL_END_TIME = new Long(-1);
    public static Long RC_POSE_REAL_TIME = new Long(-1);
    public static Long RC_POSE_VIRT_TIME = new Long(-1);    
    public static Boolean RC_IS_OUTLIER_FILTERED = Boolean.valueOf(false);
    public static String RC_VALID_LOG_LIST = "";

    public ProjectionsConfigurationReader(String filename)
    {
	baseName = FileUtils.getBaseName(filename);
	logDirectory = FileUtils.dirFromFile(filename);
	configData = new Hashtable();
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
	    String Line,Type,Name;
	    while ((Line = InFile.readLine()) != null) {
		StringTokenizer st = new StringTokenizer(Line);
		String s1 = st.nextToken();
		String tempStr = "";
		// All rc descriptors must start with this string
		if (!s1.startsWith("RC_")) {
		    System.err.println("Warning: Key [" + s1 + "] does not " +
				       "start with RC_ and is rejected.");
		    continue;
		}
		try {
		    Field rcField =
			Class.forName("projections.analysis.ProjectionsConfigurationReader").getField(s1);
		    // The configuration variables must either support the
		    // valueOf(String) method or be String-compatible.
		    // Failure to support valueOf is caught by the exception
		    // NoSuchMethodException and is an internal error (i.e.
		    // a member of the development team used an incompatible
		    // type)
		    tempStr = st.nextToken();
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
		    System.out.println(rcField.get(null));
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

    public void add(String key, Object value)
    {
	configData.put(key, value);
	dirty = true;
    }

    // *********************
    // * Public interface to the configuration reader
    // * whichever tool of projections uses this is expected
    // * to know the data type returned and cast it accordingly.
    // * otherwise, there is an internal error and the system
    // * should be shut down.
    // *********************
    public Object getValue(String key) {
	return configData.get(key);
    }

    public void close() 
    {
	try {
	    flush();
	} catch (LogLoadException e) {
	    System.err.println(e.toString());
	    System.exit(-1);
	}
    }

    private void writeValue(String key, Object value, PrintWriter writer) {
	if (key.equals("REAL_TOTAL_TIME")) {
	    writer.print(((Long)value).longValue());
	} else if (key.equals("POSE_REAL_TOTAL_TIME")) {
	    writer.print(((Double)value).doubleValue());
	} else if (key.equals("POSE_VIRT_TOTAL_TIME")) {
	    writer.print(((Long)value).longValue());
	} else {
	    System.err.println("Internal Error: Unknown key [" +
			       key + "]! Please report to developers!");
	    System.exit(-1);
	}
    }

    private void flush() 
	throws LogLoadException
    {
	if (dirty) {
	    try {
		PrintWriter writer =
		    new PrintWriter(new FileWriter(configurationName));
		Iterator dataIterator = 
		    configData.entrySet().iterator();
		Map.Entry entry;
		while (dataIterator.hasNext()) {
		    entry = (Map.Entry)dataIterator.next();
		    writer.print((String)(entry.getKey()) + " ");
		    writeValue((String)entry.getKey(), entry.getValue(),
			       writer);
		    writer.println();
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
}

