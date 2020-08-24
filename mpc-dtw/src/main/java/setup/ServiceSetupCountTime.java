package setup;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.rmit.maggie.mpc_dtw.log4jExample;

import additive.MultiplicationTriple;
import additive.ShareGenerator;
import common.parser.WriteFile;
import common.util.Config;
import common.util.Constants;
import flexSC.flexsc.Flag;
import flexSC.network.Client;
import flexSC.network.Server;
import utilMpc.Config2PC;
import utilMpc.ConnectionHelper;
import utilMpc.Constants2PC;

public class ServiceSetupCountTime {

	private static final String hostname = Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
	private static final int portBase = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
	private static double time;
	private static long cosByte;
	private static long cisByte;
	private static final int threadNum = 300;
	static Logger log = Logger.getLogger(ServiceSetupCountTime.class.getName());
	
	public static void main(String[] args) {
		//generate MTs to file
		//int counter = 3000;
		int counter = Config.getSettingInt(Constants.CONFIG_MT_NUM);	
		
		
		//String fileNameIn = Config.getSetting(Constants.CONFIG_MT_FILE_NAME_IN);
		/*final String fileNameOutBase = Config.getSetting(Constants.CONFIG_MT_FILE_NAME_OUT);
		final String filePath = Config.getSetting(Constants.CONFIG_MT_PATH);
		final String separator = Config.getSetting(Constants.CONFIG_MT_SEPARATOR);
		System.out.println("Separator: ["+separator+"]");
		
		final WriteFile writeFile = new WriteFile();
		final boolean isLastLine = false;
		*/
		double e = System.nanoTime();
		Flag.sw.startTotal();
		for(int j=0; j<counter/threadNum; j++) {
			System.out.println("1) j:"+j);
			//final String fileNameOut = j+fileNameOutBase;
			int i = 0;		
			ArrayList<Runnable> tasks = new ArrayList<Runnable>();
			ExecutorService exec = Executors.newFixedThreadPool(threadNum);
			
			while(i<threadNum) {
				//System.out.println("2) i"+i);
				final int port = portBase+i;
				final int portClient = portBase+i+threadNum;
				Runnable task = new Runnable() {
					@Override
					public void run() {
						Server sndChannel = new Server();
						Client rcvChannel = new Client();						
						ShareGenerator shr = new ShareGenerator(true);

						ConnectionHelper conn = new ConnectionHelper();
						//System.out.println("port:"+port+" start establishing");
						conn.connect(hostname, port, portClient, sndChannel, rcvChannel);
						
						//System.out.println("port:"+port+" established");
						MultiplicationTriple mt = new MultiplicationTriple(true, shr, sndChannel, rcvChannel);
						Flag.sw.stopTotal();
						if(Flag.countIO) {
							cosByte += rcvChannel.cos.getByteCount();
							cisByte += rcvChannel.cis.getByteCount();
						}
						
						//System.out.println("disconnect:" + port);

						// write to file
						/*String line = mt.tripleA0 + separator + mt.tripleA1 + separator + mt.tripleB0 + separator
								+ mt.tripleB1 + separator + mt.tripleC0 + separator + mt.tripleC1;
						writeFile.writeFile(filePath, fileNameOut, line, isLastLine);
						*///System.out.println("write file");
					}
					
					

				};
				tasks.add(task);
				i++;
			}
			
			//System.out.println("begin");
			for(int z = 0; z<tasks.size(); z++) {
				//exec.execute(tasks.get(z));
				exec.submit(tasks.get(z));
			}
			//shutdown
			// Connection should be established within 60s
			exec.shutdown();
			try {
				if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
					// Execution finished
					exec.shutdownNow();
				}
			} catch (InterruptedException e1) {
				// Something is wrong
				exec.shutdownNow();
				Thread.currentThread().interrupt();
				throw new RuntimeException(e1);
			}
			//System.out.println("finish");
			
		}
		
		double s = System.nanoTime();
		time = s-e;
		System.out.println("Number of MTs:"+counter+" total time:"+time/1e9);
		System.out.println("send from client:"+ cosByte/ 1024.0
				/ 1024.0 + "MB\n" + "Data Sent to Client :"+ cisByte/ 1024.0
				/ 1024.0 + "MB\n");
		log.info("------------------ServiceSetupCountTime------------------");
		log.info("Number of MTs:"+counter+" total time:"+time/1e9);
		log.info("send from client:"+ cosByte/ 1024.0
				/ 1024.0 + "MB\n" + "Data Sent to Client :"+ cisByte/ 1024.0
				/ 1024.0 + "MB\n");
		
	}
}
