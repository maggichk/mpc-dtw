package gadgets;

import flexSC.circuits.arithmetic.IntegerLib;
import flexSC.flexsc.CompEnv;
import gcMpc.A2Y;
import gcMpc.Y2A;
import flexSC.util.Utils;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SCMPGadget {

	static public <T> T[] compute(IntegerLib<T> arithmeticGate, T[] shareA0, T[] shareA1, T[] shareB0, T[] shareB1,
			T[] random) {
		// IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);

		// A2Y
		T[] A = A2Y.computeInteger(arithmeticGate, shareA0, shareA1);
		T[] B = A2Y.computeInteger(arithmeticGate, shareB0, shareB1);

		// min gate
		T[] min = arithmeticGate.min(A, B);

		T[] res = Y2A.computeInteger(arithmeticGate, random, min);

		return res;
	}

	public static class Generator<T> extends GenRunnable<T> {
		// T[] inputA0;
		// T[] inputA1;
		// T[] inputB0;
		// T[] inputB1;
		T[] scResult;
		T[][] inputAlice; // <A>_0, <B>_0, random
		T[][] inputBob; // <A>_1, <B>_1
		long outputAlice = 0;

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[][] argsArr = new boolean[3][32];
			argsArr[0] = Utils.fromInt(new Integer(args[0]), 32);
			argsArr[1] = Utils.fromInt(new Integer(args[1]), 32);
			argsArr[2] = Utils.fromInt(new Integer(args[2]), 32);

			inputAlice = gen.inputOfAlice(argsArr);
			gen.flush();
			inputBob = gen.inputOfBob(new boolean[2][32]);
		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);
			scResult = compute(arithmeticGate, inputAlice[0], inputBob[0], inputAlice[1], inputBob[1], inputAlice[2]);
		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
			outputAlice = Utils.to31UnSignedInt(gen.outputToAlice(scResult));
			long outputBob = Utils.to31UnSignedInt(gen.outputToBob(scResult));

			//System.out.println("[Generator]: Alice output:" + outputAlice + " out length = " + scResult.length);
			//System.out.println("[Generator]: Bob output:" + outputBob);
		}

		@Override
		public long getOutputAlice() throws Exception {

			return outputAlice;
		}

	}

	public static class Evaluator<T> extends EvaRunnable<T> {

		T[] scResult;
		T[][] inputAlice; // <A>_0, <B>_0
		T[][] inputBob; // <A>_1, <B>_1
		long outputBob = 0;

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			boolean[][] argsArr = new boolean[2][32];
			argsArr[0] = Utils.fromInt(new Integer(args[0]), 32);
			argsArr[1] = Utils.fromInt(new Integer(args[1]), 32);

			inputAlice = gen.inputOfAlice(new boolean[3][32]);
			gen.flush();
			inputBob = gen.inputOfBob(argsArr);

		}

		@Override
		public void secureCompute(CompEnv<T> gen) throws Exception {
			IntegerLib<T> arithmeticGate = new IntegerLib<T>(gen);
			scResult = compute(arithmeticGate, inputAlice[0], inputBob[0], inputAlice[1], inputBob[1], inputAlice[2]);

		}

		@Override
		public void prepareOutput(CompEnv<T> gen) throws Exception {
			// must have the same order of calling methods as generator

			long outputAlice = Utils.to31UnSignedInt(gen.outputToAlice(scResult));
			outputBob = Utils.to31UnSignedInt(gen.outputToBob(scResult));
			//System.out.println("[Evaluator] Bob's output:" + outputBob);
			//System.out.println("[Evaluator] Alice's output:" + outputAlice);
		}

		@Override
		public long getOutputBob() throws Exception {

			return outputBob;
		}

	}

}
