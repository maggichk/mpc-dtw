package testing;

public class Tester {
	public static void main(String[] args) {
		
		long candidateSeqNum = 8637;
		long allSeqNum = 15000;
		double candidateSeqNumDouble = candidateSeqNum;
		double allSeqNumDouble = allSeqNum;

		double candidateRatio = candidateSeqNumDouble/allSeqNumDouble;
		double ratio = 1.0 - candidateRatio;
		System.out.println("candidateRatio:"+candidateRatio);
		System.out.println("ratio:"+ratio);
		
		
	}
}
