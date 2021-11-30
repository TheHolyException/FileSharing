package de.minebug.filesharing.data;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;

public class RandomAcessStorageFile implements RandomAcessStorage {
	//public RandomAcessStorage_MFT mft;
	public long minLength;
	public PrintStream log;
	
	private RandomAccessFile database;
	private long length;

	public RandomAcessStorageFile(File file) throws FileNotFoundException {
		this(file, 0);
	}
	public RandomAcessStorageFile(File file, long minLen) throws FileNotFoundException {
		database = new RandomAccessFile(file, "rw");
		length = file.length();
		//mft = new RandomAcessStorage_MFT(this);
	}
	
	public void write(byte[] w, long memOffset) {
		synchronized(database) {
			try{
				database.seek(memOffset);
				database.write(w);
			}catch(Exception e){
				if(log != null) e.printStackTrace(log);
			}
		}
	}
	public void write(byte[] arr, int offset, int len, long memOffset) {
		synchronized(database) {
			try{
				database.seek(memOffset);
				database.write(arr, offset, len);
			}catch(Exception e){
				if(log != null) e.printStackTrace(log);
			}
		}
	}
	
	public void read(byte[] arr, long memOffset) {
		read(arr, 0, arr.length, memOffset);
	}
	
	public void read(byte[] arr, int offset, int len, long memOffset) {
		if(memOffset + (long)len > length) len = (int)(length - memOffset);
		if(len > 0){
			synchronized(database) {
				try{
					database.seek(memOffset);
					database.readFully(arr, offset, len);
				}catch(Exception e){
					if(log != null) e.printStackTrace(log);
				}
			}
		}
	}
	
	public long getDBsize(){
		return length;
	}
	
	public void resizeDB(long newLen) {
		if(minLength > newLen) newLen = minLength;
		System.out.println("resizeDB: " + length + " -> " + newLen);
		/*
		char[] oldDB = database == null ? p.Storage.ToCharArray() : database;
		database = new char[newLen];
		if(oldDB.Length > 0){
			if(newLen > oldDB.Length){
				write(oldDB, 0);
			} else {
				write(oldDB, 0, newLen, 0);
			}
		}
		*/
		synchronized(database) {
			try{
				database.setLength(length = newLen);
			} catch (Exception e) {
				if (log != null) e.printStackTrace(log);
			}
		}
	}
	
	public long readLong(long offset) {
		byte[] raw = new byte[8];
		try{
			read(raw, offset);
		} catch (Exception e) {
			if (log != null) e.printStackTrace(log);
		}
		return ((raw[7] & 0xFFL) << 56) | ((raw[6] & 0xFFL) << 48) | ((raw[5] & 0xFFL) << 40) | ((raw[4] & 0xFFL) << 32) | 
			   ((raw[3] & 0xFFL) << 24) | ((raw[2] & 0xFFL) << 16) | ((raw[1] & 0xFFL) <<  8) | raw[0] & 0xFFL;
	}
	
	public void writeLong(long offset, long i) {
		write(new byte[]{
			(byte)((i) & 0xFF),
			(byte)((i >>  8) & 0xFF),
			(byte)((i >> 16) & 0xFF),
			(byte)((i >> 24) & 0xFF),
			(byte)((i >> 32) & 0xFF),
			(byte)((i >> 40) & 0xFF),
			(byte)((i >> 48) & 0xFF),
			(byte)((i >> 56) & 0xFF)
		}, offset);
	}
	public int read(long offset) {
		if(offset > length) return 0;
		synchronized(database) {
			try{
				database.seek(offset);
				return database.read();
			} catch (Exception e) {
				if (log != null) e.printStackTrace(log);
				return 0;
			}
		}
	}
	
	public void write(long offset, int val) {
		if(offset > length) return;
		synchronized(database) {
			try{
				database.seek(offset);
				database.write(val);
			} catch (Exception e) {
				if (log != null) e.printStackTrace(log);
			}
		}
	}

	@Override
	public void close() {
		try {
			database.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}