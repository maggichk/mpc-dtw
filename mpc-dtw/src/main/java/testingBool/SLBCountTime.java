package testingBool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import org.apache.log4j.Logger;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.ShareGenerator;
import additive.SharedSequence;
import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import common.db.ConnectRedis;
import common.util.Config;
import common.util.Constants;
import distances.SDTW;
import distancesBoolean.SLBConcurrent;
import flexSC.network.Client;
import flexSC.network.Server;
import pruning.ServicePruning;
import redis.clients.jedis.Jedis;
import setup.ServiceSetupDummy;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class SLBCountTime {

	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);
	private static int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
	private static String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);

	private static int numSeqs =  Config.getSettingInt(Constants.CONFIG_NUM_SEQS);
	private static double time;
	private static double bandwidth;
	static double[] timeList = new double[numSeqs];
	static double[] bandwidthList = new double[numSeqs];
	
	static Logger log = Logger.getLogger(SLBCountTime.class.getName());

	public static void main(String[] args) {
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
		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		sndChannel.disconnect();
		rcvChannel.disconnect();
		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ANDTriple mt2 = new ANDTriple(boolGen);

		System.out.println("finish generating MTs");

		
		ShareGenerator generator = new ShareGenerator(true);
		// u
		long[] udata = new long[queryLength];
		// u0data[0] = 10;
		// u0data[1] = 10;
		long[] usqdata = new long[queryLength];
		// u0sqdata[0] = 100;
		// u0sqdata[1] = 100;
		for (int i = 0; i < queryLength; i++) {
			udata[i] = 10;
			usqdata[i] = 100;
			// u0data[i] = generator.generateRandom(true);
			// u0sqdata[i] = AdditiveUtil.mul(u0data[i], u0data[i]);
		}
		SharedSequence U0 = new SharedSequence(queryLength, 0, 0, 0, udata, usqdata);
		// u1
		generator.generateSharedSequence(U0);
		SharedSequence U1 = generator.S1;
		U0 = generator.S0;
		System.out.println(
				"u[0]:" + udata[0] + " U0[0]:" + U0.getSharedSequence()[0] + " U1[0]:" + U1.getSharedSequence()[0]);

		// l
		long[] ldata = new long[queryLength];
		// l0data[0] = 7;
		// l0data[1] = 7;
		long[] lsqdata = new long[queryLength];
		// l0sqdata[0] = 49;
		// l0sqdata[1] = 49;
		for (int i = 0; i < queryLength; i++) {
			// l0data[i] = generator.generateRandom(true);
			// l0sqdata[i] = AdditiveUtil.mul(l0data[i], l0data[i]);
			ldata[i] = 7;
			lsqdata[i] = 49;
		}
		SharedSequence L0 = new SharedSequence(queryLength, 0, 0, 0, ldata, lsqdata);
		// l1
		generator.generateSharedSequence(L0);
		SharedSequence L1 = generator.S1;
		L0 = generator.S0; // l-L1
		System.out.println(
				"l[0]:" + ldata[0] + " L0[0]:" + L0.getSharedSequence()[0] + " L1[0]:" + L1.getSharedSequence()[0]);

		// y
		long[] ydata = new long[queryLength];
		// y0data[0] = 3;
		// y0data[1] = 4;
		long[] ysqdata = new long[queryLength];
		// y0sqdata[0] = 9;
		// y0sqdata[1] = 16;
		for (int i = 0; i < queryLength; i++) {
			ydata[i] = 30;
			ysqdata[i] = 900;
			//y0data[i] = generator.generateRandom(true);
			//y0sqdata[i] = AdditiveUtil.mul(y0data[i], y0data[i]);
		}
		SharedSequence Y0 = new SharedSequence(queryLength, 0, 0, 0, ydata, ysqdata);
		// System.out.println("1) Y0:" + String.valueOf(Y0.getSharedData(0)[0]) + " " +
		// Y0.getSharedData(0)[1]);

		// y1
		generator.generateSharedSequence(Y0);
		SharedSequence Y1 = generator.S1;
		Y0 = generator.S0;

		System.out.println("y[0]:" + AdditiveUtil.add(Y0.getSharedSequence()[0], Y1.getSharedSequence()[0]));
		System.out.println("u[0]:" + AdditiveUtil.add(U0.getSharedSequence()[0], U1.getSharedSequence()[0]));
		System.out.println("l[0]:" + AdditiveUtil.add(L0.getSharedSequence()[0], L1.getSharedSequence()[0]));


		int counter = 0;
		while (counter < numSeqs) {
			System.out.println("counter:" + counter);
			// slb
			SLBConcurrent slb = new SLBConcurrent(queryLength);
			double e = System.nanoTime();			
			long[] SLB = slb.computeConcurrent(mt, mt2, U0, U1, L0, L1, Y0, Y1);
			double s = System.nanoTime();
			//System.out.println("SLB:" + AdditiveUtil.add(SLB[0], SLB[1]));
			time += s - e;
			bandwidth += slb.bandwidth;
			
			timeList[counter] = (s-e);
			bandwidthList[counter] = slb.bandwidth;
			System.out.println("unit time:"+ (s-e)/1e9);
			System.out.println("unit bandwidth:"+slb.bandwidth/1024.0/1024.0+" MB");
			log.info("unit time:"+ (s-e)/1e9);
			log.info("unit bandwidth:"+slb.bandwidth/1024.0/1024.0+" MB");

			/*try {
				System.out.println("counter:" + counter + " sleep...");
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/

			counter++;

		}
		System.out.println("end");
		log.info("----------------------SLB------------------------------");
		log.info("num of calls:" + numSeqs);
		log.info("bandwidth:" + bandwidth / 1024.0 / 1024.0 + " MB");
		log.info("time:" + time / 1e9 + " seconds");

		System.out.println("num of calls:" + numSeqs);
		System.out.println("bandwidth:" + bandwidth / 1024.0 / 1024.0 + " MB");
		System.out.println("time:" + time / 1e9 + " seconds");
		
		log.info("unit time...");
		for(int i=0; i<numSeqs; i++) {
			log.info(timeList[i]/1e9);
			
		}

	}

}
