package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Carte;
import objects.Carte.MountPark;

import common.World;

import core.Console;
import database.AbstractDAO;

public class MountparkData extends AbstractDAO<MountPark>{

	public MountparkData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(MountPark obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(MountPark obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(MountPark obj) {
		try {
			String baseQuery = "REPLACE INTO `mountpark_data`( `mapid` , `cellid`, `size` , `owner` , `guild` , `price` , `data` )"
					+ " VALUES (?,?,?,?,?,?,?);";
			
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			
			statement.setInt(1, obj.get_map().get_id());
			statement.setInt(2, obj.get_cellid());
			statement.setInt(3, obj.get_size());
			statement.setInt(4, obj.get_owner());
			statement.setInt(5, (obj.get_guild() == null ? -1 : obj.get_guild().get_id()));
			statement.setInt(6, obj.get_price());
			statement.setString(7, obj.parseDBData());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(MountparkData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public MountPark load(int id) {
		MountPark park = null;
		try {
			ResultSet result = getData("SELECT * FROM mountpark_data WHERE mapid ="+id);
			while (result.next()) {
				Carte map = World.data.getCarte(result.getShort("mapid"));
				
				if (map == null)
					continue;
				
				park = new MountPark(result.getInt("owner"), map,
						result.getInt("cellid"), result.getInt("size"), result
								.getString("data"), result.getInt("guild"), result
								.getInt("price"));
				World.data.addMountPark(park);
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(MountparkData): "+e.getMessage());
		}
		return null;
	}
}
