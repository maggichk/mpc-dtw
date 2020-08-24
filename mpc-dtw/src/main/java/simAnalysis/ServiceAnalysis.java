package simAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import additive.MultiplicationTriple;
import additive.ShareGenerator;
import additive.SharedSequence;
import common.util.Config;
import common.util.Constants;
import distances.SDTW;
import distances.SLBConcurrent;
import gadgets.SCMPRankRunnable;
import gadgets.SCMPRunnable;
import flexSC.network.Client;
import flexSC.network.Server;
import preparation.ExtractSequenceFromDB;
import pruning.ServicePruning;
import redis.clients.jedis.Jedis;
import setup.ServiceSetupDummy;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class ServiceAnalysis {
	private static String hostname = Config.getSetting(Constants.CONFIG_DB_IP);// LOCALHOST
	// private static int redisPort =
	// Config.getSettingInt(Constants.CONFIG_DB_PORT);// 6379
	private static int arithmeticPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);// 5000
	private static int gcPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);// 50000
	private static int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);// 128
	private static double band_cr = Config.getSettingDouble(Constants.CONFIG_DTW_BAND_CR);// 0.05

	
	public long numCandidateSeq=0;
	static Logger log = Logger.getLogger(ServiceAnalysis.class.getName());
	
	
	public double time;
	
	
	public HashMap<Integer, long[]> getDTWs(Jedis jedis, ArrayList<Integer> clusters, ServicePruning servicePruning) throws InterruptedException {

		ShareGenerator generator = new ShareGenerator(true);
		HashMap<Integer, long[]> mapDTW = new HashMap<Integer, long[]>();
		// compare LB
		SLBConcurrent slb = new SLBConcurrent(queryLength);
		
		SCMPRankRunnable scmpRank = new SCMPRankRunnable();
		SCMPRunnable scmp = new SCMPRunnable();
		ConnectionHelper connector = new ConnectionHelper();


		for (int i = 0; i < clusters.size(); i++) {
			// cluster no
			int clusterNo = clusters.get(i);

			// retrieve all share0 keys from DB
			String keyPattern = "false|" + clusterNo + "|*|0";
			Set<String> keys = jedis.keys(keyPattern);
			
			numCandidateSeq += keys.size();

			// load mts
			/*int keysSize = keys.size();
			int mtNumLB = keysSize * 2 * queryLength;
			int mtNumDTW = keysSize * (queryLength * (2*servicePruning.cr+1));
			int mtNum = mtNumLB + mtNumDTW;*/
			
			//dummy mt
			int mtNumDummy = queryLength * 2 + (queryLength * (2*servicePruning.cr+1));
			ServiceSetupDummy mtLoader = new ServiceSetupDummy();
			//ArrayList<MultiplicationTriple> mts = mtLoader.loadMT(mtNum);
			ArrayList<MultiplicationTriple> mtsDummy = mtLoader.loadMT(mtNumDummy);

			// retrieve each sequence belonging to candidate clusters from DB
			Iterator<String> itr = keys.iterator();
			while (itr.hasNext()) {
				ArrayList<MultiplicationTriple> mts = new ArrayList<MultiplicationTriple>();
				mts.addAll(mtsDummy);
				
				
				
				String key0 = itr.next();
				SharedSequence seq0 = ExtractSequenceFromDB.extract(jedis, key0);
				String key1 = key0.substring(0, key0.length()-1).concat("1");
				SharedSequence seq1 = ExtractSequenceFromDB.extract(jedis, key1);

				double e = System.nanoTime();
				// compute SLB
				long[] SLB = slb.computeConcurrent(mts, servicePruning.U0, servicePruning.U1, servicePruning.L0,
						servicePruning.L1, seq0, seq1);
				double s = System.nanoTime();
				time += s-e;
				
				//System.out.println("Finish SLB calculation.");
				//log.info("Finish SLB calculation.");
				
				/*System.out.println("sleeping...");
				Thread.sleep(5000);*/
				
				
				
				String[][] args = scmpRank.prepareArgs(SLB[0], servicePruning.thresholdShares[0], SLB[1],
						servicePruning.thresholdShares[1]);// rank = 0, X<Y -> SLB<threshold
				String[] argsGen = args[0];
				String[] argsEva = args[1];
				
				double e1 = System.nanoTime();
				scmpRank.run(gcPort, argsGen, argsEva);
				double s1 = System.nanoTime();
				time += s1-e1;
				
				long rank = scmpRank.rank;
				if (rank == 0) {

					// SDWT(sequence, query)
					SharedSequence[] sequences = new SharedSequence[4];
					sequences[0] = seq0; // x0
					sequences[1] = seq1; // x1
					sequences[2] = servicePruning.query0; // y0
					sequences[3] = servicePruning.query1; // y1
					
					// DTW
					Server sndChannelDTW = new Server();
					Client rcvChannelDTW = new Client();
					if (ConnectionHelper.available(arithmeticPort) == false) {
						System.out.println("aithmeticPort in use:" + arithmeticPort);
					}
					if (ConnectionHelper.available(gcPort) == false) {
						System.out.println("gcPort in use:" + gcPort);
					}
					connector.connect(hostname, arithmeticPort, sndChannelDTW, rcvChannelDTW);
					SDTW sdtw = new SDTW(sndChannelDTW, rcvChannelDTW, queryLength, servicePruning.cr);
					
					double e2 = System.nanoTime();					
					long[] SDTW = sdtw.compute(mts, sequences, queryLength);
					double s2 = System.nanoTime();
					time += s2-e2;
					
					// SCMP
					long random = generator.generateRandom();
					String[][] args2 = scmp.prepareArgs(SDTW[0], servicePruning.thresholdShares[0], SDTW[1],
							servicePruning.thresholdShares[1], random);
					String[] argsGen2 = args2[0];
					String[] argsEva2 = args2[1];
					
					double e3 = System.nanoTime();
					scmp.run(gcPort, argsGen2, argsEva2);
					double s3 = System.nanoTime();
					time += s3-e3;
					
					long res = scmp.res;
					long[] out = new long[2];
					out[0] = random;
					out[1] = res;
					mapDTW.put(seq0.getIndex(), out);
					
				}

			}

		}

		return mapDTW;
	}

}
