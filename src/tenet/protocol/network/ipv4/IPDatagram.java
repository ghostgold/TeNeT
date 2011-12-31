package tenet.protocol.network.ipv4;

import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.BitSet;
import tenet.util.ByteLib;

public class IPDatagram {
	public static int defaultVersion = 4;
	public static int defaultIHL = 5;
	public static byte defaultFirstByte = 69;
	
	byte versionWithIHL;
	
	byte typeOfService;
	
	int totalLength;
	
	int identification;
	int flagsWithOffset;
	
	byte timeToLive;
	byte protocol;
	int headerChecksum;
	
	byte[] srcAddress;
	byte[] dstAddress;
	
	byte[] option;
	
	byte[] data;
	
	int nextIP;

	public IPDatagram() {
		
	}
	
	public IPDatagram(byte[] data, int src, int dst, int protocolType, int fragmentID) {
		this.data = data;
		this.srcAddress = intAddressToBytes(src);
		this.dstAddress = intAddressToBytes(dst);
		this.protocol = ByteLib.byteFromUnsigned(protocolType, 0);
		
		this.versionWithIHL = defaultFirstByte;
		this.typeOfService = 0;
		this.totalLength = defaultIHL * 4 + data.length;
		this.identification = fragmentID & 0xFFFF;
		this.flagsWithOffset = 0;
		
		this.timeToLive = ByteLib.byteFromUnsigned(IPProtocol.maxTimeToLive, 0);
		this.option = null;
		this.headerChecksum = calcHeaderChecksum();
	}
	
	public static IPDatagram fromBytes(byte[] datagram) {
		IPDatagram result = new IPDatagram();
		result.versionWithIHL = datagram[0];
		result.typeOfService = datagram[1];
		result.totalLength = ByteLib.byteToUnsigned(datagram[2], 8) + ByteLib.byteToUnsigned(datagram[3], 0);
		result.identification = ByteLib.byteToUnsigned(datagram[4], 8) + ByteLib.byteToUnsigned(datagram[5], 0);
		result.flagsWithOffset = ByteLib.byteToUnsigned(datagram[6], 8) + ByteLib.byteToUnsigned(datagram[7], 0);
		result.timeToLive = datagram[8];
		result.protocol = datagram[9];
		result.headerChecksum = ByteLib.byteToUnsigned(datagram[10], 8) + ByteLib.byteToUnsigned(datagram[11], 0);
		result.srcAddress = Arrays.copyOfRange(datagram, 12, 16);
		result.dstAddress = Arrays.copyOfRange(datagram, 16, 20);
		result.option = Arrays.copyOfRange(datagram, 20, result.getIHL() * 4);
		result.data = Arrays.copyOfRange(datagram, result.getIHL() * 4 , datagram.length);
		return result;
	}
	
	public byte[] toBytes() {
		byte[] datagram;
		datagram = new byte[totalLength];
		datagram[0] = versionWithIHL;
		datagram[1] = typeOfService;
		datagram[2] = ByteLib.byteFromUnsigned(totalLength, 8);
		datagram[3] = ByteLib.byteFromUnsigned(totalLength, 0);
		datagram[4] = ByteLib.byteFromUnsigned(identification, 8);
		datagram[5] = ByteLib.byteFromUnsigned(identification, 0);
		datagram[6] = ByteLib.byteFromUnsigned(flagsWithOffset, 8);
		datagram[7] = ByteLib.byteFromUnsigned(flagsWithOffset, 0);
		datagram[8] = timeToLive;
		datagram[9] = protocol;
		datagram[10] = ByteLib.byteFromUnsigned(headerChecksum, 8);
		datagram[11] = ByteLib.byteFromUnsigned(headerChecksum, 0);
		System.arraycopy(srcAddress, 0, datagram, 12, 4);
		System.arraycopy(dstAddress, 0, datagram, 16, 4);
		if (option != null && option.length != 0)
			System.arraycopy(option, 0, datagram, 20, option.length);
		System.arraycopy(data, 0, datagram, this.getIHL() * 4, data.length);
		return datagram;
	}
	
	public int getIHL() {
		return versionWithIHL & 0xF;
	}
	
	public void decreaseTTL() {
		timeToLive--;
		headerChecksum = calcHeaderChecksum();
	}
	
	private int calcHeaderChecksum() {
		//TODO
		return 0;
	}
	
	public boolean check() {
		//TODO
		return true;
	}
	
	public boolean canFragment() {
		return (((flagsWithOffset >> 14) & 1) == 0); 
	}
	
	public boolean moreFragment() {
		return (((flagsWithOffset >> 13) & 1) == 1); 
	}
	
	public void setMoreFragment() {
		flagsWithOffset |= (1 << 13); 
		headerChecksum = calcHeaderChecksum(); 			
	}
	
	public int fragmentOffset() {
		return flagsWithOffset & 0x1FFF;
	}
	
	public int getIdentification() {
		return identification;
	}
	
	public int getSrc() {
		return bytesAddressToInt(srcAddress);
	}
	
	public int getDst() {
		return bytesAddressToInt(dstAddress);
	}
	
	public byte[] getBytesSrc() {
		return srcAddress;
	}
	
	public byte[] getBytesDst() {
		return dstAddress;
	}
	
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] newData) {
		data = newData;
		totalLength = data.length + getIHL() * 4;
		headerChecksum = calcHeaderChecksum(); 
	}
	
	public void setOffset(int offset) {
		flagsWithOffset -= fragmentOffset();
		flagsWithOffset += offset;
		headerChecksum = calcHeaderChecksum(); 		
	}
	
	public static int bytesAddressToInt(byte[] address) {
		int result = 0;
		result |= address[0] << 24;
		result |= address[1] << 16;
		result |= address[2] << 8;
		result |= address[3];
		return result;
		
	}
	
	public static byte[] intAddressToBytes(int address) {
		byte[] result = new byte[4];
		result[0] = ByteLib.byteFromUnsigned(address, 24);
		result[1] = ByteLib.byteFromUnsigned(address, 16);
		result[2] = ByteLib.byteFromUnsigned(address, 8);
		result[3] = ByteLib.byteFromUnsigned(address, 0);
		return result;
	}
	
	
	public static byte[] reassemble(List<IPDatagram> list) {
		BitSet testBit = new BitSet();
		int size = -1;
		for (IPDatagram datagram: list) {
			if (!datagram.moreFragment()) 
				size = datagram.fragmentOffset() * 8 +  datagram.totalLength - datagram.getIHL() * 4;
			for (int i = datagram.fragmentOffset(); i <= datagram.fragmentOffset() + ((datagram.totalLength - datagram.getIHL() * 4) + 7) / 8; i++)
				testBit.set(i);
		}
		if (size < 0)
			return null;
		for (int i = 0; i <= (size + 7) / 8; i++) {
			if(!testBit.get(i))
				return null;
		}
		byte[] result = new byte[size];		
		for (IPDatagram datagram: list) {
			System.arraycopy(datagram.getData(), 0, result, datagram.fragmentOffset() * 8, datagram.getData().length);
		}
		return result;
	}
}
