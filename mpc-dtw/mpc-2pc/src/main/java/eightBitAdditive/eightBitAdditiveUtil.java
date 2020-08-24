package eightBitAdditive;

import eightBitAdditive.eightBitConstants2PC;

public class eightBitAdditiveUtil {
	
	public static short modulus = eightBitConstants2PC.MODULUS; //2^31
	public static short modAdditive(int ran) {
		//return ((short) ((ran  + 128 ) % 128));
		return ((short) ((ran + modulus) % modulus));//+ modulus
	}
	

	public static short sub(short x, short x0) {
		return eightBitAdditiveUtil.modAdditive(x  - x0 );
		//return eightBitAdditiveUtil.modAdditive(((x & 0xff) - (x0 & 0xff)));
	}
	
	public static short add(short x0, short x1) {
		return eightBitAdditiveUtil.modAdditive(x0  + x1 );
		//return eightBitAdditiveUtil.modAdditive(((x0 & 0xff) + (x1 & 0xff)));
	}
	
	public static short mul(short x0, short x1) {
		return eightBitAdditiveUtil.modAdditive(x0  * x1 );
		//return eightBitAdditiveUtil.modAdditive(((x0 & 0xff) * (x1 & 0xff)));
	}
	
	

}
