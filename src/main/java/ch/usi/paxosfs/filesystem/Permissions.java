package ch.usi.paxosfs.filesystem;

/**
 * Handling of unix file mode/permissions
 * @author pacheco
 *
 */
public class Permissions {
	public static int ALL = (7 << 6) | (7 << 3) | 7;
	public static int ROOT_DEFAULT = (7 << 6) | (5 << 3) | 5;
	public static int UX = 1 << 6;
	public static int UW = 2 << 6;
	public static int UR = 4 << 6;
	public static int GX = 1 << 3;
	public static int GW = 2 << 3;
	public static int GR = 4 << 3;
	public static int OX = 1;
	public static int OW = 2;
	public static int OR = 4;
	public static int LINK = UR | UW | UX | GX | GR | OX | OR;
}
