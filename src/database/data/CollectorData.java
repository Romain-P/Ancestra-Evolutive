package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Carte;
import objects.Percepteur;

import common.World;

import core.Console;
import database.AbstractDAO;

public class CollectorData extends AbstractDAO<Percepteur>{

	public CollectorData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Percepteur obj) {
		String baseQuery = "INSERT INTO `percepteurs`" +
				" VALUES (?,?,?,?,?,?,?,?,?,?);";
		try {
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			statement.setInt(1, obj.getGuid());
			statement.setInt(2, obj.get_mapID());
			statement.setInt(3, obj.get_cellID());
			statement.setInt(4, obj.getOrientation());
			statement.setInt(5, obj.get_guildID());
			statement.setInt(6, obj.get_N1());
			statement.setInt(7, obj.get_N2());
			statement.setString(8, "");
			statement.setLong(9, 0);
			statement.setLong(10, 0);
			
			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(CollectorData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public boolean delete(Percepteur obj) {
		String baseQuery = "DELETE FROM percepteurs WHERE guid = "+obj.getGuid();
		execute(baseQuery);
		return true;
	}

	@Override
	public boolean update(Percepteur obj) {
		String baseQuery = "UPDATE `percepteurs` SET " + "`objets` = ?,"
				+ "`kamas` = ?," + "`xp` = ?" + " WHERE guid = ?;";

		try {
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			statement.setString(1, obj.parseItemPercepteur());
			statement.setLong(2, obj.getKamas());
			statement.setLong(3, obj.getXp());
			statement.setInt(4, obj.getGuid());
			
			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(CollectorData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public Percepteur load(int id) {
		Percepteur collector = null;
		try {
			ResultSet RS = getData("SELECT * FROM percepteurs WHERE guid = "+id);
			
			if(RS.next()) {
				Carte map = World.data.getCarte(RS.getShort("mapid"));
				if(map == null) return null;
				
				collector = new Percepteur(
								RS.getInt("guid"),
								RS.getShort("mapid"),
								RS.getInt("cellid"),
								RS.getByte("orientation"),
								RS.getInt("guild_id"),
								RS.getShort("N1"),
								RS.getShort("N2"),
								RS.getString("objets"),
								RS.getLong("kamas"),
								RS.getLong("xp"));
				World.data.addPerco(collector);
			}
			closeResultSet(RS);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(CollectorData): "+e.getMessage());
		}
		return collector;
	}
	
	public Percepteur loadByMap(int id) {
		Percepteur collector = null;
		try {
			ResultSet RS = getData("SELECT * FROM percepteurs WHERE mapid = "+id);
			
			if(RS.next()) {
				collector = new Percepteur(
								RS.getInt("guid"),
								RS.getShort("mapid"),
								RS.getInt("cellid"),
								RS.getByte("orientation"),
								RS.getInt("guild_id"),
								RS.getShort("N1"),
								RS.getShort("N2"),
								RS.getString("objets"),
								RS.getLong("kamas"),
								RS.getLong("xp"));
				World.data.addPerco(collector);
			}
			closeResultSet(RS);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(CollectorData): "+e.getMessage());
		}
		return collector;
	}
	
	public int nextId() {
		int guid = -1;
		
		try {
			String query = "SELECT MAX(guid) AS max FROM percepteurs;";
			ResultSet result = getData(query);
			
			while(result.next())
				guid = result.getInt("max")+1;
			
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(CollectorData): "+e.getMessage());
		}
		return guid;
	}
}
