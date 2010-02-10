package projections.gui;

public class SanitizeForHTML {

	/** Convert characters from a user string into their appropriate HTML strings */
	public static String sanitize(String s) {
		String r = s;
		
		r = r.replace("&", "&amp;");
		r = r.replace("<", "&lt;");
		r = r.replace(">", "&gt;");
		r = r.replace("_", "&#95;");
		
		return r;
	}
	
}
