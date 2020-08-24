package eightBitAdditive;

import additive.AdditiveUtil;
import common.parser.WriteFile;
import common.util.Config;
import common.util.Constants;
import flexSC.flexsc.Flag;
import flexSC.gc.GCSignal;
import flexSC.network.Client;
import flexSC.network.Network;
import flexSC.network.Server;
import flexSC.ot.OTExtReceiver;
import flexSC.ot.OTExtSender;
import flexSC.ot.OTReceiver;
import flexSC.ot.OTSender;
import flexSC.util.Utils;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class eightBitMultiplicationTriple {

	public short tripleA0, tripleA1;
	public short tripleB0, tripleB1;
	public short tripleC0, tripleC1; // A * B = C

	private OTSender snd;
	private OTReceiver rcv;

	private short tripleU0, tripleU1;
	private short tripleV0, tripleV1;

	private eightBitShareGenerator shrGen;
	private SecureRandom random;

	private static double elapsedTimeTotal;
	//private double startTime;

	public eightBitMultiplicationTriple() {

	}

	public eightBitMultiplicationTriple(short tripleA0, short tripleA1, short tripleB0, short tripleB1, short tripleC0, short tripleC1) {
		this.tripleA0 = tripleA0;
		this.tripleA1 = tripleA1;
		this.tripleB0 = tripleB0;
		this.tripleB1 = tripleB1;
		this.tripleC0 = tripleC0;
		this.tripleC1 = tripleC1;
	}


	public eightBitMultiplicationTriple(eightBitShareGenerator shrGen, final Network sndChannel, final Network rcvChannel) {
		this.shrGen = shrGen;
		this.random = shrGen.random;
		this.generateSharedAB(random);

		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {
			@Override
			public void run() {

				setSnd(sndChannel);

				generateU0();
				sndChannel.flush();
				generateV0();
				sndChannel.flush();

			}
		});

		exec.execute(new Runnable() {

			@Override
			public void run() {

				setRcv(rcvChannel);

				generateU1();
				rcvChannel.flush();
				generateV1();
				rcvChannel.flush();

			}

		});


		// Create OT instance
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

		generateTripleC();
	}

	public eightBitMultiplicationTriple(boolean isCloseSocket, eightBitShareGenerator shrGen, final Server sndChannel, final Client rcvChannel) {
		this.shrGen = shrGen;
		this.random = shrGen.random;
		this.generateSharedAB(random);		

		ExecutorService exec = Executors.newFixedThreadPool(2); 
		exec.execute(new Runnable() {
			@Override
			public void run() {

				setSnd(sndChannel);				
				generateU0();
				sndChannel.flush();
				generateV0();
				sndChannel.flush();

			}
		});

		exec.execute(new Runnable() {

			@Override
			public void run() {

				setRcv(rcvChannel);
				
				generateU1();
				rcvChannel.flush();
				generateV1();
				rcvChannel.flush();

			}

		});

		/*try {
			exec.wait(Long.MAX_VALUE);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
		
		// Create OT instance
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
				
				generateTripleC();
				
				rcvChannel.disconnect();
				sndChannel.disconnect();
				//System.out.println("6) disconnect");
			}
		} catch (InterruptedException e) {
			// Something is wrong
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		
		
	}
	
	public eightBitMultiplicationTriple(final Network sndChannel, final Network rcvChannel) {

		this.shrGen = new eightBitShareGenerator(true);
		this.random = shrGen.random;
		// generate shares of <A>, <B>
		this.generateSharedAB(random);
		 //System.out.println("a0:" + (this.tripleA0 & 0xff) + " a1:" + (this.tripleA1 & 0xff) + " b0:" +
		 //	  (this.tripleB0 & 0xff) + " b1:" + (this.tripleB1 & 0xff));

		ExecutorService exec = Executors.newFixedThreadPool(2); 
		exec.execute(new Runnable() {
			@Override
			public void run() {

				setSnd(sndChannel);
				generateU0();
				sndChannel.flush();
				generateV0();
				sndChannel.flush();


			}
		});

		exec.execute(new Runnable() {

			@Override
			public void run() {

				setRcv(rcvChannel);
				generateU1();
				rcvChannel.flush();
				generateV1();
				rcvChannel.flush();

			}

		});

		/*
		 * exec.execute(new Runnable() {
		 * 
		 * @Override public void run() { setSnd(sndChannel);
		 * 
		 * generateV0(); sndChannel.flush(); } });
		 * 
		 * 
		 * exec.execute(new Runnable() {
		 * 
		 * @Override public void run() { setRcv(rcvChannel);
		 * 
		 * generateV1(); rcvChannel.flush(); }
		 * 
		 * });
		 */

		// Create OT instance
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

		generateTripleC();
	}

	private void generateTripleC() {

		// recover <U>
		short tripleU = eightBitAdditiveUtil.add(tripleU0, tripleU1);

		// recover <V>
		short tripleV = eightBitAdditiveUtil.add(tripleV0, tripleV1);

		// get A0*B0
		short A0B0 = eightBitAdditiveUtil.mul(tripleA0, tripleB0);
		// get A1*B1
		short A1B1 = eightBitAdditiveUtil.mul(tripleA1, tripleB1);
		// System.out.println("U0:"+tripleU0+" U1:"+tripleU1+" V0:"+tripleV0+"
		// V1:"+tripleV1);
		// System.out.println("A0B0:"+A0B0+" A1B1:"+A1B1+" A0B1(U):"+tripleU+"
		// A1B0(V):"+tripleV);

		// compute C
		short C = eightBitAdditiveUtil.add(eightBitAdditiveUtil.add(eightBitAdditiveUtil.add(A0B0, A1B1), tripleU), tripleV);

		// C0, C1
		shrGen.generateSharedDataPoint(C);
		this.tripleC0 = shrGen.x0;
		this.tripleC1 = shrGen.x1;

	}

	private void setSnd(Network sndChannel) {
		if (sndChannel != null) {
			//System.out.println("Initialize OTExtSender");
			snd = new OTExtSender(7, sndChannel);
		}
	}

	private void setRcv(Network rcvChannel) {
		if (rcvChannel != null) {
			rcv = new OTExtReceiver(rcvChannel);
		}
	}

	private void generateSharedAB(SecureRandom random) {
		shrGen.generateSharedDataPointSet(random);
		this.tripleA0 = shrGen.x0;
		this.tripleA1 = shrGen.x1;

		shrGen.generateSharedDataPointSet(random);
		this.tripleB0 = shrGen.x0;
		this.tripleB1 = shrGen.x1;

	}

	private GCSignal[] generatePairs(short si0, short si1) {
		GCSignal[] label = new GCSignal[2];

		label[0] = GCSignal.newInstance(BigInteger.valueOf(si0).toByteArray());
		label[1] = GCSignal.newInstance(BigInteger.valueOf(si1).toByteArray());
//		byte[] si0_bytes = new byte[1];
//		si0_bytes[0] = si0;
//		byte[] si1_bytes = new byte[1];
//		si1_bytes[0] = si1;
//		label[0] = GCSignal.newInstance(si0_shorts);
//		//label[0] = GCSignal.newInstance(BigInteger.valueOf(si0).toByteArray());
//
//		label[1] = GCSignal.newInstance(si1_bytes);
//		//label[1] = GCSignal.newInstance(BigInteger.valueOf(si1).toshortArray());
		return label;
	}

	/**
	 * Generate <V>_0, <V> = <A>_1 * <B>_0
	 */
	private void generateV0() {
		// send B0
		this.tripleV0 = this.generateP0Share(this.tripleB0);
		//System.out.println("V0:" + this.tripleV0);
	}

	/**
	 * Generate <U>_0, <U> = <A>_0 * <B>_1
	 */
	private void generateU0() {
		// send A0
		this.tripleU0 = this.generateP0Share(this.tripleA0);
		//System.out.println("U0:" + this.tripleU0);
	}

	/**
	 * Generate <U> = <A>_0 * <B>_1
	 */
	private void generateU1() {
		// receive B1
		this.tripleU1 = this.generateP1Share(this.tripleB1);
		//System.out.println("U1:" + this.tripleU1);
	}

	/**
	 * Generate <V>_1, <V> = <A>_1 * <B>_0
	 */
	private void generateV1() {
		// receive A1
		this.tripleV1 = this.generateP1Share(this.tripleA1);
		//System.out.println("V1:" + this.tripleV1);
	}

	private short generateP0Share(short A0) {
		short U0 = 0;
		GCSignal[][] pair = new GCSignal[7][2];
		for (int i = 0; i < 7; i++) {
			short si0 = (short) random.nextInt(127);
			short si1 = eightBitAdditiveUtil.sub(eightBitAdditiveUtil.modAdditive(A0 << i), si0);
			U0 = eightBitAdditiveUtil.add(si0, U0);

			pair[i] = generatePairs(eightBitAdditiveUtil.modAdditive(-si0), si1);// si0, si1
//			byte to_bytes[] = new byte[1];
//			//to_bytes[0] = Byte.MAX_VALUE;
//			byte[] bytes = new byte[1];
//			random.nextBytes(bytes);
//			//System.out.println("Original bytes:" + (bytes[0] & 0xff));
//			//byte si0 = bytes[0];
//			byte si0 = (byte) (bytes[0]  >>> 1);
//			//System.out.println("Moved bytes::" +si0);
//			byte si1 = eightBitAdditiveUtil.sub(eightBitAdditiveUtil.modAdditive(A0 << i), si0);
//			//System.out.println("si0:" + (si0 & 0xff));
//			//System.out.println("Ui0:" + (U0 & 0xff));
//			U0 = eightBitAdditiveUtil.add(si0, U0);
//			//System.out.println("Ui0:" + (U0 & 0xff));

//			byte[] text_bytes = new byte[2];
//			random.nextBytes(text_bytes);
//			byte test1 = text_bytes[0];
//			byte test2 = text_bytes[1];
//			System.out.println("test1:" + (test1 & 0xff));
//			System.out.println("test2:" + (test2 & 0xff));
//			byte testr1 = eightBitAdditiveUtil.add(test1, test2);
//			System.out.println("testr1:" + (testr1 & 0xff));
//			byte testr2 = eightBitAdditiveUtil.sub(test1, test2);
//			System.out.println("testr2:" + (testr2 & 0xff));
//			byte testr3 = eightBitAdditiveUtil.mul(test1, test2);
//			System.out.println("testr3:" + (testr3 & 0xff));
//
//			pair[i] = generatePairs(eightBitAdditiveUtil.modAdditive(-si0), si1);// si0, si1
		}
		try {
			snd.send(pair);

		} catch (IOException e) {
			e.printStackTrace();
		}

		U0 = eightBitAdditiveUtil.modAdditive(U0);

		return U0;
	}

	private short generateP1Share(short B1) {
		short U1 = 0;
		boolean[] inputB1 = Utils.fromInt((int) B1, 7);
//		boolean[] inputB1 = Utils.fromByte((byte) B1, 8);
		try {
			GCSignal[] res = rcv.receive(inputB1);// select bits between si0, si1 based on value B1
			for (int i = 0; i < 7; i++) {
				U1 += AdditiveUtil.modAdditive(new BigInteger(res[i].bytes).shortValue());
				//U1 += eightBitAdditiveUtil.modAdditive(res[i].bytes[0]);
				//U1 += eightBitAdditiveUtil.modAdditive(new BigInteger(res[i].bytes).intValue());
			}
			U1 = eightBitAdditiveUtil.modAdditive(U1);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return U1;
	}

	public short getTripleA0() {
		return tripleA0;
	}

	public void setTripleA0(short tripleA0) {
		this.tripleA0 = tripleA0;
	}

	public short getTripleA1() {
		return tripleA1;
	}

	public void setTripleA1(short tripleA1) {
		this.tripleA1 = tripleA1;
	}

	public short getTripleB0() {
		return tripleB0;
	}

	public void setTripleB0(short tripleB0) {
		this.tripleB0 = tripleB0;
	}

	public short getTripleB1() {
		return tripleB1;
	}

	public void setTripleB1(short tripleB1) { this.tripleB1 = tripleB1; }

	public short getTripleC0() { return tripleC0; }

	public void setTripleC0(short tripleC0) {
		this.tripleC0 = tripleC0;
	}

	public short getTripleC1() {
		return tripleC1;
	}

	public void setTripleC1(short tripleC1) {
		this.tripleC1 = tripleC1;
	}

	public short getTripleU0() {
		return tripleU0;
	}

	public void setTripleU0(short tripleU0) {
		this.tripleU0 = tripleU0;
	}

	public short getTripleU1() {
		return tripleU1;
	}

	public void setTripleU1(short tripleU1) {
		this.tripleU1 = tripleU1;
	}

	public short getTripleV0() {
		return tripleV0;
	}

	public void setTripleV0(short tripleV0) {
		this.tripleV0 = tripleV0;
	}

	public short getTripleV1() {
		return tripleV1;
	}

	public void setTripleV1(short tripleV1) {
		this.tripleV1 = tripleV1;
	}

	public static void main(String[] args) {
		//int counter = Config.getSettingInt(Constants.CONFIG_MT_NUM);	
		int counter =1;
		
		//String fileNameIn = Config.getSetting(Constants.CONFIG_MT_FILE_NAME_IN);
		String fileNameOut = Config.getSetting(Constants.CONFIG_MT_FILE_NAME_OUT);
		String filePath = Config.getSetting(Constants.CONFIG_MT_PATH);
		String separator = Config.getSetting(Constants.CONFIG_MT_SEPARATOR);
		System.out.println("Separator: ["+separator+"]");
		
		WriteFile writeFile = new WriteFile();
		boolean isLastLine = false;
		//int queryLength = Config.getSettingInt(Constants.CONFIG_DTW_QUERY_LENGTH);//128
		
		
		final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		final int port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
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

		System.out.println("Starting Multiplication Triple generation...");

		
		Flag.sw.startTotal();
		for (int i = 0; i < counter; i++) {
			if(i == counter-1) {
				isLastLine = true;
			}
			String line = "";
			
			double s = System.nanoTime();
			eightBitMultiplicationTriple mt = new eightBitMultiplicationTriple(sndChannel, rcvChannel);
			double e = System.nanoTime();
			elapsedTimeTotal += e-s;
						
			
			//write to file
			line = mt.tripleA0+separator+mt.tripleA1+separator+mt.tripleB0+separator+mt.tripleB1+separator+mt.tripleC0+separator+mt.tripleC1;
			writeFile.writeFile(filePath, fileNameOut, line, isLastLine);
			
			
			
			// System.out.println("A0:"+mt.tripleA0+" A1:"+mt.tripleA1+" B0:"+mt.tripleB0+"
			// B1:"+mt.tripleB1+" C0:"+mt.tripleC0+" C1:"+mt.tripleC1);

			/*long a = AdditiveUtil.add(mt.tripleA0, mt.tripleA1);
			long b = AdditiveUtil.add(mt.tripleB0, mt.tripleB1);
			long c = AdditiveUtil.add(mt.tripleC0, mt.tripleC1);
			
			  System.out.print("MT A:" + a); System.out.print(" MT B:" + b);
			  System.out.println(" MT C:" + c);
			 

			// verify
			long cVer = AdditiveUtil.mul(a, b);
			
			  System.out.println("verify c:" + cVer +" u:"+AdditiveUtil.mul(mt.tripleA0,
			  mt.tripleB1)+" v:"+AdditiveUtil.mul(mt.tripleA1, mt.tripleB0)); 
			 */

			// Shutdown channel in test program
			
			if (i % 1000 == 0) {
				System.out.println("Progress:" + i);
			}
		}
		Flag.sw.stopTotal();
		//double e = System.nanoTime();
		System.out.println("Gen running time(second):"+elapsedTimeTotal/1e9);
		System.out.println("Gen running time(mu second):"+elapsedTimeTotal/1e3);
		rcvChannel.disconnect();
		sndChannel.disconnect();
		if(Flag.CountTime)
			Flag.sw.print();
		if(Flag.countIO)
			rcvChannel.printStatistic();

	}

}
