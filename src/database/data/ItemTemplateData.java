package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Action;
import objects.Objet.ObjTemplate;

import common.World;

import core.Console;
import database.AbstractDAO;

public class ItemTemplateData extends AbstractDAO<ObjTemplate>{

	public ItemTemplateData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(ObjTemplate obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(ObjTemplate obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(ObjTemplate obj) {
		try {
			String baseQuery = "UPDATE `item_template`"
					+ " SET sold = ?, avgPrice = ?" + " WHERE id = ?";
			PreparedStatement statement = connection.prepareStatement(baseQuery);

			statement.setLong(1, obj.getSold());
			statement.setInt(2, obj.getAvgPrice());
			statement.setInt(3, obj.getID());
			
			execute(statement);
			
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(ItemTemplateData): "+e.getMessage());
		}
		return false;
	}

	@Override
	public ObjTemplate load(int id) {
		ObjTemplate template = null;
		try {
			ResultSet result = getData("SELECT * FROM item_template WHERE id = "+id);
			
			if(result.next()) {
				template = new ObjTemplate(result.getInt("id"), result
						.getString("statsTemplate"), result.getString("name"), result
						.getInt("type"), result.getInt("level"), result.getInt("pod"),
						result.getInt("prix"), result.getInt("panoplie"), result
								.getString("condition"), result
								.getString("armesInfos"), result.getInt("sold"), result
								.getInt("avgPrice"));
				World.data.addObjTemplate(template);
				loadUseAction(id);
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(ItemTemplateData): "+e.getMessage());
		}
		return template;
	}
	
	public void loadUseAction(int item) {
		try {
			ResultSet result = getData("SELECT * FROM use_item_actions WHERE template = "+item);
			while (result.next()) {
				int id = result.getInt("template");
				int type = result.getInt("type");
				String args = result.getString("args");
				if (World.data.getObjTemplate(id) == null)
					continue;
				World.data.getObjTemplate(id).addAction(
						new Action(type, args, ""));
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(ItemTemplateData): "+e.getMessage());
		}
	}
}
