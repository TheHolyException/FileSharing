package de.minebug.filesharing.data;

public interface RandomAcessStorage {
	public int read(long offset);
	public long readLong(long offset);
	public void read(byte[] arr, long memOffset);
	public void read(byte[] arr, int offset, int len, long memOffset);
	
	public void write(long offset, int val);
	public void writeLong(long offset, long i);
	public void write(byte[] w, long memOffset);
	public void write(byte[] arr, int offset, int len, long memOffset);
	
	public long getDBsize();
	public void resizeDB(long newLen);
	public void close();
}
