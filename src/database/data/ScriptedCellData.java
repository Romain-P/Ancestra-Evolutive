package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Carte.Case;

import common.World;

import core.Console;
import database.AbstractDAO;

public class ScriptedCellData extends AbstractDAO<Case>{

	public ScriptedCellData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Case obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Case obj) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean delete(int mapid, int cellid) {
		String baseQuery = "DELETE FROM `scripted_cells` WHERE `MapID` = "+mapid+" AND `CellID` = "+cellid;
		execute(baseQuery);
		return true;
	}

	@Override
	public boolean update(Case obj) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean update(int mapID1, int cellID1, int action, int event, String args, String cond) {
		try {
			String baseQuery = "REPLACE INTO `scripted_cells` VALUES (?,?,?,?,?,?);";
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			statement.setInt(1, mapID1);
			statement.setInt(2, cellID1);
			statement.setInt(3, action);
			statement.setInt(4, event);
			statement.setString(5, args);
			statement.setString(6, cond);

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(CellData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public Case load(int mapid) {
		//retourne null dans tous les cas
		Case cell = null;
		try {
			ResultSet result = getData("SELECT * FROM scripted_cells WHERE mapid = "+mapid);
			while (result.next()) {
				if (World.data.getCarte(result.getShort("MapID")) == null)
					continue;
				if (World.data.getCarte(result.getShort("MapID")).getCase(
						result.getInt("CellID")) == null)
					continue;

				if(result.getInt("EventID") == 1) {
					World.data.getCarte(result.getShort("MapID"))
							.getCase(result.getInt("CellID"))
							.addOnCellStopAction(result.getInt("ActionID"),
							result.getString("ActionsArgs"),
							result.getString("Conditions"));
				}
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(CellData): "+e.getMessage());
		}
		return cell;
	}
}
