package de.minebug.filesharing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.sql.Timestamp;

public interface FileInterface {
	
	public FileInfo getFileInfo(String key) throws IOException;
	public void getFile(String key, BufferedOutputStream bos) throws IOException;
	public String addFile(BufferedInputStream is, String filename, byte[] token, String contentType, Timestamp timestamp, long contentLength) throws IOException;
	public void update()  throws IOException;
	public void checkFileSystem()  throws IOException;
}
