package tool.command;

public abstract class CommandAccess<T> {
	public abstract boolean authorizes(T t);
	public abstract String getRequiertsMessage();
}