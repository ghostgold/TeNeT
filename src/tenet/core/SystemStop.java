package tenet.core;

import tenet.util.pattern.IParam;
import tenet.util.pattern.cbc.Command;

public class SystemStop extends Command {

	public SystemStop(double m_time) {
		super(m_time, null);
	}

	@Override
	public IParam execute() {
		return null;
	}

}
