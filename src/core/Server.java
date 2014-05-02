package core;

import game.GameServer;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import objects.Personnage;
import realm.RealmServer;
import tool.command.Command;
import tool.command.CommandAccess;
import tool.time.restricter.RestrictLevel;
import tool.time.restricter.TimeRestricter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import common.Constants;
import common.CryptManager;
import common.World;


public class Server {
	
	public static Server config = new Server();
	
	//emulator
	private boolean isRunning;
	private boolean isSaving;
	
	private GameServer gameServer;
	private RealmServer realmServer;
	
	//console
	private boolean canLog;
	private boolean debug;
	
	//database
	private String host, user, pass;
	private String databaseName;
	
	//network
	private boolean useIp;
	private String ip;
	private int realmPort, gamePort;
	
	//on player connection
	private String motd, motdColor;
	private PrintStream ps;
	
	//player
	private int maxPlayersPerAccount;
	private short startMap;
	private int startCell;
	private int startLevel, startKamas;
	
	private boolean multiAccount;
	private boolean allZaaps;
	private boolean mulePvp;
	private boolean customStartMap;
	private boolean auraSystem;
	private int maxIdleTime;
	
	//server
	private int saveTime;
	private long floodTime;
	private int loadDelay;
	private int reloadMobDelay;
	private int playerLimitOnServer;
	private boolean useMobs;
	
	//rates
	private int rateDrop;
	private int rateXpPvm;
	private int rateKamas;
	private int rateHonor;
	private int rateXpJob;
	private int rateXpPvp;
	private int averageLevelPvp;
	
	//arena
	private ArrayList<Integer> arenaMaps = new ArrayList<>();
	private int arenaTime;
	
	//hdv
	private ArrayList<Integer> noInHdv = new ArrayList<>();
	
	//Config
	private Config configFile = ConfigFactory.parseFile(new File("config.conf"));
	
	public void initialize() {
		Log.initLogs();
		
		try {
			//console
			this.debug = configFile.getBoolean("console.debug");
			this.setCanLog(configFile.getBoolean("console.log"));
			
			//database
			this.host = configFile.getString("database.host");
			this.user = configFile.getString("database.user");
			this.pass = configFile.getString("database.password");
			this.databaseName = configFile.getString("database.databaseName");
			
			//network
			this.ip = configFile.getString("network.ip");
			this.useIp = !configFile.getBoolean("network.local");
			this.realmPort = configFile.getInt("network.realmPort");
			this.gamePort = configFile.getInt("network.gamePort");
			
			//on player connected
			this.maxPlayersPerAccount = configFile.getInt("onClientConnected" +
					".maxPlayersPerAccount");
			this.startLevel = configFile.getInt("onClientConnected.startLevel");
			this.startMap = (short)configFile.getInt("onClientConnected.startMap");
			this.startCell = configFile.getInt("onClientConnected.startCell");
			this.startKamas = configFile.getInt("onClientConnected.startKamas");
			this.multiAccount = configFile.getBoolean("onClientConnected.multiAccount");
			this.allZaaps = configFile.getBoolean("onClientConnected.allZaaps");
			this.mulePvp = configFile.getBoolean("onClientConnected.mulePvp");
			this.customStartMap = configFile.getBoolean("onClientConnected.customStartMap");
			this.auraSystem = configFile.getBoolean("onClientConnected.auraSystem");
			this.maxIdleTime = configFile.getInt("onClientConnected.maxIdleTime");
			
			//server
			this.saveTime = configFile.getInt("server.saveTime");
			this.floodTime = configFile.getInt("server.floodTime");
			this.loadDelay = configFile.getInt("server.liveActionDelay");
			this.useMobs = configFile.getBoolean("server.useMob");
			this.reloadMobDelay = configFile.getInt("server.reloadMobDelay");
			this.playerLimitOnServer = configFile.getInt("server.playerLimit");
			this.motd = configFile.getString("server.welcomMessage.content");
			this.motdColor = configFile.getString("server.welcomMessage.color");
			
			//rates
			this.rateDrop = configFile.getInt("rates.drop");
			this.rateKamas = configFile.getInt("rates.kamas");
			this.rateHonor = configFile.getInt("rates.honor");
			this.rateXpPvm = configFile.getInt("rates.xpPvm");
			this.rateXpJob = configFile.getInt("rates.xpJob");
			this.rateXpPvp = configFile.getInt("rates.xpPvp");
			this.averageLevelPvp = configFile.getInt("rates.averageLevelPvp");
			
			//arena
			String maps = configFile.getString("arena.maps");
			maps = maps.contains(",") ? maps : maps+",";
			for(String s: maps.split(","))
				this.arenaMaps.add(Integer.parseInt(s));
			
			this.arenaTime = configFile.getInt("arena.time");
			
			//hdvs
			String items = configFile.getString("hdvs.notInHdv");
			items = items.contains(",") ? items : items+",";
			for(String s: maps.split(","))
				this.noInHdv.add(Integer.parseInt(s));
			
			this.arenaTime = configFile.getInt("arena.time");
			
			//initialisation de commandes
			this.initializeCommands();
		} catch(Exception e) {
			Console.instance.writeln(" <> Config illisible ou champs manquants: "+e.getMessage());
			System.exit(1);
		}
	}
	
	public void initializeCommands() {
		//totalit� des commandes
		Map<String, Command<Personnage>> playerCommands = new HashMap<>();
		Map<String, Command<Console>> consoleCommands = new HashMap<>();
		
		/**
		 * Commandes des joueurs
		 */
		
		//teleportation zone de d�part
		if(configFile.getBoolean("commands.players.teleport.savePos.active")) {
			String name = configFile.getString("commands.players.teleport.savePos.name");
			
			//cr�ation de la commande
			Command<Personnage> command = new Command<Personnage>(name) {
				
				@Override
				public void action(Personnage player, String[] args) {
					player.warpToSavePos();
				}
				
			};
			
			//ajout de condition
			command.addAccess(new CommandAccess<Personnage>() {
				@Override
				public boolean authorizes(Personnage player) {
					return player.get_fight() == null;
				}
				
				@Override
				public String getRequiertsMessage() {
					return "Action impossible en combat";
				}
			});
			
			//ajout message de succ�s
			command.addSuccessMessage("Vous avez bien �t� t�l�port� � votre derni�re position sauvegard�e");
			//ajout aux commmandes
			playerCommands.put(name, command);
		}
		
		//sauvegarde du personnage
		if(configFile.getBoolean("commands.players.save.playerSave.active")) {
			String name = configFile.getString("commands.players.save.playerSave.name");
			
			//cr�ation de la commande
			Command<Personnage> command = new Command<Personnage>(name) {
				
				@Override
				public void action(Personnage player, String[] args) {
					player.save();
				}
				
			};
			
			//mise en place d'un restricteur de temps. 1 save/heure puis 5 minutes d'attente avant relance
			TimeRestricter restricter = 
					new TimeRestricter(RestrictLevel.ACCOUNT, 1, 1, TimeUnit.HOURS, 5, TimeUnit.MINUTES);
			command.attachRestricter(restricter).activeErrorMessage(); //indique le temps restant avant relance
			
			
			//ajout message de succ�s
			command.addSuccessMessage("Votre personnage a �t� sauvegard� avec succ�s.");
			//ajout aux commmandes
			playerCommands.put(name, command);
		}
		
		//informations du serveur
		if(configFile.getBoolean("commands.players.informations.serverInfos.active")) {
			String name = configFile.getString("commands.players.informations.serverInfos.name");
			
			//cr�ation de la commande
			Command<Personnage> command = new Command<Personnage>(name) {
				
				@Override
				public void action(Personnage player, String[] args) {
					player.sendText(Constants.serverInfos());
				}
				
			};
			
			//ajout aux commmandes
			playerCommands.put(name, command);
		}

		//liste des commandes
		if(configFile.getBoolean("commands.players.list.commandList.active")) {
			String name = configFile.getString("commands.players.list.commandList.name");
			
			//cr�ation de la commande
			Command<Personnage> command = new Command<Personnage>(name) {
				
				@Override
				public void action(Personnage player, String[] args) {
					StringBuilder commands = new StringBuilder("<b>Liste des commandes disponibles: </b>");
					for(Command<Personnage> command: World.data.getPlayerCommands().values())
						commands.append(command.getName()).append(", ");
					player.sendText(commands.toString().substring(0,
							commands.toString().length()-2));
				}
				
			};
			//ajout aux commmandes
			playerCommands.put(name, command);
		}
		
		/**
		 * Commandes console
		 */
		//informations du serveur
		if(configFile.getBoolean("commands.console.server.uptime.active")) {
			String name = configFile.getString("commands.console.server.uptime.name");
			
			//cr�ation de la commande
			Command<Console> command = new Command<Console>(name) {
				
				@Override
				public void action(Console console, String[] args) {
					Console.instance.writeln(Constants.serverInfos());
				}
				
			};
			
			//ajout aux commmandes
			consoleCommands.put(name, command);
		}
		
		if(configFile.getBoolean("commands.console.server.reboot.active")) {
			String name = configFile.getString("commands.console.server.reboot.name");
			
			//cr�ation de la commande
			Command<Console> command = new Command<Console>(name) {
				
				@Override
				public void action(Console console, String[] args) {
					World.data.saveData(-1);
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {}
					System.exit(0);
				}
				
			};
			
			//ajout aux commmandes
			consoleCommands.put(name, command);
		}
		
		if(configFile.getBoolean("commands.console.server.save.active")) {
			String name = configFile.getString("commands.console.server.save.name");
			
			//cr�ation de la commande
			Command<Console> command = new Command<Console>(name) {
				
				@Override
				public void action(Console console, String[] args) {
					World.data.saveData(-1);
					Console.instance.write(" <> Sauvegarde terminee");
				}
				
			};
			
			//ajout aux commmandes
			consoleCommands.put(name, command);
		}
		
		//Commande fixe HELP
		Command<Console> command = new Command<Console>("HELP") {
			
			@Override
			public void action(Console console, String[] args) {
				StringBuilder commands = new StringBuilder("Liste des commandes disponibles: \n");
				for(Command<Console> command: World.data.getConsoleCommands().values())
					commands.append(command.getName()).append(", ");
				Console.instance.writeln(commands.toString().substring(0,
						commands.toString().length()-2));
			}
			
		};
		
		//ajout aux commmandes
		consoleCommands.put("HELP", command);
		
		
		//ajout des commandes dans les donn�es du serveur
		World.data.getPlayerCommands().putAll(playerCommands);
		World.data.getConsoleCommands().putAll(consoleCommands);
	}
	
	public String getIp() {
		return ip;
	}

	public String getHost() {
		return host;
	}

	public String getUser() {
		return user;
	}

	public String getPass() {
		return pass;
	}
	public long getFloodTime() {
		return floodTime;
	}

	public String getGameServerIp() {
		return getGameServerIpCrypted();
	}

	public String getMotd() {
		return motd;
	}

	public String getMotdColor() {
		return motdColor;
	}

	public boolean isDebug() {
		return debug;
	}

	public PrintStream getPs() {
		return ps;
	}

	public int getRealmPort() {
		return realmPort;
	}

	public int getGamePort() {
		return gamePort;
	}

	public int getMaxPlayersPerAccount() {
		return maxPlayersPerAccount;
	}

	public short getStartMap() {
		return startMap;
	}

	public int getStartCell() {
		return startCell;
	}

	public boolean isMultiAccount() {
		return multiAccount;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public int getStartKamas() {
		return startKamas;
	}

	public int getSaveTime() {
		return saveTime;
	}

	public int getRateDrop() {
		return rateDrop;
	}

	public boolean isAllZaaps() {
		return allZaaps;
	}

	public int getLoadDelay() {
		return loadDelay;
	}

	public int getReloadMobDelay() {
		return reloadMobDelay;
	}

	public int getPlayerLimitOnServer() {
		return playerLimitOnServer;
	}

	public int getRateXpPvp() {
		return rateXpPvp;
	}

	public int getAverageLevelPvp() {
		return averageLevelPvp;
	}

	public boolean isMulePvp() {
		return mulePvp;
	}

	public int getRateXpPvm() {
		return rateXpPvm;
	}

	public int getRateKamas() {
		return rateKamas;
	}

	public int getRateHonor() {
		return rateHonor;
	}

	public int getRateXpJob() {
		return rateXpJob;
	}

	public boolean isCustomStartMap() {
		return customStartMap;
	}

	public boolean isUseMobs() {
		return useMobs;
	}

	public boolean isUseIp() {
		return useIp;
	}

	public GameServer getGameServer() {
		return gameServer;
	}

	public RealmServer getRealmServer() {
		return realmServer;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean isCanLog() {
		return canLog;
	}

	public boolean isSaving() {
		return isSaving;
	}

	public boolean isAuraSystem() {
		return auraSystem;
	}

	public ArrayList<Integer> getArenaMaps() {
		return arenaMaps;
	}

	public int getArenaTimer() {
		return arenaTime;
	}

	public int getMaxIdleTime() {
		return maxIdleTime;
	}

	public ArrayList<Integer> getNoInHdv() {
		return noInHdv;
	}

	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	public void setGameServer(GameServer gameServer) {
		this.gameServer = gameServer;
	}

	public void setRealmServer(RealmServer realmServer) {
		this.realmServer = realmServer;
	}

	public void setSaving(boolean isSaving) {
		this.isSaving = isSaving;
	}

	public void setPs(PrintStream ps) {
		this.ps = ps;
	}

	public void setCanLog(boolean canLog) {
		this.canLog = canLog;
	}

	public String getGameServerIpCrypted() {
		return CryptManager.CryptIP(ip)+CryptManager.CryptPort(gamePort);
	}

	public String getDatabaseName() {
		return databaseName;
	}
}
