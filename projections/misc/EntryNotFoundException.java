package projections.misc;

public class EntryNotFoundException extends java.io.IOException
{

public EntryNotFoundException ()
{
	super ("no entries by that name on this processor");
}

}

