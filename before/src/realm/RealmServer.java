package realm;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Calendar;

import common.Ancestra;

public class RealmServer implements Runnable{

	private ServerSocket _SS;
	private Thread _t;
	
	public static int _totalNonAbo = 0;//Total de connections non abo
	public static int _totalAbo = 0;//Total de connections abo
	public static int _queueID = -1;//Numéro de la queue
	public static int _subscribe = 1;//File des non abonnées (0) ou abonnées (1)
	
	public RealmServer()
	{
		try {
			_SS = new ServerSocket(Ancestra.CONFIG_REALM_PORT);
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		} catch (IOException e) {
			addToLog("IOException: "+e.getMessage());
			Ancestra.closeServers();
		}
		
	}

	public void run()
	{	
		while(Ancestra.isRunning)//bloque sur _SS.accept()
		{
			try
			{
				new RealmThread(_SS.accept());
			}catch(IOException e)
			{
				addToLog("IOException: "+e.getMessage());
				try
				{
					addToLog("Fermeture du serveur de connexion");	
					if(!_SS.isClosed())_SS.close();
				}
				catch(IOException e1){}
			}
		}
	}
	
	public void kickAll()
	{
		try {
			_SS.close();
		} catch (IOException e) {}
	}
	public synchronized static void addToLog(String str)
	{
		System.out.println(str);
		if(Ancestra.canLog)
		{
			try {
				String date = Calendar.HOUR_OF_DAY+":"+Calendar.MINUTE+":"+Calendar.SECOND;
				Ancestra.Log_Realm.write(date+": "+str);
				Ancestra.Log_Realm.newLine();
				Ancestra.Log_Realm.flush();
			} catch (IOException e) {}//ne devrait pas avoir lieu
		}
	}
	
	public synchronized static void addToSockLog(String str)
	{
		if(Ancestra.CONFIG_DEBUG)System.out.println(str);
		if(Ancestra.canLog)
		{
			try {
				String date = Calendar.HOUR_OF_DAY+":"+Calendar.MINUTE+":"+Calendar.SECOND;
				Ancestra.Log_RealmSock.write(date+": "+str);
				Ancestra.Log_RealmSock.newLine();
				Ancestra.Log_RealmSock.flush();
			} catch (IOException e) {}//ne devrait pas avoir lieu
		}
	}

	public Thread getThread()
	{
		return _t;
	}
}
