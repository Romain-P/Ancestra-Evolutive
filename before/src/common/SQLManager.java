package common;

import game.GameServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import com.mysql.jdbc.PreparedStatement;

import common.World.*;
import objects.*;
import objects.NPC_tmpl.*;
import objects.Objet.*;
import objects.Sort.*;
import objects.Carte.*;
import objects.Guild.GuildMember;
import objects.HDV.HdvEntry;
import realm.RealmServer;

public class SQLManager {

	private static Connection othCon;
	private static Connection statCon;
	
	private static Timer timerCommit;
	private static boolean needCommit;
	
	public synchronized static ResultSet executeQuery(String query,String DBNAME) throws SQLException
	{
		if(!Ancestra.isInit)
			return null;
		
		Connection DB;
		if(DBNAME.equals(Ancestra.OTHER_DB_NAME))
			DB = othCon;
		else
			DB = statCon;
		
		Statement stat = DB.createStatement();
		ResultSet RS = stat.executeQuery(query);
		stat.setQueryTimeout(300);
		return RS;
	}
	public synchronized static PreparedStatement newTransact(String baseQuery,Connection dbCon) throws SQLException
	{
		PreparedStatement toReturn = (PreparedStatement) dbCon.prepareStatement(baseQuery);
		
		needCommit = true;
		return toReturn;
	}
	public synchronized static void commitTransacts()
	{
		try
		{
			if(othCon.isClosed() || statCon.isClosed())
			{
				closeCons();
				setUpConnexion();
			}
			
			statCon.commit();
			othCon.commit();
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR:"+e.getMessage());
			e.printStackTrace();
			commitTransacts();
		}
	}
	public synchronized static void closeCons()
	{
		try
		{
			commitTransacts();
			
			othCon.close();
			statCon.close();
		}catch (Exception e)
		{
			System.out.println("Erreur a la fermeture des connexions SQL:"+e.getMessage());
			e.printStackTrace();
		}
	}
	public static final boolean setUpConnexion()
	{
		try
		{
			othCon = DriverManager.getConnection("jdbc:mysql://"+Ancestra.DB_HOST+"/"+Ancestra.OTHER_DB_NAME,Ancestra.DB_USER,Ancestra.DB_PASS);
			othCon.setAutoCommit(false);
			
			statCon = DriverManager.getConnection("jdbc:mysql://"+Ancestra.DB_HOST+"/"+Ancestra.STATIC_DB_NAME,Ancestra.DB_USER,Ancestra.DB_PASS);
			statCon.setAutoCommit(false);
			
			if(!statCon.isValid(1000) || !othCon.isValid(1000))
			{
				GameServer.addToLog("SQLError : Connexion a la BD invalide!");
				return false;
			}
			
			needCommit = false;
			TIMER(true);
			
			return true;
		}catch(SQLException e)
		{
			System.out.println("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	private static void closeResultSet(ResultSet RS)
	{
		try {
			RS.getStatement().close();
			RS.close();
		} catch (SQLException e) {e.printStackTrace();}

		
	}
	private static void closePreparedStatement(PreparedStatement p)
	{
		try {
			p.clearParameters();
			p.close();
		} catch (SQLException e) {e.printStackTrace();}
	}
	
	public static void UPDATE_ACCOUNT_DATA(Compte acc)
	{
		try
		{
			String baseQuery = "UPDATE accounts SET " +
								"`bankKamas` = ?,"+
								"`bank` = ?,"+
								"`level` = ?,"+
								"`banned` = ?,"+
								"`friends` = ?,"+
								"`enemy` = ?"+
								" WHERE `guid` = ?;";
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			p.setLong(1, acc.getBankKamas());
			p.setString(2, acc.parseBankObjetsToDB());
			p.setInt(3, acc.get_gmLvl());
			p.setInt(4, (acc.isBanned()?1:0));
			p.setString(5, acc.parseFriendListToDB());
			p.setString(6, acc.parseEnemyListToDB());
			p.setInt(7, acc.get_GUID());
			
			p.executeUpdate();
			closePreparedStatement(p);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_CRAFTS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from crafts;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				ArrayList<Couple<Integer,Integer>> m = new ArrayList<Couple<Integer,Integer>>();
				
				boolean cont = true;
				for(String str : RS.getString("craft").split(";"))
				{
					try
					{
							int tID = Integer.parseInt(str.split("\\*")[0]);
							int qua =  Integer.parseInt(str.split("\\*")[1]);
							m.add(new Couple<Integer,Integer>(tID,qua));
					}catch(Exception e){e.printStackTrace();cont = false;};
				}
				//s'il y a eu une erreur de parsing, on ignore cette recette
				if(!cont)continue;
				
				World.addCraft
				(
					RS.getInt("id"),
					m
				);
			}
			closeResultSet(RS);;
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_GUILDS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from guilds;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				World.addGuild
				(
				new Guild(
						RS.getInt("id"),
						RS.getString("name"),
						RS.getString("emblem"),
						RS.getInt("lvl"),
						RS.getLong("xp"),
						RS.getInt("capital"),
						RS.getInt("nbrmax"),
						RS.getString("sorts"),
						RS.getString("stats")
				),false
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_GUILD_MEMBERS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from guild_members;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				Guild G = World.getGuild(RS.getInt("guild"));
				if(G == null)continue;
				G.addMember(RS.getInt("guid"), RS.getString("name"), RS.getInt("level"), RS.getInt("gfxid"), RS.getInt("rank"), RS.getByte("pxp"), RS.getLong("xpdone"), RS.getInt("rights"), RS.getByte("align"),RS.getDate("lastConnection").toString().replaceAll("-","~"));
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_MOUNTS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from mounts_data;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				World.addDragodinde
				(
					new Dragodinde
					(
						RS.getInt("id"),
						RS.getInt("color"),
						RS.getInt("sexe"),
						RS.getInt("amour"),
						RS.getInt("endurance"),
						RS.getInt("level"),
						RS.getLong("xp"),
						RS.getString("name"),
						RS.getInt("fatigue"),
						RS.getInt("energie"),
						RS.getInt("reproductions"),
						RS.getInt("maturite"),
						RS.getInt("serenite"),
						RS.getString("items"),
						RS.getString("ancetres")
					)
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_DROPS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from drops;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				Monstre MT = World.getMonstre(RS.getInt("mob"));
				MT.addDrop(new Drop(
						RS.getInt("item"),
						RS.getInt("seuil"),
						RS.getFloat("taux"),
						RS.getInt("max")
				));
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_ITEMSETS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from itemsets;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				World.addItemSet(
							new ItemSet
							(
								RS.getInt("id"),
								RS.getString("items"),
								RS.getString("bonus")
							)
						);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_IOTEMPLATE()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from interactive_objects_data;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				World.addIOTemplate(
							new IOTemplate
							(
								RS.getInt("id"),
								RS.getInt("respawn"),
								RS.getInt("duration"),
								RS.getInt("unknow"),
								RS.getInt("walkable")==1
							)
						);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static int LOAD_MOUNTPARKS()
	{
		int nbr = 0;
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from mountpark_data;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				Carte map = World.getCarte(RS.getShort("mapid"));
				if(map == null)continue;
					World.addMountPark(
						new MountPark(
						RS.getInt("owner"),
						map,
						RS.getInt("cellid"),
						RS.getInt("size"),
						RS.getString("data"),
						RS.getInt("guild"),
						RS.getInt("price")
						));
					nbr++;
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}
	public static void LOAD_JOBS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from jobs_data;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				World.addJob(
						new Metier(
							RS.getInt("id"),
							RS.getString("tools"),
							RS.getString("crafts")
							)
						);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void LOAD_AREA()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from area_data;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				Area A = new Area
					(
						RS.getInt("id"),
						RS.getInt("superarea"),
						RS.getString("name")
					);
				World.addArea(A);
				//on ajoute la zone au continent
				A.get_superArea().addArea(A);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_SUBAREA()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from subarea_data;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				SubArea SA = new SubArea
					(
						RS.getInt("id"),
						RS.getInt("area"),
						RS.getInt("alignement"),
						RS.getString("name")
					);
				World.addSubArea(SA);
				//on ajoute la sous zone a la zone
				if(SA.get_area() != null)
					SA.get_area().addSubArea(SA);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static int LOAD_NPCS()
	{
		int nbr = 0;
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from npcs;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				Carte map = World.getCarte(RS.getShort("mapid"));
				if(map == null)continue;
				map.addNpc(RS.getInt("npcid"), RS.getInt("cellid"), RS.getInt("orientation"));
				
				nbr ++;
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}
	public static int LOAD_PERCEPTEURS()
	{
		int nbr = 0;
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from percepteurs;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				Carte map = World.getCarte(RS.getShort("mapid"));
				if(map == null)continue;
				
				World.addPerco(
						new Percepteur(
						RS.getInt("guid"),
						RS.getShort("mapid"),
						RS.getInt("cellid"),
						RS.getByte("orientation"),
						RS.getInt("guild_id"),
						RS.getShort("N1"),
						RS.getShort("N2"),
						RS.getString("objets"),
						RS.getLong("kamas"),
						RS.getLong("xp")
						));
				nbr ++;
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}
	public static int LOAD_HOUSES()
	{
		int nbr = 0;
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from houses;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				Carte map = World.getCarte(RS.getShort("map_id"));
				if(map == null)continue;
				
				World.addHouse(
						new House(
						RS.getInt("id"),
						RS.getShort("map_id"),
						RS.getInt("cell_id"),
						RS.getInt("owner_id"),
						RS.getInt("sale"),
						RS.getInt("guild_id"),
						RS.getInt("access"),
						RS.getString("key"),
						RS.getInt("guild_rights"),
						RS.getInt("mapid"),
						RS.getInt("caseid")
						));
				nbr ++;
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}
	public static void LOAD_COMPTES()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts;",Ancestra.OTHER_DB_NAME);
			String baseQuery = "UPDATE accounts " +
								"SET `reload_needed` = 0 " +
								"WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, othCon);
			while(RS.next())
			{
				Compte C = new Compte(
						RS.getInt("guid"),
						RS.getString("account").toLowerCase(),
						RS.getString("pass"),
						RS.getString("pseudo"),
						RS.getString("question"),
						RS.getString("reponse"),
						RS.getInt("level"),
						RS.getInt("vip"),
						(RS.getInt("banned") == 1),
						RS.getString("lastIP"),
						RS.getString("lastConnectionDate"),
						RS.getString("bank"),
						RS.getInt("bankKamas"),
						RS.getString("friends"),
						RS.getString("enemy")
						);
				World.addAccount(C);
				World.addAccountbyName(C);
				
				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
				
			}
			closePreparedStatement(p);
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static int getNextPersonnageGuid()
	{
		try
		{
			ResultSet RS = executeQuery("SELECT guid FROM personnages ORDER BY guid DESC LIMIT 1;",Ancestra.OTHER_DB_NAME);
			if(!RS.first())return 1;
			int guid = RS.getInt("guid");
			guid++;
			closeResultSet(RS);
			return guid;
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			Ancestra.closeServers();
		}
		return 0;
	}
	public static void LOAD_PERSO_BY_ACCOUNT(int accID)
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM personnages WHERE account = '"+accID+"';", Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				TreeMap<Integer,Integer> stats = new TreeMap<Integer,Integer>();
				stats.put(Constants.STATS_ADD_VITA, RS.getInt("vitalite"));
				stats.put(Constants.STATS_ADD_FORC, RS.getInt("force"));
				stats.put(Constants.STATS_ADD_SAGE, RS.getInt("sagesse"));
				stats.put(Constants.STATS_ADD_INTE, RS.getInt("intelligence"));
				stats.put(Constants.STATS_ADD_CHAN, RS.getInt("chance"));
				stats.put(Constants.STATS_ADD_AGIL, RS.getInt("agilite"));
				
				Personnage perso = new Personnage(
						RS.getInt("guid"),
						RS.getString("name"),
						RS.getInt("sexe"),
						RS.getInt("class"),
						RS.getInt("color1"),
						RS.getInt("color2"),
						RS.getInt("color3"),
						RS.getLong("kamas"),
						RS.getInt("spellboost"),
						RS.getInt("capital"),
						RS.getInt("energy"),
						RS.getInt("level"),
						RS.getLong("xp"),
						RS.getInt("size"),
						RS.getInt("gfx"),
						RS.getByte("alignement"),
						RS.getInt("account"),
						stats,
						RS.getByte("seeFriend"),
						RS.getByte("seeAlign"),
						RS.getByte("seeSeller"),
						RS.getString("canaux"),
						RS.getShort("map"),
						RS.getInt("cell"),
						RS.getString("objets"),
						RS.getString("storeObjets"),
						RS.getInt("pdvper"),
						RS.getString("spells"),
						RS.getString("savepos"),
						RS.getString("jobs"),
						RS.getInt("mountxpgive"),
						RS.getInt("mount"),
						RS.getInt("honor"),
						RS.getInt("deshonor"),
						RS.getInt("alvl"),
						RS.getString("zaaps"),
						RS.getByte("title"),
						RS.getInt("wife")
						);
				//Vérifications pré-connexion
				perso.VerifAndChangeItemPlace();
				World.addPersonnage(perso);
				int guildId = isPersoInGuild(RS.getInt("guid"));
				if(guildId >= 0)
				{
					perso.setGuildMember(World.getGuild(guildId).getMember(RS.getInt("guid")));
				}
				if(World.getCompte(accID) != null)
					World.getCompte(accID).addPerso(perso);
			}
			
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			Ancestra.closeServers();
		}
	}
	public static void LOAD_PERSO(int persoID)
	{
				try
				{
					ResultSet RS = SQLManager.executeQuery("SELECT * FROM personnages WHERE guid = '"+persoID+"';", Ancestra.OTHER_DB_NAME);
					int accID;
					while(RS.next())
					{
						TreeMap<Integer,Integer> stats = new TreeMap<Integer,Integer>();
						stats.put(Constants.STATS_ADD_VITA, RS.getInt("vitalite"));
						stats.put(Constants.STATS_ADD_FORC, RS.getInt("force"));
						stats.put(Constants.STATS_ADD_SAGE, RS.getInt("sagesse"));
						stats.put(Constants.STATS_ADD_INTE, RS.getInt("intelligence"));
						stats.put(Constants.STATS_ADD_CHAN, RS.getInt("chance"));
						stats.put(Constants.STATS_ADD_AGIL, RS.getInt("agilite"));
						
						accID = RS.getInt("account");
						
						Personnage perso = new Personnage(
								RS.getInt("guid"),
								RS.getString("name"),
								RS.getInt("sexe"),
								RS.getInt("class"),
								RS.getInt("color1"),
								RS.getInt("color2"),
								RS.getInt("color3"),
								RS.getLong("kamas"),
								RS.getInt("spellboost"),
								RS.getInt("capital"),
								RS.getInt("energy"),
								RS.getInt("level"),
								RS.getLong("xp"),
								RS.getInt("size"),
								RS.getInt("gfx"),
								RS.getByte("alignement"),
								accID,
								stats,
								RS.getByte("seeFriend"),
								RS.getByte("seeAlign"),
								RS.getByte("seeSeller"),
								RS.getString("canaux"),
								RS.getShort("map"),
								RS.getInt("cell"),
								RS.getString("objets"),
								RS.getString("storeObjets"),
								RS.getInt("pdvper"),
								RS.getString("spells"),
								RS.getString("savepos"),
								RS.getString("jobs"),
								RS.getInt("mountxpgive"),
								RS.getInt("mount"),
								RS.getInt("honor"),
								RS.getInt("deshonor"),
								RS.getInt("alvl"),
								RS.getString("zaaps"),
								RS.getByte("title"),
								RS.getInt("wife")
								);
						//Vérifications pré-connexion
						perso.VerifAndChangeItemPlace();
						World.addPersonnage(perso);
						int guildId = isPersoInGuild(RS.getInt("guid"));
						if(guildId >= 0)
						{
							perso.setGuildMember(World.getGuild(guildId).getMember(RS.getInt("guid")));
						}
						if(World.getCompte(accID) != null)
							World.getCompte(accID).addPerso(perso);
					}
					
					closeResultSet(RS);
				}catch(SQLException e)
				{
					RealmServer.addToLog("SQL ERROR: "+e.getMessage());
					e.printStackTrace();
					Ancestra.closeServers();
				}
	}
	public static void LOAD_PERSOS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM personnages;", Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				TreeMap<Integer,Integer> stats = new TreeMap<Integer,Integer>();
				stats.put(Constants.STATS_ADD_VITA, RS.getInt("vitalite"));
				stats.put(Constants.STATS_ADD_FORC, RS.getInt("force"));
				stats.put(Constants.STATS_ADD_SAGE, RS.getInt("sagesse"));
				stats.put(Constants.STATS_ADD_INTE, RS.getInt("intelligence"));
				stats.put(Constants.STATS_ADD_CHAN, RS.getInt("chance"));
				stats.put(Constants.STATS_ADD_AGIL, RS.getInt("agilite"));
				
				Personnage perso = new Personnage(
						RS.getInt("guid"),
						RS.getString("name"),
						RS.getInt("sexe"),
						RS.getInt("class"),
						RS.getInt("color1"),
						RS.getInt("color2"),
						RS.getInt("color3"),
						RS.getLong("kamas"),
						RS.getInt("spellboost"),
						RS.getInt("capital"),
						RS.getInt("energy"),
						RS.getInt("level"),
						RS.getLong("xp"),
						RS.getInt("size"),
						RS.getInt("gfx"),
						RS.getByte("alignement"),
						RS.getInt("account"),
						stats,
						RS.getByte("seeFriend"),
						RS.getByte("seeAlign"),
						RS.getByte("seeSeller"),
						RS.getString("canaux"),
						RS.getShort("map"),
						RS.getInt("cell"),
						RS.getString("objets"),
						RS.getString("storeObjets"),
						RS.getInt("pdvper"),
						RS.getString("spells"),
						RS.getString("savepos"),
						RS.getString("jobs"),
						RS.getInt("mountxpgive"),
						RS.getInt("mount"),
						RS.getInt("honor"),
						RS.getInt("deshonor"),
						RS.getInt("alvl"),
						RS.getString("zaaps"),
						RS.getByte("title"),
						RS.getInt("wife")
						);
				//Vérifications pré-connexion
				perso.VerifAndChangeItemPlace();
				World.addPersonnage(perso);
				if(World.getCompte(RS.getInt("account")) != null)
					World.getCompte(RS.getInt("account")).addPerso(perso);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			Ancestra.closeServers();
		}
	}
	public static boolean DELETE_PERSO_IN_BDD(Personnage perso)
	{
		int guid = perso.get_GUID();
		String baseQuery = "DELETE FROM personnages WHERE guid = ?;";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, guid);
			
			p.execute();
			
			if(!perso.getItemsIDSplitByChar(",").equals(""))
			{
				baseQuery = "DELETE FROM items WHERE guid IN (?);";
				p = newTransact(baseQuery, othCon);
				p.setString(1, perso.getItemsIDSplitByChar(","));
				
				p.execute();
			}
			if(!perso.getStoreItemsIDSplitByChar(",").equals(""))
			{
				baseQuery = "DELETE FROM items WHERE guid IN (?);";
				p = newTransact(baseQuery, othCon);
				p.setString(1, perso.getStoreItemsIDSplitByChar(","));
				
				p.execute();
			}
			if(perso.getMount() != null)
			{
				baseQuery = "DELETE FROM mounts_data WHERE id = ?";
				p = newTransact(baseQuery, othCon);
				p.setInt(1, perso.getMount().get_id());
				
				p.execute();
				World.delDragoByID(perso.getMount().get_id());
			}
			
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
			GameServer.addToLog("Game: Supression du personnage echouee");
			return false;
		}
	}
	public static boolean ADD_PERSO_IN_BDD(Personnage perso)
	{
		String baseQuery = "INSERT INTO personnages( `guid` , `name` , `sexe` , `class` , `color1` , `color2` , `color3` , `kamas` , `spellboost` , `capital` , `energy` , `level` , `xp` , `size` , `gfx` , `account`,`cell`,`map`,`spells`,`objets`, `storeObjets`)" +
				" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'', '');";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			p.setInt(1,perso.get_GUID());
			p.setString(2, perso.get_name());
			p.setInt(3,perso.get_sexe());
			p.setInt(4,perso.get_classe());
			p.setInt(5,perso.get_color1());
			p.setInt(6,perso.get_color2());
			p.setInt(7,perso.get_color3());
			p.setLong(8,perso.get_kamas());
			p.setInt(9,perso.get_spellPts());
			p.setInt(10,perso.get_capital());
			p.setInt(11,perso.get_energy());
			p.setInt(12,perso.get_lvl());
			p.setLong(13,perso.get_curExp());
			p.setInt(14,perso.get_size());
			p.setInt(15,perso.get_gfxID());
			p.setInt(16,perso.getAccID());
			p.setInt(17,perso.get_curCell().getID());
			p.setInt(18,perso.get_curCarte().get_id());
			p.setString(19, perso.parseSpellToDB());
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
			GameServer.addToLog("Game: Creation du personnage echouee");
			return false;
		}
	}
	public static void LOAD_EXP()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from experience;",Ancestra.STATIC_DB_NAME);
			while(RS.next())World.addExpLevel(RS.getInt("lvl"),new World.ExpLevel(RS.getLong("perso"),RS.getInt("metier"),RS.getInt("dinde"),RS.getInt("pvp")));
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	public static int LOAD_TRIGGERS()
	{
		try
		{
			int nbr = 0;
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM `scripted_cells`",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				if(World.getCarte(RS.getShort("MapID")) == null) continue;
				if(World.getCarte(RS.getShort("MapID")).getCase(RS.getInt("CellID")) == null) continue;
				
				switch(RS.getInt("EventID"))
				{
					case 1://Stop sur la case(triggers)
						World.getCarte(RS.getShort("MapID")).getCase(RS.getInt("CellID")).addOnCellStopAction(RS.getInt("ActionID"), RS.getString("ActionsArgs"), RS.getString("Conditions"));	
					break;
						
					default:
						GameServer.addToLog("Action Event "+RS.getInt("EventID")+" non implante");
					break;
				}
				nbr++;
			}
			closeResultSet(RS);
			return nbr;
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
		return 0;
	}
	public static void LOAD_MAPS()
	{
		try
		{
			ResultSet RS;
			RS = SQLManager.executeQuery("SELECT  * from maps LIMIT "+Constants.DEBUG_MAP_LIMIT+";",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
					World.addCarte(
							new Carte(
							RS.getShort("id"),
							RS.getString("date"),
							RS.getByte("width"),
							RS.getByte("heigth"),
							RS.getString("key"),
							RS.getString("places"),
							RS.getString("mapData"),
							RS.getString("cells"),
							RS.getString("monsters"),
							RS.getString("mappos"),
							RS.getByte("numgroup"),
							RS.getByte("groupmaxsize")
							));
			}
			SQLManager.closeResultSet(RS);
			RS = SQLManager.executeQuery("SELECT  * from mobgroups_fix;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
					Carte c = World.getCarte(RS.getShort("mapid"));
					if(c == null)continue;
					if(c.getCase(RS.getInt("cellid")) == null)continue;
					c.addStaticGroup(RS.getInt("cellid"), RS.getString("groupData"));
			}
			SQLManager.closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	public static void SAVE_PERSONNAGE(Personnage _perso, boolean saveItem)
	{
		String baseQuery = "UPDATE `personnages` SET "+
						"`kamas`= ?,"+
						"`spellboost`= ?,"+
						"`capital`= ?,"+
						"`energy`= ?,"+
						"`level`= ?,"+
						"`xp`= ?,"+
						"`size` = ?," +
						"`gfx`= ?,"+
						"`alignement`= ?,"+
						"`honor`= ?,"+
						"`deshonor`= ?,"+
						"`alvl`= ?,"+
						"`vitalite`= ?,"+
						"`force`= ?,"+
						"`sagesse`= ?,"+
						"`intelligence`= ?,"+
						"`chance`= ?,"+
						"`agilite`= ?,"+
						"`seeSpell`= ?,"+
						"`seeFriend`= ?,"+
						"`seeAlign`= ?,"+
						"`seeSeller`= ?,"+
						"`canaux`= ?,"+
						"`map`= ?,"+
						"`cell`= ?,"+
						"`pdvper`= ?,"+
						"`spells`= ?," +
						"`objets`= ?,"+
						"`storeObjets`= ?,"+
						"`savepos`= ?,"+
						"`zaaps`= ?,"+
						"`jobs`= ?,"+
						"`mountxpgive`= ?,"+
						"`mount`= ?,"+
						"`title`= ?,"+
						"`wife`= ?"+
						" WHERE `personnages`.`guid` = ? LIMIT 1 ;";
		
		PreparedStatement p = null;
		
		try
		{
			p = newTransact(baseQuery, othCon);
			
			p.setLong(1,_perso.get_kamas());
			p.setInt(2,_perso.get_spellPts());
			p.setInt(3,_perso.get_capital());
			p.setInt(4,_perso.get_energy());
			p.setInt(5,_perso.get_lvl());
			p.setLong(6,_perso.get_curExp());
			p.setInt(7,_perso.get_size());
			p.setInt(8,_perso.get_gfxID());
			p.setInt(9,_perso.get_align());
			p.setInt(10,_perso.get_honor());
			p.setInt(11,_perso.getDeshonor());
			p.setInt(12,_perso.getALvl());
			p.setInt(13,_perso.get_baseStats().getEffect(Constants.STATS_ADD_VITA));
			p.setInt(14,_perso.get_baseStats().getEffect(Constants.STATS_ADD_FORC));
			p.setInt(15,_perso.get_baseStats().getEffect(Constants.STATS_ADD_SAGE));
			p.setInt(16,_perso.get_baseStats().getEffect(Constants.STATS_ADD_INTE));
			p.setInt(17,_perso.get_baseStats().getEffect(Constants.STATS_ADD_CHAN));
			p.setInt(18,_perso.get_baseStats().getEffect(Constants.STATS_ADD_AGIL));
			p.setInt(19,(_perso.is_showSpells()?1:0));
			p.setInt(20,(_perso.is_showFriendConnection()?1:0));
			p.setInt(21,(_perso.is_showWings()?1:0));
			p.setInt(22,(_perso.is_showSeller()?1:0));
			p.setString(23,_perso.get_canaux());
			p.setInt(24,_perso.get_curCarte().get_id());
			p.setInt(25,_perso.get_curCell().getID());
			p.setInt(26,_perso.get_pdvper());
			p.setString(27,_perso.parseSpellToDB());
			p.setString(28,_perso.parseObjetsToDB());
			p.setString(29, _perso.parseStoreItemstoBD());
			p.setString(30,_perso.get_savePos());
			p.setString(31,_perso.parseZaaps());
			p.setString(32,_perso.parseJobData());
			p.setInt(33,_perso.getMountXpGive());
			p.setInt(34, (_perso.getMount()!=null?_perso.getMount().get_id():-1));
			p.setByte(35,(_perso.get_title()));
			p.setInt(36,_perso.getWife());
			p.setInt(37,_perso.get_GUID());
			
			p.executeUpdate();
			
			if(_perso.getGuildMember() != null)
				UPDATE_GUILDMEMBER(_perso.getGuildMember());
			if(_perso.getMount() != null)
				UPDATE_MOUNT_INFOS(_perso.getMount());
			GameServer.addToLog("Personnage "+_perso.get_name()+" sauvegarde");
		}catch(Exception e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.out.println("Requete: "+baseQuery);
			System.out.println("Le personnage n'a pas ete sauvegarde");
			System.exit(1);
		};
		if(saveItem)
		{
			baseQuery = "UPDATE `items` SET qua = ?, pos= ?, stats = ?"+
			" WHERE guid = ?;";
			try {
				p = newTransact(baseQuery, othCon);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			for(String idStr : _perso.getItemsIDSplitByChar(":").split(":"))
			{
				try
				{
					int guid = Integer.parseInt(idStr);
					Objet obj = World.getObjet(guid);
					if(obj == null)continue;
					
					p.setInt(1, obj.getQuantity());
					p.setInt(2, obj.getPosition());
					p.setString(3, obj.parseToSave());
					p.setInt(4, Integer.parseInt(idStr));
					
					p.execute();
				}catch(Exception e){continue;};
				
			}
			
			if(_perso.get_compte() == null)
				return;
			for(String idStr : _perso.getBankItemsIDSplitByChar(":").split(":"))
			{
				try
				{
					int guid = Integer.parseInt(idStr);
					Objet obj = World.getObjet(guid);
					if(obj == null)continue;
					
					p.setInt(1, obj.getQuantity());
					p.setInt(2, obj.getPosition());
					p.setString(3, obj.parseToSave());
					p.setInt(4, Integer.parseInt(idStr));
					
					p.execute();
				}catch(Exception e){continue;};
				
			}
		}
		
		closePreparedStatement(p);
	}
	public static void LOAD_SORTS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT  * from sorts;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("id");
				Sort sort = new Sort(id,RS.getInt("sprite"),RS.getString("spriteInfos"),RS.getString("effectTarget"));
				SortStats l1 = parseSortStats(id,1,RS.getString("lvl1"));
				SortStats l2 = parseSortStats(id,2,RS.getString("lvl2"));
				SortStats l3 = parseSortStats(id,3,RS.getString("lvl3"));
				SortStats l4 = parseSortStats(id,4,RS.getString("lvl4"));
				SortStats l5 = null;
				if(!RS.getString("lvl5").equalsIgnoreCase("-1"))
					l5 = parseSortStats(id,5,RS.getString("lvl5"));
				SortStats l6 = null;
				if(!RS.getString("lvl6").equalsIgnoreCase("-1"))
						l6 = parseSortStats(id,6,RS.getString("lvl6"));
				sort.addSortStats(1,l1);
				sort.addSortStats(2,l2);
				sort.addSortStats(3,l3);
				sort.addSortStats(4,l4);
				sort.addSortStats(5,l5);
				sort.addSortStats(6,l6);
				World.addSort(sort);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	public static void LOAD_OBJ_TEMPLATE()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT  * from item_template;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
					World.addObjTemplate
					(
						new ObjTemplate
						(
							RS.getInt("id"),
							RS.getString("statsTemplate"),
							RS.getString("name"),
							RS.getInt("type"),
							RS.getInt("level"),
							RS.getInt("pod"),
							RS.getInt("prix"),
							RS.getInt("panoplie"),
							RS.getString("condition"),
							RS.getString("armesInfos"),
							RS.getInt("sold"),
							RS.getInt("avgPrice")
						)
					);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	private static SortStats parseSortStats(int id,int lvl,String str)
	{
		try
		{
			SortStats stats = null;
			String[] stat = str.split(",");
			String effets = stat[0];
			String CCeffets = stat[1];
			int PACOST = 6;
			try
			{
				PACOST = Integer.parseInt(stat[2].trim());
			}catch(NumberFormatException e){};
			
			int POm = Integer.parseInt(stat[3].trim());
			int POM = Integer.parseInt(stat[4].trim());
			int TCC = Integer.parseInt(stat[5].trim());
			int TEC = Integer.parseInt(stat[6].trim());
			boolean line = stat[7].trim().equalsIgnoreCase("true");
			boolean LDV = stat[8].trim().equalsIgnoreCase("true");
			boolean emptyCell = stat[9].trim().equalsIgnoreCase("true");
			boolean MODPO = stat[10].trim().equalsIgnoreCase("true");
			//int unk = Integer.parseInt(stat[11]);//All 0
			int MaxByTurn = Integer.parseInt(stat[12].trim());
			int MaxByTarget = Integer.parseInt(stat[13].trim());
			int CoolDown = Integer.parseInt(stat[14].trim());
			String type = stat[15].trim();
			int level = Integer.parseInt(stat[stat.length-2].trim());
			boolean endTurn = stat[19].trim().equalsIgnoreCase("true");
			stats = new SortStats(id,lvl,PACOST,POm, POM, TCC, TEC, line, LDV, emptyCell, MODPO, MaxByTurn, MaxByTarget, CoolDown, level, endTurn, effets, CCeffets,type);
			return stats;
		}catch(Exception e)
		{
			e.printStackTrace();
			int nbr = 0;
			System.out.println("[DEBUG]Sort "+id+" lvl "+lvl);
			for(String z:str.split(","))
			{
				System.out.println("[DEBUG]"+nbr+" "+z);
				nbr++;
			}
			System.exit(1);
			return null;
		}
	}
	public static void LOAD_MOB_TEMPLATE() {
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM monsters;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("id");
				int gfxID = RS.getInt("gfxID");
				int align = RS.getInt("align");
				String colors = RS.getString("colors");
				String grades = RS.getString("grades");
				String spells = RS.getString("spells");
				String stats = RS.getString("stats");
				String pdvs = RS.getString("pdvs");
				String pts = RS.getString("points");
				String inits = RS.getString("inits");
				int mK = RS.getInt("minKamas");
				int MK = RS.getInt("maxKamas");
				int IAType = RS.getInt("AI_Type");
				String xp = RS.getString("exps");
				boolean capturable;
				if(RS.getInt("capturable") == 1)
				{
					capturable = true;
				}else
				{
					capturable = false;
				}
				
				World.addMobTemplate
				(
					id,
					new Monstre
					(
						id,
						gfxID,
						align,
						colors,
						grades,
						spells,
						stats,
						pdvs,
						pts,
						inits,
						mK,
						MK,
						xp,
						IAType,
						capturable
					)
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	public static void LOAD_NPC_TEMPLATE()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_template;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("id");
				int bonusValue = RS.getInt("bonusValue");
				int gfxID = RS.getInt("gfxID");
				int scaleX = RS.getInt("scaleX");
				int scaleY = RS.getInt("scaleY");
				int sex = RS.getInt("sex");
				int color1 = RS.getInt("color1");
				int color2 = RS.getInt("color2");
				int color3 = RS.getInt("color3");
				String access = RS.getString("accessories");
				int extraClip = RS.getInt("extraClip");
				int customArtWork = RS.getInt("customArtWork");
				int initQId = RS.getInt("initQuestion");
				String ventes = RS.getString("ventes");
				World.addNpcTemplate
				(
					new NPC_tmpl
					(
						id,
						bonusValue,
						gfxID,
						scaleX,
						scaleY,
						sex,
						color1,
						color2,
						color3,
						access,
						extraClip,
						customArtWork,
						initQId,
						ventes
					)
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	public static void SAVE_NEW_ITEM(Objet item)
	{
		try {
		String baseQuery = "REPLACE INTO `items` VALUES(?,?,?,?,?);";
		
		PreparedStatement p = newTransact(baseQuery, othCon);
		
		p.setInt(1,item.getGuid());
		p.setInt(2,item.getTemplate().getID());
		p.setInt(3,item.getQuantity());
		p.setInt(4,item.getPosition());
		p.setString(5,item.parseToSave());
		
		p.execute();
		closePreparedStatement(p);
		} catch (SQLException e) {e.printStackTrace();}
	}
	public static boolean SAVE_NEW_FIXGROUP(int mapID,int cellID,String groupData)
	{
		try {
		String baseQuery = "REPLACE INTO `mobgroups_fix` VALUES(?,?,?)";
		PreparedStatement p = newTransact(baseQuery, statCon);
		
		p.setInt(1, mapID);
		p.setInt(2, cellID);
		p.setString(3, groupData);
		
		p.execute();
		closePreparedStatement(p);
		
		return true;
		} catch (SQLException e) {e.printStackTrace();}
		return false;
	}
	public static void LOAD_NPC_QUESTIONS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_questions;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				World.addNPCQuestion
				(
					new NPC_question
					(
						RS.getInt("ID"),
						RS.getString("responses"),
						RS.getString("params"),
						RS.getString("cond"),
						RS.getInt("ifFalse")
					)
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	public static void LOAD_NPC_ANSWERS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_reponses_actions;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("ID");
				int type = RS.getInt("type");
				String args = RS.getString("args");
				if(World.getNPCreponse(id) == null)
					World.addNPCreponse(new NPC_reponse(id));
				World.getNPCreponse(id).addAction(new Action(type,args,""));
				
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	public static int LOAD_ENDFIGHT_ACTIONS()
	{
		int nbr = 0;
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM endfight_action;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				Carte map = World.getCarte(RS.getShort("map"));
				if(map == null)continue;
				map.addEndFightAction(RS.getInt("fighttype"),
						new Action(RS.getInt("action"),RS.getString("args"),RS.getString("cond")));
				nbr++;
			}
			closeResultSet(RS);
			return nbr;
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
		return nbr;
	}
	public static int LOAD_ITEM_ACTIONS()
	{
		int nbr = 0;
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM use_item_actions;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("template");
				int type = RS.getInt("type");
				String args = RS.getString("args");
				if(World.getObjTemplate(id) == null)continue;
				World.getObjTemplate(id).addAction(new Action(type,args,""));
				nbr++;
			}
			closeResultSet(RS);
			return nbr;
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
		return nbr;
	}
	public static void LOAD_ITEMS(String ids)
	{
		String req = "SELECT * FROM items WHERE guid IN ("+ids+");";
		try
		{
			ResultSet RS = SQLManager.executeQuery(req,Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				int guid 	= RS.getInt("guid");
				int tempID 	= RS.getInt("template");
				int qua 	= RS.getInt("qua");
				int pos		= RS.getInt("pos");
				String stats= RS.getString("stats");
				World.addObjet
				(
					World.newObjet
					(
						guid,
						tempID,
						qua,
						pos,
						stats
					),
					false
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.out.println("Requete: \n"+req);
			System.exit(1);
		}
	}
	public static void DELETE_ITEM(int guid)
	{
		String baseQuery = "DELETE FROM items WHERE guid = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, guid);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void SAVE_ITEM(Objet item)
	{
		String baseQuery = "REPLACE INTO `items` VALUES (?,?,?,?,?);";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, item.getGuid());
			p.setInt(2, item.getTemplate().getID());
			p.setInt(3, item.getQuantity());
			p.setInt(4, item.getPosition());
			p.setString(5,item.parseToSave());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}	
	}
	public static void CREATE_MOUNT(Dragodinde DD)
	{
		String baseQuery = "REPLACE INTO `mounts_data`(`id`,`color`,`sexe`,`name`,`xp`,`level`," +
				"`endurance`,`amour`,`maturite`,`serenite`,`reproductions`,`fatigue`,`items`," +
				"`ancetres`,`energie`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1,DD.get_id());
			p.setInt(2,DD.get_color());
			p.setInt(3,DD.get_sexe());
			p.setString(4,DD.get_nom());
			p.setLong(5,DD.get_exp());
			p.setInt(6,DD.get_level());
			p.setInt(7,DD.get_endurance());
			p.setInt(8,DD.get_amour());
			p.setInt(9,DD.get_maturite());
			p.setInt(10,DD.get_serenite());
			p.setInt(11,DD.get_reprod());
			p.setInt(12,DD.get_fatigue());
			p.setString(13,DD.getItemsId());
			p.setString(14,DD.get_ancetres());
			p.setInt(15,DD.get_energie());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void REMOVE_MOUNT(int DID)
	{
		String baseQuery = "DELETE FROM `mounts_data` WHERE `id` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, DID);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void LOAD_ACCOUNT_BY_GUID(int user)
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts WHERE `guid` = '"+user+"';",Ancestra.OTHER_DB_NAME);
			
			String baseQuery = "UPDATE accounts " +
								"SET `reload_needed` = 0 " +
								"WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			while(RS.next())
			{
				//Si le compte est déjà connecté, on zap
				if(World.getCompte(RS.getInt("guid")) != null)if(World.getCompte(RS.getInt("guid")).isOnline())continue;
				
				Compte C = new Compte(
						RS.getInt("guid"),
						RS.getString("account").toLowerCase(),
						RS.getString("pass"),
						RS.getString("pseudo"),
						RS.getString("question"),
						RS.getString("reponse"),
						RS.getInt("level"),
						RS.getInt("vip"),
						(RS.getInt("banned") == 1),
						RS.getString("lastIP"),
						RS.getString("lastConnectionDate"),
						RS.getString("bank"),
						RS.getInt("bankKamas"),
						RS.getString("friends"),
						RS.getString("enemy")
						);
				World.addAccount(C);
				World.ReassignAccountToChar(C);
				
				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
			}
			
			closePreparedStatement(p);
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_ACCOUNT_BY_USER(String user)
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts WHERE `account` LIKE '"+user+"';",Ancestra.OTHER_DB_NAME);
			
			String baseQuery = "UPDATE accounts " +
								"SET `reload_needed` = 0 " +
								"WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			while(RS.next())
			{
				//Si le compte est déjà connecté, on zap
				if(World.getCompte(RS.getInt("guid")) != null)
					if(World.getCompte(RS.getInt("guid")).isOnline())
						continue;
				
				Compte C = new Compte(
						RS.getInt("guid"),
						RS.getString("account").toLowerCase(),
						RS.getString("pass"),
						RS.getString("pseudo"),
						RS.getString("question"),
						RS.getString("reponse"),
						RS.getInt("level"),
						RS.getInt("vip"),
						(RS.getInt("banned") == 1),
						RS.getString("lastIP"),
						RS.getString("lastConnectionDate"),
						RS.getString("bank"),
						RS.getInt("bankKamas"),
						RS.getString("friends"),
						RS.getString("enemy")
						);
				World.addAccount(C);
				World.ReassignAccountToChar(C);
				
				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
			}
			
			closePreparedStatement(p);
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void UPDATE_LASTCONNECTION_INFO(Compte compte)
	{
		String baseQuery = "UPDATE accounts SET " +
		"`lastIP` = ?," +
		"`lastConnectionDate` = ?" +
		" WHERE `guid` = ?;";
		
		try
		{
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			p.setString(1, compte.get_curIP());
			p.setString(2, compte.getLastConnectionDate());
			p.setInt(3, compte.get_GUID());
			
			p.executeUpdate();
			closePreparedStatement(p);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			RealmServer.addToLog("Query: "+baseQuery);
			e.printStackTrace();
		}
	}
	public static void UPDATE_MOUNT_INFOS(Dragodinde DD)
	{
		String baseQuery = "UPDATE mounts_data SET " +
		"`name` = ?," +
		"`xp` = ?," +
		"`level` = ?," +
		"`endurance` = ?," +
		"`amour` = ?," +
		"`maturite` = ?," +
		"`serenite` = ?," +
		"`reproductions` = ?," +
		"`fatigue` = ?," +
		"`energie` = ?," +
		"`ancetres` = ?," +
		"`items` = ?" +
		" WHERE `id` = ?;";
		
		try
		{
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setString(1,DD.get_nom());
			p.setLong(2,DD.get_exp());
			p.setInt(3,DD.get_level());
			p.setInt(4,DD.get_endurance());
			p.setInt(5,DD.get_amour());
			p.setInt(6,DD.get_maturite());
			p.setInt(7,DD.get_serenite());
			p.setInt(8,DD.get_reprod());
			p.setInt(9,DD.get_fatigue());
			p.setInt(10,DD.get_energie());
			p.setString(11,DD.get_ancetres());
			p.setString(12,DD.getItemsId());
			p.setInt(13,DD.get_id());
			
			p.execute();
			closePreparedStatement(p);

		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Query: "+baseQuery);
			e.printStackTrace();
		}
	}
	public static void SAVE_MOUNTPARK(MountPark MP)
	{
		String baseQuery = "REPLACE INTO `mountpark_data`( `mapid` , `cellid`, `size` , `owner` , `guild` , `price` , `data` )" +
				" VALUES (?,?,?,?,?,?,?);";
				
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1,MP.get_map().get_id());
			p.setInt(2,MP.get_cellid());
			p.setInt(3,MP.get_size());
			p.setInt(4,MP.get_owner());
			p.setInt(5,(MP.get_guild()==null?-1:MP.get_guild().get_id()));
			p.setInt(6,MP.get_price());
			p.setString(7,MP.parseDBData());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void UPDATE_MOUNTPARK(MountPark MP)
	{
		String baseQuery = "UPDATE `mountpark_data` SET "+
		"`data` = ?"+
		" WHERE mapid = ?;";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setString(1, MP.parseDBData());
			p.setShort(2, MP.get_map().get_id());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static boolean SAVE_TRIGGER(int mapID1, int cellID1, int action, int event,String args, String cond)
	{
		String baseQuery = "REPLACE INTO `scripted_cells`" +
				" VALUES (?,?,?,?,?,?);";
		
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1,mapID1);
			p.setInt(2,cellID1);
			p.setInt(3,action);
			p.setInt(4,event);
			p.setString(5,args);
			p.setString(6,cond);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean REMOVE_TRIGGER(int mapID, int cellID)
	{
		String baseQuery = "DELETE FROM `scripted_cells` WHERE "+
							"`MapID` = ? AND "+
							"`CellID` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, mapID);
			p.setInt(2, cellID);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean SAVE_MAP_DATA(Carte map)
	{
		String baseQuery = "UPDATE `maps` SET "+
		"`places` = ?, "+
		"`numgroup` = ? "+
		"WHERE id = ?;";
		
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setString(1,map.get_placesStr());
			p.setInt(2, map.getMaxGroupNumb());
			p.setInt(3, map.get_id());
			
			p.executeUpdate();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean DELETE_NPC_ON_MAP(int m,int c)
	{
		String baseQuery = "DELETE FROM npcs WHERE mapid = ? AND cellid = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, m);
			p.setInt(2, c);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean DELETE_PERCO(int id)
	{
		String baseQuery = "DELETE FROM percepteurs WHERE guid = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, id);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean ADD_NPC_ON_MAP(int m,int id,int c,int o)
	{
		
		String baseQuery = "INSERT INTO `npcs`" +
				" VALUES (?,?,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, m);
			p.setInt(2, id);
			p.setInt(3, c);
			p.setInt(4, o);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean ADD_PERCO_ON_MAP(int guid,int mapid, int guildID, int cellid,int o, short N1, short N2)
	{
		String baseQuery = "INSERT INTO `percepteurs`" +
				" VALUES (?,?,?,?,?,?,?,?,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, guid);
			p.setInt(2, mapid);
			p.setInt(3, cellid);
			p.setInt(4, o);
			p.setInt(5, guildID);
			p.setShort(6, N1);
			p.setShort(7, N2);
			p.setString(8, "");
			p.setLong(9, 0);
			p.setLong(10, 0);
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static void UPDATE_PERCO(Percepteur P)
	{
		String baseQuery = "UPDATE `percepteurs` SET "+
		"`objets` = ?,"+
		"`kamas` = ?," +
		"`xp` = ?" +
		" WHERE guid = ?;";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setString(1, P.parseItemPercepteur());
			p.setLong(2, P.getKamas());
			p.setLong(3, P.getXp());
			p.setInt(4, P.getGuid());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static boolean ADD_ENDFIGHTACTION(int mapID, int type, int Aid,String args,String cond)
	{
		if(!DEL_ENDFIGHTACTION(mapID,type,Aid))return false;
		String baseQuery = "INSERT INTO `endfight_action` " +
				"VALUES (?,?,?,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, mapID);
			p.setInt(2, type);
			p.setInt(3, Aid);
			p.setString(4,args);
			p.setString(5, cond);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}

	public static boolean DEL_ENDFIGHTACTION(int mapID, int type, int aid)
	{
		String baseQuery = "DELETE FROM `endfight_action` " +
				"WHERE map = ? AND " +
				"fighttype = ? AND " +
				"action = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, mapID);
			p.setInt(2, type);
			p.setInt(3, aid);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
			return false;
		}
	}
	public static void SAVE_NEWGUILD(Guild g)
	{
		String baseQuery = "INSERT INTO `guilds` " +
				"VALUES (?,?,?,1,0,0,0,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, g.get_id());
			p.setString(2, g.get_name());
			p.setString(3, g.get_emblem());
			p.setString(4, "462;0|461;0|460;0|459;0|458;0|457;0|456;0|455;0|454;0|453;0|452;0|451;0|");
			p.setString(5, "176;100|158;1000|124;100|");
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void DEL_GUILD(int id)
	{
		String baseQuery = "DELETE FROM `guilds` " +
				"WHERE `id` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, id);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void DEL_ALL_GUILDMEMBER(int guildid)
	{
		String baseQuery = "DELETE FROM `guild_members` " +
				"WHERE `guild` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, guildid);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void DEL_GUILDMEMBER(int id)
	{
		String baseQuery = "DELETE FROM `guild_members` " +
				"WHERE `guid` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, id);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void UPDATE_GUILD(Guild g)
	{
		String baseQuery = "UPDATE `guilds` SET "+
		"`lvl` = ?,"+
		"`xp` = ?," +
		"`capital` = ?," +
		"`nbrmax` = ?," +
		"`sorts` = ?," +
		"`stats` = ?" +
		" WHERE id = ?;";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, g.get_lvl());
			p.setLong(2, g.get_xp());
			p.setInt(3, g.get_Capital());
			p.setInt(4, g.get_nbrPerco());
			p.setString(5, g.compileSpell());
			p.setString(6, g.compileStats());
			p.setInt(7, g.get_id());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void UPDATE_GUILDMEMBER(GuildMember gm)
	{
		String baseQuery = "REPLACE INTO `guild_members` " +
						"VALUES(?,?,?,?,?,?,?,?,?,?,?);";
						
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1,gm.getGuid());
			p.setInt(2,gm.getGuild().get_id());
			p.setString(3,gm.getName());
			p.setInt(4,gm.getLvl());
			p.setInt(5,gm.getGfx());
			p.setInt(6,gm.getRank());
			p.setLong(7,gm.getXpGave());
			p.setInt(8,gm.getPXpGive());
			p.setInt(9,gm.getRights());
			p.setInt(10,gm.getAlign());
			p.setString(11,gm.getLastCo());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static int isPersoInGuild(int guid)
	{
		int guildId = -1;
		
		try
		{
			ResultSet GuildQuery = SQLManager.executeQuery("SELECT guild FROM `guild_members` WHERE guid="+guid+";", Ancestra.OTHER_DB_NAME);
			
			boolean found = GuildQuery.first();
			
			if(found)
				guildId = GuildQuery.getInt("guild");
			
			closeResultSet(GuildQuery);
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
		
		return guildId;
	}
	public static int[] isPersoInGuild(String name)
	{
		int guildId = -1;
		int guid = -1;
		try
		{
			ResultSet GuildQuery = SQLManager.executeQuery("SELECT guild,guid FROM `guild_members` WHERE name='"+name+"';", Ancestra.OTHER_DB_NAME);
			boolean found = GuildQuery.first();
			
			if(found)
			{
				guildId = GuildQuery.getInt("guild");
				guid = GuildQuery.getInt("guid");
			}
			
			closeResultSet(GuildQuery);
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
		int[] toReturn = {guid,guildId};
		return toReturn;
	}
	public static boolean ADD_REPONSEACTION(int repID, int type, String args)
	{
		String baseQuery = "DELETE FROM `npc_reponses_actions` " +
						"WHERE `ID` = ? AND " +
						"`type` = ?;";
		PreparedStatement p; 
		try {
			p = newTransact(baseQuery, statCon);
			p.setInt(1, repID);
			p.setInt(2, type);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		baseQuery = "INSERT INTO `npc_reponses_actions` " +
				"VALUES (?,?,?);";
		try {
			p = newTransact(baseQuery, statCon);
			p.setInt(1, repID);
			p.setInt(2, type);
			p.setString(3, args);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean UPDATE_INITQUESTION(int id, int q)
	{
		String baseQuery = "UPDATE `npc_template` SET " +
							"`initQuestion` = ? " +
							"WHERE `id` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, q);
			p.setInt(2, id);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean UPDATE_NPCREPONSES(int id, String reps)
	{
		String baseQuery = "UPDATE `npc_questions` SET " +
							"`responses` = ? " +
							"WHERE `ID` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setString(1, reps);
			p.setInt(2, id);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static void LOAD_ACTION()
	{
			/*Variables représentant les champs de la base*/
			Personnage perso;
			int action;
			int nombre;
			int id;
			Ancestra.addToShopLog("Lancement de l'application des Lives Actions ...");
			String sortie;
			String couleur = "DF0101"; //La couleur du message envoyer a l'utilisateur (couleur en code HTML)
			ObjTemplate t;
			Objet obj;
			PreparedStatement p;
			/*FIN*/
			try
			{
				ResultSet RS = executeQuery("SELECT * from live_action;",Ancestra.OTHER_DB_NAME);

				while(RS.next())
				{
					perso = World.getPersonnage(RS.getInt("PlayerID"));
					if(perso == null)
					{
						Ancestra.addToShopLog("Personnage "+RS.getInt("PlayerID")+" non trouve, personnage non charge ?");
						continue;
					}
					if(!perso.isOnline())
					{
						Ancestra.addToShopLog("Personnage "+RS.getInt("PlayerID")+" hors ligne");
						continue;
					}
					if(perso.get_compte() == null)
					{
						Ancestra.addToShopLog("Le Personnage "+RS.getInt("PlayerID")+" n'est attribue a aucun compte charge");
						continue;
					}
					if(perso.get_compte().getGameThread() == null)
					{
						Ancestra.addToShopLog("Le Personnage "+RS.getInt("PlayerID")+" n'a pas thread associe, le personnage est il hors ligne ?");
						continue;
					}
					if(perso.get_fight() != null) continue; // Perso en combat  @ Nami-Doc
					action = RS.getInt("Action");
					nombre = RS.getInt("Nombre");
					id = RS.getInt("ID");
					sortie = "+";
						
					switch (action)
					{
						case 1:	//Monter d'un level
							if(perso.get_lvl()==World.getExpLevelSize())continue;
							for(int n = nombre;n>1;n--)perso.levelUp(false,true);
							perso.levelUp(true,true);
							sortie+= nombre+" Niveau(x)";
							break;
						case 2:	//Ajouter X point d'experience
							if(perso.get_lvl()==World.getExpLevelSize())continue;
							perso.addXp(nombre);
							sortie+=nombre+" Xp";
							break;
						case 3:	//Ajouter X kamas
							perso.addKamas(nombre);
							sortie+=nombre+" Kamas";
							break;
						case 4:	//Ajouter X point de capital
							perso.addCapital(nombre);
							sortie+=nombre+" Point(s) de capital";
							break;
						case 5:	//Ajouter X point de sort
							perso.addSpellPoint(nombre);
							sortie+=nombre+" Point(s) de sort";
							break;
						case 20:	//Ajouter un item avec des jets aléatoire
							t = World.getObjTemplate(nombre);
							if(t == null)continue;
							obj = t.createNewItem(1,false); //Si mis à "true" l'objet à des jets max. Sinon ce sont des jets aléatoire
							if(obj == null)continue;
							if(perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
								World.addObjet(obj,true);
							GameServer.addToSockLog("Objet "+nombre+" ajouter a "+perso.get_name()+" avec des stats aleatoire");
							SocketManager.GAME_SEND_MESSAGE(perso,"L'objet \""+t.getName()+"\" viens d'etre ajouter a votre personnage",couleur);
							break;
						case 21:	//Ajouter un item avec des jets MAX
							t = World.getObjTemplate(nombre);
							if(t == null)continue;
							obj = t.createNewItem(1,true); //Si mis à "true" l'objet à des jets max. Sinon ce sont des jets aléatoire
							if(obj == null)continue;
							if(perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
								World.addObjet(obj,true);
							GameServer.addToSockLog("Objet "+nombre+" ajoute a "+perso.get_name()+" avec des stats MAX");
							SocketManager.GAME_SEND_MESSAGE(perso,"L'objet \""+t.getName()+"\" avec des stats maximum, viens d'etre ajoute a votre personnage",couleur);
							break;
						case 118://Force
							perso.get_baseStats().addOneStat(action, nombre);
							SocketManager.GAME_SEND_STATS_PACKET(perso);
							sortie+=nombre+" force";
							break;
						case 119://Agilite
							perso.get_baseStats().addOneStat(action, nombre);
							SocketManager.GAME_SEND_STATS_PACKET(perso);
							sortie+=nombre+" agilite";
							break;
						case 123://Chance
							perso.get_baseStats().addOneStat(action, nombre);
							SocketManager.GAME_SEND_STATS_PACKET(perso);
							sortie+=nombre+" chance";
							break;
						case 124://Sagesse
							perso.get_baseStats().addOneStat(action, nombre);
							SocketManager.GAME_SEND_STATS_PACKET(perso);
							sortie+=nombre+" sagesse";
							break;
						case 125://Vita
							perso.get_baseStats().addOneStat(action, nombre);
							SocketManager.GAME_SEND_STATS_PACKET(perso);
							sortie+=nombre+" vita";
							break;
						case 126://Intelligence
							int statID = action;
							perso.get_baseStats().addOneStat(statID, nombre);
							SocketManager.GAME_SEND_STATS_PACKET(perso);
							sortie+=nombre+" intelligence";
							break;
					}
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					if(action < 20 || action >100) SocketManager.GAME_SEND_MESSAGE(perso,sortie+" a votre personnage",couleur); //Si l'action n'est pas un ajout d'objet on envoye un message a l'utilisateur
					
					Ancestra.addToShopLog("(Commande "+id+")Action "+action+" Nombre: "+nombre+" appliquee sur le personnage "+RS.getInt("PlayerID")+"("+perso.get_name()+")");
				try
				{
					String query = "DELETE FROM live_action WHERE ID="+id+";";
					p = newTransact(query, othCon);
					p.execute();
					closePreparedStatement(p);
					Ancestra.addToShopLog("Commande "+id+" supprimee.");
				}catch(SQLException e)
				{
					GameServer.addToLog("SQL ERROR: "+e.getMessage());
					Ancestra.addToShopLog("Error Delete From: "+e.getMessage());
					e.printStackTrace();
				}
				SQLManager.SAVE_PERSONNAGE(perso,true);
			}
				closeResultSet(RS);
		}catch(Exception e)
		{
			GameServer.addToLog("ERROR: "+e.getMessage());
			Ancestra.addToShopLog("Error: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOG_OUT(int accID, int logged)
	{
		PreparedStatement p;
		String query = "UPDATE `accounts` SET logged=? WHERE `guid`=?;";
		try {
			p = newTransact(query, othCon);
			p.setInt(1, logged);
			p.setInt(2, accID);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+query);
		}
	}
	public static void LOGGED_ZERO() 
	{	
		PreparedStatement p;
		String query = "UPDATE `accounts` SET logged=0;";
		try {
			p = newTransact(query, othCon);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+query);
		}
		
	}
	public static void LOAD_ITEMS_FULL() {
		    try {
		      ResultSet RS = executeQuery("SELECT * FROM items;", Ancestra.OTHER_DB_NAME);
		      while (RS.next()) {
		        int guid = RS.getInt("guid");
		        int tempID = RS.getInt("template");
		        int qua = RS.getInt("qua");
		        int pos = RS.getInt("pos");
		        String stats = RS.getString("stats");
		        World.addObjet(new Objet(guid, tempID, qua, pos, stats), false);
		      }
		      closeResultSet(RS);
		    } catch (SQLException e) {
		      GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
		      System.exit(1);
		    }
	}
	public static void TIMER(boolean start)
	{
			if(start)
			{
				timerCommit = new Timer();
				timerCommit.schedule(new TimerTask() {
					
					public void run() {
						if(!needCommit)return;
						
						commitTransacts();
						needCommit = false;
						
					}
				}, Ancestra.CONFIG_DB_COMMIT, Ancestra.CONFIG_DB_COMMIT);
			}
			else
				timerCommit.cancel();
		}
		public static boolean persoExist(String name)
		{
			boolean exist = false;
			PreparedStatement p;
			String query = "SELECT COUNT(*) AS exist FROM personnages WHERE name LIKE ?;";
			try
			{
				p = newTransact(query, othCon);
				p.setString(1, name);
				ResultSet RS =  p.executeQuery();
				
				boolean found = RS.first();
				
				if(found)
				{
					if(RS.getInt("exist") != 0)
						exist = true;
				}
				
				closeResultSet(RS);
				closePreparedStatement(p);
			}catch(SQLException e)
			{
				RealmServer.addToLog("SQL ERROR: "+e.getMessage());
				e.printStackTrace();
			}
			return exist;
		}
		public static void HOUSE_BUY(Personnage P, House h) 
		{	
			
			PreparedStatement p;
			String query = "UPDATE `houses` SET `sale`='0', `owner_id`=?, `guild_id`='0', `access`='0', `key`='-', `guild_rights`='0' WHERE `id`=?;";
			try {
				p = newTransact(query, othCon);
				p.setInt(1, P.getAccID());
				p.setInt(2, h.get_id());
				
				p.execute();
				closePreparedStatement(p);
				
				h.set_sale(0);
				h.set_owner_id(P.getAccID());
				h.set_guild_id(0);
				h.set_access(0);
				h.set_key("-");
				h.set_guild_rights(0);
			} catch (SQLException e) {
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				GameServer.addToLog("Game: Query: "+query);
			}
			
			ArrayList<Trunk> trunks = Trunk.getTrunksByHouse(h);
			for(Trunk trunk : trunks)
			{
				trunk.set_owner_id(P.getAccID());
				trunk.set_key("-");
			}
			
			query = "UPDATE `coffres` SET `owner_id`=?, `key`='-' WHERE `id_house`=?;";
			try {
				p = newTransact(query, othCon);
				p.setInt(1, P.getAccID());
				p.setInt(2, h.get_id());
				p.execute();
				closePreparedStatement(p);
			} catch (SQLException e) {
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				GameServer.addToLog("Game: Query: "+query);
			}
		}
		public static void HOUSE_SELL(House h, int price) 
		{	
			h.set_sale(price);
			
			PreparedStatement p;
			String query = "UPDATE `houses` SET `sale`=? WHERE `id`=?;";
			try {
				p = newTransact(query, othCon);
				p.setInt(1, price);
				p.setInt(2, h.get_id());
				
				p.execute();
				closePreparedStatement(p);
				
			} catch (SQLException e) {
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				GameServer.addToLog("Game: Query: "+query);
			}
		}
		public static void HOUSE_CODE(Personnage P, House h, String packet) 
		{	
			PreparedStatement p;
			String query = "UPDATE `houses` SET `key`=? WHERE `id`=? AND owner_id=?;";
			try {
				p = newTransact(query, othCon);
				p.setString(1, packet);
				p.setInt(2, h.get_id());
				p.setInt(3, P.getAccID());
				
				p.execute();
				closePreparedStatement(p);
				
				h.set_key(packet);
			} catch (SQLException e) {
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				GameServer.addToLog("Game: Query: "+query);
			}
		}
		public static void HOUSE_GUILD(House h, int GuildID, int GuildRights) 
		{	
			PreparedStatement p;
			String query = "UPDATE `houses` SET `guild_id`=?, `guild_rights`=? WHERE `id`=?;";
			try {
				p = newTransact(query, othCon);
				p.setInt(1, GuildID);
				p.setInt(2, GuildRights);
				p.setInt(3, h.get_id());
				
				p.execute();
				closePreparedStatement(p);
				
				h.set_guild_id(GuildID);
				h.set_guild_rights(GuildRights);
			} catch (SQLException e) {
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				GameServer.addToLog("Game: Query: "+query);
			}
		}
		public static void HOUSE_GUILD_REMOVE(int GuildID) 
		{	
			PreparedStatement p;
			String query = "UPDATE `houses` SET `guild_rights`='0', `guild_id`='0' WHERE `guild_id`=?;";
			try {
				p = newTransact(query, othCon);
				p.setInt(1, GuildID);
				
				p.execute();
				closePreparedStatement(p);
				
			} catch (SQLException e) {
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				GameServer.addToLog("Game: Query: "+query);
			}
		}
		public static void UPDATE_HOUSE(House h)
		{
			String baseQuery = "UPDATE `houses` SET "+
			"`owner_id` = ?,"+
			"`sale` = ?," +
			"`guild_id` = ?," +
			"`access` = ?," +
			"`key` = ?," +
			"`guild_rights` = ?" +
			" WHERE id = ?;";
			
			try {
				PreparedStatement p = newTransact(baseQuery, othCon);
				p.setInt(1, h.get_owner_id());
				p.setInt(2, h.get_sale());
				p.setInt(3, h.get_guild_id());
				p.setInt(4, h.get_access());
				p.setString(5, h.get_key());
				p.setInt(6, h.get_guild_rights());
				p.setInt(7, h.get_id());
				
				p.execute();
				closePreparedStatement(p);
			} catch (SQLException e) {
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				GameServer.addToLog("Game: Query: "+baseQuery);
			}
		}
		public static int GetNewIDPercepteur() 
		{
			int i = -50;//Pour éviter les conflits avec touts autre NPC
			try
			{
				String query = "SELECT `guid` FROM `percepteurs` ORDER BY `guid` ASC LIMIT 0 , 1;";
				
				ResultSet RS = executeQuery(query,Ancestra.OTHER_DB_NAME);
				while (RS.next()) 
				{
					i = RS.getInt("guid")-1; 
				}
				
				closeResultSet(RS);
			}catch(SQLException e)
			{
				RealmServer.addToLog("SQL ERROR: "+e.getMessage());
				e.printStackTrace();
			}
			return i;
		}
		public static int LOAD_ZAAPIS() 
		{
			int i = 0;
			String Bonta = "";
			String Brak = "";
			String Neutre = "";
			try
			{
				ResultSet RS = SQLManager.executeQuery("SELECT mapid, align from zaapi;",Ancestra.OTHER_DB_NAME); 
				while (RS.next()) 
				{ 
					if(RS.getInt("align") == Constants.ALIGNEMENT_BONTARIEN)
					{
						Bonta += RS.getString("mapid");
						if(!RS.isLast()) Bonta += ",";
					}
					else if(RS.getInt("align") == Constants.ALIGNEMENT_BRAKMARIEN)
					{
						Brak += RS.getString("mapid");
						if(!RS.isLast()) Brak += ",";
					}
					else
					{
						Neutre += RS.getString("mapid");
						if(!RS.isLast()) Neutre += ",";
					}
					i++;
				}
				Constants.ZAAPI.put(Constants.ALIGNEMENT_BONTARIEN, Bonta);
				Constants.ZAAPI.put(Constants.ALIGNEMENT_BRAKMARIEN, Brak);
				Constants.ZAAPI.put(Constants.ALIGNEMENT_NEUTRE, Neutre);
				closeResultSet(RS);
			}catch(SQLException e)
			{
				RealmServer.addToLog("SQL ERROR: "+e.getMessage());
				e.printStackTrace();
			}
			return i;
		}
		public static int LOAD_ZAAPS() 
		{
			int i = 0;
			try
			{
				ResultSet RS = SQLManager.executeQuery("SELECT mapID, cellID from zaaps;",Ancestra.OTHER_DB_NAME); 
				while (RS.next()) 
				{ 
					Constants.ZAAPS.put(RS.getInt("mapID"), RS.getInt("cellID"));
					i++;
				}
				closeResultSet(RS);
			}catch(SQLException e)
			{
				RealmServer.addToLog("SQL ERROR: "+e.getMessage());
				e.printStackTrace();
			}
			return i;
		}
		public static int getNextObjetID()
		{
			try
			{
				ResultSet RS = executeQuery("SELECT MAX(guid) AS max FROM items;",Ancestra.OTHER_DB_NAME);
				
				int guid = 0;
				boolean found = RS.first();
				
				if(found)
					guid = RS.getInt("max");
				
				closeResultSet(RS);
				return guid;
			}catch(SQLException e)
			{
				RealmServer.addToLog("SQL ERROR: "+e.getMessage());
				e.printStackTrace();
				Ancestra.closeServers();
			}
			return 0;
		}
		public static int LOAD_BANIP() 
		{
			int i = 0;
			try
			{
				ResultSet RS = executeQuery("SELECT ip from banip;",Ancestra.OTHER_DB_NAME); 
				while (RS.next()) 
				{ 
					Constants.BAN_IP += RS.getString("ip");
					if(!RS.isLast()) Constants.BAN_IP += ",";
					i++;
			    }
				closeResultSet(RS);
			}catch(SQLException e)
			{
				RealmServer.addToLog("SQL ERROR: "+e.getMessage());
				e.printStackTrace();
			}
			return i;
		}
		public static boolean ADD_BANIP(String ip)
		{
			String baseQuery = "INSERT INTO `banip`" +
					" VALUES (?);";
			try {
				PreparedStatement p = newTransact(baseQuery, othCon);
				p.setString(1, ip);
				p.execute();
				closePreparedStatement(p);
				return true;
			} catch (SQLException e) {
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				GameServer.addToLog("Game: Query: "+baseQuery);
			}
			return false;
		}
		public static void LOAD_HDVS()
		{
			try
			{
				ResultSet RS = executeQuery("SELECT * FROM `hdvs` ORDER BY id ASC",Ancestra.OTHER_DB_NAME);
				
				while(RS.next())
				{
					World.addHdv(new HDV(
									RS.getInt("map"),
									RS.getFloat("sellTaxe"),
									RS.getShort("sellTime"),
									RS.getShort("accountItem"),
									RS.getShort("lvlMax"),
									RS.getString("categories")));
					
				}
				
				RS = executeQuery("SELECT id MAX FROM `hdvs`",Ancestra.OTHER_DB_NAME);
				RS.first();
				World.setNextHdvID(RS.getInt("MAX"));
				
				closeResultSet(RS);
			}catch(SQLException e)
			{
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				e.printStackTrace();
			}
		}
		public static void LOAD_HDVS_ITEMS()
		{
			try
			{
				long time1 = System.currentTimeMillis();	//TIME
				ResultSet RS = executeQuery("SELECT i.*"+
						" FROM `items` AS i,`hdvs_items` AS h"+
						" WHERE i.guid = h.itemID",Ancestra.OTHER_DB_NAME);
				
				//Load items
				while(RS.next())
				{
					int guid 	= RS.getInt("guid");
					int tempID 	= RS.getInt("template");
					int qua 	= RS.getInt("qua");
					int pos		= RS.getInt("pos");
					String stats= RS.getString("stats");
					World.addObjet
					(
						World.newObjet
						(
							guid,
							tempID,
							qua,
							pos,
							stats
						),
						false
					);
				}
				
				//Load HDV entry
				RS = executeQuery("SELECT * FROM `hdvs_items`",Ancestra.OTHER_DB_NAME);
				while(RS.next())
				{
					HDV tempHdv = World.getHdv(RS.getInt("map"));
					if(tempHdv == null)continue;
					
					
					tempHdv.addEntry(new HDV.HdvEntry(
											RS.getInt("price"),
											RS.getByte("count"),
											RS.getInt("ownerGuid"),
											World.getObjet(RS.getInt("itemID"))));
				}
				System.out.println (System.currentTimeMillis() - time1 + "ms pour loader les HDVS items");	//TIME
				closeResultSet(RS);
			}catch(SQLException e)
			{
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				e.printStackTrace();
			}
		}
		public static void SAVE_HDVS_ITEMS(ArrayList<HdvEntry> liste)
		{
			PreparedStatement queries = null;
			try
			{
				String emptyQuery = "TRUNCATE TABLE `hdvs_items`";
				PreparedStatement emptyTable = newTransact(emptyQuery, othCon);
				emptyTable.execute();
				closePreparedStatement(emptyTable);
				
				String baseQuery = "INSERT INTO `hdvs_items` "+
									"(`map`,`ownerGuid`,`price`,`count`,`itemID`) "+
									"VALUES(?,?,?,?,?);";
				queries = newTransact(baseQuery, othCon);
				for(HdvEntry curEntry : liste)
				{
					
					if(curEntry.getOwner() == -1)continue;
					queries.setInt(1, curEntry.getHdvID());
					queries.setInt(2, curEntry.getOwner());
					queries.setInt(3, curEntry.getPrice());
					queries.setInt(4, curEntry.getAmount(false));
					queries.setInt(5, curEntry.getObjet().getGuid());
					
					queries.execute();
				}
				closePreparedStatement(queries);
				SAVE_HDV_AVGPRICE();
				}catch(SQLException e)
				{
					GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
					e.printStackTrace();
				}
		}
		public static void SAVE_HDV_AVGPRICE()
		{
			String baseQuery = "UPDATE `item_template`"+
								" SET sold = ?,avgPrice = ?"+
								" WHERE id = ?;";
			
			PreparedStatement queries = null;
			
			try
			{
				queries = newTransact(baseQuery, statCon);
				
				for(ObjTemplate curTemp : World.getObjTemplates())
				{
					if(curTemp.getSold() == 0)
						continue;
					
					queries.setLong(1, curTemp.getSold());
					queries.setInt(2, curTemp.getAvgPrice());
					queries.setInt(3, curTemp.getID());
					queries.executeUpdate();
				}
				closePreparedStatement(queries);
			}catch(SQLException e)
			{
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				e.printStackTrace();
			}
		}
		public static void LOAD_ANIMATIONS()
		{
			try
			{
				ResultSet RS = executeQuery("SELECT * from animations;",Ancestra.STATIC_DB_NAME);
				while(RS.next())
				{
					World.addAnimation(new Animations(
							RS.getInt("guid"),
							RS.getInt("id"),
							RS.getString("nom"),
							RS.getInt("area"),
							RS.getInt("action"),
							RS.getInt("size")
					));
				}
				closeResultSet(RS);
			}catch(SQLException e)
			{
				GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
				e.printStackTrace();
			}
		}
	    public static int LOAD_TRUNK()
        {
                int nbr = 0;
                try
                {
                        ResultSet RS = SQLManager.executeQuery("SELECT * from coffres;",Ancestra.OTHER_DB_NAME);
                        while(RS.next())
                        {                      
                                World.addTrunk(
                                                new Trunk(
                                                RS.getInt("id"),
                                                RS.getInt("id_house"),
                                                RS.getShort("mapid"),
                                                RS.getInt("cellid"),
                                                RS.getString("object"),
                                                RS.getInt("kamas"),
                                                RS.getString("key"),
                                                RS.getInt("owner_id")
                                                ));
                                nbr ++;
                        }
                        closeResultSet(RS);
                }catch(SQLException e)
                {
                        RealmServer.addToLog("SQL ERROR: "+e.getMessage());
                        e.printStackTrace();
                        nbr = 0;
                }
                return nbr;
        }
       
        public static void TRUNK_CODE(Personnage P, Trunk t, String packet)
        {      
                PreparedStatement p;
                String query = "UPDATE `coffres` SET `key`=? WHERE `id`=? AND owner_id=?;";
                try {
                        p = newTransact(query, othCon);
                        p.setString(1, packet);
                        p.setInt(2, t.get_id());
                        p.setInt(3, P.getAccID());
                       
                        p.execute();
                        closePreparedStatement(p);
                } catch (SQLException e) {
                        GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
                        GameServer.addToLog("Game: Query: "+query);
                }
        }
       
        public static void UPDATE_TRUNK(Trunk t)
        {      
                PreparedStatement p;
                String query = "UPDATE `coffres` SET `kamas`=?, `object`=? WHERE `id`=?";

                try {
                        p = newTransact(query, othCon);
                        p.setLong(1, t.get_kamas());
                        p.setString(2, t.parseTrunkObjetsToDB());
                        p.setInt(3, t.get_id());
                       
                        p.execute();
                        closePreparedStatement(p);
                } catch (SQLException e) {
                        GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
                        GameServer.addToLog("Game: Query: "+query);
                }
        }
}

