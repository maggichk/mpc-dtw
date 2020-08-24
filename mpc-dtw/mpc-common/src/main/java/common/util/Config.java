package common.util;

import java.io.IOException;
import java.util.Properties;

public final class Config {

    private static final Properties properties = new Properties();

    static {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            //properties.load(loader.getResourceAsStream("config.properties"));
            properties.load(loader.getResourceAsStream("configlinux.properties"));
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
    // ...
    
    public static void  main(String[] args) {
    	System.out.println("configtest:"+Config.getSetting(Constants.CONFIG_DATA_FILE_NAME_IN));
    }
}