//XXX author Ruixin Qiang 
package tenet.protocol.network.arp;


import tenet.core.Simulator;

import tenet.node.INode;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.INetworkLayer;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.datalink.MediumAddress;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IRegistryableService;
import tenet.util.pattern.serviceclient.IService;
import tenet.util.ByteLib;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//XXX The MAC conflict should throw the 0x80000500 to the upper layer.XXX
class ProtocolPair {
	int ptype;
	byte[] protocolAddress;
	public ProtocolPair(int ptype, byte[] protocolAddress) {
		this.ptype = ptype;
		this.protocolAddress = protocolAddress.clone();
	}
	public boolean equals(Object m) {
		if(m == null)
			return false;
		if(!(m instanceof ProtocolPair))
			return false;
		ProtocolPair p = (ProtocolPair)m;
		if (p.ptype == this.ptype && Arrays.equals(p.protocolAddress, this.protocolAddress))
			return true;
		return false;
	}
	public int hashCode() {
		int result = 0;
		for (int i = 0; i < protocolAddress.length; i++) {
			result += protocolAddress[i];
			result <<= 8;
		}
		return result;
	}
}

class HardwarePair {
	MediumAddress mac;
	double time;
	public HardwarePair(MediumAddress mac, double time) {
		this.mac = mac;
		this.time = time;
	}
}
public class ARP extends InterruptObject implements INetworkLayer,
		IRegistryableService<Integer> {

	public final static int htypeDefault = 1;
	public final static int hlnDefault = 48;
	public final static int plnDefault = 32;
	public final static int REQUEST = 1;
	public final static int REPLY = 2;
	public final static int macConflict = 0xE0000500;
	
	/**
	 * protocol layer below ARP
	*/
	protected IDataLinkLayer datalink;
	protected INode node;
	protected Map<ProtocolPair, HardwarePair> arpTable;
	protected Map<Integer, IClient<Integer>> networkLayers; 

	/**
	 * protocol id for ARP
	 */
	static Integer arpId = 0x806;
	static double timeToLive = 10000;
	static double arpDelay = 0.001;
	
	public ARP() {
		arpTable = new HashMap<ProtocolPair, HardwarePair>();
		networkLayers = new HashMap<Integer, IClient<Integer>>();
		this.wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
	}
	

	
	public void send(int HTYPE, byte[] hardwareAddr, int PTYPE, byte[] protocolAddr) {
		//The basic function that you can send a arp package to the others.
		//SEther's HTYPE is 1, IPv4's PTYPE is 0x0800 as default
		//Some different PTYPE will appear in the verification
	}


	public byte[] getHardwareAddress(int PTYPE, byte[] protocolAddr) {
		//The mapping you have maintained will be used by some other protocol.
		//The higher layer will use it to get the mac.
		IClient<Integer> client = networkLayers.get(new Integer(PTYPE));
		if (client != null) {
			if (Arrays.equals(node.getAddress(client), protocolAddr)) {
				return datalink.getUniqueID().toBytes();
			}
		}
		HardwarePair ans = arpTable.get(new ProtocolPair(PTYPE, protocolAddr));
		if (ans != null && ans.time + timeToLive > Simulator.getInstance().getTime()) {
			return ans.mac.toBytes();
		}
		else {
			if (ans != null) 
				arpTable.remove(new ProtocolPair(PTYPE, protocolAddr));

			int hln = datalink.getUniqueID().toBytes().length;
			int pln = node.getAddress(networkLayers.get(PTYPE)).length;
			ArpFrame request = new ArpFrame(htypeDefault, PTYPE, hln, pln,
					REQUEST, datalink.getUniqueID().toBytes(),
					node.getAddress(networkLayers.get(PTYPE)), new byte[hln], protocolAddr);
			FrameParamStruct packet = new FrameParamStruct(new MediumAddress("FF:FF:FF:FF:FF:FF"), datalink.getUniqueID(), 
					getUniqueID(), request.toBytes());
			datalink.transmitFrame(packet);
			return null;
		}
	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		
		// This is the most important function in ARP.
		// Ths signals may be caused by the IDataLinkLayer and yourself.
		// IDataLinkLayer.INT_FRAME_RECEIVE & IDataLinkLayer.INT_FRAME_TRANSMIT are the important signals. 
		
		switch (signal) {
		case IDataLinkLayer.INT_FRAME_RECEIVE:
			IDataLinkLayer.ReceiveParam packet = (IDataLinkLayer.ReceiveParam)param;
			
			if (packet.status == IDataLinkLayer.ReceiveStatus.receiveOK) {
				ArpFrame arpframe = ArpFrame.fromBytes(packet.frame.dataParam);

				//?Do I have the hardware type in ar$hrd?
				if (arpframe.htype == ARP.htypeDefault) {
					IClient<Integer> client = networkLayers.get(new Integer(arpframe.ptype));
					//Detect MAC address conflict
					if ((client == null || !Arrays.equals(node.getAddress(client), arpframe.getSPA())) 
							&& Arrays.equals(datalink.getUniqueID().toBytes(), arpframe.sha)) {
						for (IClient<Integer> c: networkLayers.values()) {
							if (c instanceof InterruptObject) {
								((InterruptObject)c).delayInterrupt(macConflict, datalink.getUniqueID(), arpDelay);
							}
						}	
					}				
					//?Do I speak the protocol in ar$pro?
					if(client != null) {
						boolean mergeFlag = false;
						ProtocolPair key = new ProtocolPair(arpframe.ptype, arpframe.getSPA());
					

						
						if (arpTable.get(key) != null) {
							arpTable.put(key, new HardwarePair(MediumAddress.fromBytes(arpframe.sha), Simulator.getInstance().getTime()));
							mergeFlag = true;
						}
						
						
						
						//?Am I the target protocol address?
						if (Arrays.equals(node.getAddress(client), arpframe.getTPA())) {
							if (!mergeFlag) 
								arpTable.put(key, new HardwarePair(MediumAddress.fromBytes(arpframe.sha), Simulator.getInstance().getTime()));
						
							if (arpframe.op == ARP.REQUEST) {
								arpframe.swap();
								arpframe.spa = node.getAddress(client);
								arpframe.sha = datalink.getUniqueID().toBytes();
								arpframe.op = ARP.REPLY;
								FrameParamStruct reply = new FrameParamStruct(MediumAddress.fromBytes(arpframe.tha), datalink.getUniqueID(), this.getUniqueID(), arpframe.toBytes());
								datalink.transmitFrame(reply);
							}
						}
					}
				}

			}
			//datalink.receiveFrame(new FrameParamStruct(null, null, this.getUniqueID(), null));
			waitReceive();
			break;
		case IDataLinkLayer.INT_INTERFACE_UP:
			//datalink.receiveFrame(new FrameParamStruct(null, null, this.getUniqueID(), null));
			waitReceive();
			break;
		case IDataLinkLayer.INT_INTERFACE_DOWN:
			this.resetInterrupt(IDataLinkLayer.INT_FRAME_RECEIVE);
			this.wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
			break;
		}
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
		return null;		
	}

	@Override
	public void registryClient(IClient<Integer> client) {
		//A higher layer client may transfer it.
		//AssertLib.AssertTrue(client instanceof INetworkLayer, "client of ARP should be NetworkLayer");
		networkLayers.put(client.getUniqueID(), client);
		client.attachTo(this);
	}

	@Override
	public void unregistryClient(IClient<Integer> client) {
		if (networkLayers.get(client.getUniqueID()) == client)
			networkLayers.remove(client.getUniqueID());
		client.detachFrom(this);
	}

	@Override
	public void unregistryClient(Integer id) {
		unregistryClient(networkLayers.get(id));
	}

	void waitReceive() {
		datalink.receiveFrame(new FrameParamStruct(null, null, this.getUniqueID(), null));
		wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
	}
	@Override
	public void attachTo(IService service) {
		if (service instanceof IDataLinkLayer) {
			datalink = (IDataLinkLayer)service;
			//waitReceive();
		}
		if (service instanceof INode) {
			node = (INode) service;
		}
	}

	@Override
	public void detachFrom(IService service) {
		if (datalink != null && datalink.equals(service)) {//XXX or instanceof IDatalinklayer
			datalink = null;
		}
		
		if (node != null && node.equals(service)) {
			node = null;
		}
	}

	@Override
	public Integer getUniqueID() {
		return 	arpId;	
	}

	@Override
	public void setUniqueID(Integer id) {
		arpId = id;
	}


}

class ArpFrame {
	// the ARP frame data struct.
	int htype;
	int ptype;
	int hln;
	int pln;
	int op;
	byte[] sha;
	byte[] spa;
	byte[] tha;
	byte[] tpa;
	public ArpFrame(int htype, int ptype, int hln, int pln, int op, byte[] sha, byte[] spa, byte[] tha, byte[] tpa){
		this.htype = htype;
		this.ptype = ptype;
		this.hln = hln;
		this.pln= pln;
		this.op = op;
		this.sha = sha.clone();
		this.spa = spa.clone();
		this.tha = tha.clone();
		this.tpa = tpa.clone();
	}
	
	public byte[] getSPA() {
		byte[] result = new byte[pln];
		System.arraycopy(spa, 0, result, 0, result.length);
		return result;
	}


	public byte[] getTPA() {
		byte[] result = new byte[pln];
		System.arraycopy(tpa, 0, result, 0, result.length);
		return result;
	}
	public void swap() {
		byte[] temp;
		temp = sha;
		sha = tha;
		tha = temp;
		temp = spa;
		spa = tpa;
		tpa = temp;
	}
	
	public byte[] toBytes() {
		byte[] packet = new byte[28];
		packet[0] = ByteLib.byteFromUnsigned(htype, 8);
		packet[1] = ByteLib.byteFromUnsigned(htype, 0);
		packet[2] = ByteLib.byteFromUnsigned(ptype, 8);
		packet[3] = ByteLib.byteFromUnsigned(ptype, 0);
		packet[4] = ByteLib.byteFromUnsigned(hln, 0);
		packet[5] = ByteLib.byteFromUnsigned(pln, 0);
		packet[6] = ByteLib.byteFromUnsigned(op, 8);
		packet[7] = ByteLib.byteFromUnsigned(op, 0);
		System.arraycopy(sha, 0, packet, 8, sha.length);
		System.arraycopy(spa, 0, packet, 14, spa.length);
		System.arraycopy(tha, 0, packet, 18, tha.length);
		System.arraycopy(tpa, 0, packet, 24, tpa.length);
		return packet;
	}
	
		
	public static ArpFrame fromBytes(byte[] packet) {
		return new ArpFrame(ByteLib.byteToUnsigned(packet[0], 8) + ByteLib.byteToUnsigned(packet[1], 0),
							ByteLib.byteToUnsigned(packet[2], 8) + ByteLib.byteToUnsigned(packet[3], 0),
							ByteLib.byteToUnsigned(packet[4], 0), ByteLib.byteToUnsigned(packet[5], 0),
							ByteLib.byteToUnsigned(packet[6], 8) + ByteLib.byteToUnsigned(packet[7], 0),
							Arrays.copyOfRange(packet, 8, 14), Arrays.copyOfRange(packet, 14, 18),
							Arrays.copyOfRange(packet, 18, 24), Arrays.copyOfRange(packet, 24, 28));
	}
}
