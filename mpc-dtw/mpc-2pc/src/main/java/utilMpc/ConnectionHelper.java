package utilMpc;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import flexSC.network.Client;
import flexSC.network.Server;

public class ConnectionHelper {

	private int backupPort = Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_BACKUP_PORT);
	private int checkPort = 0;

	public void connect(final String hostname, final int port, final Server sndChannel, final Client rcvChannel) {
		// final String hostname =
		// Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		// final int port = 5554;
		// Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
		System.out.println("Connection| hostname:port, " + hostname + ":" + port);

		checkPort = port;
		if (!ConnectionHelper.available(port)) {
			for (int i = 0; i < 128; i++) {
				if (ConnectionHelper.available(backupPort + i)) {
					checkPort = backupPort+i;
					break;
				}
			}
		}

		// final Server sndChannel = new Server();
		// final Client rcvChannel = new Client();

		// Establish the connection
		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {

			@Override
			public void run() {
				// System.out.println("start listen:"+port);

				sndChannel.listen(checkPort);

				sndChannel.flush();

			}
		});

		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// System.out.println("start connect:"+port);
					rcvChannel.connect(hostname, checkPort);

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				rcvChannel.flush();
			}
		});

		// Connection should be established within 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
			}
		} catch (InterruptedException e) {
			// Something is wrong
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

	}

	public void connect(final String hostname, final int port, final int portClient, final Server sndChannel,
			final Client rcvChannel) {
		// final String hostname =
		// Config2PC.getSetting(Constants2PC.CONFIG2PC_SERVER_HOSTNAME);
		// final int port = 5554;
		// Config2PC.getSettingInt(Constants2PC.CONFIG2PC_SERVER_ARITHMETIC_PORT);
		//System.out.println("Connection| hostname:port, " + hostname + ":" + port + " client port:" + portClient);

		checkPort = port;
		if (!ConnectionHelper.available(port)) {
			for (int i = 0; i < 128; i++) {
				if (ConnectionHelper.available(backupPort + i)) {
					checkPort = backupPort+i;
					break;
				}
			}
		}
		// final Server sndChannel = new Server();
		// final Client rcvChannel = new Client();

		// Establish the connection
		ExecutorService exec = Executors.newFixedThreadPool(2);
		exec.execute(new Runnable() {

			@Override
			public void run() {
				// System.out.println("start listen:"+port);
				sndChannel.listen(checkPort);
				sndChannel.flush();

			}
		});

		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// System.out.println("start connect:"+port);
					rcvChannel.connect(hostname, checkPort, checkPort+(portClient-port));

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				rcvChannel.flush();
			}
		});

		// Connection should be established within 60s
		exec.shutdown();
		try {
			if (exec.awaitTermination(60, TimeUnit.SECONDS)) {
				// Execution finished
				exec.shutdownNow();
			}
		} catch (InterruptedException e) {
			// Something is wrong
			exec.shutdownNow();
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

	}

	/**
	 * Checks to see if a specific port is available.
	 *
	 * @param port the port to check for availability
	 */
	public static boolean available(int port) {
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("Invalid start port: " + port);
		}

		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} catch (IOException e) {
		} finally {
			if (ds != null) {
				ds.close();
			}

			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					/* should not be thrown */
				}
			}
		}

		return false;
	}

}
