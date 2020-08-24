package gcMpc;

import flexSC.circuits.arithmetic.IntegerLib;
import flexSC.circuits.arithmetic.VectorLib;
import flexSC.flexsc.CompEnv;
import utilMpc.Constants2PC;

public class Y2A {
	
	
	public static <T> T[][] computeVector(CompEnv<T> gen, IntegerLib<T> arithmeticGate, T[][] inputRandom, T[][] input){
		VectorLib<T> Y2AGate = new VectorLib<T>(gen, arithmeticGate);
		T[] mod = arithmeticGate.publicValue(Constants2PC.MODULUS,32);
		T[][] sub = Y2AGate.sub(input, inputRandom);
		for(int i =0; i<sub.length; i++) {
			sub[i] = arithmeticGate.mod(sub[i], mod);
		}
		
		return sub;
	}
	
	public static <T> T[] computeInteger(IntegerLib<T> arithmeticGate, T[] inputRandom, T[] input) {
		//IntegerLib<T> Y2AGate = new IntegerLib<T>(gen);
		T[] mod = arithmeticGate.publicValue(Constants2PC.MODULUS,32);
		//T[] max = arithmeticGate.publicValue(Constants2PC.MAX_PLUS_1);//2^31-1
		
		//sub
		T[] sub = arithmeticGate.sub(input, inputRandom);		
				
		//mod
		//T[] value1 = arithmeticGate.publicValue(1);		
		//sub = arithmeticGate.and(sub, mod);
		//sub = arithmeticGate.add(sub, value1);
		
		//sub = arithmeticGate.add(sub, mod);
		//sub = arithmeticGate.mod(sub, mod);
		sub = arithmeticGate.modUnsigned(sub, mod);
		
		return sub;
	}
}
