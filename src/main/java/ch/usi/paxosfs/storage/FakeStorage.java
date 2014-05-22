package ch.usi.paxosfs.storage;

import java.util.ArrayList;
import java.util.List;

public class FakeStorage implements Storage {
	byte[] data = new byte[1024*512];
	
	@Override
	public boolean put(byte[] key, byte[] data) {
		return true;
	}

	@Override
	public byte[] get(byte[] key) {
		return data;
	}

	@Override
	public boolean delete(byte[] key) {
		return true;
	}

	@Override
	public boolean multiPut(List<byte[]> keys, List<byte[]> data) {
		return true;
	}

	@Override
	public List<byte[]> multiGet(List<byte[]> keys) {
		List<byte[]> result = new ArrayList<byte[]>(keys.size());
		for (int i = 0; i < keys.size(); i++) {
			result.add(data);
		}
		return result;
	}

}
