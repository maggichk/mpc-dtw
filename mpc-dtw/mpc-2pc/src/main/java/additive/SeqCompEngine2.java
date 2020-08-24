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

public class SeqCompEngine2 {

	private Server sndChannel;
	private Client rcvChannel;
	public long z0;
	public long z1;
	public long x0;
	public long x1;
	public long y0;
	public long y1;

	public double bandwidth=0;
	
	public long time=0;
	
	public SeqCompEngine2(boolean isDisconnect, Server sndChannel, Client rcvChannel, final MultiplicationTriple2 mt,
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
				
				long st2= System.nanoTime();
				// generate e0 f0
				long[] ef0 = ef0(mt, x0, y0);
				final long e0 = ef0[0];
				final long f0 = ef0[1];
				
				sendP0(e0, f0);
				long[] ef1P0 = receiveP0();
				
				
				long et2 = System.nanoTime();
				time += et2 - st2;
				
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
				
				long st3 = System.nanoTime();
								
				sendP1(e1, f1);
				long[] ef0P1 = receiveP1();
				
				
				long et3 = System.nanoTime();
				time += et3 - st3;
				
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
		
		//long type divide 2
		bandwidth = (rcvChannel.cos.getByteCount() + rcvChannel.cis.getByteCount())/2;
		//System.out.println("bit engine bandwidth:"+bandwidth);
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

	public long[] ef0(MultiplicationTriple2 mt, long x0, long y0) {
		long tripleA0 = mt.tripleA0;
		long tripleB0 = mt.tripleB0;
		// long tripleC0 = mt.tripleC0;
		long e0 = AdditiveUtil2.sub(x0, tripleA0);// e0= x0 - a0
		long f0 = AdditiveUtil2.sub(y0, tripleB0);// f0 = y0 - b0
		long[] res = new long[2];
		res[0] = e0;
		res[1] = f0;
		return res;
	}

	public void mulP0(MultiplicationTriple2 mt, long e0, long f0, long e1, long f1) {
		/*
		 * long tripleA0 = mt.tripleA0; long tripleB0 = mt.tripleB0; long tripleC0 =
		 * mt.tripleC0; long e0 = AdditiveUtil.sub(x0, tripleA0);//e0= x0 - a0 long f0 =
		 * AdditiveUtil.sub(y0, tripleB0);//f0 = y0 - b0
		 */

		long e = AdditiveUtil2.add(e0, e1);
		long f = AdditiveUtil2.add(f0, f1);
		// System.out.println("[P0] e:"+e+" f:"+f);

		/*this.z0 = AdditiveUtil2.add(AdditiveUtil2.add(AdditiveUtil2.mul(f, mt.tripleA0), AdditiveUtil2.mul(e, mt.tripleB0)),
				mt.tripleC0);*/
		this.z0 = Math.floorMod(f*mt.tripleA0+e*mt.tripleB0+mt.tripleC0, 2);
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

	public long[] ef1(MultiplicationTriple2 mt, long x1, long y1) {
		long tripleA1 = mt.tripleA1;
		long tripleB1 = mt.tripleB1;

		long e1 = AdditiveUtil2.sub(x1, tripleA1);
		long f1 = AdditiveUtil2.sub(y1, tripleB1);
		long[] res = new long[2];
		res[0] = e1;
		res[1] = f1;
		return res;
	}

	public void mulP1(MultiplicationTriple2 mt, long e1, long f1, long e0, long f0) {
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
		//long tripleC1 = mt.tripleC1;
		long e = AdditiveUtil2.add(e0, e1);
		long f = AdditiveUtil2.add(f0, f1);
		// System.out.println("[P1] e:"+e+" f:"+f);
		long ief = AdditiveUtil2.mul(e, f);

		/*this.z1 = AdditiveUtil2.add(ief, AdditiveUtil2
				.add(AdditiveUtil2.add(AdditiveUtil2.mul(f, mt.tripleA1), AdditiveUtil2.mul(e, mt.tripleB1)), tripleC1));*/
		this.z1 = Math.floorMod(ief + f*mt.tripleA1+e*mt.tripleB1 + mt.tripleC1, 2);
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

		MultiplicationTriple2 mt = new MultiplicationTriple2(sndChannel, rcvChannel);
		//long bandwidthMT = rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

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

			final SeqCompEngine2 engine = new SeqCompEngine2(false, sndChannel, rcvChannel, mt, x0, y0, x1, y1);
			long z = AdditiveUtil2.add(engine.z0, engine.z1);
			// verify
			long x = AdditiveUtil2.add(engine.x0, engine.x1);
			long y = AdditiveUtil2.add(engine.y0, engine.y1);
			System.out.println("x:" + x + " y:" + y);
			long zVer = AdditiveUtil2.mul(x, y);

			System.out.println("z:" + z + " zVer:" + zVer);
			
			System.out.println("bandwidth:"+ engine.bandwidth+" Bytes");

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sndChannel.disconnect();
		rcvChannel.disconnect();

	}
}
