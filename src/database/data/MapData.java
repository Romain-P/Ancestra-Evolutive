package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Action;
import objects.Carte;

import common.World;

import core.Console;
import database.AbstractDAO;

public class MapData extends AbstractDAO<Carte>{

	public MapData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Carte obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Carte obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Carte obj) {
		try {
			String baseQuery = "UPDATE `maps` SET " + "`places` = ?, "
					+ "`numgroup` = ? " + "WHERE id = ?;";
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			
			statement.setString(1, obj.get_placesStr());
			statement.setInt(2, obj.getMaxGroupNumb());
			statement.setInt(3, obj.get_id());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(MapData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public Carte load(int id) {
		Carte map = null;
		try {
			ResultSet result = getData("SELECT * FROM maps WHERE id = "+id);
			
			if(result.next()) {
				map = new Carte(result.getShort("id"), result
						.getString("date"), result.getByte("width"), result
						.getByte("heigth"), result.getString("key"), result
						.getString("places"), result.getString("mapData"), result
						.getString("cells"), result.getString("monsters"), result
						.getString("mappos"), result.getByte("numgroup"), result
						.getByte("groupmaxsize"));
				World.data.addCarte(map);
				World.database.getCollectorData().loadByMap(id);
				World.database.getHouseData().load(id);
				World.database.getHdvData().load(id);
				World.database.getMountparkData().load(id);
				World.database.getNpcData().load(id);
				World.database.getScriptedCellData().load(id);
				World.database.getTrunkData().load(id);
				loadFightActions(map);
			}
			closeResultSet(result);
			
			result = getData("SELECT * from mobgroups_fix WHERE mapid = "+id);
			
			while (result.next()) {
				if (map == null)
					continue;
				if (map.getCase(result.getInt("cellid")) == null)
					continue;
				map.addStaticGroup(result.getInt("cellid"), result.getString("groupData"));
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(MapData): "+e.getMessage());
		}
		return map;
	}
	
	public void loadFightActions(Carte map) {
		try {
			ResultSet result = getData("SELECT * FROM endfight_action WHERE map = "+map.get_id());
			while (result.next()) {
				
				map.addEndFightAction(result.getInt("fighttype"),
						new Action(result.getInt("action"), result.getString("args"),
								result.getString("cond")));
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(MapData): "+e.getMessage());
		}
	}
}
