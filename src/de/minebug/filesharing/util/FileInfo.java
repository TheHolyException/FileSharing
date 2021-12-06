package de.minebug.filesharing.util;

import java.sql.Timestamp;

public class FileInfo {

	private String filename;
	private long size;
	private String name;
	private String contentType;
	private Timestamp available;
	
	public FileInfo(String filename, long size, String name, String contentType, Timestamp available) {
		this.filename = filename;
		this.size = size;
		this.name = name;
		this.contentType = contentType;
		this.available = available;
	}
	
	public String getKey() {
		return filename;
	}
	
	public long getSize() {
		return size;
	}
	
	public String getName() {
		return name;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public Timestamp getAvailable() {
		return available;
	}
	
	@Override
	public String toString() {
		return "[Filename: " + filename + ", Size: " + size + ", Name: " + name + ", ContentType: " + contentType + "]";
	}
	
}
