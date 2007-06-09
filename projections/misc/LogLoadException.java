package projections.misc;

public class LogLoadException extends java.io.IOException
{

	private static final long serialVersionUID = 1L;

	public static final int OPEN  = 0;
    public static final int READ  = 1;
    public static final int CLOSE = 2;
    public static final int WRITE = 3;

    int Operation;
    String FileName;
    
    public LogLoadException ()
    {
    }
    public LogLoadException (String File)
    {
	super ("ERROR:  couldn't process file.");
	FileName = File;
    }
    public LogLoadException (String File, int Op)
    {
	super ("ERROR:  couldn't process file.");
	FileName = File;
	Operation = Op;
    }
}
