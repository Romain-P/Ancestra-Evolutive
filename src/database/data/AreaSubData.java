package database.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.SubArea;

import common.World;

import core.Console;
import database.AbstractDAO;

public class AreaSubData extends AbstractDAO<SubArea> {

	public AreaSubData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(SubArea obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(SubArea obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(SubArea obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SubArea load(int id) {
		SubArea subArea = null;
		try {
			ResultSet result = super.getData("SELECT * FROM subarea_data WHERE id = "+id);
			
			if(result.next()) {
				subArea = new SubArea(result.getInt("id"),
						result.getInt("area"),
						result.getInt("alignement"),
						result.getString("name"));
				World.data.addSubArea(subArea);
				//on ajoute la sous zone a la zone
				if(subArea.get_area() != null)
					subArea.get_area().addSubArea(subArea);
			}
			closeResultSet(result);
		} catch(Exception e) {
			Console.instance.writeln("SQL ERROR(AreaSubData): "+e.getMessage());
		}
		return subArea;
	}
}
