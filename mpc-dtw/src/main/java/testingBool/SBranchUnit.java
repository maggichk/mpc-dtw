package testingBool;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import common.util.Config;
import common.util.Constants;
import flexSC.flexsc.CompEnv;
import flexSC.flexsc.Flag;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBoolean.SBranch2PC;
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
	private static int numInputs = Config.getSettingInt(Constants.CONFIG_NUM_INPUTS_GADGETS);
	private static int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
	private static String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
	static Logger log = Logger.getLogger(SBranchUnit.class.getName());

	private static double time;
	private static double bandwidth;
	static double[] timeList = new double[numInputs];
	static double[] bandwidthList = new double[numInputs];
	
	public static void main(String[] args) throws Exception {
		System.out.println("Connection| hostname:port, " + hostname + ":" + port);
		final Server sndChannel = new Server();
		final Client rcvChannel = new Client();
		// Establish the connection
				ExecutorService exec = Executors.newFixedThreadPool(2);
				exec.execute(new Runnable() {

					@Override
					public void run() {
						sndChannel.listen(port);
						sndChannel.flush();
					}
				});

				exec.execute(new Runnable() {
					@Override
					public void run() {
						try {
							rcvChannel.connect(hostname, port);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						rcvChannel.flush();
					}
				});
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
		
				BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
				ANDTriple mt2 = new ANDTriple(boolGen);
				MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
				// long bandwidthMT =
				// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
				rcvChannel.cis.resetByteCount();
				rcvChannel.cos.resetByteCount();

				ShareGenerator generator = new ShareGenerator(true);
				// generate two shares
				long m1 = 100;//u
				long m2 = 14;//y

				long c1 = 9;
				long c2 = 0;

				long omega = generator.generateRandom();
				System.out.println("omega(random value):" + omega);

				generator.generateSharedDataPoint(m1, true);
				long m1_0 = generator.x0;
				long m1_1 = generator.x1;

				System.out.println("m1:" + m1 + " m1_0:" + m1_0 + " m1_1:" + m1_1 + " verify:" + AdditiveUtil.add(m1_0, m1_1));

				generator.generateSharedDataPoint(m2, true);
				long m2_0 = generator.x0;
				long m2_1 = generator.x1;

				System.out.println("m2:" + m2 + " m2_0:" + m2_0 + " m2_1:" + m2_1 + " verify:" + AdditiveUtil.add(m2_0, m2_1));

				generator.generateSharedDataPoint(c1, true);
				long c1_0 = generator.x0;
				long c1_1 = generator.x1;
				System.out.println("c1:" + c1 + " c1_0:" + c1_0 + " c1_1:" + c1_1 + " verify:" + AdditiveUtil.add(c1_0, c1_1));

				generator.generateSharedDataPoint(c2, true);
				long c2_0 = generator.x0;
				long c2_1 = generator.x1;
				System.out.println("c2:" + c2 + " c2_0:" + c2_0 + " c2_1:" + c2_1 + " verify:" + AdditiveUtil.add(c2_0, c2_1));

		
		int counter = 0;
		while(counter < numInputs) {
			SBranch2PC sbranch = new SBranch2PC();
			double e = System.nanoTime();			
			long[] dist = sbranch.compute(false, sndChannel, rcvChannel, mt2, mt, m1_0, m2_0, m1_1, m2_1, c1_0,c2_0, c1_1, 
					c2_1, omega);
			double s = System.nanoTime();
			time = s-e;
			bandwidth = sbranch.bandwidth;
			
			timeList[counter] = time;
			bandwidthList[counter] = bandwidth;

			if(counter%1000==0) {
				System.out.println("Progress:"+counter);
			}
			
			counter++;
		}
		
		
		
		
		
		log.info("----------------SBranchUnit------------------");
		log.info("time (seconds),bandwidth (MB)");
		for(int i = 0; i< numInputs; i++) {
			
			log.info(timeList[i]/1e9+","+bandwidthList[i]/1024.0/1024.0);
		}
		
		
	}

}
