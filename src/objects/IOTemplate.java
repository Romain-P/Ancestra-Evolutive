package objects;

public class IOTemplate
{
	private int _id;
	private int _respawnTime;
	private int _duration;
	private int _unk;
	private boolean _walkable;
	
	public IOTemplate(int a_i,int a_r,int a_d,int a_u, boolean a_w)
	{
		_id = a_i;
		_respawnTime = a_r;
		_duration = a_d;
		_unk = a_u;
		_walkable = a_w;
	}
	
	public int getId() {
		return _id;
	}	
	public boolean isWalkable() {
		return _walkable;
	}

	public int getRespawnTime() {
		return _respawnTime;
	}
	public int getDuration() {
		return _duration;
	}
	public int getUnk() {
		return _unk;
	}
}