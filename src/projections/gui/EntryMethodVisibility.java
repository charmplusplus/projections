package projections.gui;

public interface EntryMethodVisibility {
	public void makeEntryVisibleID(Integer id);
	public void makeEntryInvisibleID(Integer id);
	public void displayMustBeRedrawn();
	public boolean entryIsVisibleID(Integer id);
	public int[] getEntriesArray();
	public boolean hasEntryList();
	public boolean handleIdleOverhead();

	/** Whether the numbers in getEntriesArray() are counts worth reading,
	 *  rather than a bare "this one is present".
	 *
	 *  A tool that says yes gets its chooser ordered by those counts, largest
	 *  first, with the counts in a column of their own and the negligible ones
	 *  greyed at the bottom -- on a large trace most entry methods in range
	 *  are a handful of events against millions, and sorting by entry method
	 *  id buries the ones that matter among them. Everything else keeps the
	 *  list it always had. */
	default boolean sortEntriesByCount() {
		return false;
	}
}