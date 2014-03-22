package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import objects.House;
import objects.Personnage;
import objects.Trunk;

import common.World;

import core.Console;
import database.AbstractDAO;

public class HouseData extends AbstractDAO<House>{

	public HouseData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(House obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(House obj) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean reset(int guild) {
		String query = "UPDATE `houses` SET `guild_rights`='0', `guild_id`='0' WHERE `guild_id`= "+guild;
		execute(query);
		return true;
	}

	@Override
	public boolean update(House obj) {
		try {
			String baseQuery = "UPDATE `houses` SET " + "`owner_id` = ?,"
					+ "`sale` = ?," + "`guild_id` = ?," + "`access` = ?,"
					+ "`key` = ?," + "`guild_rights` = ?" + " WHERE id = ?;";
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			
			statement.setInt(1, obj.get_owner_id());
			statement.setInt(2, obj.get_sale());
			statement.setInt(3, obj.get_guild_id());
			statement.setInt(4, obj.get_access());
			statement.setString(5, obj.get_key());
			statement.setInt(6, obj.get_guild_rights());
			statement.setInt(7, obj.get_id());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HouseData): "+e.getMessage());
		}
		return false;
	}
	
	public boolean update(Personnage player, House house) {
		try {
			String query = "UPDATE `houses` SET `sale`='0', " +
					"`owner_id`= "+player.getAccID()+", `guild_id`='0', `access`='0'," +
					" `key`='-', `guild_rights`='0' WHERE `id` = "+house.get_id();
			execute(query);
			
			house.set_sale(0);
			house.set_owner_id(player.getAccID());
			house.set_guild_id(0);
			house.set_access(0);
			house.set_key("-");
			house.set_guild_rights(0);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HouseData): "+e.getMessage());
		}

		
		ArrayList<Trunk> trunks = Trunk.getTrunksByHouse(house);
		for (Trunk trunk : trunks) {
			trunk.set_owner_id(player.getAccID());
			trunk.set_key("-");
		}

		try {
			String query = "UPDATE `coffres` SET `owner_id`=?, `key`='-' WHERE `id_house` = ?;";
			PreparedStatement statement = connection.prepareStatement(query);
			
			statement.setInt(1, player.getAccID());
			statement.setInt(2, house.get_id());
			
			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HouseData): "+e.getMessage());
		}
		return false;
	}
	
	public boolean update(House house, int price) {
		house.set_sale(price);
		
		String query = "UPDATE `houses` SET `sale`=? WHERE `id` = ?;";
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			statement.setInt(1, price);
			statement.setInt(2, house.get_id());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HouseData): "+e.getMessage());
		}
		return false;
	}
	
	public boolean update(Personnage player, House house, String packet) {
		try {
			String query = "UPDATE `houses` SET `key`=? WHERE `id`=? AND owner_id=?;";
			PreparedStatement statement = connection.prepareStatement(query);
			
			statement.setString(1, packet);
			statement.setInt(2, house.get_id());
			statement.setInt(3, player.getAccID());

			execute(statement);

			house.set_key(packet);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HouseData): "+e.getMessage());
		}
		return false;
	}
	
	public boolean update(House house, int guild, int rights) {
		try {
			String query = "UPDATE `houses` SET `guild_id`=?, `guild_rights`=? WHERE `id`=?;";
			PreparedStatement statement = connection.prepareStatement(query);
			statement.setInt(1, guild);
			statement.setInt(2, rights);
			statement.setInt(3, house.get_id());

			execute(statement);
			
			house.set_guild_id(guild);
			house.set_guild_rights(rights);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HouseData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public House load(int id) {
		House house = null;
		try {
			ResultSet result = getData("SELECT * FROM houses WHERE id = "+id);
			
			if(result.next()) {
				house = new House(result.getInt("id"), result
						.getShort("map_id"), result.getInt("cell_id"), result
						.getInt("owner_id"), result.getInt("sale"), result
						.getInt("guild_id"), result.getInt("access"), result
						.getString("key"), result.getInt("guild_rights"), result
						.getInt("mapid"), result.getInt("caseid"));
				World.data.addHouse(house);
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HouseData): "+e.getMessage());
		}
		return house;
	}
}
