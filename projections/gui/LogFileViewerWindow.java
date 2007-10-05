package projections.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import projections.misc.*;
import projections.analysis.*;

public class LogFileViewerWindow extends ProjectionsWindow
   implements ActionListener
{
    // LogFileViewerWindow is another tool that uses its own dialog.
    private LogFileViewerDialog dialog;
    private int logfilenum = -1;
    private int oldlogfilenum = -1;
    
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    static int myRun = 0;

    private LogFileViewerTextArea textArea;
    private Label lTitle;
    private Button bOpen, bClose;
    private Panel titlePanel;
    
    void windowInit() {
	// do nothing, no parameters need to be set.
    }
    
    public LogFileViewerWindow(MainWindow parentWindow, Integer myWindowID)
    {
	super(parentWindow, myWindowID);
	
	setBackground(Color.lightGray);
	setTitle("Projections Log File Viewer - " + 
		 MainWindow.runObject[myRun].getFilename() + ".sts");
	  
	CreateMenus();
	CreateLayout();
	
	pack();
	showDialog();
	setVisible(true);
    }   
    
    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource() instanceof MenuItem) {
	    MenuItem m = (MenuItem)evt.getSource();
	    
	    if (m.getLabel().equals("Open File")) {
		showDialog();
	    } else if(m.getLabel().equals("Close")) {
		close();
	    }
	} else if(evt.getSource() instanceof Button) {
	    Button b = (Button)evt.getSource();
	    
	    if(b == bOpen)
		showDialog();
	    else if(b == bClose)
		close();
	}
    }   
    
    public void CloseDialog()
    {
	if(dialog != null)
	    {
		dialog.dispose();
		dialog = null;
	    }
	
	setCursor(new Cursor(Cursor.WAIT_CURSOR));
	if(logfilenum != oldlogfilenum)
	    textArea.setText(getLogFileText(logfilenum));
	  setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
   
    private void CreateLayout()
    {
	  Panel p = new Panel();
	  p.setBackground(Color.lightGray);
	  
	  GridBagLayout gbl = new GridBagLayout();
	  GridBagConstraints gbc = new GridBagConstraints();
	  gbc.fill = GridBagConstraints.BOTH;
	  
	  getContentPane().add(p);
	  
	  textArea = new LogFileViewerTextArea();
	  
	  p.setLayout(gbl);
	  
	  titlePanel = new Panel();
	  titlePanel.setBackground(Color.black);
	  lTitle = new Label("", Label.CENTER);
	  lTitle.setForeground(Color.white);
	  lTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
	  titlePanel.add(lTitle);
	  
	  Util.gblAdd(p, titlePanel, gbc, 0,0, 1,1, 1,0, 5,5,0,5);
	  
	  Util.gblAdd(p, textArea, gbc, 0,1, 1,1, 1,1, 0,5,5,5);  
	  
	  Panel p2 = new Panel();
	  bOpen = new Button("Open File");
	  bClose = new Button("Close Window");
	  bOpen.addActionListener(this);
	  bClose.addActionListener(this);
	  
	  p2.add(bOpen);
	  p2.add(bClose);
	  Util.gblAdd(p, p2, gbc, 0,2, 1,1, 1,0, 0,5,5,5);
   }   
   private void CreateMenus()
   {
	  MenuBar mbar = new MenuBar();
	  
	  mbar.add(Util.makeMenu("File", new Object[]
	  {
		 "Open File",
		 null,
		 "Close"
	  },
	  this));
	   
	  Menu helpMenu = new Menu("Help");
	  mbar.add(Util.makeMenu(helpMenu, new Object[]      {                          
		 "Index",                
		 "About"
	  },                         
	  this));                    
								 
	  mbar.setHelpMenu(helpMenu);
	  setMenuBar(mbar);             
   }   

   public void setLogFileNum(int p)
   {
       logfilenum = p;
       lTitle.setText("LOG FILE FOR PROCESSOR " + p);
       lTitle.invalidate();
       titlePanel.validate();
   }   
    
   public void showDialog()
   {
       oldlogfilenum = logfilenum;
       if (dialog == null) {
	   dialog = new LogFileViewerDialog(this);
       }
       dialog.setVisible(true);
   }   

    /**************** Utility/Access *************/
    public static String[][] getLogFileText( int num ) {
	if (!(MainWindow.runObject[myRun].hasLogData())) {
	    return null;
	} else {
	    Vector v = null;
	    try {
		v = MainWindow.runObject[myRun].logLoader.view(num);
	    } catch (LogLoadException e) {
		System.err.println("Failed to load Log files");
		return null;
	    }
	    if( v == null ) {
		return null;
	    }
	    int length = v.size();
	    if( length == 0 ) {
		return null;
	    }
	    String[][] text = new String[ length ][ 2 ];
	    ViewerEvent ve;
	    for( int i = 0;i < length;i++ ) {
		ve = (ViewerEvent)v.elementAt(i);
		text[ i ][ 0 ] = "" + ve.Time;
		switch( ve.EventType ) {
		case ( ProjDefs.CREATION ):
		    text[ i ][ 1 ] = "CREATE message to be sent to " + ve.Dest;
		    break;
		case ( ProjDefs.CREATION_BCAST ):
		    if (ve.numDestPEs == 
			MainWindow.runObject[myRun].getNumProcessors()) {
			text[ i ][ 1 ] = "GROUP BROADCAST (" + ve.numDestPEs +
			    " processors)";
		    } else {
			text[ i ][ 1 ] = "NODEGROUP BROADCAST (" + 
			    ve.numDestPEs +
			    " processors)";
		    }
		    break;
		case ( ProjDefs.CREATION_MULTICAST ):
		    text[ i ][ 1 ] = "MULTICAST message sent to " + 
			ve.numDestPEs + " processors";
		    break;
		case ( ProjDefs.BEGIN_PROCESSING ):
		    text[ i ][ 1 ] = "BEGIN PROCESSING of message sent to " + 
			ve.Dest;
		    text[ i ][ 1 ] += " from processor " + ve.SrcPe;
		    break;
		case ( ProjDefs.END_PROCESSING ):
		    text[ i ][ 1 ] = "END PROCESSING of message sent to " + 
			ve.Dest;
		    text[ i ][ 1 ] += " from processor " + ve.SrcPe;
		    break;
		case ( ProjDefs.ENQUEUE ):
		    text[ i ][ 1 ] = "ENQUEUEING message received from " +
			"processor " + ve.SrcPe + " destined for " + ve.Dest;
		    break;
		case ( ProjDefs.BEGIN_IDLE ):
		    text[ i ][ 1 ] = "IDLE begin";
		    break;
		case ( ProjDefs.END_IDLE ):
		    text[ i ][ 1 ] = "IDLE end";
		    break;
		case ( ProjDefs.BEGIN_PACK ):
		    text[ i ][ 1 ] = "BEGIN PACKING a message to be sent";
		    break;
		case ( ProjDefs.END_PACK ):
		    text[ i ][ 1 ] = "FINISHED PACKING a message to be sent";
		    break;
		case ( ProjDefs.BEGIN_UNPACK ):
		    text[ i ][ 1 ] = "BEGIN UNPACKING a received message";
		    break;
		case ( ProjDefs.END_UNPACK ):
		    text[ i ][ 1 ] = "FINISHED UNPACKING a received message";
		    break;
		default:
		    text[ i ][ 1 ] = "!!!! ADD EVENT TYPE " + ve.EventType +
			" !!!";
		    break;
		}
	    }
	    return text;
	}
    }

    public void showWindow() {
	// do nothing for now.
    }

    public void getDialogData() {
	// do nothing. This tool uses its own dialog.
    }
}
