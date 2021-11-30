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
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import de.minebug.filesharing.FileInfo;
import de.minebug.filesharing.FileSharing;

public class RequestHandler {

	public static void onGET(BufferedOutputStream os, String string, HashMap<String, String> uRL_args,
			HashMap<String, String> HTTP_body) throws IOException {

		String[] args = string.split("/");
		
		if (args.length == 0) {
			writeFile("html/upload.html", "text/html", os);
		}
		
		if (args.length == 2) {
			if (args[1].equalsIgnoreCase("favicon.ico")) {
				writeFile("images/logo.png", "image/png", os);
			}			
			else {
				FileInfo info = FileSharing.getFileManager().getFileInfo(args[1]);
				if (info == null) {
					writeFile("html/404.html", "text/html", os);
				} else {
					try {
						String downloadAddress = "localhost/download/" + info.getFileName() + "/" + info.getName();
						
						BufferedImage image = generateQRCodeImage(downloadAddress);
						ByteArrayOutputStream ios = new ByteArrayOutputStream();
						ImageIO.write(image, "png", ios);
						byte[] imageBytes = ios.toByteArray();
						byte[] encoded = java.util.Base64.getEncoder().encode(imageBytes);
						

						writeFile("html/download.html", "text/html", os, 
								"$key$", info.getFileName(),
								"$qrcode$", ("<img class=\"qrCode\" alt=\"\" src=\"data:image/png;base64, " + new String(encoded) + "\" />"),
								"$name$", info.getName());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}				
			}
			
		} else if (args.length == 3) {
			if (args[1].equalsIgnoreCase("images")) {
				writeFile("images/"+args[2], "image/png", os);
			}

			else if (args[1].equalsIgnoreCase("styles")) {
				writeFile("html/"+args[2]+".css", "text/css", os);
			}

			else if (args[1].equalsIgnoreCase("scripts")) {
				writeFile("html/"+args[2]+".js", "text/javascript", os);
			}
			
			
		} else if (args.length == 4) {
			if (args[1].equalsIgnoreCase("download")) {
				String key = args[2];
				
				FileInfo info;
				if ((info = FileSharing.getFileManager().getFileInfo(key)) == null)
					writeFile("html/404.html", "text/html", os);
				else {
					writeHeader("application/octet-stream", info.getSize(), os);
					FileSharing.getFileManager().getFile(key, os);
				}
			}
		}
		
	}

	public static void onPOST(BufferedOutputStream os, String string, HashMap<String, String> uRL_args,
			HashMap<String, String> hTTP_body, HashMap<String, String> pOST_args) throws IOException {
		
	}

	public static void onPOST_multipart(BufferedOutputStream os, String string, HashMap<String, String> URL_args, HashMap<String, String> HTTP_body, BufferedInputStream is) throws IOException {
		System.out.println(HTTP_body);
		String token = HTTP_body.get("content-type").split("boundary\\=")[1].split(";")[0];
//		System.out.println("token = " + token);
		AtomicLong expectedDataLen = new AtomicLong(Long.parseLong(HTTP_body.get("content-length")));


		String token2 = new String(readUntil(is, "\r\n".getBytes(), expectedDataLen));
		System.out.println("token3:" + token2);
		
		readUntil(is, " filename=\"".getBytes(), expectedDataLen);
		String filename = new String(readUntil(is, "\"".getBytes(), expectedDataLen));
		
		readUntil(is, "Content-Type: ".getBytes(), expectedDataLen);
		String contentType = new String(readUntil(is, "\n".getBytes(), expectedDataLen));
		
		readUntil(is, "\n".getBytes(), expectedDataLen);
		System.out.println(expectedDataLen.get());
		
		
		expectedDataLen.getAndAdd(-(token.length() + 8));
		
		System.out.println("writing");
		
		String key = FileSharing.getFileManager().addFile(
				is,
				filename,
				("\r\n"+token2).getBytes(),
				contentType,
				new Timestamp(System.currentTimeMillis()+(1000*60*60*24)),
				expectedDataLen.get());
		
		
		
//		os.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: "+result.length+"\r\nConnection: Close\r\n\r\n").getBytes());
//		os.write(result);
		
		writeFile("html/redirect.html", "text/html", os,
				"$key$", key);
		
		os.flush();
		os.close();
	}
	
	
	
	
	private static byte[] readUntil(BufferedInputStream is, byte[] word, AtomicLong l) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int c;
		int pointer = 0;
		while(is.available() > 0) {
			c = is.read();
			l.decrementAndGet();
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
			}
			baos.write(c);
		}
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
	
	private static void writeHeader(String contentType, long l, OutputStream os) throws IOException {
		os.write((""
				+ "HTTP/1.1 200 OK\r\n"
				+ "Content-Type: "+contentType+"\r\n"
				+ "Content-Length: "+l+"\r\n"
				+ "\r\n").getBytes());
	}
	
	private static void writeFile(String path, String type, OutputStream os) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
		byte[] data = bis.readAllBytes();
		bis.close();

		writeHeader(type, data.length, os);
		os.write(data);
	}
	
	private static void writeFile(String path, String type, OutputStream os, String... replacements) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
		byte[] data = bis.readAllBytes();
		bis.close();
		String s = new String(data);
		for (int i = 0; i < replacements.length-1; i +=2) {
			s = s.replace(replacements[i], replacements[i+1]);
		}
		data = s.getBytes();
		writeHeader(type, data.length, os);
		os.write(data);
	}
	
	public static BufferedImage generateQRCodeImage(String barcodeText) throws Exception {
	    QRCodeWriter barcodeWriter = new QRCodeWriter();
	    BitMatrix bitMatrix = 
	      barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 200, 200);
	    
	    return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}
	
}
