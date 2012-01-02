package tenet.protocol.transport.tcp;

import java.util.LinkedList;
import java.util.Map;

import tenet.core.Simulator;

public class TCB {
	enum TCPState {
		Closed, Listen, SynSent, SynRcvd, Established, FinWait1, FinWait2, Closing, CloseWait, LastAck, TimeWait
	}

	public TCB(int handle, int port, TransportControlProtocol tcp) {
		this.handle = handle;
		this.tcp = tcp;
		localPort = port;
		localIP = tcp.getIP();
		currentState = TCPState.Closed;
		handler = new ClosedStateHandler();
	}
	
	void listen() {
		handler.passiveOpen(this);
	}
	
	void connect(int dstIP, int dstPort) {
		handler.activeOpen(this, dstIP, dstPort);
	}
	
	void close() {
		handler.close(this);
	}
	
	void abort() {
		handler.abort(this);
	}
	
	void send(byte[] data) {
		handler.send(this, data);
	}
	
	void receive() {
		handler.receive(this);
	}
	
	void segArrive(TCPSegment segment, int ip) {
		handler.segArrive(this, segment, ip);
	}
	
	TCB fork() {
		TCB child = new TCB(tcp.socket(), localPort, tcp);
		child.setState(this.currentState);
		return child;
	}
	
	void sendSegment(TCPSegment segment, int ip) {
		tcp.sendSegment(segment, ip);
		retransQueue.add(new RetransPair(segment, ip));
	}
	
	void clean() {
		dACK = 0;
		while (!retransQueue.isEmpty() && retransQueue.getFirst().segment.seqNum < sendUnACK) {
			if (retransQueue.getFirst().segment.getFIN()) 
				finACK = true;
			retransQueue.removeFirst();
		}
	}
	
	TCPSegment newSegment() {
		TCPSegment segment = new TCPSegment();
		segment.srcPort = localPort;
		segment.dstPort = remotePort;
		return segment;
	}
	
	byte[] extractSendBuffer() {
		byte[] result = new byte[0];
		while (sendBuffer.size() > 0) {
			byte[] old = result;
			byte[] data = sendBuffer.removeFirst();
			result = new byte[old.length + data.length];
			System.arraycopy(old, 0, result, 0, old.length);
			System.arraycopy(data, 0, result, old.length, data.length);
		}
		return result;
	}
	
	byte[] extractReceiveBuffer() {
		byte[] result = new byte[0];
		while (receiveBuffer.size() > 0) {
			byte[] old = result;
			byte[] data = receiveBuffer.removeFirst();
			result = new byte[old.length + data.length];
			System.arraycopy(old, 0, result, 0, old.length);
			System.arraycopy(data, 0, result, old.length, data.length);
		}
		return result;
	}
	
	void mergeReceive() {
		byte[] result = new byte[0];
		while (ooReceiveBuffer.size() > 0) {
			TCPSegment next = null;
			for (TCPSegment segment: ooReceiveBuffer) {
				if (segment.seqNum == receiveNext) {
					next = segment;
				}
			}
			if (next != null) {
				receiveNext += next.data.length;
				ooReceiveBuffer.remove(next);
				byte[] old = result;
				result = new byte[old.length + next.data.length];
				System.arraycopy(old, 0, result, 0, old.length);
				System.arraycopy(next.data, 0, result, old.length, next.data.length);
			}
			else 
				break;
		}
		if (result.length > 0) {
			receiveBuffer.add(result);
		}
		if (receiveBuffer.size() > 0 && receiveCalls > 0) {
			receiveCalls--;
			handler.receive(this);
		}
	}
	
	void retransmit() {
		retransQueue.getFirst().time = Simulator.GetTime();
		tcp.sendSegment(retransQueue.getFirst().segment, retransQueue.getFirst().ip);
	}
	
	void setState(TCPState state) {
		preState = currentState;
		currentState = state;
		switch(state) {
		case Closed:
			handler = new ClosedStateHandler();
			break;
		case Listen:
			handler = new ListenStateHandler();
			break;
		case SynSent:
			handler = new SynSentStateHandler();
			break;
		case SynRcvd:
			handler = new SynRcvdStateHandler();
			break;
		case Established:
			handler = new EstablishedStateHandler();
			if (sendBuffer.size() > 0) {
				handler.send(this, new byte[0]);
			}
			break;
		case FinWait1:
			handler = new FinWait1StateHandler();
			break;
		case FinWait2:
			handler = new FinWait2StateHandler();
			break;
		case Closing:
			handler = new ClosingStateHandler();
			break;
		case CloseWait:
			handler = new CloseWaitStateHandler();
			break;
		case LastAck:
			handler = new LastAckStateHandler();
			break;
		case TimeWait:
			handler = new TimeWaitStateHandler();
			break;
		}
	}
	
	void checkTime() {
		if (!Double.isNaN(timewait)) {
			if (timewait < Simulator.GetTime()) {
				handler.timeWaitTimeOut(this);
				
			}
		}
	}
	
	int localPort;
	int localIP;
	int remotePort;
	int remoteIP;
	
	int sendUnACK;
	int sendNext;
	int sendWindow = 20000;
	int initialSendSeqNum;
	
	int receiveNext;
	int receiveWindow = 20000;
	int initialReceiveSeqNum;
	
	int dACK;
	
	int handle;
	TransportControlProtocol tcp;
	TCPState currentState;
	TCPState preState;
	StateHandler handler;
	
	LinkedList<RetransPair> retransQueue = new LinkedList<RetransPair>();
	LinkedList<TCPSegment> ooReceiveBuffer = new LinkedList<TCPSegment>();		
	LinkedList<byte[]> sendBuffer = new LinkedList<byte[]> ();
	LinkedList<byte[]> receiveBuffer = new LinkedList<byte[]> ();
	int receiveCalls = 0;
	int waitClose = 0;
	boolean finACK = false;
	
	double timewait = Double.NaN;
	
	public static final double MSL = 4;
	
	class RetransPair {
		public RetransPair(TCPSegment segment, int ip) {
			this.segment = segment;
			this.ip = ip;
			this.time = Simulator.GetTime();
		}
		TCPSegment segment;
		int ip;
		double time;
	}
	
}
