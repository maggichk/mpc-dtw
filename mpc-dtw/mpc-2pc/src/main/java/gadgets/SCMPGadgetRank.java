package gadgets;

import flexSC.circuits.arithmetic.IntegerLib;
import flexSC.flexsc.CompEnv;
import gcMpc.A2Y;
import flexSC.util.Utils;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SCMPGadgetRank {
	static public<T> T compute(IntegerLib<T> arithmeticGate, T[] inputA0, T[] inputA1, T[] inputB0,  T[] inputB1) {
		//IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);
		
		//A2Y
		T[] inputA = A2Y.computeInteger(arithmeticGate, inputA0, inputA1);
		T[] inputB = A2Y.computeInteger(arithmeticGate, inputB0, inputB1);
		
		//compare A, B		
		//geqBit = 1 if A >= B; =0, if A < B
		
		T geqBit = arithmeticGate.geq(inputA, inputB);
	
		
		return geqBit;
	}
	
	public static class Generator<T> extends GenRunnable<T>{
		//T[] inputA0;
		//T[] inputA1;
		//T[] inputB0;
		//T[] inputB1;
		T scResult;
		T[][] inputAlice; //<A>_0, <B>_0
		T[][] inputBob; //<A>_1, <B>_1
		long outputAlice = 0;
		

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[][] argsArr = new  boolean[2][32];
			argsArr[0] = Utils.fromInt(new Integer(args[0]),32);//A0
			argsArr[1] = Utils.fromInt(new Integer(args[1]),32);//B0
						
			inputAlice = gen.inputOfAlice(argsArr);
			gen.flush();
			inputBob = gen.inputOfBob(new boolean[2][32]);
		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);
			scResult = compute(arithmeticGate, inputAlice[0], inputBob[0], inputAlice[1], inputBob[1]);			
		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
			//gen.outputToAlice(scResult);
			outputAlice = Utils.toInt(gen.outputToAlice(scResult));	
			/*gen.outputToAlice(scResult);
			System.out.println("[Generator]: Alice output:");*/
			/*long outputBob = Utils.toInt(gen.outputToBob(scResult));	
			System.out.println("[Generator]: Bob output:"+outputBob);*/
		}

		@Override
		public long getOutputAlice() throws Exception {
			
			return outputAlice;
		}
		
	}
	
	
	public static class Evaluator<T> extends EvaRunnable<T>{

		T scResult;
		T[][] inputAlice; //<A>_0, <B>_0
		T[][] inputBob; //<A>_1, <B>_1
		long outputBob = 0;
		
		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[][] argsArr = new  boolean[2][32];
			argsArr[0] = Utils.fromInt(new Integer(args[0]),32);
			argsArr[1] = Utils.fromInt(new Integer(args[1]),32);
						
			inputAlice = gen.inputOfAlice(new boolean[2][32]);
			gen.flush();
			inputBob = gen.inputOfBob(argsArr);
			
		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);
			scResult = compute(arithmeticGate, inputAlice[0], inputBob[0], inputAlice[1], inputBob[1]);		
			
		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
			
			//must have the same order of calling methods as  generator
			long outputAlice = Utils.toInt(gen.outputToAlice(scResult));	
			//gen.outputToAlice(scResult);
			//System.out.println("[Evaluator]: Alice output:"+outputAlice);
			/*outputBob = Utils.toInt(gen.outputToBob(scResult));		
			System.out.println("[Evaluator]: Bob output:"+outputBob);*/
			
			
		}

		@Override
		public long getOutputBob() throws Exception {
			
			return outputBob;
		}
		
	}

}
