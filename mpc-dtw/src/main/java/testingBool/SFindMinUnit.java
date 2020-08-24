package testingBool;

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
import gadgetBoolean.SFindMin2PC;
import gadgets.SFindMinGadget;
import gadgets.SFindMinRunnable;
import utilMpc.Config2PC;

import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SFindMinUnit {
	
	
	//private static long numGate;
	private static int numInputs = Config.getSettingInt(Constants.CONFIG_NUM_INPUTS_GADGETS);
	private static int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
	private static String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
	static Logger log = Logger.getLogger(SFindMinUnit.class.getName());
	
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
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		ShareGenerator generator = new ShareGenerator(true);
		
		// generate three shares
				long a = 700;// a
				long b = 14;// b
				long c = 30;// c

				long omega1 = generator.generateRandom();
				System.out.println("omega1 (random value):" + omega1);

				long omega2 = generator.generateRandom();
				System.out.println("omega2 (random value):" + omega2);

				generator.generateSharedDataPoint(a, true);
				long a_0 = generator.x0;
				long a_1 = generator.x1;

				System.out.println("a:" + a + " a_0:" + a_0 + " a_1:" + a_1 + " verify:" + AdditiveUtil.add(a_0, a_1));

				generator.generateSharedDataPoint(b, true);
				long b_0 = generator.x0;
				long b_1 = generator.x1;

				System.out.println("b:" + b + " b_0:" + b_0 + " b_1:" + b_1 + " verify:" + AdditiveUtil.add(b_0, b_1));

				generator.generateSharedDataPoint(c, true);
				long c_0 = generator.x0;
				long c_1 = generator.x1;
				System.out.println("c:" + c + " c_0:" + c_0 + " c_1:" + c_1 + " verify:" + AdditiveUtil.add(c_0, c_1));

		
		
		
		int counter = 0;
		while(counter < numInputs) {
			SFindMin2PC sfindmin = new SFindMin2PC();
			double e = System.nanoTime();
			long[] dmin = sfindmin.compute(false, sndChannel, rcvChannel, mt2, mt, a_0, b_0, c_0, a_1, b_1, c_1, omega1,
					omega2);			
			double s = System.nanoTime();
			time += s-e;
			bandwidth += sfindmin.bandwidth;
			
			timeList[counter] = s-e;
			bandwidthList[counter] = sfindmin.bandwidth;
			
			if(counter%1000==0) {
				System.out.println("Progress:"+counter);
			}
			
			counter++;
		}
		
		
		log.info("----------------SFindMinUnit------------------");
		System.out.println("total time (seconds),bandwidth (MB):"+time/1e9+","+bandwidth/1024.0/1024.0+"MB");
		log.info("total time (seconds),bandwidth (MB):"+time/1e9+","+bandwidth/1024.0/1024.0+"MB");
		log.info("time (seconds),bandwidth (MB)");
		for(int i = 0; i< numInputs; i++) {
			
			log.info(timeList[i]/1e9+","+bandwidthList[i]/1024.0/1024.0);
		}
		
		
	}

}
