package de.minebug.filesharing.web;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import de.minebug.filesharing.FileInfo;
import de.minebug.filesharing.FileSharing;
import de.minebug.filesharing.util.AutoScaleDriver_v2;
import de.minebug.filesharing.util.Render2DEngine_NoGL;

public class RequestHandler {

	private static Random random = new Random();
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	private static boolean debugEnabled = Boolean.parseBoolean(FileSharing.getConfig().getProperty("debug"));
	private static String hostname = FileSharing.getConfig().get("host").toString();
	
	public static void onGET(BufferedOutputStream os, String string, HashMap<String, String> uRL_args,
			HashMap<String, String> HTTP_body) throws IOException {

		if (debugEnabled)
			System.out.println(HTTP_body);
		
		String[] args = string.split("/");
		
		if (args.length == 0) {
			writeFile("html/upload.html", "text/html", os,
					"$host$", hostname);
		}
		
		if (args.length == 2) {
			if (args[1].equalsIgnoreCase("favicon.ico")) {
				writeFile("images/logo.png", "image/png", os);
			}			
			else {
				FileInfo info = FileSharing.getFileManager().getFileInfo(args[1]);
				if (info == null) {
					writeFile("html/404.html", "text/html", os,
							"$host$", hostname);
				} else {
					try {
						String downloadAddress = hostname+"/download/" + info.getKey() + "/" + info.getName();
						
						BufferedImage image = generateQRCodeImage(downloadAddress);
						
						
						/*
						Render2DEngine_NoGL icon = new Render2DEngine_NoGL((BufferedImage)ImageIO.read(
								//new SefsInputStream(MainController.getDrive(file.getDrive()), file.getPath()))
//								FileSharing.getFileManager().getFile(info.getKey())
								new File("./images/logo.png")
						));
						Render2DEngine_NoGL out = new Render2DEngine_NoGL(image.getWidth() / 6, image.getWidth() / 6);
						//out.setEnableAlphaSupport(false);
						AutoScaleDriver_v2 asd = new AutoScaleDriver_v2(icon);
						asd.onResize(out.getWidth(), out.getHeight());
						asd.onRender(out);
						
						Render2DEngine_NoGL renderBuffer = new Render2DEngine_NoGL(image);
						renderBuffer.drawImage(out, (image.getWidth() / 2) - (out.getWidth() / 2), (image.getHeight() / 2) - (out.getHeight() / 2));
						image = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
						image.setRGB(0, 0, image.getWidth(), image.getHeight(), renderBuffer.getRaw(), 0, renderBuffer.getWidth());
						//image.setRGB(0, 0, out.getWidth(), out.getHeight(), out.getRaw(), 0, out.getWidth());
						*/
						
						
						ByteArrayOutputStream ios = new ByteArrayOutputStream();
						ImageIO.write(image, "png", ios);
						byte[] imageBytes = ios.toByteArray();
						byte[] encoded = java.util.Base64.getEncoder().encode(imageBytes);
						

						writeFile("html/download.html", "text/html", os, 
								"$key$", info.getKey(),
								"$qrcode$", ("<img class=\"qrCode\" alt=\"\" src=\"data:image/png;base64, " + new String(encoded) + "\" />"),
								"$name$", info.getName(),
								"$host$", hostname,
								"$size$", readableFileSize(info.getSize()),
								"$valid$", dateFormat.format(new Date(info.getAvailable().getTime()))
								);
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
				writeFile("html/"+args[2]+".css", "text/css", os,
						"$rnd$", ""+(random.nextInt(4)+1),
						"$host$", hostname);
			}

			else if (args[1].equalsIgnoreCase("scripts")) {
				writeFile("html/"+args[2]+".js", "text/javascript", os,
						"$host$", FileSharing.getConfig().get("host").toString());
			}
			
			
		} else if (args.length == 4) {
			if (args[1].equalsIgnoreCase("download")) {
				String key = args[2];
				
				FileInfo info;
				if ((info = FileSharing.getFileManager().getFileInfo(key)) == null)
					writeFile("html/404.html", "text/html", os,
							"$host$", hostname);
				else {
//					writeHeader("application/octet-stream", info.getSize(), os);
					writeDownloadHeader(info.getName(), info.getSize(), os);
					FileSharing.getFileManager().getFile(key, os);
				}
			}
		}
		
	}

	public static void onPOST(BufferedOutputStream os, String string, HashMap<String, String> uRL_args,
			HashMap<String, String> hTTP_body, HashMap<String, String> pOST_args) throws IOException {
		
	}
	
	private static HashMap<String, String> readHTTP_body(InputStream is, AtomicLong expectedDataLen) throws IOException {
		//DataInputStream dis = new DataInputStream(is);
		HashMap<String, String> out = new HashMap<String, String>();
		String row;
		while((row = readLine(is, expectedDataLen)) != null && row.length() > 2) {
			String[] subArgs = Connection._split(row, ":");
			out.put(subArgs[0].toLowerCase(), subArgs[1].trim());
		}
		return out;
	}
	
	private static String readLine(InputStream is, AtomicLong expectedDataLen) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
		int chr;
		while((chr = is.read()) != '\n') baos.write(chr);
		expectedDataLen.addAndGet(-(baos.size() + 1));
		return new String(baos.toByteArray()).trim().replace("\r", "");
	}

	public static void onPOST_multipart(BufferedOutputStream os, String string, HashMap<String, String> URL_args, HashMap<String, String> HTTP_body, BufferedInputStream is) throws IOException {
		
		if (debugEnabled) 
			System.out.println(HTTP_body);
		String token = HTTP_body.get("content-type").split("boundary\\=")[1].split(";")[0];
//		System.out.println("token = " + token);
		AtomicLong expectedDataLen = new AtomicLong(Long.parseLong(HTTP_body.get("content-length")));
		
		
		String token2 = new String(readUntil(is, "\r\n".getBytes(), expectedDataLen));
//		System.out.println("token3:" + token2);
		
		HashMap<String, String> postBody = readHTTP_body(is, expectedDataLen);
/*
		
		
		readUntil(is, " filename=\"".getBytes(), expectedDataLen);
		String filename = new String(readUntil(is, "\"".getBytes(), expectedDataLen));
		
		readUntil(is, "Content-Type: ".getBytes(), expectedDataLen);
		String contentType = new String(readUntil(is, "\n".getBytes(), expectedDataLen));
		
		readUntil(is, "\n".getBytes(), expectedDataLen);
		System.out.println(expectedDataLen.get());
		*/
		System.out.println("postBody : " + postBody);
		//{content-disposition=form-data; name="files"; filename="test_file (1).txt",
		// content-type=text/plain}
		ByteArrayInputStream is2 = new ByteArrayInputStream(postBody.get("content-disposition").getBytes());
		AtomicLong dummy = new AtomicLong();
		readUntil(is2, " filename=\"".getBytes(), dummy);
		String filename = new String(readUntil(is2, "\"".getBytes(), dummy));
		//is2 = new ByteArrayInputStream(postBody.get("Content-Type").getBytes());
		//readUntil(is, "Content-Type: ".getBytes(), expectedDataLen);
		String contentType = postBody.get("content-type");//new String(readUntil(is, "\n".getBytes(), expectedDataLen));

		filename = filename.replace("$", "_");
		
		expectedDataLen.getAndAdd(-(2 + 2 + token.length() + 2 + 2));
		
		String key = FileSharing.getFileManager().addFile(
				is,
				filename,
				contentType,
				new Timestamp(System.currentTimeMillis()+(1000*60*60*24)),
				expectedDataLen.get());
		
		
		
//		os.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: "+result.length+"\r\nConnection: Close\r\n\r\n").getBytes());
//		os.write(result);
		
		writeFile("html/redirect.html", "text/html", os,
				"$key$", key);
		
		try { Thread.sleep(200); } catch (Exception ex) { ex.printStackTrace(); }
		
		os.flush();
		os.close();
	}
	
	
	
	
	private static byte[] readUntil(InputStream is, byte[] word, AtomicLong l) throws IOException {
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
	
	private static void writeDownloadHeader(String fileName, long l, OutputStream os) throws IOException {
		os.write((""
				+ "HTTP/1.1 200 OK\r\n"
				+ "Content-Type: application/octet-stream\r\n"
				+ "Content-Disposition: attachment; filename=" + fileName + "\r\n"
				+ "Content-Length: "+l+"\r\n"
				+ "\r\n").getBytes());
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
	      barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 400, 400);
	    
	    return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}
	
	public static String readableFileSize(long size) {	
	    if(size <= 0) return "0";
	    final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
	    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
	    return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
	
}
