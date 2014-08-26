package projections.gui;
import java.text.NumberFormat;

  /** To enable the correct format for both int and scientific. */
  public class FormattedNumber extends Number
  implements Comparable<FormattedNumber> {

	  private double       number;
    private NumberFormat format;
    public FormattedNumber(double d, NumberFormat f) {
      number = d;
      format = f;
    }
    public byte byteValue() { return (byte) number; }
    public double doubleValue() { return number; }
    public float floatValue() { return (float) number; }
    public int intValue() { return (int) number; }
    public long longValue() { return (long) number; }
    public short shortValue() { return (short) number; }
    public String toString() { return format.format(number); }
    public int compareTo(FormattedNumber otherNum)
    {
	if (this.number < otherNum.number) return -1;
	else if (this.number == otherNum.number) return 0;
	else return 1;
    }
  }
