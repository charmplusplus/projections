package projections.gui;


/** Store data that is used by the range dialog boxes for all the tools */
public class RangeDialogPersistantData {

	public OrderedIntList plist;
	public long begintime;
	public long endtime;

	public RangeDialogPersistantData(OrderedIntList plist, long begintime, long endtime){
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
