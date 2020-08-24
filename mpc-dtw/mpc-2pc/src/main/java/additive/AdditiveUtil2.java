package additive;

import utilMpc.Constants2PC;

public class AdditiveUtil2 {
	
	public static long modulus = 2; //2
	public static long modAdditive(long ran) {
		return (ran + modulus ) % modulus;
	}
	

	public static long sub(long x, long x0) {
		return AdditiveUtil2.modAdditive(x - x0);
	}
	
	public static long add(long x0, long x1) {
		return AdditiveUtil2.modAdditive(x0 + x1);
	}
	
	public static long mul(long x0, long x1) {
		return AdditiveUtil2.modAdditive(x0 * x1);
	}
	
	

}
