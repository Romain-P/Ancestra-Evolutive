package game;

import java.util.concurrent.TimeUnit;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import tool.packetfilter.PacketFilter;

import common.Constants;
import common.SocketManager;

import core.Console;
import core.Server;

public class GameHandler implements IoHandler {
	private static PacketFilter filter = new PacketFilter(5, 1, TimeUnit.SECONDS).activeSafeMode();
	
	@Override
	public void sessionCreated(IoSession arg0) throws Exception {
		if(!filter.authorizes(Constants.getIp(arg0.getRemoteAddress().toString())))
			arg0.close(true);
		else {
			GameClient client = new GameClient(arg0);
	
			SocketManager.GAME_SEND_HELLOGAME_PACKET(client);
			Server.config.getGameServer().getClients().put(arg0.getId(), client);
			
			if(Server.config.getGameServer().getClients().size() 
					> Server.config.getGameServer().getMaxPlayer())
				Server.config.getGameServer().updateMaxPlayer();
			
			Console.instance.println("gSession "+arg0.getId()+" : created");
		}
	}
	
	@Override
	public void messageReceived(IoSession arg0, Object arg1) throws Exception {
		String packet = (String) arg1;

		String[] toParse = packet.split("\n");
		
		for(int i=toParse.length ; i > 0 ; i--) {
			Server.config.getGameServer().getClients().get(arg0.getId()).parsePacket(toParse[toParse.length-i]);
			Console.instance.println("gSession "+arg0.getId()+" : < recv < "+toParse[toParse.length-i]);
		}
	}
	
	@Override
	public void sessionClosed(IoSession arg0) throws Exception {
		GameClient client = Server.config.getGameServer().getClients().get(arg0.getId());
		client.kick();
		Server.config.getGameServer().getClients().remove(client.getSession().getId());
	}

	@Override
	public void exceptionCaught(IoSession arg0, Throwable arg1)throws Exception {
		Console.instance.println("gSession "+arg0.getId()+" : exception "+arg1.getMessage());
		Server.config.getGameServer().getClients().get(arg0.getId()).kick();
	}

	@Override
	public void messageSent(IoSession arg0, Object arg1) throws Exception {
		Console.instance.println("gSession "+arg0.getId()+" > sent > "+arg1.toString());
	}

	@Override
	public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {
		Console.instance.println("rSession "+arg0.getId()+" : disconnected ("+arg1.toString()+")");
		
		GameClient client = Server.config.getGameServer().getClients().get(arg0.getId());
		SocketManager.REALM_SEND_MESSAGE(client,"01|"); 
		client.kick();
		Server.config.getGameServer().getClients().remove(client.getSession().getId());
	}

	@Override
	public void sessionOpened(IoSession arg0) throws Exception {
		arg0.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 60*15*1000);
	}
}
