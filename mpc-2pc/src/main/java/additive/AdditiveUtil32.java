package additive;

import java.math.BigInteger;

import utilMpc.Constants2PC;

public class AdditiveUtil32 {
	
	public static long modulus = Constants2PC.MODULUS_32; //2^32
	/*public static long modAdditive(long ran) {
		return (ran + modulus ) % modulus;
	}*/
	
	
	public static long modAdditive(long ran) {
		BigInteger ranBI = BigInteger.valueOf(ran);
		BigInteger addBI = ranBI.add(BigInteger.valueOf(modulus));
		BigInteger addMod = addBI.mod(BigInteger.valueOf(modulus));
		return addMod.longValue();
	}
	

	public static long sub(long x, long x0) {	
		BigInteger xBI = BigInteger.valueOf(x);
		BigInteger x0BI = BigInteger.valueOf(x0);
		BigInteger subBI = xBI.subtract(x0BI).add(BigInteger.valueOf(modulus));
		BigInteger subMod = subBI.mod(BigInteger.valueOf(modulus));
		return subMod.longValue();		
	}
	
	public static long add(long x0, long x1) {
		//return AdditiveUtilDbl.modAdditive(x0 + x1);
		BigInteger x0BI = BigInteger.valueOf(x0);
		BigInteger x1BI = BigInteger.valueOf(x1);
		BigInteger addBI = x0BI.add(x1BI);
		BigInteger addMod = addBI.mod(BigInteger.valueOf(modulus));
		return addMod.longValue();
	}
	
	public static long mul(long x0, long x1) {
		//return AdditiveUtilDbl.modAdditive(x0 * x1);
		BigInteger x0BI = BigInteger.valueOf(x0);
		BigInteger x1BI = BigInteger.valueOf(x1);
		BigInteger mulBI = x0BI.multiply(x1BI);
		BigInteger mulMod = mulBI.mod(BigInteger.valueOf(modulus));
		return mulMod.longValue();
	}
	
	

}
