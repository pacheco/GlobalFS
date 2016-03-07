package ch.usi.paxosfs.util;

public class UnixConstants {
	public static final int O_RDONLY = 0x0000;
	public static final int O_WRONLY = 0x0001;
	public static final int O_RDWR = 0x0002;
	public static final int O_ACCMODE = 0x0003; // Mask for access mode (O_RDONLY, O_WRONLY, O_RDWR)
	public static final int O_APPEND = 0x0008;
	public static final int O_TRUNC = 0x0400;
}
