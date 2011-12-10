package tenet.protocol.network.ipv4;

import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.util.pattern.serviceclient.IService;

public class InternetProtocol extends InterruptObject implements IPProtocol{

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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void dump() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getIdentify() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void attachTo(IService<?> service) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detachFrom(IService<?> service) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Integer getUniqueID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUniqueID(Integer id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canSend(Integer destIPAddr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void sendPacket(byte[] data, Integer srcIPAddr, Integer destIPAddr,
			Integer clientProtocolId) {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void clearRouteTable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearDynamicRoutingTable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearStaticRoutingTable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addRoute(Integer destIPAddr, Integer mask, Integer linkNumber,
			Integer nextNodeIP, Integer metric) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addDefaultRoute(Integer linkNumber, Integer nextRouterIP) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Integer getLinkNumber(Integer ipaddr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteDefaultRoute() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMTU(int mtu) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		// TODO Auto-generated method stub
		
	}

}
