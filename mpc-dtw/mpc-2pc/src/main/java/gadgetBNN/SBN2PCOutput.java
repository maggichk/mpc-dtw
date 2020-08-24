package gadgetBNN;

import additive.*;
import eightBitAdditive.*;
import booleanShr.*;
import flexSC.network.Client;
import flexSC.network.Server;
import flexSC.util.Utils;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SBN2PCOutput {

	private Server sndChannel;
	private Client rcvChannel;
	public byte z0AND;
	public byte z1AND;

	public byte z0l = 0;
	public byte z1l = 0;

	public double bandwidth = 0.0;
	public double timeNetwork = 0.0;

	

	
	public long[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple mt,
			long x_0,  long ep1_0, long ep2_0, long x_1, long ep1_1, long ep2_1, int factor) throws Exception {
		long z[] = new long[2];
		
		
		
		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;

		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {
				
				
				
				//x_0 * ep1_0 
				MulP0 mulP0 = new MulP0();
				mulP0.compute(isDisconnect, sndChannel, mt, x_0, ep1_0);
				timeNetwork += mulP0.time;
				
				long zq_mul_0 = AdditiveUtil.add(mulP0.z0 , ep2_0);
				//short z_mul_0= (short) zq_mul_0;

				
				
				z[0] = zq_mul_0;

			}

		});

		exec.execute(new Runnable() {
			@Override
			public void run() {
				
				
				//x_1 * ep1_1 
				
				MulP1 mulP1= new MulP1();
				mulP1.compute(isDisconnect, rcvChannel, mt, x_1, ep1_1);
				
				
				long zq_mul_1 = AdditiveUtil.add(mulP1.z1 , ep2_1);
				//short z_mul_1 = (short)zq_mul_1;

				
				
				z[1] = zq_mul_1;

				
			}
		});

		// long st4 = System.nanoTime();
		
		
		// should be done with in 1s
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();

				if (isDisconnect == true) {

					// System.out.println("SLB disconnecting...");
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
		
		double st1 = System.nanoTime();
		this.bandwidth += rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		
		double et1 = System.nanoTime();
		timeNetwork += et1-st1;
		

		return z;
	}

/*	public void sendP0(byte e0, byte f0) {
		// System.out.println("[P0] e0:" + e0 + " f0:" + f0+" sndChannel:"+sndChannel);
		sndChannel.writeByte(e0);
		sndChannel.writeByte(f0);
		sndChannel.flush();

	}

	public byte[] receiveP0() {
		byte[] res = new byte[2];
		// res[0] = sndChannel.readBytes(1)[0];// e1
		// res[1] = sndChannel.readBytes(1)[0];// f1

		res = sndChannel.readBytes(2);
		sndChannel.flush();
		// System.out.println("size teste1:"+teste1.length+" testf1:"+testf1.length);
		// System.out.println("[P0] e1:" + res[0] + " f1:" + res[1]);
		return res;
	}

	public byte[] ef0(ANDTriple mt, byte x0, byte y0) {
		byte tripleA0 = mt.tripleA0;
		byte tripleB0 = mt.tripleB0;
		// long tripleC0 = mt.tripleC0;
		byte e0 = BooleanUtil.xor(x0, tripleA0);// e0= x0 - a0
		byte f0 = BooleanUtil.xor(y0, tripleB0);// f0 = y0 - b0
		byte[] res = new byte[2];
		res[0] = e0;
		res[1] = f0;
		return res;
	}

	public void mulP0(ANDTriple mt, byte e0, byte f0, byte e1, byte f1) {
		
		 * long tripleA0 = mt.tripleA0; long tripleB0 = mt.tripleB0; long tripleC0 =
		 * mt.tripleC0; long e0 = AdditiveUtil.sub(x0, tripleA0);//e0= x0 - a0 long f0 =
		 * AdditiveUtil.sub(y0, tripleB0);//f0 = y0 - b0
		 

		byte e = BooleanUtil.xor(e0, e1);
		byte f = BooleanUtil.xor(f0, f1);
		// System.out.println("[P0] e:"+e+" f:"+f);

		this.z0AND = BooleanUtil.xor(BooleanUtil.xor(BooleanUtil.and(f, mt.tripleA0), BooleanUtil.and(e, mt.tripleB0)),
				mt.tripleC0);

	}

	public void sendP1(byte e1, byte f1) {
		// System.out.println("[P1] e1:" + e1 + " f1:" + f1 + " rcvChannel:" +
		// rcvChannel);
		rcvChannel.writeByte(e1);
		rcvChannel.writeByte(f1);
		rcvChannel.flush();

	}

	public byte[] receiveP1() {
		byte[] res = new byte[2];
		// System.out.println("e1 readLong:"+rcvChannel.readLong() );
		// res[0] = (byte) rcvChannel.readLong();// e0
		// res[1] = (byte) rcvChannel.readLong();// f0
		res = rcvChannel.readBytes(2);
		// res[0] = rcvChannel.readBytes(1)[0];// e0
		// res[1] = rcvChannel.readBytes(1)[0];// f0
		rcvChannel.flush();
		// System.out.println("[P1] e0:" + res[0] + " f0:" + res[1]);
		return res;
	}

	public byte[] ef1(ANDTriple mt, byte x1, byte y1) {
		byte tripleA1 = mt.tripleA1;
		byte tripleB1 = mt.tripleB1;

		byte e1 = BooleanUtil.xor(x1, tripleA1);
		byte f1 = BooleanUtil.xor(y1, tripleB1);
		byte[] res = new byte[2];
		res[0] = e1;
		res[1] = f1;
		return res;
	}

	public void mulP1(ANDTriple mt, byte e1, byte f1, byte e0, byte f0) {
		
		 * long tripleA1 = mt.tripleA1; long tripleB1 = mt.tripleB1;
		 * 
		 * long e1 = AdditiveUtil.sub(x1, tripleA1); long f1 = AdditiveUtil.sub(y1,
		 * tripleB1);
		 

		
		 * long e0 = sndChannel.readLong(); long f0 = sndChannel.readLong();
		 * sndChannel.flush(); System.out.println("e0:"+e0+" f0:"+f0);
		 
		// long tripleC1 = mt.tripleC1;
		byte e = BooleanUtil.xor(e0, e1);
		byte f = BooleanUtil.xor(f0, f1);
		// System.out.println("[P1] e:"+e+" f:"+f);
		byte ief = BooleanUtil.and(e, f);

		this.z1AND = BooleanUtil.xor(ief, BooleanUtil
				.xor(BooleanUtil.xor(BooleanUtil.and(f, mt.tripleA1), BooleanUtil.and(e, mt.tripleB1)), mt.tripleC1));

	}*/

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

		

		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		//eightBitMultiplicationTriple mt = new eightBitMultiplicationTriple(sndChannel, rcvChannel);//2^31-1 31bit
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		//eightBitShareGenerator generator = new eightBitShareGenerator(true);
		ShareGenerator generator = new ShareGenerator(true);
		
		
		//scaling factor of ep1 ep2
		int factor = 1000000;//10^6
		
		
		// generate two shares
		//long ep1 = (long) (0.5*factor);// -> 0.5*2^14 = 0.5*16384
		//long ep2 = (long) (-0.5*factor);// -> 0.5*2^14 = 0.5*16384
		//long x = 3;//(3 * 0.5 - 0.5)=1 
		
		long ep1=(long) (0.5*factor);
		long ep2 =(long) (0.5*factor);
		long x = -2;//

		generator.generateSharedDataPoint(ep1, true);
		long ep1_0 = generator.x0;
		long ep1_1 = generator.x1;

		System.out.println("ep1:" + ep1 + " ep1_0:" + ep1_0 + " ep1_1:" + ep1_1 + " verify:" + AdditiveUtil.add(ep1_0,ep1_1 ));

		generator.generateSharedDataPoint(ep2, true);
		long ep2_0 = generator.x0;
		long ep2_1 = generator.x1;

		System.out.println("ep2:" + ep2 + " ep2_0:" + ep2_0 + " ep2_1:" + ep2_1 + " verify:" + AdditiveUtil.add(ep2_0, ep2_1));
		
		generator.generateSharedDataPoint(x, true);
		long x_0 = generator.x0;
		long x_1 = generator.x1;

		System.out.println("x:" + x + " x0:" + x_0 + " x_1:" + x_1 + " verify:" + AdditiveUtil.add(x_0, x_1));

		
		
		
		SBN2PCOutput bn = new SBN2PCOutput();

		int round = 1;
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {			
			//extract MSB of a
			long[] z = bn.compute(false, sndChannel, rcvChannel, mt, x_0,ep1_0,ep2_0,x_1,ep1_1,ep2_1,factor);
			// verify
			long z0 = z[0];
			long z1 = z[1];
			System.out.println("z:" +AdditiveUtil.add(z0, z1) + " z0:" + z0 + " z1:" + z1);
			System.out.println("z scale back:"+AdditiveUtil.add(z0, z1)/factor);

			if (i % 1000 == 0) {
				System.out.println("Progress:" + i);
			}
		}
		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + bn.bandwidth / 1024.0 + " KB");
		System.out.println("timeNetwork:" + bn.timeNetwork / 1e9 + " seconds");
		
		System.out.println("no network delay time:" + (time-bn.timeNetwork) / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

}
