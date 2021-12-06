package de.minebug.filesharing.filemanagers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import de.minebug.filesharing.FileSharing;
import de.minebug.filesharing.util.FileInfo;
import de.theholyexception.holyapi.datastorage.dataconnection.DataBaseInterface;

public class FileManagerMulti implements FileInterface {

	private DataBaseInterface dataBaseInterface;
	private final File baseDir;
	
	public FileManagerMulti(DataBaseInterface dataBaseInterface, File baseDir) {
		this.dataBaseInterface = dataBaseInterface;
		this.baseDir = baseDir;
		
		if (!baseDir.exists()) baseDir.mkdirs();
		
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
	
	public FileInfo getFileInfo(String key) throws IOException {
		try {
			ResultSet result = dataBaseInterface.executeQuerySafe(""
					+ "SELECT szKey, szContentType, szFilename, tValid FROM files "
					+ "WHERE tValid > CURRENT_TIMESTAMP() AND szKey=?;", key);
			File file = new File(baseDir, key);
			if (!file.exists() || !result.next()) return null;
			return new FileInfo(
					key, 
					file.length(), 
					result.getString("szFilename"), 
					result.getString("szContentType"), 
					result.getTimestamp("tValid"));	
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	public void getFile(String key, BufferedOutputStream bos) throws IOException {
		FileInputStream fis = new FileInputStream(new File(baseDir, key));
		
		long contentLength = getFileInfo(key).getSize();
		byte[] buffer = new byte[1024*1024];
		while(contentLength > 0) {
			int maxLen = buffer.length;
			if(contentLength < maxLen) maxLen = (int)contentLength;
			int l = fis.read(buffer, 0, maxLen);
			bos.write(buffer, 0, l);
			contentLength -= l;
			
		}
	}
	
	public BufferedInputStream getFile(String key) throws IOException {
		return new BufferedInputStream(new FileInputStream(new File(baseDir, key)));
	}
	
	public String addFile(BufferedInputStream is, String filename, String contentType, Timestamp timestamp, long contentLength) throws IOException {
		if (FileSharing.isDebugEnabled())
			FileSharing.getLogger().log(Level.INFO, "[DEBUG] addFile() filename: " + filename + "; ContentType: " + contentType);
		if (is == null || filename == null || filename.length() == 0)
			throw new IllegalStateException("Invalid Arguments is: " + is + " filename: " + filename + " contentType: " + contentType);
		
		if (contentType == null) contentType = "";
		String key = UUID.randomUUID().toString().substring(0, 8);
		
		File file = new File(baseDir, key);
		FileOutputStream fos = new FileOutputStream(file);

//		long maxExcpectedLen = contentLength;
//		long offset = 0;
		long length = new Long(contentLength);
		byte[] buffer = new byte[1024*1024];
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

		if (timestamp == null) {
			dataBaseInterface.executeSafeAsync("INSERT INTO files (`szKey`, `szFilename`, szContentType, nContentLength) VALUES (?, ?, ?, ?)", key, filename, contentType, String.valueOf(contentLength));
		} else {
			dataBaseInterface.executeSafeAsync("INSERT INTO files (`szKey`, `szFilename`, szContentType, nContentLength, `tValid`) VALUES (?, ?, ?, ?, ?)", key, filename, contentType, String.valueOf(contentLength), timestamp.toString());
		}
		
		return key;
	}
	
	public void update() throws IOException {
		dataBaseInterface.executeQueryAsync(result -> {
			try {
				while (result != null && result.next()) {
					try {
						String key = result.getString("szKey");
						Files.delete(new File(baseDir, key).toPath());
						System.out.println("File went Invalid: " + key);
						FileSharing.getLogger().log(Level.INFO, "[Validity Checker] Invalid File Found: " + result.getString("szFilename") + "("+key+")");
						dataBaseInterface.executeSafe("DELETE FROM files WHERE `szKey`=?", key);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}, "SELECT szKey, szFilename, tValid FROM files WHERE tValid < CURRENT_TIMESTAMP();");
	}
	
	public void checkFileSystem() throws IOException {
		//region Step1
		FileSharing.getLogger().log(Level.INFO, "[FileSystem Checker] Checking FileSystems");
		FileSharing.getLogger().log(Level.INFO, "[FileSystem Checker] Step 1: Checking if Files on DB exists in FS");
		
		int valid = 0;
		List<String> invalid = new ArrayList<>();
		ResultSet result = dataBaseInterface.executeQuery("SELECT szKey, szFilename, tValid FROM files");
		try {
			while (result.next()) {
				if (getFileInfo(result.getString("szKey")) == null) {
					invalid.add(result.getString("szKey"));
					continue;
				}
				valid++;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			return;
		}
		FileSharing.getLogger().log(Level.INFO, "[FileSystem Checker] Step 1:  Valid Files: " + valid + ";  Invalid Files: " + invalid.size());
		
		for (String a : invalid) {
			FileSharing.getLogger().log(Level.INFO, "[FileSystem Checker] Deleting file on Database: " + a);
			dataBaseInterface.executeSafeAsync("DELETE FROM files WHERE `szKey`=?", a);
		}
		//endregion Step1
		
		//region Step2
		FileSharing.getLogger().log(Level.INFO, "[FileSystem Checker] Step 2: Checking if Files in FS exists on DB");
		valid = 0;
		invalid.clear();
		
		try {
			for (File filename : baseDir.listFiles()) {
				ResultSet rs = dataBaseInterface.executeQuerySafe("SELECT * FROM files WHERE `szKey`=?", filename.getName());
				if (!rs.next()) {
					invalid.add(filename.getName());
					continue;
				}
				valid++;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		FileSharing.getLogger().log(Level.INFO, "[FileSystem Checker] Step 2:  Valid Files: " + valid + ";  Invalid Files: " + invalid.size());
		
		for (String a : invalid) {
			FileSharing.getLogger().log(Level.INFO, "[FileSystem Checker] Deleting file on FileSystem: " + a);
			Files.delete(new File(baseDir, a).toPath());
		}
		//endregion Step2
	}
}
