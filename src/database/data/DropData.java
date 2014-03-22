package database.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Drop;
import objects.Monstre;

import common.World;

import core.Console;
import database.AbstractDAO;

public class DropData extends AbstractDAO<Drop>{

	public DropData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Drop obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Drop obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Drop obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Drop load(int mob) {
		Drop drop = null;
		try {
			ResultSet result = getData("SELECT * FROM drops WHERE mob = "+mob);
			
			while (result.next()) {
				Monstre MT = World.data.getMonstre(result.getInt("mob"));
				drop = new Drop(result.getInt("item"), result.getInt("seuil"), result
						.getFloat("taux"), result.getInt("max"));
				MT.addDrop(drop);				
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(DropData): "+e.getMessage());
		}
		return drop;
	}

}
