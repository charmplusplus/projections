package projections.streaming;



/** This class should probably not exist. There is functionality in DataInputStream.java that is very similar to this */
public class ByteParser {

	private final static boolean swapEndians = false;

	public static int bytesToInt (byte[] arr, int start) {
		int len = 4;	
		int result = 0;	
		for (int i = 0; i < len; i++) {
			if(swapEndians)
				result |= ( (long)( arr[i+start] & 0xff ) ) << ((len-i-1)*8);
			else
				result |= ( (long)( arr[i+start] & 0xff ) ) << (i*8);
		}
		return result;
	}

	public static long bytesToLong (byte[] arr, int start) {
		int len = 8;	
		long result = 0;	
		for (int i = 0; i < len; i++) {
			if(swapEndians)
				result |= ( (long)( arr[i+start] & 0xff ) ) << ((len-i-1)*8);
			else
				result |= ( (long)( arr[i+start] & 0xff ) ) << (i*8);
		}
		return result;
	}


	// These shouldn't be used because we can't swap the endianness yet
	//	public static float bytesToFloat (byte[] arr, int start) {
	//		return Float.intBitsToFloat(bytesToInt(arr,start));
	//	}
	//
	//	public static double bytesToDouble (byte[] arr, int start) {
	//		return Double.longBitsToDouble(bytesToLong(arr,start));
	//	}

	public static int unsignedByteToInt(byte b) {
		return b & 0xFF;
	}

	public static int bytesToUnsignedChar(byte[] arr, int start) {
		return unsignedByteToInt(arr[start]);
	}

	public static short bytesToShort(byte[] arr, int start) {	
		int len = 2;	
		short result = 0;
		for (int i = 0; i < len; i++){
			if(swapEndians)
				result |= ( (long)( arr[i+start] & 0xff ) ) << ((len-i-1)*8);
			else
				result |= ( (long)( arr[i+start] & 0xff ) ) << (i*8);
		}
		return result;
	}

}
