package database.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Area;

import common.World;

import core.Console;
import database.AbstractDAO;

public class AreaData extends AbstractDAO<Area>{

	public AreaData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Area obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Area obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Area obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Area load(int id) {
		Area area = null;
		try {
			ResultSet result = super.getData("SELECT * FROM area_data WHERE id = "+id);
			
			if(result.next()) {
				area = new Area(result.getInt("id"), result.getInt("superarea"), result.getString("name"));
				World.data.addArea(area);
				//on ajoute la zone au continent
				area.get_superArea().addArea(area);
			}
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(AreaData): "+e.getMessage());
		}
		return area;
	}
}
