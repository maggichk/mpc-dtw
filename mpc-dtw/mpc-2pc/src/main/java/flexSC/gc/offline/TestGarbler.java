package flexSC.gc.offline;

import flexSC.gc.GCSignal;

import java.security.SecureRandom;



public class TestGarbler {
	SecureRandom rnd = new SecureRandom();
	GCSignal a = GCSignal.freshLabel(rnd);
	GCSignal b = GCSignal.freshLabel(rnd);
	GCSignal m = GCSignal.freshLabel(rnd);
	Garbler gb = new Garbler();
	
	public void test() {
		gb.enc(a, b, 0, m);
	}

	
	public void test1000() {
		double t1 = System.nanoTime();
		int len = 1000000;
		for(int i = 0; i<len; i++)
			test();
		double t2 = System.nanoTime();
		System.out.println(len/(t2-t1)*1000000000.0);
	}
	
	
	public static void main(String[] args) {
		TestGarbler a = new TestGarbler();
		a.test1000();
	}
}