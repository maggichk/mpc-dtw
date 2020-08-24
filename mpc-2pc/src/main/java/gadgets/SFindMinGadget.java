package gadgets;

import flexSC.circuits.arithmetic.IntegerLib;
import flexSC.flexsc.CompEnv;
import gcMpc.A2Y;
import gcMpc.Y2A;
import flexSC.util.Utils;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SFindMinGadget {
	
	
	static public<T> T[] compute(IntegerLib<T> arithmeticGate, T[] ran, T[] shareA0, T[] shareA1, T[] shareB0, T[] shareB1, T[] shareC0,  T[] shareC1) {
		//A2Y
		T[] A = A2Y.computeInteger(arithmeticGate, shareA0, shareA1);
		T[] B = A2Y.computeInteger(arithmeticGate, shareB0, shareB1);
		T[] C = A2Y.computeInteger(arithmeticGate, shareC0, shareC1);
		
		T[] min;
		//minGate(A,B)		
		min = arithmeticGate.min(A, B);
		//minGate(min, C)
		min = arithmeticGate.min(min, C);
		min = Y2A.computeInteger(arithmeticGate, ran, min);
		return min;
	}
	
	public static class Generator<T> extends GenRunnable<T>{
		//T[] inputA0;
		//T[] inputA1;
		//T[] inputB0;
		//T[] inputB1;
		T[] scResult;
		T[][] inputAlice; //<A>_0, <B>_0, <C>_0, r
		T[][] inputBob; //<A>_1, <B>_1, <C>_1
		long outputAlice = 0L;
		
		

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[][] argsArr = new  boolean[4][32];
			//<A>_0, <B>_0, <C>_0, r
			argsArr[0] = Utils.fromInt(new Integer(args[0]),32);
			argsArr[1] = Utils.fromInt(new Integer(args[1]),32);
			argsArr[2] = Utils.fromInt(new Integer(args[2]),32);
			argsArr[3] = Utils.fromInt(new Integer(args[3]),32);
						
			inputAlice = gen.inputOfAlice(argsArr);
			gen.flush();
			inputBob = gen.inputOfBob(new boolean[3][32]);
		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);
			scResult = compute(arithmeticGate, inputAlice[3], inputAlice[0], inputBob[0], inputAlice[1], inputBob[1], inputAlice[2], inputBob[2]);			
		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
					
			outputAlice = Utils.to31UnSignedInt(gen.outputToAlice(scResult));	
			long outputBob = Utils.to31UnSignedInt(gen.outputToBob(scResult));
			//System.out.println("[Generator]: Alice output:"+outputAlice+" out length = " + scResult.length);
			//System.out.println("[Generator]: Bob output:"+outputBob);
		}

		@Override
		public long getOutputAlice() throws Exception {
			
			return outputAlice;
		}
		
	}
	
	
	public static class Evaluator<T> extends EvaRunnable<T>{

		T[] scResult;
		T[][] inputAlice; //<A>_0, <B>_0, <C>_0
		T[][] inputBob; //<A>_1, <B>_1, <C>_1
		long outputBob=0L;
		
		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[][] argsArr = new  boolean[3][32];
			//<A>_0, <B>_0, <C>_0
			argsArr[0] = Utils.fromInt(new Integer(args[0]),32);
			argsArr[1] = Utils.fromInt(new Integer(args[1]),32);
			argsArr[2] = Utils.fromInt(new Integer(args[2]),32);
						
			inputAlice = gen.inputOfAlice(new boolean[4][32]);
			gen.flush();
			inputBob = gen.inputOfBob(argsArr);
			
		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);
			scResult = compute(arithmeticGate, inputAlice[3], inputAlice[0], inputBob[0], inputAlice[1], inputBob[1], inputAlice[2], inputBob[2]);		
			
		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
			//must have the same order of calling methods as  generator
			long outputAlice = Utils.to31UnSignedInt(gen.outputToAlice(scResult));
			outputBob = Utils.to31UnSignedInt(gen.outputToBob(scResult));
			
			//System.out.println("[Evaluator] Bob's output:"+outputBob);
			//System.out.println("[Evaluator] Alice's output:"+outputAlice);
		}

		@Override
		public long getOutputBob() throws Exception {
			
			return outputBob;
		}
		
	}

}
