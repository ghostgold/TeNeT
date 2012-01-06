package tenet.node.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import tenet.node.INode;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.ipv4.IPDatagram;
import tenet.protocol.network.ipv4.IPProtocol;
import tenet.protocol.network.ipv4.IPReceiveParam;
import tenet.protocol.network.ipv4.InternetProtocol;
import tenet.protocol.statecontrol.IStateSetable;
import tenet.util.ByteLib;
import tenet.util.pattern.serviceclient.IClient;

public class Router extends InterruptObject implements INode {



	public Router() {
		super();
		wait(IPProtocol.recPacketSignal, Double.NaN);
		defaultRoute = null;
		staticRoutingTable = new LinkedList<RouteEntry>();
		dynamicRoutingTable = new LinkedList<RouteEntry>();
		slots = new ArrayList<InternetProtocol>();
	}

	@Override
	public void registryClient(IClient client) {
		clientset.add(client);
		client.attachTo(this);
		if (client instanceof InternetProtocol) {
			//little endian in node
			int address = ((InternetProtocol) client).getAddress();
			slots.add((InternetProtocol)client);
			byte[] little = new byte[4];
			ByteLib.bytesFromInt(little, 0, address);
			addrmap.put(client, little);

//			slots.add((InternetProtocol)client);
//			addrmap.put(client, ((InternetProtocol) client).getBytesAddress());
		}
	}

	@Override
	public void unregistryClient(Object id) {
	}

	@Override
	public void unregistryClient(IClient<Object> client) {
		clientset.remove(client);
		client.detachFrom(this);
	}

	@Override
	public void disable() {
		if(!power) return;
		for (IClient<?> client:clientset)
			if (client instanceof IStateSetable)
				((IStateSetable)client).disable();
		power = false;
	}

	@Override
	public void enable() {
		if (power) return;
		for (IClient<?> client:clientset)
			if (client instanceof IStateSetable)
				((IStateSetable)client).enable();
		power = true;
	}

	@Override
	public boolean isEnable() {
		return power;
	}

	@Override
	public void dump() {
		// TODO Auto-generated method stub
	}

	@Override
	public void setAddress(IClient<?> protocol, byte[] address) {
		addrmap.put(protocol, address);
	}

	@Override
	public byte[] getAddress(IClient<?> protocol) {
		return addrmap.get(protocol);
	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case IPProtocol.recPacketSignal:
			IPReceiveParam ipparam = (IPReceiveParam)param;
			if (ipparam.getState() == IPReceiveParam.ReceiveType.Error)
				break;
			IPDatagram datagram = IPDatagram.fromBytes(ipparam.getData());
			datagram.decreaseTTL();
			RouteEntry result = null; 
			for (RouteEntry route: staticRoutingTable) {
				if (route.match(datagram.getDst())) {
					if (result == null || result.metric > route.metric) 
						result = route;
				}
			}
			for (RouteEntry route: dynamicRoutingTable) {
				if (route.match(datagram.getDst())) {
					if (result == null || result.metric > route.metric) 
						result = route;
				}
			}
			if (result == null) {
				result = defaultRoute;
			}
			if (result != null && ((slots.size() > 1) || datagram.getSrc() == slots.get(0).getAddress())) {
				slots.get(result.linkNumber).forward(datagram.toBytes(), result.nextNodeIP);
				//slots.get(result.linkNumber).forward(datagram.toBytes(), IPProtocol.broadcastIP);
				//System.out.println("To: " + ipToString(datagram.getDst()) + ", by " + ipToString(slots.get(result.linkNumber).getAddress()) + ", next "  + ipToString(result.nextNodeIP));
			}
			
			wait(IPProtocol.recPacketSignal, Double.NaN);
			break;
		}
		
	}
	
	public void addRoute(Integer destIPAddr, Integer mask, Integer linkNumber, Integer nextNodeIP, Integer metric) {
		staticRoutingTable.add(new RouteEntry(destIPAddr, mask, linkNumber, nextNodeIP, metric));
	}
	
	public void addDefaultRoute(Integer linkNumber, Integer nextNodeIP) {
		defaultRoute = new RouteEntry(0, 32, linkNumber, nextNodeIP, 0);
	}
	
	public void deleteDefaultRoute() {
		defaultRoute = null;
	}
	
	public Integer getLinkNumber(Integer ipaddr) {
		for (int i = 0; i < slots.size(); i++) {
			if (slots.get(i).getAddress() == ipaddr)
				return i;
		}
		return -1;
	}
	
	public int totalLink() {
		return slots.size();
	}
	
	public void clearRouteTable() {
		staticRoutingTable.clear();
	}
	
	public void clearDynamicRoutingTable() {
		dynamicRoutingTable.clear();
	}
	
	public void clearStaticRoutingTable() {
		staticRoutingTable.clear();
	}
	
	public static String ipToString(int x) {
		return ((x >> 24) & 0xff) + "." +  ((x >> 16) & 0xff)+ "." +  ((x >> 8) & 0xff) + "." +  ((x >> 0) & 0xff); 
	}
	private boolean power;
	HashMap<IClient<?>,byte[]> addrmap = new HashMap<IClient<?>,byte[]>();
	HashSet<IClient<?>> clientset = new HashSet<IClient<?>>();
	ArrayList<InternetProtocol> slots;
	
	LinkedList<RouteEntry> staticRoutingTable;
	LinkedList<RouteEntry> dynamicRoutingTable;
	protected RouteEntry defaultRoute;

	


}

class RouteEntry {
	int dest;
	int mask;
	int binaryMask;
	int linkNumber;
	int nextNodeIP;
	int metric;

	public RouteEntry(Integer destIPAddr, Integer mask, Integer linkNumber, Integer nextNodeIP, Integer metric) {
		this.dest = destIPAddr;
		this.mask = mask;
		this.binaryMask = ~((1 << (32 - mask)) - 1);
		if (mask == 0)
			this.binaryMask = 0;
		this.linkNumber = linkNumber;
		this.nextNodeIP = nextNodeIP;
		this.metric = metric;
	}
	
	public boolean match(Integer address) {
		if ((dest & binaryMask) == (address.intValue() & binaryMask)) 
			return true;
		return false;
	}
	

	
}
