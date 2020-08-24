package booleanShr;

import flexSC.network.Client;
import flexSC.network.Server;
import flexSC.util.Utils;
import org.bouncycastle.util.Arrays;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BooleanANDEngineBatchEightBit {

	private Server sndChannel;
	private Client rcvChannel;
	public byte[] z0;
	public byte[] z1;
	public byte[] x0;
	public byte[] x1;
	public byte[] y0;
	public byte[] y1;

	public byte[] getZ0() {
		return z0;
	}

	public void setZ0(byte[] z0) {
		this.z0 = Arrays.clone(z0);
	}

	public byte[] getZ1() {
		return z1;
	}

	public void setZ1(byte[] z1) {
		this.z1 = Arrays.clone(z1);
	}

	public byte[] getX0() {
		return x0;
	}

	public void setX0(byte[] x0) {
		this.x0 = x0;
	}

	public byte[] getX1() {
		return x1;
	}

	public void setX1(byte[] x1) {
		this.x1 = Arrays.clone(x1);
	}

	public byte[] getY0() {
		return y0;
	}

	public void setY0(byte[] y0) {
		this.y0 = Arrays.clone(y0);
	}

	public byte[] getY1() {
		return y1;
	}

	public void setY1(byte[] y1) {
		this.y1 = y1;
	}

	public double bandwidth = 0;

	public long time = 0;

	public BooleanANDEngineBatchEightBit(boolean isDisconnect, Server sndChannel, Client rcvChannel, final ANDTriple mt, byte[] x0,
                                         byte[] y0, byte[] x1, byte[] y1) throws Exception {
		// System.out.println("engine...start..:"+isDisconnect);
		this.setX0(x0);
		this.setX1(x1);
		this.setY0(y0);
		this.setY1(y1);
		this.z0 = new byte[15];
		this.z1 = new byte[15];
		
		this.sndChannel = sndChannel;
		this.rcvChannel = rcvChannel;
		// System.out.println("x0:"+x0+" x1:"+x1);

		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {

				
				
				
				byte[] e0f0Arr = new byte[30];
				
				for(int i=0; i<15; i++) {
					// generate e0 f0
					byte[] ef0 = ef0(mt, x0[i], y0[i]);
					e0f0Arr[2*i] = ef0[0];//e0
					e0f0Arr[2*i+1] = ef0[1];//f0
				}
				
				long st2 = System.nanoTime();
				sendP0(e0f0Arr);
				byte[] ef1P0 = receiveP0();

				long et2 = System.nanoTime();
				time += et2 - st2;
				//System.out.println("time send:"+time);
				
				for(int i=0; i<15; i++) {
					mulP0(mt, e0f0Arr[2*i], e0f0Arr[2*i+1], ef1P0[2*i], ef1P0[2*i+1],i);
				}

			}

		});

		exec.execute(new Runnable() {
			@Override
			public void run() {
				
				byte[] e1f1Arr = new byte[30];
				for(int i=0; i<15; i++) {
					// generate e1 f1
					byte[] ef1 = ef1(mt, x1[i], y1[i]);
					e1f1Arr[2*i] = ef1[0];
					e1f1Arr[2*i+1] = ef1[1];
				}
				
				
				sendP1(e1f1Arr);
				byte[] ef0P1 = receiveP1();


				for(int i=0; i<15; i++) {
					mulP1(mt, e1f1Arr[2*i], e1f1Arr[2*i+1], ef0P1[2*i], ef0P1[2*i+1],i);
				}
				
				

			}
		});

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

		long st1 = System.nanoTime();
		// long type divide 2
		bandwidth = (rcvChannel.cos.getByteCount() + rcvChannel.cis.getByteCount());
		// System.out.println("bit engine bandwidth:"+bandwidth);
		// reset channel bandwidth
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();
		long et1 = System.nanoTime();
		time += et1 - st1;
	}

	public void sendP0(byte[] e0f0Arr) {
		// System.out.println("[P0] e0:" + e0 + " f0:" + f0+" sndChannel:"+sndChannel);
		//sndChannel.writeByte(e0);
		sndChannel.writeByte(e0f0Arr,30);
		sndChannel.flush();

	}

	public byte[] receiveP0() {
		byte[] res = new byte[30];
		//res[0] = sndChannel.readBytes(1)[0];// e1
		//res[1] = sndChannel.readBytes(1)[0];// f1
		
		res = sndChannel.readBytes(30);
		sndChannel.flush();
		//System.out.println(res.length);
		//System.out.println("size teste1:"+teste1.length+" testf1:"+testf1.length);
		//System.out.println("[P0] e1:" + res[0] + " f1:" + res[1]);
		return res;
	}

	public byte[] ef0(ANDTriple mt, byte x0, byte y0) {
		byte tripleA0 = mt.tripleA0;
		byte tripleB0 = mt.tripleB0;
		// long tripleC0 = mt.tripleC0;
		byte e0 = BooleanUtil.xor(x0, tripleA0);// e0= x0 xor a0
		byte f0 = BooleanUtil.xor(y0, tripleB0);// f0 = y0 xor b0
		byte[] res = new byte[2];
		res[0] = e0;
		res[1] = f0;
		return res;
	}

	public void mulP0(ANDTriple mt, byte e0, byte f0, byte e1, byte f1, int index) {
		/*
		 * long tripleA0 = mt.tripleA0; long tripleB0 = mt.tripleB0; long tripleC0 =
		 * mt.tripleC0; long e0 = AdditiveUtil.sub(x0, tripleA0);//e0= x0 - a0 long f0 =
		 * AdditiveUtil.sub(y0, tripleB0);//f0 = y0 - b0
		 */

		byte e = BooleanUtil.xor(e0, e1);
		byte f = BooleanUtil.xor(f0, f1);
		// System.out.println("[P0] e:"+e+" f:"+f);

		this.z0[index] = BooleanUtil.xor(BooleanUtil.xor(BooleanUtil.and(f, mt.tripleA0), BooleanUtil.and(e, mt.tripleB0)),
				mt.tripleC0);

	}

	public void sendP1(byte[] e1f1Arr) {
		// System.out.println("[P1] e1:" + e1 + " f1:" + f1 + " rcvChannel:" +
		// rcvChannel);
		//rcvChannel.writeByte(e1); 
		//rcvChannel.writeByte(f1);
		rcvChannel.writeByte(e1f1Arr,14);
		rcvChannel.flush();

	}

	public byte[] receiveP1() {
		byte[] res = new byte[30];
		//System.out.println("e1 readLong:"+rcvChannel.readLong() );
		//res[0] = (byte) rcvChannel.readLong();// e0
		//res[1] = (byte) rcvChannel.readLong();// f0
		res = rcvChannel.readBytes(30);
		rcvChannel.flush();
		
		//res[0] =  rcvChannel.readBytes(1)[0];// e0
		//res[1] =  rcvChannel.readBytes(1)[0];// f0
		
		//System.out.println("[P1] e0:" + res[0] + " f0:" + res[1]);
		return res;
	}

	public byte[] ef1(ANDTriple mt, byte x1, byte y1) {
		byte tripleA1 = mt.tripleA1;
		byte tripleB1 = mt.tripleB1;

		byte e1 = BooleanUtil.xor(x1, tripleA1);//x1 xor A1
		byte f1 = BooleanUtil.xor(y1, tripleB1);//y1 xor B1
		byte[] res = new byte[2];
		res[0] = e1;
		res[1] = f1;
		return res;
	}

	public void mulP1(ANDTriple mt, byte e1, byte f1, byte e0, byte f0, int index) {
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

		this.z1[index] = BooleanUtil.xor(ief, BooleanUtil
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
		System.out.println("tripleA0:"+mt.tripleA0 +" tripleA1:"+mt.tripleA1+" tripleB0:"+mt.tripleB0+
				" tripleB1:"+mt.tripleB1+" tripleC0:"+mt.tripleC0+" tripleC1:"+mt.tripleC1);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		// generate points
		
		try {
			

			// generate two shares
			
			byte z = 3;//1100000000...
			byte[] zBitArr = Utils.fromByte2byteRightmost(z, 7);
			byte[] z0BitArr = new byte[7];
			byte[] z1BitArr = new byte[7];//z0 AND z1
			for(int i=0; i< 7; i++) {
				generator.generateSharedDataPoint(zBitArr[i], true);
				z0BitArr[i] = generator.x0;
				z1BitArr[i] = generator.x1;
			}
			
			for(int i=0; i<7; i++) {
				System.out.print(z0BitArr[i]);;
			}
			System.out.println("--------z0-----------");
			for(int i=0; i<7; i++) {
				System.out.print(z1BitArr[i]);;
			}
			System.out.println("--------z1-----------");
			for(int i=0; i<7;i++) {
				System.out.print(BooleanUtil.xor(z0BitArr[i], z1BitArr[i]));
			}
			System.out.println("verify");
			
			byte[] ARR_0 = {
					 0, 0, 0, 0, 0, 0, 0};

			long timer1 = System.nanoTime();
			//for(int i=0; i<1; i++) {
			BooleanANDEngineBatchEightBit engineBatch = new BooleanANDEngineBatchEightBit(false, sndChannel, rcvChannel,
					mt, z0BitArr, ARR_0, ARR_0, z1BitArr);
			//}
			long timer2= System.nanoTime();			
			System.out.println("time:"+ (timer2-timer1)  +"  nanosec" );
			
			byte[] d0BitArr = engineBatch.z0;
			byte[] d1BitArr = engineBatch.z1;
			
			for(int i=0; i<7; i++) {
				System.out.print(BooleanUtil.and(z0BitArr[i], z1BitArr[i]));
			}
			System.out.println("----------Z0 AND Z1-------");
			
			for(int i=0 ; i<7; i++) {
				System.out.print(BooleanUtil.xor(d0BitArr[i], d1BitArr[i]));
			}
			System.out.println("--------verify engine--------");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sndChannel.disconnect();
		rcvChannel.disconnect();

	}

	
}
