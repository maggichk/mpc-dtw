package utilMpc;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import common.util.Config;
import common.util.Constants;

public final class Config2PC {

    private static final Properties properties = new Properties();

    static {
        try {
        	ClassLoader loader = Thread.currentThread().getContextClassLoader();
        	//ClassLoader loader = Config2PC.class.getClassLoader();
           // properties.load(loader.getResourceAsStream("./resources/2pc.properties"));
            properties.load(loader.getResourceAsStream("2pc.properties"));
            
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String getSetting(String key) {
        return properties.getProperty(key);
    }

    public static int getSettingInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
    
    public static double getSettingDouble(String key) {
        return Double.parseDouble(properties.getProperty(key));
    }
    
    public static char getSettingChar(String key) {
        return String.valueOf(properties.getProperty(key)).charAt(0);
    }
    public static boolean getSettingBoolean(String key) {
    	if(String.valueOf(properties.getProperty(key)).trim().equalsIgnoreCase("TRUE")){
    		return true;
    	}else {
    		return false;
    	}
    	
    }
    // ...
    
    public static void  main(String[] args) {
    	System.out.println("configtest:"+Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT));
    }
}