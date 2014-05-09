package ch.usi.paxosfs.replica;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;

import ch.usi.da.paxos.api.Proposer;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;
import ch.usi.da.paxos.storage.Decision;
import ch.usi.paxosfs.replica.commands.Command;
import ch.usi.paxosfs.replica.commands.CommandType;
import ch.usi.paxosfs.replica.commands.RenameCmd;
import ch.usi.paxosfs.replica.commands.Signal;
import ch.usi.paxosfs.rpc.FSError;

/**
 * Implements a higher level of abstraction to the communication done between the replicas
 * @author pacheco
 *
 */
public class CommunicationService {
	public static int BATCH_SIZE = 128*1024;
	public static long BATCH_TIMEOUT = 25;
	
	/**
	 * Batch commands before proposing them
	 * @param size
	 * @param timeoutMillis
	 */
	private class Batcher implements Runnable {
		private ByteBuffer batch;
		private long timeout;
		private Proposer proposer;
		public BlockingQueue<byte[]> toSend;
		
		public Batcher(Proposer p, int size, long timeoutMillis) {
			this.batch = ByteBuffer.allocate(size);
			this.timeout = timeoutMillis;
			this.toSend = new LinkedBlockingQueue<>();
			this.proposer = p;
		}
		
		private void sendBatch() {
			this.batch.flip();
			byte[] bytes = new byte[batch.limit()];
			batch.get(bytes);
			//System.err.println("Sending batch: " + bytes.length);
			this.proposer.propose(bytes);
			this.batch.clear();
		}
		
		@Override
		public void run() {
			long lastSendTime = System.currentTimeMillis();
			while (true) {
				long remaining = timeout - (System.currentTimeMillis() - lastSendTime);
				if (remaining <= 0) {
					lastSendTime = System.currentTimeMillis();
					if (this.batch.position() > 0) {
						this.sendBatch();
					}
				} else {
					byte[] cmd = null;
					try {
						cmd = this.toSend.poll(remaining, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
					}
					if (cmd != null) {
						if (cmd.length + 4 > this.batch.remaining()) { 
							// does not fit into remaining space
							lastSendTime = System.currentTimeMillis();
							this.sendBatch();
							if (cmd.length + 4 > this.batch.remaining()) {
								// larger than a batch!
								log.error("Value to send is larger than a batch!");
							} else {
								batch.putInt(cmd.length);
								batch.put(cmd);
							}
						} else {
							batch.putInt(cmd.length);
							batch.put(cmd);
						}
					}
				}
			}
		}
	}
	
	private static Logger log = Logger.getLogger(CommunicationService.class);
	private final byte GLOBAL_RING = 0;
	/**
	 * Queue of commands received. Will be filled as commands arrive.
	 */
	public final BlockingQueue<Command> commands;
	/**
	 * Queue of signals received. Will be filled as signals arrive.
	 */
	public final BlockingQueue<Command> signals;
	private Node paxos;
	private Map<Byte, Batcher> batchers;
	private Thread learnerThr;
	private volatile boolean stop = false;
	private int id;
	private Byte partition; 
	
	/** 
	 * Expects to receive a running paxos Node
	 * @param paxos
	 */
	public CommunicationService(int id, byte partition, Node paxos) {
		log.setLevel(Level.INFO);
		this.paxos = paxos;
		this.commands = new LinkedBlockingQueue<>();
		this.signals = new LinkedBlockingQueue<>();
		this.batchers = new HashMap<Byte, Batcher>();
		this.id = id;
		this.partition = Byte.valueOf(partition);
		for (RingDescription r: paxos.getRings()) {
			Proposer p = paxos.getProposer(r.getRingID());
			if (p != null) {
				Batcher b = new Batcher(p, BATCH_SIZE, BATCH_TIMEOUT);
				this.batchers.put(Byte.valueOf(Integer.valueOf(r.getRingID()).byteValue()), b);
			}
		}
	}
	
	/**
	 * Start service. After calling start(), commands/signals can be obtained by calling take() from the respective queues
	 */
	public void start() {
		// start batchers
		for (Batcher b: this.batchers.values()) {
			Thread batchThread = new Thread(b);
			batchThread.start();
		}
		
		// start learner
		learnerThr = new Thread(new Runnable() {
			@Override
			public void run() {
				final TDeserializer deserializer = new TDeserializer();
				log.debug(new StrBuilder().append("Starting communication service ").toString());
				while (!stop) {
					try {
						Decision d = paxos.getLearner().getDecisions().take();
						if (!d.getValue().isSkip()) {
							ByteBuffer buf = ByteBuffer.wrap(d.getValue().getValue());
							while (buf.remaining() > 4) {
								int size = buf.getInt();
								if (buf.remaining() < size) break;
								byte[] cmdBytes = new byte[size];
								buf.get(cmdBytes);
								Command c = new Command();
								deserializer.deserialize(c, cmdBytes);
								if (!c.getInvolvedPartitions().contains(partition)) {
									// command does not involve this partition.
									// Ignore
									log.error(new StrBuilder().append("Received a command we dont care about. Discarding.").toString());
									continue;
								}
								if (c.getType() == CommandType.SIGNAL.getValue()) {
									signals.add(c);
								} else {
									/*
									 * Here we are already sending signals for
									 * commands that can decide on fail/success
									 * without the signal result.
									 */
									switch (CommandType.findByValue(c.getType())) {
									/* these would be the read-only */
									case GETDIR:
									case ATTR:
									case OPEN:
									case RELEASE:
									case READ_BLOCKS:
										/* these can be replicated */
									case CHMOD:
									case CHOWN:
									case TRUNCATE:
									case WRITE_BLOCKS:
									case UTIME:
										if (c.getInvolvedPartitions().size() > 1) {
											signal(c.getReqId(), new Signal(partition.byteValue(), true), c.getInvolvedPartitions());
										}
										break;
									/* these might be multi-partition */
									case RMDIR:
									case MKDIR:
									case MKNOD:
									case SYMLINK:
									case UNLINK:
										if (c.getInvolvedPartitions().size() > 1) {
											signal(c.getReqId(), new Signal(partition.byteValue(), true), c.getInvolvedPartitions());
										}
										break;
									/* rename is a special case */
									case RENAME:
										RenameCmd r = c.getRename();
										if (c.getInvolvedPartitions().size() > 1) {
											if (!r.getPartitionFrom().contains(partition) && !r.getPartitionTo().contains(partition)) {
												// if only part of the
												// partitions with the parent
												// directories, can signal now
												signal(c.getReqId(), new Signal(partition.byteValue(), true), c.getInvolvedPartitions());
											}
										}
										break;
									default:
										break;
									}
									// add command to queue
									commands.add(c);
								}
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (TException e) {
						e.printStackTrace();
					}
				}
			}
		});
		learnerThr.start();
	}
	
	/**
	 * Stop service. No more items will be queued.
	 */
	public void stop() {
		stop = true;
		learnerThr.interrupt();
	}
	
	/**
	 * Atomically multicast command.
	 * @param ringId
	 * @param command
	 * @return A FutureDecision that can be waited on
	 */
	public void amcast(Command command) throws FSError {
		// right now, it either sends to the given partition or to the global ring
		byte ringid = GLOBAL_RING;
		if (command.getInvolvedPartitions().size() == 1) {
			ringid = command.getInvolvedPartitions().iterator().next().byteValue();
		}
		// FIXME: right now its not possible to submit to rings the replica is not part of
		log.debug(new StrBuilder().append("Submitting command to ring ").append(ringid).toString());
		Batcher b = this.batchers.get(Byte.valueOf(ringid));
		// TSerializer is not threadsafe, create a new one for each amcast. Is this too expensive?
		final TSerializer serializer = new TSerializer();
		try {
			try {
				b.toSend.put(serializer.serialize(command));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (TException e) {
			e.printStackTrace();
			throw new FSError(-1, "Error serializing message");
		}
	}
	
	/**
	 * Send a signal
	 * @param ringId
	 * @param command
	 * @throws FSError
	 */
	public void signal(long reqId, Signal signal, Set<Byte> involvedPartitions) throws FSError {
		// FIXME: only one replica sends the signal - here its fixed to replica 
		if (id != 0) {
			return;
		}
		// right now it just sends signals to the big ring
		Command cmd = new Command(CommandType.SIGNAL.getValue(), reqId, 0, involvedPartitions);
		cmd.setSignal(signal);
		this.amcast(cmd);
	}
	
	public BlockingQueue<Command> getCommands() {
		return commands;
	}

	public BlockingQueue<Command> getSignals() {
		return signals;
	}	
}
