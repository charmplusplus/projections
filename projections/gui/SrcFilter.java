package projections.gui;

import java.io.*;

public class SrcFilter implements FilenameFilter
{
    public boolean accept(File dir, String name)
    { return (name.endsWith(".sts")); }
}
