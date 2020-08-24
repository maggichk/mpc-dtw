package gadgets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import flexSC.flexsc.CompEnv;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SBranchRunnable {

public long dist1_shr0 ;
public long dist2_shr0 ;

public long bandwidth;

public void run(final int port, final String[] argsGen, final String[] argsEva, final int selectionBit) {
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		//initialize GC		
		@SuppressWarnings("rawtypes")
		final GenRunnable runGen = (GenRunnable) new SBranchGadget.Generator<Long>();		
		@SuppressWarnings("rawtypes")
		final EvaRunnable runEva = (EvaRunnable) new SBranchGadget.Evaluator<Long>();
		
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
					if(selectionBit == 1) {
						//branch(U,Y)
						dist1_shr0 =  runGen.getOutputAlice();
					}else if(selectionBit == 2) {
						//branch(Y,L)
						dist2_shr0 = runGen.getOutputAlice();
					}
					
				} catch (Exception e) {					
					e.printStackTrace();
				}
				
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
					
			}			
		});
		
		//shutdown threads
		// Connection should be established within 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
				
				runEva.disconnection();			
				runGen.disconnection();
				
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
