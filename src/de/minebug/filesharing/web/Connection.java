package de.minebug.filesharing.web;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;


public class Connection implements Runnable {
	private Socket socket;
	private BufferedInputStream is;
	private BufferedOutputStream os;
	private ByteArrayOutputStream readLineBuffer = new ByteArrayOutputStream(32);
	public Connection(Socket socket) {
		try{
			this.socket = socket;
			this.is = new BufferedInputStream(socket.getInputStream(), 4096);
			this.os = new BufferedOutputStream(socket.getOutputStream(), 8192);
			new Thread(this).start();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void run() {
		try{
			String[] http_destination = readLine().split(" ");
			//[0] == GET | POST
			//[1] == destination
			//[2] == HTTP-verstion
			System.out.println(Arrays.deepToString(http_destination));
			boolean isPost = http_destination[0].equalsIgnoreCase("POST");
			if(http_destination[0].equalsIgnoreCase("GET") || isPost){
				String[] a = http_destination[1].indexOf('?') == -1 ? new String[]{http_destination[1], null} : http_destination[1].split("\\?");
				HashMap<String, String> URL_args = readHTTP_URL_arguments(a[1]);
				HashMap<String, String> HTTP_body = readHTTP_body();
				if(!isPost){
					RequestHandler.onGET(os, a[0], URL_args, HTTP_body);
				} else {
					if(HTTP_body.containsKey("content-type") && HTTP_body.get("content-type").startsWith("multipart/form-data;")){
						RequestHandler.onPOST_multipart(os, a[0], URL_args, HTTP_body, is);
					} else {
						byte[] data;
						if(HTTP_body.containsKey("content-length")){
							int len = Integer.parseInt(HTTP_body.get("content-length"));
							data = new byte[len];
							new DataInputStream(is).readFully(data);
						} else {
							int l;
							byte[] buffer = new byte[4096];
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							while((l = is.read(buffer)) != -1) baos.write(buffer, 0, l);
							data = baos.toByteArray();
						}
						HashMap<String, String> POST_args = readHTTP_URL_arguments(new String(data));
						RequestHandler.onPOST(os, a[0], URL_args, HTTP_body, POST_args);
					}
				}
			}
			os.flush();
//			Thread.sleep(1000);
			os.close();
			is.close();
			socket.close();
		}catch(Exception e){
			e.printStackTrace();
			try {
				os.write(("HTTP/1.1 400 Bad Request\r\nContent-Type: text/html\r\n\r\nThe Server was unable to parse or handle your http request.").getBytes());
				os.flush();
//				Thread.sleep(1000);
				os.close();
				is.close();
				socket.close();
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}
	}
	
	private String readLine() throws IOException {
		int chr;
		ByteArrayOutputStream baos = this.readLineBuffer;
		BufferedInputStream is = this.is;
		//diese unscheinbraen zeilen optimieren den pogrammfluss, weil
		// 1. weniger bytes im bytecode stehen und 
		// 2. es sich in den ersten 4 slots sich befinden, welche mit einem byte im bytecode abgerudfen werden können.
		while(((chr = is.read()) != '\n') && chr != -1){
			baos.write(chr);
		}
		if(baos.size() == 0) return "";//null;
		String out = new String(baos.toByteArray());
		baos.reset(); // recyclen des schreib-puffers um heap zu sparen.
		if(out.charAt(out.length() - 1) == '\r') return out.substring(0, out.length() - 1);
		return out;
	}
	
	@SuppressWarnings("deprecation")
	public static HashMap<String, String> readHTTP_URL_arguments(String meta) throws IOException {
		HashMap<String, String> out = new HashMap<String, String>();
		if(meta == null) return out;
		String[] args = meta.split("&");
		if (args != null) {
			if (args.length > 0) {
				for (String arg : args) {
					String[] subArgs = _split(arg, "=");
					if (subArgs.length > 1) {
						out.put(subArgs[0], URLDecoder.decode(subArgs[1]));
					}
				}
			}
		}
		return out;
	}
	
	private HashMap<String, String> readHTTP_body() throws IOException {
		HashMap<String, String> out = new HashMap<String, String>();
		String row;
		while((row = readLine()) != null && row.length() > 0) {
			String[] subArgs = _split(row, ":");
			out.put(subArgs[0].toLowerCase(), subArgs[1].trim());
		}
		return out;
	}
	
	public static String[] _split(String src, String filter) {
		int i = 0;
		int last = 0;
		int len = 1;
		while ((i = src.indexOf(filter, last)) != -1) {
			len++;
			last = i + filter.length();
		}
		String[] out = new String[len];
		last = 0;
		len = 0;
		while ((i = src.indexOf(filter, last)) != -1) {
			out[len] = src.substring(last, i);
			last = i + filter.length();
			len++;
		}
		out[len] = src.substring(last, src.length());
		return out;
	}
}