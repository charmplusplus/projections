// this class stores the x and y cordinates of the line joining
//a message's creation and execution. 
package projections.gui;

public class TimelineLine{
	//cordinates of message creation
	public int x1,y1;
	//cordinates of message deletion
	public int x2,y2;
	// Pe on which execution of the message occurs
	public int pCurrent;
	public int pCreation;
	public long executiontime;
	public TimelineObject obj;
	public long creationtime;

	TimelineLine(int x1,int y1,int x2,int y2,int pCurrent,long executiontime){
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.pCurrent = pCurrent;
		this.executiontime = executiontime;
		
	};

	TimelineLine(int pCreation,int pCurrent,TimelineObject obj,long creationtime,long executiontime){
		this.pCreation = pCreation;
		this.pCurrent=pCurrent;
		this.obj=obj;
		this.creationtime = creationtime;
		this.executiontime = executiontime;
	}
};
