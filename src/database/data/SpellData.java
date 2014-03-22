package database.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.Sort;
import objects.Sort.SortStats;

import common.World;

import core.Console;
import database.AbstractDAO;

public class SpellData extends AbstractDAO<Sort>{

	public SpellData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Sort obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Sort obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Sort obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Sort load(int id) {
		Sort sort = null;
		try {
			String query = "SELECT * FROM sorts WHERE id = "+id; //TODO
			ResultSet result = getData(query);
			if(result.next()) {
				sort = new Sort(id, result.getInt("sprite"),
						result.getString("spriteInfos"),
						result.getString("effectTarget"));
				SortStats l1 = sort.parseSortStats(id, 1, result.getString("lvl1"));
				SortStats l2 = sort.parseSortStats(id, 2, result.getString("lvl2"));
				SortStats l3 = sort.parseSortStats(id, 3, result.getString("lvl3"));
				SortStats l4 = sort.parseSortStats(id, 4, result.getString("lvl4"));
				SortStats l5 = null;
				if (!result.getString("lvl5").equalsIgnoreCase("-1"))
					l5 = sort.parseSortStats(id, 5, result.getString("lvl5"));
				SortStats l6 = null;
				if (!result.getString("lvl6").equalsIgnoreCase("-1"))
					l6 = sort.parseSortStats(id, 6, result.getString("lvl6"));
				sort.addSortStats(1, l1);
				sort.addSortStats(2, l2);
				sort.addSortStats(3, l3);
				sort.addSortStats(4, l4);
				sort.addSortStats(5, l5);
				sort.addSortStats(6, l6);
				World.data.addSort(sort);
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(SpellData): "+e.getMessage());
		}
		return sort;
	}
}
