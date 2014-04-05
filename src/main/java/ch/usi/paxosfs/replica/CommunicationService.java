package ch.usi.paxosfs.replica;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;

import ch.usi.da.paxos.api.Proposer;
import ch.usi.da.paxos.ring.Node;
import ch.usi.da.paxos.ring.RingDescription;
import ch.usi.da.paxos.storage.Decision;
import ch.usi.paxosfs.replica.commands.Command;
import ch.usi.paxosfs.replica.commands.CommandType;
import ch.usi.paxosfs.replica.commands.Signal;
import ch.usi.paxosfs.rpc.FSError;

/**
 * Implements a higher level of abstraction to the communication done between the replicas
 * @author pacheco
 *
 */
public class CommunicationService {
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
	private Map<Byte, Proposer> proposers;
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
		this.proposers = new HashMap<Byte, Proposer>();
		for (RingDescription r: paxos.getRings()) {
			Proposer p = paxos.getProposer(r.getRingID());
			if (p != null) {
				this.proposers.put(Byte.valueOf(Integer.valueOf(r.getRingID()).byteValue()), p);
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
						/* FIXME: commands that don't need to exchange state can send signals as soon as the command is delivered. It would be done here */
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
	 * Atomically multicast command.
	 * @param ringId
	 * @param command
	 * @return A FutureDecision that can be waited on
	 */
	public void amcast(Command command, Set<Byte> partitions) throws FSError {
		// right now, it either sends to the given partition or to the global ring
		byte ringid = GLOBAL_RING;
		if (partitions.size() == 1) {
			ringid = partitions.iterator().next().byteValue();
		}
		System.out.println("Submitting command " + command.getReqId() + " to ring " + ringid);
		Proposer p = this.proposers.get(Byte.valueOf(ringid));
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
	 * Send a signal
	 * @param ringId
	 * @param command
	 * @throws FSError
	 */
	public void signal(long reqId, Signal signal, Set<Byte> partitions) throws FSError {
		// right now it just sends signals to the big ring
		Command cmd = new Command(CommandType.SIGNAL.getValue(), reqId, 0);
		cmd.setSignal(signal);
		this.amcast(cmd, partitions);
	}
	
	public BlockingQueue<Command> getCommands() {
		return commands;
	}

	public BlockingQueue<Command> getSignals() {
		return signals;
	}	
}
