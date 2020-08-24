package booleanShr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.MultiplicationTriple2;
import additive.SeqCompEngine;
import additive.SeqCompEngine2;
import additive.ShareGenerator;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetAdditive.SCMPAdditive;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

public class SBranchAdditive {

	private Server sndChannel;
	private Client rcvChannel;
	private int port;
	private String hostname;
	private int portCli;

	public double bandwidth = 0;

	//private static final long MODULUS = Constants2PC.MODULUS; // 2^31

	public SBranchAdditive() {

	}

	public SBranchAdditive(int port, int portCli, String hostname) {
		this.port = port;
		this.hostname = hostname;
		this.portCli = portCli;
	}

	/**
	 * SBranch(m1, m2, c1,c2, omega)
	 * 
	 * @param isDisconnect
	 * @param sndChannel
	 * @param rcvChannel
	 * @param mt2
	 * @param mt
	 * @param m1_0
	 * @param m2_0
	 * @param m1_1
	 * @param m2_1
	 * @param c_0
	 * @param c_1
	 * @param omega
	 * @return
	 * @throws Exception
	 */
	public long[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple2 mt2,
			MultiplicationTriple mt, long m1_0, long m2_0, long m1_1, long m2_1, long c1_0, long c2_0, long c1_1, 
			long c2_1, long omega) throws Exception {

		long[] dist = new long[2];

		SCMPAdditive scmp = new SCMPAdditive();

		long[] z = scmp.compute(isDisconnect, sndChannel, rcvChannel, mt2, mt, m1_0, m2_0, m1_1, m2_1);
		//bandwidth scmp
		this.bandwidth += scmp.bandwidth;
		
		long z0 = z[0];
		long z1 = z[1];

		long z0s1 = AdditiveUtil.sub(0, z0);
		long z1s1 = AdditiveUtil.sub(1, z1);
		SeqCompEngine engine_1 = new SeqCompEngine(isDisconnect, sndChannel, rcvChannel, mt, z0s1, c1_0, z1s1, c1_1);
		this.bandwidth += engine_1.bandwidth;
		
		SeqCompEngine engine_2 = new SeqCompEngine(isDisconnect, sndChannel, rcvChannel, mt, z0, c2_0, z1, c2_1);
		this.bandwidth += engine_2.bandwidth;
		
		long dist0 = AdditiveUtil.sub( AdditiveUtil.add(engine_1.z0, engine_2.z0), omega);
		long dist1 = AdditiveUtil.add( AdditiveUtil.add(engine_1.z1, engine_2.z1), omega);

		dist[0] = dist0;
		dist[1] = dist1;

		return dist;
	}

	public static void main(String[] args) throws Exception {
		final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		final int port = 5553;
		// Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
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

		// ArrayList<MultiplicationTriple> mts = new ArrayList<MultiplicationTriple>();

		/*
		 * for (int i = 0; i < 4; i++) { MultiplicationTriple mt = new
		 * MultiplicationTriple(sndChannel, rcvChannel); mts.add(mt); }
		 */

		MultiplicationTriple2 mt2 = new MultiplicationTriple2(sndChannel, rcvChannel);
		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		ShareGenerator generator = new ShareGenerator(true);

		// generate two shares
		long m1 = 7;//u
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

		SBranchAdditive sbranch = new SBranchAdditive();		
		
		int round = 100000; //10^5
		double time=0.0;
		for(int i=0; i<round; i++) {
			double e = System.nanoTime();		
			long[] dist = sbranch.compute(false, sndChannel, rcvChannel, mt2, mt, m1_0, m2_0, m1_1, m2_1, c1_0,c2_0, c1_1, 
					c2_1, omega);
			double s = System.nanoTime();
			time += s-e;
			if(i%1000==0) {
				System.out.println("Progress:"+i);
			}		
		}

		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + sbranch.bandwidth/1024.0/1024.0/1024.0 + " GB");
		
		// verify
		//long dist0 = dist[0];
		//long dist1 = dist[1];
		//System.out.println("dist:" + AdditiveUtil.add(dist0, dist1) + " dist0:" + dist0 + " dist1:" + dist1);

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}
}
