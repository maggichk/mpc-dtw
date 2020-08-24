package utilMpc;

import java.util.Arrays;

import org.apache.commons.cli.ParseException;

import flexSC.flexsc.CompEnv;
import flexSC.flexsc.Flag;
import flexSC.flexsc.Mode;
import flexSC.flexsc.Party;

public abstract class EvaRunnable<T> extends flexSC.network.Client implements Runnable {
	public abstract void prepareInput(CompEnv<T> gen) throws Exception;

	public abstract void secureCompute(CompEnv<T> gen) throws Exception;

	public abstract void prepareOutput(CompEnv<T> gen) throws Exception;

	public abstract long getOutputBob() throws Exception;

	Mode m;
	int port;
	String host;
	protected String[] args;
	public boolean verbose = true;
	// public ConfigParser config;
	public CompEnv<T> env;

	public void setInput(String[] args) {
		this.args = args;
	}

	public void setConnection(int port) {
		this.m = Mode.getMode(Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_MODE));

		this.port = port;

		this.host = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);

		this.verbose = false;
		// this.verbose =
		// Config2PC.getSettingBoolean(Constants2PC.CONFIG2PC_SERVER_VERBOSE);
	}

	public void setConnection() {
		this.m = Mode.getMode(Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_MODE));
		// this.port = config.getInt("Port");
		this.port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);
		// host = config.getString("Host");
		this.host = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		// this.config = config;
		this.verbose = false;
		// = Config2PC.getSettingBoolean(Constants2PC.CONFIG2PC_SERVER_VERBOSE);
	}

	public void setParameter(String[] args) {
		// this.m = Mode.getMode(config.getString("Mode"));
		this.m = Mode.getMode(Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_MODE));
		// this.port = config.getInt("Port");
		this.port = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);
		// host = config.getString("Host");
		this.host = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		this.args = args;
		// this.config = config;
		// this.verbose=false;
		// this.verbose = false;
		this.verbose = Config2PC.getSettingBoolean(Constants2PC.CONFIG2PC_SERVER_VERBOSE);
	}

	public void setParameter(Mode m, String host, int port) {
		this.m = m;
		this.port = port;
		this.host = host;
	}

	public CompEnv<T> connect(int serverPort, int clientPort){
		try {
			if (verbose)
				System.out.println("connecting");
			connect(host, serverPort, clientPort);

			if (verbose)
				System.out.println("connected");

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//System.out.println("start getEnv| serverPort:"+serverPort+" cliPort:"+clientPort);
		@SuppressWarnings("unchecked")
		CompEnv<T> env = CompEnv.getEnv(m, Party.Bob, this);
		return env;
	}
	
	
	public CompEnv<T> connect() {
		try {
			if (verbose)
				System.out.println("connecting");
			connect(host, port);

			if (verbose)
				System.out.println("connected");

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//System.out.println("start getEnv");
		@SuppressWarnings("unchecked")
		CompEnv<T> env = CompEnv.getEnv(m, Party.Bob, this);
		
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
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void disconnection() {
		disconnect();
		/*
		 * try { this.disconnectCli(); } catch (Exception e) {
		 * 
		 * e.printStackTrace(); }
		 */
	}

	public void run() {
		try {
			if (verbose)
				System.out.println("connecting");
			connect(host, port);
			if (verbose)
				System.out.println("connected");

			@SuppressWarnings("unchecked")
			CompEnv<T> env = CompEnv.getEnv(m, Party.Bob, this);

			double s = System.nanoTime();
			Flag.sw.startTotal();
			System.out.println("Eva: start prepare input");
			prepareInput(env);
			System.out.println("Eva:finishe prepare input");
			os.flush();
			System.out.println("Eva: start compute");
			secureCompute(env);
			System.out.println("Eva: finish compute");
			os.flush();
			System.out.println("Eva: start prepare output");
			prepareOutput(env);
			System.out.println("Eva: finish prepare output");
			os.flush();
			Flag.sw.stopTotal();
			double e = System.nanoTime();
			System.out.println("Start disconnect");
			disconnect();
			if (verbose) {
				System.out.println("Eva running time:" + (e - s) / 1e9);
				System.out.println("Number Of AND Gates:" + env.numOfAnds);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@SuppressWarnings("rawtypes")
	public static void main(String[] args)
			throws InstantiationException, IllegalAccessException, ParseException, ClassNotFoundException {
		// ConfigParser config = new ConfigParser("Config.conf");

		Class<?> clazz = Class.forName(args[0] + "$Evaluator");
		EvaRunnable run = (EvaRunnable) clazz.newInstance();
		// run.setParameter(config, Arrays.copyOfRange(args, 1, args.length));
		run.setParameter(Arrays.copyOfRange(args, 1, args.length));
		run.run();
		if (Flag.CountTime)
			Flag.sw.print();
		if (Flag.countIO)
			run.printStatistic();
	}
}