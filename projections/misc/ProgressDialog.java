package projections.misc;

import java.awt.*;
import java.awt.event.*;
/**
 * A simple progress bar-- displays the amount of "work"
 * (double from 0 to 1) completed so far.
 * Shows itself only if the operation takes a while to
 * complete.
 * Orion Sky Lawlor, olawlor@acm.org
 */
public class ProgressDialog extends Frame 
	implements ActionListener {

	private double curWork;//Current amount of work completed.
	private class Bar extends Canvas {
		public void update(Graphics g) {paint(g);}
		public void paint(Graphics g) {
			int wid=getSize().width, ht=getSize().height;
			int swap=(int)(curWork*wid);//Length of progress bar
			g.setColor(Color.darkGray);
			g.fillRect(0,0,swap,ht);
			g.setColor(Color.lightGray);
			g.fillRect(swap,0,wid,ht);
			g.setColor(Color.black);
			g.drawRect(0,0,wid,ht);
		}
		public Dimension getMinimumSize() {return new Dimension(300,16);}
		public Dimension getPreferredSize() {return getMinimumSize();}
	};
	private Bar bar;
	private Label label;
	private String oldLabel;
	private String doingWhat;
	private boolean windowShown;//Have we shown ourselves yet?
	private boolean isCancelled;//Has the user cancelled the operation?
	private boolean isDone;//Has done been executed the operation?

	public ProgressDialog(String what)
	{
		doingWhat=what;
		curWork=0.0;
		isCancelled=false;
		windowShown=false;
		//Show this window if we don't finish in 400 milliseconds
		new Thread(new Runnable() {public void run() {
	  		try {Thread.currentThread().sleep(400);}
	  		catch (Exception E) {}
	  		if (!isDone) showWindow();
	 	}}).start();
	}
public void actionPerformed(ActionEvent evt) {
	//The only item we listen for is the cancel button
	isCancelled=true;
	dispose();
}
public void done() {
	if (windowShown) dispose();
	isDone=true;
}
	/*As above, but second argument is total amount of work.
	 */
	public boolean progress(double current,double total,String newLabel) {
		return progress(current/total,newLabel);
	}
	/*Returns true if work should continue,
	 *false if the user cancelled.
	 */
	public boolean progress(double newWork,String newLabel) {
		if (isCancelled) return false;
		if (newWork>0.001+curWork) {
			curWork=newWork;
			if (windowShown) bar.repaint(40);
		}
		setLabel(newLabel);
		return true;
	}
/**
 * This method was automatically generated in IBM VisualAge
 * by Orion Sky Lawlor, olawlor@acm.org
 * 
 * @param newLabel java.lang.String
 */
public void setLabel(String newLabel) {
	if (newLabel!=oldLabel) {
		oldLabel=newLabel;
		if (label!=null) {
			if (newLabel!=null)
				label.setText(doingWhat+" ("+newLabel+")");
			else label.setText(doingWhat);
		}
	}
}
	/*Shows this dialog.
	 */
	private void showWindow() {
		windowShown=true;
		
		addWindowListener(new WindowAdapter()
		{                    
			public void windowClosing(WindowEvent e)
			{
				isCancelled=true;
				dispose();
			}
		});
		setTitle("Please Wait...");
		
		GridBagLayout      gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout(gbl);
		gbc.fill  = GridBagConstraints.HORIZONTAL;
		
		gbc.gridx = 0; gbc.gridy=0;
		gbc.insets=new Insets(5,10,5,10);
		label=new Label("");
		String newLabel=oldLabel; oldLabel="";setLabel(newLabel);
		add(label,gbc);
		
		gbc.gridx = 0; gbc.gridy=1;
		bar=new Bar();
		add(bar,gbc);
		
		gbc.fill  = GridBagConstraints.NONE;
		gbc.anchor= GridBagConstraints.EAST;
		gbc.gridx = 0; gbc.gridy=2;
		Button cancel=new Button("Cancel");
		add(cancel,gbc);
		cancel.addActionListener(this);
		
		pack();
		setVisible(true);
	}
}