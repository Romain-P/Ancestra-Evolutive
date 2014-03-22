package realm;

import objects.Compte;

import org.apache.mina.core.session.IoSession;

import client.Client;

import common.Constants;
import common.SocketManager;
import common.World;

import core.Server;
import enums.EmulatorInfos;

public class RealmClient implements Client{
	private String _hashKey;
	private int _packetNum = 0;
	private String _accountName;
	private String _hashPass;
	private Compte _compte;
	private IoSession session;
	
	public RealmClient(IoSession session) {
		this.setSession(session);
	}
	
	public void parsePacket(String packet) {
		switch(_packetNum)
		{
			case 1://Version
				if(!packet.equalsIgnoreCase(EmulatorInfos.CLIENT_RELESE.toString()) && !Constants.IGNORE_VERSION)
				{
					SocketManager.REALM_SEND_REQUIRED_VERSION(this);
					try {
						this.kick();
					} catch (Exception e) {}
				}
				break;
			case 2://Account Name
				_accountName = packet.toLowerCase();
				break;
			case 3://HashPass
				if(!packet.substring(0, 2).equalsIgnoreCase("#1"))
				{
					try {
						this.kick();
					} catch (Exception e) {}
				}
				_hashPass = packet;
				
				if(Compte.COMPTE_LOGIN(_accountName,_hashPass,get_hashKey()))
				{
					_compte = World.data.getCompteByName(_accountName);
					
					if(_compte.isLogged()) {
						if(_compte.getRealmThread() != null)
							_compte.getRealmThread().kick();
						else if(_compte.getGameClient() != null) 
							_compte.getGameClient().kick();
						
						if(!_compte.isLogged()) {
							SocketManager.REALM_SEND_ALREADY_CONNECTED(this);
							kick();
							return;
						} else {
							World.data.getAccounts().remove(_compte.get_GUID());
							_compte = World.database.getAccountData().loadByName(_accountName);
						}
					}
					
					if(_compte.isBanned())
					{
						SocketManager.REALM_SEND_BANNED(this);
						try {
							kick();
						} catch (Exception e) {}
						return;
					}
					if(Server.config.getPlayerLimitOnServer() != -1 && Server.config.getPlayerLimitOnServer() <= Server.config.getGameServer().getPlayerNumber())
					{
						//Seulement si joueur
						if(_compte.get_gmLvl() == 0  && _compte.get_vip() == 0)
						{
							SocketManager.REALM_SEND_TOO_MANY_PLAYER_ERROR(this);
							try {
								kick();
							} catch (Exception e) {}
							return;
						}
					}
					if(World.data.getGmAccess() > _compte.get_gmLvl())
					{
						SocketManager.REALM_SEND_TOO_MANY_PLAYER_ERROR(this);
						return;
					}
					String ip = Constants.getIp(session.getRemoteAddress().toString());
					if(Constants.IPcompareToBanIP(ip))
					{
						SocketManager.REALM_SEND_BANNED(this);
						return;
					}
					//Verification Multi compte
					if(!Server.config.isMultiAccount())
					{
						if(World.data.ipIsUsed(ip))
						{
							SocketManager.REALM_SEND_TOO_MANY_PLAYER_ERROR(this);
							try {
								kick();
							} catch (Exception e) {}
							return;
						}
					}
					_compte.setRealmThread(this);
					_compte.setCurIP(ip);
					_compte.setLogged(true);
					World.database.getAccountData().update(_compte);
					SocketManager.REALM_SEND_Ad_Ac_AH_AlK_AQ_PACKETS(this, _compte.get_pseudo(),(_compte.get_gmLvl()>0?(1):(0)), _compte.get_question() ); 
				}else//Si le compte n'a pas �t� reconnu
				{
					SocketManager.REALM_SEND_LOGIN_ERROR(this);
					try {
						kick();
					} catch (Exception e) {}
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
					_compte = World.data.getCompteByName(_accountName);
					SocketManager.REALM_SEND_PERSO_LIST(this, _compte.GET_PERSO_NUMBER());
				}else
				if(packet.equals("AX1"))
				{
					Server.config.getGameServer().addWaitingCompte(_compte);
					String ip = _compte.get_curIP();
					SocketManager.REALM_SEND_GAME_SERVER_IP(this, _compte.get_GUID(),ip.equals("127.0.0.1"));
				}
				break;
		}
	}
	
	public void kick() {
		_compte.setLogged(false);
		World.database.getAccountData().update(_compte);
		
		if(!session.isClosing())
			session.close(true);
		Server.config.getRealmServer().getClients().remove(session.getId());
		_compte.setRealmThread(null);
	}

	public String get_hashKey() {
		return _hashKey;
	}

	public void set_hashKey(String _hashKey) {
		this._hashKey = _hashKey;
	}

	public IoSession getSession() {
		return session;
	}

	public void setSession(IoSession session) {
		this.session = session;
	}

	public void addPacket() {
		this._packetNum++;
	}
}
