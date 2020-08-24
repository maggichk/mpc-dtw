package gadgets;

import flexSC.circuits.arithmetic.IntegerLib;
import flexSC.flexsc.CompEnv;
import gcMpc.A2Y;
import gcMpc.Y2A;
import flexSC.util.Utils;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SBranchGadget {

	/**
	 * SBranch(m1, m2, c1, c2, w)
	 * if m1>=m2, select c2; if m1<m2, select c1
	 * if A>=B, SELECT V; IF A<B, SELECT U
	 * 
	 * @param arithmeticGate
	 * @param ran
	 * @param shareA0
	 * @param shareA1
	 * @param shareB0
	 * @param shareB1
	 * @param shareU0
	 * @param shareU1
	 * @param shareV0
	 * @param shareV1
	 * @return
	 */
	static public <T> T[] compute(IntegerLib<T> arithmeticGate, T[] ran, T[] shareA0, T[] shareA1, T[] shareB0,
			T[] shareB1, T[] shareU0, T[] shareU1, T[] shareV0, T[] shareV1) {
		T[] res;

		// A2Y
		T[] A = A2Y.computeInteger(arithmeticGate, shareA0, shareA1);
		T[] B = A2Y.computeInteger(arithmeticGate, shareB0, shareB1);
		T[] U = A2Y.computeInteger(arithmeticGate, shareU0, shareU1);
		T[] V = A2Y.computeInteger(arithmeticGate, shareV0, shareV1);
		

		// compare
		// geq gate, z=1 if A>=B, select V; 
		// z=0 if A<B, select U
		T geqBit = arithmeticGate.geq(A, B);

		// MUX
		// select U when z == 0; select V when z == 1.
		res = arithmeticGate.mux(U, V, geqBit);

		// Y2A
		res = Y2A.computeInteger(arithmeticGate, ran, res);
		//System.out.println("res:"+res);

		return res;
	}

	public static class Generator<T> extends GenRunnable<T> {

		T[] scResult;
		T[][] inputAlice; // <A>_0, <B>_0, <U>_0, <V>_0, ran
		T[][] inputBob; // <A>_1, <B>_1, <U>_1, <V>_1
		long outputAlice = 0;

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[][] argsArr = new boolean[5][32];
			argsArr[0] = Utils.fromInt(new Integer(args[0]), 32);// a
			argsArr[1] = Utils.fromInt(new Integer(args[1]), 32);// b
			argsArr[2] = Utils.fromInt(new Integer(args[2]), 32);// u
			argsArr[3] = Utils.fromInt(new Integer(args[3]), 32);// v
			argsArr[4] = Utils.fromInt(new Integer(args[4]), 32);// ran
			
			//System.out.println("a0:"+args[0]+" b0:"+args[1]+" u0:"+args[2]+" v0:"+args[3]+" r:"+args[4]);
			
			inputAlice = gen.inputOfAlice(argsArr);
			gen.flush();
			inputBob = gen.inputOfBob(new boolean[4][32]);
		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);
			
			scResult = compute(arithmeticGate, inputAlice[4], inputAlice[0], inputBob[0], inputAlice[1], inputBob[1],
					inputAlice[2], inputBob[2], inputAlice[3], inputBob[3]);

		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
			outputAlice = Utils.to31UnSignedInt(gen.outputToAlice(scResult));
			//System.out.println("[Generator] Alice's ouput:"+outputAlice);
			//System.out.println("[Generator]: Alice output:"+Utils.toInt(gen.outputToAlice(scResult)));			
			Utils.to31UnSignedInt(gen.outputToBob(scResult));
			
			
			

		}

		@Override
		public long getOutputAlice() throws Exception {
			return outputAlice;
		}

	}

	public static class Evaluator<T> extends EvaRunnable<T> {

		T[] scResult;
		T[][] inputAlice; // <A>_0, <B>_0, <U>_0, <V>_0, ran
		T[][] inputBob; // <A>_1, <B>_1, <U>_1, <V>_1
		long outputBob = 0;

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[][] argsArr = new boolean[4][32];
			argsArr[0] = Utils.fromInt(new Integer(args[0]), 32);// <A>_1
			argsArr[1] = Utils.fromInt(new Integer(args[1]), 32);// <B>_1
			argsArr[2] = Utils.fromInt(new Integer(args[2]), 32);// <U>_1
			argsArr[3] = Utils.fromInt(new Integer(args[3]), 32);// <V>_1

			inputAlice = gen.inputOfAlice(new boolean[5][32]);
			gen.flush();
			inputBob = gen.inputOfBob(argsArr);

		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);
			scResult = compute(arithmeticGate, inputAlice[4], inputAlice[0], inputBob[0], inputAlice[1], inputBob[1],
					inputAlice[2], inputBob[2], inputAlice[3], inputBob[3]);

		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
			//must have the same order of calling methods as  generator
			Utils.to31UnSignedInt(gen.outputToAlice(scResult));
			outputBob = Utils.to31UnSignedInt(gen.outputToBob(scResult));
			
			
			
			//System.out.println("[Evaluator] Bob's output:"+outputBob);

		}

		@Override
		public long getOutputBob() throws Exception {
			return outputBob;
			
		}

	}

}
