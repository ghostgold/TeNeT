package tenet.protocol.transport.tcp;

public class EstablishedStateHandler implements StateHandler {

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
		if (data.length > 0)
			TCPUtils.queueSend(tcb, data);
		if (tcb.sendNext < tcb.sendUnACK + tcb.sendWindow) {
			data = tcb.extractSendBuffer();
			TCPSegment segment = tcb.newSegment();
			segment.seqNum = tcb.sendNext;
			segment.ackNum = tcb.receiveNext;
			segment.setACK();
			segment.data = data;
			tcb.sendSegment(segment, tcb.remoteIP);
			tcb.sendNext += data.length;
			if (tcb.waitClose > 0 && tcb.sendBuffer.size() == 0) {
				this.close(tcb);
			}	
		}
		else {
			
		}
	}
	

	@Override
	public void receive(TCB tcb) {
		byte[] data = tcb.extractReceiveBuffer();
		if (data.length > 0) {
			TCPProtocol.ReturnParam success = new TCPProtocol.ReturnParam();
			success.handle = tcb.handle;
			success.status = TCPProtocol.ReturnStatus.OK;
			success.type = TCPProtocol.ReturnType.RECEIVE;
			success.result = data.length;
			success.data = data;
			tcb.tcp.returnResult(success);
		}
		else {
			tcb.receiveCalls++;
		}
	}

	@Override
	public void close(TCB tcb) {
		//TCPUtils.returnCloseOK(tcb);
		if (tcb.sendBuffer.size() > 0) {
			tcb.waitClose++;
		}
		else {
			TCPSegment reply;
			reply = tcb.newSegment();
			reply.seqNum = tcb.sendNext;
			reply.ackNum = tcb.receiveNext;
			reply.setFIN();
			reply.setACK();
			tcb.sendSegment(reply, tcb.remoteIP);
			tcb.sendNext ++;
			tcb.setState(TCB.TCPState.FinWait1);
			TCPUtils.resetReceive(tcb);
		}
	}

	@Override
	public void abort(TCB tcb) {
		TCPUtils.returnAbortOK(tcb);		
		TCPSegment reply;
		reply = tcb.newSegment();
		reply.seqNum = tcb.sendNext;
		reply.setRST();
		tcb.sendSegment(reply, tcb.remoteIP);
		TCPUtils.resetSendAndReceive(tcb);
		TCPUtils.deleteTCB(tcb);
	}

	public void es3(TCB tcb, TCPSegment segment, int ip) {
		tcb.ooReceiveBuffer.add(segment);
		if (segment.seqNum == tcb.receiveNext) 
			tcb.mergeReceive();
		TCPSegment reply = tcb.newSegment();
		reply.seqNum = tcb.sendNext;
		reply.ackNum = tcb.receiveNext;
		reply.setACK();
		tcb.sendSegment(reply, ip);
		
		if (segment.getFIN()) {
			TCPUtils.finBitProcessing(tcb.sendNext, tcb.receiveNext, segment.seqNum, tcb);
			tcb.setState(TCB.TCPState.CloseWait);
			reply = tcb.newSegment();
			reply.seqNum = tcb.sendNext;
			reply.ackNum = tcb.receiveNext;
			reply.setFIN();
			reply.setACK();
			tcb.sendSegment(reply, ip);
			tcb.setState(TCB.TCPState.LastAck);
			tcb.sendNext++;
			TCPUtils.resetReceive(tcb);			
		}
	}
	
	@Override
	public void segArrive(TCB tcb, TCPSegment segment, int ip) {
		TCPSegment reply;
		if (TCPUtils.checkSegmentSeq(segment.seqNum, segment.data.length, tcb.receiveNext, tcb.receiveWindow)) {
			if (segment.getRST()) {
				TCPUtils.resetSendAndReceive(tcb);
				TCPUtils.deleteTCB(tcb);
			}
			else {
				if (segment.getSYN()) {
					abort(tcb);
				}
				else {
					if (segment.getACK()) {
						if (segment.ackNum > tcb.sendUnACK && segment.ackNum <= tcb.sendNext) {
							tcb.sendUnACK = segment.ackNum;
							tcb.clean();
							es3(tcb, segment, ip);							
						}
						else {
							if (segment.ackNum == tcb.sendUnACK) {
								tcb.dACK++;
								if (tcb.dACK == 3) {
									tcb.retransmit();
									tcb.dACK = 0;
								}
								es3(tcb, segment, ip);
							}
						}
					}
					else {
//						tcb.ooReceiveBuffer.add(segment);
//						if (tcb.receiveNext == segment.seqNum) 
//							tcb.mergeReceive();
//						reply = tcb.newSegment();
//						reply.seqNum = tcb.sendNext;
//						reply.ackNum = tcb.receiveNext;
//						tcb.sendSegment(reply, ip);
						if (segment.getFIN()) {
							TCPUtils.finBitProcessing(tcb.sendNext, tcb.receiveNext, segment.seqNum, tcb);
							tcb.setState(TCB.TCPState.CloseWait);
							reply = tcb.newSegment();
							reply.seqNum = tcb.sendNext;
							reply.ackNum = tcb.receiveNext;
							reply.setFIN();
							reply.setACK();
							tcb.sendSegment(reply, ip);
							tcb.setState(TCB.TCPState.LastAck);
							tcb.sendNext++;
							TCPUtils.resetReceive(tcb);
						}
					}
				}
			}
		}
		else {
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

	}

	@Override
	public void timeWaitTimeOut(TCB tcb) {
	}

	@Override
	public void userTimeOut(TCB tcb) {
	}
}
