package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Objet;

import common.World;

import core.Console;
import database.AbstractDAO;

public class ItemData extends AbstractDAO<Objet>{

	public ItemData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Objet obj) {
		try {
			String baseQuery = "INSERT INTO `items` VALUES(?,?,?,?,?);";

			PreparedStatement statement = connection.prepareStatement(baseQuery);

			statement.setInt(1, obj.getGuid());
			statement.setInt(2, obj.getTemplate().getID());
			statement.setInt(3, obj.getQuantity());
			statement.setInt(4, obj.getPosition());
			statement.setString(5, obj.parseToSave());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(ItemData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public boolean delete(Objet obj) {
		String baseQuery = "DELETE FROM items WHERE guid = "+obj.getGuid();
		execute(baseQuery);
		return true;
	}

	@Override
	public boolean update(Objet obj) {
		try {
			String baseQuery = "REPLACE INTO `items` VALUES(?,?,?,?,?);";

			PreparedStatement statement = connection.prepareStatement(baseQuery);

			statement.setInt(1, obj.getGuid());
			statement.setInt(2, obj.getTemplate().getID());
			statement.setInt(3, obj.getQuantity());
			statement.setInt(4, obj.getPosition());
			statement.setString(5, obj.parseToSave());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(ItemData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public Objet load(int id) {
		Objet item = null;
		try {
			ResultSet RS = getData("SELECT * FROM items WHERE guid = "+id);
			
			if(RS.next()) {
				int guid = RS.getInt("guid");
				int tempID = RS.getInt("template");
				int qua = RS.getInt("qua");
				int pos = RS.getInt("pos");
				String stats = RS.getString("stats");
				item = new Objet(guid, tempID, qua, pos, stats);
				
				World.data.addObjet(item, false);
			}
			closeResultSet(RS);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(ItemData): "+e.getMessage());
		}
		return item;
	}
	
	public void load(String items) {
		try {
			String req = "SELECT * FROM items WHERE guid IN (" + items + ")";
			ResultSet result = getData(req);
			
			while (result.next()) {
				int guid = result.getInt("guid");
				int tempID = result.getInt("template");
				int qua = result.getInt("qua");
				int pos = result.getInt("pos");
				
				String stats = result.getString("stats");
				
				World.data.addObjet(World.data.newObjet(guid, tempID, qua, pos, stats),false);
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(ItemData): "+e.getMessage());
		}
	}
	
	public int nextId() {
		int guid = -1;
		
		try {
			String query = "SELECT MAX(guid) AS max FROM items;";
			ResultSet result = getData(query);
			
			while(result.next())
				guid = result.getInt("max")+1;
			
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(ItemData): "+e.getMessage());
		}
		return guid;
	}
}
