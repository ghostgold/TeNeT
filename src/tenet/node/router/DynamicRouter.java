package tenet.node.router;

import java.util.Collection;
import java.util.LinkedList;

import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.ipv4.IPDatagram;
import tenet.protocol.network.ipv4.IPProtocol;
import tenet.protocol.network.ipv4.IPReceiveParam;
import tenet.protocol.network.ipv4.InternetProtocol;
import tenet.protocol.statecontrol.IStateSetable;
import tenet.util.pattern.serviceclient.IClient;

public class DynamicRouter extends Router  {

	public DynamicRouter() {
		super();
		wait(RIPSignal, Double.NaN);
		wait(RIPTIMERSignal, RIPTIMESeg);
	}
	public static final int RIPSignal = 0xFFDAEB;
	public static final int RIPTIMERSignal = 0xACEBEF;	
	public static final int RIPProtocol = 0x67;
	public static final double RIPTIMESeg = 20.0;
	
	
	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case RIPSignal:
			IPReceiveParam ipparam = (IPReceiveParam)param;
			if (ipparam.getState() == IPReceiveParam.ReceiveType.Error)
				break;
			int src = ipparam.getSource();
			RIPPacket rippacket = RIPPacket.fromBytes(ipparam.getData());
			checkRoutingTable(rippacket.getRouteEntrys(), src);
			for (RouteEntry r: rippacket.getRouteEntrys()) {
				updateDynamicRoutingTable(r, src, getLinkNumber(ipparam.getDestination()));
			}
			break;
		case RIPTIMERSignal:
			RIPPacket routeInfo = new RIPPacket();
			LinkedList<RouteEntry> toBeRemove= new LinkedList<RouteEntry>();
			for (RouteEntry r: dynamicRoutingTable) {

				if (!slots.get(r.linkNumber).canSend(IPProtocol.broadcastIP)) {
					toBeRemove.add(r);
				}
			}
			for (RouteEntry r: toBeRemove) {
				dynamicRoutingTable.remove(r);
			}
			for (RouteEntry r: staticRoutingTable) {
				routeInfo.addEntry(r);
			}
			for (RouteEntry r: dynamicRoutingTable) {
				routeInfo.addEntry(r);
			}
			byte[] data = routeInfo.toBytes();
			if (data.length > 0) {
				for (IPProtocol ip: slots) {
					if (ip instanceof InternetProtocol) {
						((InternetProtocol)ip).sendPacket(data, RIPProtocol);
					}
				}
			}
			wait(RIPTIMERSignal, RIPTIMESeg);
			break;
		default:
			super.interruptHandle(signal, param);
		}
	}
	
	void updateDynamicRoutingTable(RouteEntry newroute, int nexthop, int linknum) {
		boolean get = false;
		for (RouteEntry r: dynamicRoutingTable) {
			if (r.dest == newroute.dest && r.mask == newroute.mask) {
				get = true;
				if (r.metric > newroute.metric + 1 && newroute.nextNodeIP != slots.get(linknum).getAddress()) {
					r.metric = newroute.metric + 1;
					r.linkNumber = linknum;
					r.nextNodeIP = nexthop;
				}
			}
		}
		for (RouteEntry r: staticRoutingTable) {
			if (r.dest == newroute.dest && r.mask == newroute.mask) {
				get = true;
			}
		}
		if (!get && newroute.nextNodeIP != slots.get(linknum).getAddress()) {
			dynamicRoutingTable.add(new RouteEntry(newroute.dest, newroute.mask, linknum, nexthop, newroute.metric + 1));
		}
	}
	
	void checkRoutingTable(Collection<RouteEntry> neighboorTable, int nexthop) {
		LinkedList<RouteEntry> toBeRemove= new LinkedList<RouteEntry>();
		for (RouteEntry r: dynamicRoutingTable) {
			if (r.nextNodeIP == nexthop) {
				boolean find = false;
				for (RouteEntry nr: neighboorTable) {
					if (nr.dest == r.dest && nr.mask == r.mask && nr.metric + 1 == r.metric) {
						find = true;
					}
				}
				if (!find)
					toBeRemove.add(r);
			}
		}
		for (RouteEntry r: toBeRemove) {
			dynamicRoutingTable.remove(r);
		}
	}
}
