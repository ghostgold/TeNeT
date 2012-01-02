package tenet.protocol.transport.tcp;

import tenet.core.Simulator;

public class FinWait1StateHandler implements StateHandler {

	@Override
	public void passiveOpen(TCB tcb) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_CLOSING;
		error.type = TCPProtocol.ReturnType.LISTEN;
		tcb.tcp.returnResult(error);
	}

	@Override
	public void activeOpen(TCB tcb, int dstIP, int dstPort) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_CLOSING;
		error.type = TCPProtocol.ReturnType.CONNECT;
		tcb.tcp.returnResult(error);
	}

	@Override
	public void send(TCB tcb, byte[] data) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_CLOSING;
		error.type = TCPProtocol.ReturnType.SEND;
		tcb.tcp.returnResult(error);
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
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_CLOSING;
		error.type = TCPProtocol.ReturnType.CLOSE;
		tcb.tcp.returnResult(error);
	}

	@Override
	public void abort(TCB tcb) {
		TCPUtils.abort(tcb);
	}

	void fw2(TCB tcb, TCPSegment segment, int ip) {
		tcb.ooReceiveBuffer.add(segment);
		if (segment.seqNum == tcb.receiveNext) 
			tcb.mergeReceive();
		TCPSegment reply = tcb.newSegment();
		reply.seqNum = tcb.sendNext;
		reply.ackNum = tcb.receiveNext;
		reply.setACK();
		tcb.sendSegment(reply, ip);
		if (tcb.finACK) {		
			if (segment.getFIN()) {
				TCPUtils.finBitProcessing(tcb.sendNext, tcb.receiveNext, segment.seqNum, tcb);
				tcb.timewait = Simulator.GetTime() + 2 * TCB.MSL;
				tcb.setState(TCB.TCPState.TimeWait);
			}
			else {
				tcb.setState(TCB.TCPState.FinWait2);
			}
		}
		else {
			if (segment.getFIN()) {
				TCPUtils.finBitProcessing(tcb.sendNext, tcb.receiveNext, segment.seqNum, tcb);
				tcb.setState(TCB.TCPState.Closing);
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
					abort(tcb);
				}
				else {
					if (segment.getACK()) {
						if (segment.ackNum > tcb.sendUnACK && segment.ackNum <= tcb.sendNext) {
							tcb.sendUnACK = segment.ackNum;
							tcb.clean();
						}
						fw2(tcb, segment, ip);
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
