package utilMpc;

import java.util.Arrays;

import org.apache.commons.cli.ParseException;

import flexSC.flexsc.CompEnv;
import flexSC.flexsc.Flag;
import flexSC.flexsc.Mode;
import flexSC.flexsc.Party;


public abstract class GenRunnable<T> extends flexSC.network.Server implements Runnable {

	Mode m;
	int port;
	protected String[] args;
	public boolean verbose = true;
	public CompEnv<T> env;
	/*public ConfigParser config;
	public void setParameter(ConfigParser  config, String[] args) {
		this.m = Mode.getMode(config.getString("Mode"));
		this.port = config.getInt("Port");
		this.args = args;
		this.config = config;
	}*/
	
	public void setConnection(int port) {
		this.m = Mode.getMode(Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_MODE));
		this.port = port;
				//Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);		
		//this.config = config;
		this.verbose = false;
		//= Config2PC.getSettingBoolean(Constants2PC.CONFIG2PC_SERVER_VERBOSE);
	}
	
	public void setConnection() {
		this.m = Mode.getMode(Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_MODE));
		this.port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);		
		//this.config = config;
		this.verbose = false;
				//Config2PC.getSettingBoolean(Constants2PC.CONFIG2PC_SERVER_VERBOSE);
	}
	public void setInput(String[] args) {
		this.args = args;
	}
	public void setParameter(String[] args) {
		this.m = Mode.getMode(Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_MODE));
		this.port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);
		this.args = args;
		//this.config = config;
		//this.verbose = false;
		this.verbose= false;
		//this.verbose=Config2PC.getSettingBoolean(Constants2PC.CONFIG2PC_SERVER_VERBOSE);
	}

	public void setParameter(Mode m, int port) {
		this.m = m;
		this.port = port;
	}

	public abstract void prepareInput(CompEnv<T> gen) throws Exception;
	public abstract void secureCompute(CompEnv<T> gen) throws Exception;
	public abstract void prepareOutput(CompEnv<T> gen) throws Exception;
	public abstract long getOutputAlice() throws Exception;
	
	public CompEnv<T> connect(){
		if(verbose)
			System.out.println("connecting");
		listen(port);
		
		if(verbose)
			System.out.println("connected");

		//System.out.println("start get Gen");
		@SuppressWarnings("unchecked")
		CompEnv<T> env = CompEnv.getEnv(m, Party.Alice, this);
		
		return env;
		
	}
	
	public void run(CompEnv<T> env) {
		try {
			prepareInput(env);
			os.flush();
			secureCompute(env);
			os.flush();
			prepareOutput(env);
			os.flush();
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void disconnection() {
		disconnect();
		/*try {
			this.disconnectServer();
		} catch (Exception e) {
			
			e.printStackTrace();
		}*/
	}

	public void run() {
		try {
			if(verbose)
				System.out.println("connecting");
			System.out.println("port:"+port);
			listen(port);
			if(verbose)
				System.out.println("connected");

			@SuppressWarnings("unchecked")
			CompEnv<T> env = CompEnv.getEnv(m, Party.Alice, this);

			double s = System.nanoTime();
			Flag.sw.startTotal();
			System.out.println("Gen: start prepare input");
			prepareInput(env);
			os.flush();
			System.out.println("Gen: start compute");
			secureCompute(env);
			os.flush();
			System.out.println("Gen: start prepare output");
			prepareOutput(env);
			os.flush();
			Flag.sw.stopTotal();
			double e = System.nanoTime();
			disconnect();
			if(verbose) {
				System.out.println("Gen running time:"+(e-s)/1e9);
				System.out.println(env.numOfAnds);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	@SuppressWarnings("rawtypes")
	public static void main(String[] args) throws ParseException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		//ConfigParser config = new ConfigParser("Config.conf");

		Class<?> clazz = Class.forName(args[0]+"$Generator");
		GenRunnable run = (GenRunnable) clazz.newInstance();
		run.setParameter(Arrays.copyOfRange(args, 1, args.length));
		run.run();
		if(Flag.CountTime)
			Flag.sw.print();
	}
}
