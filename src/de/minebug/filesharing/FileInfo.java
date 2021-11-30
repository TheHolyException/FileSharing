package de.minebug.filesharing;

public class FileInfo {

	private String filename;
	private long size;
	private String name;
	private String contentType;
	
	public FileInfo(String filename, long size, String name, String contentType) {
		this.filename = filename;
		this.size = size;
		this.name = name;
		this.contentType = contentType;
	}
	
	public String getFileName() {
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
	
	@Override
	public String toString() {
		return "[Filename: " + filename + ", Size: " + size + ", Name: " + name + ", ContentType: " + contentType + "]";
	}
	
}
