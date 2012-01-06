package tenet.protocol.datalink.sfdatalink;

import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.datalink.SEther.SimpleEthernetDatalink;
import tenet.protocol.network.arp.ARP;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IService;

public class DatalinkWithARP extends SimpleEthernetDatalink {

	public DatalinkWithARP(MediumAddress m_mac) {
		super(m_mac);
		arp = new ARP();
		this.registryClient(arp);
	}
	
	ARP arp;
	public ARP getARP() {
		return arp;
	}
	
	public void registryClient(IClient client) {
		super.registryClient(client);
		arp.registryClient(client);
	}
	
	public void attachTo(IService service) {
		super.attachTo(service);
		arp.attachTo(service);
	}
}
