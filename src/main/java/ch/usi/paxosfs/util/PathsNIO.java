package ch.usi.paxosfs.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;


/**
 * Utilities to manipulate paths represented as a string. Same as Paths but implemented using Path class from nio.
 * 
 * @author pacheco
 * 
 */
public class PathsNIO {
	/**
	 * Returns the path after removing redundant elements: duplicate "/" and
	 * dots in the middle of the path. Borrowed from the golang implementation of path.Clean
	 * 
	 * @param path
	 * @return
	 */
	public static String clean(String path) {
		if (path.isEmpty()) { 
			path = "."; // nio Paths.get does not handle ""
		}
		String result = Paths.get(path).normalize().toString();
		if (result.isEmpty()){ 
			return ".";
		}
		return result;
	}

	/**
	 * Returns true if path is absolute.
	 * @param path
	 * @return
	 */
	public static boolean isAbsolute(String path){
		return Paths.get(path).isAbsolute();
	}
	
	/**
	 * Returns an Iterable of the elements of path. The path is expected to have been processed by Paths.clean.
	 * @param path
	 * @return
	 */
	public static Iterable<String> elements(String path) {
		Path p = Paths.get(path);
		Iterable<String> ret = Iterables.transform(p, Functions.toStringFunction()); 
		if (p.isAbsolute()) {
			ret = Iterables.concat(Lists.newArrayList("/"), ret);
		}
		return ret;
	}
	
	/**
	 * Returns an Iterator to the elements of path. The path is expected to have been processed by Paths.clean.
	 * @param path
	 * @return
	 */
	public static Iterator<String> elementIterator(String path) {
		return Iterators.transform(Paths.get(path).iterator(), Functions.toStringFunction());
	}
	
	/**
	 * Returns the last element of path. The path is expected to have been processed by Paths.clean.
	 * @param path
	 * @return
	 */
	public static String basename(String path) {
		if (path.equals("/")) return path; // nio Paths.get does not handle /
		return Paths.get(path).getFileName().toString();
	}
	
	/**
	 * Returns the directory portion of the path. The path is expected to have been processed by Paths.clean.
	 */
	public static String dirname(String path) {
		if (path == "") return ".";
		Path p = Paths.get(path);
		Path parent = p.getParent();
		if (parent == null){
			if (p.isAbsolute()) return "/";
			return ".";
		}
		return parent.toString();
	}
}
