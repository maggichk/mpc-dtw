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

import gadgets.SFindMinGadget;
import gadgets.SFindMinRunnable;
import utilMpc.Config2PC;

import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SFindMinUnit {
	
	
	//private static long numGate;
	private static int numInputs = 1;
	//= Config.getSettingInt(Constants.CONFIG_NUM_INPUTS_GADGETS);
	private static int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);
	static Logger log = Logger.getLogger(SFindMinUnit.class.getName());
	
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
			long a0 = generator.x0;
			long a1 = generator.x1;
			
			generator.generateSharedDataPointSet(generator.random);
			long b0 = generator.x0;
			long b1 = generator.x1;
			
			generator.generateSharedDataPointSet(generator.random);
			long c0 = generator.x0;
			long c1 = generator.x1;
			
			
			long r = generator.generateRandom(true);
			
			String[] argsGen = new String[4];
			argsGen[0] = String.valueOf(a0);
			argsGen[1] = String.valueOf(b0);
			argsGen[2]  =String.valueOf(c0);
			argsGen[3] = String.valueOf(r);
			
			
			String[] argsEva = new String[3];
			argsEva[0] = String.valueOf(a1);
			argsEva[1] = String.valueOf(b1);
			argsEva[2] = String.valueOf(c1);
			
			
			int portRunner = port + counter;
			double e = System.nanoTime();
			
			SFindMinRunnable runner = new SFindMinRunnable();
			runner.run(portRunner, argsGen, argsEva);
			
			double s = System.nanoTime();
			time = s-e;
			bandwidth = runner.bandwidth;
			
			timeList[counter] = time;
			bandwidthList[counter] = bandwidth;
			
			counter++;
		}
		
		Flag.sw.startTotal();
		
		
		
		log.info("----------------SFindMinUnit------------------");
		log.info("time (seconds),bandwidth (MB)");
		for(int i = 0; i< numInputs; i++) {
			
			log.info(timeList[i]/1e9+","+bandwidthList[i]/1024.0/1024.0);
		}
		
		
	}

}
