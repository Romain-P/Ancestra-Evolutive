package common;

import game.GameServer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;

import realm.RealmServer;

public class Ancestra {
	
	private static final String CONFIG_FILE = "config.txt";
	public static String IP = "127.0.0.1";
	public static boolean isInit = false;
	public static String DB_HOST;
	public static String DB_USER;
	public static String DB_PASS;
	public static String STATIC_DB_NAME;
	public static String OTHER_DB_NAME;
	public static long FLOOD_TIME = 60000;
	public static String GAMESERVER_IP;
	public static String CONFIG_MOTD = "";
	public static String CONFIG_MOTD_COLOR = "";
	public static boolean CONFIG_DEBUG = false;
	public static PrintStream PS;
	public static boolean CONFIG_POLICY = false;
	public static int CONFIG_REALM_PORT = 443;
	public static int CONFIG_GAME_PORT 	= 5555;
	public static int CONFIG_MAX_PERSOS = 5;
	public static short CONFIG_START_MAP = 10298;
	public static int CONFIG_START_CELL = 314;
	public static boolean CONFIG_ALLOW_MULTI = false;
	public static int CONFIG_START_LEVEL = 1;
	public static int CONFIG_START_KAMAS = 0;
	public static int CONFIG_SAVE_TIME = 10*60*10000;
	public static int CONFIG_DROP = 1;
	public static boolean CONFIG_ZAAP = false;
	public static int CONFIG_LOAD_DELAY = 60000;
	public static int CONFIG_RELOAD_MOB_DELAY = 18000000;
	public static int CONFIG_PLAYER_LIMIT = 30;
	public static boolean CONFIG_IP_LOOPBACK = true;
	public static int XP_PVP = 10;
	public static int LVL_PVP = 15;
	public static boolean ALLOW_MULE_PVP = false;
	public static int XP_PVM = 1;
	public static int KAMAS = 1;
	public static int HONOR = 1;
	public static int XP_METIER = 1;
	public static boolean CONFIG_CUSTOM_STARTMAP;
	public static boolean CONFIG_USE_MOBS = false;
	public static boolean CONFIG_USE_IP = false;
	public static GameServer gameServer;
	public static RealmServer realmServer;
	public static boolean isRunning = false;
	public static BufferedWriter Log_GameSock;
	public static BufferedWriter Log_Game;
	public static BufferedWriter Log_Realm;
	public static BufferedWriter Log_MJ;
	public static BufferedWriter Log_RealmSock;
	public static BufferedWriter Log_Shop;
	public static boolean canLog;
	public static boolean isSaving = false;
	public static boolean AURA_SYSTEM = false;
	//Arene
	public static ArrayList<Integer> arenaMap = new ArrayList<Integer>(8);
	public static int CONFIG_ARENA_TIMER = 10*60*1000;// 10 minutes
	//BDD
	public static int CONFIG_DB_COMMIT = 30*1000;
	//Inactivité
	public static int CONFIG_MAX_IDLE_TIME = 1800000;//En millisecondes
	//HDV
	public static ArrayList<Integer> NOTINHDV = new ArrayList<Integer>();
	//UseCompactDATA
	public static boolean CONFIG_SOCKET_USE_COMPACT_DATA = false;
	public static int CONFIG_SOCKET_TIME_COMPACT_DATA = 200;
	
	public static void main(String[] args)
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				Ancestra.closeServers();
			}
		}
		);
		System.out.println("==============================================================\n\n");
		System.out.println(makeHeader());
		System.out.println("==============================================================\n");
		System.out.println("Chargement de la configuration :");
		loadConfiguration();
		isInit = true;
		System.out.println("Configuration Ok !");
		System.out.println("Connexion a la base de donnee :");
		if(SQLManager.setUpConnexion()) System.out.println("Connexion Ok !");
		else
		{
			System.out.println("Connexion invalide");
			Ancestra.closeServers();
			System.exit(0);
		}
		System.out.println("Creation du Monde :");
		long startTime = System.currentTimeMillis();
		World.createWorld();
		long endTime = System.currentTimeMillis();
		long differenceTime = (endTime - startTime)/1000;
		System.out.println("Monde Ok ! en : "+differenceTime+" s");
		isRunning = true;
		System.out.println("Lancement du serveur de Jeu sur le port "+CONFIG_GAME_PORT);
		String Ip = "";
		try
		{
			Ip = InetAddress.getLocalHost().getHostAddress();
		}catch(Exception e)
		{
			System.out.println(e.getMessage());
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {}
			System.exit(1);
		}
		Ip = IP;
		gameServer = new GameServer(Ip);
		System.out.println("Lancement du serveur de Connexion sur le port : "+CONFIG_REALM_PORT);
		realmServer = new RealmServer();
		if(CONFIG_USE_IP)
			System.out.println("Ip du serveur "+IP+" crypt "+GAMESERVER_IP);
		System.out.println("En attente de connexions");
		if(CONFIG_SOCKET_USE_COMPACT_DATA)
		{
			System.out.println("Lancement du FlushTimer");
			SendManager.FlushTimer().start();
			System.out.println("FlushTimer : Ok !");
		}
	}
	
	private static void loadConfiguration()
	{
		boolean log = false;
		try {
			BufferedReader config = new BufferedReader(new FileReader(CONFIG_FILE));
			String line = "";
			while ((line=config.readLine())!=null)
			{
				if(line.split("=").length == 1) continue ;
				String param = line.split("=")[0].trim();
				String value = line.split("=")[1].trim();
				if(param.equalsIgnoreCase("DEBUG"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Ancestra.CONFIG_DEBUG = true;
						System.out.println("Mode Debug: On");
					}
				}else if(param.equalsIgnoreCase("SEND_POLICY"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Ancestra.CONFIG_POLICY = true;
					}
				}else if(param.equalsIgnoreCase("LOG"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						log = true;
					}
				}else if(param.equalsIgnoreCase("USE_CUSTOM_START"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Ancestra.CONFIG_CUSTOM_STARTMAP = true;
					}
				}else if(param.equalsIgnoreCase("START_KAMAs"))
				{
					Ancestra.CONFIG_START_KAMAS = Integer.parseInt(value);
					if(Ancestra.CONFIG_START_KAMAS < 0 )
						Ancestra.CONFIG_START_KAMAS = 0;
					if(Ancestra.CONFIG_START_KAMAS > 1000000000)
						Ancestra.CONFIG_START_KAMAS = 1000000000;
				}else if(param.equalsIgnoreCase("START_LEVEL"))
				{
					Ancestra.CONFIG_START_LEVEL = Integer.parseInt(value);
					if(Ancestra.CONFIG_START_LEVEL < 1 )
						Ancestra.CONFIG_START_LEVEL = 1;
					if(Ancestra.CONFIG_START_LEVEL > 200)
						Ancestra.CONFIG_START_LEVEL = 200;
				}else if(param.equalsIgnoreCase("START_MAP"))
				{
					Ancestra.CONFIG_START_MAP = Short.parseShort(value);
				}else if(param.equalsIgnoreCase("START_CELL"))
				{
					Ancestra.CONFIG_START_CELL = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("KAMAS"))
				{
					Ancestra.KAMAS = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("HONOR"))
				{
					Ancestra.HONOR = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("SAVE_TIME"))
				{
					Ancestra.CONFIG_SAVE_TIME = Integer.parseInt(value)*60*1000000000;
				}else if(param.equalsIgnoreCase("XP_PVM"))
				{
					Ancestra.XP_PVM = Integer.parseInt(value);
				}
				else if(param.equalsIgnoreCase("XP_PVP"))
				{
					Ancestra.XP_PVP = Integer.parseInt(value);
				}
				else if(param.equalsIgnoreCase("LVL_PVP"))
				{
					Ancestra.LVL_PVP = Integer.parseInt(value);
				}
				else if(param.equalsIgnoreCase("DROP"))
				{
					Ancestra.CONFIG_DROP = Integer.parseInt(value);
				}
				else if(param.equalsIgnoreCase("LOCALIP_LOOPBACK"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Ancestra.CONFIG_IP_LOOPBACK = true;
					}
				}else if(param.equalsIgnoreCase("ZAAP"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Ancestra.CONFIG_ZAAP = true;
					}
				}else if(param.equalsIgnoreCase("USE_IP"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Ancestra.CONFIG_USE_IP = true;
					}
				}else if(param.equalsIgnoreCase("MOTD"))
				{
					Ancestra.CONFIG_MOTD = line.split("=",2)[1];
				}else if(param.equalsIgnoreCase("MOTD_COLOR"))
				{
					Ancestra.CONFIG_MOTD_COLOR = value;
				}else if(param.equalsIgnoreCase("XP_METIER"))
				{
					Ancestra.XP_METIER = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("GAME_PORT"))
				{
					Ancestra.CONFIG_GAME_PORT = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("REALM_PORT"))
				{
					Ancestra.CONFIG_REALM_PORT = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("FLOODER_TIME"))
				{
					Ancestra.FLOOD_TIME = Integer.parseInt(value);
				}
				else if(param.equalsIgnoreCase("HOST_IP"))
				{
					Ancestra.IP = value;
				}
				else if(param.equalsIgnoreCase("DB_HOST"))
				{
					Ancestra.DB_HOST = value;
				}else if(param.equalsIgnoreCase("DB_USER"))
				{
					Ancestra.DB_USER = value;
				}else if(param.equalsIgnoreCase("DB_PASS"))
				{
					if(value == null) value = "";
					Ancestra.DB_PASS = value;
				}else if(param.equalsIgnoreCase("STATIC_DB_NAME"))
				{
					Ancestra.STATIC_DB_NAME = value;
				}else if(param.equalsIgnoreCase("OTHER_DB_NAME"))
				{
					Ancestra.OTHER_DB_NAME = value;
				}else if(param.equalsIgnoreCase("MAX_PERSO_PAR_COMPTE"))
				{
					Ancestra.CONFIG_MAX_PERSOS = Integer.parseInt(value);
				}else if (param.equalsIgnoreCase("USE_MOBS"))
				{
					Ancestra.CONFIG_USE_MOBS = value.equalsIgnoreCase("true");
				}else if (param.equalsIgnoreCase("ALLOW_MULTI_ACCOUNT"))
				{
					Ancestra.CONFIG_ALLOW_MULTI = value.equalsIgnoreCase("true");
				}else if (param.equalsIgnoreCase("LOAD_ACTION_DELAY"))
				{
					Ancestra.CONFIG_LOAD_DELAY = (Integer.parseInt(value) * 1000);
				}else if (param.equalsIgnoreCase("PLAYER_LIMIT"))
				{
					Ancestra.CONFIG_PLAYER_LIMIT = Integer.parseInt(value);
				}else if (param.equalsIgnoreCase("ARENA_MAP"))
				{
					for(String curID : value.split(","))
					{
						Ancestra.arenaMap.add(Integer.parseInt(curID));
					}
				}else if (param.equalsIgnoreCase("ARENA_TIMER"))
				{
					Ancestra.CONFIG_ARENA_TIMER = Integer.parseInt(value);
				}else if (param.equalsIgnoreCase("AURA_SYSTEM"))
				{
					Ancestra.AURA_SYSTEM = value.equalsIgnoreCase("true");
				}else if (param.equalsIgnoreCase("ALLOW_MULE_PVP"))
				{
					Ancestra.ALLOW_MULE_PVP = value.equalsIgnoreCase("true");
				}else if (param.equalsIgnoreCase("MAX_IDLE_TIME"))
				{
					Ancestra.CONFIG_MAX_IDLE_TIME = (Integer.parseInt(value)*60000);
				}else if (param.equalsIgnoreCase("NOT_IN_HDV"))
				{
					for(String curID : value.split(","))
					{
						Ancestra.NOTINHDV.add(Integer.parseInt(curID));
					}
				}else if (param.equalsIgnoreCase("USE_COMPACT_DATA"))
				{
					Ancestra.CONFIG_SOCKET_USE_COMPACT_DATA = value.equalsIgnoreCase("true");
				}else if (param.equalsIgnoreCase("TIME_COMPACT_DATA"))
					Ancestra.CONFIG_SOCKET_TIME_COMPACT_DATA = Integer.parseInt(value);
				}
			
			if(STATIC_DB_NAME == null || OTHER_DB_NAME == null || DB_HOST == null || DB_PASS == null || DB_USER == null)
			{
				throw new Exception();
			}
		} catch (Exception e) {
            System.out.println(e.getMessage());
			System.out.println("Fichier de configuration non existant ou illisible");
			System.out.println("Fermeture du serveur");
			System.exit(1);
		}
		if(CONFIG_DEBUG)Constants.DEBUG_MAP_LIMIT = 20000;
		try
		{
			String date = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)+"-"+(Calendar.getInstance().get(Calendar.MONTH)+1)+"-"+Calendar.getInstance().get(Calendar.YEAR);
			if(log)
			{
				Log_GameSock = new BufferedWriter(new FileWriter("Game_logs/"+date+"_packets.txt", true));
				Log_Game = new BufferedWriter(new FileWriter("Game_logs/"+date+".txt", true));
				Log_Realm = new BufferedWriter(new FileWriter("Realm_logs/"+date+".txt", true));
				Log_RealmSock = new BufferedWriter(new FileWriter("Realm_logs/"+date+"_packets.txt", true));
				Log_Shop = new BufferedWriter(new FileWriter("Shop_logs/"+date+".txt", true));
				PS = new PrintStream(new File("Error_logs/"+date+"_error.txt"));
				PS.append("Lancement du serveur..\n");
				PS.flush();
				System.setErr(PS);
				Log_MJ = new BufferedWriter(new FileWriter("Gms_logs/"+date+"_GM.txt",true));
				canLog = true;
				String str = "Lancement du serveur...\n";
				Log_GameSock.write(str);
				Log_Game.write(str);
				Log_MJ.write(str);
				Log_Realm.write(str);
				Log_RealmSock.write(str);
				Log_Shop.write(str);
				Log_GameSock.flush();
				Log_Game.flush();
				Log_MJ.flush();
				Log_Realm.flush();
				Log_RealmSock.flush();
				Log_Shop.flush();
			}
		}catch(IOException e)
		{
			/*On créer les dossiers*/
			System.out.println("Les fichiers de logs n'ont pas pu etre creer");
			System.out.println("Creation des dossiers");
			new File("Shop_logs").mkdir(); 
			new File("Game_logs").mkdir(); 
			new File("Realm_logs").mkdir(); 
			new File("Gms_logs").mkdir(); 
			new File("Error_logs").mkdir();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	public static void closeServers()
	{
		System.out.println("Arret du serveur demande ...");
		if(isRunning)
		{
			isRunning = false;
			Ancestra.gameServer.kickAll();
			World.saveAll(null);
			SQLManager.closeCons();
		}
		System.out.println("Arret du serveur: OK");
		isRunning = false;
	}

	public static void addToMjLog(String str)
	{
		if(!canLog)return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Log_MJ.write("["+date+"]"+str);
			Log_MJ.newLine();
			Log_MJ.flush();
		} catch (IOException e) {}
	}
	
	public static void addToShopLog(String str)
	{
		if(!canLog)return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Log_Shop.write("["+date+"]"+str);
			Log_Shop.newLine();
			Log_Shop.flush();
		} catch (IOException e) {}
	}
	
	public static String makeHeader()
	{
		StringBuilder mess = new StringBuilder();
		mess.append("Ancestra-R v"+Constants.SERVER_VERSION);
		mess.append("\nPar "+Constants.SERVER_MAKER+" pour Dofus "+Constants.CLIENT_VERSION);
		mess.append("\nThanks Diabu.");
		mess.append("\nhttp://sourceforge.net/projects/ancestrar/\n\n");
		return mess.toString();
	}
}
