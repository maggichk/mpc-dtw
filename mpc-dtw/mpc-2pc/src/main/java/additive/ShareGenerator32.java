package additive;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import common.util.Constants;
import utilMpc.Constants2PC;

public class ShareGenerator32 {
	
	
	public static long MODULUS_DBL = Constants2PC.MODULUS_32; // 2^32

	public SharedSequence32 S1;
	public SharedSequence32 S0;

	public long x0; // <x>_0, <x>_1
	public long x1;

	public SecureRandom random;

	public ShareGenerator32() {

	}

	public ShareGenerator32(boolean isGenerateRandom) {
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

		//this.x1 = random.nextLong()%MODULUS_DBL;
		//this.x0 = random.nextLong()%MODULUS_DBL;
		this.x1 = AdditiveUtil32.modAdditive(random.nextLong());
		this.x0 = AdditiveUtil32.modAdditive(random.nextLong());

	}

	public long generateRandom() {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			//return random.nextLong()%MODULUS_DBL;
			return AdditiveUtil32.modAdditive(random.nextLong());

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
			return 0L;
		}

	}

	/**
	 * Given instance
	 * 
	 * @param isGivenRandom
	 * @return
	 */
	public long generateRandom(boolean isGivenRandom) {

		//return this.random.nextLong()%MODULUS_DBL;
		return AdditiveUtil32.modAdditive(random.nextLong());
	}

	public void generateSharedDataPoint(long x0, boolean isGivenRandom) {

		SecureRandom random = this.random;
		//this.x1 = random.nextLong()%MODULUS_DBL;
		this.x1 = AdditiveUtil32.modAdditive(random.nextLong());
		this.x0 = AdditiveUtil32.sub(x0, x1);

	}

	public void generateSharedDataPoint(long x0) {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			//this.x1 = random.nextLong()%MODULUS_DBL;
			this.x1 = AdditiveUtil32.modAdditive(random.nextLong());
			this.x0 = AdditiveUtil32.sub(x0, x1);

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
		}
	}

	public void generateSharedSequence(SharedSequence32 S0, boolean isGivenRandom) {
		// SecureRandom random
		this.S1 = new SharedSequence32(S0.getLength(), S0.getIndex(), S0.getClusterIndex(), 1, S0.isClusterCenter(),
				random); // 1 indicates share1
		S0.minus(S1);
		S0.setShareNo(0);
		this.S0 = S0;
	}

	public void generateSharedSequence(SharedSequence32 S0) {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			// int queryLength, int index, int clusterIndex, int shareNo, boolean isCenter,
			// SecureRandom random
			this.S1 = new SharedSequence32(S0.getLength(), S0.getIndex(), S0.getClusterIndex(), 1, S0.isClusterCenter(),
					random); // 1 indicates share1
			S0.minus(S1);
			S0.setShareNo(0);
			this.S0 = S0;

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
		}
	}

	public void generate2SharedSequences(SharedSequence32 S, boolean isGivenRandom) {
		// SecureRandom random
		this.S1 = new SharedSequence32(S.getLength(), S.getIndex(), S.getClusterIndex(), 1, S.isClusterCenter(), random); // 1
																														// indicates
																														// share1
		this.S0 = new SharedSequence32(S.getLength(), S.getIndex(), S.getClusterIndex(), 0, S.isClusterCenter(),
				S.getSharedSequence(), S.getSharedSquareSequence());
		S0.minus(S1);
		S0.setShareNo(0);

	}

	public void generate2SharedSequences(SharedSequence32 S) {
		try {

			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			// int queryLength, int index, int clusterIndex, int shareNo, boolean isCenter,
			// SecureRandom random
			this.S1 = new SharedSequence32(S.getLength(), S.getIndex(), S.getClusterIndex(), 1, S.isClusterCenter(),
					random); // 1 indicates share1
			this.S0 = new SharedSequence32(S.getLength(), S.getIndex(), S.getClusterIndex(), 0, S.isClusterCenter(),
					S.getSharedSequence(), S.getSharedSquareSequence());
			S0.minus(S1);
			S0.setShareNo(0);

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Fail to generate shared sequence.");
			e.printStackTrace();
		}
	}

	public SharedSequence32 recover(SharedSequence32 S0, SharedSequence32 S1) {
		S0.add(S1);
		return S0;
	}

	public static void main(String[] args) {
		String queryLength = Constants.CONFIG_DTW_QUERY_LENGTH;

	}

}
