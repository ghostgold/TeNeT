package tenet.protocol.transport.tcp;

import tenet.protocol.transport.tcp.TCPProtocol.ReturnType;

public interface StateHandler {
	void passiveOpen(TCB tcb);
	void activeOpen(TCB tcb, int dstIP, int dstPort);
	void send(TCB tcb, byte[] data);
	void receive(TCB tcb);
	void close(TCB tcb);
	void abort(TCB tcb);
	
	void segArrive(TCB tcb, TCPSegment segment, int ip);
	
	void rexmtTimeOut(TCB tcb);
	void timeWaitTimeOut(TCB tcb);
	void userTimeOut(TCB tcb);
}

class TCPUtils {
	public static boolean checkSegmentSeq(int segseq, int seglen, int rcvnxt, int rcvwnd) {
//		return true;
		if (rcvwnd == 0) {
			if (seglen == 0) {
				if (segseq == rcvnxt) {
					return true;
				}
				else
					return false;
			}
			else {
				return false;
			}
		}
		else  {
			if (seglen == 0) {
				if (rcvnxt <= segseq && segseq < rcvnxt + rcvwnd)
					return true;
				else 
					return false;
			}
			else { 
				if ((rcvnxt <= segseq && segseq < rcvnxt + rcvwnd) || ( rcvnxt <= segseq + seglen-1  && segseq + seglen-1 < rcvnxt + rcvwnd))
					return true;
				else 
					return false;
			}
		}
	}
	
	public static void resetSendAndReceive(TCB tcb) {
		if (tcb.sendBuffer.size() > 0) {
			TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
			error.handle = tcb.handle;
			error.status = TCPProtocol.ReturnStatus.CONN_RESET;
			error.type = TCPProtocol.ReturnType.SEND;
			error.result = -1;
			tcb.tcp.returnResult(error);			
		}
		if (tcb.receiveCalls > 0) {
			TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
			error.handle = tcb.handle;
			error.status = TCPProtocol.ReturnStatus.CONN_RESET;
			error.type = TCPProtocol.ReturnType.RECEIVE;
			error.result = -1;
			tcb.tcp.returnResult(error);			
		}		
	}
	
	public static void resetReceive(TCB tcb) {
		while (tcb.receiveCalls > 0) {
			TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
			error.handle = tcb.handle;
			error.status = TCPProtocol.ReturnStatus.CONN_CLOSING;
			error.type = TCPProtocol.ReturnType.RECEIVE;
			error.result = -1;
			tcb.tcp.returnResult(error);
			tcb.receiveCalls--;
		}		
	}
	
	public static void deleteTCB(TCB tcb) {
		tcb.tcp.connections.remove(new Integer(tcb.handle));
		tcb.setState(TCB.TCPState.Closed);						
	}

	public static void finBitProcessing(int sndnxt, int rcvnxt, int segseq, TCB tcb) {
		tcb.receiveNext = segseq + 1;
		TCPSegment reply;
		reply = tcb.newSegment();
		reply.seqNum  = tcb.sendNext;
		reply.ackNum = tcb.receiveNext;
		reply.setACK();
		TCPUtils.resetReceive(tcb);
		tcb.sendSegment(reply, tcb.remoteIP);
	}

	public static void abort(TCB tcb) {
		TCPSegment reply;
		reply = tcb.newSegment();
		reply.seqNum = tcb.sendNext;
		reply.setRST();
		tcb.sendSegment(reply, tcb.remoteIP);
		TCPUtils.resetSendAndReceive(tcb);
		TCPUtils.deleteTCB(tcb);			
	}
	
	public static void queueSend(TCB tcb, byte[] data) {
		tcb.sendBuffer.add(data);
		TCPProtocol.ReturnParam success = new TCPProtocol.ReturnParam();
		success.handle = tcb.handle;
		success.status = TCPProtocol.ReturnStatus.OK;
		success.type = TCPProtocol.ReturnType.SEND;
		success.result = data.length;
		tcb.tcp.returnResult(success);					
	}

	public static void returnListenOK(TCB tcb) {
		TCPProtocol.ReturnParam success = new TCPProtocol.ReturnParam();
		success.handle = tcb.handle;
		success.status = TCPProtocol.ReturnStatus.OK;
		success.type = TCPProtocol.ReturnType.LISTEN;
		success.result = 0;
		tcb.tcp.returnResult(success);
	}

	public static void returnCloseOK(TCB tcb) {
		TCPProtocol.ReturnParam success = new TCPProtocol.ReturnParam();
		success.handle = tcb.handle;
		success.status = TCPProtocol.ReturnStatus.OK;
		success.type = TCPProtocol.ReturnType.CLOSE;
		success.result = 0;
		tcb.tcp.returnResult(success);
	}

	public static void returnConnectOK(TCB tcb) {
		TCPProtocol.ReturnParam success = new TCPProtocol.ReturnParam();
		success.handle = tcb.handle;
		success.status = TCPProtocol.ReturnStatus.OK;
		success.type = TCPProtocol.ReturnType.CONNECT;
		success.result = 0;
		tcb.tcp.returnResult(success);		
	}

	public static void returnNoConnectionError(ReturnType type, int handle, TransportControlProtocol tcp) {
		TCPProtocol.ReturnParam error = new TCPProtocol.ReturnParam();
		error.handle = handle;
		error.status = TCPProtocol.ReturnStatus.CONN_NOT_EXIST;
		error.type = TCPProtocol.ReturnType.CONNECT;
		error.result = 0;
		tcp.returnResult(error);		
	}

	public static void returnAbortOK(TCB tcb) {
		TCPProtocol.ReturnParam success = new TCPProtocol.ReturnParam();
		success.handle = tcb.handle;
		success.status = TCPProtocol.ReturnStatus.OK;
		success.type = TCPProtocol.ReturnType.ABORT;
		success.result = 0;
		tcb.tcp.returnResult(success);			
	}
}