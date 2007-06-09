package projections.misc;

public class EntryNotFoundException extends java.io.IOException
{

private static final long serialVersionUID = 1L;

public EntryNotFoundException ()
{
	super ("no entries by that name on this processor");
}
}