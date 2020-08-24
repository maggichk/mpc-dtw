package additive;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import common.util.Config;
import common.util.Constants;
import flexSC.network.Client;
import flexSC.network.Server;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

public class SeqCompEngine {

	private Server sndChannel;
	private Client rcvChannel;
	public long z0;
	public long z1;
	public long x0;
	public long x1;
	public long y0;
	public long y1;

	public long bandwidth=0;
	
	
	
	public SeqCompEngine(boolean isDisconnect, Server sndChannel, Client rcvChannel, final MultiplicationTriple mt,
			long x0, long y0, long x1, long y1) throws Exception {
		//System.out.println("engine...start..:"+isDisconnect);
		this.x0 = x0;
		this.y0 = y0;

		this.x1 = x1;
		this.y1 = y1;

		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;
		// System.out.println("x0:"+x0+" x1:"+x1);
		

		

		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {
				// generate e0 f0
				long[] ef0 = ef0(mt, x0, y0);
				final long e0 = ef0[0];
				final long f0 = ef0[1];
				
				sendP0(e0, f0);
				long[] ef1P0 = receiveP0();
				mulP0(mt, e0, f0, ef1P0[0], ef1P0[1]);
			}

		});

		exec.execute(new Runnable() {
			@Override
			public void run() {
				
				// generate e1 f1
				long[] ef1 = ef1(mt, x1, y1);
				final long e1 = ef1[0];
				final long f1 = ef1[1];
				sendP1(e1, f1);
				long[] ef0P1 = receiveP1();
				mulP1(mt, e1, f1, ef0P1[0], ef0P1[1]);
			}
		});

		// should be done with in 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(2, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();

				if (isDisconnect == true) {

					//System.out.println("SLB disconnecting...");
					rcvChannel.disconnectCli();
					sndChannel.disconnectServer();

				}
			}
		} catch (InterruptedException e) {
			// Something is wrong
			System.out.println("Unexpected interrupt");
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		
		bandwidth = (rcvChannel.cos.getByteCount() + rcvChannel.cis.getByteCount())/2;
		//System.out.println("engine bandwidth:"+bandwidth);
		//reset channel bandwidth
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
	}

	public void sendP0(long e0, long f0) {
		//System.out.println("[P0] e0:" + e0 + " f0:" + f0+" sndChannel:"+sndChannel);
		sndChannel.writeLong(e0);
		sndChannel.writeLong(f0);
		sndChannel.flush();

	}

	public long[] receiveP0() {
		long[] res = new long[2];
		res[0] = sndChannel.readLong();// e1
		res[1] = sndChannel.readLong();// f1
		sndChannel.flush();
		// System.out.println("[P0] e1:" + res[0] + " f1:" + res[1]);
		return res;
	}

	public long[] ef0(MultiplicationTriple mt, long x0, long y0) {
		long tripleA0 = mt.tripleA0;
		long tripleB0 = mt.tripleB0;
		// long tripleC0 = mt.tripleC0;
		long e0 = AdditiveUtil.sub(x0, tripleA0);// e0= x0 - a0
		long f0 = AdditiveUtil.sub(y0, tripleB0);// f0 = y0 - b0
		long[] res = new long[2];
		res[0] = e0;
		res[1] = f0;
		return res;
	}

	public void mulP0(MultiplicationTriple mt, long e0, long f0, long e1, long f1) {
		/*
		 * long tripleA0 = mt.tripleA0; long tripleB0 = mt.tripleB0; long tripleC0 =
		 * mt.tripleC0; long e0 = AdditiveUtil.sub(x0, tripleA0);//e0= x0 - a0 long f0 =
		 * AdditiveUtil.sub(y0, tripleB0);//f0 = y0 - b0
		 */

		long e = AdditiveUtil.add(e0, e1);
		long f = AdditiveUtil.add(f0, f1);
		// System.out.println("[P0] e:"+e+" f:"+f);

		this.z0 = AdditiveUtil.add(AdditiveUtil.add(AdditiveUtil.mul(f, mt.tripleA0), AdditiveUtil.mul(e, mt.tripleB0)),
				mt.tripleC0);
		// System.out.println("z0:"+z0);
		// return z0;
	}

	public void sendP1(long e1, long f1) {
		//System.out.println("[P1] e1:" + e1 + " f1:" + f1 + " rcvChannel:" + rcvChannel);
		rcvChannel.writeLong(e1);
		rcvChannel.writeLong(f1);
		rcvChannel.flush();

	}

	public long[] receiveP1() {
		long[] res = new long[2];
		res[0] = rcvChannel.readLong();// e0
		res[1] = rcvChannel.readLong();// f0
		rcvChannel.flush();
		// System.out.println("[P1] e0:" + res[0] + " f0:" + res[1]);
		return res;
	}

	public long[] ef1(MultiplicationTriple mt, long x1, long y1) {
		long tripleA1 = mt.tripleA1;
		long tripleB1 = mt.tripleB1;

		long e1 = AdditiveUtil.sub(x1, tripleA1);
		long f1 = AdditiveUtil.sub(y1, tripleB1);
		long[] res = new long[2];
		res[0] = e1;
		res[1] = f1;
		return res;
	}

	public void mulP1(MultiplicationTriple mt, long e1, long f1, long e0, long f0) {
		/*
		 * long tripleA1 = mt.tripleA1; long tripleB1 = mt.tripleB1;
		 * 
		 * long e1 = AdditiveUtil.sub(x1, tripleA1); long f1 = AdditiveUtil.sub(y1,
		 * tripleB1);
		 */

		/*
		 * long e0 = sndChannel.readLong(); long f0 = sndChannel.readLong();
		 * sndChannel.flush(); System.out.println("e0:"+e0+" f0:"+f0);
		 */
		long tripleC1 = mt.tripleC1;
		long e = AdditiveUtil.add(e0, e1);
		long f = AdditiveUtil.add(f0, f1);
		// System.out.println("[P1] e:"+e+" f:"+f);
		long ief = AdditiveUtil.mul(e, f);

		this.z1 = AdditiveUtil.add(ief, AdditiveUtil
				.add(AdditiveUtil.add(AdditiveUtil.mul(f, mt.tripleA1), AdditiveUtil.mul(e, mt.tripleB1)), tripleC1));
		// System.out.println("z1:"+z1);
		// return z1;
	}

	public static void main(String[] args) throws Exception {
		final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		final int port = 5552;
		// Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
		System.out.println("Connection| hostname:port, " + hostname + ":" + port);
		final Server sndChannel = new Server();
		final Client rcvChannel = new Client();

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
					rcvChannel.flush();

				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

			}

		});

		// should be done with in 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
			}
		} catch (InterruptedException e) {
			// Something is wrong
			System.out.println("Unexpected interrupt");
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);

		// generate points
		SecureRandom random;
		try {
			random = SecureRandom.getInstance("SHA1PRNG");
			ShareGenerator generator = new ShareGenerator();
			generator.generateSharedDataPointSet(random);
			long x0 = generator.x0;
			long x1 = generator.x1;
			generator.generateSharedDataPointSet(random);
			long y0 = generator.x0;
			long y1 = generator.x1;

			long timer1 = System.nanoTime();
			final SeqCompEngine engine = new SeqCompEngine(false, sndChannel, rcvChannel, mt, x0, y0, x1, y1);
			long timer2 = System.nanoTime();
			System.out.println("time:"+(timer2-timer1)/1e9+" s");
			long z = AdditiveUtil.add(engine.z0, engine.z1);
			// verify
			long x = AdditiveUtil.add(engine.x0, engine.x1);
			long y = AdditiveUtil.add(engine.y0, engine.y1);
			System.out.println("x:" + x + " y:" + y);
			long zVer = AdditiveUtil.mul(x, y);

			System.out.println("z:" + z + " zVer:" + zVer);

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sndChannel.disconnect();
		rcvChannel.disconnect();

	}
}
