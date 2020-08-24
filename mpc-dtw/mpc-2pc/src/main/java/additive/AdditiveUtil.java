package additive;

import utilMpc.Constants2PC;

public class AdditiveUtil {
	
	public static long modulus = Constants2PC.MODULUS; //2^31
	public static long modAdditive(long ran) {
		return (ran + modulus ) % modulus;
	}
	

	public static long sub(long x, long x0) {
		return AdditiveUtil.modAdditive(x - x0);
	}
	
	public static long add(long x0, long x1) {
		return AdditiveUtil.modAdditive(x0 + x1);
	}
	
	public static long mul(long x0, long x1) {
		return AdditiveUtil.modAdditive(x0 * x1);
	}
	
	

}
