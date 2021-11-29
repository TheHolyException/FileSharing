package de.minebug.filesharing.web;

import java.io.IOException;
import java.net.ServerSocket;

public class HTTPserver implements Runnable {
	ServerSocket ss;
	
	public HTTPserver(int port) {
		try{
			ss = new ServerSocket(port);
			new Thread(this).start();
		}catch(Exception e){
			System.out.println("Unable to start HTTP Server. Is the port already bound somewhere else?");
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while(true){
			try {
				new Connection(ss.accept());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
