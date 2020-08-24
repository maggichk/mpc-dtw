package booleanShr;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

public class ANDTriple {

	public byte tripleA0, tripleA1;
	public byte tripleB0, tripleB1;
	public byte tripleC0, tripleC1; // (A0 xor A1) AND (B0 xor B1) = (C0 xor C1)



	private BooleanShrGenerator shrGen;
	private SecureRandom random;

	private static double elapsedTimeTotal;
	// private double startTime;

	

	
	/**
	 * Dummy MT generation
	 * @param shrGen
	 */
	public ANDTriple(BooleanShrGenerator shrGen) {
		this.shrGen = shrGen;
		this.random = shrGen.random;
		this.generateSharedAB(random);
		
		// get A
		byte A = BooleanUtil.xor(tripleA0, tripleA1);
		// get B
		byte B = BooleanUtil.xor(tripleB0, tripleB1);
		
		// compute C
		byte C = BooleanUtil.and(A,B);

		// C0, C1
		shrGen.generateSharedDataPoint(C);
		this.tripleC0 = shrGen.x0;
		this.tripleC1 = shrGen.x1;				
		
	}

	private void generateSharedAB(SecureRandom random) {
		shrGen.generateSharedDataPointSet(random);
		this.tripleA0 = shrGen.x0;
		this.tripleA1 = shrGen.x1;

		shrGen.generateSharedDataPointSet(random);
		this.tripleB0 = shrGen.x0;
		this.tripleB1 = shrGen.x1;

	}

	
}
