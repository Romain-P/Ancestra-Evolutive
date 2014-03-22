package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.HDV;
import objects.HDV.HdvEntry;

import common.World;

import core.Console;
import database.AbstractDAO;

public class HdvData extends AbstractDAO<HDV>{

	public HdvData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(HDV obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(HDV obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(HDV obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HDV load(int id) {
		HDV hdv = null;
		try {
			ResultSet result = getData("SELECT * FROM hdvs WHERE map = "+id);

			if (result.next()) {
				hdv = new HDV(result.getInt("map"), result
						.getFloat("sellTaxe"), result.getShort("sellTime"), result
						.getShort("accountItem"), result.getShort("lvlMax"), result
						.getString("categories"));
				World.data.addHdv(hdv);
				loadHdvItems(id);
			}
			closeResultSet(result);

			result = getData("SELECT id MAX FROM `hdvs`");
			
			if (result.first())
				World.data.setNextHdvID(result.getInt("MAX"));
			
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HdvData): "+e.getMessage());
		}
		return hdv;
	}
	
	public void loadHdvItems(int map) {
		try {
			ResultSet result = getData("SELECT * FROM hdvs_items WHERE map = "+map);

			while (result.next()) {
				HDV tempHdv = World.data.getHdv(result.getInt("map"));
				if (tempHdv == null)
					continue;

				tempHdv.addEntry(new HDV.HdvEntry(result.getInt("price"), result
						.getByte("count"), result.getInt("ownerGuid"), World.data
						.getObjet(result.getInt("itemID"))));
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HdvData): "+e.getMessage());
		}
	}
	
	public void updateHdvItems(int map) {
		try {
			String baseQuery = "INSERT INTO `hdvs_items` "
					+ "(`map`,`ownerGuid`,`price`,`count`,`itemID`) "
					+ "VALUES(?,?,?,?,?);";
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			
			for (HdvEntry curEntry : World.data.getHdv(map).getAllEntry()) {
				if (curEntry.getOwner() == -1)
					continue;
				
				statement.setInt(1, curEntry.getHdvID());
				statement.setInt(2, curEntry.getOwner());
				statement.setInt(3, curEntry.getPrice());
				statement.setInt(4, curEntry.getAmount(false));
				statement.setInt(5, curEntry.getObjet().getGuid());
				
				statement.execute();
				World.database.getItemTemplateData().update(curEntry.getObjet().getTemplate());
			}
			closeStatement(statement);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(HdvData): "+e.getMessage());
		}
	}
}
