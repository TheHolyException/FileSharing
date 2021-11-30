package de.minebug.filesharing.data;


public class FileLocation{
	public static final int elementSize = 8 + 8 + 1;
	
	public long position, length;
	public int flags;
	
	public FileLocation(RandomAcessStorage disk, long memoryOffset) {
		position = disk.readLong(memoryOffset);
		memoryOffset += 8;
		length = disk.readLong(memoryOffset);
		memoryOffset += 8;
		flags = disk.read(memoryOffset);
		memoryOffset++;
	}
	
	public FileLocation(long position, long length){
		this.position = position;
		this.length = length;
		flags = 0;
	}
	
	public String toString(){
		return "{pos: " + position + ", len: " + length + "}";
	}

	public long save(RandomAcessStorage disk, long pos) {
		disk.writeLong(pos, position);
		disk.writeLong(pos + 8, length);
		disk.write(pos + 16, flags);
		return pos + elementSize;
	}
	
	public boolean isEncrypted(){
		return getFlagBit(0);
	}
	
	public void setEncrypted(boolean b){
		setFlagBit(0, b);
	}
	
	private boolean getFlagBit(int slot){
		return (flags & (1 << slot)) == 1;
	}
	private void setFlagBit(int slot, boolean b){
		if(b){
			flags |= (1 << slot);
		} else {
			flags &= 255 ^ (1 << slot);
		}
	}
}
