// this class stores the x and y cordinates of the line joining
//a message's creation and execution. 
package projections.gui.Timeline;

public class Line{

	// Pe on which execution of the message occurs
	public int pCurrent;
	public int pCreation;
	public long executiontime;
	public EntryMethodObject obj;
	public long creationtime;

//	Line(int x1,int y1,int x2,int y2,int pCurrent,long executiontime){
//		this.x1 = x1;
//		this.y1 = y1;
//		this.x2 = x2;
//		this.y2 = y2;
//		this.pCurrent = pCurrent;
//		this.executiontime = executiontime;
//		
//	}

	Line(int pCreation,int pCurrent,EntryMethodObject obj,long creationtime,long executiontime){
		this.pCreation = pCreation;
		this.pCurrent=pCurrent;
		this.obj=obj;
		this.creationtime = creationtime;
		this.executiontime = executiontime;
	}
}
