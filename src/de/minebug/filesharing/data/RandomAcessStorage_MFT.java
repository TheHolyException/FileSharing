package de.minebug.filesharing.data;


import java.util.ArrayList;
import java.util.HashMap;

public class RandomAcessStorage_MFT {
	RandomAcessStorage disk;
	long location;
	HashMap<String, FileLocation> fileMap = new HashMap<String, FileLocation>();
	
	public RandomAcessStorage_MFT(RandomAcessStorage disk)  {
		this.disk = disk;
		location = disk.readLong(0);
		System.out.println("location: " + location);
		if(location <= 0){
			location = 8;
			disk.resizeDB(8);
			fileMap.put("$MFT", new FileLocation(location, 8));
		} else {
			readMFT();
		}
	}
	
	public int createFile(String name, long size){
		synchronized (this) {
			if(name.length() > 255) return 1;
			if(size < 0) return 2;
			FileLocation fl = new FileLocation(findNextFileLocation(size), size);
			deleteFile(name);
			fileMap.put(name, fl);
			writeMFT();
			long requiredLen = getLastFilePos();
			System.out.println("createFile: " + requiredLen + " " + disk.getDBsize());
			if(requiredLen == fl.position) requiredLen += fl.length;
			if(requiredLen > disk.getDBsize()){
				disk.resizeDB(requiredLen);
			}
		}
//		DirectoryUpdateEvent.onUpdate(name, this);
		return -1;
	}
	
	public void deleteFile(String name){
		synchronized (this) {
			fileMap.remove(name);
			writeMFT();
			long requiredLen = getLastFilePos();
			if(disk.getDBsize() > requiredLen){
				disk.resizeDB(requiredLen);
			}
		}
//		DirectoryUpdateEvent.onUpdate(name, this);
	}
	
	public void readFile(String name, long fileOffset, byte[] data, int dataOffset, int dataLen){
		synchronized (this) {
			FileLocation fl = fileMap.get(name);
			if(fl != null){
				disk.read(data, dataOffset, dataLen, fl.position + fileOffset);
			}
		}
	}
	
	public void writeFile(String name, long fileOffset, byte[] data, int dataOffset, int dataLen){
		synchronized (this) {
			FileLocation fl = fileMap.get(name);
			if(fl != null){
				if(fileOffset < 0) {
					return;
				}
				if(fileOffset + dataLen > fl.length){
					dataLen = (int)((fileOffset + (long)dataLen) - fl.length);
					if(dataLen <= 0) return;
				}
				System.out.println("writeFile: at offset: " + ((fileOffset + (long)dataLen)));
				disk.write(data, dataOffset, dataLen, fl.position + fileOffset);
			}
		}
	}
	
	public ArrayList<String> listFiles(String dir, boolean includeSubDirs, boolean fullPath){
		if(!dir.endsWith("/")) dir += "/";
		if(dir.startsWith("/")) dir = dir.substring(1);
		ArrayList<String> in = listFiles();
		ArrayList<String> out = new ArrayList<String>();
		for(String f : in) {
			if(f.startsWith(dir)) {
				String sub = /*fullPath ? f :*/ f.substring(dir.length());
				//System.out.println(sub);
				if(!includeSubDirs){
					int p = sub.indexOf('/');
					if(p != -1) sub = sub.substring(0, p + 1);
				}
				if(fullPath) sub = dir + sub;
				if(!out.contains(sub)) out.add(sub);
			}
		}
		return out;
	}
	
	public ArrayList<String> listFiles(){
		ArrayList<String> a = new ArrayList<String>(fileMap.size());
		synchronized (this) {
			a.addAll(fileMap.keySet());
		}
		return a;
	}
	
	public long getFileSize(String name){
		synchronized (this) {
			FileLocation fl = fileMap.get(name);
			if(fl != null){
				return fl.length;
			}
		}
		return -1;
	}
	
	public void renameFile(String oldName, String newName){
		System.out.println("renameFile(\""+oldName+"\", \""+newName+"\")");
		if(!fileMap.containsKey(oldName)){
			System.err.println("unable to locate source file: " + oldName);
			Thread.dumpStack();
			return;
		}
		synchronized (this) {
			fileMap.remove(newName);
			fileMap.put(newName, fileMap.remove(oldName));
			writeMFT();
		}
//		DirectoryUpdateEvent.onUpdate(oldName, this);
//		DirectoryUpdateEvent.onUpdate(newName, this);
	}
	
	public void resizeFile(String name, long newSize){
		synchronized (this) {
			FileLocation fl = fileMap.get(name);
			if(fl != null){
				long oldSize = fl.length;
				if(newSize == oldSize) return;
				if(newSize < oldSize) {
					fl.length = newSize;
					writeMFT();
					return;
				}
				String tempName = "$TEMP$" + name;
				renameFile(name, tempName);
				createFile(name, newSize);
				FileLocation fl2 = fileMap.get(tempName);
				long pos = 0;
				byte[] copyCache = new byte[1024*1024*32];
				while(true){
					int len = copyCache.length;
					if(pos + (long)len > fl.length) len = (int)(pos - fl.length);
					disk.read(copyCache, 0, len, fl.position + pos);
					disk.write(copyCache, 0, len, fl2.position + pos);
					writeFile(name, pos, copyCache, 0, len);
					if((pos += len) == fl.length) break;
				}
				deleteFile(tempName);
			}
		}
	}
	
	private void readMFT(){
		long count = disk.readLong(location);
		long readPos = location + 8;
		System.out.println("readPos = " + readPos + ", count = " + count);
		for(int i=0; i<count; i++){
			FileLocation fl = new FileLocation(disk, readPos);
			readPos += FileLocation.elementSize;
			byte[] a = new byte[disk.read(readPos)];
			readPos++;
			disk.read(a, readPos);
			readPos += a.length;
			fileMap.put(new String(a), fl);
		}
		fileMap.put("$MFT", new FileLocation(location, readPos));
		System.out.println("readMFT: " + fileMap.keySet());
	}
	
	private void writeMFT(){
		long len = 8;
		for(String name : fileMap.keySet()){
			if(!name.equals("$MFT")) len += name.length() + FileLocation.elementSize + 1;//fileMap.get(name).calcEntrySize(name);
		}
		long pos = location = findNextFileLocation(len);
		disk.writeLong(0, pos);
		fileMap.put("$MFT", new FileLocation(pos, len));
		long requiredLen = pos + len;//getLastFilePos();
		//if(requiredLen == pos) requiredLen += len;
		//instance.Echo("writeMFT: " + requiredLen + " " + disk.getDBsize());
		System.out.println("writeMFT: p1: " + pos + ", p2: " + requiredLen + " disksize: " + disk.getDBsize());
		if(requiredLen > disk.getDBsize()){
			disk.resizeDB(requiredLen);
		}
		//instance.Echo("pos="+pos);
		disk.writeLong(pos, fileMap.size()-1);
		pos += 8;
		for(String name : fileMap.keySet()){
			if(name.equals("$MFT")) continue;
			/*
			FileLocation fl = fileMap.get(name);
			disk.writeLong(pos, fl.position);
			disk.writeLong(pos+8, fl.length);
			*/
			pos = fileMap.get(name).save(disk, pos);
			byte[] a = name.getBytes();
			disk.write(pos, a.length);
			pos++;
			disk.write(a, pos);
			pos += a.length;
		}
		//System.out.println(pos + "=" + len);
	}
	
	private long getLastFilePos(){
		long pos = 8;
		FileLocation fl;
		while((fl = getNextFileByOffset(pos)) != null){
			long a = fl.position + fl.length;
			if(a > pos) pos = a;
		}
		return pos;
	}
	
	private long findNextFileLocation(long declaredSize){
		long pos = 8;
		FileLocation fl;
		while((fl = getNextFileByOffset(pos)) != null){
			if(fl.position - pos >= declaredSize && fl.length > 0) {
				System.out.println("findNextFileLocation: fl.position = " + fl.position);
				System.out.println("findNextFileLocation(" + declaredSize +"): (pre-esc) " + pos);
				return pos;
			}
			pos = fl.position + fl.length;
		}
		System.out.println("findNextFileLocation(" + declaredSize +"): " + pos);
		return pos;
	}
	
	private FileLocation getNextFileByOffset(long pos){
		//System.out.println("MFT::getNextFileByOffset(): " + fileMap);
		long best = Long.MAX_VALUE;
		FileLocation f = null;
		for(FileLocation fl : fileMap.values()){
			if(fl.length == 0) continue;
			long a = fl.position;//.length;
			if(a < best && a >= pos) {
				best = a;
				f = fl;
			}
		}
		return f;
	}

//	@Override
//	public void close() {
//		disk.close();
//	}
}