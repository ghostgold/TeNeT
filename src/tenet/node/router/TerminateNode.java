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

public class TerminateNode extends Router{
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
			if (result != null && (datagram.getSrc()) == slots.get(0).getAddress()) {
				slots.get(result.linkNumber).forward(datagram.toBytes(), result.nextNodeIP);
				//slots.get(result.linkNumber).forward(datagram.toBytes(), IPProtocol.broadcastIP);
				//System.out.println("To: " + ipToString(datagram.getDst()) + ", by " + ipToString(slots.get(result.linkNumber).getAddress()) + ", next "  + ipToString(result.nextNodeIP));
			}
			wait(IPProtocol.recPacketSignal, Double.NaN);
			break;
		}
	}
}
