package gadgets;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import additive.ShareGenerator;
import common.db.ConnectRedis;
import common.util.Config;
import common.util.Constants;
import flexSC.flexsc.CompEnv;
import redis.clients.jedis.Jedis;
import utilMpc.Config2PC;
import utilMpc.Constants2PC;
import utilMpc.EvaRunnable;
import utilMpc.GenRunnable;

public class SCMPRankRunnable {

	public long rank;
	private int counter;
	public long bandwidth;

	public String[][] prepareArgs(long x0, long y0, long x1, long y1) {
		String[][] args = new String[2][2];

		String[] argsGen = new String[2];
		argsGen[0] = String.valueOf(x0);
		argsGen[1] = String.valueOf(y0);
		args[0] = argsGen;

		String[] argsEva = new String[2];
		argsEva[0] = String.valueOf(x1);
		argsEva[1] = String.valueOf(y1);
		args[1] = argsEva;

		return args;
	}

	public void run(final int port, final String[] argsGen, final String[] argsEva) {
		ExecutorService exec = Executors.newFixedThreadPool(2);
		// initialize GC
		@SuppressWarnings("rawtypes")
		final GenRunnable runGen = (GenRunnable) new SCMPGadgetRank.Generator<Long>();
		@SuppressWarnings("rawtypes")
		final EvaRunnable runEva = (EvaRunnable) new SCMPGadgetRank.Evaluator<Long>();

		// Alice
		exec.execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {

				runGen.setInput(argsGen);
				runGen.setConnection(port);

				CompEnv<Long> env = runGen.connect();
				//System.out.println(counter+"server connected");
				runGen.run(env);
				//System.out.println(counter+"server finished");

				// runGen.setParameter(argsGen);
				// runGen.run();
				try {

					rank = runGen.getOutputAlice();

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		});

		// Bob
		exec.execute(new Runnable() {
			@Override
			@SuppressWarnings("unchecked")
			public void run() {

				runEva.setInput(argsEva);
				runEva.setConnection(port);
				CompEnv<Long> env = runEva.connect();
				//System.out.println(counter+"client connected");
				runEva.run(env);
				//System.out.println(counter+"client finished");

				// runEva.setParameter(argsEva);
				// runEva.run();

			}
		});

		// shutdown threads
		// Connection should be established within 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(120, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
				
				//System.out.println(counter+"disconnect...");
				runEva.disconnection();
				//System.out.println(counter+"client disconnected");
				runGen.disconnection();
				//System.out.println(counter+"server disconnected");
			}else {
				System.out.println("SCMPRankGadget| Something wrong");
				System.exit(0);
			}
			
		} catch (InterruptedException e) {
			// Something is wrong
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

		bandwidth = (runEva.cos.getByteCount() + runEva.cis.getByteCount());

	}

	public static void main(String[] args) throws InterruptedException {
		String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		int gcPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_GC_PORT);
		long x = 3L;
		long y = 4L;
		ShareGenerator generator = new ShareGenerator(true);
		SCMPRankRunnable scmp = new SCMPRankRunnable();

		int counter = 0;
		while (counter < 1) {
			
			scmp.counter = counter;
			generator.generateSharedDataPoint(x);
			long x0 = generator.x0;
			long x1 = generator.x1;

			generator.generateSharedDataPoint(y);
			long y0 = generator.x0;
			long y1 = generator.x1;

			double e = System.nanoTime();
			System.out.println("starting...");
			String[][] argsSCMP = scmp.prepareArgs(x0, y0, x1, y1);
			String[] argeGen = argsSCMP[0];
			String[] argsEva = argsSCMP[1];
			//Thread.sleep(5000);
			scmp.run(gcPort+counter, argeGen, argsEva);// rank = 0 -> x<y
			long rank = scmp.rank;
			System.out.println("counter:" + counter + " rank:" + rank);

			double s = System.nanoTime();
			double time = s-e;
			System.out.println("time:"+time/1e9+" seconds");
			counter++;
		}

	}
}
