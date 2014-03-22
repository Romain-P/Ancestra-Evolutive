package tool.command;

import java.util.HashMap;
import java.util.Map;

import objects.Personnage;
import tool.time.restricter.TimeRestricter;



import core.Console;

public abstract class Command<T>{
	private String name;
	private Map<String, Parameter<T>> parameters = new HashMap<>();
	private CommandGroupAccess<T> commandGroupAccess = new CommandGroupAccess<T>();
	private StringBuilder successMessages = new StringBuilder();
	private TimeRestricter restricter;
	
	public abstract void action(T t);
	
	public Command(String name) {
		this.name = name.toLowerCase();
	}
	
	public Parameter<T> addParameter(Parameter<T> parameter) {
		this.getParameters().put(parameter.getName(), parameter);
		return parameter;
	}
	
	public void addAccess(CommandAccess<T> access) {
		this.commandGroupAccess.addAccess(access);
	}
	
	public void addSuccessMessage(String message) {
		this.successMessages.append(message).append("\n");
	}
	
	public void execute(T t) {
		if(this.commandGroupAccess.authorizes(t)) {
			if(t instanceof Personnage && 
					this.restricter != null && !this.restricter.authorizes((Personnage)t))
				return;
			this.action(t);
			Console.instance.print(this.successMessages.toString(), t);
		}
	}
	
	public TimeRestricter attachRestricter(TimeRestricter restricter) {
		this.restricter = restricter;
		return restricter;
	}

	public String getName() {
		return name;
	}

	public Map<String, Parameter<T>> getParameters() {
		return parameters;
	}
}
