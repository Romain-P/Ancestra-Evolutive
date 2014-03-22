package client;

import org.apache.mina.core.session.IoSession;

public interface Client {
	public IoSession getSession();
}
