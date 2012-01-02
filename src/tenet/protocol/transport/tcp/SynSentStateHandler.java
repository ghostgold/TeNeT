package tenet.protocol.transport.tcp;

public class SynSentStateHandler implements StateHandler {

	@Override
	public void passiveOpen(TCB tcb) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_NOT_EXIST;
		error.type = TCPProtocol.ReturnType.LISTEN;
		tcb.tcp.returnResult(error);		
	}

	@Override
	public void activeOpen(TCB tcb, int dstIP, int dstPort) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_NOT_EXIST;
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
		TCPUtils.resetSendAndReceive(tcb);
		tcb.tcp.connections.remove(new Integer(tcb.handle));
		tcb.setState(TCB.TCPState.Closed);
	}

	@Override
	public void abort(TCB tcb) {	
		TCPUtils.resetSendAndReceive(tcb);
		
		tcb.tcp.connections.remove(new Integer(tcb.handle));
		tcb.setState(TCB.TCPState.Closed);	
	}

	@Override
	public void segArrive(TCB tcb, TCPSegment segment, int ip) {
		TCPSegment reply;
		if (segment.getACK()) {
			if (segment.ackNum <= tcb.initialSendSeqNum || segment.ackNum > tcb.sendNext) {
				reply = new TCPSegment();
				reply.srcPort = segment.dstPort;
				reply.dstPort = segment.srcPort;
				reply.seqNum = segment.ackNum;
				reply.setRST();
				tcb.sendSegment(reply, ip);
			}
			else { //segment.ackNum <= tcb.initialSendSeqNum || segment.ackNum > tcb.sendNext
				if (segment.ackNum >= tcb.sendUnACK && segment.ackNum <= tcb.sendNext) {
					if (segment.getRST()) {
						TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
						error.handle = tcb.handle;
						error.status = TCPProtocol.ReturnStatus.CONN_RESET;
						error.type = TCPProtocol.ReturnType.CONNECT;
						error.result = -1;
						tcb.tcp.returnResult(error);	
						tcb.tcp.connections.remove(new Integer(tcb.handle));
						tcb.setState(TCB.TCPState.Closed);
					}
					else { //segment.getRST()
						if (segment.getSYN()) {
							tcb.receiveNext = segment.seqNum + 1;
							tcb.initialReceiveSeqNum = segment.seqNum;
							tcb.sendUnACK = segment.ackNum;
							tcb.clean();
							if (tcb.sendUnACK > tcb.initialSendSeqNum) {
								reply = new TCPSegment();
								reply.srcPort = segment.dstPort;
								reply.dstPort = segment.srcPort;
								reply.seqNum = tcb.sendNext;
								reply.ackNum = tcb.receiveNext;
								reply.setACK();
								tcb.sendSegment(reply, ip);
								tcb.setState(TCB.TCPState.Established);
								TCPUtils.returnConnectOK(tcb);
							}
							else { //tcb.sendUnACK > tcb.initialSendSeqNum
								reply = new TCPSegment();
								reply.srcPort = segment.dstPort;
								reply.dstPort = segment.srcPort;
								reply.seqNum = tcb.initialSendSeqNum;
								reply.ackNum = tcb.receiveNext;
								reply.setSYN();
								reply.setACK();
								tcb.sendSegment(reply, ip);
								tcb.setState(TCB.TCPState.SynRcvd);
							}							
						}
						else {//segment.getSYN()
							
						}
					}
				}
				else { //segment.ackNum >= tcb.sendUnACK && segment.ackNum <= tcb.sendNext
					reply = new TCPSegment();
					reply.srcPort = segment.dstPort;
					reply.dstPort = segment.srcPort;
					reply.seqNum = segment.ackNum;
					reply.setRST();
					tcb.sendSegment(reply, ip);					
				}
			}
		}
		else { //segment.getACK()
			if (segment.getRST()) {
				
			}
			else { //segment.getRST()
				if (segment.getSYN()) {
					tcb.receiveNext = segment.seqNum;
					tcb.initialReceiveSeqNum = segment.seqNum;
					reply = new TCPSegment();
					reply.srcPort = segment.dstPort;
					reply.dstPort = segment.srcPort;
					reply.seqNum = tcb.initialSendSeqNum;
					reply.ackNum = tcb.receiveNext;
					reply.setACK();
					reply.setSYN();
					tcb.sendSegment(reply, ip);
					tcb.setState(TCB.TCPState.SynRcvd);
				}
				else {//segment.getSYN()
				}
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
