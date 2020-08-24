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
import gadgetBoolean.SCMP2PC;
import gadgets.SBranchGadget;
import gadgets.SBranchRunnable;
import gadgets.SCMPRunnable;
import setup.ServiceSetupCountTime;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SCMPUnit {

	// private static long numGate;
	private static int numInputs = Config.getSettingInt(Constants.CONFIG_NUM_INPUTS_GADGETS);
	private static int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
	private static String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
	static Logger log = Logger.getLogger(SCMPUnit.class.getName());

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

		// generate two shares
		long a = 4000;
		long b = 10;

		generator.generateSharedDataPoint(a, true);
		long a0 = generator.x0;
		long a1 = generator.x1;

		System.out.println("a:" + a + " a0:" + a0 + " a1:" + a1 + " verify:" + AdditiveUtil.add(a0, a1));

		generator.generateSharedDataPoint(b, true);
		long b0 = generator.x0;
		long b1 = generator.x1;

		System.out.println("b:" + b + " b0:" + b0 + " b1:" + b1 + " verify:" + AdditiveUtil.add(b0, b1));

		
		
		int counter = 0;
		while (counter < numInputs) {
			SCMP2PC scmp = new SCMP2PC();// y, u
			double e = System.nanoTime();
			long[] z = scmp.compute(false, sndChannel, rcvChannel, mt2, mt, a0, b0, a1, b1);
			double s = System.nanoTime();
			time = s - e;
			bandwidth = scmp.bandwidth;

			timeList[counter] = time;
			bandwidthList[counter] = bandwidth;

			if(counter%1000==0) {
				System.out.println("Progress:"+counter);
			}
			
			counter++;
		}

		Flag.sw.startTotal();

		log.info("----------------SCMPUnit------------------");
		log.info("time (seconds),bandwidth (MB)");
		for (int i = 0; i < numInputs; i++) {

			log.info(timeList[i] / 1e9 + "," + bandwidthList[i] / 1024.0 / 1024.0);
		}

	}

}
