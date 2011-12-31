package tenet.protocol.network.ipv4;

import tenet.protocol.network.INetworkLayer;
import tenet.protocol.statecontrol.IStateSetable;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IRegistryableService;

public interface IPProtocol extends IStateSetable, INetworkLayer, IClient<Integer>, IRegistryableService<Integer>{

	/* 
	*	  Some constants: 
	*     @prtocolID: id of IP protocol to register in low level layer (e.g. Link Layer)
	*     @boardcastIP:  ...
	*     @timeToLive:  the max initial value of TTL in tenet, can bu used by source node 
	*                  to set TTL field
	*/
	public static final Integer protocolID = 0x0800;
	public static final Integer broadcastIP = 0xFFFFFFFF;       // 255.255.255.255
	public static final Integer maxTimeToLive = 50;                // max TTL

	
	/* 
	*      Interrupt signals which IP protcol sends to high level protocols(e.g. TCP)
	*      @canSendSignal:  IP allows high level protocol to transmit pakcets. 	                     
	*      @recPacketSignal: High level protocol receives this signal when there's new packet.
	*/
	public static final int canSendSignal = 0;
	public static final int recPacketSignal = 1;	

	
	/*
	*	   Interface for high level protocols(e.g. TCP) to send packets:
	*      @canSend:   High level protocols need to call 'canSend' every time
	*                  before calling sendPacket except when you are dealing with 
	*                  canSend signal. If 'canSend' return false from 
	*                  IP protocol, high level protocols should receive a 
	*				   'canSendSignal' later.
	*	   @sendPacket : ...
	*	   
	*/
	public boolean canSend(Integer destIPAddr);
	public void sendPacket(byte[] data, Integer srcIPAddr, Integer destIPAddr,
			Integer clientProtocolId);
			

	/*
	*      Almost the same as above functions , except that they assign 
	*       which link to send the packets , so dynamic routing can use these 
	*	   function.
	*/
	public boolean canSend(int linkNumber);
	public void sendPacket(byte[] data, Integer srcIPAddr, int linkNumber,
			Integer clientProtocolId);

	/* 	   For dynamic routing:
	*      return the total number of links attached to this node
	*/   
	public int totalLink();	
	
	// For dynamic routing or administrators to set the routing table directly
	public void clearRouteTable();
	public void clearDynamicRoutingTable();
	public void clearStaticRoutingTable();
	
	public void addRoute(Integer destIPAddr, Integer mask, Integer linkNumber,Integer nextNodeIP,Integer metric);
	public void addDefaultRoute(Integer linkNumber,Integer nextRouterIP);
	
	public Integer getLinkNumber(Integer ipaddr);
	
	public void deleteDefaultRoute();

	// set the mtu for fragmentation
	public void setMTU(int mtu);

}
