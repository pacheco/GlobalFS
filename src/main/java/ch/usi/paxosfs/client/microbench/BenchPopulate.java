package ch.usi.paxosfs.client.microbench;

import ch.usi.paxosfs.client.FileSystemClient;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.util.UnixConstants;
import com.google.common.io.ByteStreams;
import fuse.FuseException;
import org.HdrHistogram.Histogram;
import org.apache.thrift.TException;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by pacheco on 10/14/15.
 */
public class BenchPopulate extends BenchWorker {
    private final int BLOCK_SIZE = 1024;
    private final Random rand = new Random();

    public BenchPopulate(FileSystemClient fs, String pathPrefix, int workerId, int fileSizeBytes) throws TException {
        super(0, ByteStreams.nullOutputStream());
        String path = pathPrefix; // FIXME: single file per machine, don't use worker ID
        try {
            fs.mknod(path, 0755, 0);
        } catch (FSError e) {
            if (e.getErrorCode() != FuseException.EEXIST) {
                throw e;
            }
            System.out.println("File exists. Not populating");
            return; // FIXME: don't populate file if it already exists...
        }
        FileHandle fh;
        fh = fs.open(path, UnixConstants.O_WRONLY | UnixConstants.O_APPEND);
        byte[] data = new byte[BLOCK_SIZE];
        rand.nextBytes(data);

        ByteBuffer writeData;
        writeData = ByteBuffer.wrap(data);
        writeData.mark();

        for (int i = 0; i < fileSizeBytes; i += BLOCK_SIZE) {
            writeData.reset();
            fs.write(path, fh, 0, writeData);
        }
    }

    @Override
    public int doOperation() throws Exception {
        // not used
        return 0;
    }
}
