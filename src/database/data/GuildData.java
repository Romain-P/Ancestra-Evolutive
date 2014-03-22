package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Guild;

import common.World;

import core.Console;
import database.AbstractDAO;


public class GuildData extends AbstractDAO<Guild>{

	public GuildData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Guild obj) {
		String baseQuery = "INSERT INTO `guilds` VALUES (?,?,?,1,0,0,0,?,?);";
		try {
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			statement.setInt(1, obj.get_id());
			statement.setString(2, obj.get_name());
			statement.setString(3, obj.get_emblem());
			statement.setString(4, "462;0|461;0|460;0|459;0|458;0|457;0|456;0|455;0|454;0|453;0|452;0|451;0|");
			statement.setString(5, "176;100|158;1000|124;100|");

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(GuildData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public boolean delete(Guild obj) {
		String baseQuery = "DELETE FROM `guilds` WHERE `id` = "+obj.get_id();
		execute(baseQuery);
		return true;
	}

	@Override
	public boolean update(Guild obj) {
		String baseQuery = "UPDATE `guilds` SET `lvl` = ?, `xp` = ?,"
				+ "`capital` = ?, `nbrmax` = ?, `sorts` = ?,"
				+ "`stats` = ? " 
				+ "WHERE id = ?;";

		try {
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			
			statement.setInt(1, obj.get_lvl());
			statement.setLong(2, obj.get_xp());
			statement.setInt(3, obj.get_Capital());
			statement.setInt(4, obj.get_nbrPerco());
			statement.setString(5, obj.compileSpell());
			statement.setString(6, obj.compileStats());
			statement.setInt(7, obj.get_id());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(GuildData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public Guild load(int id) {
		Guild guild = null;
		try {
			ResultSet result = getData("SELECT * FROM guilds WHERE id = "+id);
			
			if (result.next()) {
				guild = new Guild(result.getInt("id"), result.getString("name"), result
								.getString("emblem"), result.getInt("lvl"), result
								.getLong("xp"), result.getInt("capital"), result
								.getInt("nbrmax"), result.getString("sorts"), result
								.getString("stats"));
				World.data.addGuild(guild, false);
				World.database.getGuildMemberData().load(id);
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(GuildData): "+e.getMessage());
		}
		return guild;
	}
	
	public boolean exist(String name) {
		boolean exist = false;
		try {
			ResultSet result = getData("SELECT * FROM guilds WHERE name = '"+name+"'");
			exist = result.next();
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(GuildData): "+e.getMessage());
		}
		return exist;
	}

}
