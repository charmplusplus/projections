package projections.gui;
import java.text.NumberFormat;

  /** To enable the correct format for both int and scientific. */
  public class FormattedNumber extends Number {

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
  }
