package tenet.protocol.transport.tcp;

public class CloseWaitStateHandler implements StateHandler {

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
		new EstablishedStateHandler().send(tcb, data);
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
			TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
			error.handle = tcb.handle;
			error.status = TCPProtocol.ReturnStatus.CONN_CLOSING;
			error.type = TCPProtocol.ReturnType.RECEIVE;
			tcb.tcp.returnResult(error);			
		}
	}

	@Override
	public void close(TCB tcb) {
		new EstablishedStateHandler().close(tcb);
	}

	@Override
	public void abort(TCB tcb) {
		TCPUtils.abort(tcb);
		TCPUtils.returnAbortOK(tcb);
	}

	void cw1(TCB tcb, TCPSegment segment, int ip) {
		if (segment.getACK()) {
			
		}
		else {
			if (segment.ackNum > tcb.sendUnACK && segment.ackNum <= tcb.sendNext) {
				tcb.sendUnACK = segment.ackNum;
				tcb.clean();
				if (segment.getFIN()) {
					TCPUtils.finBitProcessing(tcb.sendNext, tcb.receiveNext, segment.seqNum, tcb);
				}
			}
			
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
					TCPUtils.abort(tcb);
				}
				else {
					cw1(tcb, segment, ip);
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
