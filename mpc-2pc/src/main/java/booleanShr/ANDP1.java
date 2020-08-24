package booleanShr;

import flexSC.network.Client;
import flexSC.network.Server;

public class ANDP1 {
	
	
	private Client rcvChannel;
	
	public byte z1;
	public byte x1;
	public byte y1;

	
	public double time = 0.0;
	
	
	public void compute(boolean isDisconnect, Client rcvChannel, final ANDTriple mt2, byte x1, byte y1) {
		this.x1 = x1;
		this.y1 = y1;

		this.rcvChannel = rcvChannel;
		
		// generate e1 f1
		byte[] ef1 = ef1(mt2, x1, y1);
		final byte e1 = ef1[0];
		final byte f1 = ef1[1];				

		double st1 = System.nanoTime();
		
		sendP1(e1, f1);
		byte[] ef0P1 = receiveP1();		
		
		double et1 = System.nanoTime();
		time = et1 -st1;

		mulP1(mt2, e1, f1, ef0P1[0], ef0P1[1]);
	}
	
	
	
	
	public void sendP1(byte e1, byte f1) {
		// System.out.println("[P1] e1:" + e1 + " f1:" + f1 + " rcvChannel:" +
		// rcvChannel);
		rcvChannel.writeByte(e1);
		//rcvChannel.flush();
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
		
		// long tripleC1 = mt.tripleC1;
		byte e = BooleanUtil.xor(e0, e1);
		byte f = BooleanUtil.xor(f0, f1);
		// System.out.println("[P1] e:"+e+" f:"+f);
		byte ief = BooleanUtil.and(e, f);

		this.z1 = BooleanUtil.xor(ief, BooleanUtil
				.xor(BooleanUtil.xor(BooleanUtil.and(f, mt.tripleA1), BooleanUtil.and(e, mt.tripleB1)), mt.tripleC1));

	}

}
