package gcMpc;



import flexSC.circuits.arithmetic.IntegerLib;
import flexSC.circuits.arithmetic.VectorLib;
import flexSC.flexsc.CompEnv;
import flexSC.util.Utils;
import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class A2Y {
    static public<T> T[][] computeVector(CompEnv<T> gen, IntegerLib<T> arithmeticGate, T[][] inputA0, T[][] inputA1){
        VectorLib<T> A2YGate = new VectorLib<T>(gen, arithmeticGate);
        T[] mod = arithmeticGate.publicValue(Constants2PC.MODULUS,32);
        T[][] sum = A2YGate.add(inputA0, inputA1);
        for(int i = 0; i < sum.length; i++) {
            sum[i] = arithmeticGate.mod(sum[i], mod);
        }

        return sum;
    }
    
    static public<T> T[] computeInteger(IntegerLib<T> arithmeticGate, T[] inputA0, T[] inputA1){
        //IntegerLib<T> A2YGate = new IntegerLib<T>(gen);
        T[] mod = arithmeticGate.publicValue(Constants2PC.MODULUS,32);//2^31
        //T[] value1 = arithmeticGate.publicValue(1);
        
        T[] sum = arithmeticGate.add(inputA0, inputA1);//???? left shift or right
        //T[] sum = arithmeticGate.unSignedAdd(inputA0, inputA1);//????
        //sum = arithmeticGate.absolute(sum);
        //sum = arithmeticGate.mod(sum, mod);
        //sum = arithmeticGate.unSignedAdd(sum, mod);
        //sum = arithmeticGate.modUnsigned(sum, mod);
        sum = arithmeticGate.modUnsigned(sum, mod);

        return sum;
    }
    
    
    public static class Generator<T> extends GenRunnable<T> {

		T[] scResult;
		T[] inputAlice; // <A>_0
		T[] inputBob; // <A>_1
		long outputAlice = 0;

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[] argsArr = new boolean[32];
			argsArr = Utils.fromInt(new Integer(args[0]), 32);// a
			
			
			inputAlice = gen.inputOfAlice(argsArr);
			gen.flush();
			inputBob = gen.inputOfBob(new boolean[32]);
		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen,32);
			
			scResult = computeInteger(arithmeticGate, inputAlice, inputBob);

		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
			boolean[] aliceOut = gen.outputToAlice(scResult);
			//System.out.println("len:"+aliceOut.length);
			for(int i=0; i<aliceOut.length; i++) {
				if(aliceOut[i] == true) {
					System.out.print(1);
				}else {
					System.out.print(0);
				}
			}
			outputAlice = Utils.to31UnSignedInt(aliceOut);// read from where?
			//System.out.println("");
			
			//int outputAliceInt = Utils.toInt(aliceOut);
			//System.out.println("[Generator] Alice's ouput:"+outputAlice+" "+outputAliceInt);
			//System.out.println("[Generator]: Alice output:"+Utils.toInt(gen.outputToAlice(scResult)));			
			gen.outputToBob(scResult);
			
			/*int test = -1;
			System.out.println("test "+Integer.toBinaryString(test));*/
			

		}

		@Override
		public long getOutputAlice() throws Exception {
			return outputAlice;
		}

	}

	public static class Evaluator<T> extends EvaRunnable<T> {

		T[] scResult;
		T[] inputAlice; // <A>_0, <B>_0, <U>_0, <V>_0, ran
		T[] inputBob; // <A>_1, <B>_1, <U>_1, <V>_1
		long outputBob = 0;

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[] argsArr = new boolean[32];
			argsArr = Utils.fromInt(new Integer(args[0]), 32);// <A>_1
			inputAlice = gen.inputOfAlice(new boolean[32]);
			gen.flush();
			inputBob = gen.inputOfBob(argsArr);
			
			

		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen, 32);
			scResult = computeInteger(arithmeticGate, inputAlice,inputBob);

		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
			//must have the same order of calling methods as  generator
			gen.outputToAlice(scResult);
			outputBob = Utils.to31UnSignedInt(gen.outputToBob(scResult));
			
			
			
			System.out.println("[Evaluator] Bob's output:"+outputBob);

		}

		@Override
		public long getOutputBob() throws Exception {
			return outputBob;
			
		}

	}
    
}
