package projections.gui;


/** Store data that is used by the range dialog boxes for all the tools */
public class RangeDialogPersistantData {

	protected OrderedIntList plist;
	protected long begintime;
	protected long endtime;

	protected RangeDialogPersistantData(OrderedIntList plist, long begintime, long endtime){
		this.plist = plist;
		this.begintime = begintime;
		this.endtime = endtime;
			}

	public void update(long begintime, long endtime, OrderedIntList plist) {
		this.plist = plist;
		this.begintime = begintime;
		this.endtime = endtime;	
	}
	
}
