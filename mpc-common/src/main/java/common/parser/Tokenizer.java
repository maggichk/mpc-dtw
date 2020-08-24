package common.parser;


import common.util.UtilHelper;

/**
 * Split tweets to words
 * @author maggie liu
 *
 */
public class Tokenizer {
	
	public static int trim2Integer(String pointStr) {
		//System.out.println(pointStr+" "+pointStr.substring(0, 6));
		int pointInt = 0;
		double pointDouble = 0;
		if(Double.valueOf(pointStr) >= 0) {
			pointDouble = Double.valueOf(pointStr.substring(0, 4));
		}else {
			pointDouble = Double.valueOf(pointStr.substring(0, 5));
		}
		
		pointInt = (int) (pointDouble * 100 + 1000);
		return pointInt;
	}

	public String[] splitTweetToWords(String singleTweet){
	
		String[] wordsArray = null;
		if(!UtilHelper.isEmptyString(singleTweet)){
			
			//wordsArray = singleTweet.trim().split("\\s+");
			
			wordsArray = singleTweet.trim().split("[\\p{Punct}\\s]+");
			
		}else{
			System.out.println("Empty");
		}
		
		
		
		return wordsArray;
	}
}
