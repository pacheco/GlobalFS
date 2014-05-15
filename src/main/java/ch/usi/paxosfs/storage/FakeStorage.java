package ch.usi.paxosfs.storage;

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

}
