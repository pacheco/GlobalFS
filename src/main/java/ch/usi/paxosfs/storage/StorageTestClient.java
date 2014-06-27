package ch.usi.paxosfs.storage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import ch.usi.paxosfs.util.UUIDUtils;

public class StorageTestClient {
	public HttpStorageClient storage;
	
	public StorageTestClient() {
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		Random rand = new Random();
		StorageTestClient c = new StorageTestClient();
		c.storage = new HttpStorageClient("http://localhost:5000");
		
		byte[] value = new byte[1024*12*8];
		for (int i = 0; i < value.length; i++) {
			value[i] = 1;
		}
		byte[] key = UUIDUtils.longToBytes(rand.nextLong());
		
		List<byte[]> keys = new LinkedList<>();
		byte[] multiValue = new byte[1024*8];
		for (int i = 0; i < multiValue.length; i++) {
			multiValue[i] = 1;
		}
		List<byte[]> values = new LinkedList<>();
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		keys.add(UUIDUtils.longToBytes(rand.nextLong()));
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);
		values.add(multiValue);

		long start, end;
		start = System.currentTimeMillis();
		boolean result = c.storage.put(key, value).get();
		end = System.currentTimeMillis();
		System.out.println("single put: " + (end - start) + " - " + result);
		
		start = System.currentTimeMillis();
		List<Future<Boolean>> putFutures = new ArrayList<Future<Boolean>>(keys.size());
		for (int i = 0; i < keys.size(); i++) {
			putFutures.add(c.storage.put(keys.get(i), values.get(i)));
		}
		for (Future<Boolean> f: putFutures) {
			if (!f.get()) {
				System.out.println("ERROR ON PUT");
				return;
			}
		}
		end = System.currentTimeMillis();
		System.out.println("multi put: " + (end - start));
		
		start = System.currentTimeMillis();
		byte[] retValue = c.storage.get(key).get();
		end = System.currentTimeMillis();
		System.out.println("single get: " + (end - start));
		for (int i = 0; i < retValue.length; i++) {
			if (retValue[i] != 1) {
				System.out.println("WRONG VALUE STORED! - single put");
				return;
			}
		}
		
		start = System.currentTimeMillis();
		List<Future<byte[]>> getFutures = new ArrayList<>(keys.size());
		for (int i = 0; i < keys.size(); i++){
			getFutures.add(c.storage.get(keys.get(i)));
		}
		List<byte[]> retValues = new ArrayList<>(keys.size());
		for (Future<byte[]> f: getFutures) {
			retValues.add(f.get());
		}
		
		end = System.currentTimeMillis();
		System.out.println("multi get: " + (end - start));
		if (retValues.size() != values.size()) {
			System.out.println("WRONG NUMBER OF VALUES RETURNED!");
			return;
		}
		for (byte[] v : retValues) {
			for (int i = 0; i < v.length; i++) {
				if (v[i] != 1) {
					System.out.println("WRONG VALUE STORED! - multi put");
					return;
				}
			}
		}
	}
}
