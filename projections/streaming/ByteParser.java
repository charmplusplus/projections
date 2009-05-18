package projections.streaming;

public class ByteParser {

	public static int bytesToInt (byte[] arr, int start) {
		int len = 4;	
		int result = 0;	
		for (int i = 0; i < len; i++) {
			result |= ( (long)( arr[i+start] & 0xff ) ) << (i*8);
		}
		return result;
	}

	public static long bytesToLong (byte[] arr, int start) {
		return bytesToLong (arr, start, false);
	}

	public static long bytesToLong (byte[] arr, int start, boolean swapEndians) {
		int len = 8;	
		long result = 0;	
		for (int i = 0; i < len; i++) {
			result |= ( (long)( arr[i+start] & 0xff ) ) << (i*8);
		}

		if(swapEndians){
			// Swap Endians if needed
			long b0 = (result >>  0) & 0xff;
			long b1 = (result >>  8) & 0xff;
			long b2 = (result >> 16) & 0xff;
			long b3 = (result >> 24) & 0xff;
			long b4 = (result >> 32) & 0xff;
			long b5 = (result >> 40) & 0xff;
			long b6 = (result >> 48) & 0xff;
			long b7 = (result >> 56) & 0xff;
			result =  b0 << 56 | b1 << 48 | b2 << 40 | b3 << 32 | b4 << 24 | b5 << 16 | b6 <<  8 | b7 ;
		}

		return result;
	}


	public static float bytesToFloat (byte[] arr, int start) {
		return Float.intBitsToFloat(bytesToInt(arr,start));
	}

	public static double bytesToDouble (byte[] arr, int start) {
		return Double.longBitsToDouble(bytesToLong(arr,start));
	}

	public static int unsignedByteToInt(byte b) {
		return b & 0xFF;
	}

	public static int bytesToUnsignedChar(byte[] arr, int start) {
		return unsignedByteToInt(arr[start]);
	}

	public static short bytesToShort(byte[] arr, int start) {	
		int len = 2;	
		short result = 0;
		for (int i = 0; i < len; i++)
			result |= ( (long)( arr[i+start] & 0xff ) ) << (i*8);
		return result;
	}

}
