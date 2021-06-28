package projections.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import projections.misc.FileUtils;
import projections.misc.LogLoadException;

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
  private String projrcFileName;
  
  private boolean dirty;
  FileUtils fileNameHandler;
  // Configuration Variables. They *must* begin with "RC_"
  // For convenience of coding, these are static. This will have to
  // be changed once multiple runs are supported generically in
  // Projections.
  public Long RC_GLOBAL_END_TIME = (long) -1;
  public Long RC_POSE_REAL_TIME = (long) -1;
  public Long RC_POSE_VIRT_TIME = (long) -1;
  public Boolean RC_OUTLIER_FILTERED = Boolean.FALSE;
  
  public ProjectionsConfigurationReader(FileUtils fileNameHandler)
  {
	  this.fileNameHandler = fileNameHandler;
	  projrcFileName = fileNameHandler.getProjRCName();
	  dirty = false;
    try {
      readfile();
    } catch (LogLoadException e) {
      System.err.println(e.toString());
      System.exit(-1);
    }
  }

  private void readfile() 
  throws LogLoadException
  {
	  try {
		  BufferedReader InFile = new BufferedReader(new InputStreamReader(new FileInputStream(projrcFileName)));
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
	  } catch (IOException e) {
		  throw new LogLoadException (projrcFileName);
	  }
  }
  
  public void writeFile() 
  throws LogLoadException
  {

	  // Generate a string that will be written into the file
	  String filedata = "";
	  try {
		  Field rcFields[] = this.getClass().getFields();
		  for (int field=0; field<rcFields.length; field++) {
			  String fieldname = rcFields[field].getName();
			  if (fieldname.startsWith("RC_")) {
				  filedata += "" + fieldname + " " + rcFields[field].get(this).toString() + "\n";
			  }
		  }
	  } catch (IllegalArgumentException e) {
		  System.err.println("Internal Error: Cannot write configuration (.projrc) file. Please report to developers!");
		  System.err.println(e.toString());
		  System.exit(-1);
	  } catch (IllegalAccessException e) {
		  System.err.println("Internal Error: Cannot write configuration (.projrc) file. Please report to developers!");
		  System.err.println(e.toString());
		  System.exit(-1);
	  }

	  
	  // Write the string into the file 
	  if (dirty && filedata.length()>0) {  
		  try {
			  PrintWriter writer =  new PrintWriter(new FileWriter(projrcFileName));
			  try {
				  writer.print(filedata);
			  } catch (Exception e) {
				  System.err.println("Internal Error: Cannot write configuration (.projrc) file. Please report to developers!");
				  System.err.println(e.toString());
				  System.exit(-1);
			  }
			  writer.close();
		  } catch (FileNotFoundException e) {
			  throw new LogLoadException (projrcFileName);
		  } catch (IOException e) {
			  throw new LogLoadException (projrcFileName);
		  }
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

