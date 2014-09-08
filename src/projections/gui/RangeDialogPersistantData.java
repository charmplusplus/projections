package projections.gui;


import java.util.SortedSet;

/** Store data that is used by the range dialog boxes for all the tools */
public class RangeDialogPersistantData { // NO_UCD

	protected SortedSet<Integer> plist;
	protected long begintime;
	protected long endtime;

	public RangeDialogPersistantData(SortedSet<Integer> plist, long begintime, long endtime){
		this.plist = plist;
		this.begintime = begintime;
		this.endtime = endtime;
			}

	public void update(long begintime, long endtime, SortedSet<Integer> plist) {
		this.plist = plist;
		this.begintime = begintime;
		this.endtime = endtime;	
	}
	
}
