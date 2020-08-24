package testingBool;

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
import distances.SLBConcurrent;
import distancesBoolean.SDTW;
import flexSC.network.Client;
import flexSC.network.Server;
import pruning.ServicePruning;
import redis.clients.jedis.Jedis;
import setup.ServiceSetupDummy;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class SDTWCountTimeDummyMT {

	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);
	private static String hostname = Config.getSetting(Constants.CONFIG_DB_IP);// LOCALHOST
	private static int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
	
	private static double band_cr = Config.getSettingDouble(Constants.CONFIG_DTW_BAND_CR);// 0.05
	public static int cr = (int) Math.floor((double) band_cr * queryLength + 1);
	
	

	//private static int numSeqs = 2;
	private static int numSeqs= Config.getSettingInt(Constants.CONFIG_NUM_SEQS);
	private static double time;
	private static long bandwidth;
	private static double[] timeList = new double[numSeqs];
	private static long[] bandwidthList = new long[numSeqs];
	
	static Logger log = Logger.getLogger(SDTWCountTimeDummyMT.class.getName());

	public static void main(String[] args) {
		log.info("-----------------------SDTWCountTimeDummy----------------------");
		log.info("start....num:"+numSeqs);
		
		
		
		
		// int loopNum = (int) Math.ceil((double)numSeqs/centersNum);
		int loopNum = numSeqs;
		log.info("loopNum:" + loopNum);
		System.out.println("loopNum:" + loopNum);
		
			
		
		final Server sndChannel = new Server();
		final Client rcvChannel = new Client();
		ConnectionHelper connectHelper = new ConnectionHelper();
		connectHelper.connect(hostname, arithmeticPort, sndChannel, rcvChannel);
		System.out.println("Connection| hostname:port, " + hostname + ":" + arithmeticPort);
		log.info("Connection| hostname:port, " + hostname + ":" + arithmeticPort);
		
		//Dummy MT
		ShareGenerator generator = new ShareGenerator(true);		
		MultiplicationTriple mt = new MultiplicationTriple(generator, sndChannel, rcvChannel);		
		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ANDTriple mt2 = new ANDTriple(boolGen);
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		System.out.println("finish generating MTs");
		
		//Dummy sequences
		// x0
				long[] x0data = new long[queryLength];
				/*x0data[0] = 1;
				x0data[1] = 1;
				x0data[2] = 1;
				x0data[3] = 2;
				x0data[4] = 3;*/
				long[] x0sqdata = new long[queryLength];
				/*x0sqdata[0] = 1;
				x0sqdata[1] = 1;
				x0sqdata[2] = 1;
				x0sqdata[3] = 4;
				x0sqdata[4] = 9;*/

				
				  for (int i = 0; i < queryLength; i++) { x0data[i] =
				  generator.generateRandom(true); x0sqdata[i] = AdditiveUtil.mul(x0data[i],
				  x0data[i]); }
				 
				SharedSequence X0 = new SharedSequence(queryLength, 0, 0, 0, x0data, x0sqdata);
				// u1
				generator.generateSharedSequence(X0);
				SharedSequence X1 = generator.S1;

				// y0
				long[] y0data = new long[queryLength];
				/*y0data[0] = 4;
				y0data[1] = 4;
				y0data[2] = 5;
				y0data[3] = 6;
				y0data[4] = 7;*/

				long[] y0sqdata = new long[queryLength];
				/*y0sqdata[0] = 16;
				y0sqdata[1] = 16;
				y0sqdata[2] = 25;
				y0sqdata[3] = 36;
				y0sqdata[4] = 49;*/

				
				  for (int i = 0; i < queryLength; i++) { y0data[i] =
				  generator.generateRandom(true); y0sqdata[i] = AdditiveUtil.mul(y0data[i],
				  y0data[i]); }
				 
				SharedSequence Y0 = new SharedSequence(queryLength, 0, 0,0, y0data, y0sqdata);
				// System.out.println("1) Y0:" + String.valueOf(Y0.getSharedData(0)[0]) + " " +
				// Y0.getSharedData(0)[1]);

				// y1
				generator.generateSharedSequence(Y0);
				SharedSequence Y1 = generator.S1;

				System.out.println("y:" + AdditiveUtil.add(Y0.getSharedSequence()[0], Y1.getSharedSequence()[0]));
				System.out.println("x:" + AdditiveUtil.add(X0.getSharedSequence()[0], X1.getSharedSequence()[0]));
		

		SDTW sdtw = new SDTW(sndChannel, rcvChannel, queryLength, cr);
		
		int counter = 0;
		while (counter < loopNum) {
			/*System.out.println("counter:" + counter);
			log.info("counter:" + counter);*/

			
			
			double e = System.nanoTime();
			
			// sndChannel, rcvChannel, mts, X0, X1, Y0, Y1, queryLength
			try {
				long[] SDTW = sdtw.compute(sndChannel, rcvChannel, mt,mt2, X0, X1, Y0, Y1, queryLength);
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
				log.error(e2.getMessage());
				log.error(e2.getStackTrace());
			}
			
			
			double s = System.nanoTime();
			
			time += s - e;
			
			
			timeList[counter] = (s-e);
			//bandwidthList[counter] = sdtw.bandwidth;
			//System.out.println("unit time:"+ (s-e)/1e9);
			//System.out.println("unit bandwidth:"+sdtw.bandwidth/1024.0/1024.0+" MB");
			log.info("unit time:"+ (s-e)/1e9);
			//log.info("unit bandwidth:"+sdtw.bandwidth/1024.0/1024.0+" MB");

			/*try {
				System.out.println("counter:" + counter + " sleep...");
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/

			if(counter%100==0) {
				System.out.println("Progress:"+counter);
			}
			counter++;

		}
		
		sndChannel.disconnect();
		rcvChannel.disconnect();
		bandwidth += sdtw.bandwidthGC;
		bandwidth += sdtw.bandwidthSSED;
		
		System.out.println("end");
		log.info("end");
		log.info("----------------------SDTWCountTimeDummy------------------------------");
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
