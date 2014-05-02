package tool.command;

import java.util.Deque;
import java.util.LinkedList;

import objects.Personnage;

import common.World;

import core.Console;

public class CommandParser {

	public static void parse(String line, Object t) {
		String name;
		String[] parameters = null;
		
		try {
			 String[] split = line.contains(" ") 
					 ? line.split(" ") 
					 : new String[] { line };
			 
			 name = split[0];
			 
			 if(split.length > 1) {
				 line = line.substring(name.length()+1);
				 parameters = line.contains(" ") 
						 ? line.split(" ")
						 : new String[] { line };
			 }
		} catch(Exception e) {
			Console.instance.print("Erreur de syntaxe", t);
			return; 
		}
		
		if(t instanceof Personnage) {
			Command<Personnage> command = World.data.getPlayerCommands().get(name);
			
			if(command == null) {
				Console.instance.print("Commande non reconnue", t);
				return; 
			}
			
			if(parameters != null) {
				Deque<String> params = new LinkedList<>();
				for(String param: parameters)
					params.addLast(param);
				
				Parameter<Personnage> lastParameter = null;
				
				while(!params.isEmpty()) {
                    String param = params.pop();
					Parameter<Personnage> temporary = command.getParameters().get(param);
					if(temporary == null) {
						if(lastParameter != null) {
                            params.addFirst(param);
							lastParameter.action((Personnage)t, (String[])params.toArray());
                        } else
							command.execute((Personnage)t, (String[])params.toArray());
					} else
						lastParameter = temporary;
				}
			} else 
				command.execute((Personnage)t, parameters);
		} 
		else if (t instanceof Console) {
			Command<Console> command = World.data.getConsoleCommands().get(name);
			
			if(command == null) {
				Console.instance.print("Commande non reconnue", t);
				return; 
			}
			
			if(parameters != null) {
				Deque<String> params = new LinkedList<>();
				for(String param: parameters)
					params.addLast(param);
				
				Parameter<Console> lastParameter = null;
				
				while(params.isEmpty()) {
					Parameter<Console> temporary = command.getParameters().get(params.pop());
					if(temporary == null) {
						if(lastParameter != null)
							lastParameter.action((Console)t, (String[])params.toArray());
						else
							command.execute((Console)t, (String[])params.toArray());
					} else
						lastParameter = temporary;
				}
			} else 
				command.execute((Console)t, parameters);
		}
	}

}
