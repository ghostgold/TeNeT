package tenet.protocol.network.ipv4;

import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import tenet.core.Simulator;
import tenet.node.INode;
import tenet.node.router.DynamicRouter;
import tenet.node.router.Router;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.datalink.IDataLinkLayer.ReceiveParam;
import tenet.protocol.datalink.IDataLinkLayer.TransmitParam;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.datalink.sfdatalink.DatalinkWithARP;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.arp.ARP;
import tenet.util.ByteLib;
import tenet.util.pattern.serviceclient.IService;
import tenet.util.pattern.serviceclient.IClient;

public class InternetProtocol extends InterruptObject implements IPProtocol{

	
	public InternetProtocol(Integer ip, Integer mask) {
		this.address = ip;
		this.mask = mask; 
		this.binaryMask = ~((1 << (32 - mask)) - 1);

		transportLayers = new HashMap<Integer, IClient<Integer>>();
		outBuffer = new  LinkedList<IPDatagram>() ;
		arpBuffer = new LinkedList<IPDatagram>(); 
		frameBuffer = new LinkedList<FrameParamStruct>();
		timeOfBufferIdentifier = new HashMap<BufferIdentifier, Double>(); 
		inBuffer = new HashMap<BufferIdentifier, LinkedList<IPDatagram>>(); 
	}
	
	public InternetProtocol(String ip, Integer mask) {
		this(new Integer(intFromIPString(ip)), mask); 
	}
	
	private static int intFromIPString(String s) {
		int ans = 0;
		for (String x:s.split("\\.")) {
			ans <<= 8;
			ans += Integer.valueOf(x);
		}
		return ans;
	}
	
	public int getAddress() {
		return address;
	}
	
	public byte[] getBytesAddress() {
		return IPDatagram.intAddressToBytes(address);
	}
	@Override
	public void disable() {
		if (!power)
			return;
		power = false;
	}

	@Override
	public void enable() {
		if (power) 
			return;
		wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
		wait(TIMER, 10.0);
		power = true;
	}

	@Override
	public boolean isEnable() {
		return power;
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
		return "InetnetProtocol";
	}

	@Override
	public void attachTo(IService<?> service) {
		if (service instanceof IDataLinkLayer) { 
			datalink = (IDataLinkLayer)service;
//			if (datalink instanceof DatalinkWithARP) {
//				((DatalinkWithARP)datalink).getARP().registryClient(this); 
//			}
		}
		else if (service instanceof Router) {
			node = (Router)service;
		}
		else if (service instanceof ARP) {
			arp = (ARP)service;
		}
	}

	@Override
	public void detachFrom(IService<?> service) {
		if (service == datalink) {
			datalink = null;
		}
		else if (service == node) {
			node = null;
		}
		else if (service == arp) { 
			arp = null;
		}
		
	}

	@Override
	public Integer getUniqueID() {
		return protocolID;
	}

	@Override
	public void setUniqueID(Integer id) {
	}

	@Override
	public boolean canSend(Integer destIPAddr) {
		if (power && datalink.isLinkUp()) {
			return true;
		}
		waitingCanSend = true;
		return false;
	}

	@Override
	public void sendPacket(byte[] data, Integer srcIPAddr, Integer destIPAddr,
			Integer clientProtocolId) {
		IPDatagram datagram = new IPDatagram(data, address, destIPAddr, clientProtocolId, fragmentID++);
		node.delayInterrupt(IPProtocol.recPacketSignal, 
				new IPReceiveParam(IPReceiveParam.ReceiveType.OK, destIPAddr, srcIPAddr, protocolID, datagram.toBytes()),
				ipDelay);
	}

	public void sendPacket(byte[] data, Integer protocol) {
		IPDatagram datagram = new IPDatagram(data, address, broadcastIP, protocol, fragmentID++);
		forward(datagram.toBytes(), broadcastIP);
	}
	
	public void forward(byte[] data, Integer nextIP) {
		IPDatagram datagram = IPDatagram.fromBytes(data);
		int dst = datagram.getDst();
		if((dst &binaryMask) != (address & binaryMask)) 
			dst = nextIP.intValue();
		datagram.nextIP = dst;
		if (!canSend(IPProtocol.broadcastIP))
			return;
		outBuffer.add(datagram);
		if (noDatagramOnGoing) {
			processOutBuffer();
		}
	}
	
	public void processOutBuffer() {

		if (!frameBuffer.isEmpty()) {
			datalink.transmitFrame(frameBuffer.remove());
			noDatagramOnGoing = false;
		}
		else if (!outBuffer.isEmpty()) {
			
			IPDatagram datagram = outBuffer.remove();
			//little endian
			byte[] little = new byte[4];
			ByteLib.bytesFromInt(little, 0, datagram.nextIP);
			byte[] hardAddress ;
			if (datagram.getDst() == broadcastIP)
				hardAddress = MediumAddress.fromString("FF:FF:FF:FF:FF:FF").toBytes();
			else  {
				hardAddress = arp.getHardwareAddress(IPProtocol.protocolID, little);
				//arpBuffer.add(datagram);
				//wait(ARPRESULT, 0.2);
				if (hardAddress == null) 
					hardAddress = MediumAddress.fromString("FF:FF:FF:FF:FF:FF").toBytes();
//			byte[] hardAddress = arp.getHardwareAddress(IPProtocol.protocolID, IPDatagram.intAddressToBytes(datagram.nextIP));
			}
			if (hardAddress == null) {
			}
			else {
				int mtu = datalink.getMTU();
				if (datagram.toBytes().length <= mtu) {
					FrameParamStruct frame = new FrameParamStruct(MediumAddress.fromBytes(hardAddress), datalink.getUniqueID(), IPProtocol.protocolID, datagram.toBytes());
					wait(IDataLinkLayer.INT_FRAME_TRANSMIT, Double.NaN);
					datalink.transmitFrame(frame);
				}
				else {
					if (!datagram.canFragment()) {
						processOutBuffer();
						return;
					}
					int nfb = (mtu - datagram.getIHL() * 4) / 8;
					IPDatagram first = IPDatagram.fromBytes(datagram.toBytes());
					first.setData(Arrays.copyOfRange(datagram.getData(), 0, nfb * 8));
					first.setMoreFragment();
					FrameParamStruct frame = new FrameParamStruct(MediumAddress.fromBytes(hardAddress), datalink.getUniqueID(), IPProtocol.protocolID, first.toBytes());
					wait(IDataLinkLayer.INT_FRAME_TRANSMIT, Double.NaN);					
					datalink.transmitFrame(frame);
					IPDatagram second = IPDatagram.fromBytes(datagram.toBytes());
					second.setData(Arrays.copyOfRange(datagram.getData(), nfb * 8, datagram.getData().length));
					second.setOffset(datagram.fragmentOffset() + nfb);
					second.nextIP = datagram.nextIP;
					outBuffer.addFirst(second);
				}
				noDatagramOnGoing = false;
			}
		}
	}
	
	
	@Override
	public boolean canSend(int linkNumber) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void sendPacket(byte[] data, Integer srcIPAddr, int linkNumber,
			Integer clientProtocolId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int totalLink() {
		return node.totalLink();
	}

	@Override
	public void clearRouteTable() {
		node.clearRouteTable();
	}

	@Override
	public void clearDynamicRoutingTable() {
		node.clearDynamicRoutingTable();
	}

	@Override
	public void clearStaticRoutingTable() {
		node.clearStaticRoutingTable();
	}

	@Override
	public void addRoute(Integer destIPAddr, Integer mask, Integer linkNumber,
			Integer nextNodeIP, Integer metric) {
		node.addRoute(destIPAddr, mask, linkNumber, nextNodeIP, metric);
	}

	@Override
	public void addDefaultRoute(Integer linkNumber, Integer nextRouterIP) {
		node.addDefaultRoute(linkNumber, nextRouterIP);
	}

	@Override
	public Integer getLinkNumber(Integer ipaddr) {
		return node.getLinkNumber(ipaddr);
	}

	@Override
	public void deleteDefaultRoute() {
		node.deleteDefaultRoute();
	}

	@Override
	public void setMTU(int mtu) {
		//TODO
	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case IDataLinkLayer.INT_INTERFACE_UP:
			waitReceive();
			wait(IDataLinkLayer.INT_INTERFACE_DOWN, Double.NaN);
			
			resetInterrupt(IDataLinkLayer.INT_INTERFACE_UP);
			break;
		case IDataLinkLayer.INT_INTERFACE_DOWN:
			wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
			resetInterrupt(IDataLinkLayer.INT_INTERFACE_DOWN);
			break;
		case IDataLinkLayer.INT_FRAME_TRANSMIT_READY:
			noDatagramOnGoing = true;
			processOutBuffer();
			break;
		case IDataLinkLayer.INT_FRAME_TRANSMIT:
			TransmitParam transmitParam = (TransmitParam)param;
			noDatagramOnGoing = true;			
			switch (transmitParam.status) {
			case transmitOK:
				processOutBuffer();
				break;
			case dataLinkOff:
			case transmitCollision:
			case transmitError:
				frameBuffer.addFirst(transmitParam.frame);
				wait(IDataLinkLayer.INT_FRAME_TRANSMIT_READY, Double.NaN);
				break;
			}
			break;
		case ARPRESULT:
			while (!arpBuffer.isEmpty())
				outBuffer.addFirst(arpBuffer.remove());
			if (noDatagramOnGoing) 
				processOutBuffer();
			break;
			
		case TIMER:
			LinkedList<BufferIdentifier> toBeRemoved = new LinkedList<BufferIdentifier>();
			for (Entry<BufferIdentifier, Double> bi: timeOfBufferIdentifier.entrySet()) {
				if (bi.getValue() + timeLimit < Simulator.GetTime()) 
					toBeRemoved.add(bi.getKey());
			}
			for (BufferIdentifier bi: toBeRemoved) {
				timeOfBufferIdentifier.remove(bi);
				inBuffer.remove(bi);
			}
			wait(TIMER, 10.0);
			break;
		case IDataLinkLayer.INT_FRAME_RECEIVE:
			ReceiveParam receiveParam = (ReceiveParam)param;
			switch (receiveParam.status) {
			case receiveOK:
				IPDatagram  datagram = IPDatagram.fromBytes(receiveParam.frame.dataParam);
				//System.out.println("IP " + Router.ipToString(address) + " receive " + datagram.protocol);
				if (datagram.getDst() == address || datagram.getDst() == broadcastIP) {
					BufferIdentifier bi = new BufferIdentifier(datagram.getSrc(), datagram.getDst(), datagram.protocol, datagram.getIdentification());
					LinkedList<IPDatagram> buffer = inBuffer.get(bi);
					if (buffer == null) { 
						buffer = new LinkedList<IPDatagram>();
						inBuffer.put(bi, buffer);
					}
					buffer.add(datagram);					
					timeOfBufferIdentifier.put(bi, Simulator.GetTime());
					byte[] data = IPDatagram.reassemble(buffer);
					
					if (data != null) {
						
						if (datagram.protocol == DynamicRouter.RIPProtocol) {
							node.delayInterrupt(DynamicRouter.RIPSignal, 
									new IPReceiveParam(IPReceiveParam.ReceiveType.OK, this.address, datagram.getSrc() , DynamicRouter.RIPProtocol, data),
									ipDelay);
						}
						else if (transportLayers.get(new Integer(datagram.protocol)) != null){
							((InterruptObject)transportLayers.get(new Integer(datagram.protocol))).delayInterrupt(recPacketSignal,
								new IPReceiveParam(IPReceiveParam.ReceiveType.OK, datagram.getDst(), datagram.getSrc(), datagram.protocol, data), ipDelay);
						}
						inBuffer.remove(bi);
						timeOfBufferIdentifier.remove(bi);
					}
				}
				else {
					node.delayInterrupt(IPProtocol.recPacketSignal, 
							new IPReceiveParam(IPReceiveParam.ReceiveType.OK, datagram.getDst(), datagram.getSrc() , protocolID, datagram.toBytes()),
							ipDelay);
				}
				waitReceive();
				break;
			case receiveCollision:
				wait(IDataLinkLayer.INT_FRAME_RECEIVE_READY, Double.NaN);
				break;
			}
			break;
		case IDataLinkLayer.INT_FRAME_RECEIVE_READY:
			waitReceive();
		}
	}
	
	void notifyCanSend() {
		if (waitingCanSend) {
			for (IClient client: transportLayers.values()) {
				if (client instanceof InterruptObject) {
					((InterruptObject)client).delayInterrupt(IPProtocol.canSendSignal, null, ipDelay);
				}
			}
		}
	}
	void waitReceive() {
		datalink.receiveFrame(new FrameParamStruct(null, null, IPProtocol.protocolID, null));
		wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
	}
	
	@Override
	public void registryClient(IClient<Integer> client) {
		transportLayers.put(client.getUniqueID(), client);
		client.attachTo(this);
	}

	@Override
	public void unregistryClient(Integer id) {
		unregistryClient(transportLayers.get(id));
	}

	@Override
	public void unregistryClient(IClient<Integer> client) {
		if (client != null) {
			transportLayers.remove(client.getUniqueID());
			client.detachFrom(this);
		}
	}

	IDataLinkLayer datalink;
	Router node;
	ARP arp;
	boolean power = false;
	Map<Integer, IClient<Integer>> transportLayers;
	
	//private static final int bufferLimit = 10000;
	static final double ipDelay = 0.00001;
	static final double timeLimit = 20;
	
	private LinkedList<IPDatagram> outBuffer;
	private LinkedList<IPDatagram> arpBuffer;
	private LinkedList<FrameParamStruct> frameBuffer;
	private HashMap<BufferIdentifier, Double> timeOfBufferIdentifier;
	private HashMap<BufferIdentifier, LinkedList<IPDatagram>> inBuffer;
	private boolean waitingCanSend;
	
	private boolean noDatagramOnGoing = true;

	int address;
	int mask;
	int binaryMask;
	
	int fragmentID = 0;

	static final int ARPRESULT = 0xDEADBEEF;
	static final int TIMER = 0xDEEDBEEF;
	
	class BufferIdentifier {
		int src;
		int dst;
		int identifier;
		int protocol;
		public BufferIdentifier(int src, int dst, int protocol, int identifier) {
			this.src = src;
			this.dst = dst;
			this.protocol = protocol;
			this.identifier = identifier;
		}
		
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (o instanceof BufferIdentifier) {
				BufferIdentifier buid = (BufferIdentifier)o;
				return (buid.src == src && buid.dst == dst && buid.protocol == protocol && buid.identifier == identifier);
			}
			return false;
		}
		
		public int hashCode() {
			return src^dst^protocol^identifier;
		}

	}
}
