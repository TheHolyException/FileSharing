package de.minebug.filesharing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import de.theholyexception.holyapi.datastorage.dataconnection.DataBaseInterface;
import me.kaigermany.utilitys.data.Pair;
public class FileManager {

	private DataBaseInterface dataBaseInterface;
	private final File baseDir;
	
	public FileManager(DataBaseInterface dataBaseInterface, File baseDir) {
		this.dataBaseInterface = dataBaseInterface;
		this.baseDir = baseDir;
		
		if (!baseDir.exists()) baseDir.mkdirs();
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				update();
			}
		}, 1000, 60000);
	}
	
	public Pair<String, BufferedInputStream> getFile(String key) {
		ResultSet result = dataBaseInterface.executeQuerySafe(""
				+ "SELECT szKey, szFilename, tValid FROM files "
				+ "WHERE tValid > CURRENT_TIMESTAMP() AND szKey=?;", key);
		
		try {
			if (result.first()) {
				File f = new File(baseDir, result.getString("szKey"));
				if (f.exists()) {
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
					return new Pair<String, BufferedInputStream>(result.getString("szFilename"), bis);
				}
			}
		} catch (SQLException | FileNotFoundException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	public String addFile(BufferedInputStream is, String filename, byte[] token, Timestamp timestamp) {
		if (is == null || filename == null || token == null || filename.length() == 0 || token.length == 0)
			throw new IllegalStateException("Invalid Arguments");
		
		String key = UUID.randomUUID().toString().substring(0, 8);
		File file = new File(baseDir, key);
		
		try {
			file.createNewFile();

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			int c;
			int pointer = 0;
			while(is.available() > 0) {
				c = is.read();
				if (c == -1) break;
				if (c == token[pointer]) {
					pointer++;
					if (pointer == token.length) {
						System.out.println("REACHED");
						break;
					}
					continue;
				} else {
					bos.write(token, 0, pointer);
					if (c == token[0]) {
						pointer = 1;
						continue;
					} else pointer = 0;
				}
				bos.write(c);
			}

			bos.flush();
			bos.close();
			
			if (timestamp == null) {
				dataBaseInterface.executeSafeAsync("INSERT INTO files (`szKey`, `szFilename`) VALUES (?, ?)", key, filename);
			} else {
				dataBaseInterface.executeSafeAsync("INSERT INTO files (`szKey`, `szFilename`, `tValid`) VALUES (?, ?, ?)", key, filename, timestamp.toString().split(".")[0]);
			}
			
			return key;
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	private void update() {
		dataBaseInterface.executeQueryAsync(result -> {
			try {
				if (result.first()) {
					
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}, "SELECT tValid FROM files WHERE tValid > CURRENT_TIMESTAMP();");
	}
	
	private void checkFileSystem() {
		
		
		System.out.println("Checking FileSystem Step 1: Database check");
		
		int valid = 0;
		List<String> invalid = new ArrayList<>();
		ResultSet result = dataBaseInterface.executeQuery("SELECT szKey, szFilename, tValid FROM files");
		try {
			while (result.next()) {
				File file = new File(baseDir, result.getString("szKey"));
				if (!file.exists()) {
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
			dataBaseInterface.executeSafeAsync("DELETE FROM files WHERE `szKey`=?", a);
		}
		
		System.out.println("Step 2: Reverse Database check");
		valid = 0;
		invalid.clear();
		
		for (File files : baseDir.listFiles()) {
			
		}
		
	}
	
	@SuppressWarnings("unused")
	private byte[] readUntil(BufferedInputStream is, byte[] word) throws IOException {
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
}
