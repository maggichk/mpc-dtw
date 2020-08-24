package flexSC.example;

import flexSC.util.EvaRunnable;
import flexSC.util.GenRunnable;
import flexSC.util.Utils;
import flexSC.circuits.arithmetic.IntegerLib;
import flexSC.flexsc.CompEnv;
import flexSC.gc.BadLabelException;

public class Add {
	
	static public<T> T[] compute(CompEnv<T> gen, T[] inputA, T[] inputB){
	    return new IntegerLib<T>(gen).addFull(inputA, inputB, false);
	}
	
	public static class Generator<T> extends GenRunnable<T> {

		T[] inputA;
		T[] inputB;
		T[] scResult;
		
		@Override
		public void prepareInput(CompEnv<T> gen) {
			inputA = gen.inputOfAlice(Utils.fromInt(new Integer(args[0]), 32));
			gen.flush();
			inputB = gen.inputOfBob(new boolean[32]);
		}
		
		@Override
		public void secureCompute(CompEnv<T> gen) {
			scResult = compute(gen, inputA, inputB);
		}
		
		@Override
		public void prepareOutput(CompEnv<T> gen) throws BadLabelException {
		    System.out.println(Utils.toInt(gen.outputToBob(scResult)));
		    System.out.println("out length = " + scResult.length);
		}
	}
	
	public static class Evaluator<T> extends EvaRunnable<T> {
		T[] inputA;
		T[] inputB;
		T[] scResult;
		
		@Override
		public void prepareInput(CompEnv<T> gen) {
			inputA = gen.inputOfAlice(new boolean[32]);
			gen.flush();
			inputB = gen.inputOfBob(Utils.fromInt(new Integer(args[0]), 32));
		}
		
		@Override
		public void secureCompute(CompEnv<T> gen) {
			scResult = compute(gen, inputA, inputB);
		}
		
		@Override
		public void prepareOutput(CompEnv<T> gen) throws BadLabelException {
		    //gen.outputToAlice(scResult);
			System.out.println("Output to Bob:"+Utils.toInt(gen.outputToBob(scResult)));
		}
	}
}
