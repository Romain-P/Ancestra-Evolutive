package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import objects.Compte;
import objects.Objet;
import objects.Personnage;

import common.Constants;
import common.World;

import core.Console;
import database.AbstractDAO;

public class CharacterData extends AbstractDAO<Personnage>{

	public CharacterData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Personnage obj) {
		String baseQuery = "INSERT INTO personnages( `guid` ," +
				" `name` , `sexe` , `class` , `color1` , `color2` , `color3` , `kamas` ," +
				" `spellboost` , `capital` , `energy` , `level` , `xp` , `size` , `gfx` ," +
				" `account`,`cell`,`map`,`spells`,`objets`, `storeObjets`)" +
				" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'', '');";
		
		try {
			PreparedStatement statement = super.connection.prepareStatement(baseQuery);
			
			statement.setInt(1,obj.get_GUID());
			statement.setString(2, obj.get_name());
			statement.setInt(3,obj.get_sexe());
			statement.setInt(4,obj.get_classe());
			statement.setInt(5,obj.get_color1());
			statement.setInt(6,obj.get_color2());
			statement.setInt(7,obj.get_color3());
			statement.setLong(8,obj.get_kamas());
			statement.setInt(9,obj.get_spellPts());
			statement.setInt(10,obj.get_capital());
			statement.setInt(11,obj.get_energy());
			statement.setInt(12,obj.get_lvl());
			statement.setLong(13,obj.get_curExp());
			statement.setInt(14,obj.get_size());
			statement.setInt(15,obj.get_gfxID());
			statement.setInt(16,obj.getAccID());
			statement.setInt(17,obj.get_curCell().getID());
			statement.setInt(18,obj.get_curCarte().get_id());
			statement.setString(19, obj.parseSpellToDB());
			
			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(CharacterData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public boolean delete(Personnage obj) {
		try {
			String baseQuery = "DELETE FROM personnages WHERE guid = "+obj.get_GUID();
			execute(baseQuery);
			
			if(!obj.getItemsIDSplitByChar(",").equals("")) {
				baseQuery = "DELETE FROM items WHERE guid IN ('"+obj.getItemsIDSplitByChar(",")+"');";
				execute(baseQuery);
			}
			if(!obj.getStoreItemsIDSplitByChar(",").equals("")) {
				baseQuery = "DELETE FROM items WHERE guid IN ('"+obj.getStoreItemsIDSplitByChar(",")+"');";
				execute(baseQuery);
			}
			if(obj.getMount() != null) {
				baseQuery = "DELETE FROM mounts_data WHERE id = "+obj.getMount().get_id();
				execute(baseQuery);
				World.data.delDragoByID(obj.getMount().get_id());
			}
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(CharacterData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public boolean update(Personnage obj)  {
		try {
			String baseQuery = "UPDATE `personnages` SET `kamas`= ?,`spellboost`= ?,"+
							"`capital`= ?, `energy`= ?, `level`= ?, `xp`= ?, `size` = ?," +
							"`gfx`= ?,`alignement`= ?,`honor`= ?,`deshonor`= ?,`alvl`= ?,"+
							"`vitalite`= ?,`force`= ?,`sagesse`= ?,`intelligence`= ?,"+
							"`chance`= ?,`agilite`= ?,`seeSpell`= ?,`seeFriend`= ?,"+
							"`seeAlign`= ?,`seeSeller`= ?,`canaux`= ?,`map`= ?,"+
							"`cell`= ?,`pdvper`= ?,`spells`= ?,`objets`= ?,`storeObjets`= ?,"+
							"`savepos`= ?,`zaaps`= ?,`jobs`= ?,`mountxpgive`= ?,`mount`= ?,"+
							"`title`= ?,`wife`= ?"+
							" WHERE `personnages`.`guid` = ? LIMIT 1 ;";
			
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			
			statement.setLong(1,obj.get_kamas());
			statement.setInt(2,obj.get_spellPts());
			statement.setInt(3,obj.get_capital());
			statement.setInt(4,obj.get_energy());
			statement.setInt(5,obj.get_lvl());
			statement.setLong(6,obj.get_curExp());
			statement.setInt(7,obj.get_size());
			statement.setInt(8,obj.get_gfxID());
			statement.setInt(9,obj.get_align());
			statement.setInt(10,obj.get_honor());
			statement.setInt(11,obj.getDeshonor());
			statement.setInt(12,obj.getALvl());
			statement.setInt(13,obj.get_baseStats().getEffect(Constants.STATS_ADD_VITA));
			statement.setInt(14,obj.get_baseStats().getEffect(Constants.STATS_ADD_FORC));
			statement.setInt(15,obj.get_baseStats().getEffect(Constants.STATS_ADD_SAGE));
			statement.setInt(16,obj.get_baseStats().getEffect(Constants.STATS_ADD_INTE));
			statement.setInt(17,obj.get_baseStats().getEffect(Constants.STATS_ADD_CHAN));
			statement.setInt(18,obj.get_baseStats().getEffect(Constants.STATS_ADD_AGIL));
			statement.setInt(19,(obj.is_showSpells()?1:0));
			statement.setInt(20,(obj.is_showFriendConnection()?1:0));
			statement.setInt(21,(obj.is_showWings()?1:0));
			statement.setInt(22,(obj.is_showSeller()?1:0));
			statement.setString(23,obj.get_canaux());
			statement.setInt(24,obj.get_curCarte().get_id());
			statement.setInt(25,obj.get_curCell().getID());
			statement.setInt(26,obj.get_pdvper());
			statement.setString(27,obj.parseSpellToDB());
			statement.setString(28,obj.parseObjetsToDB());
			statement.setString(29, obj.parseStoreItemstoBD());
			statement.setString(30,obj.get_savePos());
			statement.setString(31,obj.parseZaaps());
			statement.setString(32,obj.parseJobData());
			statement.setInt(33,obj.getMountXpGive());
			statement.setInt(34, (obj.getMount()!=null?obj.getMount().get_id():-1));
			statement.setByte(35,(obj.get_title()));
			statement.setInt(36,obj.getWife());
			statement.setInt(37,obj.get_GUID());
			
			execute(statement);
			
			if(obj.getGuildMember() != null)
				World.database.getGuildMemberData().update(obj.getGuildMember());
			if(obj.getMount() != null)
				World.database.getMountData().update(obj.getMount());
			updateItems(obj);
			Console.instance.println("Personnage "+obj.get_name()+" sauvegarde");
			return true;
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(CharacterData): "+e.getMessage());
		}
		return false;
	}
	
	public boolean updateItems(Personnage obj) {
		try {
			for(Objet item: obj.getItems().values()) {
				World.database.getItemData().update(item);
			}
			
			for(String s : obj.getBankItemsIDSplitByChar(":").split(":")) {
				try {
					int guid = Integer.parseInt(s);
					Objet item = World.data.getObjet(guid);
					if(item == null)continue;
					
					World.database.getItemData().update(item);
				} catch(Exception e) { continue; }
			}
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(CharacterData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public Personnage load(int id) {
		Personnage player = null;
		try {
			ResultSet result = getData("SELECT * FROM personnages WHERE guid = "+id);
			
			if(result.next()) {
				TreeMap<Integer,Integer> stats = new TreeMap<Integer,Integer>();
				stats.put(Constants.STATS_ADD_VITA, result.getInt("vitalite"));
				stats.put(Constants.STATS_ADD_FORC, result.getInt("force"));
				stats.put(Constants.STATS_ADD_SAGE, result.getInt("sagesse"));
				stats.put(Constants.STATS_ADD_INTE, result.getInt("intelligence"));
				stats.put(Constants.STATS_ADD_CHAN, result.getInt("chance"));
				stats.put(Constants.STATS_ADD_AGIL, result.getInt("agilite"));
				
				player = new Personnage(
						result.getInt("guid"),
						result.getString("name"),
						result.getInt("sexe"),
						result.getInt("class"),
						result.getInt("color1"),
						result.getInt("color2"),
						result.getInt("color3"),
						result.getLong("kamas"),
						result.getInt("spellboost"),
						result.getInt("capital"),
						result.getInt("energy"),
						result.getInt("level"),
						result.getLong("xp"),
						result.getInt("size"),
						result.getInt("gfx"),
						result.getByte("alignement"),
						result.getInt("account"),
						stats,
						result.getByte("seeFriend"),
						result.getByte("seeAlign"),
						result.getByte("seeSeller"),
						result.getString("canaux"),
						result.getShort("map"),
						result.getInt("cell"),
						result.getString("objets"),
						result.getString("storeObjets"),
						result.getInt("pdvper"),
						result.getString("spells"),
						result.getString("savepos"),
						result.getString("jobs"),
						result.getInt("mountxpgive"),
						result.getInt("mount"),
						result.getInt("honor"),
						result.getInt("deshonor"),
						result.getInt("alvl"),
						result.getString("zaaps"),
						result.getByte("title"),
						result.getInt("wife")
						);
				//Vérifications pré-connexion
				player.VerifAndChangeItemPlace();
				World.data.addPersonnage(player);
				
			}
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(CharacterData): "+e.getMessage());
		}
		return player;
	}
	
	public Personnage loadByAccount(Compte obj) {
		Personnage player = null;
		try {
			ResultSet result = getData("SELECT * FROM personnages WHERE account = "+obj.get_GUID());
			while(result.next()) {
				TreeMap<Integer,Integer> stats = new TreeMap<Integer,Integer>();
				stats.put(Constants.STATS_ADD_VITA, result.getInt("vitalite"));
				stats.put(Constants.STATS_ADD_FORC, result.getInt("force"));
				stats.put(Constants.STATS_ADD_SAGE, result.getInt("sagesse"));
				stats.put(Constants.STATS_ADD_INTE, result.getInt("intelligence"));
				stats.put(Constants.STATS_ADD_CHAN, result.getInt("chance"));
				stats.put(Constants.STATS_ADD_AGIL, result.getInt("agilite"));
				
				player = new Personnage(
						result.getInt("guid"),
						result.getString("name"),
						result.getInt("sexe"),
						result.getInt("class"),
						result.getInt("color1"),
						result.getInt("color2"),
						result.getInt("color3"),
						result.getLong("kamas"),
						result.getInt("spellboost"),
						result.getInt("capital"),
						result.getInt("energy"),
						result.getInt("level"),
						result.getLong("xp"),
						result.getInt("size"),
						result.getInt("gfx"),
						result.getByte("alignement"),
						result.getInt("account"),
						stats,
						result.getByte("seeFriend"),
						result.getByte("seeAlign"),
						result.getByte("seeSeller"),
						result.getString("canaux"),
						result.getShort("map"),
						result.getInt("cell"),
						result.getString("objets"),
						result.getString("storeObjets"),
						result.getInt("pdvper"),
						result.getString("spells"),
						result.getString("savepos"),
						result.getString("jobs"),
						result.getInt("mountxpgive"),
						result.getInt("mount"),
						result.getInt("honor"),
						result.getInt("deshonor"),
						result.getInt("alvl"),
						result.getString("zaaps"),
						result.getByte("title"),
						result.getInt("wife"));
				
				//vérifications pré-connexion
				player.VerifAndChangeItemPlace();
				World.data.addPersonnage(player);
				
				//ajout de la guilde
				int guildId = World.database.getGuildMemberData().getGuildByPlayer(player.get_GUID());
				if(guildId >= 0)
					player.setGuildMember(World.data.getGuild(guildId).getMember(result.getInt("guid")));
				
				
			}
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(CharacterData): "+e.getMessage());
		}
		return player;
	}
	
	public boolean exist(String name) {
		boolean exist = false;
		try {
			ResultSet result = getData("SELECT * FROM personnages WHERE name = '"+name+"'");
			exist = result.next();
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(CharacterData): "+e.getMessage());
		}
		return exist;
	}
	
	public int nextId() {
		int guid = -1;
		
		try {
			String query = "SELECT MAX(guid) AS max FROM personnages;";
			ResultSet result = getData(query);
			
			while(result.next())
				guid = result.getInt("max")+1;
			
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(CharacterData): "+e.getMessage());
		}
		return guid;
	}
}
