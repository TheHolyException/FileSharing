package de.minebug.filesharing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.minebug.filesharing.data.RandomAcessStorageFile;
import de.minebug.filesharing.data.RandomAcessStorage_MFT;
import de.theholyexception.holyapi.datastorage.dataconnection.DataBaseInterface;

public class FileManagerSingle implements FileInterface {

	private DataBaseInterface dataBaseInterface;
	private RandomAcessStorage_MFT mft;
	
	public FileManagerSingle(DataBaseInterface dataBaseInterface, File datafile) {
		this.dataBaseInterface = dataBaseInterface;
		try {
			mft = new RandomAcessStorage_MFT(new RandomAcessStorageFile(datafile));
			System.out.println("aaaaa");
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
			result.first();
			return new FileInfo(
					key,
					mft.getFileSize(key),
					result.getString("szFilename"),
					result.getString("szContentType"), 
					result.getTimestamp("tValid"));	
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	public void getFile(String key, BufferedOutputStream bos) throws IOException {
		long contentLength = getFileInfo(key).getSize();
		long offset = 0;
		byte[] buffer = new byte[1024*1024];
		while(contentLength > 0) {
			int l = buffer.length;
			if(contentLength < l) l = (int)contentLength;
			synchronized (mft) {
				mft.readFile(key, offset, buffer, 0, l);
			}
			bos.write(buffer, 0, l);
			contentLength -= l;
			offset+=l;
		}
	}
	
	public String addFile(BufferedInputStream is, String filename, byte[] token, String contentType, Timestamp timestamp, long contentLength) throws IOException {
		if (is == null || filename == null || token == null || contentType == null || filename.length() == 0 || token.length == 0)
			throw new IllegalStateException("Invalid Arguments is: " + is + " filename: " + filename + " token: " + token + " contentType: " + contentType);
		
		String key = UUID.randomUUID().toString().substring(0, 8);
		
		synchronized (mft) {
			mft.createFile(key, contentLength);
		}
		long maxExcpectedLen = contentLength;
		long offset = 0;
		byte[] buffer = new byte[1024*1024];
		while(contentLength > 0) {
			int maxLen = buffer.length;
			if(contentLength < maxLen) maxLen = (int)contentLength;
			int l = is.read(buffer, 0, maxLen);
			synchronized (mft) {
				mft.writeFile(key, offset, buffer, 0, l);
			}
			contentLength -= l;
			System.out.println("write: " + offset + " - " + (offset + l) + ", max: " + maxExcpectedLen);
			offset+=l;
		}

		if (timestamp == null) {
			dataBaseInterface.executeSafeAsync("INSERT INTO files (`szKey`, `szFilename`, szContentType, nContentLength) VALUES (?, ?, ?, ?)", key, filename, contentType, String.valueOf(contentLength));
		} else {
			System.out.println("TIMESTAMP: " + timestamp);
			dataBaseInterface.executeSafeAsync("INSERT INTO files (`szKey`, `szFilename`, szContentType, nContentLength, `tValid`) VALUES (?, ?, ?, ?, ?)", key, filename, contentType, String.valueOf(contentLength), timestamp.toString());
		}
		
		return key;
	}
	
	public void update() {
		dataBaseInterface.executeQueryAsync(result -> {
			try {
				while (result.next()) {
					try {
						String key = result.getString("szKey");
						mft.deleteFile(key);
						System.out.println("asdf" + key);
						dataBaseInterface.executeSafe("DELETE FROM files WHERE `szKey`=?", key);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}, "SELECT szKey, tValid FROM files WHERE tValid < CURRENT_TIMESTAMP();");
	}
	
	public void checkFileSystem() throws IOException {
		
		System.out.println("Checking FileSystem \nStep 1: Database check");
		
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
		System.out.println("Step 1: Valid: " + valid + " Invalid: " + invalid.size());
		
		for (String a : invalid) {
			System.out.println("Delete from DB: " + a);
			dataBaseInterface.executeSafeAsync("DELETE FROM files WHERE `szKey`=?", a);
		}
		
		System.out.println("Step 2: Reverse Database check");
		valid = 0;
		invalid.clear();
		
		try {
			for (String filename : mft.listFiles()) {
				if (filename.equals("$MFT")) continue;
				ResultSet rs = dataBaseInterface.executeQuerySafe("SELECT * FROM files WHERE `szKey`=?", filename);
				if (!rs.first()) {
					invalid.add(filename);
					continue;
				}
				valid++;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		System.out.println("Step 2: Valid: " + valid + " Invalid: " + invalid.size());
		
		for (String a : invalid) {
			System.out.println("Delete from FS: " + a);
			try {
				mft.deleteFile(a);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
