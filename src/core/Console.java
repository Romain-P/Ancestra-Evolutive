package core;

import java.util.NoSuchElementException;
import java.util.Scanner;

import objects.Personnage;
import tool.command.CommandParser;

public class Console extends Thread{
	public static Console instance;
	private Scanner scanner = new Scanner(System.in);
	
	public void initialize() {
		super.setDaemon(true);
		super.start();
	}
	
	@Override
	public void run() {
		while(Server.config.isRunning()) {
			try {
				write("\nConsole > ");
				String line = scanner.next();
				CommandParser.parse(line, this);
			} catch (NoSuchElementException ignored) { }
		}
		super.interrupt();
	}
	
	public void println(String string) {
		if(Server.config.isDebug())
			System.out.println(string);
		Log.addToLog(string);
	}
	
	public void print(String string) {
		if(Server.config.isDebug())
			System.out.print(string);
		Log.addToLog(string);
	}
	
	public void write(String string) {
		System.out.print(string);
		Log.addToLog(string);
	}
	
	public void writeln(String string) {
		System.out.println(string);
		Log.addToLog(string);
	}
	
	public void print(String string, Object t) {
		if(t instanceof Personnage)
			((Personnage)t).sendText(string);
		else if (t instanceof Console)
			write(string);
	}
}