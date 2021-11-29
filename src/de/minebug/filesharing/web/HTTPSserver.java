package de.minebug.filesharing.web;


import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

public class HTTPSserver implements Runnable {
	private ServerSocket ss;
	public HTTPSserver(int port, String keyStore, String keyStore_password) {
		if(keyStore_password == null) {
			System.out.println("Unable to start HTTPS Server: Can't find password in config arguments!");
			return;
		}
		if(keyStore == null) {
			System.out.println("Warning on HTTPS Server: Can't find keystore File (*.jks) in config arguments. Using Program Directory.");
			keyStore = "./";
		}
		File ks = new File(keyStore);
		if(!ks.exists()){
			System.out.println("Unable to start HTTPS Server: Can't find keystore File (*.jks) in \"" + ks.getAbsolutePath() + "\"!");
			return;
		} else {
			if(ks.isDirectory()){
				for(File f : ks.listFiles()) {
					if(f.getName().toLowerCase().endsWith(".jks")) {
						ks = f;
						break;
					}
				}
				if(!ks.getName().toLowerCase().endsWith(".jks")){
					System.out.println("Unable to start HTTPS Server: Can't find keystore File (*.jks) in \"" + ks.getAbsolutePath() + "\"!");
					return;
				}
			}
		}
		System.setProperty("javax.net.ssl.keyStore", ks.getAbsolutePath());
	    System.setProperty("javax.net.ssl.keyStorePassword", keyStore_password);
	    System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
		try{
			SSLServerSocketFactory socketFactory = SSLContext.getDefault().getServerSocketFactory();
		    ss = socketFactory.createServerSocket(port);
			new Thread(this).start();
		}catch(Exception e){
			System.out.println("Unable to start HTTPS Server. Is the port already bound somewhere else and/or is your certificate outdated?");
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