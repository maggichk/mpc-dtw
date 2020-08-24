package additive;

import flexSC.network.Client;
import flexSC.network.Server;

public class MulP1 {
	
	private Client rcvChannel;
	
	public long z1;
	
	public long x1;
	
	public long y1;
	
	public void compute(boolean isDisconnect, Client rcvChannel, final MultiplicationTriple mt, long x1, long y1) {
		this.x1 = x1;
		this.y1 = y1;
		this.rcvChannel = rcvChannel;
		// generate e1 f1
		long[] ef1 = ef1(mt, x1, y1);
		final long e1 = ef1[0];
		final long f1 = ef1[1];
		sendP1(e1, f1);
		long[] ef0P1 = receiveP1();
		mulP1(mt, e1, f1, ef0P1[0], ef0P1[1]);
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
}
