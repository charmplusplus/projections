package projections.gui;

public interface EntryMethodVisibility {
	public void makeEntryVisibleID(Integer id);
	public void makeEntryInvisibleID(Integer id);
	public void displayMustBeRedrawn();
	public boolean entryIsVisibleID(Integer id);
	public int[] getEntriesArray();
	public boolean hasEntryList();
	public boolean handleIdleOverhead();
}