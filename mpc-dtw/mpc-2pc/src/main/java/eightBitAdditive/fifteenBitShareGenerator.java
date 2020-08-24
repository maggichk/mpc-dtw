package eightBitAdditive;

import common.util.Constants;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class fifteenBitShareGenerator {

	public fifteenBitSharedSequence S1;
	public fifteenBitSharedSequence S0;

	public short x0; // <x>_0, <x>_1
	public short x1;

	public SecureRandom random;

	public fifteenBitShareGenerator() {

	}

	public fifteenBitShareGenerator(boolean isGenerateRandom) {
		if (isGenerateRandom) {
			try {
				this.random = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				System.out.println("Fail to get SecureRandom instance.");
				e.printStackTrace();
			}
		}

	}

	public void generateSharedDataPointSet() {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			this.generateSharedDataPointSet(random);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
		}
	}

	public void generateSharedDataPointSet(SecureRandom random) {

		this.x1 = (short) random.nextInt(32767);
		this.x0 = (short) random.nextInt(32767);
//		short[] bytes = new byte[2];
//		random.nextBytes(bytes);
//		this.x1 = (byte) (bytes[0]  >>> 1);
//		this.x0 = (byte) (bytes[1]  >>> 1);
//		//this.x1 = random.nextInt(Integer.MAX_VALUE);
//		//this.x0 = random.nextInt(Integer.MAX_VALUE);

	}

	public short generateRandom() {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			return (short) random.nextInt(32767);
//			short[] shorts = new byte[1];
//			random.nextBytes(bytes);
//			return (byte) (bytes[0]  >>> 1);

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
			return 0;
		}

	}

	/**
	 * Given instance
	 * 
	 * @param isGivenRandom
	 * @return
	 */
	public short generateRandom(boolean isGivenRandom) {

		return (short) this.random.nextInt(32767);
//		byte[] bytes = new byte[1];
//		random.nextBytes(bytes);
//		return (byte) (bytes[0]  >>> 1);
	}

	public void generateSharedDataPoint(short x0, boolean isGivenRandom) {

		SecureRandom random = this.random;
		this.x1 = (short) random.nextInt(32767);
		this.x0 = fifteenBitAdditiveUtil.sub(x0, x1);
//		byte[] bytes = new byte[1];
//		random.nextBytes(bytes);
//		//System.out.println("input:" + x0 );
//		this.x1 = (byte) (bytes[0]  >>> 1);
//		this.x0 = eightBitAdditiveUtil.sub(x0, x1);
//		//System.out.println(" x0(input - x1):" + this.x0 + " x1(random number):" + this.x1);

	}

	public void generateSharedDataPoint(short x0) {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
//			byte[] bytes = new byte[1];
//			random.nextBytes(bytes);
//			this.x1 = (byte) (bytes[0]  >>> 1);
			this.x1 = (short) random.nextInt(32767);
			this.x0 = fifteenBitAdditiveUtil.sub(x0, x1);

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
		}
	}

	public void generateSharedSequence(fifteenBitSharedSequence S0, boolean isGivenRandom) {
		// SecureRandom random
		this.S1 = new fifteenBitSharedSequence(S0.getLength(), S0.getIndex(), S0.getClusterIndex(), 1, S0.isClusterCenter(),
				random); // 1 indicates share1
		S0.minus(S1);
		S0.setShareNo(0);
		this.S0 = S0;
	}

	public void generateSharedSequence(fifteenBitSharedSequence S0) {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			// int queryLength, int index, int clusterIndex, int shareNo, boolean isCenter,
			// SecureRandom random
			this.S1 = new fifteenBitSharedSequence(S0.getLength(), S0.getIndex(), S0.getClusterIndex(), 1, S0.isClusterCenter(),
					random); // 1 indicates share1
			S0.minus(S1);
			S0.setShareNo(0);
			this.S0 = S0;

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
		}
	}

	public void generate2SharedSequences(fifteenBitSharedSequence S, boolean isGivenRandom) {
		// SecureRandom random
		this.S1 = new fifteenBitSharedSequence(S.getLength(), S.getIndex(), S.getClusterIndex(), 1, S.isClusterCenter(), random); // 1
																														// indicates
																														// share1
		this.S0 = new fifteenBitSharedSequence(S.getLength(), S.getIndex(), S.getClusterIndex(), 0, S.isClusterCenter(),
				S.getSharedSequence(), S.getSharedSquareSequence());
		S0.minus(S1);
		S0.setShareNo(0);

	}

	public void generate2SharedSequences(fifteenBitSharedSequence S) {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			// int queryLength, int index, int clusterIndex, int shareNo, boolean isCenter,
			// SecureRandom random
			this.S1 = new fifteenBitSharedSequence(S.getLength(), S.getIndex(), S.getClusterIndex(), 1, S.isClusterCenter(),
					random); // 1 indicates share1
			this.S0 = new fifteenBitSharedSequence(S.getLength(), S.getIndex(), S.getClusterIndex(), 0, S.isClusterCenter(),
					S.getSharedSequence(), S.getSharedSquareSequence());
			S0.minus(S1);
			S0.setShareNo(0);

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
		}
	}

	public fifteenBitSharedSequence recover(fifteenBitSharedSequence S0, fifteenBitSharedSequence S1) {
		S0.add(S1);
		return S0;
	}

	public static void main(String[] args) {
		String queryLength = Constants.CONFIG_DTW_QUERY_LENGTH;

	}

}
