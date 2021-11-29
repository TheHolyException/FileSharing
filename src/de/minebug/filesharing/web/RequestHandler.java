package de.minebug.filesharing.web;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.HashMap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import de.minebug.filesharing.FileSharing;
import me.kaigermany.utilitys.data.Pair;

public class RequestHandler {

	public static void onGET(BufferedOutputStream os, String string, HashMap<String, String> uRL_args,
			HashMap<String, String> HTTP_body) throws IOException {
		

//		System.out.println(string);
//		System.out.println(uRL_args);
//		System.out.println(hTTP_body.toString().replace(", ", ",\n"));

//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		System.out.println(string);
		
		String[] args = string.split("/");
		
		if (args.length == 0) {
			writeFile("html/upload.html", "text/html", os);
		}
		
		if (args.length == 2) {
			switch(string) {				
				case "/favicon.ico": {
					writeFile("images/logo.png", "image/png", os);
					break;
				}
				
				default: {
					String key = string.substring(1, string.length());
					Pair<String, BufferedInputStream> fileData = FileSharing.getFileManager().getFile(key);
					if (fileData == null) {
						System.out.println("nope");
						writeDefaultTemplate_404(os);
						break;
					}
					
//					baos.write("dingens".getBytes());
					
					byte[] buffer = new byte[1024];
					int l;
					while((l = fileData.getSecond().read(buffer)) != -1) os.write(buffer);
					
//					baos.write("done".getBytes());
//					byte[] result = baos.toByteArray();
//					os.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: "+result.length+"\r\nConnection: Close\r\n\r\n").getBytes());
					/*
					try {
						System.out.println("s");
						BufferedImage image = generateQRCodeImage("https://minebug.de/"+key);
						ByteArrayOutputStream ios = new ByteArrayOutputStream();
						ImageIO.write(image, "png", ios);
//						writeImageHeader(os, ios.toByteArray());
						
						byte[] imageBytes = ios.toByteArray();
						String encoded = java.util.Base64.getEncoder().encodeToString(imageBytes);
						
						String s = "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";
						baos.write("test".getBytes());
						baos.write(("<html><body><img alt=\"\" src\"data:image/png;base64, " + s + "\" /></body></html>").getBytes());

						ios.close();
						
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					*/
					break;
				}		
			}
			
		} else if (args.length == 3) {
			if (args[1].equalsIgnoreCase("images")) {
				writeFile("images/"+args[2], "image/png", os);
			}
			
			if (args[1].equalsIgnoreCase("styles")) {
				writeFile("html/"+args[2]+".css", "text/css", os);
			}
		}
		
		
//		byte[] result = baos.toByteArray();
//		os.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: "+result.length+"\r\nConnection: Close\r\n\r\n").getBytes());
//		os.write(result);
		
	}

	public static void onPOST(BufferedOutputStream os, String string, HashMap<String, String> uRL_args,
			HashMap<String, String> hTTP_body, HashMap<String, String> pOST_args) throws IOException {
		
	}

	public static void onPOST_multipart(BufferedOutputStream os, String string, HashMap<String, String> URL_args, HashMap<String, String> HTTP_body, BufferedInputStream is) throws IOException {
		String token = HTTP_body.get("content-type").split("boundary\\=")[1].split(";")[0];
		System.out.println("token = " + token);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		String token2 = new String(readUntil(is, "\n".getBytes()));
//		String token2 = new String(readUntil(is, (/*"--"+*/token).getBytes()));
		System.out.println("token3:" + token2);
		
		readUntil(is, " filename=\"".getBytes());
		String filename = new String(readUntil(is, "\"".getBytes()));
		
		readUntil(is, "Content-Type: ".getBytes());
		String contentType = new String(readUntil(is, "\n".getBytes()));
		
		readUntil(is, "\n".getBytes());
//		String data = new String(readUntil(is, ("\n"+token2).getBytes()));
		
//		FileSharing.getFileManager().addFile(is, "");
		System.out.println("Filename: " + filename);
		System.out.println("Content-Type: " + contentType);
//		System.out.println("data: " + data);
		
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		System.out.println(timestamp);
		String key = FileSharing.getFileManager().addFile(is, filename, ("\r\n"+token2).getBytes(), null);
		
		System.out.println(key);
		
		baos.write(("https://transfer.minebug.de/"+key).getBytes());
		
		
		byte[] result = baos.toByteArray();//token.getBytes();
		os.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: "+result.length+"\r\nConnection: Close\r\n\r\n").getBytes());
		os.write(result);
		os.flush();
		os.close();
	}
	
	
	
	
	private static byte[] readUntil(BufferedInputStream is, byte[] word) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int c;
		int pointer = 0;
		while(is.available() > 0) {
//			System.out.println("is.available() = " + is.available());
			c = is.read();
			if(c == -1) break;
			if (c == word[pointer]) {
				pointer++;
				if(pointer == word.length) break;
				continue;
			} else {
				baos.write(word, 0, pointer);
				if(c == word[0]) {
					pointer = 1;
					continue;
				} else {
					pointer = 0;
				}
				//pointer = c == word[0] ? 1 : 0;
			}
			baos.write(c);
		}

//		System.out.println("is.available() = " + is.available());
		return baos.toByteArray();
	}
	

	private static void writeDefaultTemplate_403(OutputStream os) throws IOException {
		os.write(("HTTP/1.1 403 Forbidden\r\nContent-Type: text/html\r\nConnection: Close\r\n\r\nYou are not allowed to view this Ressource.").getBytes());
	}
	private static void writeDefaultTemplate_404(OutputStream os) throws IOException {
		os.write(("HTTP/1.1 404 Not Found\r\nContent-Type: text/html\r\nConnection: Close\r\n\r\nThe Server can't find the requested Ressource.").getBytes());
	}
	private static void writeDefaultTemplate_429(OutputStream os) throws IOException {
		os.write(("HTTP/1.1 429 Too Many Requests\r\nContent-Type: text/html\r\nConnection: Close\r\n\r\nThe Server does not allow that many requests.").getBytes());
	}
	private static void writeDefaultTemplate_501(OutputStream os) throws IOException {
		os.write(("HTTP/1.1 501 Not Implemented\r\nContent-Type: text/html\r\nConnection: Close\r\n\r\nThe Server does not support the requested API method.").getBytes());
	}
	
	private static void writeImageHeader(OutputStream os, byte[] image) throws IOException {
		os.write("HTTP/1.1 200 OK\r\n".getBytes());
		os.write("Date: Mon, 23 May 2005 22:38:34 GMT\r\n".getBytes());
		os.write("Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)\r\n".getBytes());
		os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\r\n".getBytes());
		os.write(("Content-Length: " + image.length + "\r\n").getBytes());
		os.write("Content-Type: image/png\r\n".getBytes());
		os.write("\r\n".getBytes());
	}
	
	
	private static void writeFile(String path, String type, OutputStream os) {
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
			byte[] data = bis.readAllBytes();
			bis.close();

			os.write((""
					+ "HTTP/1.1 200 OK\r\n"
					+ "Content-Type: "+type+"\r\n"
					+ "Content-Length: "+data.length+"\r\n"
					+ "Connection: Close\r\n"
					+ "\r\n").getBytes());
			os.write(data);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	@Deprecated
	private static void writeHTMLFile(String path, OutputStream os) {
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream("./html/"+path+".html"));
			byte[] data = bis.readAllBytes();
			bis.close();

			os.write((""
					+ "HTTP/1.1 200 OK\r\n"
					+ "Content-Type: text/html\r\n"
					+ "Content-Length: "+data.length+"\r\n"
					+ "Connection: Close\r\n"
					+ "\r\n").getBytes());
			os.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Deprecated
	private static void writeCSSFile(String path, OutputStream os) {
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream("./html/"+path+".css"));
			byte[] data = bis.readAllBytes();
			bis.close();			

			os.write((""
					+ "HTTP/1.1 200 OK\r\n"
					+ "Content-Length: " + data.length + "\r\n"
					+ "Content-Type: text/css\r\n"
					+ "\r\n").getBytes());
			os.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Deprecated
	private static void sendImage(String path, OutputStream os) {
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream("./images/"+path));
			byte[] data = bis.readAllBytes();		
			bis.close();

			os.write((""
					+ "HTTP/1.1 200 OK\r\n"
					+ "Content-Length: " + data.length + " \r\n"
					+ "Content-Type: image/png\r\n"
					+ "\r\n").getBytes());
			os.write(data);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public static BufferedImage generateQRCodeImage(String barcodeText) throws Exception {
	    QRCodeWriter barcodeWriter = new QRCodeWriter();
	    BitMatrix bitMatrix = 
	      barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 200, 200);
	    
	    return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}
	
}
