package tenet.protocol.transport.tcp;

public class LastAckStateHandler implements StateHandler {

	@Override
	public void passiveOpen(TCB tcb) {
		// TODO Auto-generated method stub

	}

	@Override
	public void activeOpen(TCB tcb, int dstIP, int dstPort) {
		// TODO Auto-generated method stub

	}

	@Override
	public void send(TCB tcb, byte[] data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receive(TCB tcb) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close(TCB tcb) {
		// TODO Auto-generated method stub

	}

	@Override
	public void abort(TCB tcb) {
		TCPUtils.abort(tcb);
	}

	@Override
	public void segArrive(TCB tcb, TCPSegment segment, int ip) {
		TCPSegment reply;
		if (TCPUtils.checkSegmentSeq(segment.seqNum, segment.data.length, tcb.receiveNext, tcb.receiveWindow)) {
			if (segment.getRST()) {
				TCPUtils.deleteTCB(tcb);
			}
			else {
				if (segment.getSYN()) {
					TCPUtils.abort(tcb);
				}
				else {
					if (segment.getACK()) {
						if (segment.ackNum > tcb.sendUnACK && segment.ackNum <= tcb.sendNext) {
							tcb.sendUnACK = segment.ackNum;
							tcb.clean();
							if (tcb.finACK) {
								TCPUtils.deleteTCB(tcb);
								//TCPUtils.returnCloseOK(tcb);
							}
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
