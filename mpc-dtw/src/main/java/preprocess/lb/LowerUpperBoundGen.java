package preprocess.lb;

import org.bouncycastle.util.Arrays;

import common.util.UtilHelper;

public class LowerUpperBoundGen {
	
	public void generateBounds(int queryLength, int[] query, int[] lb, int[] ub, double cr){
		int band = new Double(Math.floor(cr * queryLength)).intValue();
		
		for(int i=0; i<queryLength; i++) {
			int[] temp;
			if(i<band) {
				temp = new int[band+i];
				temp = Arrays.copyOfRange(query, 0, band+i+1);
				/*for(int j=0; j<(band+i); j++) {					
						
						temp[j] = query[j];
					
				}*/
				
			
			}else if(i+band >= queryLength ){
				temp = new int[queryLength -i +band];
				temp = Arrays.copyOfRange(query, i-band, query.length);
				/*for(int j=0; j<temp.length; j++) {
					temp[j] = query[i-band+j];
					
				}*/
				
			}else {
				temp = new int[2*band+1];
				temp = Arrays.copyOfRange(query, i-band, i+band+1);
				/*for(int j=0; j<(2*band+1); j++) {
					temp[j] = query[i+j-band];
				}*/
			}
			UtilHelper.bubbleSortAscendingInt(temp);
			lb[i]=temp[0];
			ub[i]=temp[temp.length-1];
			
		}
		
	}
}
