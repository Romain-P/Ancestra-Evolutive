package realm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import core.Console;
import core.Server;

public class RealmServer {
	public static int _totalNonAbo = 0;//Total de connections non abo
	public static int _totalAbo = 0;//Total de connections abo
	public static int _queueID = -1;//Numéro de la queue
	public static int _subscribe = 1;//File des non abonnées (0) ou abonnées (1)
	
	private Map<Long, RealmClient> clients = new HashMap<>();
	private IoAcceptor acceptor;
	
	public RealmServer() {
		Executor worker = Executors.newCachedThreadPool();
		acceptor = new NioSocketAcceptor(worker, new NioProcessor(worker));
		acceptor.getFilterChain().addLast("realm-codec-filter", 
				new ProtocolCodecFilter(
				new TextLineCodecFactory(Charset.forName("UTF8"), LineDelimiter.NUL, 
				new LineDelimiter("\n\0"))));
		acceptor.setHandler(new RealmHandler());
	}
	
	public void initialize() {
		try { 
			acceptor.bind(new InetSocketAddress(Server.config.getRealmPort()));
		} catch (IOException e) {
			Console.instance.writeln("NioSocket ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	
	public void close() {
		 acceptor.unbind();
		 
		 for (IoSession session : acceptor.getManagedSessions().values())
			 if (session.isConnected() || !session.isClosing()) 
				 session.close(true);
	     
	     acceptor.dispose();
	}

	public Map<Long, RealmClient> getClients() {
		return clients;
	}
}
