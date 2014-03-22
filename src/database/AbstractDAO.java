package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.locks.ReentrantLock;

import core.Console;

public abstract class AbstractDAO<T> implements DAO<T>{
	
	protected final Connection connection;
	private ReentrantLock locker;
	  
	public AbstractDAO(Connection connection, ReentrantLock locker) {
		this.connection = connection;
		this.locker = locker;
	}
	
	protected void execute(String query) {
		locker.lock();
		//on stop l'auto commit
	    try {
			connection.setAutoCommit(false);
		} catch (Exception e1) { 
			Console.instance.writeln(" > SQL ERROR: "+e1.getMessage());
		}
	    
	    //on execute la query
	    try {
	        connection.createStatement().execute(query);
	        connection.commit();
	    } catch (Exception e) {
	    	Console.instance.writeln(" > SQL ERROR: "+e.getMessage()+" :"+query); 
	        try {
				connection.rollback();
			} catch (Exception e1) {
				Console.instance.writeln(" > SQL ERROR: "+e1.getMessage()); 
			}
	    } finally {
	    	//on relance l'auto commit
	        try {
				connection.setAutoCommit(true);
			} catch (Exception e) {
				Console.instance.writeln(" > SQL ERROR: "+e.getMessage());
			}
	        locker.unlock();
	    }
	}
	
	protected void execute(PreparedStatement statement) {
		locker.lock();
		//on stop l'auto commit
	    try {
			connection.setAutoCommit(false);
		} catch (Exception e1) { 
			Console.instance.writeln(" > SQL ERROR: "+e1.getMessage());
		}
	    
	    //on execute la query
	    try {
	        statement.execute();
	        closeStatement(statement);
	        connection.commit();
	    } catch (Exception e) {
	    	Console.instance.writeln(" > SQL ERROR: "+e.getMessage()); 
	        try {
				connection.rollback();
			} catch (Exception e1) {
				Console.instance.writeln(" > SQL ERROR: "+e1.getMessage()); 
			}
	    } finally {
	    	//on relance l'auto commit
	        try {
				connection.setAutoCommit(true);
			} catch (Exception e) {
				Console.instance.writeln(" > SQL ERROR: "+e.getMessage());
			}
	        locker.unlock();
	    }
	}
	
	protected ResultSet getData(String query) {
		locker.lock();
		//on stop l'auto commit
	    try {
			connection.setAutoCommit(false);
		} catch (Exception e1) { 
			Console.instance.writeln(" > SQL ERROR: "+e1.getMessage());
		}
	    
	    //on execute la query
	    try {
	        ResultSet result = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
	                                     .executeQuery(query);
	        connection.commit();
	        return result;
	    } catch (Exception e) {
	    	Console.instance.writeln(" > SQL ERROR: "+e.getMessage()+" :"+query); 
	        try {
				connection.rollback();
			} catch (Exception e1) {
				Console.instance.writeln(" > SQL ERROR: "+e1.getMessage()); 
			}
	        return null;
	    } finally {
	    	//on relance l'auto commit
	        try {
				connection.setAutoCommit(true);
			} catch (Exception e) {
				Console.instance.writeln(" > SQL ERROR: "+e.getMessage());
			}
	        locker.unlock();
	    }
	}
	
	protected void closeResultSet(ResultSet result) {
		try {
			result.getStatement().close();
			result.close();
		} catch (Exception e) {
			Console.instance.writeln(e.getMessage());
		}
	}
	
	protected void closeStatement(PreparedStatement statement) {
		try {
			statement.clearParameters();
	        statement.close();
		} catch (Exception e) {
			Console.instance.writeln(e.getMessage());
		}
	}
}
