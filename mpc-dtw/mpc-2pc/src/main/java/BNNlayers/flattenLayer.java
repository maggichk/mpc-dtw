package BNNlayers;

import booleanShr.ANDTriple;
import booleanShr.BooleanShrGenerator;
import booleanShr.BooleanUtil;
import flexSC.network.Client;
import flexSC.network.Server;
import gadgetBNN.SecMaxPooling;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class flattenLayer {

	//flaten layer convert neurons from multiple dimension matrixs to a vector.

	//input:
	public byte[][][][] x;

	//output:
	public byte[][] y;
	//parameter:
	public int stride = 2;

	private Server sndChannel;
	private Client rcvChannel;
	public byte z0AND;
	public byte z1AND;

	public byte a0 = 0;
	public byte a1 = 0;

	public double bandwidth = 0.0;

	public long timeNetwork = 0;

	private byte[] ARR_0 = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0 };

	
	public byte[][] compute_1_flatten(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			byte[][][][] x) throws Exception {

		//flatten:
		//input:
		//x[channel][length][width]
		//output:
		//y[channel]
		int output = x[0].length;
		byte[][] out_y = new byte[2][output];

		for(int i=0; i<output; i++){
			out_y[0][i] = x[0][i][0][0];
			out_y[1][i] = x[1][i][0][0];
		}

//		int channel = x[0].length;
//		int length = x[0][0].length;
//		int width = x[0][0][0].length;
//
//
//		//byte[][][][] in_x = new byte[channel][(int) Math.ceil(length/2)][(int) Math.ceil(width/2)][stride*stride];
//
//		//SecMaxPooling maxp = new SecMaxPooling();
//		//each channel
//		for(int i=0; i<channel; i++){
//			for(int i_y=0; i_y<length; i_y++){
//				System.arraycopy(x[0][i][i_y], 0, out_y[0], width * (i * length + i_y),width);
//				System.arraycopy(x[1][i][i_y], 0, out_y[1], width * (i * length + i_y),width);
//			}
//		}
		return out_y;
	}
	
	
	public byte[][] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, ANDTriple mt2,
			byte[][][][] x) throws Exception {

		//flatten:
		//input:
		//x[channel][length][width]
		//output:
		//y[channel]
		int channel = x[0].length;
		int length = x[0][0].length;
		int width = x[0][0][0].length;

		byte[][] out_y = new byte[2][channel * length * width];
		//byte[][][][] in_x = new byte[channel][(int) Math.ceil(length/2)][(int) Math.ceil(width/2)][stride*stride];

		SecMaxPooling maxp = new SecMaxPooling();
		//each channel
		for(int i=0; i<channel; i++){
			for(int i_y=0; i_y<length; i_y++){
				System.arraycopy(x[0][i][i_y], 0, out_y[0], width * (i * length + i_y),width);
				System.arraycopy(x[1][i][i_y], 0, out_y[1], width * (i * length + i_y),width);
			}
		}
		return out_y;
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

		BooleanShrGenerator boolGen = new BooleanShrGenerator(true);
		ANDTriple mt2 = new ANDTriple(boolGen);
		System.out.println("mt2 A0:" + mt2.tripleA0 + " A1:" + mt2.tripleA1 + " B0:" + mt2.tripleB0 + " B1:"
				+ mt2.tripleB1 + " C0:" + mt2.tripleC0 + " C1:" + mt2.tripleC1);

		//MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
		// long bandwidthMT =
		// rcvChannel.cis.getByteCount()+rcvChannel.cos.getByteCount();
		rcvChannel.cis.resetByteCount();
		rcvChannel.cos.resetByteCount();

		//test case in XONN
		byte[][][][] x = {
				{
						{{(byte) 1, (byte) 0},
								{(byte) 1, (byte) 1}
						},
						{{(byte) 0, (byte) 1},
								{(byte) 1, (byte) 1}
						},
				},
				{
						{{(byte) 0, (byte) 1},
								{(byte) 1, (byte) 1}
						},
						{{(byte) 1, (byte) 0},
								{(byte) 1, (byte) 1}
						},
				},
		};


		flattenLayer secmp = new flattenLayer();// y, u
		int round = 1;
		long time = 0;
		//double bandwidth = 0.0;
		long e = System.nanoTime();
		for (int i = 0; i < round; i++) {			
			//extract MSB of a
			byte[][] z = secmp.compute(false, sndChannel, rcvChannel, mt2,  x);
			// verify
			//System.out.println("z:" + Arrays.toString(z));
			System.out.println("x0" + Arrays.toString(z[0]));
			System.out.println("x0" + Arrays.toString(z[1]));

		}


		long s = System.nanoTime();
		time += s - e;
		//bandwidth = rcvChannel.cis.getByteCount() + rcvChannel.cos.getByteCount();
		System.out.println("time:" + time / 1e9 + " seconds");
		System.out.println("bandwidth:" + secmp.bandwidth / 1024.0 / 1024.0 / 1024.0 + " GB");
		System.out.println("timeNetwork:" + secmp.timeNetwork / 1e9 + " seconds");

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

	
	
}
