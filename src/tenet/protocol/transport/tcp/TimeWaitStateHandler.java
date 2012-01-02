package tenet.protocol.transport.tcp;

import tenet.core.Simulator;

public class TimeWaitStateHandler implements StateHandler {

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
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = tcb.handle;
		error.status = TCPProtocol.ReturnStatus.CONN_CLOSING;
		error.type = TCPProtocol.ReturnType.RECEIVE;
		tcb.tcp.returnResult(error);
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
		TCPProtocol.ReturnParam success = new TCPProtocol.ReturnParam();
		success.handle = tcb.handle;
		success.status = TCPProtocol.ReturnStatus.CONN_CLOSING;
		success.type = TCPProtocol.ReturnType.ABORT;
		success.result = 0;
		tcb.tcp.returnResult(success);
		TCPUtils.deleteTCB(tcb);
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
							if (segment.getFIN()) {
								TCPUtils.finBitProcessing(tcb.sendNext, tcb.receiveNext, segment.seqNum, tcb);
								tcb.timewait = Simulator.GetTime() + 2 * TCB.MSL;
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
		TCPUtils.returnCloseOK(tcb);
		TCPUtils.deleteTCB(tcb);
		//TCPUtils.returnCloseOK(tcb);
	}

	@Override
	public void userTimeOut(TCB tcb) {
		// TODO Auto-generated method stub

	}

}
