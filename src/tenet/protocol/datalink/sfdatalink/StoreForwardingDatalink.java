//XXX author Ruixin Qiang
package tenet.protocol.datalink.sfdatalink;


import java.util.LinkedList;

import tenet.core.Simulator;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.datalink.SEther.SimpleEthernetDatalink;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.physics.Link;

/**
* 二层交换需要使用到的具有储存转发能力的IDatalinkLayer
* 一般来说，只有L2Switch会使用到StoreForwardingDatalink
* 同时，StoreForwardingDatalink绑定到的Node一般是L2Switch
*/
public class StoreForwardingDatalink extends SimpleEthernetDatalink {

	protected final static boolean debug=false;

    /**
     * 所要转发的帧的队列
     */
	private LinkedList<FrameParamStruct> sendQueue;

	private boolean noFrameOnGoing  = true;
	
	public StoreForwardingDatalink(MediumAddress m_mac) {
		super(m_mac);
		sendQueue = new LinkedList<FrameParamStruct>();
	}

	/**
	* 当链路断开时进行的处理过程
	*/
	@Override
	protected void linkDown() {
		super.linkDown(); //调用父类过程，使得状态改变的信号达到所有相关对对象。
		//重置所有中断的等待状态，避免接收到不合理的中断导致逻辑错误
		this.resetInterrupt(Link.INT_LINK_READ_ERROR);
		this.resetInterrupt(Link.INT_LINK_READ_OK);
		this.resetInterrupt(INT_RECEIVE_LINKDOWN);
		this.resetInterrupt(INT_RECEIVE_COLLISION);
		this.resetInterrupt(Link.INT_LINK_SEND_ERROR);
		this.resetInterrupt(Link.INT_LINK_SEND_OK);
		this.resetInterrupt(INT_SEND_LINKDOWN);
		this.resetInterrupt(INT_SEND_COLLISION);
		this.sendQueue.clear();//所有等待发送的帧都可以被丢弃
	}

	/**
	* 当链路变为连接状态时进行的处理
	*/
	@Override
	protected void linkUp() {
		super.linkUp();//调用父类过程，使得状态改变的信号达到所有相关对对象。
		this.waitReceiveSignal();//等待所有接受需要的中断信号
	}

	//下面的这些过程都是在SimpleEthernetDatalink.interruptHandler中调用的函数，具体对应的中断信号可以去原函数查看
	
	/**
	 * 处理接收帧出现错误的情况
	 */
	@Override
	protected void onReadError(FrameParamStruct param) {
		this.waitReceiveSignal();
	}

	/**
	 * 处理接收帧正确的情况
	 */
	@Override
	protected void onReadOK(FrameParamStruct param) {
		if (m_node instanceof InterruptObject) {
			ReceiveParam frame =  new ReceiveParam(ReceiveStatus.receiveOK, (InterruptObject) m_node, this,
						(FrameParamStruct) param);
			((InterruptObject)m_node).delayInterrupt(INT_FRAME_RECEIVE, frame, m_delay);
		}
		this.waitReceiveSignal();
	}

	/**
	 * 处理接收帧出现校验错误的情况
	 */
	@Override
	protected void onReadOKwithCheckError(FrameParamStruct param) {
		this.waitReceiveSignal();
	}

	/**
	 * 处理出现接收冲突的自中断的情况
	 */
	@Override
	protected void onReceiveRequireCollision(FrameParamStruct param) {
		this.waitReceiveSignal();
	}

	/**
	 * 处理出现非接入状态接收的自中断的情况
	 */
	@Override
	protected void onReceiveRequireLinkDown(FrameParamStruct param) {
		this.waitReceiveSignal();
	}

	/**
	 * 处理发送帧出现错误的情况
	 */
	@Override
	protected void onSendError(FrameParamStruct param) {
		onSendOK(param);
	}

	/**
	 * 处理成功发送帧的情况
	 */
	@Override
	protected void onSendOK(FrameParamStruct param) {
		if (!isLinkUp()) 
			return;
		noFrameOnGoing = true;
		while (true) {
			if (sendQueue.isEmpty())
				break;
			FrameParamStruct next = sendQueue.remove();
			if (next != null && next.dataParam.length <= m_MTU) { 
				waitTransmitSignal();
				TransmitFrame command = new TransmitFrame(Simulator.getInstance().getTime() + m_delay, this, next.toBytes()); 
				Simulator.getInstance().schedule(command);
				noFrameOnGoing = false;
				break;
			}
		}
	}

	/**
	 * 处理出现发送冲突的自中断的情况
	 */
	@Override
	protected void onTransmitRequireCollision(FrameParamStruct param) {
		onSendOK(param);
	}

	/**
	 * 处理出现非接入状态发送的自中断的情况
	 */
	@Override
	protected void onTransmitRequireLinkDown(FrameParamStruct param) {
		onSendOK(param);
	}
	
	@Override 
	public void transmitFrame(FrameParamStruct frame) {
		sendQueue.add(frame);
		if (noFrameOnGoing) 
			onSendOK(null);
	}
}
