package flexSC.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;

import flexSC.flexsc.Flag;

public class Client extends Network {

	public CountingOutputStream cos;
	public CountingInputStream cis;
	
	public void disconnectCli() throws Exception  {
		os.flush();
		
		sock.close();
	}
	public void connect(String server, int portServer, int portClient) throws InterruptedException {
		try{
			while (true) {
				try {
					InetAddress addr = InetAddress.getByName(server);
					sock = new java.net.Socket(server, portServer, addr, portClient);
					
					sock.setReuseAddress(true);
					sock.setSoLinger(true, 0);
					
					//sock.setTcpNoDelay(true);
					//sock.setSoTimeout(0);
					//sock.setKeepAlive(false);
					
					//sock.bind(new InetSocketAddress(server, portClient));
					//sock.connect(new InetSocketAddress(server, portServer));
					
					if (sock != null)
						break;
				} catch(IOException e){
					Thread.sleep(10);
				}
			}
			if (Flag.countIO) {
				cos = new CountingOutputStream(sock.getOutputStream());
				cis = new CountingInputStream(sock.getInputStream());
				os = new BufferedOutputStream(cos);
				is = new BufferedInputStream(cis);
			} else {
				os = new BufferedOutputStream(sock.getOutputStream());
				is = new BufferedInputStream(sock.getInputStream());

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void connect(String server, int port) throws InterruptedException {
		try{
			while (true) {
				try {
					sock = new java.net.Socket(server, port); // create socket 					
					//sock.setSoLinger(true, 0);
					sock.setReuseAddress(true);
					//sock.setTcpNoDelay(true);
					//sock.setSoTimeout(0);
					//sock.setKeepAlive(false);
					
					
					if (sock != null)
						break;
				} catch(IOException e){
					Thread.sleep(10);
				}
			}
			if (Flag.countIO) {
				cos = new CountingOutputStream(sock.getOutputStream());
				cis = new CountingInputStream(sock.getInputStream());
				os = new BufferedOutputStream(cos);
				is = new BufferedInputStream(cis);
			} else {
				os = new BufferedOutputStream(sock.getOutputStream());
				is = new BufferedInputStream(sock.getInputStream());

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printStatistic() {
		if (Flag.countIO) {
			System.out.println("\n********************************\n"
					+ "Data Sent from Client :" + cos.getByteCount() / 1024.0
					/ 1024.0 + "MB\n" + "Data Sent to Client :"
					+ cis.getByteCount() / 1024.0 / 1024.0 + "MB"
					+ "\n********************************");
		}
	}
}
