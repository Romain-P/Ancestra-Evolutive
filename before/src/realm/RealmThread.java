package realm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import objects.Compte;

import common.Ancestra;
import common.Constants;
import common.SQLManager;
import common.SocketManager;
import common.World;

public class RealmThread implements Runnable{
	private BufferedReader _in;
	private Thread _t;
	public PrintWriter _out;
	private Socket _s;
	private String _hashKey;
	private int _packetNum = 0;
	private String _accountName;
	private String _hashPass;
	private Compte _compte;
	
	public RealmThread(Socket sock)
	{
		try
		{
			_s = sock;
			_in = new BufferedReader(new InputStreamReader(_s.getInputStream()));
			_out = new PrintWriter(_s.getOutputStream());
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		}
		catch(IOException e)
		{
			try {
				if(!_s.isClosed())_s.close();
			} catch (IOException e1) {}
		}
		finally
		{
			if(_compte != null)
			{
				_compte.setRealmThread(null);
				_compte.setGameThread(null);
				_compte.setCurIP("");
			}
		}
	}

	public void run()
	{
		try
    	{
			String packet = "";
			char charCur[] = new char[1];
			if(Ancestra.CONFIG_POLICY)
				SocketManager.REALM_SEND_POLICY_FILE(_out);
	        
			_hashKey = SocketManager.REALM_SEND_HC_PACKET(_out);
	        
	    	while(_in.read(charCur, 0, 1)!=-1 && Ancestra.isRunning)
	    	{
	    		if (charCur[0] != '\u0000' && charCur[0] != '\n' && charCur[0] != '\r')
		    	{
	    			packet += charCur[0];
		    	}else if(!packet.isEmpty())
		    	{
		    		RealmServer.addToSockLog("Realm: Recv << "+packet);
		    		_packetNum++;
		    		parsePacket(packet);
		    		packet = "";
		    	}
	    	}
    	}catch(IOException e)
    	{
    		try
    		{
	    		_in.close();
	    		_out.close();
	    		if(_compte != null)
	    		{
	    			_compte.setCurPerso(null);
	    			_compte.setGameThread(null);
	    			_compte.setRealmThread(null);
	    			_compte.setCurIP("");
	    		}
	    		if(!_s.isClosed())_s.close();
	    		_t.interrupt();
	    	}catch(IOException e1){};
    	}
    	finally
    	{
    		try
    		{
	    		_in.close();
	    		_out.close();
	    		if(_compte != null)
	    		{
	    			_compte.setCurPerso(null);
	    			_compte.setGameThread(null);
	    			_compte.setRealmThread(null);
	    			_compte.setCurIP("");
	    		}
	    		if(!_s.isClosed())_s.close();
	    		_t.interrupt();
	    	}catch(IOException e1){};
    	}
	}
	
	private void parsePacket(String packet)
	{
		switch(_packetNum)
		{
			case 1://Version
				if(!packet.equalsIgnoreCase(Constants.CLIENT_VERSION) && !Constants.IGNORE_VERSION)
				{
					SocketManager.REALM_SEND_REQUIRED_VERSION(_out);
					try {
						this._s.close();
					} catch (IOException e) {}
				}
				break;
			case 2://Account Name
				_accountName = packet.toLowerCase();
				break;
			case 3://HashPass
				if(!packet.substring(0, 2).equalsIgnoreCase("#1"))
				{
					try {
						this._s.close();
					} catch (IOException e) {}
				}
				_hashPass = packet;
				
				if(Compte.COMPTE_LOGIN(_accountName,_hashPass,_hashKey))
				{
					_compte = World.getCompteByName(_accountName);
					if(_compte.isOnline() && _compte.getGameThread() != null)
					{
						_compte.getGameThread().closeSocket();
					}else if(_compte.isOnline() && _compte.getGameThread() == null)
					{
						SocketManager.REALM_SEND_ALREADY_CONNECTED(_out);
						SocketManager.REALM_SEND_ALREADY_CONNECTED(_compte.getRealmThread()._out);
						return;
					}
					if(_compte.isBanned())
					{
						SocketManager.REALM_SEND_BANNED(_out);
						try {
							_s.close();
						} catch (IOException e) {}
						return;
					}
					if(Ancestra.CONFIG_PLAYER_LIMIT != -1 && Ancestra.CONFIG_PLAYER_LIMIT <= Ancestra.gameServer.getPlayerNumber())
					{
						//Seulement si joueur
						if(_compte.get_gmLvl() == 0  && _compte.get_vip() == 0)
						{
							SocketManager.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
							try {
								_s.close();
							} catch (IOException e) {}
							return;
						}
					}
					if(World.getGmAccess() > _compte.get_gmLvl())
					{
						SocketManager.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
						return;
					}
					String ip = _s.getInetAddress().getHostAddress();
					if(Constants.IPcompareToBanIP(ip))
					{
						SocketManager.REALM_SEND_BANNED(_out);
						return;
					}
					//Verification Multi compte
					if(!Ancestra.CONFIG_ALLOW_MULTI)
					{
						if(World.ipIsUsed(ip))
						{
							SocketManager.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
							try {
								_s.close();
							} catch (IOException e) {}
							return;
						}
					}
					_compte.setRealmThread(this);
					_compte.setCurIP(ip);
					RealmServer._totalAbo++;//On incrémente le total
					_compte._position = RealmServer._totalAbo;//On lui donne une position
					SocketManager.REALM_SEND_Ad_Ac_AH_AlK_AQ_PACKETS(_out, _compte.get_pseudo(),(_compte.get_gmLvl()>0?(1):(0)), _compte.get_question() ); 
				}else//Si le compte n'a pas été reconnu
				{
					SQLManager.LOAD_ACCOUNT_BY_USER(_accountName);
					if(Compte.COMPTE_LOGIN(_accountName,_hashPass,_hashKey))
					{
						_compte = World.getCompteByName(_accountName);
						if(_compte.isOnline() && _compte.getGameThread() != null)
						{
							_compte.getGameThread().closeSocket();
						}else if(_compte.isOnline() && _compte.getGameThread() == null)
						{
							SocketManager.REALM_SEND_ALREADY_CONNECTED(_out);
							SocketManager.REALM_SEND_ALREADY_CONNECTED(_compte.getRealmThread()._out);
							return;
						}
						if(_compte.isBanned())
						{
							SocketManager.REALM_SEND_BANNED(_out);
							try {
								this._s.close();
							} catch (IOException e) {}
							return;
						}
						if(Ancestra.CONFIG_PLAYER_LIMIT != -1 && Ancestra.CONFIG_PLAYER_LIMIT <= Ancestra.gameServer.getPlayerNumber())
						{
							//Seulement si joueur
							if(_compte.get_gmLvl() == 0  && _compte.get_vip() == 0)
							{
								SocketManager.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
								try {
									_s.close();
								} catch (IOException e) {}
								return;
							}
						}
						if(World.getGmAccess() > _compte.get_gmLvl())
						{
							SocketManager.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
							return;
						}
						String ip = _s.getInetAddress().getHostAddress();
						if(Constants.IPcompareToBanIP(ip))
						{
							SocketManager.REALM_SEND_BANNED(_out);
							return;
						}
						//Verification Multi compte
						if(!Ancestra.CONFIG_ALLOW_MULTI)
						{
							if(World.ipIsUsed(ip))
							{
								SocketManager.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
								try {
									_s.close();
								} catch (IOException e) {}
								return;
							}
						}
						_compte.setCurIP(ip);
						_compte.setRealmThread(this);
						RealmServer._totalAbo++;//On incrémente le total
						_compte._position = RealmServer._totalAbo;//On lui donne une position
						SocketManager.REALM_SEND_Ad_Ac_AH_AlK_AQ_PACKETS(_out, _compte.get_pseudo(),(_compte.get_gmLvl()>0?(1):(0)), _compte.get_question() ); 
					}else//Si le compte n'a pas été reconnu
					{
						SocketManager.REALM_SEND_LOGIN_ERROR(_out);
						try {
							this._s.close();
						} catch (IOException e) {}
					}
				}
				break;
			default:
				if(packet.substring(0,2).equals("Af"))
				{
					_packetNum--;
					Pending.PendingSystem(_compte);
				}else
				if(packet.substring(0,2).equals("Ax"))
				{
					if(_compte == null)return;
					SQLManager.LOAD_PERSO_BY_ACCOUNT(_compte.get_GUID());
					SocketManager.REALM_SEND_PERSO_LIST(_out, _compte.GET_PERSO_NUMBER());
				}else
				if(packet.equals("AX1"))
				{
					Ancestra.gameServer.addWaitingCompte(_compte);
					String ip = _compte.get_curIP();
					SocketManager.REALM_SEND_GAME_SERVER_IP(_out, _compte.get_GUID(),ip.equals("127.0.0.1"));
				}
				break;
		}
	}
}
