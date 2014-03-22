package database.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.IOTemplate;

import common.World;

import core.Console;
import database.AbstractDAO;

public class IOTemplateData extends AbstractDAO<IOTemplate>{

	public IOTemplateData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(IOTemplate obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(IOTemplate obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(IOTemplate obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IOTemplate load(int id) {
		IOTemplate template = null;
		try {
			ResultSet result = getData("SELECT * FROM interactive_objects_data WHERE id = "+id);
			
			if(result.next()) {
				template = new IOTemplate(result.getInt("id"), result
						.getInt("respawn"), result.getInt("duration"), result
						.getInt("unknow"), result.getInt("walkable") == 1);
				World.data.addIOTemplate(template);
			}
			
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(IOTemplateData): "+e.getMessage());
		}
		return template;
	}

}
