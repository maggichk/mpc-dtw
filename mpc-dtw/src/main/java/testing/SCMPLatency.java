package testing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import additive.ShareGenerator;
import common.util.Config;
import common.util.Constants;
import flexSC.flexsc.CompEnv;
import flexSC.flexsc.Flag;
import gadgets.SCMPGadget;
import gadgets.SCMPGadgetRank;
import gadgets.SFindMinGadget;

import utilMpc.Config2PC;

import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SCMPLatency {
	
	private static double time;
	//private static long numGate;
	private static int numInputs = Config.getSettingInt(Constants.CONFIG_NUM_INPUTS_GADGETS);
	private static int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);
	static Logger log = Logger.getLogger(SCMPLatency.class.getName());
	private static long andGate = 0;
	private static double bandwidth =0.0;
	
	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
		
		
			
			
		
		//initialize GC		
		/*@SuppressWarnings("rawtypes")
		final GenRunnable runGen = (GenRunnable) new SCMPGadget.Generator<Long>();		
		@SuppressWarnings("rawtypes")
		final EvaRunnable runEva = (EvaRunnable) new SCMPGadget.Evaluator<Long>();*/
		
		
		
		Flag.sw.startTotal();
		int counter = 0;
		while(counter < numInputs) {
			System.out.println("counter:"+counter);
			
			ShareGenerator generator = new ShareGenerator(true);			
			ExecutorService exec = Executors.newFixedThreadPool(2);
			
			double e1 = System.nanoTime();
			//long timeroff1 = System.nanoTime();
			final GenRunnable runGen = (GenRunnable) new SCMPGadget.Generator<Long>();
			final EvaRunnable runEva = (EvaRunnable) new SCMPGadget.Evaluator<Long>();
			//long timeroff2 = System.nanoTime();
			//System.out.println("offline time:"+(timeroff2-timeroff1)/1e9+" s");
			
		
		//Alice
		exec.execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				
				runGen.setConnection(port);		
				
				//listen port + garbling
				CompEnv<Long> env = runGen.connect();
				runGen.env = env;
								
			}
			
		});
		
		//Bob
		exec.execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				//runEva.setInput(argsEva);
				runEva.setConnection(port);
				CompEnv<Long> env = runEva.connect();
				runEva.env = env;
				//runEva.run(env);
					
			}			
		});
		
		//shutdown threads
		// Connection should be established within 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(10, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
			}
		} catch (InterruptedException e) {
			// Something is wrong
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		
		
		
		
		
			generator.generateSharedDataPointSet(generator.random);
			long a0 = generator.x0;
			long a1 = generator.x1;
			
			generator.generateSharedDataPointSet(generator.random);
			long b0 = generator.x0;
			long b1 = generator.x1;
			
			long r = generator.generateRandom(true);
			
			String[] argsGen = new String[3];
			argsGen[0] = String.valueOf(a0);
			argsGen[1] = String.valueOf(b0);
			argsGen[2] = String.valueOf(r);
			
			
			String[] argsEva = new String[2];
			argsEva[0] = String.valueOf(a1);
			argsEva[1] = String.valueOf(b1);
			
			
			
			
			
			
			ExecutorService exec2 = Executors.newFixedThreadPool(2);
			exec2.execute(new Runnable() {

				@Override
				public void run() {
					runGen.setInput(argsGen);
					runGen.run(runGen.env);
					//numGate += runGen.env.numOfAnds;
					//System.out.println("number of and gates:"+runGen.env.numOfAnds);
					
				}
				
			});
			
			exec2.execute(new Runnable() {

				@Override
				public void run() {
					runEva.setInput(argsEva);
					runEva.run(runEva.env);
					
				}
				
			});
			
			
			//shutdown threads
			// Connection should be established within 60s
			exec2.shutdown();
			try {
				if (exec2.awaitTermination(10, TimeUnit.SECONDS)) {
					// Execution finished
					exec2.shutdownNow();
				}
			} catch (InterruptedException e2) {
				// Something is wrong
				exec2.shutdownNow();
				Thread.currentThread().interrupt();
				throw new RuntimeException(e2);
			}
			
			
			
			
			
			double s1 = System.nanoTime();
			time += s1-e1;
			andGate += runGen.env.numOfAnds;
			bandwidth += runEva.cos.getByteCount() + runEva.cis.getByteCount();
			
			runEva.disconnection();			
			runGen.disconnection();
			
			counter++;
		}
		
		Flag.sw.startTotal();
		
		
		System.out.println("----------------SCMP------------------");
		System.out.println("number of inputs:"+numInputs);
		System.out.println("time:"+time/1e9);
		System.out.println("number of and gates:"+andGate);
		System.out.println("\n********************************\n"
					+ "Bandwidth :" + bandwidth / 1024.0 / 1024.0 + "MB"
					+ "\n********************************");
		
		/*if (Flag.countIO)
			runEva.printStatistic();*/
		
		log.info("----------------SCMP------------------");
		log.info("number of inputs:"+numInputs);
		log.info("time:"+time/1e9);
		log.info("number of and gates:"+andGate);
		log.info("\n********************************\n"
					+ "Bandwidth :" + bandwidth / 1024.0 / 1024.0 + "MB"
					+ "\n********************************");
		
		
	}

}
