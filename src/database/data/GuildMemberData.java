package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Guild;
import objects.Guild.GuildMember;

import common.World;

import core.Console;
import database.AbstractDAO;

public class GuildMemberData extends AbstractDAO<GuildMember>{

	public GuildMemberData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(GuildMember obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(GuildMember obj) {
		String baseQuery = "DELETE FROM `guild_members` WHERE `guid` = "+obj.getGuid();
		execute(baseQuery);
		return true;
	}
	
	public boolean deleteAllByGuild(int guild) {
		String baseQuery = "DELETE FROM `guild_members` WHERE `guild` = "+guild;
		execute(baseQuery);
		return true;
	}

	@Override
	public boolean update(GuildMember obj) {
		String baseQuery = "REPLACE INTO `guild_members` VALUES(?,?,?,?,?,?,?,?,?,?,?);";

		try {
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			statement.setInt(1, obj.getGuid());
			statement.setInt(2, obj.getGuild().get_id());
			statement.setString(3, obj.getName());
			statement.setInt(4, obj.getLvl());
			statement.setInt(5, obj.getGfx());
			statement.setInt(6, obj.getRank());
			statement.setLong(7, obj.getXpGave());
			statement.setInt(8, obj.getPXpGive());
			statement.setInt(9, obj.getRights());
			statement.setInt(10, obj.getAlign());
			statement.setString(11, obj.getLastCo());

			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(GuildMemberData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public GuildMember load(int guildid) {
		GuildMember member = null;
		try {
			ResultSet result = getData("SELECT * FROM guild_members WHERE guild = "+guildid);
			
			while(result.next()) {
				Guild guild = World.data.getGuild(guildid);
				
				if (guild == null)
					return null;
				
				member = guild.addMember(result.getInt("guid"), result.getString("name"),
						result.getInt("level"), result.getInt("gfxid"),
						result.getInt("rank"), result.getByte("pxp"),
						result.getLong("xpdone"), result.getInt("rights"),
						result.getByte("align"), result.getDate("lastConnection")
								.toString().replaceAll("-", "~"));
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(GuildMemberData): "+e.getMessage());
		}
		return member;
	}
	
	public int getGuildByPlayer(int guid) {
		int guild = -1;
		try {
			ResultSet result = getData("SELECT guid FROM `guild_members` WHERE guid = "+guid);
			if(result.next())
				guild = result.getInt("guid");
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(GuildMemberData): "+e.getMessage());
		}
		return guild;
	}
	
	public int playerExistInGuild(int guid) {
		int guildID = 0;
		try {
			ResultSet result = getData("SELECT guild FROM `guild_members` WHERE guid = "+ guid);
			if(result.next())
				guildID = result.getInt("guild");
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(GuildMemberData): "+e.getMessage());
		}
		return guildID;
	}
	
	public int[] playerExistInGuild(String name) {
		int guildId = -1, guid = -1;
		try {
			ResultSet result = getData("SELECT guild,guid FROM `guild_members` WHERE name = '" + name + "'");
			if(result.next()) {
				guildId = result.getInt("guild");
				guid = result.getInt("guid");
			}
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(GuildMemberData): "+e.getMessage());
		}
		return new int[] { guid, guildId };
	}

}
