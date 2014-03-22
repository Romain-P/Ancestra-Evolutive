package objects;

public class ExpLevel
{
	public long perso;
	public int metier;
	public int dinde;
	public int pvp;
	public long guilde;
	
	public ExpLevel(long c, int m, int d, int p)
	{
		perso = c;
		metier = m;
		dinde = d;
		pvp = p;
		guilde = perso*10;
	}
	
}