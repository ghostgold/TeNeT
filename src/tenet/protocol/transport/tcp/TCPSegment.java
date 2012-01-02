package tenet.protocol.transport.tcp;

import java.util.Arrays;

import tenet.util.ByteLib;

public class TCPSegment {
	public TCPSegment() {
		
	}
	
	int srcPort;
	int dstPort;
	int seqNum;
	int ackNum;
	byte dataOffset = 5	;
	byte controlBits;
	int windowSize;
	int checkSum;
	byte[] data = new byte[0];
	
	boolean getACK() {
		return ((controlBits >> 4) & 1) == 1;
	}
	boolean getRST() {
		return ((controlBits >> 2) & 1) == 1;
	}
	boolean getSYN() {
		return ((controlBits >> 1) & 1) == 1;
	}
	boolean getFIN() {
		return ((controlBits >> 0) & 1) == 1;
	}
	
	void setACK() {
		controlBits |= (1 << 4);
	}
	
	void setRST() {
		controlBits |= (1 << 2);
	}

	void setSYN() {
		controlBits |= (1 << 1);
	}

	void setFIN() {
		controlBits |= (1 << 0);
	}

	public byte[] toBytes() {
		byte[] result = new byte[dataOffset * 4 + data.length];
		result[0] = ByteLib.byteFromUnsigned(srcPort, 8);
		result[1] = ByteLib.byteFromUnsigned(srcPort, 0);
		result[2] = ByteLib.byteFromUnsigned(dstPort, 8);
		result[3] = ByteLib.byteFromUnsigned(dstPort, 0);
		System.arraycopy(intToBytes(seqNum), 0, result, 4, 4);
		System.arraycopy(intToBytes(ackNum), 0, result, 8, 4);
		result[12] = dataOffset;
		result[13] = controlBits;
		result[14] = ByteLib.byteFromUnsigned(windowSize, 8);
		result[15] = ByteLib.byteFromUnsigned(windowSize, 0);
		System.arraycopy(intToBytes(checkSum), 0, result, 16, 4);
		System.arraycopy(data, 0, result, 20, data.length);
		return result;
	}
	
	public static TCPSegment fromBytes(byte[] segment) {
		TCPSegment result = new TCPSegment();
		result.srcPort = ((segment[0] & 0xFF)<< 8) + segment[1];
		result.dstPort = ((segment[2]  & 0xFF) << 8) + segment[3];
		result.seqNum = bytesToInt(segment, 4);
		result.ackNum = bytesToInt(segment, 8);
		result.dataOffset = segment[12];
		result.controlBits = segment[13];
		result.windowSize =( (segment[14] & 0xFF)<< 8) + segment[15];
		result.checkSum = bytesToInt(segment, 16);
		result.data = Arrays.copyOfRange(segment, result.dataOffset * 4, segment.length);
		return result;
	}

	private static byte[] intToBytes(int value) {
		byte[] result = new byte[4];
		result[0] = ByteLib.byteFromUnsigned(value, 24);
		result[1] = ByteLib.byteFromUnsigned(value, 16);
		result[2] = ByteLib.byteFromUnsigned(value, 8);
		result[3] = ByteLib.byteFromUnsigned(value, 0);
		return result;
	}
	
	private static int bytesToInt(byte[] value, int offset) {
		return ByteLib.byteToUnsigned(value[offset], 24)+ ByteLib.byteToUnsigned(value[offset+1], 16) + ByteLib.byteToUnsigned(value[offset + 2], 8) + ByteLib.byteToUnsigned(value[offset + 3], 0);
	}
}
