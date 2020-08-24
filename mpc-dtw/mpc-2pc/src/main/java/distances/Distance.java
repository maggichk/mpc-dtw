package distances;

import java.util.ArrayList;

import additive.MultiplicationTriple;
import additive.SharedSequence;


public interface Distance {
	
	public long[] compute(ArrayList<MultiplicationTriple> mts,
			SharedSequence[] sequences, int queryLength);

}
