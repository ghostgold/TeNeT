package tenet.protocol.transport.tcp;

public class SynRcvdStateHandler implements StateHandler {

	@Override
	public void passiveOpen(TCB tcb) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_ALREADY_EXIST;
		error.type = TCPProtocol.ReturnType.LISTEN;
		tcb.tcp.returnResult(error);
	}

	@Override
	public void activeOpen(TCB tcb, int dstIP, int dstPort) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_ALREADY_EXIST;
		error.type = TCPProtocol.ReturnType.CONNECT;
		tcb.tcp.returnResult(error);
	}

	@Override
	public void send(TCB tcb, byte[] data) {
		TCPUtils.queueSend(tcb, data);
	}

	@Override
	public void receive(TCB tcb) {
		tcb.receiveCalls++;
	}

	@Override
	public void close(TCB tcb) {
		if (tcb.sendBuffer.size() > 0) {
			tcb.waitClose++;
		}
		else {
			TCPSegment reply = new TCPSegment();
			reply.srcPort = tcb.localPort;
			reply.dstPort = tcb.remotePort;
			reply.seqNum = tcb.sendNext;
			reply.setFIN();
			tcb.sendSegment(reply, tcb.remoteIP);
			tcb.setState(TCB.TCPState.FinWait1);
		}
	}

	@Override
	public void abort(TCB tcb) {
		TCPSegment reply = tcb.newSegment();
		reply.seqNum = tcb.sendNext;
		reply.setRST();
		tcb.sendSegment(reply, tcb.remoteIP);
		TCPUtils.resetSendAndReceive(tcb);
		TCPUtils.deleteTCB(tcb);
	}

	@Override
	public void segArrive(TCB tcb, TCPSegment segment, int ip) {
		TCPSegment reply;
		if (TCPUtils.checkSegmentSeq(segment.seqNum, segment.data.length, tcb.receiveNext, tcb.receiveWindow)) {
			if (segment.getRST()) {
				if (tcb.preState == TCB.TCPState.Listen) {
					//XXX problem page 13 
				}
				else {
					//XXX problem page 13
					TCPUtils.deleteTCB(tcb);
				}
			}
			else {//segment.getRST()
				if (segment.getSYN()) {
					if (tcb.preState == TCB.TCPState.SynSent) {
						if (segment.getACK()) {
							tcb.receiveNext = segment.seqNum + 1;
							tcb.initialReceiveSeqNum = segment.seqNum;
							tcb.sendUnACK = segment.ackNum;
							tcb.setState(TCB.TCPState.Established);
							TCPUtils.returnConnectOK(tcb);
						}
					}
					else 
						abort(tcb);
				}
				else {
					if (segment.getACK()) {
						if (tcb.sendUnACK <= segment.ackNum && segment.ackNum <= tcb.sendNext) {
							tcb.setState(TCB.TCPState.Established);
						}
						else {
							if (segment.getFIN()) {
								TCPUtils.finBitProcessing(tcb.sendNext, tcb.receiveNext, segment.seqNum, tcb);
								tcb.setState(TCB.TCPState.CloseWait);
							}
							else {
								reply = tcb.newSegment();
								reply.seqNum = tcb.sendNext;
								reply.setRST();
								TCPUtils.resetSendAndReceive(tcb);
								TCPUtils.deleteTCB(tcb);
							}
						}
					}
					else {
						//Do nothing
					}
				}
			}
		}
		else {//TCPUtils.checkSegmentSeq(segment.seqNum, segment.data.length, tcb.receiveNext, tcb.receiveWindow)
			if (segment.getRST()) {
			}
			else {
				reply = tcb.newSegment();
				reply.seqNum = tcb.sendNext;
				reply.ackNum = tcb.receiveNext;
				reply.setACK();
				tcb.sendSegment(reply, ip);
			}
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
