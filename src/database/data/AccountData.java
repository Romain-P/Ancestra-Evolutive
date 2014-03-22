package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Compte;

import common.World;

import core.Console;
import database.AbstractDAO;

public class AccountData extends AbstractDAO<Compte>{
	
	public AccountData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Compte obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Compte obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Compte obj) {
		try {
			String baseQuery = "UPDATE accounts SET " +
								"`bankKamas` = ?,"+
								"`bank` = ?,"+
								"`level` = ?,"+
								"`banned` = ?,"+
								"`friends` = ?,"+
								"`enemy` = ?,"+
								"`lastIP` = ?," +
								"`lastConnectionDate` = ?," +
								"`logged` = ?" +
								" WHERE `guid` = ?;";
			
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			
			statement.setLong(1, obj.getBankKamas());
			statement.setString(2, obj.parseBankObjetsToDB());
			statement.setInt(3, obj.get_gmLvl());
			statement.setInt(4, (obj.isBanned()?1:0));
			statement.setString(5, obj.parseFriendListToDB());
			statement.setString(6, obj.parseEnemyListToDB());
			statement.setString(7, obj.get_curIP());
			statement.setString(8, obj.getLastConnectionDate());
			statement.setInt(9, obj.isLogged() == true ? 1:0);
			statement.setInt(10, obj.get_GUID());
			super.execute(statement);
			return true;
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(AccountData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public Compte load(int id) {
		Compte account = null;
		try {
			String query = "SELECT * FROM accounts WHERE guid = "+id;
			ResultSet result = super.getData(query);
			
			if(result.next()) {
				account = new Compte(
						result.getInt("guid"),
						result.getString("account").toLowerCase(),
						result.getString("pass"),
						result.getString("pseudo"),
						result.getString("question"),
						result.getString("reponse"),
						result.getInt("level"),
						result.getInt("vip"),
						result.getInt("banned") == 1,
						result.getString("lastIP"),
						result.getString("lastConnectionDate"),
						result.getString("bank"),
						result.getInt("bankKamas"),
						result.getString("friends"),
						result.getString("enemy"),
						result.getInt("logged") == 1 ? true:false);
				World.data.addAccount(account);
				World.database.getCharacterData().loadByAccount(account);
				
				query = "UPDATE accounts SET reload_needed = 0 WHERE guid = "+id;
				super.execute(query);
			}
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(AccountData): "+e.getMessage()); 
		}
		return account;
	}
	
	public Compte loadByName(String name) {
		try {
			String query = "SELECT * FROM accounts WHERE account = '"+name+"'";
			ResultSet result = super.getData(query);
			
			if(result.next()) {
				Compte account = new Compte(
						result.getInt("guid"),
						result.getString("account").toLowerCase(),
						result.getString("pass"),
						result.getString("pseudo"),
						result.getString("question"),
						result.getString("reponse"),
						result.getInt("level"),
						result.getInt("vip"),
						result.getInt("banned") == 1,
						result.getString("lastIP"),
						result.getString("lastConnectionDate"),
						result.getString("bank"),
						result.getInt("bankKamas"),
						result.getString("friends"),
						result.getString("enemy"),
						result.getInt("logged") == 1 ? true:false);
				
				closeResultSet(result);
				
				World.data.addAccount(account);
				World.database.getCharacterData().loadByAccount(account);
				
				query = "UPDATE accounts SET reload_needed = 0 WHERE account = '"+name+"'";
				super.execute(query);
				
				return account;
			}
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(AccountData): "+e.getMessage()); 
		}
		return null;
	}
	
	public void updateState(boolean online) {
		int state = online ? 1 : 0;
		String baseQuery = "UPDATE accounts SET logged = "+state;
		
		super.execute(baseQuery);
	}
	
	public void updateState(Compte account, boolean online) { 
		int state = online ? 1 : 0;
		String baseQuery = "UPDATE accounts SET logged = "+state+" WHERE account = '"+account.get_name()+"';";
		execute(baseQuery);
	}
}
