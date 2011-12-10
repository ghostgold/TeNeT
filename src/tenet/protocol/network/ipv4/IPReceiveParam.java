package tenet.protocol.network.ipv4;

import tenet.protocol.interrupt.InterruptParam;


/**
 * IP协议读取包的中断参数
 * 当读取成功时state为OK,对应值为包的内容
 * 当下层错误,导致读取不能时,state为Error,对应值的destination为接口IP,source为0,uid为自身协议号,数据段为null
 * @author meilunsheng
 *
 */
public class IPReceiveParam extends InterruptParam {
	public enum ReceiveType{
		OK,Error
	}
	
	public IPReceiveParam(ReceiveType state, int destination, int source,
			int uid, byte[] data) {
		super();
		this.state = state;
		this.destination = destination;
		this.source = source;
		this.uid = uid;
		this.data = data;
	}
	
	public ReceiveType getState() {
		return state;
	}
	public int getDestination() {
		return destination;
	}
	public int getSource() {
		return source;
	}
	public int getUid() {
		return uid;
	}
	public byte[] getData() {
		return data;
	}

	/**
	 * 状态
	 */
	protected ReceiveType state;
	/**
	 * 目标IP
	 */
	protected int destination; 
	/**
	 * 源IP
	 */
	protected int source;
	/**
	 * 传输层协议号
	 */
	protected int uid;
	/**
	 * 数据段
	 */
	protected byte[] data;
}
