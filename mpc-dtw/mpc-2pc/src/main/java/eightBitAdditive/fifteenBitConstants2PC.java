package eightBitAdditive;

public class fifteenBitConstants2PC {
	//modulus
	public static final int MODULUS = 65536;//32768; //2^31:2147483648 0```31~1```31
	//max value+1 of positive integer = 2^31 (31 bits range)
	//public static final long MAX_PLUS_1 = 2147483648L;
	
	
	public static final short MODULUS_32 = Short.valueOf((short) (2^15));
	public static final short MAX_DBL = Short.valueOf((short) (2^15-1));
	
	
	
	
	
	public static final String MACHINE_SPEC = "machine_spec/";
	public static final String INPUT_DIR = "in/";
	public static final int OFFLINE_FILE_BUFFER_SIZE = 1024 * 1024 * 1024;//1024 * 1024 * 1024

	//2PC Params
	public static final String CONFIG2PC_SERVER_HOSTNAME = "server.hostname";
	public static final String CONFIG2PC_SERVER_ARITHMETIC_PORT="server.arithmetic.port";
	public static final String CONFIG2PC_SERVER_GC_PORT = "server.gc.port";
	public static final String CONFIG2PC_SERVER_ROLE = "server.role";
	public static final String CONFIG2PC_SERVER_VERBOSE = "server.verbose";
	public static final String CONFIG2PC_SERVER_MODE ="server.mode";
	public static final String CONFIG2PC_SERVER_ARITHMETIC_BACKUP_PORT = "server.arithmetic.backup.port";
	public static final String CONFIG2PC_SERVER_GC_BACKUP_PORT="server.gc.backup.port";
}
