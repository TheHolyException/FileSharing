package de.minebug.filesharing.filemanagers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.minebug.filesharing.FileSharing;
import de.minebug.filesharing.util.FileInfo;
import de.minebug.filesharing.util.StreamConverter;
import de.theholyexception.holyapi.datastorage.dataconnection.DataBaseInterface;
import de.theholyexception.holyapi.util.exceptions.NotImplementedException;

public class FileManagerSQL implements FileInterface {

	private DataBaseInterface dataBaseInterface;
	
	public FileManagerSQL(DataBaseInterface dataBaseInterface) {
		this.dataBaseInterface = dataBaseInterface;
		
		dataBaseInterface.execute("""
				CREATE TABLE IF NOT EXISTS `data` (
					`szKey` VARCHAR(8) NOT NULL COLLATE 'utf8mb3_general_ci',
					`nOffset` INT(11) UNSIGNED NOT NULL,
					`nDataLength` INT(11) UNSIGNED NOT NULL,
					`szData` TEXT NOT NULL COLLATE 'utf8mb3_general_ci',
					PRIMARY KEY (`szKey`, `nOffset`) USING BTREE,
					INDEX `nOffset` (`nOffset`) USING BTREE
				)
				COLLATE='utf8mb3_general_ci'
				ENGINE=InnoDB
				;
				""");
		
		try {
			Thread t = new Thread(new Runnable() {			
				@Override
				public void run() {
					try {
						checkFileSystem();
						while(true) {
							update();
							try {
								Thread.sleep(60000); 
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			});
			t.setName("FileManager Background Tasks");
			t.start();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new IllegalStateException("Failed to initialize FileManager");
		}
		
	}
	
	@Override
	public FileInfo getFileInfo(String key) throws IOException {
		try {
			
			ResultSet result = dataBaseInterface.executeQuerySafe("""
					SELECT szKey, szContentType, szFilename, nContentLength, tValid FROM files
					WHERE tValid > CURRENT_TIMESTAMP() AND szKey=?;
					""", key);
			if (!result.first()) return null;
			return new FileInfo(
					key, 
					result.getLong("nContentLength"), 
					result.getString("szFilename"), 
					result.getString("szContentType"), 
					result.getTimestamp("tValid"));	
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public void getFile(String key, BufferedOutputStream bos) throws IOException {
		
		ResultSet result = dataBaseInterface.executeQuerySafe("""
				SELECT nOffset, szData, nDataLength FROM data WHERE szKey=? ORDER BY nOffset ASC
				""", key);
		
		System.out.println("getFile()");
		try {
			long contentLength = getFileInfo(key).getSize();
			byte[] buffer = new byte[32768];
			
			//StreamConverter gzToOut = new StreamConverter(1 << 20);
			StreamConverter dbToGz = new StreamConverter(1 << 20);
			
			GZIPInputStream gzDecoder = new GZIPInputStream(dbToGz.getInputStream());
			new Thread(()->{
				try {
					byte[] cache = new byte[1 << 12];
					int l;
					while((l = gzDecoder.read(cache)) != -1) bos.write(cache, 0, l);
				}catch (Exception e) {e.printStackTrace();}
			}).start();
			//gzDecoder.read() -> bos;
			//inStream <- result.getString("szData").getBytes();
			OutputStream os = dbToGz.getOutputStream();
			while (result.next()) {
				int maxLen = buffer.length;
				if (contentLength < maxLen) maxLen = (int) contentLength;
				int l = result.getInt("nDataLength");
				buffer = result.getString("szData").getBytes();
				buffer = Base64.getDecoder().decode(buffer);
				os.write(buffer, 0, l = buffer.length);
				System.out.println("result.next(): " + l);
				contentLength -= l;
			}
			System.out.println("result.next(): close");
			os.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		

	}

	@Override
	public BufferedInputStream getFile(String key) throws IOException {
		throw new NotImplementedException("");
	}

	@Override
	public String addFile(BufferedInputStream is, String filename, String contentType, Timestamp timestamp, long contentLength) throws IOException {
		if (FileSharing.isDebugEnabled())
			FileSharing.getLogger().log(Level.INFO, "[DEBUG] addFile() filename: " + filename + "; ContentType: " + contentType);
		if (is == null || filename == null || filename.length() == 0)
			throw new IllegalStateException("Invalid Arguments is: " + is + " filename: " + filename + " contentType: " + contentType);
		
		if (contentType == null) contentType = "";
		String key = UUID.randomUUID().toString().substring(0, 8);
		
		StreamConverter sc = new StreamConverter(1 << 20);
		
		//GZIPInputStream zis = new GZIPInputStream(is);
		GZIPOutputStream gzos = new GZIPOutputStream(sc.getOutputStream());
		final long contentLength_copy = contentLength;
		new Thread(() -> {
			try {
				//while((l = is.read(arr)) != -1)  gzos.write(arr, 0, l);
				int l;
				long length = contentLength_copy;
				byte[] buffer = new byte[32768];
				while(length > 0) {
					int maxLen = buffer.length;
					if(length < maxLen) maxLen = (int)length;
					l = is.read(buffer, 0, maxLen);
					System.out.println("http.read: " + l);
					gzos.write(buffer, 0, l);
					System.out.println("http.read2: " + l);
					length -= l;
				}
				gzos.close();
			}catch (Exception e) {e.printStackTrace();}
		}).start();
		
		
		InputStream zis = sc.getInputStream();

//		long maxExcpectedLen = contentLength;
//		long offset = 0;
		/*
		long length = contentLength;
		
//		byte[] buffer = new byte[1024*1024];
		byte[] buffer = new byte[32768];
		int offset = 0;
		while(length > 0) {
			int maxLen = buffer.length;
			if(length < maxLen) maxLen = (int)length;
			int l = zis.read(buffer, 0, maxLen);
			String s = new String(buffer, 0, maxLen);
			
			System.out.println(s.length());
			dataBaseInterface.executeSafeAsync("INSERT INTO data (`szKey`, `nOffset`, `nDataLength`, `szData`) VALUES (?, ?, ?, ?)", key, String.valueOf(offset), String.valueOf(s.length()), s);
			length -= l;
			offset++;
		}
		*/
		contentLength = 0;
		byte[] buffer = new byte[(32768 * 2 / 3 ) - 2];
		int l;
		while((l = zis.read(buffer)) != -1) {
			System.out.println("zis.read: " + l);
			//String s = new String(buffer, 0, l);
			
			dataBaseInterface.executeSafeAsync("INSERT INTO data (`szKey`, `nOffset`, `nDataLength`, `szData`) VALUES (?, ?, ?, ?)",
					key, String.valueOf(contentLength), String.valueOf(/*s.length()*/l), 
					new String(Base64.getEncoder().encode(cutByteArr(buffer, l))));
			contentLength += l;
		}
		/*
		while(length > 0) {
			int maxLen = buffer.length;
			if(length < maxLen) maxLen = (int)length;
			int l = is.read(buffer, 0, maxLen);
			fos.write(buffer, 0, l);
			length -= l;
//			System.out.println("write: " + offset + " - " + (offset + l) + ", max: " + maxExcpectedLen);
//			offset+=l;
		}
		
		fos.flush();
		fos.close();
		 */

		System.out.println("SAFING");
		
		if (timestamp == null) {
			dataBaseInterface.executeSafeAsync("INSERT INTO files (`szKey`, `szFilename`, szContentType, nContentLength) VALUES (?, ?, ?, ?)", key, filename, contentType, String.valueOf(contentLength));
		} else {
			dataBaseInterface.executeSafeAsync("INSERT INTO files (`szKey`, `szFilename`, szContentType, nContentLength, `tValid`) VALUES (?, ?, ?, ?, ?)", key, filename, contentType, String.valueOf(contentLength), timestamp.toString());
		}
		System.out.println("SAFED");
		
		return key;
	}
	
	public static byte[] cutByteArr(byte[] in, int newLen) {
		byte[] out = new byte[newLen];
		for(int i=0; i<newLen; i++) in[i] = out[i];
		return out;
	}

	@Override
	public void update() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void checkFileSystem() throws IOException {
		// TODO Auto-generated method stub

	}

}
