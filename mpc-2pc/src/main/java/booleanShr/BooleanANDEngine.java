package booleanShr;

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

public class BooleanANDEngine {

	private Server sndChannel;
	private Client rcvChannel;
	public byte z0;
	public byte z1;
	public byte x0;
	public byte x1;
	public byte y0;
	public byte y1;

	public double bandwidth = 0;

	public long time = 0;

	public BooleanANDEngine(boolean isDisconnect, Server sndChannel, Client rcvChannel, final ANDTriple mt2, byte x0,
			byte y0, byte x1, byte y1) throws Exception {
		// System.out.println("engine...start..:"+isDisconnect);
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

				//long st1 = System.nanoTime();
				// generate e0 f0
				byte[] ef0 = ef0(mt2, x0, y0);
				final byte e0 = ef0[0];
				final byte f0 = ef0[1];
				//long et1 = System.nanoTime();
				//System.out.println("timer1 ef0:"+(et1-st1)+" nanos");

				long st2 = System.nanoTime();
				sendP0(e0, f0);
				byte[] ef1P0 = receiveP0();

				long et2 = System.nanoTime();
				time += et2 - st2;
				//System.out.println("timer2 network:"+(et2 - st2)+" nanos");

				//long st3 = System.nanoTime();
				mulP0(mt2, e0, f0, ef1P0[0], ef1P0[1]);
				//long et3 = System.nanoTime();
				//System.out.println("timer3 multi:"+(et3-st3)+" nanos");

			}

		});

		exec.execute(new Runnable() {
			@Override
			public void run() {
				// generate e1 f1
				byte[] ef1 = ef1(mt2, x1, y1);
				final byte e1 = ef1[0];
				final byte f1 = ef1[1];				

				sendP1(e1, f1);
				byte[] ef0P1 = receiveP1();				

				mulP1(mt2, e1, f1, ef0P1[0], ef0P1[1]);

			}
		});

		//long st4 = System.nanoTime();
		// should be done with in 1s
		exec.shutdown();		
		try {
			if (exec.awaitTermination(1, TimeUnit.SECONDS)) {
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
		//long et4 = System.nanoTime();
		//System.out.println("timer4 shutdown:"+(et4-st4)+" nanosec");

		//long st5 = System.nanoTime();
		// long type divide 2
		bandwidth = (rcvChannel.cos.getByteCount() + rcvChannel.cis.getByteCount());
		// System.out.println("bit engine bandwidth:"+bandwidth);
		// reset channel bandwidth
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		//long et5 = System.nanoTime();
		//System.out.println("timer5 count bandwidth:"+(et5-st5)+" nanos");
	}

	public void sendP0(byte e0, byte f0) {
		// System.out.println("[P0] e0:" + e0 + " f0:" + f0+" sndChannel:"+sndChannel);
		sndChannel.writeByte(e0);
		sndChannel.writeByte(f0);
		sndChannel.flush();

	}

	public byte[] receiveP0() {
		byte[] res = new byte[2];
		//res[0] = sndChannel.readBytes(1)[0];// e1
		//res[1] = sndChannel.readBytes(1)[0];// f1
		
		res = sndChannel.readBytes(2);			
		sndChannel.flush();
		//System.out.println("size teste1:"+teste1.length+" testf1:"+testf1.length);
		//System.out.println("[P0] e1:" + res[0] + " f1:" + res[1]);
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
		/*
		 * long tripleA0 = mt.tripleA0; long tripleB0 = mt.tripleB0; long tripleC0 =
		 * mt.tripleC0; long e0 = AdditiveUtil.sub(x0, tripleA0);//e0= x0 - a0 long f0 =
		 * AdditiveUtil.sub(y0, tripleB0);//f0 = y0 - b0
		 */

		byte e = BooleanUtil.xor(e0, e1);
		byte f = BooleanUtil.xor(f0, f1);
		// System.out.println("[P0] e:"+e+" f:"+f);

		this.z0 = BooleanUtil.xor(BooleanUtil.xor(BooleanUtil.and(f, mt.tripleA0), BooleanUtil.and(e, mt.tripleB0)),
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
		//System.out.println("e1 readLong:"+rcvChannel.readLong() );
		//res[0] = (byte) rcvChannel.readLong();// e0
		//res[1] = (byte) rcvChannel.readLong();// f0
		res = rcvChannel.readBytes(2);
		//res[0] =  rcvChannel.readBytes(1)[0];// e0
		//res[1] =  rcvChannel.readBytes(1)[0];// f0
		rcvChannel.flush();
		//System.out.println("[P1] e0:" + res[0] + " f0:" + res[1]);
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
		// long tripleC1 = mt.tripleC1;
		byte e = BooleanUtil.xor(e0, e1);
		byte f = BooleanUtil.xor(f0, f1);
		// System.out.println("[P1] e:"+e+" f:"+f);
		byte ief = BooleanUtil.and(e, f);

		this.z1 = BooleanUtil.xor(ief, BooleanUtil
				.xor(BooleanUtil.xor(BooleanUtil.and(f, mt.tripleA1), BooleanUtil.and(e, mt.tripleB1)), mt.tripleC1));

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

		
		
		BooleanShrGenerator generator = new BooleanShrGenerator(true);
		ANDTriple mt = new ANDTriple(generator); //dummy MT no ROT
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		// generate points
		SecureRandom random;
		try {
			random = SecureRandom.getInstance("SHA1PRNG");
			
			generator.generateSharedDataPointSet(random);
			byte x0 = generator.x0;
			byte x1 = generator.x1;
			generator.generateSharedDataPointSet(random);
			byte y0 = generator.x0;
			byte y1 = generator.x1;

			long timer1 = System.nanoTime();
			for(int i=0; i<1; i++) {
			final BooleanANDEngine engine = new BooleanANDEngine(false, sndChannel, rcvChannel, mt, x0, y0, x1, y1);
			}
			long timer2= System.nanoTime();
			System.out.println("time:"+ (timer2-timer1) +"  sec" );
			
			/*byte z = BooleanUtil.xor(engine.z0, engine.z1);
			
			// verify
			byte x = BooleanUtil.xor(engine.x0, engine.x1);
			byte y = BooleanUtil.xor(engine.y0, engine.y1);
			System.out.println("ver x:" + x +" x0:"+x0+" x1:"+x1 + " y:" + y +" y0:"+y0+" y1:"+y1);
			
			byte zVer = BooleanUtil.and(x, y);
			System.out.println("engine z:" + z + " zVer:" + zVer);

			System.out.println("bandwidth:" + engine.bandwidth + " Bytes");*/

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sndChannel.disconnect();
		rcvChannel.disconnect();

	}
}
