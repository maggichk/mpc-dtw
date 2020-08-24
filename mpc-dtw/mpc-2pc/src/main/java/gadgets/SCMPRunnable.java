package gadgets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import flexSC.flexsc.CompEnv;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SCMPRunnable {
	
	public long res;
	public long bandwidth;
	
	public String[][] prepareArgs(long x0, long y0, long x1, long y1, long random){
		String[][] args = new String[2][];
		
		String[] argsGen = new String[3];
		argsGen[0] = String.valueOf(x0);
		argsGen[1] = String.valueOf(y0);
		argsGen[2] = String.valueOf(random);
		args[0] = argsGen;
		
		String[] argsEva =  new String[2];
		argsEva[0] = String.valueOf(x1);
		argsEva[1] = String.valueOf(y1);
		args[1] = argsEva;
		
		return args;
	}
	
	public void run(final int port, final String[] argsGen, final String[] argsEva) {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		//initialize GC		
		@SuppressWarnings("rawtypes")
		final GenRunnable runGen = (GenRunnable) new SCMPGadget.Generator<Long>();		
		@SuppressWarnings("rawtypes")
		final EvaRunnable runEva = (EvaRunnable) new SCMPGadget.Evaluator<Long>();
		
		//Alice
				exec.execute(new Runnable() {
					@Override
					@SuppressWarnings("unchecked")
					public void run() {
						runGen.setInput(argsGen);
						runGen.setConnection(port);		
						
						CompEnv<Long> env = runGen.connect();
						runGen.run(env);
						try {
							
								res =  runGen.getOutputAlice();
							
							
						} catch (Exception e) {					
							e.printStackTrace();
						}
						runGen.disconnection();
					}
					
				});
				
				//Bob
				exec.execute(new Runnable() {
					@Override
					@SuppressWarnings("unchecked")
					public void run() {
						runEva.setInput(argsEva);
						runEva.setConnection(port);
						CompEnv<Long> env = runEva.connect();
						runEva.run(env);
						runEva.disconnection();				
					}			
				});
				
				//shutdown threads
				// Connection should be established within 60s
				exec.shutdown();
				try {
					if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
						// Execution finished
						exec.shutdownNow();
					}
				} catch (InterruptedException e) {
					// Something is wrong
					exec.shutdownNow();
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				
				bandwidth = (runEva.cos.getByteCount() + runEva.cis.getByteCount());
				
	}

}
