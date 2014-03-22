package database.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.ExpLevel;

import common.World;

import core.Console;
import database.AbstractDAO;

public class ExpData extends AbstractDAO<ExpLevel>{

	public ExpData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(ExpLevel obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(ExpLevel obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(ExpLevel obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ExpLevel load(int id) {
		ExpLevel exp = null;
		try {
			ResultSet RS = getData("SELECT * FROM experience WHERE lvl = "+id);
			while (RS.next())
				World.data.addExpLevel(RS.getInt("lvl"),
						new ExpLevel(RS.getLong("perso"), RS.getInt("metier"),
								RS.getInt("dinde"), RS.getInt("pvp")));
			closeResultSet(RS);
		} catch (Exception e) {
			Console.instance.print("SQL ERROR(ExpData): "+e.getMessage());
		}
		return exp;
	}
	
	public void loadAll() {
		try {
			ResultSet RS = getData("SELECT * FROM experience");
			while (RS.next())
				World.data.addExpLevel(RS.getInt("lvl"),
						new ExpLevel(RS.getLong("perso"), RS.getInt("metier"),
								RS.getInt("dinde"), RS.getInt("pvp")));
			closeResultSet(RS);
		} catch (Exception e) {
			Console.instance.print("SQL ERROR(ExpData): "+e.getMessage());
		}
	}

}
