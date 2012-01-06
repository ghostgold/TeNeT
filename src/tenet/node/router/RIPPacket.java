package tenet.node.router;

import java.util.Collection;
import java.util.LinkedList;

import tenet.util.ByteLib;

public class RIPPacket {
	static RIPPacket fromBytes(byte[] data) {
		RIPPacket result = new RIPPacket();
		for (int i = 0; i < data.length; i+=16) {
			RouteEntry entry = new RouteEntry(ByteLib.bytesToInt(data, i), ByteLib.bytesToInt(data, i+4), -1, ByteLib.bytesToInt(data, i+12), ByteLib.bytesToInt(data, i+8));
			result.addEntry(entry);
		}
		return result;
	}
	
	public RIPPacket() { 
	}

	public void addEntry(RouteEntry entry) {
		table.add(entry);
	}
	
	public byte[] toBytes() {
		byte[] result = new byte[table.size() * 16];
		int i = 0; 
		for(RouteEntry r: table) {
			ByteLib.bytesFromInt(result, i, r.dest);
			ByteLib.bytesFromInt(result, i + 4, r.mask);
			ByteLib.bytesFromInt(result, i + 8, r.metric);
			ByteLib.bytesFromInt(result, i + 12, r.nextNodeIP);
			i += 16;
		}
		return result;
	}
	
	public Collection<RouteEntry> getRouteEntrys() {
		return table;
	}
	LinkedList<RouteEntry> table = new LinkedList<RouteEntry>();
}
