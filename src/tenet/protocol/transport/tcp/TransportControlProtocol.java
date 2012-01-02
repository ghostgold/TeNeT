package tenet.protocol.transport.tcp;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import tenet.core.Simulator;
import tenet.node.INode;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.INetworkLayer;
import tenet.protocol.network.ipv4.IPProtocol;
import tenet.protocol.network.ipv4.IPReceiveParam;
import tenet.util.ByteLib;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IService;

public class TransportControlProtocol extends InterruptObject implements TCPProtocol  {

	public TransportControlProtocol () {
		wait(TIMER, 1.0);
	}
	@Override
	public void disable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isEnable() {
		return true;
	}

	@Override
	public void dump() {
	}

	@Override
	public String getIdentify() {
		return null;
	}

	@Override
	public String getName() {
		return "TCP";
	}

	@Override
	public void attachTo(IService<?> service) {
		if (service instanceof IPProtocol) {
			ipservice = (IPProtocol)service;
			this.wait(IPProtocol.recPacketSignal, Double.NaN);
		}
		else if (service instanceof INode) {
			node = (INode)service;
		}
	}

	@Override
	public void detachFrom(IService<?> service) {
		if (service == ipservice) {
			ipservice = null;
		}
		else if (service == node) {
			node = null;
		}
	}

	@Override
	public Integer getUniqueID() {
		return 6;
	}

	@Override
	public void setUniqueID(Integer id) {
	}

		@Override
	public void registryClient(IClient<Integer> client) {
		clients.add(client);
		client.attachTo(this);
	}

	@Override
	public void unregistryClient(Integer id) {
		for (IClient<Integer> client: clients) {
			if (client.getUniqueID() == id) {
				unregistryClient(client);
				return;
			}
		}
	}

	@Override
	public void unregistryClient(IClient<Integer> client) {
		clients.remove(client);
		client.detachFrom(this);
	}
	
	
	@Override
	public int socket() {
		return handleCounter++;
	}

	@Override
	public int bind(int handle, int port) {
		if (connections.get(new Integer(handle)) == null)
			connections.put(new Integer(handle), new TCB(handle, port, this));
		return 0;
	}

	@Override
	public void listen(int handle) {
		if (connections.get(new Integer(handle)) != null)
			connections.get(new Integer(handle)).listen();
		else {
			TCPUtils.returnNoConnectionError(TCPProtocol.ReturnType.LISTEN, handle, this);
		}
	}

	@Override
	public void connect(int handle, int dstIP, int dstPort) {
		if (connections.get(new Integer(handle)) == null) {
			connections.put(new Integer(handle), new TCB(handle, randomPort(), this));
		}
		connections.get(new Integer(handle)).connect(dstIP, dstPort);
	}

	@Override
	public void close(int handle) {
		if (connections.get(new Integer(handle)) != null) {
			connections.get(new Integer(handle)).close();
		}
		else {
			TCPUtils.returnNoConnectionError(TCPProtocol.ReturnType.CLOSE, handle, this);
		}
	}

	@Override
	public void accept(int handle) {
		
	}

	@Override
	public void abort(int handle) {
		if (connections.get(new Integer(handle)) != null) {
			connections.get(new Integer(handle)).abort();
		}
		else {
			TCPUtils.returnNoConnectionError(TCPProtocol.ReturnType.ABORT, handle, this);
		}		
	}

	@Override
	public void send(int handle, byte[] data) {
		if (connections.get(new Integer(handle)) != null) {
			connections.get(new Integer(handle)).send(data);
		}
		else 
			TCPUtils.returnNoConnectionError(TCPProtocol.ReturnType.SEND, handle, this);
	}

	@Override
	public void receive(int handle) {
		if (connections.get(new Integer(handle)) != null) {
			connections.get(new Integer(handle)).receive();
		}
		else {
			TCPUtils.returnNoConnectionError(TCPProtocol.ReturnType.RECEIVE, handle, this);
		}
	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {

		case IPProtocol.recPacketSignal:
			IPReceiveParam datagram = (IPReceiveParam)param;
			TCPSegment segment = TCPSegment.fromBytes(datagram.getData());
			//System.out.println("IP:" + datagram.getDestination() + " receive: " + segment.controlBits +" acknum: " + segment.ackNum +  " at " + Simulator.GetTime());
			for (TCB tcb: connections.values()) {
				if (tcb.localIP == datagram.getDestination() && tcb.localPort == segment.dstPort 
						&& tcb.remoteIP == datagram.getSource() && tcb.remotePort == segment.srcPort) {
					tcb.segArrive(segment, datagram.getSource());
					return;
				}
			}
			for (TCB tcb: connections.values()) {
				if (tcb.localIP == datagram.getDestination() && tcb.localPort == segment.dstPort) {
					tcb.segArrive(segment, datagram.getSource());
					return;
				}
			}
			break;
		case TIMER:
			Collection<TCB> x = connections.values();
			for (TCB tcb: x) {
				tcb.checkTime();
			}
			wait(TIMER, 1.0);
			break;
			
		}
	}
	
	void sendSegment(TCPSegment segment, int dstIP) {
		ipservice.sendPacket(segment.toBytes(), 
				new Integer(getIP()), 
				new Integer(dstIP), new Integer(this.getUniqueID()));
	}
	
	void returnResult(ReturnParam param) {
		for (IClient client: clients) {
			if (client instanceof InterruptObject) {
				((InterruptObject)client).delayInterrupt(TCPProtocol.INT_RETURN, param, returnDelay);
			}
		}
	}

	int randomPort() {
		return portCount++;
	}
	
	int getIP() {
		return ByteLib.bytesToInt(node.getAddress(ipservice), 0);
	}
	
	IPProtocol ipservice;
	INode node;
	
	int handleCounter = 1;
	int portCount = 50000;
	static final int TIMER = 0xFFFFDDDD;
	Map<Integer, TCB> connections = new HashMap<Integer, TCB>();
	
	double returnDelay = 0.001;
	LinkedList<IClient<Integer>> clients = new LinkedList<IClient<Integer>>();
}
