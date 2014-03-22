package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Personnage;
import objects.Trunk;

import common.World;

import core.Console;
import database.AbstractDAO;

public class TrunkData extends AbstractDAO<Trunk>{

	public TrunkData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Trunk obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Trunk obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Trunk obj) {
		try {
			String query = "UPDATE `coffres` SET `kamas`=?, `object`=? WHERE `id`=?";
			PreparedStatement statement = connection.prepareStatement(query);
			
			statement.setLong(1, obj.get_kamas());
			statement.setString(2, obj.parseTrunkObjetsToDB());
			statement.setInt(3, obj.get_id());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(TrunkData): "+e.getMessage());
		}
		return false;
	}

	public boolean update(Personnage player, Trunk trunk, String packet) {
		try {
			String query = "UPDATE `coffres` SET `key`=? WHERE `id`=? AND owner_id=?;";
			PreparedStatement statement = connection.prepareStatement(query);
			
			statement.setString(1, packet);
			statement.setInt(2, trunk.get_id());
			statement.setInt(3, player.getAccID());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(TrunkData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public Trunk load(int mapid) {
		Trunk trunk = null;
		try {
			ResultSet result = getData("SELECT * FROM coffres WHERE mapid ="+mapid);
			while (result.next()) {
				World.data.addTrunk(new Trunk(result.getInt("id"), result
						.getInt("id_house"), result.getShort("mapid"), result
						.getInt("cellid"), result.getString("object"), result
						.getInt("kamas"), result.getString("key"), result
						.getInt("owner_id")));
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(TrunkData): "+e.getMessage());
		}
		return trunk;
	}

}
