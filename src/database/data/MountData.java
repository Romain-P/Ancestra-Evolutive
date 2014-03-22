package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Dragodinde;

import common.World;

import core.Console;
import database.AbstractDAO;

public class MountData extends AbstractDAO<Dragodinde>{

	public MountData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Dragodinde obj) {
		try {
			String baseQuery = "REPLACE INTO `mounts_data`(`id`,`color`,`sexe`,`name`,`xp`,`level`,"
					+ "`endurance`,`amour`,`maturite`,`serenite`,`reproductions`,`fatigue`,`items`,"
					+ "`ancetres`,`energie`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
			
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			
			statement.setInt(1, obj.get_id());
			statement.setInt(2, obj.get_color());
			statement.setInt(3, obj.get_sexe());
			statement.setString(4, obj.get_nom());
			statement.setLong(5, obj.get_exp());
			statement.setInt(6, obj.get_level());
			statement.setInt(7, obj.get_endurance());
			statement.setInt(8, obj.get_amour());
			statement.setInt(9, obj.get_maturite());
			statement.setInt(10, obj.get_serenite());
			statement.setInt(11, obj.get_reprod());
			statement.setInt(12, obj.get_fatigue());
			statement.setString(13, obj.getItemsId());
			statement.setString(14, obj.get_ancetres());
			statement.setInt(15, obj.get_energie());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(MountData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public boolean delete(Dragodinde obj) {
		String baseQuery = "DELETE FROM `mounts_data` WHERE `id` = "+obj.get_id();
		execute(baseQuery);
		return true;
	}

	@Override
	public boolean update(Dragodinde obj) {
		try {
			String baseQuery = "UPDATE mounts_data SET " + "`name` = ?,"
					+ "`xp` = ?," + "`level` = ?," + "`endurance` = ?,"
					+ "`amour` = ?," + "`maturite` = ?," + "`serenite` = ?,"
					+ "`reproductions` = ?," + "`fatigue` = ?," + "`energie` = ?,"
					+ "`ancetres` = ?," + "`items` = ?" + " WHERE `id` = ?;";
			
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			
			statement.setString(1, obj.get_nom());
			statement.setLong(2, obj.get_exp());
			statement.setInt(3, obj.get_level());
			statement.setInt(4, obj.get_endurance());
			statement.setInt(5, obj.get_amour());
			statement.setInt(6, obj.get_maturite());
			statement.setInt(7, obj.get_serenite());
			statement.setInt(8, obj.get_reprod());
			statement.setInt(9, obj.get_fatigue());
			statement.setInt(10, obj.get_energie());
			statement.setString(11, obj.get_ancetres());
			statement.setString(12, obj.getItemsId());
			statement.setInt(13, obj.get_id());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(MountData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public Dragodinde load(int id) {
		Dragodinde mount = null;
		try {
			ResultSet result = getData("SELECT * FROM mounts_data WHERE id = "+id);
			
			if(result.next()) {
				mount = new Dragodinde(result.getInt("id"), result
						.getInt("color"), result.getInt("sexe"),
						result.getInt("amour"), result.getInt("endurance"), result
								.getInt("level"), result.getLong("xp"), result
								.getString("name"), result.getInt("fatigue"), result
								.getInt("energie"), result.getInt("reproductions"),
						result.getInt("maturite"), result.getInt("serenite"), result
								.getString("items"), result.getString("ancetres"));
				World.data.addDragodinde(mount);
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(MountData): "+e.getMessage());
		}
		return mount;
	}
}
