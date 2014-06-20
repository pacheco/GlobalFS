package ch.usi.paxosfs.storage;

import java.util.ArrayList;
import java.util.List;

public class FakeStorage implements Storage {
	byte[] data = new byte[1024*512];
	
	@Override
	public boolean put(byte[] key, byte[] value) {
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
	public List<Boolean> multiPut(List<byte[]> keys, List<byte[]> values) {
		List<Boolean> result = new ArrayList<Boolean>(keys.size());
		for (int i = 0; i < result.size(); i++) {
			result.add(Boolean.TRUE);
		}
		return result;
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
