package database.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.ItemSet;

import common.World;

import core.Console;
import database.AbstractDAO;

public class ItemSetData extends AbstractDAO<ItemSet>{

	public ItemSetData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(ItemSet obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(ItemSet obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(ItemSet obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ItemSet load(int id) {
		ItemSet set = null;
		try {
			ResultSet result = getData("SELECT * FROM itemsets WHERE id = "+id);
			
			if(result.next()) {
				set = new ItemSet(result.getInt("id"), result
						.getString("items"), result.getString("bonus"));
				World.data.addItemSet(set);
			}
			
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(ItemSetData): "+e.getMessage());
		}
		return set;
	}
}
