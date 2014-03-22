package game;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import objects.Compte;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import common.World;

import core.Console;
import core.Log;
import core.Server;

public class GameServer {

	private Map<Long, GameClient> clients = new HashMap<>();
	private ArrayList<Compte> waitingClients = new ArrayList<Compte>();
	private long startTime;
	private int maxConnections = 0;
	private IoAcceptor acceptor;
	
	public GameServer() {
		Executor worker = Executors.newCachedThreadPool();
		acceptor = new NioSocketAcceptor(worker, new NioProcessor(worker));
		acceptor.getFilterChain().addLast("game-codec-filter", 
				new ProtocolCodecFilter(
				new TextLineCodecFactory(Charset.forName("UTF8"), LineDelimiter.NUL, 
				new LineDelimiter("\n\0"))));
		acceptor.setHandler(new GameHandler());
	}
	
	public void initialize() {
		try { 
			acceptor.bind(new InetSocketAddress(Server.config.getGamePort()));
			startTime = System.currentTimeMillis();
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
	
	public static String getServerDate() {
		Date actDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd");
		String jour = Integer.parseInt(dateFormat.format(actDate))+"";
		
		while(jour.length() <2)
			jour = "0"+jour;
		
		dateFormat = new SimpleDateFormat("MM");
		String mois = (Integer.parseInt(dateFormat.format(actDate))-1)+"";
		
		while(mois.length() <2)
			mois = "0"+mois;
		
		dateFormat = new SimpleDateFormat("yyyy");
		String annee = (Integer.parseInt(dateFormat.format(actDate))-1370)+"";
		return "BD"+annee+"|"+mois+"|"+jour;
	}
	
	public void scheduleActions() {
		ScheduledExecutorService scheduler = World.data.getScheduler();
		
		scheduler.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				if(!Server.config.isSaving())
					World.data.saveData(-1);
			}
		}, Server.config.getSaveTime(),Server.config.getSaveTime(), TimeUnit.MILLISECONDS);
		
		scheduler.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				World.database.getOtherData().reloadLiveActions();
				Log.addToLog("Les live actions ont ete appliquees");
			}
		}, Server.config.getLoadDelay(),Server.config.getLoadDelay(), TimeUnit.MILLISECONDS);
		
		scheduler.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				World.data.RefreshAllMob();
				Log.addToLog("La recharge des mobs est finie");
			}
		}, Server.config.getReloadMobDelay(),Server.config.getReloadMobDelay(), TimeUnit.MILLISECONDS);
	}
	
	public Map<Long, GameClient> getClients() {
		return clients;
	}
	

	public long getStartTime()
	{
		return startTime;
	}
	
	public int getMaxPlayer()
	{
		return maxConnections;
	}
	
	public int getPlayerNumber()
	{
		return clients.size();
	}
	
	public void delClient(GameClient gameClient) {
		clients.remove(gameClient.getSession().getId());
		if(clients.size() > maxConnections)maxConnections = clients.size();
	}

	public synchronized Compte getWaitingCompte(int guid) {
		for (int i = 0; i < waitingClients.size(); i++) 
			if(waitingClients.get(i).get_GUID() == guid)
				return waitingClients.get(i);
		return null;
	}
	
	public synchronized void delWaitingCompte(Compte _compte) {
		waitingClients.remove(_compte);
	}
	
	public synchronized void addWaitingCompte(Compte _compte) {
		waitingClients.add(_compte);
	}
	
	public static String getServerTime() {
		Date actDate = new Date();
		return "BT"+(actDate.getTime()+3600000);
	}

	public void updateMaxPlayer() {
		maxConnections = this.clients.size();
	}
}
