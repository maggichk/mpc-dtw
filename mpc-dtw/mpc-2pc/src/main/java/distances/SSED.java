package distances;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.AdditiveUtil;
import additive.MultiplicationTriple;
import additive.SeqCompEngine;
import additive.ShareGenerator;
import additive.SharedSequence;
import flexSC.network.Client;
import flexSC.network.Server;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class SSED {
	
	private Server sndChannel;
	private Client rcvChannel;
	private int port;
	private String hostname;
	private int portCli;
	
	public long bandwidth=0;
	
	public SSED() {
		
	}
	
	public SSED(int port, int portCli, String hostname) {
		this.port = port;
		this.hostname = hostname;	
		this.portCli = portCli;
				
	}
	
	public long[] computeConcurrent(MultiplicationTriple mt, long x0, long y0, long x1,
			long y1, long sqx0, long sqy0, long sqx1, long sqy1) throws Exception {
		this.sndChannel = new Server();
		this.rcvChannel = new Client();
		ConnectionHelper connector = new ConnectionHelper();
		//connector.connect(hostname, port, sndChannel, rcvChannel);
		connector.connect(hostname, port, portCli, sndChannel, rcvChannel);
		
		SeqCompEngine engine = new SeqCompEngine(true, sndChannel, rcvChannel, mt, x0, y0, x1, y1);
		long ssed[] = new long[2];
		long z0 = AdditiveUtil.mul(2L, engine.z0);
		long z1 = AdditiveUtil.mul(2L, engine.z1);
		//System.out.println("z0:"+z0+" z1:"+z1+" z:"+AdditiveUtil.add(z0, z1));
		ssed[0] = AdditiveUtil.sub(AdditiveUtil.add(sqx0, sqy0), z0);//x0^2 + y0^2 - 2x0y0 
		ssed[1] = AdditiveUtil.sub(AdditiveUtil.add(sqx1, sqy1), z1);//x1^2 + y1^2 - 2x1y1
		
		/*sndChannel.disconnect();
		rcvChannel.disconnect();*/
		
		bandwidth = engine.bandwidth;
		
		return ssed;
	}

	public long[] compute(boolean isDisconnect, Server sndChannel, Client rcvChannel, MultiplicationTriple mt, long x0, long y0, long x1,
			long y1, long sqx0, long sqy0, long sqx1, long sqy1) throws Exception {
		SeqCompEngine engine = new SeqCompEngine(isDisconnect, sndChannel, rcvChannel, mt, x0, y0, x1, y1);
		long ssed[] = new long[2];
		long z0 = AdditiveUtil.mul(2L, engine.z0);
		long z1 = AdditiveUtil.mul(2L, engine.z1);
		//System.out.println("z0:"+z0+" z1:"+z1+" z:"+AdditiveUtil.add(z0, z1));
		ssed[0] = AdditiveUtil.sub(AdditiveUtil.add(sqx0, sqy0), z0);//x0^2 + y0^2 - 2x0y0 
		ssed[1] = AdditiveUtil.sub(AdditiveUtil.add(sqx1, sqy1), z1);//x1^2 + y1^2 - 2x1y1
		bandwidth = engine.bandwidth;
		//System.out.println("ssed inside bandwidth:"+bandwidth);
		return ssed;
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

		ArrayList<MultiplicationTriple> mts = new ArrayList<MultiplicationTriple>();

		for (int i = 0; i < 4; i++) {
			MultiplicationTriple mt = new MultiplicationTriple(sndChannel, rcvChannel);
			mts.add(mt);
		}

		ShareGenerator generator = new ShareGenerator();

		// int queryLength = 1;
		// u0
		long[] u0data = new long[1];
		u0data[0] = 1;
		long[] u0sqdata = new long[1];
		u0sqdata[0] = 1;
		SharedSequence U0 = new SharedSequence(1, 0, 0,0, u0data, u0sqdata);
		// u1
		generator.generateSharedSequence(U0);
		SharedSequence U1 = generator.S1;

		// l0
		long[] l0data = new long[1];
		l0data[0] = 3;
		long[] l0sqdata = new long[1];
		l0sqdata[0] = 9;
		SharedSequence L0 = new SharedSequence(1, 0, 0,0, l0data, l0sqdata);
		// l1
		generator.generateSharedSequence(L0);
		SharedSequence L1 = generator.S1;

		// l0
		long[] y0data = new long[1];
		y0data[0] = 2;
		long[] y0sqdata = new long[1];
		y0sqdata[0] = 4;
		SharedSequence Y0 = new SharedSequence(1, 0, 0, 0,y0data, y0sqdata);
		// l1
		generator.generateSharedSequence(Y0);
		SharedSequence Y1 = generator.S1;
		System.out.println("Y0:" + Y0.getSharedData(0)[0] + " " + Y0.getSharedData(0)[1]);
		System.out.println("Y1:" + Y1.getSharedData(0)[0] + " " + Y1.getSharedData(0)[1]);
		// SharedSequence verY = generator.recover(Y0, Y1);
		 System.out.println("verify Y:" + AdditiveUtil.add(Y0.getSharedData(0)[0], Y1.getSharedData(0)[0]));

		SSED ssed = new SSED();// y, u
		long[] ssedYU = ssed.compute(false, sndChannel, rcvChannel, mts.get(0), Y0.getSharedData(0)[0], U0.getSharedData(0)[0],
				Y1.getSharedData(0)[0], U1.getSharedData(0)[0], Y0.getSharedData(0)[1], U0.getSharedData(0)[1],
				Y1.getSharedData(0)[1], U1.getSharedData(0)[1]);
		System.out.println("ssedYu : "+AdditiveUtil.add(ssedYU[0], ssedYU[1]));
		
		long[] ssedLU = ssed.compute(false, sndChannel, rcvChannel, mts.get(1), U0.getSharedData(0)[0], L0.getSharedData(0)[0],
				U1.getSharedData(0)[0], L1.getSharedData(0)[0], U0.getSharedData(0)[1], L0.getSharedData(0)[1],
				U1.getSharedData(0)[1], L1.getSharedData(0)[1]);
		System.out.println("ssedLu : "+AdditiveUtil.add(ssedLU[0], ssedLU[1]));

		sndChannel.disconnect();
		rcvChannel.disconnect();
	}

}
