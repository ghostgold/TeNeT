package tenet.protocol.transport.tcp;

import tenet.protocol.IProtocol;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.statecontrol.IStateSetable;
import tenet.protocol.transport.ITransportLayer;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IRegistryableService;

public interface TCPProtocol extends IStateSetable, ITransportLayer, IRegistryableService<Integer>{

	
	public static final int INT_RETURN=0x7100001;
	
	enum ReturnStatus {
		CONN_CLOSING,CONN_RESET,CONN_ALREADY_EXIST,CONN_NOT_EXIST,CONN_REFUSED,OK,OTHER_ERROR
	}
	
	enum ReturnType{
		LISTEN,CONNECT,ABORT,SEND,RECEIVE,CLOSE,ACCEPT
	}
	
	class ReturnParam extends InterruptParam {
		public ReturnType type;
		public int handle;
		public ReturnStatus status;
		public int result;
		public byte[] data;
	}
	//The int means the handle
	public int socket();//return a new handle
	public int bind(int handle,int port);
	public void listen(int handle);
	public void connect(int handle,int dstIP,int dstPort);
	public void close(int handle);
	public void accept(int handle);
	public void abort(int handle);
	
	public void send(int handle,byte[] data);	
	public void receive(int handle);
}
