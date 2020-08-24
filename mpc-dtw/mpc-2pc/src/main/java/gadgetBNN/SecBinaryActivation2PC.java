package gadgetBNN;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MulP0;
import additive.MulP1;
import additive.MultiplicationTriple;
import additive.SeqCompEngine;
import additive.ShareGenerator;
import booleanShr.ANDP0;
import booleanShr.ANDP1;
import booleanShr.ANDTriple;
import booleanShr.BooleanANDEngine;
import booleanShr.BooleanANDEngineBatch;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import common.util.Config;
import common.util.Constants;
import eightBitAdditive.eightBitAdditiveUtil;
import flexSC.network.Client;
import flexSC.network.Server;
import flexSC.util.Utils;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

public class SecBinaryActivation2PC {

	private Server sndChannel;
	private Client rcvChannel;
	public byte z0AND;
	public byte z1AND;

	public byte z0l = 0;
	public byte z1l = 0;

	public double bandwidth = 0.0;
	public double timeNetwork = 0.0;

	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0 };

	// output = sign(ep1) xnor sign(x+ ep2/ep1) = byte[zeta] xnor byte[MSB(x+ep)]
	public byte[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			MultiplicationTriple mt, long x_0, long x_1, long ep_0, long ep_1, byte zeta_0, byte zeta_1) throws Exception {
		byte z[] = new byte[2];
		//long z0 = AdditiveUtil.sub(y0, x0);
		//long z1 = AdditiveUtil.sub(y1, x1);
		
		// step 1: x+ep
		long z0 = AdditiveUtil.add(x_0, ep_0);
		long z1 = AdditiveUtil.add(x_1, ep_1);
		
		
		//bit-decomposition, from arithmetic shares to the binary strings
		byte[] z0BitArr = Utils.fromLong2byteRightmost(z0, 31);
		byte[] z1BitArr = Utils.fromLong2byteRightmost(z1, 31);

		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;

		BooleanANDEngineBatch engineBatch = new BooleanANDEngineBatch(isDisconnect, sndChannel, rcvChannel, mt2,
				z0BitArr, ARR_0, ARR_0, z1BitArr);
		byte[] d0BitArr = engineBatch.z0;
		byte[] d1BitArr = engineBatch.z1;
		this.bandwidth += engineBatch.bandwidth;
		timeNetwork += engineBatch.time;

		byte[] c0BitArr = new byte[31];
		byte[] c1BitArr = new byte[31];

		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {
				
				//Boolean AND operation
				ANDP0 andP0 = new ANDP0();
				for (int i = 0; i < 31; i++) {

					byte w0i = z0BitArr[i];
					if (i == 0) {

						c0BitArr[0] = d0BitArr[0];

					} else if (i == 30) {

						z0l = BooleanUtil.xor(w0i, c0BitArr[i - 1]);// Arr[29]

					} else {

						d0BitArr[i] = BooleanUtil.xor(d0BitArr[i], (byte) 1);
						byte d0i = d0BitArr[i];
						byte c0is1 = c0BitArr[i - 1];
						// BooleanANDEngine(mt2, w0i, c0is1, w1i, c1is1);
						// generate e0 f0
						/*byte[] ef0 = ef0(mt2, w0i, c0is1);// w0i AND c0is1
						final byte e0 = ef0[0];
						final byte f0 = ef0[1];
						sendP0(e0, f0);
						byte[] ef1P0 = receiveP0();
						mulP0(mt2, e0, f0, ef1P0[0], ef1P0[1]);*/
						
						andP0.compute(isDisconnect, sndChannel, mt2, w0i, c0is1);
						z0AND = andP0.z0;
						timeNetwork += andP0.time;

						byte e0i = BooleanUtil.xor(z0AND, (byte) 1);
						// BooleanANDEngine(mt2, e0i, d0i,e1i, d1i);
						// generate e0 f0
						/*byte[] ef0_1 = ef0(mt2, e0i, d0i);// e0i AND d0i
						final byte e0_1 = ef0_1[0];
						final byte f0_1 = ef0_1[1];
						sendP0(e0_1, f0_1);
						byte[] ef1P0_1 = receiveP0();
						mulP0(mt2, e0_1, f0_1, ef1P0_1[0], ef1P0_1[1]);*/
						
						andP0.compute(isDisconnect, sndChannel, mt2, e0i, d0i);
						z0AND = andP0.z0;
						timeNetwork += andP0.time;
						
						byte c0i = BooleanUtil.xor(z0AND, (byte) 1);
						c0BitArr[i] = c0i;
					}

				}
				//convert z0l to sign bit
				/*z0l = BooleanUtil.xor(z0l, (byte) 1);
				
				// convert back to Z31
				long t1_0 = z0l;
				long t2_0 = 0;
				//SeqCompEngine( mt, t1_0, t2_0, t1_1, t2_1); t1_0 mul t2_0
				MulP0 mulP0 = new MulP0();
				mulP0.compute(isDisconnect, sndChannel, mt, t1_0, t2_0);
				long zMUL1 = mulP0.z0;

				long z0lAddi = AdditiveUtil.sub(AdditiveUtil.add(t1_0, t2_0), AdditiveUtil.mul(2L, zMUL1));
				
				z[0] = z0lAddi;*/
				
				
				
				// sign(x+ ep2/ep1) - convert z0l to sign bit
				byte signbit = BooleanUtil.xor(z0l, (byte) 1);
				
				// xnor zeta
				z0l = BooleanUtil.xor(BooleanUtil.xor(signbit, zeta_0), (byte)1);
				
				
				z[0] = z0l;

			}

		});

		exec.execute(new Runnable() {
			@Override
			public void run() {
				ANDP1 andP1 = new ANDP1();
				
				for (int i = 0; i < 31; i++) {

					byte w1i = z1BitArr[i];

					if (i == 0) {

						c1BitArr[0] = d1BitArr[0];

					} else if (i == 30) {

						z1l = BooleanUtil.xor(w1i, c1BitArr[i - 1]);

					} else {

						byte d1i = d1BitArr[i];

						byte c1is1 = c1BitArr[i - 1];
						// BooleanANDEngine(mt2, w0i, c0is1, w1i, c1is1);
						// generate e1 f1
						/*byte[] ef1 = ef1(mt2, w1i, c1is1);// w1i AND c1is1
						final byte e1 = ef1[0];
						final byte f1 = ef1[1];
						sendP1(e1, f1);
						byte[] ef0P1 = receiveP1();
						mulP1(mt2, e1, f1, ef0P1[0], ef0P1[1]);*/
						
						andP1.compute(isDisconnect, rcvChannel, mt2, w1i, c1is1);
						z1AND = andP1.z1;
						

						byte e1i = z1AND;
						// BooleanANDEngine( mt2, e0i, d0i,e1i, d1i);
						// generate e1 f1
						/*byte[] ef1_1 = ef1(mt2, e1i, d1i);// e1i AND d1i
						final byte e1_1 = ef1_1[0];
						final byte f1_1 = ef1_1[1];
						sendP1(e1_1, f1_1);
						byte[] ef0P1_1 = receiveP1();
						mulP1(mt2, e1_1, f1_1, ef0P1_1[0], ef0P1_1[1]);*/
						
						andP1.compute(isDisconnect, rcvChannel, mt2, e1i, d1i);
						z1AND = andP1.z1;
						

						byte c1i = z1AND;
						c1BitArr[i] = c1i;
					}

				}
				
				
				
				/*// convert back to Z31
				long t1_1 = 0;
				long t2_1 = z1l;
				//SeqCompEngine(mt, t1_0, t2_0, t1_1, t2_1); t1_1 mul t2_1
				MulP1 mulP1 = new MulP1();
				mulP1.compute(isDisconnect, rcvChannel, mt, t1_1, t2_1);
				long zMUL1 = mulP1.z1;
				long z1lAddi = AdditiveUtil.sub(AdditiveUtil.add(t1_1, t2_1), AdditiveUtil.mul(2L, zMUL1));
				z[1] = z1lAddi;*/
				
				
				//signbit - z1l xor 0 = z1l
				//signbit xnor zeta = (signbit xor zeta) xor 0 = signbit xor zeta
				z1l = BooleanUtil.xor(zeta_1, z1l);
				
				
				z[1] = z1l;

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
		this.bandwidth += rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		double et1 = System.nanoTime();
		timeNetwork += et1 - st1;
		

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

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ANDTriple mt2 = new ANDTriple(boolGen);
		System.out.println("mt2 A0:" + mt2.tripleA0 + " A1:" + mt2.tripleA1 + " B0:" + mt2.tripleB0 + " B1:"
				+ mt2.tripleB1 + " C0:" + mt2.tripleC0 + " C1:" + mt2.tripleC1);

		MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		ShareGenerator generator = new ShareGenerator(true);

		// generate two shares
		long x = -2;//sign(2)=1; sign(-2)=0.
		long ep = -10;
		byte zeta = 0;//positive

		generator.generateSharedDataPoint(x, true);
		long x_0 = generator.x0;
		long x_1 = generator.x1;

		System.out.println("x:" + x + " x_0:" + x_0 + " x_1:" + x_1 + " verify:" + AdditiveUtil.add(x_0, x_1));

		generator.generateSharedDataPoint(ep, true);
		long ep_0 = generator.x0;
		long ep_1 = generator.x1;

		System.out.println("ep:" + ep + " ep_0:" + ep_0 + " ep_1:" + ep_1 + " verify:" + AdditiveUtil.add(ep_0, ep_1));
		
		boolGen.generateSharedDataPoint(zeta, true);
		byte zeta_0 = boolGen.x0;
		byte zeta_1 = boolGen.x1;

		SecBinaryActivation2PC sign = new SecBinaryActivation2PC();// y, u
		int round = 1;
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {			
			//extract MSB of a
			byte[] z = sign.compute(false, sndChannel, rcvChannel, mt2, mt, x_0, x_1, ep_0, ep_1, zeta_0, zeta_1);
			
			//expect: z = zeta * !MSB(x+ep)
			// verify
			byte z0 = z[0];
			byte z1 = z[1];
			System.out.println("z:" + BooleanUtil.xor(z0, z1) + " z0:" + z0 + " z1:" + z1);

			if (i % 1000 == 0) {
				System.out.println("Progress:" + i);
			}
		}
		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + sign.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		System.out.println("timeNetwork:" + sign.timeNetwork / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

}
