package ch.inf.paxosfs.replica;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;

import ch.inf.paxosfs.replica.commands.Command;
import ch.inf.paxosfs.replica.commands.CommandType;
import ch.inf.paxosfs.rpc.FSError;
import ch.usi.da.paxos.api.Proposer;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;
import ch.usi.da.paxos.storage.Decision;

/**
 * Implements a higher level of abstraction to the communication done between the replicas
 * @author pacheco
 *
 */
public class CommunicationService {
	private final int GLOBAL_RING = 0;
	/**
	 * Queue of commands received. Will be filled as commands arrive.
	 */
	public final BlockingQueue<Command> commands;
	/**
	 * Queue of signals received. Will be filled as signals arrive.
	 */
	public final BlockingQueue<Command> signals;
	private Node paxos;
	private Map<Integer, Proposer> proposers;
	private Thread learnerThr;
	private volatile boolean stop = false; 
	
	/** 
	 * Expects to receive a running paxos Node
	 * @param paxos
	 */
	public CommunicationService(Node paxos) {
		this.paxos = paxos;
		this.commands = new LinkedBlockingQueue<>();
		this.signals = new LinkedBlockingQueue<>();
		this.proposers = new HashMap<Integer, Proposer>();
		for (RingDescription r: paxos.getRings()) {
			Proposer p = paxos.getProposer(r.getRingID());
			if (p != null) {
				this.proposers.put(r.getRingID(), p);
			}
		}
	}
	
	/**
	 * Start service. After calling start(), commands/signals can be obtained by calling take() from the respective queues
	 */
	public void start() {
		learnerThr = new Thread(new Runnable() {
			@Override
			public void run() {
				final TDeserializer deserializer = new TDeserializer();
				System.out.println("Starting communication service");
				while (!stop) {
					try {
						Decision d = paxos.getLearner().getDecisions().take();
						if (!d.getValue().isSkip()) {
							System.out.println("Delivered command: " + d);
							Command c = new Command();
							deserializer.deserialize(c, d.getValue().getValue());
							if (c.getType() == CommandType.SIGNAL.getValue()) {
								signals.add(c);
							} else {
								commands.add(c);
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
	 * Atomically multicast command given command.involvedPartitions.
	 * @param ringId
	 * @param command
	 * @return A FutureDecision that can be waited on
	 */
	public void amcast(Command command) throws FSError {
		int ringid = GLOBAL_RING;
		if (command.getInvolvedPartitionsSize() == 1) {
			ringid = command.getInvolvedPartitions().get(0);
		}
		System.out.println("Submitting command " + command.getReqId() + " to ring " + ringid);
		Proposer p = this.proposers.get(ringid);
		// TODO: Replica not part of the ring. Implement using thrift client if we need to support this.
		assert(p != null);
		// TSerializer is not threadsafe, create a new one for each amcast. Is this too expensive?
		final TSerializer serializer = new TSerializer();
		try {
			p.propose(serializer.serialize(command));
		} catch (TException e) {
			e.printStackTrace();
			throw new FSError(-1, "Error serializing message");
		}
	}
	
	/**
	 * Send a signal to the partitions command.involvedPartitions
	 * @param ringId
	 * @param command
	 * @throws FSError
	 */
	public void signal(Command command) throws FSError {
		assert(command.getType() == CommandType.SIGNAL.getValue());
		// FIXME: right now, signals are sent to the global ring since its trivial to get a local proposer. Change this later
		command.involvedPartitions.add(this.GLOBAL_RING);
		this.amcast(command);
	}
	
	public BlockingQueue<Command> getCommands() {
		return commands;
	}

	public BlockingQueue<Command> getSignals() {
		return signals;
	}	
	
//	private class ThriftProposerClient implements Proposer {
//		@Override
//		public FutureDecision propose(byte[] b) {
//			return null;
//		}
//	}	
}
