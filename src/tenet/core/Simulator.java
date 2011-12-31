package tenet.core;

import java.util.ArrayList;
import java.util.PriorityQueue;

import org.knf.tenet.result.ResultManager;

import tenet.anticheat.ICallWatcher;
import tenet.node.INode;
import tenet.util.pattern.IParam;
import tenet.util.pattern.cbc.Command;
import tenet.util.pattern.cbc.IInvoker;
import tenet.util.pattern.cbc.IReceiver;

/**
 * 模拟器类.<br>
 * 该类作为单例类存在，使用时通过{@link #getInstance()}获得实例。
 * @author meilunsheng
 * @version 09.01
 */
public class Simulator implements IInvoker, IReceiver {

	private static Simulator m_instance = null;

	/**
	 * 获得实例
	 * @return Simulator实例
	 */
	public static Simulator getInstance() {
		if (m_instance == null)
			m_instance = new Simulator();
		return m_instance;
	}

	/**
	 * 获得当前模拟器时间
	 * @return 当前模拟器时间
	 */
	public static double GetTime() {
		return getInstance().getTime();
	}

	/**
	 * 向模拟器增加一个命令的调度
	 * @param cmd 需要被调度的命令
	 */
	public static void Schedule(Command cmd) {
		getInstance().schedule(cmd);
	}

	private PriorityQueue<Command> m_cmdQueue;

	private ICallWatcher m_cwatcher;

	private ArrayList<INode> m_nodes;

	private double m_time;

	protected Simulator() {
		super();
		this.m_cmdQueue = new PriorityQueue<Command>();
		this.m_nodes = new ArrayList<INode>();
		this.m_time = 0;
	}

	public void dump() {
		System.out.println("------Nodes-----");
		for (INode p : this.m_nodes)
			p.dump();
		System.out.println("------Commands-----");
		for (Command c : this.m_cmdQueue)
			c.dump();
	}

	public double getTime() {
		return m_time;
	}

	/**
	 * 开始模拟，将按照调度队列里面的顺序执行命令，直到执行了{@link SystemStop}命令，或者没有命令需要被执行时，系统停止
	 * 被调度的命令将根据执行时间决定优先执行，在调试模式下，可以使用{@link SystemPause}挂起模拟线程
	 */
	public void run() {
		while (!this.m_cmdQueue.isEmpty()) {
			Command cmd = this.m_cmdQueue.poll();
			m_time = cmd.getExecuteTime();
			IParam result = cmd._execute();
			assert result instanceof SystemPause : "System Pause";
			if (cmd instanceof SystemStop){
				ResultManager.getInstance().dump();
				break;
			}
		}
	}

	public void schedule(Command cmd) {
		if (m_cwatcher != null) {
			Exception e = new Exception();
			m_cwatcher.eventRecord(m_cwatcher, e.getStackTrace());
		}
		this.m_cmdQueue.add(cmd);
	}

	public void setCallWatcher(ICallWatcher cwatcher) {
		this.m_cwatcher = cwatcher;
	}

	/**
	 * 撤销调度，但不保证命令不会被执行。
	 * @param cmd
	 */
	public void unschedule(Command cmd) {
		this.m_cmdQueue.remove(cmd);
	}
}
