package ch.usi.paxosfs.client.microbench;

import ch.usi.paxosfs.client.FileSystemClient;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.util.UnixConstants;
import org.HdrHistogram.Histogram;
import org.apache.thrift.TException;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by pacheco on 10/14/15.
 */
public class BenchRead extends BenchWorker {
    private final FileSystemClient fs;
    private final String path;
    private final FileHandle fh;
    private final ByteBuffer readData;
    private final int readSizeBytes;
    private long offset = 0;

    public BenchRead(FileSystemClient fs, String pathPrefix, int workerId, int readSizeBytes, int durationMillis, OutputStream log, Histogram histogram) throws TException {
        super(durationMillis, log, histogram);
        this.fs = fs;
        this.path = pathPrefix; // FIXME: single file per client, dont use workerID
        this.readSizeBytes = readSizeBytes;

        fh = fs.open(path, UnixConstants.O_RDONLY);
        byte[] data = new byte[readSizeBytes];
        readData = ByteBuffer.wrap(data);
    }

    @Override
    public int doOperation() throws Exception {
        try {
            readData.clear();
            fs.read(path, fh, offset, readData);
            readData.flip();
            if (readData.remaining() < readSizeBytes) {
                offset = 0;
            }
        } catch (FSError e) {
            return e.errorCode;
        } catch (TException e) {
            throw e;
        }
        return 0;
    }
}
