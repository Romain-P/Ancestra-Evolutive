package realm;

import java.util.concurrent.TimeUnit;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import tool.packetfilter.PacketFilter;

import common.Constants;
import common.SocketManager;

import core.Console;
import core.Server;

public class RealmHandler implements IoHandler {
	private static PacketFilter filter = new PacketFilter(5, 1, TimeUnit.SECONDS).activeSafeMode();
	
	@Override
	public void sessionCreated(IoSession arg0) throws Exception {
		if(!filter.authorizes(Constants.getIp(arg0.getRemoteAddress().toString())))
			arg0.close(true);
		else {
			RealmClient client = new RealmClient(arg0);
			
			if(Server.config.isPolicy())
				SocketManager.REALM_SEND_POLICY_FILE(client);
	        
			client.set_hashKey(SocketManager.REALM_SEND_HC_PACKET(client));
			Server.config.getRealmServer().getClients().put(arg0.getId(), client);
			
			Console.instance.println("rSession "+arg0.getId()+" : created");
		}
	}
	
	@Override
	public void messageReceived(IoSession arg0, Object arg1) throws Exception { 
		String packet = (String) arg1;
		
		String[] toParse = packet.split("\n");
		
		for(int i=toParse.length ; i > 0 ; i--) {
			RealmClient client = Server.config.getRealmServer().getClients().get(arg0.getId());
			client.addPacket();
			client.parsePacket(toParse[toParse.length-i]);

			Console.instance.println("rSession "+arg0.getId()+" : < recv < "+toParse[toParse.length-i]);
		}
	}
	
	@Override
	public void sessionClosed(IoSession arg0) throws Exception {
		RealmClient client = Server.config.getRealmServer().getClients().get(arg0.getId());
		client.kick();
		Server.config.getRealmServer().getClients().remove(client.getSession().getId());
	}

	@Override
	public void exceptionCaught(IoSession arg0, Throwable arg1)throws Exception {
		Console.instance.println("rSession "+arg0.getId()+" : exception "+arg1.getMessage());
	}

	@Override
	public void messageSent(IoSession arg0, Object arg1) throws Exception {
		Console.instance.println("rSession "+arg0.getId()+" > sent > "+arg1.toString());
	}

	@Override
	public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {
		Console.instance.println("rSession "+arg0.getId()+" : disconnected ("+arg1.toString()+")");
		
		RealmClient client = Server.config.getRealmServer().getClients().get(arg0.getId());
		SocketManager.REALM_SEND_MESSAGE(client,"01|"); 
		client.kick();
		Server.config.getRealmServer().getClients().remove(client.getSession().getId());
	}

	@Override
	public void sessionOpened(IoSession arg0) throws Exception {
		arg0.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 60*15*1000);
	}
	
	
}
