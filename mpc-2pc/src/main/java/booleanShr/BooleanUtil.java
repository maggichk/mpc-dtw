package booleanShr;

import utilMpc.Constants2PC;

public class BooleanUtil {
	
	public static byte modulus = 2; //2
	
	

	
	
	public static byte xor(byte x0, byte x1) {
		return (byte) (x0 ^ x1);
	}
	
	public static byte and(byte x0, byte x1) {
		return (byte) (x0 & x1);
	}
	
	

}
