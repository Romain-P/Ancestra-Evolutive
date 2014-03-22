package core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;

public class Log {
	
	private static BufferedWriter Log_GameSock;
	private static BufferedWriter Log_Game;
	private static BufferedWriter Log_Realm;
	private static BufferedWriter Log_MJ;
	private static BufferedWriter Log_RealmSock;
	private static BufferedWriter Log_Shop;

	public synchronized static void addToSockLog(String str) {
		if(Server.config.isCanLog()) {
			try {
				String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
				Log.Log_GameSock.write(date+": "+str);
				Log.Log_GameSock.newLine();
				Log.Log_GameSock.flush();
			} catch (IOException e) {}//ne devrait pas avoir lieu
		}
	}

	public synchronized static void addToLog(String str) {
		if(Server.config.isCanLog()) {
			try {
				String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
				Log.Log_Game.write(date+": "+str);
				Log.Log_Game.newLine();
				Log.Log_Game.flush();
			} catch (IOException e) {e.printStackTrace();}//ne devrait pas avoir lieu
		}
	}

	public static void addToMjLog(String str)
	{
		if(!Server.config.isCanLog())return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Log.Log_MJ.write("["+date+"]"+str);
			Log.Log_MJ.newLine();
			Log.Log_MJ.flush();
		} catch (IOException e) {}
	}

	public static void addToShopLog(String str)
	{
		if(!Server.config.isCanLog())return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Log.Log_Shop.write("["+date+"]"+str);
			Log.Log_Shop.newLine();
			Log.Log_Shop.flush();
		} catch (IOException e) {}
	}

	public static void initLogs() {
		try {
			String date = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)+"-"+(Calendar.getInstance().get(Calendar.MONTH)+1)+"-"+Calendar.getInstance().get(Calendar.YEAR);
			Log.Log_GameSock = new BufferedWriter(new FileWriter("Game_logs/"+date+"_packets.txt", true));
			Log.Log_Game = new BufferedWriter(new FileWriter("Game_logs/"+date+".txt", true));
			Log.Log_Realm = new BufferedWriter(new FileWriter("Realm_logs/"+date+".txt", true));
			Log.Log_RealmSock = new BufferedWriter(new FileWriter("Realm_logs/"+date+"_packets.txt", true));
			Log.Log_Shop = new BufferedWriter(new FileWriter("Shop_logs/"+date+".txt", true));
			Server.config.setPs(new PrintStream(new File("Error_logs/"+date+"_error.txt")));
			Server.config.getPs().append("Lancement du serveur..\n");
			Server.config.getPs().flush();
			System.setErr(Server.config.getPs());
			Log.Log_MJ = new BufferedWriter(new FileWriter("Gms_logs/"+date+"_GM.txt",true));
			Server.config.setCanLog(true);
			String str = "Lancement du serveur...\n";
			Log.Log_GameSock.write(str);
			Log.Log_Game.write(str);
			Log.Log_MJ.write(str);
			Log.Log_Realm.write(str);
			Log.Log_RealmSock.write(str);
			Log.Log_Shop.write(str);
			Log.Log_GameSock.flush();
			Log.Log_Game.flush();
			Log.Log_MJ.flush();
			Log.Log_Realm.flush();
			Log.Log_RealmSock.flush();
			Log.Log_Shop.flush();
		}catch(IOException e) {
			/*On créer les dossiers*/
			Console.instance.println("Les fichiers de logs n'ont pas pu etre creer");
			Console.instance.println("Creation des dossiers");
			new File("Shop_logs").mkdir(); 
			new File("Game_logs").mkdir(); 
			new File("Realm_logs").mkdir(); 
			new File("Gms_logs").mkdir(); 
			new File("Error_logs").mkdir();
			Console.instance.println(e.getMessage());
			System.exit(1);
		}
	}

	public synchronized static void addToRealmLog(String str)
	{
		if(Server.config.isCanLog())
		{
			try {
				String date = Calendar.HOUR_OF_DAY+":"+Calendar.MINUTE+":"+Calendar.SECOND;
				Log_Realm.write(date+": "+str);
				Log_Realm.newLine();
				Log_Realm.flush();
			} catch (IOException e) {}//ne devrait pas avoir lieu
		}
	}

	public synchronized static void addToRealmSockLog(String str)
	{
		if(Server.config.isCanLog())
		{
			try {
				String date = Calendar.HOUR_OF_DAY+":"+Calendar.MINUTE+":"+Calendar.SECOND;
				Log_RealmSock.write(date+": "+str);
				Log_RealmSock.newLine();
				Log_RealmSock.flush();
			} catch (IOException e) {}//ne devrait pas avoir lieu
		}
	}

}
