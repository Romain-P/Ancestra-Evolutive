package game;

public class GameAction
{   
	public int _id;
	public int _actionID;
	public String _packet;
	public String _args;
	
	public GameAction(int aId, int aActionId,String aPacket) {
		_id = aId;
		_actionID = aActionId;
		_packet = aPacket;
	}
}