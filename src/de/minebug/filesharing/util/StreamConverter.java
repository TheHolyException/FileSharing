package de.minebug.filesharing.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.kaigermany.utilitys.data.ObjectStream;

public class StreamConverter {
	private OutputStream os;
	private InputStream is;
	private int bufferSize;
	private ObjectStream<byte[]> writeBuffer = new ObjectStream<byte[]>();
	private byte[] currentReadArray;
	private int currentReadPos;
	private boolean isEOF = false;
	
	public StreamConverter(int bufferSize) {
		this.bufferSize = bufferSize;
		os = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				System.out.println("StreamConverter.write1");
				if(b == -1) {
					synchronized (writeBuffer) {
						writeBuffer.put(null);
					}
					isEOF = true;
				} else {
					synchronized (writeBuffer) {
						writeBuffer.put(new byte[] {(byte)b});
					}
				}
			}
			@Override
			public void write(byte[] buffer, int offset, int len) throws IOException {
				System.out.println("StreamConverter.write2: " + len);
				byte[] arr = new byte[len];
				for(int i=0; i<len; i++) arr[i] = buffer[i + offset];
				synchronized (writeBuffer) {
					writeBuffer.put(arr);
				}
				System.out.println("StreamConverter.write2:exit");
			}
			
			@Override
			public void close() {
				isEOF = true;
			}
		};
		is = new InputStream() {
			@Override
			public int read() throws IOException {
				byte[] a = new byte[1];
				while(true) {
					if(readBytes(a, 0, 1) == 1) break;
					if(isEOF) return -1;
				}
				return a[0] & 0xFF;
			}
			@Override
			public int read(byte[] buffer, int offset, int len) throws IOException {
				return readBytes(buffer, offset, len);
			}
		};
	}
	
	private int readBytes(byte[] buffer, int offset, int len) {
		int bytesWritten = 0;
		while(true) {
			
			if(currentReadArray == null) {
				if(writeBuffer.hasNext()) {
					synchronized (writeBuffer) {
						currentReadArray = writeBuffer.getNext();
					}
					currentReadPos = 0;
				} else {
					while(!isEOF && !writeBuffer.hasNext()) {
						try {
							Thread.sleep(10);
						} catch (Exception e) {}
					}
					synchronized (writeBuffer) {
						if(writeBuffer.hasNext()) {
							currentReadArray = writeBuffer.getNext();
							currentReadPos = 0;
						} else {
							if(isEOF) return bytesWritten > 0 ? bytesWritten : -1;
						}
					}
				}
			}
			int lenToProcess = Math.min(len - bytesWritten, currentReadArray.length - currentReadPos);
			System.arraycopy(currentReadArray, currentReadPos, buffer, bytesWritten, lenToProcess);
			currentReadPos += lenToProcess;
			bytesWritten += lenToProcess;
			if(currentReadPos == currentReadArray.length) currentReadArray = null;
			if(bytesWritten == len) return bytesWritten;
			
		}
	}
	
	public OutputStream getOutputStream() {
		return os;
	}
	
	public InputStream getInputStream() {
		return is;
	}
}
