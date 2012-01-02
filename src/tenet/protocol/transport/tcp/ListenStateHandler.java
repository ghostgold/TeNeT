package tenet.protocol.transport.tcp;

public class ListenStateHandler implements StateHandler {

	@Override
	public void passiveOpen(TCB tcb) {
		
	}

	@Override
	public void activeOpen(TCB tcb, int dstIP, int dstPort) {
		tcb.initialSendSeqNum = 1;
		tcb.sendUnACK = tcb.initialSendSeqNum;
		tcb.sendNext = tcb.initialSendSeqNum + 1;
		tcb.remoteIP = dstIP;
		tcb.remotePort = dstPort;
		
		TCPSegment segment = new TCPSegment();
		segment.srcPort = tcb.localPort;
		segment.dstPort = dstPort;
		segment.seqNum = tcb.sendUnACK;
		segment.setSYN();
		
		tcb.sendSegment(segment, dstIP);
		tcb.currentState = TCB.TCPState.SynSent;
		tcb.setState(TCB.TCPState.SynSent);
	}

	@Override
	public void send(TCB tcb, byte[] data) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_NOT_EXIST;
		error.type = TCPProtocol.ReturnType.SEND;
		tcb.tcp.returnResult(error);
	}

	@Override
	public void receive(TCB tcb) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_NOT_EXIST;
		error.type = TCPProtocol.ReturnType.RECEIVE;
		tcb.tcp.returnResult(error);
	}

	@Override
	public void close(TCB tcb) {
		tcb.tcp.connections.remove(new Integer(tcb.handle));		
		tcb.setState(TCB.TCPState.Closed);
		TCPUtils.returnCloseOK(tcb);
	}

	@Override
	public void abort(TCB tcb) {
		tcb.tcp.connections.remove(new Integer(tcb.handle));
		tcb.setState(TCB.TCPState.Closed);
	}

	@Override
	public void segArrive(TCB tcb, TCPSegment segment, int ip) {
		if (segment.getRST()) 
			return;
		else if (segment.getACK()) {
			TCPSegment reply = new TCPSegment();
			reply.srcPort = segment.dstPort;
			reply.dstPort = segment.srcPort;
			reply.setRST();
			reply.seqNum = segment.ackNum;
			tcb.sendSegment(segment, ip);
		}
		else if (segment.getSYN()) {
			TCB child = tcb.fork();
			child.receiveNext = segment.seqNum + 1;
			child.initialReceiveSeqNum = segment.seqNum;
			
			TCPProtocol.ReturnParam result = new TCPProtocol.ReturnParam();
			result.handle = tcb.handle;
			result.result = child.handle;
			result.status = TCPProtocol.ReturnStatus.OK;
			result.type = TCPProtocol.ReturnType.LISTEN;
			tcb.tcp.returnResult(result);
			tcb.tcp.connections.put(new Integer(child.handle), child);
			
			TCPSegment reply = new TCPSegment();
			reply.srcPort = segment.dstPort;
			reply.dstPort = segment.srcPort;
			reply.seqNum = child.initialSendSeqNum;
			reply.ackNum = child.receiveNext;
			reply.setACK();
			reply.setSYN();
			tcb.sendSegment(reply, ip);
			
			child.initialSendSeqNum = 1;
			child.sendUnACK = child.initialSendSeqNum;
			child.sendNext = child.initialSendSeqNum;
			child.remoteIP = ip;
			child.remotePort = segment.srcPort;
			
			child.setState(TCB.TCPState.SynRcvd);
		}
	}

	@Override
	public void rexmtTimeOut(TCB tcb) {
		// TODO Auto-generated method stub

	}

	@Override
	public void timeWaitTimeOut(TCB tcb) {
		// TODO Auto-generated method stub

	}

	@Override
	public void userTimeOut(TCB tcb) {
		// TODO Auto-generated method stub

	}

}
