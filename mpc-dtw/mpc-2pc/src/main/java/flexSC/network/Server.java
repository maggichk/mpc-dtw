package flexSC.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Network {
	
	public void listen(int port, boolean isFlexSC) {
		try {
			serverSock = new ServerSocket(port);
			sock = serverSock.accept(); // wait for client to connect

			os = new BufferedOutputStream(sock.getOutputStream());
			is = new BufferedInputStream(sock.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // create socket and bind to port
	}
	
	public void disconnectServer() throws Exception {
		os.flush();		
		serverSock.close();
	}

	public void listen(int port) {
		try {
			//serverSock = new ServerSocket(port);
			
			serverSock = new ServerSocket(port);
			serverSock.setReuseAddress(true);
			//System.out.println("server port:"+port);
			
			//serverSock.bind(new InetSocketAddress("localhost", port));			
			sock = serverSock.accept(); // wait for client to connect
			
			os = new BufferedOutputStream(sock.getOutputStream());
			is = new BufferedInputStream(sock.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // create socket and bind to port
	}
	
	
}
