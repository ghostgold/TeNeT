//XXX author Ruixin Qiang
package tenet.node.l2switch;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

import tenet.core.Simulator;
import tenet.node.INode;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.datalink.IDataLinkLayer.ReceiveParam;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.datalink.SEther.SimpleEthernetDatalink;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.util.pattern.serviceclient.IClient;

public class L2Switch extends InterruptObject implements INode {

	public class MACWithTimestamp implements Comparable<MACWithTimestamp> {

		public Double m_time;
		public MediumAddress m_mac;

		public MACWithTimestamp(Double m_time, MediumAddress m_mac) {
			super();
			this.m_time = m_time;
			this.m_mac = m_mac;
		}

		@Override
		public int compareTo(MACWithTimestamp arg0) {
			return m_time.compareTo(arg0.m_time);
		}

	}

	protected final static int TIMER = 0x00000001;

	/**
	 * 存储二层交换机上的所有SimpleEthernetDatalink的信息
	 */
	public HashMap<MediumAddress, SimpleEthernetDatalink> m_datalinks;

	public final static int expireTime = 10;
	/**
	 * 存储转发目的地所对应的出口,即MAC表
	 */
	public HashMap<MediumAddress, SimpleEthernetDatalink> m_macport_map;
	
	public PriorityQueue<MACWithTimestamp> m_timeout;
	protected boolean m_state = false;

	public L2Switch() {
		super();
		wait(TIMER, 1.0); // 通过超时等待，每秒触发一次计时器
		wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
		m_datalinks = new HashMap<MediumAddress, SimpleEthernetDatalink>();
		m_macport_map = new HashMap<MediumAddress, SimpleEthernetDatalink>();
		m_timeout = new PriorityQueue<MACWithTimestamp>();
	}

	@Override
	public void disable() {
		if (!m_state)
			return;
		for (IDataLinkLayer iface : m_datalinks.values())
			iface.disable();
		m_state = false;
	}

	@Override
	public void dump() {

	}

	@Override
	public void enable() {
		if (m_state)
			return;
		for (IDataLinkLayer iface : m_datalinks.values())
			iface.enable();
		m_state = true;
	}

	/**
	 * 获得mac地址所对应的二层交换机上的SimpleEthernetDatalink
	 */
	public SimpleEthernetDatalink getDatalink(MediumAddress mac) {
		return m_datalinks.get(mac);
	}

	/**
	 * 获得二层交换机上的所有SimpleEthernetDatalink 
	 */
	public Collection<SimpleEthernetDatalink> getDatalinks() {
		return m_datalinks.values();
	}

	/**
	 * 从mac表中获得mac地址所对应的端口上的SimpleEthernetDatalink
	 */
	public SimpleEthernetDatalink getTransmitDatalink(MediumAddress mac) {
		return m_macport_map.get(mac);
	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case TIMER:
			this.resetInterrupt(TIMER);
			wait(TIMER, 1.0);
			double now = Simulator.getInstance().getTime();
			while (m_timeout.peek() != null && m_timeout.peek().m_time + expireTime < now) {
				m_macport_map.remove(m_timeout.remove().m_mac);
			}
			break;
		case IDataLinkLayer.INT_FRAME_RECEIVE:
			ReceiveParam wrappedFrame = (ReceiveParam)param;
			learnMAC(wrappedFrame.frame.sourceParam, (SimpleEthernetDatalink)wrappedFrame.datalink);
			if (//wrappedFrame.frame.destinationParam.equals(new MediumAddress("FF:FF:FF:FF:FF:FF")) ||
					getTransmitDatalink(wrappedFrame.frame.destinationParam) == null) {
				for (SimpleEthernetDatalink slot: getDatalinks()) {
					if (slot!= wrappedFrame.datalink) {
						slot.transmitFrame(wrappedFrame.frame);
					}
				}
			}
			else {
				getTransmitDatalink(wrappedFrame.frame.destinationParam).transmitFrame(wrappedFrame.frame);
			}
			wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
		}
	}

	@Override
	public boolean isEnable() {
		return m_state;
	}

	public void learnMAC(MediumAddress mac, SimpleEthernetDatalink iface) {
	    // TODO 更新MAC表，将MAC地址与所对应的SimpleEthernetDatalink放入表中
		m_timeout.add(new MACWithTimestamp(Simulator.getInstance().getTime(), mac));
		m_macport_map.put(mac, iface);
	}

	@Override
	public void registryClient(IClient client) {
		if (client instanceof SimpleEthernetDatalink) { 
			SimpleEthernetDatalink slot = (SimpleEthernetDatalink)client;
			this.m_datalinks.put(slot.getUniqueID(), slot);
			slot.attachTo(this);
		}
	}

	@Override
	public void unregistryClient(IClient client) {
		if (m_datalinks.get(client.getUniqueID()) != null) {
			m_datalinks.remove(client.getUniqueID());
			LinkedList<MediumAddress> remove = new LinkedList<MediumAddress>();
			for (Map.Entry<MediumAddress, SimpleEthernetDatalink> macSlot: m_macport_map.entrySet()) {
				if (client.equals(macSlot.getValue())) 
					remove.add(macSlot.getKey());
			}
			for (MediumAddress mac:remove) {
				m_macport_map.remove(mac);
			}
		}
	}

	@Override
	public void unregistryClient(Object id) {
		unregistryClient(m_datalinks.get(id));
	}


	@Override
	public void setAddress(IClient<?> protocol, byte[] address) {
		// TODO Auto-generated method stub
	}

	@Override
	public byte[] getAddress(IClient<?> protocol) {
		// TODO Auto-generated method stub
		return null;
	}
}
