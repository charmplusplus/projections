/**
 * A simple implementation of a YAxis where everything is fixed.
 * Orion Sky Lawlor, olawlor@acm.org, 4/2/2002.
 */

package projections.gui.graph;

import projections.gui.U;

public class YAxisFixed extends YAxis
{
	private String title;
	private String units;
	private double max;

	public YAxisFixed(String title_,String units_,double max_) {
		title=title_; units=units_; max=max_;
	}

	public String getTitle() {return title;}
	public String getUnits() {return units;}
	public double getMax() {return max;}

		
	public String getValueName(double value) {
		// Put M or K instead of printing whole thing
		if(value > 1000000.0){
			String m = "" + (value / 1000000.0);
			m = U.truncateTrailingZeroPeriod(m);
			return "" + m + "M";
		} else if(value > 1000.0){
			String k = "" + (value / 1000.0);
			k = U.truncateTrailingZeroPeriod(k);
			return "" + k + "K";
		} else {
			String v = ""+value;
			return U.truncateTrailingZeroPeriod(v);
		}
	}
}


