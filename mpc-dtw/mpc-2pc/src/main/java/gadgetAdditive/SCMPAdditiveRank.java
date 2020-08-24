package gadgetAdditive;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.AdditiveUtil2;
import additive.MultiplicationTriple;
import additive.MultiplicationTriple2;
import additive.SeqCompEngine;
import additive.SeqCompEngine2;
import additive.ShareGenerator;
import additive.SharedSequence;
import flexSC.network.Client;
import flexSC.network.Server;
import flexSC.util.Utils;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class SCMPAdditiveRank {

	private Server sndChannel;
	private Client rcvChannel;
	private int port;
	private String hostname;
	private int portCli;

	public double bandwidth = 0;

	public SCMPAdditiveRank() {

	}

	public SCMPAdditiveRank(int port, int portCli, String hostname) {
		this.port = port;
		this.hostname = hostname;
		this.portCli = portCli;

	}

	public long[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple2 mt2,
			MultiplicationTriple mt, long x0, long y0, long x1, long y1) throws Exception {
		long z[] = new long[2];

		// share over Z31
		long z0 = AdditiveUtil.sub(y0, x0);
		long z1 = AdditiveUtil.sub(y1, x1);

		long zVer = AdditiveUtil.add(z0, z1);
		System.out.println("z0:" + z0 + " z1:" + z1 + " z:" + zVer);

		// share over Z2
		int[] z0BitArr = Utils.fromLong2intRightmost(z0, 31);
		int[] z1BitArr = Utils.fromLong2intRightmost(z1, 31);

		/*for (int i = 0; i < 31; i++) {
			System.out.print(z0BitArr[i]);
		}
		System.out.println("-----------z0 string--------");

		for (int i = 0; i < 31; i++) {
			System.out.print(z1BitArr[i]);
		}
		System.out.println("-----------z1 string--------");

		// verify
		int[] zBitArr = Utils.fromLong2intRightmost(zVer, 31);
		for (int i = 0; i < 31; i++) {
			System.out.print(zBitArr[i]);
		}
		System.out.println("-----z string------");*/

		// c0, c1
		long[] c0BitArr = new long[31];
		long[] c1BitArr = new long[31];

		long z0l = 0;
		long z1l = 0;

		for (int i = 0; i < 31; i++) {
			int z0i = z0BitArr[i];
			int z1i = z1BitArr[i];

			int p0i = z0i;
			int p1i = 0;
			int q0i = 0;
			int q1i = z1i;

			int w0i = z0i;
			int w1i = z1i;
			
			//System.out.print(AdditiveUtil2.add(z0i, z1i));
			//System.out.print(AdditiveUtil2.add(w0i, w1i));
			
			

			if (i == 0) {

				// MultiplicationTriple2 mt = mts.get(0);

				SeqCompEngine2 engine0 = new SeqCompEngine2(isDisconnect, sndChannel, rcvChannel, mt2, p0i, q0i, p1i,
						q1i);
				bandwidth += engine0.bandwidth ;

				long c0i = engine0.z0;
				long c1i = engine0.z1;

				c0BitArr[0] = c0i;
				c1BitArr[0] = c1i;
				//System.out.println("round 0 verify c:" + AdditiveUtil2.add(c0i, c1i)+" verify w:"+AdditiveUtil2.add(w0i, w1i));

				// mts.remove(0);
			} else if (i == 30) {

				z0l = AdditiveUtil2.add(w0i, c0BitArr[i - 1]);//Arr[29]
				z1l = AdditiveUtil2.add(w1i, c1BitArr[i - 1]);
				//System.out.println("round:"+i+" verify w:"+AdditiveUtil2.add(w0i, w1i)+" c_i-1:"+AdditiveUtil2.add(c0BitArr[i - 1],c1BitArr[i - 1]));
				//System.out.println("z0l:" + z0l + " z1l:" + z1l);
				

			} else {
				// MultiplicationTriple2 mt = mts.get(0);

				SeqCompEngine2 engine = new SeqCompEngine2(isDisconnect, sndChannel, rcvChannel, mt2, p0i, q0i, p1i,
						q1i);
				bandwidth += engine.bandwidth ;
				// System.out.println("engine bandwidth| round:"+i+"
				// bandwidth:"+engine.bandwidth);

				long d0i = AdditiveUtil2.add(engine.z0, 1);
				long d1i = engine.z1;
				// long d1i = AdditiveUtil2.add(engine.z1, 1);

				long c0is1 = c0BitArr[i - 1];
				long c1is1 = c1BitArr[i - 1];
				SeqCompEngine2 engine1 = new SeqCompEngine2(isDisconnect, sndChannel, rcvChannel, mt2, w0i, c0is1, w1i,
						c1is1);
				bandwidth += engine1.bandwidth ;

				long e0i = AdditiveUtil2.add(engine1.z0, 1);
				long e1i = engine1.z1;
				//long e1i = AdditiveUtil2.add(engine1.z1, 1);

				SeqCompEngine2 engine2 = new SeqCompEngine2(isDisconnect, sndChannel, rcvChannel, mt2, e0i, d0i, e1i,
						d1i);
				bandwidth += engine2.bandwidth ;

				long c0i = AdditiveUtil2.add(engine2.z0, 1);
				long c1i = engine2.z1;
				//long c1i = AdditiveUtil2.add(engine2.z1, 1);

				c0BitArr[i] = c0i;
				c1BitArr[i] = c1i;

			/*	System.out.println("round:" + i + " verify d:" + AdditiveUtil2.add(d0i, d1i) + " verify c_i-1:"
						+ AdditiveUtil2.add(c0is1, c1is1) + " verify e:" + AdditiveUtil2.add(e0i, e1i) + " verify c_i:"
						+ AdditiveUtil2.add(c0i, c1i)+" verify w:"+AdditiveUtil2.add(w0i, w1i));*/
			}

		}

		

		z[0] = z0l;
		z[1] = z1l;

		return z;
	}

	/*
	 * public long[] compute(boolean isDisconnect, Server sndChannel, Client
	 * rcvChannel, MultiplicationTriple mt, long x0, long y0, long x1, long y1, long
	 * sqx0, long sqy0, long sqx1, long sqy1) throws Exception { SeqCompEngine
	 * engine = new SeqCompEngine(isDisconnect, sndChannel, rcvChannel, mt, x0, y0,
	 * x1, y1); long ssed[] = new long[2]; long z0 = AdditiveUtil.mul(2L,
	 * engine.z0); long z1 = AdditiveUtil.mul(2L, engine.z1);
	 * //System.out.println("z0:"+z0+" z1:"+z1+" z:"+AdditiveUtil.add(z0, z1));
	 * ssed[0] = AdditiveUtil.sub(AdditiveUtil.add(sqx0, sqy0), z0);//x0^2 + y0^2 -
	 * 2x0y0 ssed[1] = AdditiveUtil.sub(AdditiveUtil.add(sqx1, sqy1), z1);//x1^2 +
	 * y1^2 - 2x1y1 bandwidth = engine.bandwidth; return ssed; }
	 * 
	 * 
	 * public long[] computeConcurrent(MultiplicationTriple mt, long x0, long y0,
	 * long x1, long y1, long sqx0, long sqy0, long sqx1, long sqy1) throws
	 * Exception { this.sndChannel = new Server(); this.rcvChannel = new Client();
	 * ConnectionHelper connector = new ConnectionHelper();
	 * //connector.connect(hostname, port, sndChannel, rcvChannel);
	 * connector.connect(hostname, port, portCli, sndChannel, rcvChannel);
	 * 
	 * SeqCompEngine engine = new SeqCompEngine(true, sndChannel, rcvChannel, mt,
	 * x0, y0, x1, y1); long ssed[] = new long[2]; long z0 = AdditiveUtil.mul(2L,
	 * engine.z0); long z1 = AdditiveUtil.mul(2L, engine.z1);
	 * //System.out.println("z0:"+z0+" z1:"+z1+" z:"+AdditiveUtil.add(z0, z1));
	 * ssed[0] = AdditiveUtil.sub(AdditiveUtil.add(sqx0, sqy0), z0);//x0^2 + y0^2 -
	 * 2x0y0 ssed[1] = AdditiveUtil.sub(AdditiveUtil.add(sqx1, sqy1), z1);//x1^2 +
	 * y1^2 - 2x1y1
	 * 
	 * sndChannel.disconnect(); rcvChannel.disconnect();
	 * 
	 * bandwidth = engine.bandwidth;
	 * 
	 * return ssed; }
	 */

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
		long a = 40;
		long b = 200;

		generator.generateSharedDataPoint(a, true);
		long a0 = generator.x0;
		long a1 = generator.x1;

		System.out.println("a:" + a + " a0:" + a0 + " a1:" + a1 + " verify:" + AdditiveUtil.add(a0, a1));

		generator.generateSharedDataPoint(b, true);
		long b0 = generator.x0;
		long b1 = generator.x1;

		System.out.println("b:" + b + " b0:" + b0 + " b1:" + b1 + " verify:" + AdditiveUtil.add(b0, b1));

		double e = System.nanoTime();
		SCMPAdditiveRank scmp = new SCMPAdditiveRank();// y, u
		long[] z = scmp.compute(false, sndChannel, rcvChannel, mt2, mt, a0, b0, a1, b1);

		double s = System.nanoTime();
		System.out.println("unit time:" + (s - e) / 1e9 + " seconds");

		// verify
		long z0 = z[0];
		long z1 = z[1];
		System.out.println("z:" + AdditiveUtil2.add(z0, z1) + " z0:" + z0 + " z1:" + z1);

		System.out.println("bandwidth:" + scmp.bandwidth + " B");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

}
