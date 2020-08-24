package testing;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import additive.ShareGenerator;
import common.util.Config;
import common.util.Constants;
import flexSC.flexsc.CompEnv;
import flexSC.flexsc.Flag;
import gadgets.SBranchGadget;
import gadgets.SBranchRunnable;
import setup.ServiceSetupCountTime;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SBranchUnit {
	
	
	//private static long numGate;
	private static int numInputs = 1000;
	//= Config.getSettingInt(Constants.CONFIG_NUM_INPUTS_GADGETS);
	private static int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);
	static Logger log = Logger.getLogger(SBranchUnit.class.getName());

	private static double time;
	private static long bandwidth;
	static double[] timeList = new double[numInputs];
	static long[] bandwidthList = new long[numInputs];
	
	public static void main(String[] args) {
		ShareGenerator generator = new ShareGenerator(true);
		
		
		
		
		Flag.sw.startTotal();
		int counter = 0;
		while(counter < numInputs) {
			System.out.println("counter:"+counter);
			generator.generateSharedDataPointSet(generator.random);
			long m10 = generator.x0;
			long m11 = generator.x1;
			
			generator.generateSharedDataPointSet(generator.random);
			long m20 = generator.x0;
			long m21 = generator.x1;
			
			generator.generateSharedDataPointSet(generator.random);
			long c10 = generator.x0;
			long c11 = generator.x1;
			
			generator.generateSharedDataPointSet(generator.random);
			long c20 = generator.x0;
			long c21 = generator.x1;
			
			long r = generator.generateRandom(true);
			
			String[] argsGen = new String[5];
			argsGen[0] = String.valueOf(m10);
			argsGen[1] = String.valueOf(m20);
			argsGen[2]  =String.valueOf(c10);
			argsGen[3] = String.valueOf(c20);
			argsGen[4] = String.valueOf(r);
			
			String[] argsEva = new String[4];
			argsEva[0] = String.valueOf(m11);
			argsEva[1] = String.valueOf(m21);
			argsEva[2] = String.valueOf(c11);
			argsEva[3] = String.valueOf(c21);
			
			
			int portRunner = port + counter;
			double e = System.nanoTime();
			
			SBranchRunnable runner = new SBranchRunnable();
			runner.run(portRunner, argsGen, argsEva, 1);
			
			
			double s = System.nanoTime();
			time = s-e;
			bandwidth = runner.bandwidth;
			
			timeList[counter] = time;
			bandwidthList[counter] = bandwidth;
			
			counter++;
		}
		
		Flag.sw.startTotal();
		
		
		
		log.info("----------------SBranchUnit------------------");
		log.info("time (seconds),bandwidth (MB)");
		for(int i = 0; i< numInputs; i++) {
			
			log.info(timeList[i]/1e9+","+bandwidthList[i]/1024.0/1024.0);
		}
		
		
	}

}
