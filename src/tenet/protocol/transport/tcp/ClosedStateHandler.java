package tenet.protocol.transport.tcp;

public class ClosedStateHandler implements StateHandler {

	@Override
	public void passiveOpen(TCB tcb) {
		tcb.setState(TCB.TCPState.Listen);
		TCPUtils.returnListenOK(tcb);
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
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_NOT_EXIST;
		error.type = TCPProtocol.ReturnType.CLOSE;
		tcb.tcp.returnResult(error);
	}

	@Override
	public void abort(TCB tcb) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_NOT_EXIST;
		error.type = TCPProtocol.ReturnType.ABORT;
		tcb.tcp.returnResult(error);
	}

	@Override
	public void segArrive(TCB tcb, TCPSegment segment, int ip) {
		TCPSegment reply = new TCPSegment();
		reply.srcPort = segment.dstPort;
		reply.dstPort = segment.srcPort;
		if (segment.getACK()) {
			reply.setRST();
			reply.setACK();
			reply.seqNum = segment.seqNum + segment.data.length;
		}
		else {
			reply.setRST();
			reply.seqNum = segment.ackNum;
		}
		tcb.sendSegment(reply, ip);
	}

	@Override
	public void rexmtTimeOut(TCB tcb) {

	}

	@Override
	public void timeWaitTimeOut(TCB tcb) {
	}

	@Override
	public void userTimeOut(TCB tcb) {
	}
}
