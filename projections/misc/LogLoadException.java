package projections.misc;

public class LogLoadException extends java.io.IOException
{

	public static final int OPEN  = 0;
	public static final int READ  = 1;
	public static final int CLOSE = 2;
	public static final int WRITE = 3;

	int Operation;
	String FileName;

	public LogLoadException (String File, int Op)
	{
		super ("ERROR:  couldn't process file.");
		FileName = File;
		Operation = Op;
	}
}
