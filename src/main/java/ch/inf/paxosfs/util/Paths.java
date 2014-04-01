package ch.inf.paxosfs.util;

import java.util.Iterator;

import com.google.common.collect.Lists;


/**
 * Utilities to manipulate paths represented as a string
 * 
 * @author pacheco
 * 
 */
public class Paths {
	private static class PathIterator implements Iterator<String> {
		final String path;
		int idx = 0;
		
		public PathIterator(String path) {
			this.path = path;
		}
		
		public boolean hasNext() {
			return idx < path.length();
		}

		public String next() {
			if (idx >= path.length()) return null;
			
			int newIdx = path.indexOf('/', idx); // find the end of the next element
			
			if (newIdx == 0) { 
				// special case for absolute path
				idx = 1;
				return "/";
			}
			
			if (newIdx == -1){
				// last element
				String elem = path.substring(idx, path.length());
				idx = path.length();
				return elem;
			}
			
			// common case
			String elem = path.substring(idx, newIdx);
			idx = newIdx + 1;
			return elem;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	
	/**
	 * Returns the path after removing redundant elements: duplicate "/" and
	 * dots in the middle of the path. Borrowed from the golang implementation of path.Clean
	 * 
	 * @param path
	 * @return
	 */
	public static String clean(String path) {
		// FIXME: it seems that simplifying .. in the middle of a path is not a good idea. Take the example of foo/../bar.
		// If foo is a symlink, reducing the path to 'bar' might be incorrect.
		if (path.equals("")) {
			return ".";
		}
		
		char[] cpath = path.toCharArray();
		StringBuilder result = new StringBuilder();
		int idx = 0; // pointer traversing the path
		int prefixIdx = 0; // used to limit .. backtracking
		
		boolean absolute = (cpath[0] == '/');
		if (absolute) {
			result.append('/');
			idx = 1;
			prefixIdx = 1;
		}
		
		for (; idx < path.length(); idx++){
			if (cpath[idx] == '/'){
				// empty element
			} else if (cpath[idx] == '.' 
					&& (idx+1 == path.length() || cpath[idx+1] == '/')) {
				// . element
			} else if (cpath[idx] == '.' 
					&& (idx+1 == path.length() || cpath[idx+1] == '.')
					&& (idx+2 == path.length() || cpath[idx+2] == '/')) {
				// .. element. Backtrack once if possible
				if (result.length() > prefixIdx) {
					// backtrack one element
					int lastSlash = result.lastIndexOf("/");
					if (lastSlash < 0) lastSlash = 0;
					result.setLength(lastSlash);
				} else if (!absolute) {
					// append the ..
					if (result.length() != 0) {
						result.append('/');
					}
					result.append("..");
					prefixIdx = result.length();
				}
			} else {
				// just copy the new element
				if ((absolute && result.length() != 1) 
					|| (!absolute && result.length() != 0)){
					result.append('/');
				}
				for (; idx < path.length() && cpath[idx] != '/'; idx++){
					result.append(cpath[idx]);
				}
			}
		}
		
		if (result.length() == 0) {
			if (absolute) {
				return "/";
			}
			return ".";
		}
		return result.toString();
	}

	/**
	 * Returns true if path is absolute.
	 * @param path
	 * @return
	 */
	public static boolean isAbsolute(String path){
		return path.startsWith("/");
	}
	
	/**
	 * Returns an Iterable of the elements of path. The path is expected to have been processed by Paths.clean.
	 * @param path
	 * @return
	 */
	public static Iterable<String> elements(String path) {
		return Lists.newArrayList(new PathIterator(path));
	}
	
	/**
	 * Returns an Iterator to the elements of path. The path is expected to have been processed by Paths.clean.
	 * @param path
	 * @return
	 */
	public static Iterator<String> elementIterator(String path) {
		return new PathIterator(path);
	}
	
	/**
	 * Returns the last element of path. The path is expected to have been processed by Paths.clean.
	 * @param path
	 * @return
	 */
	public static String basename(String path) {
		int idx = path.lastIndexOf("/");
		if (idx < 0) return path;
		if (path.equals("/")) return "/";
		return path.substring(idx+1, path.length());
	}
	
	/**
	 * Returns the directory portion of the path. The path is expected to have been processed by Paths.clean.
	 */
	public static String dirname(String path) {
		int idx = path.lastIndexOf("/");
		if (idx < 0) return ".";
		if (path.equals("/") || idx == 0) return "/";
		return path.substring(0, idx);
	}
}
