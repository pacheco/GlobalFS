package ch.usi.paxosfs.client;

import ch.usi.paxosfs.rpc.*;
import ch.usi.paxosfs.util.PathsNIO;
import ch.usi.paxosfs.util.UnixConstants;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.apache.thrift.TException;

import java.nio.ByteBuffer;
import java.util.*;

public class CommandLineClient {
	private static Map<Byte, Long> instanceMap = new HashMap<Byte, Long>();
	private static FileSystemClient fs;
	
	private static class PathCompleter implements Completer {
		@Override
		public int complete(String buffer, int cursor,
				List<CharSequence> candidates) {
			String[] parts = buffer.substring(0,cursor).split("\\s+");
			
			String origPath = parts[parts.length-1];
			String path = PathsNIO.clean(origPath);
			String dir;
			String name;
			if (origPath.endsWith("/")) {
				dir = path;
				name = "";
			} else {
				dir = PathsNIO.dirname(path);
				name = PathsNIO.basename(path);
			}
			try {
				List<DirEntry> entries = fs.getdir(dir);
				for (DirEntry e: entries) {
					if (e.getName().startsWith(name)){
						// fix for root "/"
						if (dir.endsWith("/")) {
							candidates.add(dir + e.getName());
						} else {
							candidates.add(dir + "/" + e.getName());
						}
					}
				}
			} catch (TException e) {
			}
			return cursor - origPath.length();
		}
		
	}

	public static void main(String[] args) throws Exception {
		Random rand = new Random();
		
		if (args.length != 3) {
			System.err.println("client <npartitions> <zkhost> <storageCfg>");
			System.exit(1);
		}
		
		int nPartitions = Integer.parseInt(args[0]);
		String zoohost = args[1];
		String storageCfg = args[2];

		fs = new FileSystemClient(nPartitions, zoohost, storageCfg, 0, Integer.valueOf(1).byteValue());
		fs.start();

		ConsoleReader reader = new ConsoleReader();
		reader.setPrompt("> ");
		reader.addCompleter(new StringsCompleter("statfs", "getdir", "mknod", "getattr", 
				"mkdir", "rmdir", "unlink", "rename", 
				"open", "write", "read", "release"));
		reader.addCompleter(new PathCompleter());
		
		String line;
		FileHandle fh = null;
		while((line = reader.readLine()) != null) {
			String[] parts = line.split("\\s+");
			String cmd = parts[0];

			try {
			switch (cmd) {
			case "statfs": {
				System.out.println(fs.statfs());
				break;
			}
			case "getdir": {
                if (parts.length < 2) continue;
				String path = parts[1];
				System.out.println(fs.getdir(path));
				break;
			}
			case "mknod": {
				if (parts.length < 2) continue;
				String path = parts[1];
				fs.mknod(path, 0777, 0);
				System.out.println("File created.");
				break;
			}
			case "getattr": {
				if (parts.length < 2) continue;
				String path = parts[1];
				System.out.println(fs.getattr(path));
				break;
			}
			case "mkdir": {
				if (parts.length < 2) continue;
				String path = parts[1];
				fs.mkdir(path, 0777);
				System.out.println("Dir created.");
				break;
			}
			case "rmdir": {
				if (parts.length < 2) continue;
				String path = parts[1];
				fs.rmdir(path);
				System.out.println("Dir removed.");
				break;
			}
			case "unlink": {
				if (parts.length < 2) continue;
				String path = parts[1];
				fs.unlink(path);
				System.out.println("File removed.");
				break;
			}
			case "rename": {
				if (parts.length < 3) continue;
				String from = parts[1];
				String to = parts[2];
				fs.rename(from, to);
				System.out.println("File renamed.");
				break;			
			}
			case "open": {
				if (parts.length < 2) continue;
				String path = parts[1];
				fh = fs.open(path, UnixConstants.O_RDWR);
				System.out.println(fh);
				break;
			}
			case "write": {
				if (parts.length < 4) continue;
				String path = parts[1];
				int offset = Integer.parseInt(parts[2]);
				String data = parts[3];
				if (fh == null) {
					System.out.println("Open a file first");
					break;
				}
				fs.write(path, fh, offset, ByteBuffer.wrap(data.getBytes()));
				System.out.println("File written");
				break;
			}
			case "read": {
				if (parts.length < 4) continue;
				String path = parts[1];
				int offset = Integer.parseInt(parts[2]);
				int bytes = Integer.parseInt(parts[3]);
				if (fh == null) {
					System.out.println("Open a file first");
					break;
				}
				ByteBuffer result = fs.read(path, fh, offset, bytes);
				byte[] resultBytes = new byte[result.limit()];
				result.get(resultBytes);
				System.out.println(new String(resultBytes));
				break;
			}
			case "release": {
				if (parts.length < 2) continue;
				String path = parts[1];
				if (fh == null) {
					System.out.println("Open a file first");
					break;
				}
				fs.release(path, fh, 0);
				System.out.println("File closed");
				fh = null;
				break;
			}			
			default:
				System.out.println("Unknown command");
			}
			} catch (FSError e) {
				System.out.println(e.getErrorMsg());
			}
		}
	}
}
