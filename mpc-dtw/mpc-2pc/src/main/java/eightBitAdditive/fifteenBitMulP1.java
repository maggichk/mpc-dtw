package eightBitAdditive;

import flexSC.network.Client;

public class fifteenBitMulP1 {
	
	private Client rcvChannel;
	
	public short z1;
	
	public short x1;
	
	public short y1;
	
	public void compute(boolean isDisconnect, Client rcvChannel, final fifteenBitMultiplicationTriple mt, short x1, short y1) {
		this.x1 = x1;
		this.y1 = y1;
		this.rcvChannel = rcvChannel;
		// generate e1 f1
		short[] ef1 = ef1(mt, x1, y1);
		final short e1 = ef1[0];
		final short f1 = ef1[1];
		sendP1(e1, f1);
		short[] ef0P1 = receiveP1();
		mulP1(mt, e1, f1, ef0P1[0], ef0P1[1]);
	}

	public void sendP1(short e1, short f1) {
		//System.out.println("[P1] e1:" + e1 + " f1:" + f1 + " rcvChannel:" + rcvChannel);
		rcvChannel.writeShort(e1);
		rcvChannel.writeShort(f1);
		rcvChannel.flush();

	}

	public short[] receiveP1() {
		short[] res = new short[2];
		//res = rcvChannel.readBytes(2);
		res[0] = rcvChannel.readShort();// e0
		res[1] = rcvChannel.readShort();// f0
		//res[0] = rcvChannel.readInt();// e0
		//res[1] = rcvChannel.readInt();// f0
		rcvChannel.flush();
		// System.out.println("[P1] e0:" + res[0] + " f0:" + res[1]);
		return res;
	}

	public short[] ef1(fifteenBitMultiplicationTriple mt, short x1, short y1) {
		short tripleA1 = mt.tripleA1;
		short tripleB1 = mt.tripleB1;

		short e1 = fifteenBitAdditiveUtil.sub(x1, tripleA1);
		short f1 = fifteenBitAdditiveUtil.sub(y1, tripleB1);
		short[] res = new short[2];
		res[0] = e1;
		res[1] = f1;
		return res;
	}

	public void mulP1(fifteenBitMultiplicationTriple mt, short e1, short f1, short e0, short f0) {
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
		short tripleC1 = mt.tripleC1;
		short e = fifteenBitAdditiveUtil.add(e0, e1);
		short f = fifteenBitAdditiveUtil.add(f0, f1);
		// System.out.println("[P1] e:"+e+" f:"+f);
		short ief = fifteenBitAdditiveUtil.mul(e, f);

		this.z1 = fifteenBitAdditiveUtil.add(ief, fifteenBitAdditiveUtil
				.add(fifteenBitAdditiveUtil.add(fifteenBitAdditiveUtil.mul(f, mt.tripleA1), fifteenBitAdditiveUtil.mul(e, mt.tripleB1)), tripleC1));
		// System.out.println("z1:"+z1);
		// return z1;
	}
}
