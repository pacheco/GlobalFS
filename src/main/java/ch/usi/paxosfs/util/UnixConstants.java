package ch.usi.paxosfs.util;

public enum UnixConstants {
	O_RDONLY(0x0000),
	O_WRONLY(0x0001),
	O_RDWR(0x0002),
	/*
	 * Mask for access mode (O_RDONLY, O_WRONLY, O_RDWR)
	 */
	O_ACCMODE(0x0003),
	O_APPEND(0x0008),
	O_TRUNC(0x0400);
	;
	private final int value;
	
	private UnixConstants(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
