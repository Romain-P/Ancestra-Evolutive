package database.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import objects.job.Job;

import common.World;

import core.Console;
import database.AbstractDAO;

public class JobData extends AbstractDAO<Job>{

	public JobData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean create(Job obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(Job obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Job obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Job load(int id) {
		Job job = null;
		try {
			ResultSet result = getData("SELECT * FROM jobs_data WHERE id = "+id);
			
			if(result.next()) {
				job = new Job(result.getInt("id"), result
						.getString("tools"), result.getString("crafts"));
				World.data.addJob(job);
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(JobData): "+e.getMessage());
		}
		return job;
	}
}
