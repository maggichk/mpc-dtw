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
import gadgets.SCMPGadgetRank;
import gadgets.SFindMinGadget;

import utilMpc.Config2PC;

import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SCMPRank {
	
	private static double time;
	//private static long numGate;
	private static int numInputs =1;
	//= Config.getSettingInt(Constants.CONFIG_NUM_INPUTS_GADGETS);
	private static int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);
	static Logger log = Logger.getLogger(SCMPRank.class.getName());
	
	public static void main(String[] args) {
		ShareGenerator generator = new ShareGenerator(true);
		
		ExecutorService exec = Executors.newFixedThreadPool(2);
		//initialize GC		
		@SuppressWarnings("rawtypes")
		final GenRunnable runGen = (GenRunnable) new SCMPGadgetRank.Generator<Long>();		
		@SuppressWarnings("rawtypes")
		final EvaRunnable runEva = (EvaRunnable) new SCMPGadgetRank.Evaluator<Long>();
		
		
		
		//Alice
		exec.execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {
				
				runGen.setConnection(port);		
				
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
		
		
		
		long SDTW =10862651;
		long threshold = 600000;
		Flag.sw.startTotal();
		int counter = 0;
		while(counter < numInputs) {
			System.out.println("counter:"+counter);
			//generator.generateSharedDataPointSet(generator.random);
			generator.generateSharedDataPoint(SDTW, true);
			long a0 = generator.x0;
			long a1 = generator.x1;
			
			//generator.generateSharedDataPointSet(generator.random);
			generator.generateSharedDataPoint(threshold, true);
			long b0 = generator.x0;
			long b1 = generator.x1;
			
			
			
			String[] argsGen = new String[2];
			argsGen[0] = String.valueOf(a0);
			argsGen[1] = String.valueOf(b0);
			
			
			
			String[] argsEva = new String[2];
			argsEva[0] = String.valueOf(a1);
			argsEva[1] = String.valueOf(b1);
			
			
			
			
			double e = System.nanoTime();
			
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
			try {
				System.out.println("rank:"+runGen.getOutputAlice());
			
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			double s = System.nanoTime();
			time += s-e;
			
			counter++;
		}
		
		Flag.sw.startTotal();
		
		runEva.disconnection();			
		runGen.disconnection();
		System.out.println("number of and gates:"+runGen.env.numOfAnds);
		//System.out.println("Number Of AND Gates:" + numGate);
		System.out.println("time:"+time/1e9);
		if (Flag.countIO)
			runEva.printStatistic();
		
		log.info("----------------SCMPRank------------------");
		log.info("number of inputs:"+numInputs);
		log.info("time:"+time/1e9);
		log.info("number of and gates:"+runGen.env.numOfAnds);
		log.info("\n********************************\n"
					+ "Data Sent from Client :" + runEva.cos.getByteCount() / 1024.0
					/ 1024.0 + "MB\n" + "Data Sent to Client :"
					+ runEva.cis.getByteCount() / 1024.0 / 1024.0 + "MB"
					+ "\n********************************");
		
		
	}

}
