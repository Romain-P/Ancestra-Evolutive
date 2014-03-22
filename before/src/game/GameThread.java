package game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;


import objects.*;
import objects.Carte.*;
import objects.Fight.Fighter;
import objects.Guild.GuildMember;
import objects.HDV.HdvEntry;
import objects.Metier.StatsMetier;
import objects.Personnage;
import objects.NPC_tmpl.*;
import objects.Objet.ObjTemplate;
import objects.Personnage.Group;
import objects.Sort.SortStats;
import common.*;

public class GameThread implements Runnable
{
	private BufferedReader _in;
	private Thread _t;
	private PrintWriter _out;
	private Socket _s;
	private Compte _compte;
	private Personnage _perso;
	private Map<Integer,GameAction> _actions = new TreeMap<Integer,GameAction>();
	private long _timeLastTradeMsg = 0, _timeLastRecrutmentMsg = 0, _timeLastsave = 0, _timeLastAlignMsg = 0;
	
	private Commands command;
	
	public static class GameAction
	{
		public int _id;
		public int _actionID;
		public String _packet;
		public String _args;
		
		public GameAction(int aId, int aActionId,String aPacket)
		{
			_id = aId;
			_actionID = aActionId;
			_packet = aPacket;
		}
	}
	
	public GameThread(Socket sock)
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
				GameServer.addToLog(e.getMessage());
				if(!_s.isClosed())_s.close();
			} catch (IOException e1) {e1.printStackTrace();}
		}
	}
	
	public void run()
	{
		try
    	{
			String packet = "";
			char charCur[] = new char[1];
			SocketManager.GAME_SEND_HELLOGAME_PACKET(_out);
	    	while(_in.read(charCur, 0, 1)!=-1 && Ancestra.isRunning)
	    	{
	    		if (charCur[0] != '\u0000' && charCur[0] != '\n' && charCur[0] != '\r')
		    	{
	    			packet += charCur[0];
		    	}else if(!packet.isEmpty())
		    	{
		    		packet = CryptManager.toUnicode(packet);
		    		GameServer.addToSockLog("Game: Recv << "+packet);
		    		parsePacket(packet);
		    		packet = "";
		    	}
	    	}
    	}catch(IOException e)
    	{
    		try
    		{
    			GameServer.addToLog(e.getMessage());
	    		_in.close();
	    		_out.close();
	    		if(_compte != null)
	    		{
	    			_compte.setCurPerso(null);
	    			_compte.setGameThread(null);
	    			_compte.setRealmThread(null);
	    		}
	    		if(!_s.isClosed())_s.close();
	    	}catch(IOException e1){e1.printStackTrace();};
    	}catch(Exception e)
    	{
    		e.printStackTrace();
    		GameServer.addToLog(e.getMessage());
    	}
    	finally
    	{
    		kick();
    	}
	}

	private void parsePacket(String packet)
	{
		if(_perso != null) {
			_perso.refreshLastPacketTime();
		}
		
		if(packet.length()>3 && packet.substring(0,4).equalsIgnoreCase("ping"))
		{
			SocketManager.GAME_SEND_PONG(_out);
			return;
		}
		if(packet.length()>4 && packet.substring(0,5).equalsIgnoreCase("qping"))
		{
			SocketManager.GAME_SEND_QPONG(_out);
			return;
		}
		
		switch(packet.charAt(0))
		{
			case 'A':
				parseAccountPacket(packet);
			break;
			case 'B':
				parseBasicsPacket(packet);
			break;
			case 'c':
				parseChanelPacket(packet);
			break;
			case 'D':
				parseDialogPacket(packet);
			break;
			case 'E':
				parseExchangePacket(packet);
			break;
			case 'e':
				parse_environementPacket(packet);
			break;
			case 'F':
				parse_friendPacket(packet);
			break;
			case 'f':
				parseFightPacket(packet);
			break;
			case 'G':
				parseGamePacket(packet);
			break;
			case 'g':
				parseGuildPacket(packet);
			break;
			case 'h':
				parseHousePacket(packet);
			break;
			case 'i':
				parse_enemyPacket(packet);
			break;
			case 'K':
				parseHouseKodePacket(packet);
			break;
			case 'O':
				parseObjectPacket(packet);
			break;
			case 'P':
				parseGroupPacket(packet);
			break;
			case 'R':
				parseMountPacket(packet);
			break;
			case 'S':
				parseSpellPacket(packet);
			break;
			case 'W':
				parseWaypointPacket(packet);
			break;
		}
	}
	
	private void parseHousePacket(String packet)
	{
		switch(packet.charAt(1))
		{
		case 'B'://Acheter la maison
			packet = packet.substring(2);
			House.HouseAchat(_perso);
		break;
		case 'G'://Maison de guilde
			packet = packet.substring(2);
			if(packet.isEmpty()) packet = null;
			House.parseHG(_perso, packet);
		break;
		case 'Q'://Quitter/Expulser de la maison
			packet = packet.substring(2);
			House.Leave(_perso, packet);
		break;
		case 'S'://Modification du prix de vente
			packet = packet.substring(2);
			House.SellPrice(_perso, packet);
		break;
		case 'V'://Fermer fenetre d'achat
			House.closeBuy(_perso);
		break;
		}
	}
	
	private void parseHouseKodePacket(String packet)
	{
		switch(packet.charAt(1))
		{
		case 'V'://Fermer fenetre du code
			House.closeCode(_perso);
		break;
		case 'K'://Envoi du code
			House_code(packet);
		break;
		}
	}
	
	private void House_code(String packet)
	{
		switch(packet.charAt(2))
		{
		case '0'://Envoi du code
			packet = packet.substring(4);
			if(_perso.getInTrunk() != null)
					Trunk.OpenTrunk(_perso, packet, false);
				else
					House.OpenHouse(_perso, packet, false);
		break;
		case '1'://Changement du code
			packet = packet.substring(4);
			if(_perso.getInTrunk() != null)
				Trunk.LockTrunk(_perso, packet);
			else
			    House.LockHouse(_perso, packet);
		break;
		}
	}
	
	private void parse_enemyPacket(String packet)
	{
		switch(packet.charAt(1))
		{
		case 'A'://Ajouter
			Enemy_add(packet);
		break;
		case 'D'://Delete
			Enemy_delete(packet);
		break;
		case 'L'://Liste
			SocketManager.GAME_SEND_ENEMY_LIST(_perso);
		break;
		}
	}
	
	private void Enemy_add(String packet)
	{
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://Nom de perso
				packet = packet.substring(3);
				Personnage P = World.getPersoByName(packet);
				if(P == null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
				
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Compte C = World.getCompteByPseudo(packet);
				if(C==null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				Personnage Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1)
		{
			SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
			return;
		}
		_compte.addEnemy(packet, guid);
	}

	private void Enemy_delete(String packet)
	{
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://Nom de perso
				packet = packet.substring(3);
				Personnage P = World.getPersoByName(packet);
				if(P == null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
				
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Compte C = World.getCompteByPseudo(packet);
				if(C==null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				Personnage Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1 || !_compte.isEnemyWith(guid))
		{
			SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
			return;
		}
		_compte.removeEnemy(guid);
	}
	
	private void parseWaypointPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'U'://Use
				Waypoint_use(packet);
			break;
			case 'u'://use zaapi
				Zaapi_use(packet);
			break;
			case 'v'://quitter zaapi
				Zaapi_close();
			break;
			case 'V'://Quitter
				Waypoint_quit();
			break;
		}
	}

	private void Zaapi_close()
	{
		_perso.Zaapi_close();
	}
	
	private void Zaapi_use(String packet)
	{
		if(_perso.getDeshonor() >= 2) 
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
			return;
		}
		_perso.Zaapi_use(packet);
	}
	
	private void Waypoint_quit()
	{
		_perso.stopZaaping();
	}

	private void Waypoint_use(String packet)
	{
		short id = -1;
		try
		{
			id = Short.parseShort(packet.substring(2));
		}catch(Exception e){};
		if( id == -1)return;
		_perso.useZaap(id);
	}
	private void parseGuildPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'B'://Stats
				if(_perso.get_guild() == null)return;
				Guild G = _perso.get_guild();
				if(!_perso.getGuildMember().canDo(Constants.G_BOOST))return;
				switch(packet.charAt(2))
				{
					case 'p'://Prospec
						if(G.get_Capital() < 1)return;
						if(G.get_Stats(176) >= 500)return;
						G.set_Capital(G.get_Capital()-1);
						G.upgrade_Stats(176, 1);
					break;
					case 'x'://Sagesse
						if(G.get_Capital() < 1)return;
						if(G.get_Stats(124) >= 400)return;
						G.set_Capital(G.get_Capital()-1);
						G.upgrade_Stats(124, 1);
					break;
					case 'o'://Pod
						if(G.get_Capital() < 1)return;
						if(G.get_Stats(158) >= 5000)return;
						G.set_Capital(G.get_Capital()-1);
						G.upgrade_Stats(158, 20);
					break;
					case 'k'://Nb Perco
						if(G.get_Capital() < 10)return;
						if(G.get_nbrPerco() >= 50)return;
						G.set_Capital(G.get_Capital()-10);
						G.set_nbrPerco(G.get_nbrPerco()+1);
					break;
				}
				SQLManager.UPDATE_GUILD(G);
				SocketManager.GAME_SEND_gIB_PACKET(_perso, _perso.get_guild().parsePercotoGuild());
			break;
			case 'b'://Sorts
				if(_perso.get_guild() == null)return;
				Guild G2 = _perso.get_guild();
				if(!_perso.getGuildMember().canDo(Constants.G_BOOST))return;
				int spellID = Integer.parseInt(packet.substring(2));
				if(G2.getSpells().containsKey(spellID))
				{
					if(G2.get_Capital() < 5)return;
					G2.set_Capital(G2.get_Capital()-5);
					G2.boostSpell(spellID);
					SQLManager.UPDATE_GUILD(G2);
					SocketManager.GAME_SEND_gIB_PACKET(_perso, _perso.get_guild().parsePercotoGuild());
				}else
				{
					GameServer.addToLog("[ERROR]Sort "+spellID+" non trouve.");
				}
			break;
			case 'C'://Creation
				guild_create(packet);
			break;
			case 'f'://Téléportation enclo de guilde
				guild_enclo(packet.substring(2));
			break;
			case 'F'://Retirer percepteur
				guild_remove_perco(packet.substring(2));
			break;
			case 'h'://Téléportation maison de guilde
				guild_house(packet.substring(2));
			break;
			case 'H'://Poser un percepteur
				guild_add_perco();
			break;
			case 'I'://Infos
				guild_infos(packet.charAt(2));
			break;
			case 'J'://Join
				guild_join(packet.substring(2));
			break;
			case 'K'://Kick
				guild_kick(packet.substring(2));
			break;
			case 'P'://Promote
				guild_promote(packet.substring(2));
			break;
			case 'T'://attaque sur percepteur
				guild_perco_join_fight(packet.substring(2));
			break;
			case 'V'://Ferme le panneau de création de guilde
				guild_CancelCreate();
			break;
		}
	}
	
	private void guild_perco_join_fight(String packet) 
	{
		switch(packet.charAt(0))
		{
			case 'J'://Rejoindre
				String PercoID = Integer.toString(Integer.parseInt(packet.substring(1)), 36);
				
				int TiD = -1;
				try
				{
					TiD = Integer.parseInt(PercoID);
				}catch(Exception e){};
				
				Percepteur perco = World.getPerco(TiD);
				if(perco == null) return;
				
				int FightID = -1;
				try
				{
					FightID = perco.get_inFightID();
				}catch(Exception e){};
				
				short MapID = -1;
				try
				{
					MapID = World.getCarte((short)perco.get_mapID()).getFight(FightID).get_map().get_id();
				}catch(Exception e){};
				
				int CellID = -1;
				try
				{
					CellID = perco.get_cellID();
				}catch(Exception e){};
				
				if(Ancestra.CONFIG_DEBUG) GameServer.addToLog("[DEBUG] Percepteur INFORMATIONS : TiD:"+TiD+", FightID:"+FightID+", MapID:"+MapID+", CellID"+CellID);
				if(TiD == -1 || FightID == -1 || MapID == -1 || CellID == -1) return;
				if(_perso.get_fight() == null && !_perso.is_away())
				{
					if(_perso.get_curCarte().get_id() != MapID)
					{
						_perso.teleport(MapID, CellID);
					}
					World.getCarte(MapID).getFight(FightID).joinPercepteurFight(_perso,_perso.get_GUID(), TiD);
				}
			break;
		}
	}

	private void guild_remove_perco(String packet) 
	{
		if(_perso.get_guild() == null || _perso.get_fight() != null || _perso.is_away())return;
		if(!_perso.getGuildMember().canDo(Constants.G_POSPERCO))return;//On peut le retirer si on a le droit de le poser
		byte IDPerco = Byte.parseByte(packet);
		Percepteur perco = World.getPerco(IDPerco);
		if(perco == null || perco.get_inFight() > 0) return;
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), IDPerco);
		SQLManager.DELETE_PERCO(perco.getGuid());
		perco.DelPerco(perco.getGuid());
		for(Personnage z : _perso.get_guild().getMembers())
		{
			if(z.isOnline())
			{
				SocketManager.GAME_SEND_gITM_PACKET(z, Percepteur.parsetoGuild(z.get_guild().get_id()));
				String str = "";
				str += "R"+perco.get_N1()+","+perco.get_N2()+"|";
				str += perco.get_mapID()+"|";
				str += World.getCarte((short)perco.get_mapID()).getX()+"|"+World.getCarte((short)perco.get_mapID()).getY()+"|"+_perso.get_name();
				SocketManager.GAME_SEND_gT_PACKET(z, str);
			}
		}
	}

	private void guild_add_perco() 
	{
		if(_perso.get_guild() == null || _perso.get_fight() != null || _perso.is_away())return;
		if(!_perso.getGuildMember().canDo(Constants.G_POSPERCO))return;//Pas le droit de le poser
		if(_perso.get_guild().getMembers().size() < 10)return;//Guilde invalide
		short price = (short)(1000+10*_perso.get_guild().get_lvl());//Calcul du prix du percepteur
		if(_perso.get_kamas() < price)//Kamas insuffisants
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "182");
			return;
		}
		if(Percepteur.GetPercoGuildID(_perso.get_curCarte().get_id()) > 0)//La carte possède un perco
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1168;1");
			return;
		}
		if(_perso.get_curCarte().get_placesStr().length() < 5)//La map ne possède pas de "places"
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "113");
			return;
		}
		if(Percepteur.CountPercoGuild(_perso.get_guild().get_id()) >= _perso.get_guild().get_nbrPerco()) return;//Limite de percepteur
		short random1 = (short) (Formulas.getRandomValue(1, 39));
		short random2 = (short) (Formulas.getRandomValue(1, 71));
		//Ajout du Perco.
		int id = SQLManager.GetNewIDPercepteur();
		Percepteur perco = new Percepteur(id, _perso.get_curCarte().get_id(), _perso.get_curCell().getID(), (byte)3, _perso.get_guild().get_id(), random1, random2, "", 0, 0);
		World.addPerco(perco);
		SocketManager.GAME_SEND_ADD_PERCO_TO_MAP(_perso.get_curCarte());
		SQLManager.ADD_PERCO_ON_MAP(id, _perso.get_curCarte().get_id(), _perso.get_guild().get_id(), _perso.get_curCell().getID(), 3, random1, random2);
		for(Personnage z : _perso.get_guild().getMembers())
		{
			if(z != null && z.isOnline())
			{
				SocketManager.GAME_SEND_gITM_PACKET(z, Percepteur.parsetoGuild(z.get_guild().get_id()));
				String str = "";
				str += "S"+perco.get_N1()+","+perco.get_N2()+"|";
				str += perco.get_mapID()+"|";
				str += World.getCarte((short)perco.get_mapID()).getX()+"|"+World.getCarte((short)perco.get_mapID()).getY()+"|"+_perso.get_name();
				SocketManager.GAME_SEND_gT_PACKET(z, str);
			}
		}
	}

	private void guild_enclo(String packet)
	{
		if(_perso.get_guild() == null)
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
			return;
		}
		
		if(_perso.get_fight() != null || _perso.is_away())return;
		short MapID = Short.parseShort(packet);
		MountPark MP = World.getCarte(MapID).getMountPark();
		if(MP.get_guild().get_id() != _perso.get_guild().get_id())
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
			return;
		}
		int CellID = World.getEncloCellIdByMapId(MapID);
		if (_perso.hasItemTemplate(9035, 1))
		{
			_perso.removeByTemplateID(9035,1);
			_perso.teleport(MapID, CellID);
		}else
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1159");
			return;
		}
	}
	
	private void guild_house(String packet)
	{
		if(_perso.get_guild() == null)
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
			return;
		}
		
		if(_perso.get_fight() != null || _perso.is_away())return;
		int HouseID = Integer.parseInt(packet);
		House h = World.getHouses().get(HouseID);
		if(h == null) return;
		if(_perso.get_guild().get_id() != h.get_guild_id()) 
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
			return;
		}
		if(!h.canDo(Constants.H_GTELE))
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1136");
			return;
		}
		if (_perso.hasItemTemplate(8883, 1))
		{
			_perso.removeByTemplateID(8883,1);
			_perso.teleport((short)h.get_mapid(), h.get_caseid());
		}else
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1137");
			return;
		}
	}
	
	private void guild_promote(String packet)
	{
		if(_perso.get_guild() == null)return;	//Si le personnage envoyeur n'a même pas de guilde
		
		String[] infos = packet.split("\\|");
		
		int guid = Integer.parseInt(infos[0]);
		int rank = Integer.parseInt(infos[1]);
		byte xpGive = Byte.parseByte(infos[2]);
		int right = Integer.parseInt(infos[3]);
		
		Personnage p = World.getPersonnage(guid);	//Cherche le personnage a qui l'on change les droits dans la mémoire
		GuildMember toChange;
		GuildMember changer = _perso.getGuildMember();
		
		//Récupération du personnage à changer, et verification de quelques conditions de base
		if(p == null)	//Arrive lorsque le personnage n'est pas chargé dans la mémoire
		{
			int guildId = SQLManager.isPersoInGuild(guid);	//Récupère l'id de la guilde du personnage qui n'est pas dans la mémoire
			
			if(guildId < 0)return;	//Si le personnage à qui les droits doivent être modifié n'existe pas ou n'a pas de guilde
			
			
			if(guildId != _perso.get_guild().get_id())					//Si ils ne sont pas dans la même guilde
			{
				SocketManager.GAME_SEND_gK_PACKET(_perso, "Ed");
				return;
			}
			toChange = World.getGuild(guildId).getMember(guid);
		}
		else
		{
			if(p.get_guild() == null)return;	//Si la personne à qui changer les droits n'a pas de guilde
			if(_perso.get_guild().get_id() != p.get_guild().get_id())	//Si ils ne sont pas de la meme guilde
			{
				SocketManager.GAME_SEND_gK_PACKET(_perso, "Ea");
				return;
			}
			
			toChange = p.getGuildMember();
		}
		
		//Vérifie ce que le personnage changeur à le droit de faire
		
		if(changer.getRank() == 1)	//Si c'est le meneur
		{
			if(changer.getGuid() == toChange.getGuid())	//Si il se modifie lui même, reset tout sauf l'XP
			{
				rank = -1;
				right = -1;
			}
			else //Si il modifie un autre membre
			{
				if(rank == 1) //Si il met un autre membre "Meneur"
				{
					changer.setAllRights(2, (byte) -1, 29694);	//Met le meneur "Bras droit" avec tout les droits
					
					//Défini les droits à mettre au nouveau meneur
					rank = 1;
					xpGive = -1;
					right = 1;
				}
			}
		}
		else	//Sinon, c'est un membre normal
		{
			if(toChange.getRank() == 1)	//S'il veut changer le meneur, reset tout sauf l'XP
			{
				rank = -1;
				right = -1;
			}
			else	//Sinon il veut changer un membre normal
			{
				if(!changer.canDo(Constants.G_RANK) || rank == 1)	//S'il ne peut changer les rang ou qu'il veut mettre meneur
					rank = -1; 	//"Reset" le rang
				
				if(!changer.canDo(Constants.G_RIGHT) || right == 1)	//S'il ne peut changer les droits ou qu'il veut mettre les droits de meneur
					right = -1;	//"Reset" les droits
				
				if(!changer.canDo(Constants.G_HISXP) && !changer.canDo(Constants.G_ALLXP) && changer.getGuid() == toChange.getGuid())	//S'il ne peut changer l'XP de personne et qu'il est la cible
					xpGive = -1; //"Reset" l'XP
			}
			
			if(!changer.canDo(Constants.G_ALLXP) && !changer.equals(toChange))	//S'il n'a pas le droit de changer l'XP des autres et qu'il n'est pas la cible
				xpGive = -1; //"Reset" L'XP
		}

		toChange.setAllRights(rank,xpGive,right);
		
		SocketManager.GAME_SEND_gS_PACKET(_perso,_perso.getGuildMember());
		
		if(p != null && p.get_GUID() != _perso.get_GUID())
			SocketManager.GAME_SEND_gS_PACKET(p,p.getGuildMember());
	}
	
	private void guild_CancelCreate()
	{
		SocketManager.GAME_SEND_gV_PACKET(_perso);
	}

	private void guild_kick(String name)
	{
		if(_perso.get_guild() == null)return;
		Personnage P = World.getPersoByName(name);
		int guid = -1,guildId = -1;
		Guild toRemGuild;
		GuildMember toRemMember;
		if(P == null)
		{
			int infos[] = SQLManager.isPersoInGuild(name);
			guid = infos[0];
			guildId = infos[1];
			if(guildId < 0 || guid < 0)return;
			toRemGuild = World.getGuild(guildId);
			toRemMember = toRemGuild.getMember(guid);
		}
		else
		{
			toRemGuild = P.get_guild();
			if(toRemGuild == null)//La guilde du personnage n'est pas charger ?
			{
					toRemGuild = World.getGuild(_perso.get_guild().get_id());//On prend la guilde du perso qui l'éjecte
			}
			toRemMember = toRemGuild.getMember(P.get_GUID());
			if(toRemMember == null) return;//Si le membre n'est pas dans la guilde.
			if(toRemMember.getGuild().get_id() != _perso.get_guild().get_id()) return;//Si guilde différente
		}
		//si pas la meme guilde
		if(toRemGuild.get_id() != _perso.get_guild().get_id())
		{
			SocketManager.GAME_SEND_gK_PACKET(_perso, "Ea");
			return;
		}
		//S'il n'a pas le droit de kick, et que ce n'est pas lui même la cible
		if(!_perso.getGuildMember().canDo(Constants.G_BAN) && _perso.getGuildMember().getGuid() != toRemMember.getGuid())
		{
			SocketManager.GAME_SEND_gK_PACKET(_perso, "Ed");
			return;
		}
		//Si différent : Kick
		if(_perso.getGuildMember().getGuid() != toRemMember.getGuid())
		{
			if(toRemMember.getRank() == 1) //S'il veut kicker le meneur
				return;
			
			toRemGuild.removeMember(toRemMember.getPerso());
			if(P != null)
				P.setGuildMember(null);
			
			SocketManager.GAME_SEND_gK_PACKET(_perso, "K"+_perso.get_name()+"|"+name);
			if(P != null)
				SocketManager.GAME_SEND_gK_PACKET(P, "K"+_perso.get_name());
		}else//si quitter
		{
			Guild G = _perso.get_guild();
			if(_perso.getGuildMember().getRank() == 1 && G.getMembers().size() > 1)	//Si le meneur veut quitter la guilde mais qu'il reste d'autre joueurs
			{
				//TODO : Envoyer le message qu'il doit mettre un autre membre meneur (Pas vraiment....)
				return;
			}
			G.removeMember(_perso);
			_perso.setGuildMember(null);
			//S'il n'y a plus personne
			if(G.getMembers().isEmpty())World.removeGuild(G.get_id());
			SocketManager.GAME_SEND_gK_PACKET(_perso, "K"+name+"|"+name);
		}
	}
	
	private void guild_join(String packet)
	{
		switch(packet.charAt(0))
		{
		case 'R'://Nom perso
			Personnage P = World.getPersoByName(packet.substring(1));
			if(P == null || _perso.get_guild() == null)
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Eu");
				return;
			}
			if(!P.isOnline())
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Eu");
				return;
			}
			if(P.is_away())
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Eo");
				return;
			}
			if(P.get_guild() != null)
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Ea");
				return;
			}
			if(!_perso.getGuildMember().canDo(Constants.G_INVITE))
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Ed");
				return;
			}
			if(_perso.get_guild().getMembers().size() >= (40+_perso.get_guild().get_lvl()))//Limite membres max
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "155;"+(40+_perso.get_guild().get_lvl()));
				return;
			}
			
			_perso.setInvitation(P.get_GUID());
			P.setInvitation(_perso.get_GUID());

			SocketManager.GAME_SEND_gJ_PACKET(_perso,"R"+packet.substring(1));
			SocketManager.GAME_SEND_gJ_PACKET(P,"r"+_perso.get_GUID()+"|"+_perso.get_name()+"|"+_perso.get_guild().get_name());
		break;
		case 'E'://ou Refus
			if(packet.substring(1).equalsIgnoreCase(_perso.getInvitation()+""))
			{
				Personnage p = World.getPersonnage(_perso.getInvitation());
				if(p == null)return;//Pas censé arriver
				SocketManager.GAME_SEND_gJ_PACKET(p,"Ec");
			}
		break;
		case 'K'://Accepte
			if(packet.substring(1).equalsIgnoreCase(_perso.getInvitation()+""))
			{
				Personnage p = World.getPersonnage(_perso.getInvitation());
				if(p == null)return;//Pas censé arriver
				Guild G = p.get_guild();
				GuildMember GM = G.addNewMember(_perso);
				SQLManager.UPDATE_GUILDMEMBER(GM);
				_perso.setGuildMember(GM);
				_perso.setInvitation(-1);
				p.setInvitation(-1);
				//Packet
				SocketManager.GAME_SEND_gJ_PACKET(p,"Ka"+_perso.get_name());
				SocketManager.GAME_SEND_gS_PACKET(_perso, GM);
				SocketManager.GAME_SEND_gJ_PACKET(_perso,"Kj");
			}
		break;
		}
	}

	private void guild_infos(char c)
	{
		switch(c)
		{
		case 'B'://Perco
			SocketManager.GAME_SEND_gIB_PACKET(_perso, _perso.get_guild().parsePercotoGuild());
		break;
		case 'F'://Enclos
			SocketManager.GAME_SEND_gIF_PACKET(_perso, World.parseMPtoGuild(_perso.get_guild().get_id()));
		break;
		case 'G'://General
			SocketManager.GAME_SEND_gIG_PACKET(_perso, _perso.get_guild());
		break;
		case 'H'://House
			SocketManager.GAME_SEND_gIH_PACKET(_perso, House.parseHouseToGuild(_perso));
		break;
		case 'M'://Members
			SocketManager.GAME_SEND_gIM_PACKET(_perso, _perso.get_guild(),'+');
		break;
		case 'T'://Perco
			SocketManager.GAME_SEND_gITM_PACKET(_perso, Percepteur.parsetoGuild(_perso.get_guild().get_id()));
			Percepteur.parseAttaque(_perso, _perso.get_guild().get_id());
			Percepteur.parseDefense(_perso, _perso.get_guild().get_id());
		break;
		}
	}

	private void guild_create(String packet)
	{
		if(_perso == null)return;
		if(_perso.get_guild() != null || _perso.getGuildMember() != null)
		{
			SocketManager.GAME_SEND_gC_PACKET(_perso, "Ea");
			return;
		}
		if(_perso.get_fight() != null || _perso.is_away())return;
		try
		{
			String[] infos = packet.substring(2).split("\\|");
			//base 10 => 36
			String bgID = Integer.toString(Integer.parseInt(infos[0]),36);
			String bgCol = Integer.toString(Integer.parseInt(infos[1]),36);
			String embID =  Integer.toString(Integer.parseInt(infos[2]),36);
			String embCol =  Integer.toString(Integer.parseInt(infos[3]),36);
			String name = infos[4];
			if(World.guildNameIsUsed(name))
			{
				SocketManager.GAME_SEND_gC_PACKET(_perso, "Ean");
				return;
			}
			
			//Validation du nom de la guilde
			String tempName = name.toLowerCase();
			boolean isValid = true;
			//Vérifie d'abord si il contient des termes définit
			if(tempName.length() > 20
					|| tempName.contains("mj")
					|| tempName.contains("modo")
					|| tempName.contains("admin"))
			{
				isValid = false;
			}
			//Si le nom passe le test, on vérifie que les caractère entré sont correct.
			if(isValid)
			{
				int tiretCount = 0;
				for(char curLetter : tempName.toCharArray())
				{
					if(!(	(curLetter >= 'a' && curLetter <= 'z')
							|| curLetter == '-'))
					{
						isValid = false;
						break;
					}
					if(curLetter == '-')
					{
						if(tiretCount >= 2)
						{
							isValid = false;
							break;
						}
						else
						{
							tiretCount++;
						}
					}
				}
			}
			//Si le nom est invalide
			if(!isValid)
			{
				SocketManager.GAME_SEND_gC_PACKET(_perso, "Ean");
				return;
			}
			//FIN de la validation
			String emblem = bgID+","+bgCol+","+embID+","+embCol;//9,6o5nc,2c,0;
			if(World.guildEmblemIsUsed(emblem))
			{
				SocketManager.GAME_SEND_gC_PACKET(_perso, "Eae");
				return;
			}
			if(_perso.get_curCarte().get_id() == 2196)//Temple de création de guilde
			{
				if(!_perso.hasItemTemplate(1575,1))//Guildalogemme
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "14");
					return;
				}
				_perso.removeByTemplateID(1575, 1);
			}
			Guild G = new Guild(_perso,name,emblem);
			GuildMember gm = G.addNewMember(_perso);
			gm.setAllRights(1,(byte) 0,1);//1 => Meneur (Tous droits)
			_perso.setGuildMember(gm);//On ajoute le meneur
			World.addGuild(G, true);
			SQLManager.UPDATE_GUILDMEMBER(gm);
			//Packets
			SocketManager.GAME_SEND_gS_PACKET(_perso, gm);
			SocketManager.GAME_SEND_gC_PACKET(_perso,"K");
			SocketManager.GAME_SEND_gV_PACKET(_perso);
		}catch(Exception e){return;};
	}

	private void parseChanelPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'C'://Changement des Canaux
				Chanels_change(packet);
			break;
		}
	}

	private void Chanels_change(String packet)
	{
		String chan = packet.charAt(3)+"";
		switch(packet.charAt(2))
		{
			case '+'://Ajout du Canal
				_perso.addChanel(chan);
			break;
			case '-'://Desactivation du canal
				_perso.removeChanel(chan);
			break;
		}
		SQLManager.SAVE_PERSONNAGE(_perso, false);
	}

	private void parseMountPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'b'://Achat d'un enclos
				SocketManager.GAME_SEND_R_PACKET(_perso, "v");//Fermeture du panneau
				MountPark MP = _perso.get_curCarte().getMountPark();
				Personnage Seller = World.getPersonnage(MP.get_owner());
				if(MP.get_owner() == -1)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "196");
					return;
				}
				if(MP.get_price() == 0)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "197");
					return;
				}
				if(_perso.get_guild() == null)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
					return;
				}
				if(_perso.getGuildMember().getRank() != 1)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "198"); 
					return;
				}
				byte enclosMax = (byte)Math.floor(_perso.get_guild().get_lvl()/10);
				byte TotalEncloGuild = (byte)World.totalMPGuild(_perso.get_guild().get_id());
				if(TotalEncloGuild >= enclosMax)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "1103");
					return;
				}
				if(_perso.get_kamas() < MP.get_price())
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "182");
					return;
				}
				long NewKamas = _perso.get_kamas()-MP.get_price();
				_perso.set_kamas(NewKamas);
				if(Seller != null)
				{
					long NewSellerBankKamas = Seller.getBankKamas()+MP.get_price();
					Seller.setBankKamas(NewSellerBankKamas);
					if(Seller.isOnline())
					{
						SocketManager.GAME_SEND_MESSAGE(_perso, "Un enclo a ete vendu a "+MP.get_price()+".", Ancestra.CONFIG_MOTD_COLOR);
					}
				}
				MP.set_price(0);//On vide le prix
				MP.set_owner(_perso.get_GUID());
				MP.set_guild(_perso.get_guild());
				SQLManager.SAVE_MOUNTPARK(MP);
				SQLManager.SAVE_PERSONNAGE(_perso, true);
				//On rafraichit l'enclo
				for(Personnage z:_perso.get_curCarte().getPersos())
				{
					SocketManager.GAME_SEND_Rp_PACKET(z, MP);
				}
			break;
		
			case 'd'://Demande Description
				Mount_description(packet);
			break;
			
			case 'n'://Change le nom
				Mount_name(packet.substring(2));
			break;
			
			case 'r'://Monter sur la dinde
				Mount_ride();
			break;
			case 's'://Vendre l'enclo
				SocketManager.GAME_SEND_R_PACKET(_perso, "v");//Fermeture du panneau
				int price = Integer.parseInt(packet.substring(2));
				MountPark MP1 = _perso.get_curCarte().getMountPark();
				if(!MP1.getData().isEmpty())
				{
					SocketManager.GAME_SEND_MESSAGE(_perso, "[ENCLO] Impossible de vendre un enclo plein.", Ancestra.CONFIG_MOTD_COLOR);
					return;
				}
				if(MP1.get_owner() == -1)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "194");
					return;
				}
				if(MP1.get_owner() != _perso.get_GUID())
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "195");
					return;
				}
				MP1.set_price(price);
				SQLManager.SAVE_MOUNTPARK(MP1);
				SQLManager.SAVE_PERSONNAGE(_perso, true);
				//On rafraichit l'enclo
				for(Personnage z:_perso.get_curCarte().getPersos())
				{
					SocketManager.GAME_SEND_Rp_PACKET(z, MP1);
				}
			break;
			case 'v'://Fermeture panneau d'achat
				SocketManager.GAME_SEND_R_PACKET(_perso, "v");
			break;
			case 'x'://Change l'xp donner a la dinde
				Mount_changeXpGive(packet);
			break;
		}
	}

	private void Mount_changeXpGive(String packet)
	{
		try
		{
			int xp = Integer.parseInt(packet.substring(2));
			if(xp <0)xp = 0;
			if(xp >90)xp = 90;
			_perso.setMountGiveXp(xp);
			SocketManager.GAME_SEND_Rx_PACKET(_perso);
		}catch(Exception e){};
	}

	private void Mount_name(String name)
	{
		if(_perso.getMount() == null)return;
		_perso.getMount().setName(name);
		SocketManager.GAME_SEND_Rn_PACKET(_perso, name);
	}
	
	private void Mount_ride()
	{
		if(_perso.get_lvl()<60 || _perso.getMount() == null || !_perso.getMount().isMountable() || _perso._isGhosts)
		{
			SocketManager.GAME_SEND_Re_PACKET(_perso,"Er", null);
			return;
		}
		_perso.toogleOnMount();
	}
	
	private void Mount_description(String packet)
	{
		int DDid = -1;
		try
		{
			DDid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			//on ignore le temps?
		}catch(Exception e){};
		if(DDid == -1)return;
		Dragodinde DD = World.getDragoByID(DDid);
		if(DD == null)return;
		SocketManager.GAME_SEND_MOUNT_DESCRIPTION_PACKET(_perso,DD);
	}

	private void parse_friendPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A'://Ajouter
				Friend_add(packet);
			break;
			case 'D'://Effacer un ami
				Friend_delete(packet);
			break;
			case 'L'://Liste
				SocketManager.GAME_SEND_FRIENDLIST_PACKET(_perso);
			break;
			case 'O':
				switch(packet.charAt(2))
				{
				case '-':
					 _perso.SetSeeFriendOnline(false);
					 SocketManager.GAME_SEND_BN(_perso);
					 break;
				 case'+':
					 _perso.SetSeeFriendOnline(true);
					 SocketManager.GAME_SEND_BN(_perso);
					 break;
				}
			break;
			case 'J': //Wife
				FriendLove(packet);
			break;
		}
	}

	private void FriendLove(String packet)
	{
		Personnage Wife = World.getPersonnage(_perso.getWife());
		if(Wife == null) return;
		if(!Wife.isOnline())
		{
			if(Wife.get_sexe() == 0) SocketManager.GAME_SEND_Im_PACKET(_perso, "140");
			else SocketManager.GAME_SEND_Im_PACKET(_perso, "139");
			
			SocketManager.GAME_SEND_FRIENDLIST_PACKET(_perso);
			return;
		}
		switch(packet.charAt(2))
		{
			case 'S'://Teleportation
				if(_perso.get_fight() != null)
					return;
				else
					_perso.meetWife(Wife);
			break;
			case 'C'://Suivre le deplacement
				if(packet.charAt(3) == '+'){//Si lancement de la traque
					if(_perso._Follows != null)
					{
						_perso._Follows._Follower.remove(_perso.get_GUID());
					}
					SocketManager.GAME_SEND_FLAG_PACKET(_perso, Wife);
					_perso._Follows = Wife;
					Wife._Follower.put(_perso.get_GUID(), _perso);
				}else{//On arrete de suivre
					SocketManager.GAME_SEND_DELETE_FLAG_PACKET(_perso);
					_perso._Follows = null;
					Wife._Follower.remove(_perso.get_GUID());
				}
			break;
		}
	} 
	
	private void Friend_delete(String packet) {
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://Nom de perso
				packet = packet.substring(3);
				Personnage P = World.getPersoByName(packet);
				if(P == null)//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
				
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Compte C = World.getCompteByPseudo(packet);
				if(C==null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				Personnage Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1 || !_compte.isFriendWith(guid))
		{
			SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
			return;
		}
		_compte.removeFriend(guid);
	}

	private void Friend_add(String packet)
	{
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://Nom de perso
				packet = packet.substring(3);
				Personnage P = World.getPersoByName(packet);
				if(P == null?true:!P.isOnline())//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Compte C = World.getCompteByPseudo(packet);
				if(C==null?true:!C.isOnline())
				{
					SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				Personnage Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1)
		{
			SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
			return;
		}
		_compte.addFriend(guid);
	}

	private void parseGroupPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A'://Accepter invitation
				group_accept(packet);
			break;
			
			case 'F'://Suivre membre du groupe PF+GUID
				Group g = _perso.getGroup();
				if(g == null)return;
				
				int pGuid = -1;
				try
				{
					pGuid = Integer.parseInt(packet.substring(3));
				}catch(NumberFormatException e){return;};
				
				if(pGuid == -1) return;
				
				Personnage P = World.getPersonnage(pGuid);
				
				if(P == null || !P.isOnline()) return;
				
				if(packet.charAt(2) == '+')//Suivre
				{
					if(_perso._Follows != null)
					{
						_perso._Follows._Follower.remove(_perso.get_GUID());
					}
					SocketManager.GAME_SEND_FLAG_PACKET(_perso, P);
					SocketManager.GAME_SEND_PF(_perso, "+"+P.get_GUID());
					_perso._Follows = P;
					P._Follower.put(_perso.get_GUID(), _perso);
				}
				else if(packet.charAt(2) == '-')//Ne plus suivre
				{
					SocketManager.GAME_SEND_DELETE_FLAG_PACKET(_perso);
					SocketManager.GAME_SEND_PF(_perso, "-");
					_perso._Follows = null;
					P._Follower.remove(_perso.get_GUID());
				}
			break;
			case 'G'://Suivez le tous PG+GUID
				Group g2 = _perso.getGroup();
				if(g2 == null)return;
				
				int pGuid2 = -1;
				try
				{
					pGuid2 = Integer.parseInt(packet.substring(3));
				}catch(NumberFormatException e){return;};
				
				if(pGuid2 == -1) return;
				
				Personnage P2 = World.getPersonnage(pGuid2);
				
				if(P2 == null || !P2.isOnline()) return;
				
				if(packet.charAt(2) == '+')//Suivre
				{
					for(Personnage T : g2.getPersos())
					{
						if(T.get_GUID() == P2.get_GUID()) continue;
						if(T._Follows != null)
						{
							T._Follows._Follower.remove(_perso.get_GUID());
						}
						SocketManager.GAME_SEND_FLAG_PACKET(T, P2);
						SocketManager.GAME_SEND_PF(T, "+"+P2.get_GUID());
						T._Follows = P2;
						P2._Follower.put(T.get_GUID(), T);
					}
				}
				else if(packet.charAt(2) == '-')//Ne plus suivre
				{
					for(Personnage T : g2.getPersos())
					{
						if(T.get_GUID() == P2.get_GUID()) continue;
						SocketManager.GAME_SEND_DELETE_FLAG_PACKET(T);
						SocketManager.GAME_SEND_PF(T, "-");
						T._Follows = null;
						P2._Follower.remove(T.get_GUID());
					}
				}
			break;
			
			case 'I'://inviation
				group_invite(packet);
			break;
			
			case 'R'://Refuse
				group_refuse();
			break;
			
			case 'V'://Quitter
				group_quit(packet);
			break;
			case 'W'://Localisation du groupe
				group_locate();
			break;
		}
	}
	
	private void group_locate()
	{
		if(_perso == null)return;
		Group g = _perso.getGroup();
		if(g == null)return;
		String str = "";
		boolean isFirst = true;
		for(Personnage GroupP : _perso.getGroup().getPersos())
		{
			if(!isFirst) str += "|";
			str += GroupP.get_curCarte().getX()+";"+GroupP.get_curCarte().getY()+";"+GroupP.get_curCarte().get_id()+";2;"+GroupP.get_GUID()+";"+GroupP.get_name();
			isFirst = false;
		}
		SocketManager.GAME_SEND_IH_PACKET(_perso, str);
	}
	
	private void group_quit(String packet)
	{
		if(_perso == null)return;
		Group g = _perso.getGroup();
		if(g == null)return;
		if(packet.length() == 2)//Si aucun guid est spécifié, alors c'est que le joueur quitte
		{
			 g.leave(_perso);
			 SocketManager.GAME_SEND_PV_PACKET(_out,"");
			SocketManager.GAME_SEND_IH_PACKET(_perso, "");
		}else if(g.isChief(_perso.get_GUID()))//Sinon, c'est qu'il kick un joueur du groupe
		{
			int guid = -1;
			try
			{
				guid = Integer.parseInt(packet.substring(2));
			}catch(NumberFormatException e){return;};
			if(guid == -1)return;
			Personnage t = World.getPersonnage(guid);
			g.leave(t);
			SocketManager.GAME_SEND_PV_PACKET(t.get_compte().getGameThread().get_out(),""+_perso.get_GUID());
			SocketManager.GAME_SEND_IH_PACKET(t, "");
		}
	}

	private void group_invite(String packet)
	{
		if(_perso == null)return;
		String name = packet.substring(2);
		Personnage target = World.getPersoByName(name);
		if(target == null)return;
		if(!target.isOnline())
		{
			SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(_out,"n"+name);
			return;
		}
		if(target.getGroup() != null)
		{
			SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(_out, "a"+name);
			return;
		}
		if(_perso.getGroup() != null && _perso.getGroup().getPersosNumber() == 8)
		{
			SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(_out, "f");
			return;
		}
		target.setInvitation(_perso.get_GUID());	
		_perso.setInvitation(target.get_GUID());
		SocketManager.GAME_SEND_GROUP_INVITATION(_out,_perso.get_name(),name);
		SocketManager.GAME_SEND_GROUP_INVITATION(target.get_compte().getGameThread().get_out(),_perso.get_name(),name);
	}

	private void group_refuse()
	{
		if(_perso == null)return;
		if(_perso.getInvitation() == 0)return;
		_perso.setInvitation(0);
		SocketManager.GAME_SEND_BN(_out);
		Personnage t = World.getPersonnage(_perso.getInvitation());
		if(t == null) return;
		t.setInvitation(0);
		SocketManager.GAME_SEND_PR_PACKET(t);
	}

	private void group_accept(String packet)
	{
		if(_perso == null)return;
		if(_perso.getInvitation() == 0)return;
		Personnage t = World.getPersonnage(_perso.getInvitation());
		if(t == null) return;
		Group g = t.getGroup();
		if(g == null)
		{
			g = new Group(t,_perso);
			SocketManager.GAME_SEND_GROUP_CREATE(_out,g);
			SocketManager.GAME_SEND_PL_PACKET(_out,g);
			SocketManager.GAME_SEND_GROUP_CREATE(t.get_compte().getGameThread().get_out(),g);
			SocketManager.GAME_SEND_PL_PACKET(t.get_compte().getGameThread().get_out(),g);
			t.setGroup(g);
			SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(t.get_compte().getGameThread().get_out(),g);
		}
		else
		{
			SocketManager.GAME_SEND_GROUP_CREATE(_out,g);
			SocketManager.GAME_SEND_PL_PACKET(_out,g);
			SocketManager.GAME_SEND_PM_ADD_PACKET_TO_GROUP(g, _perso);
			g.addPerso(_perso);
		}
		_perso.setGroup(g);
		SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(_out,g);
		SocketManager.GAME_SEND_PR_PACKET(t);
	}

	private void parseObjectPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'd'://Supression d'un objet
				Object_delete(packet);
			break;
			case 'D'://Depose l'objet au sol
				Object_drop(packet);
			break;
			case 'M'://Bouger un objet (Equiper/déséquiper)
				Object_move(packet);
			break;
			case 'U'://Utiliser un objet (potions)
				Object_use(packet);
			break;
		}
	}

	private void Object_drop(String packet)
	{
		int guid = -1;
		int qua = -1;
		try
		{
			guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			qua = Integer.parseInt(packet.split("\\|")[1]);
		}catch(Exception e){};
		if(guid == -1 || qua <= 0 || !_perso.hasItemGuid(guid) || _perso.get_fight() != null || _perso.is_away())return;
		Objet obj = World.getObjet(guid);
		
		_perso.set_curCell(_perso.get_curCell());
		int cellPosition = Constants.getNearCellidUnused(_perso);
		if(cellPosition < 0)
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1145");
			return;
		}
		if(obj.getPosition() != Constants.ITEM_POS_NO_EQUIPED)
		{
			obj.setPosition(Constants.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,obj);
			if(obj.getPosition() == Constants.ITEM_POS_ARME 		||
				obj.getPosition() == Constants.ITEM_POS_COIFFE 		||
				obj.getPosition() == Constants.ITEM_POS_FAMILIER 	||
				obj.getPosition() == Constants.ITEM_POS_CAPE		||
				obj.getPosition() == Constants.ITEM_POS_BOUCLIER	||
				obj.getPosition() == Constants.ITEM_POS_NO_EQUIPED)
					SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso);
		}
		if(qua >= obj.getQuantity())
		{
			_perso.removeItem(guid);
			_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellPosition).addDroppedItem(obj);
			obj.setPosition(Constants.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, guid);
		}else
		{
			obj.setQuantity(obj.getQuantity() - qua);
			Objet obj2 = Objet.getCloneObjet(obj, qua);
			obj2.setPosition(Constants.ITEM_POS_NO_EQUIPED);
			_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellPosition).addDroppedItem(obj2);
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
		}
		SocketManager.GAME_SEND_Ow_PACKET(_perso);
		SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(_perso.get_curCarte(),'+',_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellPosition).getID(),obj.getTemplate().getID(),0);
		SocketManager.GAME_SEND_STATS_PACKET(_perso);
	}

	private void Object_use(String packet)
	{
		int guid = -1;
		int targetGuid = -1;
		short cellID = -1;
		Personnage Target = null;
		try
		{
			String[] infos = packet.substring(2).split("\\|");
			guid = Integer.parseInt(infos[0]);
			try
			{
				targetGuid = Integer.parseInt(infos[1]);
			}catch(Exception e){targetGuid = -1;};
			try
			{
				cellID = Short.parseShort(infos[2]);
			}catch(Exception e){cellID = -1;};
		}catch(Exception e){return;};
		//Si le joueur n'a pas l'objet
		if(World.getPersonnage(targetGuid) != null)
		{
			Target = World.getPersonnage(targetGuid);
		}
		if(!_perso.hasItemGuid(guid) || _perso.get_fight() != null || _perso.is_away())return;
		if(Target != null && (Target.get_fight() != null || Target.is_away()))return;
		Objet obj = World.getObjet(guid);
		if(obj == null) return;
		ObjTemplate T = obj.getTemplate();
		if(!obj.getTemplate().getConditions().equalsIgnoreCase("") && !ConditionParser.validConditions(_perso,obj.getTemplate().getConditions()))
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "119|43");
			return;
		}
		T.applyAction(_perso, Target, guid, cellID);
	}

	private synchronized void Object_move(String packet)
	{
		String[] infos = packet.substring(2).split(""+(char)0x0A)[0].split("\\|");
		try
		{
			int qua;
			int guid = Integer.parseInt(infos[0]);
			int pos = Integer.parseInt(infos[1]);
			try
			{
				qua = Integer.parseInt(infos[2]);
			}catch(Exception e)
			{
				qua = 1;
			}
			Objet obj = World.getObjet(guid);
			
			if(!_perso.hasItemGuid(guid) || obj == null)
				return;
			
			if(_perso.get_fight() != null)
			{
				if(_perso.get_fight().get_state() > 2)
				{
					return;
				}
			}
			if(!Constants.isValidPlaceForItem(obj.getTemplate(),pos) && pos != Constants.ITEM_POS_NO_EQUIPED)
			{
				return;
			}
			if(!obj.getTemplate().getConditions().equalsIgnoreCase("") && !ConditionParser.validConditions(_perso,obj.getTemplate().getConditions()))
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "119|43");
				return;
			}
			if(obj.getTemplate().getLevel() > _perso.get_lvl())
			{
				SocketManager.GAME_SEND_OAEL_PACKET(_out);
				return;
			}
			//On ne peut équiper 2 items de panoplies identiques, ou 2 Dofus identiques
			if(pos != Constants.ITEM_POS_NO_EQUIPED && (obj.getTemplate().getPanopID() != -1 || obj.getTemplate().getType() == Constants.ITEM_TYPE_DOFUS )&& _perso.hasEquiped(obj.getTemplate().getID()))
				return;
			
			Objet exObj = _perso.getObjetByPos(pos);//Objet a l'ancienne position
			if(exObj != null)//S'il y avait déja un objet sur cette place on déséquipe
			{
				Objet obj2;
				if((obj2 = _perso.getSimilarItem(exObj)) != null)//On le possède deja
				{
					obj2.setQuantity(obj2.getQuantity()+exObj.getQuantity());
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj2);
					World.removeItem(exObj.getGuid());
					_perso.removeItem(exObj.getGuid());
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, exObj.getGuid());
				}
				else//On ne le possède pas
				{
					exObj.setPosition(Constants.ITEM_POS_NO_EQUIPED);
					SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,exObj);
				}
				if(_perso.getObjetByPos(Constants.ITEM_POS_ARME) == null)
					SocketManager.GAME_SEND_OT_PACKET(_out, -1);
				
				//Si objet de panoplie
				if(exObj.getTemplate().getPanopID() > 0)SocketManager.GAME_SEND_OS_PACKET(_perso,exObj.getTemplate().getPanopID());
			}else//getNumbEquipedItemOfPanoplie(exObj.getTemplate().getPanopID()
			{
				Objet obj2;
				//On a un objet similaire
				if((obj2 = _perso.getSimilarItem(obj)) != null)
				{
					if(qua > obj.getQuantity()) qua = obj.getQuantity();
					
					obj2.setQuantity(obj2.getQuantity()+qua);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj2);
					
					if(obj.getQuantity() - qua > 0)//Si il en reste
					{
						obj.setQuantity(obj.getQuantity()-qua);
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
					}else//Sinon on supprime
					{
						World.removeItem(obj.getGuid());
						_perso.removeItem(obj.getGuid());
						SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, obj.getGuid());
					}
				}
				else//Pas d'objets similaires
				{
					obj.setPosition(pos);
					SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,obj);
					if(obj.getQuantity() > 1)
					{
						if(qua > obj.getQuantity()) qua = obj.getQuantity();
						
						if(obj.getQuantity() - qua > 0)//Si il en reste
						{
							int newItemQua = obj.getQuantity()-qua;
							Objet newItem = Objet.getCloneObjet(obj,newItemQua);
							_perso.addObjet(newItem,false);
							World.addObjet(newItem,true);
							obj.setQuantity(qua);
							SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
						}
					}
				}
			}
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
			_perso.refreshStats();
			if(_perso.getGroup() != null)
			{
				SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(_perso.getGroup(),_perso);
			}
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			if( pos == Constants.ITEM_POS_ARME 		||
				pos == Constants.ITEM_POS_COIFFE 	||
				pos == Constants.ITEM_POS_FAMILIER 	||
				pos == Constants.ITEM_POS_CAPE		||
				pos == Constants.ITEM_POS_BOUCLIER	||
				pos == Constants.ITEM_POS_NO_EQUIPED)
				SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso);
		
			//Si familier
			if(pos == Constants.ITEM_POS_FAMILIER && _perso.isOnMount())_perso.toogleOnMount();
			//Verif pour les outils de métier
			if(pos == Constants.ITEM_POS_NO_EQUIPED && _perso.getObjetByPos(Constants.ITEM_POS_ARME) == null)
				SocketManager.GAME_SEND_OT_PACKET(_out, -1);
			
			if(pos == Constants.ITEM_POS_ARME && _perso.getObjetByPos(Constants.ITEM_POS_ARME) != null)
			{
				int ID = _perso.getObjetByPos(Constants.ITEM_POS_ARME).getTemplate().getID();
				for(Entry<Integer,StatsMetier> e : _perso.getMetiers().entrySet())
				{
					if(e.getValue().getTemplate().isValidTool(ID))
						SocketManager.GAME_SEND_OT_PACKET(_out,e.getValue().getTemplate().getId());
				}
			}
			//Si objet de panoplie
			if(obj.getTemplate().getPanopID() > 0)SocketManager.GAME_SEND_OS_PACKET(_perso,obj.getTemplate().getPanopID());
			//Si en combat
			if(_perso.get_fight() != null)
			{
				SocketManager.GAME_SEND_ON_EQUIP_ITEM_FIGHT(_perso, _perso.get_fight().getFighterByPerso(_perso), _perso.get_fight());
			}
		}catch(Exception e)
		{
			e.printStackTrace();
			SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
		}
	}

	private void Object_delete(String packet)
	{
		String[] infos = packet.substring(2).split("\\|");
		try
		{
			int guid = Integer.parseInt(infos[0]);
			int qua = 1;
			try
			{
				qua = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			Objet obj = World.getObjet(guid);
			if(obj == null || !_perso.hasItemGuid(guid) || qua <= 0 || _perso.get_fight() != null || _perso.is_away())
			{
				SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
				return;
			}
			int newQua = obj.getQuantity()-qua;
			if(newQua <=0)
			{
				_perso.removeItem(guid);
				World.removeItem(guid);
				SQLManager.DELETE_ITEM(guid);
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, guid);
			}else
			{
				obj.setQuantity(newQua);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
			}
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
		}catch(Exception e)
		{
			SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
		}
	}

	private void parseDialogPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'C'://Demande de l'initQuestion
				Dialog_start(packet);
			break;
			
			case 'R'://Réponse du joueur
				Dialog_response(packet);
			break;
			
			case 'V'://Fin du dialog
				Dialog_end();
			break;
		}
	}

	private void Dialog_response(String packet)
	{
		String[] infos = packet.substring(2).split("\\|");
		try
		{
			int qID = Integer.parseInt(infos[0]);
			int rID = Integer.parseInt(infos[1]);
			NPC_question quest = World.getNPCQuestion(qID);
			NPC_reponse rep = World.getNPCreponse(rID);
			if(quest == null || rep == null || !rep.isAnotherDialog())
			{
				SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
				_perso.set_isTalkingWith(0);
			}
			rep.apply(_perso);
		}catch(Exception e)
		{
			SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
		}
	}

	private void Dialog_end()
	{
		SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
		if(_perso.get_isTalkingWith() != 0)
			_perso.set_isTalkingWith(0);
	}

	private void Dialog_start(String packet)
	{
		try
		{
			int npcID = Integer.parseInt(packet.substring(2).split((char)0x0A+"")[0]);
			NPC npc = _perso.get_curCarte().getNPC(npcID);
			if( npc == null)return;
			SocketManager.GAME_SEND_DCK_PACKET(_out,npcID);
			int qID = npc.get_template().get_initQuestionID();
			NPC_question quest = World.getNPCQuestion(qID);
			if(quest == null)
			{
				SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
				return;
			}
			SocketManager.GAME_SEND_QUESTION_PACKET(_out,quest.parseToDQPacket(_perso));
			_perso.set_isTalkingWith(npcID);
		}catch(NumberFormatException e){};
	}

	private void parseExchangePacket(String packet)
	{	
		switch(packet.charAt(1))
		{
			case 'A'://Accepter demande d'échange
				Exchange_accept();
			break;
			case 'B'://Achat
				Exchange_onBuyItem(packet);
			break;
			
			case 'H'://Demande prix moyen + catégorie
				Exchange_HDV(packet);
			break;
			
			case 'K'://Ok
				Exchange_isOK();
			break;
			case 'L'://jobAction : Refaire le craft précedent
				Exchange_doAgain();
			break;
			
			case 'M'://Move (Ajouter//retirer un objet a l'échange)
				Exchange_onMoveItem(packet);
			break;
			
			case 'q'://Mode marchand
				if(_perso.get_isTradingWith() > 0 || _perso.get_fight() != null || _perso.is_away())return;
		        if (_perso.get_curCarte().getStoreCount() == 5)
		        {
		        	SocketManager.GAME_SEND_Im_PACKET(_perso, "125;5");
		        	return;
		        }
		        if (_perso.parseStoreItemsList().isEmpty())
		        {
		        	SocketManager.GAME_SEND_Im_PACKET(_perso, "123");
		        	return;
		        }
		        int orientation = Formulas.getRandomValue(1, 3);
		        _perso.set_orientation(orientation);
		        Carte map = _perso.get_curCarte();
		        _perso.set_showSeller(true);
		        World.addSeller(_perso);
		        kick();
		        for(Personnage z : map.getPersos())
		        {
		        	if(z != null && z.isOnline())
		        		SocketManager.GAME_SEND_MERCHANT_LIST(z, z.get_curCarte().get_id());
		        }
			break;
			case 'r'://Rides => Monture
				Exchange_mountPark(packet);
			break;
			
			case 'R'://liste d'achat NPC
				Exchange_start(packet);
			break;
			case 'S'://Vente
				Exchange_onSellItem(packet);
			break;
			
			case 'V'://Fin de l'échange
				Exchange_finish_buy();
			break;
		}
	}
	
	private void Exchange_HDV(String packet)
	{
		if(_perso.get_isTradingWith() > 0 || _perso.get_fight() != null || _perso.is_away())return;
		int templateID;
		switch(packet.charAt(2))
		{
			case 'B': //Confirmation d'achat
				String[] info = packet.substring(3).split("\\|");//ligneID|amount|price
				
				HDV curHdv = World.getHdv(Math.abs(_perso.get_isTradingWith()));
				
				int ligneID = Integer.parseInt(info[0]);
				byte amount = Byte.parseByte(info[1]);
				
				if(curHdv.buyItem(ligneID,amount,Integer.parseInt(info[2]),_perso))
				{
					SocketManager.GAME_SEND_EHm_PACKET(_perso,"-",ligneID+"");//Enleve la ligne
					if(curHdv.getLigne(ligneID) != null && !curHdv.getLigne(ligneID).isEmpty())
						SocketManager.GAME_SEND_EHm_PACKET(_perso, "+", curHdv.getLigne(ligneID).parseToEHm());//Réajoute la ligne si elle n'est pas vide
					
					/*if(curHdv.getLigne(ligneID) != null)
					{
						String str = curHdv.getLigne(ligneID).parseToEHm();
						SocketManager.GAME_SEND_EHm_PACKET(_perso,"+",str);
					}*/
					
					
					_perso.refreshStats();
					SocketManager.GAME_SEND_Ow_PACKET(_perso);
					SocketManager.GAME_SEND_Im_PACKET(_perso,"068");//Envoie le message "Lot acheté"
				}
				else
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso,"172");//Envoie un message d'erreur d'achat
				}
			break;
			case 'l'://Demande listage d'un template (les prix)
				templateID = Integer.parseInt(packet.substring(3));
				try
				{
					SocketManager.GAME_SEND_EHl(_perso,World.getHdv(Math.abs(_perso.get_isTradingWith())),templateID);
				}catch(NullPointerException e)//Si erreur il y a, retire le template de la liste chez le client
				{
					SocketManager.GAME_SEND_EHM_PACKET(_perso,"-",templateID+"");
				}
				
			break;
			case 'P'://Demande des prix moyen
				templateID = Integer.parseInt(packet.substring(3));
				SocketManager.GAME_SEND_EHP_PACKET(_perso,templateID);
			break;			
			case 'T'://Demande des template de la catégorie
				int categ = Integer.parseInt(packet.substring(3));
				String allTemplate = World.getHdv(Math.abs(_perso.get_isTradingWith())).parseTemplate(categ);
				SocketManager.GAME_SEND_EHL_PACKET(_perso,categ,allTemplate);
			break;			
		}
	}
	
	private void Exchange_mountPark(String packet)
	{
		//Si dans un enclos
		if(_perso.getInMountPark() != null)
		{
			MountPark MP = _perso.getInMountPark();
			if(_perso.get_isTradingWith() > 0 || _perso.get_fight() != null)return;
			char c = packet.charAt(2);
			packet = packet.substring(3);
			int guid = -1;
			try
			{
				guid = Integer.parseInt(packet);
			}catch(Exception e){};
			switch(c)
			{
				case 'C'://Parcho => Etable (Stocker)
					if(guid == -1 || !_perso.hasItemGuid(guid))return;
					if(MP.get_size() <= MP.MountParkDATASize())
					{
						SocketManager.GAME_SEND_Im_PACKET(_perso, "1145");
						return;
					}
					Objet obj = World.getObjet(guid);
					//on prend la DD demandée
					int DDid = obj.getStats().getEffect(995);
					Dragodinde DD = World.getDragoByID(DDid);
					//FIXME mettre return au if pour ne pas créer des nouvelles dindes
					if(DD == null)
					{
						int color = Constants.getMountColorByParchoTemplate(obj.getTemplate().getID());
						if(color <1)return;
						DD = new Dragodinde(color);
					}
					//On enleve l'objet du Monde et du Perso
					_perso.removeItem(guid);
					World.removeItem(guid);
					//on ajoute la dinde a l'étable
					MP.addData(DD.get_id(), _perso.get_GUID());
					SQLManager.UPDATE_MOUNTPARK(MP);
					//On envoie les packet
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso,obj.getGuid());
					SocketManager.GAME_SEND_Ee_PACKET(_perso, '+', DD.parse());
				break;
				case 'c'://Etable => Parcho(Echanger)
					Dragodinde DD1 = World.getDragoByID(guid);
					//S'il n'a pas la dinde
					if(DD1 == null || !MP.getData().containsKey(DD1.get_id()))return;
					if(MP.getData().get(DD1.get_id()) != _perso.get_GUID() && 
						World.getPersonnage(MP.getData().get(DD1.get_id())).get_guild() != _perso.get_guild())
					{
						//Pas la même guilde, pas le même perso
						return;
					}
					if(MP.getData().get(DD1.get_id()) != _perso.get_GUID() && 
							World.getPersonnage(MP.getData().get(DD1.get_id())).get_guild() == _perso.get_guild() &&
							!_perso.getGuildMember().canDo(Constants.G_OTHDINDE))
					{
						//Même guilde, pas le droit
						SocketManager.GAME_SEND_Im_PACKET(_perso, "1101");
						return;
					}
					//on retire la dinde de l'étable
					MP.removeData(DD1.get_id());
					SQLManager.UPDATE_MOUNTPARK(MP);
					//On créer le parcho
					ObjTemplate T = Constants.getParchoTemplateByMountColor(DD1.get_color());
					Objet obj1 = T.createNewItem(1, false);
					//On efface les stats
					obj1.clearStats();
					//on ajoute la possibilité de voir la dinde
					obj1.getStats().addOneStat(995, DD1.get_id());
					obj1.addTxtStat(996, _perso.get_name());
					obj1.addTxtStat(997, DD1.get_nom());
					
					//On ajoute l'objet au joueur
					World.addObjet(obj1, true);
					_perso.addObjet(obj1, false);//Ne seras jamais identique de toute
					
					//Packets
					SocketManager.GAME_SEND_Ow_PACKET(_perso);
					SocketManager.GAME_SEND_Ee_PACKET(_perso,'-',DD1.get_id()+"");
				break;
				case 'g'://Equiper
					Dragodinde DD3 = World.getDragoByID(guid);
					//S'il n'a pas la dinde
					if(DD3 == null || !MP.getData().containsKey(DD3.get_id()) || _perso.getMount() != null)return;
					
					if(MP.getData().get(DD3.get_id()) != _perso.get_GUID() && 
							World.getPersonnage(MP.getData().get(DD3.get_id())).get_guild() != _perso.get_guild())
					{
						//Pas la même guilde, pas le même perso
						return;
					}
					if(MP.getData().get(DD3.get_id()) != _perso.get_GUID() && 
							World.getPersonnage(MP.getData().get(DD3.get_id())).get_guild() == _perso.get_guild() &&
							!_perso.getGuildMember().canDo(Constants.G_OTHDINDE))
					{
						//Même guilde, pas le droit
						SocketManager.GAME_SEND_Im_PACKET(_perso, "1101");
						return;
					}
					
					MP.removeData(DD3.get_id());
					SQLManager.UPDATE_MOUNTPARK(MP);
					_perso.setMount(DD3);
					
					//Packets
					SocketManager.GAME_SEND_Re_PACKET(_perso, "+", DD3);
					SocketManager.GAME_SEND_Ee_PACKET(_perso,'-',DD3.get_id()+"");
					SocketManager.GAME_SEND_Rx_PACKET(_perso);
				break;
				case 'p'://Equipé => Stocker
					//Si c'est la dinde équipé
					if(_perso.getMount()!=null?_perso.getMount().get_id() == guid:false)
					{
						//Si le perso est sur la monture on le fait descendre
						if(_perso.isOnMount())_perso.toogleOnMount();
						//Si ca n'a pas réussie, on s'arrete là (Items dans le sac ?)
						if(_perso.isOnMount())return;
						
						Dragodinde DD2 = _perso.getMount();
						MP.addData(DD2.get_id(), _perso.get_GUID());
						SQLManager.UPDATE_MOUNTPARK(MP);
						_perso.setMount(null);
						
						//Packets
						SocketManager.GAME_SEND_Ee_PACKET(_perso,'+',DD2.parse());
						SocketManager.GAME_SEND_Re_PACKET(_perso, "-", null);
						SocketManager.GAME_SEND_Rx_PACKET(_perso);
					}else//Sinon...
					{
						
					}
				break;
			}
		}
	}

	private void Exchange_doAgain()
	{
		if(_perso.getCurJobAction() != null)
			_perso.getCurJobAction().putLastCraftIngredients();
	}

	private void Exchange_isOK()
	{
		if(_perso.getCurJobAction() != null)
		{
			//Si pas action de craft, on s'arrete la
			if(!_perso.getCurJobAction().isCraft())return;
			_perso.getCurJobAction().startCraft(_perso);
		}
		if(_perso.get_curExchange() == null)return;
		_perso.get_curExchange().toogleOK(_perso.get_GUID());
	}

	private void Exchange_onMoveItem(String packet)
	{
		//Store
		if(_perso.get_isTradingWith() == _perso.get_GUID())
		{
			switch(packet.charAt(2))
			{
			case 'O'://Objets
				if(packet.charAt(3) == '+')
				{
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						int price  = Integer.parseInt(infos[2]);
						
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						
						if(qua > obj.getQuantity())
							qua = obj.getQuantity();
						
						_perso.addinStore(obj.getGuid(), price, qua);
						
					}catch(NumberFormatException e){};
				}else
				{
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						
						if(qua <= 0)return;
						
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						if(qua > obj.getQuantity())return;
						if(qua < obj.getQuantity()) qua = obj.getQuantity();
						
						_perso.removeFromStore(obj.getGuid(), qua);
					}catch(NumberFormatException e){};
				}
			break;
			}
			return;
		}
		//Percepteur
		if(_perso.get_isOnPercepteurID() != 0)
		{
			Percepteur perco = World.getPerco(_perso.get_isOnPercepteurID());
			if(perco == null || perco.get_inFight() > 0)return;
			switch(packet.charAt(2))
			{
			case 'G'://Kamas
				if(packet.charAt(3) == '-') //On retire
				{
					long P_Kamas = Integer.parseInt(packet.substring(4));
					long P_Retrait = perco.getKamas()-P_Kamas;
					if(P_Retrait < 0)
					{
						P_Retrait = 0;
						P_Kamas = perco.getKamas();
					}
					perco.setKamas(P_Retrait);
					_perso.addKamas(P_Kamas);
					SocketManager.GAME_SEND_STATS_PACKET(_perso);
					SocketManager.GAME_SEND_EsK_PACKET(_perso,"G"+perco.getKamas());
				}
			break;
			case 'O'://Objets
				if(packet.charAt(3) == '-') //On retire
				{
					String[] infos = packet.substring(4).split("\\|");
					int guid = 0;
					int qua = 0;
					try
					{
						guid = Integer.parseInt(infos[0]);
						qua  = Integer.parseInt(infos[1]);
					}catch(NumberFormatException e){};
					
					if(guid <= 0 || qua <= 0) return;
					
					Objet obj = World.getObjet(guid);
					if(obj == null)return;

					if(perco.HaveObjet(guid))
					{
						perco.removeFromPercepteur(_perso, guid, qua);
					}
					perco.LogObjetDrop(guid, obj);
				}
			break;
			}
			_perso.get_guild().addXp(perco.getXp());
			perco.LogXpDrop(perco.getXp());
			perco.setXp(0);
			SQLManager.UPDATE_GUILD(_perso.get_guild());
			return;
		}
		//HDV
		if(_perso.get_isTradingWith() < 0)
		{
			switch(packet.charAt(3))
			{
				case '-'://Retirer un objet de l'HDV
					int cheapestID = Integer.parseInt(packet.substring(4).split("\\|")[0]);
					int count = Integer.parseInt(packet.substring(4).split("\\|")[1]);
					if(count <= 0)return;
					
					_perso.get_compte().recoverItem(cheapestID,count);//Retire l'objet de la liste de vente du compte
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(_out,'-',"",cheapestID+"");
				break;
				case '+'://Mettre un objet en vente
					int itmID = Integer.parseInt(packet.substring(4).split("\\|")[0]);
					byte amount = Byte.parseByte(packet.substring(4).split("\\|")[1]);
					int price = Integer.parseInt(packet.substring(4).split("\\|")[2]);
					if(amount <= 0 || price <= 0)return;
					
					HDV curHdv = World.getHdv(Math.abs(_perso.get_isTradingWith()));
					int taxe = (int)(price * (curHdv.getTaxe()/100));
					
					
					if(!_perso.hasItemGuid(itmID))//Vérifie si le personnage a bien l'item spécifié et l'argent pour payer la taxe
						return;
					if(_perso.get_compte().countHdvItems(curHdv.getHdvID()) >= curHdv.getMaxItemCompte())
					{
						SocketManager.GAME_SEND_Im_PACKET(_perso, "058");
						return;
					}
					if(_perso.get_kamas() < taxe)
					{
						SocketManager.GAME_SEND_Im_PACKET(_perso, "176");
						return;
					}
					
					_perso.addKamas(taxe *-1);//Retire le montant de la taxe au personnage
					
					SocketManager.GAME_SEND_STATS_PACKET(_perso);//Met a jour les kamas du client
					
					Objet obj = World.getObjet(itmID);//Récupère l'item
					if(amount > obj.getQuantity())//S'il veut mettre plus de cette objet en vente que ce qu'il possède
						return;
					
					int rAmount = (int)(Math.pow(10,amount)/10);
					int newQua = (obj.getQuantity()-rAmount);
					
					if(newQua <= 0)//Si c'est plusieurs objets ensemble enleve seulement la quantité de mise en vente
					{
						_perso.removeItem(itmID);//Enlève l'item de l'inventaire du personnage
						SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso,itmID);//Envoie un packet au client pour retirer l'item de son inventaire
					}
					else
					{
						obj.setQuantity(obj.getQuantity() - rAmount);
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso,obj);
						
						Objet newObj = Objet.getCloneObjet(obj, rAmount);
						World.addObjet(newObj, true);
						obj = newObj;
					}
					
					HdvEntry toAdd = new HdvEntry(price,amount,_perso.get_compte().get_GUID(),obj);
					curHdv.addEntry(toAdd);	//Ajoute l'entry dans l'HDV
					
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(_out,'+',"",toAdd.parseToEmK());	//Envoie un packet pour ajouter l'item dans la fenetre de l'HDV du client
				break;
			}
			return;
		}
		//Metier
		if(_perso.getCurJobAction() != null)
		{
			//Si pas action de craft, on s'arrete la
			if(!_perso.getCurJobAction().isCraft())return;
			if(packet.charAt(2) == 'O')//Ajout d'objet
			{
				if(packet.charAt(3) == '+')
				{
					//FIXME gerer les packets du genre  EMO+173|5+171|5+172|5 (split sur '+' ?:/)
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						if(qua <= 0)return;
						if(!_perso.hasItemGuid(guid))return;
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						if(obj.getQuantity()<qua)
							qua = obj.getQuantity();
							_perso.getCurJobAction().modifIngredient(_perso,guid,qua);
					}catch(NumberFormatException e){};
				}else
				{
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						if(qua <= 0)return;
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						_perso.getCurJobAction().modifIngredient(_perso,guid,-qua);
					}catch(NumberFormatException e){};
				}
				
			}else
			if(packet.charAt(2) == 'R')
			{
				try
				{
					int c = Integer.parseInt(packet.substring(3));
					_perso.getCurJobAction().repeat(c,_perso);
				}catch(Exception e){};
			}
			return;
		}
		//Banque
		if(_perso.isInBank())
		{
			if(_perso.get_curExchange() != null)return;
			switch(packet.charAt(2))
			{
				case 'G'://Kamas
					long kamas = 0;
					try
					{
							kamas = Integer.parseInt(packet.substring(3));
					}catch(Exception e){};
					if(kamas == 0)return;
					
					if(kamas > 0)//Si On ajoute des kamas a la banque
					{
						if(_perso.get_kamas() < kamas)kamas = _perso.get_kamas();
						_perso.setBankKamas(_perso.getBankKamas()+kamas);//On ajoute les kamas a la banque
						_perso.set_kamas(_perso.get_kamas()-kamas);//On retire les kamas du personnage
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						SocketManager.GAME_SEND_EsK_PACKET(_perso,"G"+_perso.getBankKamas());
					}else
					{
						kamas = -kamas;//On repasse en positif
						if(_perso.getBankKamas() < kamas)kamas = _perso.getBankKamas();
						_perso.setBankKamas(_perso.getBankKamas()-kamas);//On retire les kamas de la banque
						_perso.set_kamas(_perso.get_kamas()+kamas);//On ajoute les kamas du personnage
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						SocketManager.GAME_SEND_EsK_PACKET(_perso,"G"+_perso.getBankKamas());
					}
				break;
				
				case 'O'://Objet
					int guid = 0;
					int qua = 0;
					try
					{
						guid = Integer.parseInt(packet.substring(4).split("\\|")[0]);
						qua = Integer.parseInt(packet.substring(4).split("\\|")[1]);
					}catch(Exception e){};
					if(guid == 0 || qua <= 0)return;
					
					switch(packet.charAt(3))
					{
						case '+'://Ajouter a la banque
							_perso.addInBank(guid,qua);
						break;
						
						case '-'://Retirer de la banque
							_perso.removeFromBank(guid,qua);
						break;
					}
				break;
			}
			return;
		}
		//Coffre
	    if(_perso.getInTrunk() != null)
        {
                if(_perso.get_curExchange() != null)return;
                Trunk t = _perso.getInTrunk();
                if(t == null) return;
               
                switch(packet.charAt(2))
                {
                	case 'G'://Kamas
                    	long kamas = 0;
                    	try
                    	{
                    		kamas = Integer.parseInt(packet.substring(3));
                        }catch(Exception e){};
                        if(kamas == 0)return;
                               
                        if(kamas > 0)//Si On ajoute des kamas au coffre
                        {
                            if(_perso.get_kamas() < kamas)kamas = _perso.get_kamas();
                            t.set_kamas(t.get_kamas() + kamas);//On ajoute les kamas au coffre
                            _perso.set_kamas(_perso.get_kamas()-kamas);//On retire les kamas du personnage
                            SocketManager.GAME_SEND_STATS_PACKET(_perso);
                        }else // On retire des kamas au coffre
                        {
                        	kamas = -kamas;//On repasse en positif
                        	if(t.get_kamas() < kamas)kamas = t.get_kamas();
                        	t.set_kamas(t.get_kamas()-kamas);//On retire les kamas de la banque
                         	_perso.set_kamas(_perso.get_kamas()+kamas);//On ajoute les kamas du personnage
                         	SocketManager.GAME_SEND_STATS_PACKET(_perso);
                        }
                        for(Personnage P : World.getOnlinePersos())
                        {
                        	if(P.getInTrunk() != null && _perso.getInTrunk().get_id() == P.getInTrunk().get_id())
                            {
                        		SocketManager.GAME_SEND_EsK_PACKET(P,"G"+t.get_kamas());
                         	}
                        }
                        SQLManager.UPDATE_TRUNK(t);
                    break;
              	
                	case 'O'://Objet
                		int guid = 0;
                		int qua = 0;
                		try
                		{
                			guid = Integer.parseInt(packet.substring(4).split("\\|")[0]);
                			qua = Integer.parseInt(packet.substring(4).split("\\|")[1]);
                		}catch(Exception e){};
                		if(guid == 0 || qua <= 0)return;
                               
                		switch(packet.charAt(3))
                		{
                			case '+'://Ajouter a la banque
                				t.addInTrunk(guid, qua, _perso);
                			break;
                                       
                			case '-'://Retirer de la banque
                				t.removeFromTrunk(guid,qua, _perso);
                			break;
                		}
                	break;
                }
                return;
        }
		if(_perso.get_curExchange() == null)return;
		switch(packet.charAt(2))
		{
			case 'O'://Objet ?
				if(packet.charAt(3) == '+')
				{
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						int quaInExch = _perso.get_curExchange().getQuaItem(guid, _perso.get_GUID());
						
						if(!_perso.hasItemGuid(guid))return;
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						
						if(qua > obj.getQuantity()-quaInExch)

							qua = obj.getQuantity()-quaInExch;
						if(qua <= 0)return;
						
						_perso.get_curExchange().addItem(guid,qua,_perso.get_GUID());
					}catch(NumberFormatException e){};
				}else
				{
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						
						if(qua <= 0)return;
						if(!_perso.hasItemGuid(guid))return;
						
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						if(qua > _perso.get_curExchange().getQuaItem(guid, _perso.get_GUID()))return;
						
						_perso.get_curExchange().removeItem(guid,qua,_perso.get_GUID());
					}catch(NumberFormatException e){};
				}
			break;
			case 'G'://Kamas
				try
				{
					long numb = Integer.parseInt(packet.substring(3));
					if(_perso.get_kamas() < numb)
						numb = _perso.get_kamas();
					_perso.get_curExchange().setKamas(_perso.get_GUID(), numb);
				}catch(NumberFormatException e){};
			break;
		}
	}

	private void Exchange_accept()
	{
		if(_perso.get_isTradingWith() == 0)return;
		Personnage target = World.getPersonnage(_perso.get_isTradingWith());
		if(target == null)return;
		SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(_out,1);
		SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(target.get_compte().getGameThread().get_out(),1);
		World.Exchange echg = new World.Exchange(target,_perso);
		_perso.setCurExchange(echg);
		_perso.set_isTradingWith(target.get_GUID());
		target.setCurExchange(echg);
		target.set_isTradingWith(_perso.get_GUID());
	}

	private void Exchange_onSellItem(String packet)
	{
		try
		{
			String[] infos = packet.substring(2).split("\\|");
			int guid = Integer.parseInt(infos[0]);
			int qua = Integer.parseInt(infos[1]);
			if(!_perso.hasItemGuid(guid))
			{
				SocketManager.GAME_SEND_SELL_ERROR_PACKET(_out);
				return;
			}
			_perso.sellItem(guid, qua);
		}catch(Exception e)
		{
			SocketManager.GAME_SEND_SELL_ERROR_PACKET(_out);
		}
	}

	private void Exchange_onBuyItem(String packet)
	{
		String[] infos = packet.substring(2).split("\\|");
		
        if (_perso.get_isTradingWith() > 0) 
        {
            Personnage seller = World.getPersonnage(_perso.get_isTradingWith());
            if (seller != null) 
            {
            	int itemID = 0;
            	int qua = 0;
            	int price = 0;
            	try
        		{
            		itemID = Integer.valueOf(infos[0]);
            		qua = Integer.valueOf(infos[1]);
        		}catch(Exception e){return;}
        		
                if (!seller.getStoreItems().containsKey(itemID) || qua <= 0) 
                {
                    SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
                    return;
                }
                price = seller.getStoreItems().get(itemID);
                Objet itemStore = World.getObjet(itemID);
                if(itemStore == null) return;
                
                if(qua > itemStore.getQuantity()) qua = itemStore.getQuantity();
                if(qua == itemStore.getQuantity())
                {
                	seller.getStoreItems().remove(itemStore.getGuid());
                	_perso.addObjet(itemStore, true);
                }else
                {
                	seller.getStoreItems().remove(itemStore.getGuid());
                	itemStore.setQuantity(itemStore.getQuantity()-qua);
                	SQLManager.SAVE_ITEM(itemStore);
                	seller.addStoreItem(itemStore.getGuid(), price);
                	
                	Objet clone = Objet.getCloneObjet(itemStore, qua);
                    SQLManager.SAVE_NEW_ITEM(clone);
                    _perso.addObjet(clone, true);
                }
	            //remove kamas
	            _perso.addKamas(-price * qua);
	            //add seller kamas
	            seller.addKamas(price * qua);
	            SQLManager.SAVE_PERSONNAGE(seller, true);
	            //send packets
	            SocketManager.GAME_SEND_STATS_PACKET(_perso);
	            SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(seller, _perso);
	            SocketManager.GAME_SEND_BUY_OK_PACKET(_out);
	            if(seller.getStoreItems().isEmpty())
	            {
	            	if(World.getSeller(seller.get_curCarte().get_id()) != null && World.getSeller(seller.get_curCarte().get_id()).contains(seller.get_GUID()))
	        		{
	        			World.removeSeller(seller.get_GUID(), seller.get_curCarte().get_id());
	        			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(seller.get_curCarte(), seller.get_GUID());
	        			Exchange_finish_buy();
	        		}
	            }
            }
            return;
        }
        
		try
		{
			int tempID = Integer.parseInt(infos[0]);
			int qua = Integer.parseInt(infos[1]);
			
			if(qua <= 0) return;
			
			ObjTemplate template = World.getObjTemplate(tempID);
			if(template == null)//Si l'objet demandé n'existe pas(ne devrait pas arrivé)
			{
				GameServer.addToLog(_perso.get_name()+" tente d'acheter l'itemTemplate "+tempID+" qui est inexistant");
				SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
				return;
			}
			if(!_perso.get_curCarte().getNPC(_perso.get_isTradingWith()).get_template().haveItem(tempID))//Si le PNJ ne vend pas l'objet voulue
			{
				GameServer.addToLog(_perso.get_name()+" tente d'acheter l'itemTemplate "+tempID+" que le present PNJ ne vend pas");
				SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
				return;
			}
			int prix = template.getPrix() * qua;
			if(_perso.get_kamas()<prix)//Si le joueur n'a pas assez de kamas
			{
				GameServer.addToLog(_perso.get_name()+" tente d'acheter l'itemTemplate "+tempID+" mais n'a pas l'argent necessaire");
				SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
				return;
			}
			Objet newObj = template.createNewItem(qua,false);
			long newKamas = _perso.get_kamas() - prix;
			_perso.set_kamas(newKamas);
			if(_perso.addObjet(newObj,true))//Return TRUE si c'est un nouvel item
				World.addObjet(newObj,true);
			SocketManager.GAME_SEND_BUY_OK_PACKET(_out);
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
		}catch(Exception e)
		{
			e.printStackTrace();
			SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
			return;
		};
	}

	private void Exchange_finish_buy()
	{
		if(_perso.get_isTradingWith() == 0 &&
		   _perso.get_curExchange() == null &&
		   _perso.getCurJobAction() == null &&
		   _perso.getInMountPark() == null &&
		   !_perso.isInBank() &&
		   _perso.get_isOnPercepteurID() == 0 &&
		   _perso.getInTrunk() == null)
			return;
		
		//Si échange avec un personnage
		if(_perso.get_curExchange() != null)
		{
			_perso.get_curExchange().cancel();
			_perso.set_isTradingWith(0);
			_perso.set_away(false);
			return;
		}
		//Si métier
		if(_perso.getCurJobAction() != null)
		{
			_perso.getCurJobAction().resetCraft();
		}
		//Si dans un enclos
		if(_perso.getInMountPark() != null)_perso.leftMountPark();
		//prop d'echange avec un joueur
		if(_perso.get_isTradingWith() > 0)
		{
			Personnage p = World.getPersonnage(_perso.get_isTradingWith());
			if(p != null)
			{
				if(p.isOnline())
				{
					PrintWriter out = p.get_compte().getGameThread().get_out();
					SocketManager.GAME_SEND_EV_PACKET(out);
					p.set_isTradingWith(0);
				}
			}
		}
		//Si perco
		if(_perso.get_isOnPercepteurID() != 0)
		{
			Percepteur perco = World.getPerco(_perso.get_isOnPercepteurID());
			if(perco == null) return;
			for(Personnage z : World.getGuild(perco.get_guildID()).getMembers())
			{
				if(z.isOnline())
				{
					SocketManager.GAME_SEND_gITM_PACKET(z, Percepteur.parsetoGuild(z.get_guild().get_id()));
					String str = "";
					str += "G"+perco.get_N1()+","+perco.get_N2();
					str += "|.|"+World.getCarte((short)perco.get_mapID()).getX()+"|"+World.getCarte((short)perco.get_mapID()).getY()+"|";
					str += _perso.get_name()+"|";
					str += perco.get_LogXp()+";";
					str += perco.get_LogItems();
					SocketManager.GAME_SEND_gT_PACKET(z, str);
				}
			}
			_perso.get_curCarte().RemoveNPC(perco.getGuid());
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), perco.getGuid());
			perco.DelPerco(perco.getGuid());
			SQLManager.DELETE_PERCO(perco.getGuid());
			_perso.set_isOnPercepteurID(0);
		}
		
		SQLManager.SAVE_PERSONNAGE(_perso,true);
		SocketManager.GAME_SEND_EV_PACKET(_out);
		_perso.set_isTradingWith(0);
		_perso.set_away(false);
		_perso.setInBank(false);
		_perso.setInTrunk(null);
	}

	private void Exchange_start(String packet)
	{
		if(packet.substring(2,4).equals("11"))//Ouverture HDV achat
		{
			if(_perso.get_isTradingWith() < 0)//Si déjà ouvert
				SocketManager.GAME_SEND_EV_PACKET(_out);
			
			if(_perso.getDeshonor() >= 5) 
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
				return;
			}
			
			HDV toOpen = World.getHdv(_perso.get_curCarte().get_id());
			
			if(toOpen == null) return;
			
			String info = "1,10,100;"+
						toOpen.getStrCategories()+
						";"+toOpen.parseTaxe()+
						";"+toOpen.getLvlMax()+
						";"+toOpen.getMaxItemCompte()+
						";-1;"+
						toOpen.getSellTime();
			SocketManager.GAME_SEND_ECK_PACKET(_perso,11,info);
			_perso.set_isTradingWith(0 - _perso.get_curCarte().get_id());	//Récupère l'ID de la map et rend cette valeur négative
			return;
		}
		else if(packet.substring(2,4).equals("10"))//Ouverture HDV vente
		{
			if(_perso.get_isTradingWith() < 0)//Si déjà ouvert
				SocketManager.GAME_SEND_EV_PACKET(_out);
			
			if(_perso.getDeshonor() >= 5) 
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
				return;
			}
			
			HDV toOpen = World.getHdv(_perso.get_curCarte().get_id());
			
			if(toOpen == null) return;
			
			String info = "1,10,100;"+
						toOpen.getStrCategories()+
						";"+toOpen.parseTaxe()+
						";"+toOpen.getLvlMax()+
						";"+toOpen.getMaxItemCompte()+
						";-1;"+
						toOpen.getSellTime();
			SocketManager.GAME_SEND_ECK_PACKET(_perso,10,info);
			_perso.set_isTradingWith(0 - _perso.get_curCarte().get_id());	//Récupère l'ID de la map et rend cette valeur négative
			
			SocketManager.GAME_SEND_HDVITEM_SELLING(_perso);
			return;
		}
		switch(packet.charAt(2))
		{
			case '0'://Si NPC
				try
				{
					int npcID = Integer.parseInt(packet.substring(4));
					NPC_tmpl.NPC npc = _perso.get_curCarte().getNPC(npcID);
					if(npc == null)return;
					SocketManager.GAME_SEND_ECK_PACKET(_out, 0, npcID+"");
					SocketManager.GAME_SEND_ITEM_VENDOR_LIST_PACKET(_out,npc);
					_perso.set_isTradingWith(npcID);
				}catch(NumberFormatException e){};
			break;
			case '1'://Si joueur
				try
				{
				int guidTarget = Integer.parseInt(packet.substring(4));
				Personnage target = World.getPersonnage(guidTarget);
				if(target == null )
				{
					SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(_out,'E');
					return;
				}
				if(target.get_curCarte()!= _perso.get_curCarte() || !target.isOnline())//Si les persos ne sont pas sur la meme map
				{
					SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(_out,'E');
					return;
				}
				if(target.is_away() || _perso.is_away() || target.get_isTradingWith() != 0)
				{
					SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(_out,'O');
					return;
				}
				SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(_out, _perso.get_GUID(), guidTarget,1);
				SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(target.get_compte().getGameThread().get_out(),_perso.get_GUID(), guidTarget,1);
				_perso.set_isTradingWith(guidTarget);
				target.set_isTradingWith(_perso.get_GUID());
			}catch(NumberFormatException e){}
			break;
            case '4'://StorePlayer
            	int pID = 0;
            	//int cellID = 0;//Inutile
            	try
				{
            		pID = Integer.valueOf(packet.split("\\|")[1]);
            		//cellID = Integer.valueOf(packet.split("\\|")[2]);
				}catch(NumberFormatException e){return;};
				if(_perso.get_isTradingWith() > 0 || _perso.get_fight() != null || _perso.is_away())return;
				Personnage seller = World.getPersonnage(pID);
				if(seller == null) return;
				_perso.set_isTradingWith(pID);
				SocketManager.GAME_SEND_ECK_PACKET(_perso, 4, seller.get_GUID()+"");
				SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(seller, _perso);
            break;
			case '6'://StoreItems
				if(_perso.get_isTradingWith() > 0 || _perso.get_fight() != null || _perso.is_away())return;
                _perso.set_isTradingWith(_perso.get_GUID());
                SocketManager.GAME_SEND_ECK_PACKET(_perso, 6, "");
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(_perso, _perso);
			break;
			case '8'://Si Percepteur
				try
				{
					int PercepteurID = Integer.parseInt(packet.substring(4));
					Percepteur perco = World.getPerco(PercepteurID);
					if(perco == null || perco.get_inFight() > 0 || perco.get_Exchange())return;
					perco.set_Exchange(true);
					SocketManager.GAME_SEND_ECK_PACKET(_out, 8, perco.getGuid()+"");
					SocketManager.GAME_SEND_ITEM_LIST_PACKET_PERCEPTEUR(_out, perco);
					_perso.set_isTradingWith(perco.getGuid());
					_perso.set_isOnPercepteurID(perco.getGuid());
				}catch(NumberFormatException e){};
			break;
		}
	}

	private void parse_environementPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'D'://Change direction
				Environement_change_direction(packet);
			break;
			
			case 'U'://Emote
				Environement_emote(packet);
			break;
		}
	}

	private void Environement_emote(String packet)
	{
		int emote = -1;
		try
		{
			emote = Integer.parseInt(packet.substring(2));
		}catch(Exception e){};
		if(emote == -1)return;
		if(_perso == null)return;
		if(_perso.get_fight() != null)return;//Pas d'émote en combat
		
		switch(emote)//effets spéciaux des émotes
		{
			case 19://s'allonger 
			case 1:// s'asseoir
				_perso.setSitted(!_perso.isSitted());
			break;
		}
		if(_perso.emoteActive() == emote)_perso.setEmoteActive(0);
		else _perso.setEmoteActive(emote);
		
		System.out.println("Set Emote "+_perso.emoteActive());
		System.out.println("Is sitted "+_perso.isSitted());
		
		SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(_perso.get_curCarte(), _perso.get_GUID(), _perso.emoteActive());
	}

	private void Environement_change_direction(String packet)
	{
		try
		{
			if(_perso.get_fight() != null)return;
			int dir = Integer.parseInt(packet.substring(2));
			_perso.set_orientation(dir);
			SocketManager.GAME_SEND_eD_PACKET_TO_MAP(_perso.get_curCarte(),_perso.get_GUID(),dir);
		}catch(NumberFormatException e){return;};
	}

	private void parseSpellPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'B':
				boostSort(packet);
			break;
			case 'F'://Oublie de sort
				forgetSpell(packet);
			break;
			case'M':
				addToSpellBook(packet);
			break;
		}
	}

	private void addToSpellBook(String packet)
	{
		try
		{
			int SpellID = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			int Position = Integer.parseInt(packet.substring(2).split("\\|")[1]);
			SortStats Spell = _perso.getSortStatBySortIfHas(SpellID);
			
			if(Spell != null)
			{
				_perso.set_SpellPlace(SpellID, CryptManager.getHashedValueByInt(Position));
			}
				
			SocketManager.GAME_SEND_BN(_out);
		}catch(Exception e){};
	}

	private void boostSort(String packet)
	{
		try
		{
			int id = Integer.parseInt(packet.substring(2));
			GameServer.addToLog("Info: "+_perso.get_name()+": Tente BOOST sort id="+id);
			if(_perso.boostSpell(id))
			{
				GameServer.addToLog("Info: "+_perso.get_name()+": OK pour BOOST sort id="+id);
				SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCED(_out, id, _perso.getSortStatBySortIfHas(id).getLevel());
				SocketManager.GAME_SEND_STATS_PACKET(_perso);
			}else
			{
				GameServer.addToLog("Info: "+_perso.get_name()+": Echec BOOST sort id="+id);
				SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(_out);
				return;
			}
		}catch(NumberFormatException e){SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(_out);return;};
	}

	private void forgetSpell(String packet)
	{
		if(!_perso.isForgetingSpell())return;
		
		int id = Integer.parseInt(packet.substring(2));
		
		if(Ancestra.CONFIG_DEBUG) GameServer.addToLog("Info: "+_perso.get_name()+": Tente Oublie sort id="+id);
		
		if(_perso.forgetSpell(id))
		{
			if(Ancestra.CONFIG_DEBUG) GameServer.addToLog("Info: "+_perso.get_name()+": OK pour Oublie sort id="+id);
			SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCED(_out, id, _perso.getSortStatBySortIfHas(id).getLevel());
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			_perso.setisForgetingSpell(false);
		}
	}

	private void parseFightPacket(String packet)
	{
		try
		{
			switch(packet.charAt(1))
			{
				case 'D'://Détails d'un combat (liste des combats)
					int key = -1;
					try
					{
						key = Integer.parseInt(packet.substring(2).replace(((int)0x0)+"", ""));
					}catch(Exception e){};
					if(key == -1)return;
					SocketManager.GAME_SEND_FIGHT_DETAILS(_out,_perso.get_curCarte().get_fights().get(key));
				break;
				
				case 'H'://Aide
					if(_perso.get_fight() == null)return;
					_perso.get_fight().toggleHelp(_perso.get_GUID());
				break;
				
				case 'L'://Lister les combats
					SocketManager.GAME_SEND_FIGHT_LIST_PACKET(_out, _perso.get_curCarte());
				break;
				case 'N'://Bloquer le combat
					if(_perso.get_fight() == null)return;
					_perso.get_fight().toggleLockTeam(_perso.get_GUID());
				break;
				case 'P'://Seulement le groupe
					if(_perso.get_fight() == null || _perso.getGroup() == null)return;
					_perso.get_fight().toggleOnlyGroup(_perso.get_GUID());
				break;
				case 'S'://Bloquer les specs
					if(_perso.get_fight() == null)return;
					_perso.get_fight().toggleLockSpec(_perso.get_GUID());
				break;
				
			}
		}catch(Exception e){e.printStackTrace();};
	}

	private void parseBasicsPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A'://Console
				Basic_console(packet);
			break;
			case 'D':
				Basic_send_Date_Hour();
			break;
			case 'M':
				Basic_chatMessage(packet);
			break;
			case 'W':
				Basic_infosmessage(packet);
			break;
			case 'S':
				_perso.emoticone(packet.substring(2));
			break;
			case 'Y':
				Basic_state(packet);
			break;
		}
	}
	public void Basic_state(String packet)
	{
		switch(packet.charAt(2))
		{
			case 'A': //Absent
				if(_perso._isAbsent)
				{

					SocketManager.GAME_SEND_Im_PACKET(_perso, "038");

					_perso._isAbsent = false;
				}
				else

				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "037");
					_perso._isAbsent = true;
				}
			break;
			case 'I': //Invisible
				if(_perso._isInvisible)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "051");
					_perso._isInvisible = false;
				}
				else
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "050");
					_perso._isInvisible = true;
				}
			break;
		}
	}
	
	public Personnage getPerso()
	{
		return _perso;
	}
	  
	private void Basic_console(String packet)
	{
		if(command == null) command = new Commands(_perso);
		command.consoleCommand(packet);
	}

	public void closeSocket()
	{
		try {
			this._s.close();
		} catch (IOException e) {}
	}

	private void Basic_chatMessage(String packet)
	{
		String msg = "";
		if(_perso.isMuted())
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1124;"+_perso.get_compte()._muteTimer.getInitialDelay());//FIXME
			return;
		}
		packet = packet.replace("<", "");
		packet = packet.replace(">", "");
		if(packet.length() == 3)return;
		switch(packet.charAt(2))
		{
			case '*'://Canal noir
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				msg = packet.split("\\|",2)[1];
				
				//Commandes joueurs
				if(msg.charAt(0) == '.')
				{
					//Retour au point de sauvegarde
					if(msg.length() > 7 && msg.substring(1, 8).equalsIgnoreCase("command"))
					{
						SocketManager.GAME_SEND_MESSAGE(_perso, "Commandes Disponibles : \n.start\n.infos\n.save", Ancestra.CONFIG_MOTD_COLOR);
						return;
					}else
					if(msg.length() > 5 && msg.substring(1, 6).equalsIgnoreCase("start"))
					{
						if(_perso.get_fight() != null)return;
						_perso.warpToSavePos();
						return;
					}else
					if(msg.length() > 5 && msg.substring(1, 6).equalsIgnoreCase("infos"))
					{
						long uptime = System.currentTimeMillis() - Ancestra.gameServer.getStartTime();
						int jour = (int) (uptime/(1000*3600*24));
						uptime %= (1000*3600*24);
						int hour = (int) (uptime/(1000*3600));
						uptime %= (1000*3600);
						int min = (int) (uptime/(1000*60));
						uptime %= (1000*60);
						int sec = (int) (uptime/(1000));
						
						String mess =	"===========\n"+Ancestra.makeHeader()
							+			"Uptime: "+jour+"j "+hour+"h "+min+"m "+sec+"s\n"
							+			"Joueurs en lignes: "+Ancestra.gameServer.getPlayerNumber()+"\n"
							+			"Record de connexion: "+Ancestra.gameServer.getMaxPlayer()+"\n"
							+			"===========";
						SocketManager.GAME_SEND_MESSAGE(_perso, mess, Ancestra.CONFIG_MOTD_COLOR);
						return;
					}else
					if(msg.length() > 4 && msg.substring(1, 5).equalsIgnoreCase("save"))
					{
						if((System.currentTimeMillis() - _timeLastsave) < 360000)
						{
							return;
						}
						_timeLastsave = System.currentTimeMillis();
						if(_perso.get_fight() != null)return;
						SQLManager.SAVE_PERSONNAGE(_perso,true);
						SocketManager.GAME_SEND_MESSAGE(_perso,  _perso.get_name()+" sauvegardé.", Ancestra.CONFIG_MOTD_COLOR);
						return;
					}
				}
				if(_perso.get_fight() == null)
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(_perso.get_curCarte(), "", _perso.get_GUID(), _perso.get_name(), msg);
				else
					SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(_perso.get_fight(), 7, "", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case '#'://Canal Equipe
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.get_fight() != null)
				{
					msg = packet.split("\\|",2)[1];
					int team = _perso.get_fight().getTeamID(_perso.get_GUID());
					if(team == -1)return;
					SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(_perso.get_fight(), team, "#", _perso.get_GUID(), _perso.get_name(), msg);
				}
			break;
			case '$'://Canal groupe
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.getGroup() == null)break;
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_GROUP(_perso.getGroup(), "$", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			
			case ':'://Canal commerce
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				long l;
				if((l = System.currentTimeMillis() - _timeLastTradeMsg) < Ancestra.FLOOD_TIME)
				{
					l = (Ancestra.FLOOD_TIME  - l)/1000;//On calcul la différence en secondes
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0115;"+((int)Math.ceil(l)+1));
					return;
				}
				_timeLastTradeMsg = System.currentTimeMillis();
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_ALL(":", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case '@'://Canal Admin
				if(_perso.get_compte().get_gmLvl() ==0)return;
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case '?'://Canal recrutement
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				long j;
				if((j = System.currentTimeMillis() - _timeLastRecrutmentMsg) < Ancestra.FLOOD_TIME)
				{
					j = (Ancestra.FLOOD_TIME  - j)/1000;//On calcul la différence en secondes
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0115;"+((int)Math.ceil(j)+1));
					return;
				}
				_timeLastRecrutmentMsg = System.currentTimeMillis();
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_ALL("?", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case '%'://Canal guilde
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.get_guild() == null)return;
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_GUILD(_perso.get_guild(), "%", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case 0xC2://Canal 
			break;
			case '!'://Alignement
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.get_align() == 0) return;
				if(_perso.getDeshonor() >= 1) 
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
					return;
				}
				long k;
				if((k = System.currentTimeMillis() - _timeLastAlignMsg) < Ancestra.FLOOD_TIME)
				{
					k = (Ancestra.FLOOD_TIME  - k)/1000;//On calcul la différence en secondes
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0115;"+((int)Math.ceil(k)+1));
					return;
				}
				_timeLastAlignMsg = System.currentTimeMillis();
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_ALIGN("!", _perso.get_GUID(), _perso.get_name(), msg, _perso);
			break;
			default:
				String nom = packet.substring(2).split("\\|")[0];
				msg = packet.split("\\|",2)[1];
				if(nom.length() <= 1)
					GameServer.addToLog("ChatHandler: Chanel non gere : "+nom);
				else
				{
					Personnage target = World.getPersoByName(nom);
					if(target == null)//si le personnage n'existe pas
					{
						SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_out, nom);
						return;
					}
					if(target.get_compte() == null)
					{
						SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_out, nom);
						return;
					}
					if(target.get_compte().getGameThread() == null)//si le perso n'est pas co
					{
						SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_out, nom);
						return;
					}
					if(target.get_compte().isEnemyWith(_perso.get_compte().get_GUID()) == true || !target.isDispo(_perso))
					{
						SocketManager.GAME_SEND_Im_PACKET(_perso, "114;"+target.get_name());
						return;
					}
					SocketManager.GAME_SEND_cMK_PACKET(target, "F", _perso.get_GUID(), _perso.get_name(), msg);
					SocketManager.GAME_SEND_cMK_PACKET(_perso, "T", target.get_GUID(), target.get_name(), msg);
				}
			break;
		}
	}

	private void Basic_send_Date_Hour()
	{
		SocketManager.GAME_SEND_SERVER_DATE(_out);
		SocketManager.GAME_SEND_SERVER_HOUR(_out);
	}
	
	private void Basic_infosmessage(String packet)
	{
			packet = packet.substring(2);
			Personnage T = World.getPersoByName(packet);
			if(T == null) return;
			SocketManager.GAME_SEND_BWK(_perso, T.get_compte().get_pseudo()+"|1|"+T.get_name()+"|-1");
	}

	private void parseGamePacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A':
				if(_perso == null)return;
					parseGameActionPacket(packet);
			break;
			case 'C':
				if(_perso == null)return;
				_perso.sendGameCreate();
			break;
			case 'f':
				Game_on_showCase(packet);
			break;
			case 'I':
				Game_on_GI_packet();
			break;
			case 'K':
				Game_on_GK_packet(packet);
			break;
			case 'P'://PvP Toogle
				_perso.toggleWings(packet.charAt(2));
			break;
			case 'p':
				Game_on_ChangePlace_packet(packet);
			break;
			case 'Q':
				Game_onLeftFight(packet);
			break;
			case 'R':
				Game_on_Ready(packet);
			break;
			case 't':
				if(_perso.get_fight() == null)return;
				_perso.get_fight().playerPass(_perso);
			break;
		}
	}

	
	private void Game_onLeftFight(String packet)
	{
		int targetID = -1;
		if(!packet.substring(2).isEmpty())
		{
			try
			{
				targetID = Integer.parseInt(packet.substring(2));
			}catch(Exception e){};
		}
		if(_perso.get_fight() == null)return;
		if(targetID > 0)//Expulsion d'un joueurs autre que soi-meme
		{
			Personnage target = World.getPersonnage(targetID);
			//On ne quitte pas un joueur qui : est null, ne combat pas, n'est pas de ça team.
			if(target == null || target.get_fight() == null || target.get_fight().getTeamID(target.get_GUID()) != _perso.get_fight().getTeamID(_perso.get_GUID()))return;
			_perso.get_fight().leftFight(_perso, target);
			
		}else
		{
			_perso.get_fight().leftFight(_perso, null);
		}
	}

	private void Game_on_showCase(String packet)
	{
		if(_perso == null)return;
		if(_perso.get_fight() == null)return;
		if(_perso.get_fight().get_state() != Constants.FIGHT_STATE_ACTIVE)return;
		int cellID = -1;
		try
		{
			cellID = Integer.parseInt(packet.substring(2));
		}catch(Exception e){};
		if(cellID == -1)return;
		_perso.get_fight().showCaseToTeam(_perso.get_GUID(),cellID);
	}

	private void Game_on_Ready(String packet)
	{
		if(_perso.get_fight() == null)return;
		if(_perso.get_fight().get_state() != Constants.FIGHT_STATE_PLACE)return;
		_perso.set_ready(packet.substring(2).equalsIgnoreCase("1"));
		_perso.get_fight().verifIfAllReady();
		SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(_perso.get_fight(),3,_perso.get_GUID(),packet.substring(2).equalsIgnoreCase("1"));
	}

	private void Game_on_ChangePlace_packet(String packet)
	{
		if(_perso.get_fight() == null)return;
		try
		{
			int cell = Integer.parseInt(packet.substring(2));
			_perso.get_fight().changePlace( _perso, cell);
		}catch(NumberFormatException e){return;};
	}

	private void Game_on_GK_packet(String packet)
	{	
		int GameActionId = -1;
		String[] infos = packet.substring(3).split("\\|");
		try
		{
			GameActionId = Integer.parseInt(infos[0]);
		}catch(Exception e){return;};
		if(GameActionId == -1)return;
		GameAction GA = _actions.get(GameActionId);
		if(GA == null)return;
		boolean isOk = packet.charAt(2) == 'K';
		
		switch(GA._actionID)
		{
			case 1://Deplacement
				if(isOk)
				{
					//Hors Combat
					if(_perso.get_fight() == null)
					{
						_perso.get_curCell().removePlayer(_perso.get_GUID());
						SocketManager.GAME_SEND_BN(_out);
						String path = GA._args;
						//On prend la case ciblée
						Case nextCell = _perso.get_curCarte().getCase(CryptManager.cellCode_To_ID(path.substring(path.length()-2)));
						Case targetCell = _perso.get_curCarte().getCase(CryptManager.cellCode_To_ID(GA._packet.substring(GA._packet.length()-2)));
						
						//On définie la case et on ajoute le personnage sur la case
						_perso.set_curCell(nextCell);
						_perso.set_orientation(CryptManager.getIntByHashedValue(path.charAt(path.length()-3)));
						_perso.get_curCell().addPerso(_perso);
						if(!_perso._isGhosts) _perso.set_away(false);
						
						if(targetCell.getObject() != null)
						{
							//Si c'est une "borne" comme Emotes, ou Création guilde
							if(targetCell.getObject().getID() == 1324)
							{
								Constants.applyPlotIOAction(_perso,_perso.get_curCarte().get_id(),targetCell.getID());
							}
							//Statues phoenix
							else if(targetCell.getObject().getID() == 542)
							{
								if(_perso._isGhosts) _perso.set_Alive();
							}
						}
						_perso.get_curCarte().onPlayerArriveOnCell(_perso,_perso.get_curCell().getID());
					}
					else//En combat
					{
						_perso.get_fight().onGK(_perso);
						return;
					}
					
				}
				else
				{
					//Si le joueur s'arrete sur une case
					int newCellID = -1;
					try
					{
						newCellID = Integer.parseInt(infos[1]);
					}catch(Exception e){return;};
					if(newCellID == -1)return;
					String path = GA._args;
					_perso.get_curCell().removePlayer(_perso.get_GUID());
					_perso.set_curCell(_perso.get_curCarte().getCase(newCellID));
					_perso.set_orientation(CryptManager.getIntByHashedValue(path.charAt(path.length()-3)));
					_perso.get_curCell().addPerso(_perso);
					SocketManager.GAME_SEND_BN(_out);
				}
			break;
			
			case 500://Action Sur Map
				_perso.finishActionOnCell(GA);
			break;

		}
		removeAction(GA);
	}

	private void Game_on_GI_packet() 
	{
		if(_perso.get_fight() != null)
		{
			//Only percepteur
			SocketManager.GAME_SEND_MAP_GMS_PACKETS(_perso.get_curCarte(), _perso);
			SocketManager.GAME_SEND_GDK_PACKET(_out);
			return;
		}
		//Enclos
		SocketManager.GAME_SEND_Rp_PACKET(_perso, _perso.get_curCarte().getMountPark());
		//Maisons
		House.LoadHouse(_perso, _perso.get_curCarte().get_id());
		//Objets sur la carte
		SocketManager.GAME_SEND_MAP_GMS_PACKETS(_perso.get_curCarte(), _perso);
		SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS(_perso.get_compte().getGameThread().get_out(), _perso.get_curCarte());
		SocketManager.GAME_SEND_MAP_NPCS_GMS_PACKETS(_out,_perso.get_curCarte());
		SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(_out,_perso.get_curCarte());
		SocketManager.GAME_SEND_MAP_OBJECTS_GDS_PACKETS(_out,_perso.get_curCarte());
		SocketManager.GAME_SEND_GDK_PACKET(_out);
		SocketManager.GAME_SEND_MAP_FIGHT_COUNT(_out, _perso.get_curCarte());
		SocketManager.GAME_SEND_MERCHANT_LIST(_perso, _perso.get_curCarte().get_id());
		//Les drapeau de combats
		Fight.FightStateAddFlag(_perso.get_curCarte(), _perso);
		//items au sol
		_perso.get_curCarte().sendFloorItems(_perso);
	}

	private void parseGameActionPacket(String packet)
	{
		int actionID;
		try
		{
			actionID = Integer.parseInt(packet.substring(2,5));
		}catch(NumberFormatException e){return;};
		
		int nextGameActionID = 0;
		if(_actions.size() > 0)
		{
			//On prend le plus haut GameActionID + 1
			nextGameActionID = (Integer)(_actions.keySet().toArray()[_actions.size()-1])+1;
		}
		GameAction GA = new GameAction(nextGameActionID,actionID,packet);
		
		switch(actionID)
		{
			case 1://Deplacement
				game_parseDeplacementPacket(GA);
			break;
			
			case 300://Sort
				game_tryCastSpell(packet);
			break;
			
			case 303://Attaque CaC
				game_tryCac(packet);
			break;
			
			case 500://Action Sur Map
				game_action(GA);
			break;
			
			case 507://Panneau intérieur de la maison
				house_action(packet);
			break;
			
			case 618://Mariage oui
				_perso.setisOK(Integer.parseInt(packet.substring(5,6)));
				SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(_perso.get_curCarte(), "", _perso.get_GUID(), _perso.get_name(), "Oui");
				if(World.getMarried(0).getisOK() > 0 && World.getMarried(1).getisOK() > 0)
				{
					World.Wedding(World.getMarried(0), World.getMarried(1), 1);
				}
				if(World.getMarried(0) != null && World.getMarried(1) != null)
				{
					World.PriestRequest((World.getMarried(0)==_perso?World.getMarried(1):World.getMarried(0)), (World.getMarried(0)==_perso?World.getMarried(1).get_curCarte():World.getMarried(0).get_curCarte()), _perso.get_isTalkingWith());
				}
			break;
			case 619://Mariage non
				_perso.setisOK(0);
				SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(_perso.get_curCarte(), "", _perso.get_GUID(), _perso.get_name(), "Non");
				World.Wedding(World.getMarried(0), World.getMarried(1), 0);
			break;
			
			case 900://Demande Defie
				game_ask_duel(packet);
			break;
			case 901://Accepter Defie
				game_accept_duel(packet);
			break;
			case 902://Refus/Anuler Defie
				game_cancel_duel(packet);
			break;
			case 903://Rejoindre combat
				game_join_fight(packet);
			break;
			case 906://Agresser
				game_aggro(packet);
			break;
			case 909://Perco
				game_perco(packet);
			break;
		}	
	}

	private void house_action(String packet)
	{
		int actionID = Integer.parseInt(packet.substring(5));
		House h = _perso.getInHouse();
		if(h == null) return;
		switch(actionID)
		{
			case 81://Vérouiller maison
				h.Lock(_perso);
			break;
			case 97://Acheter maison
				h.BuyIt(_perso);
			break;
			case 98://Vendre
			case 108://Modifier prix de vente
				h.SellIt(_perso);
			break;
		}
	}
	
	
	private void game_perco(String packet)
	{
		try
		{
			if(_perso == null)return;
			if(_perso.get_fight() != null)return;
			if(_perso.get_isTalkingWith() != 0 ||
			   _perso.get_isTradingWith() != 0 ||
			   _perso.getCurJobAction() != null ||
			   _perso.get_curExchange() != null ||
			   _perso.is_away())
					{
						return;
					}
			int id = Integer.parseInt(packet.substring(5));
			Percepteur target = World.getPerco(id);
			if(target == null || target.get_inFight() > 0) return;
			if(target.get_Exchange())
			{
				
				SocketManager.GAME_SEND_Im_PACKET(_perso, "1180");
				return;
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_MAP(_perso.get_curCarte(),"", 909, _perso.get_GUID()+"", id+"");
			_perso.get_curCarte().startFigthVersusPercepteur(_perso, target);
		}catch(Exception e){};
	}
	
	private void game_aggro(String packet)
	{
		try
		{
			if(_perso == null)return;
			if(_perso.get_fight() != null)return;
			int id = Integer.parseInt(packet.substring(5));
			Personnage target = World.getPersonnage(id);
			if(target == null || !target.isOnline() || target.get_fight() != null
			|| target.get_curCarte().get_id() != _perso.get_curCarte().get_id()
			|| target.get_align() == _perso.get_align()
			|| _perso.get_curCarte().get_placesStr().equalsIgnoreCase("|")
			|| !target.canAggro())
				return;
			
			if(target.get_align() == 0) 
			{
				_perso.setDeshonor(_perso.getDeshonor()+1);
				SocketManager.GAME_SEND_Im_PACKET(_perso, "084;1");
			}

			_perso.toggleWings('+');
			SocketManager.GAME_SEND_GA_PACKET_TO_MAP(_perso.get_curCarte(),"", 906, _perso.get_GUID()+"", id+"");
			_perso.get_curCarte().newFight(_perso, target, Constants.FIGHT_TYPE_AGRESSION);
		}catch(Exception e){};
	}

	private void game_action(GameAction GA)
	{
		String packet = GA._packet.substring(5);
		int cellID = -1;
		int actionID = -1;
		
		try
		{
			cellID = Integer.parseInt(packet.split(";")[0]);
			actionID = Integer.parseInt(packet.split(";")[1]);
		}catch(Exception e){}
		//Si packet invalide, ou cellule introuvable
		if(cellID == -1 || actionID == -1 || _perso == null || _perso.get_curCarte() == null ||
				_perso.get_curCarte().getCase(cellID) == null)
			return;
		GA._args = cellID+";"+actionID;
		_perso.get_compte().getGameThread().addAction(GA);
		_perso.startActionOnCell(GA);
	}

	private void game_tryCac(String packet)
	{
		try
		{
			if(_perso.get_fight() ==null)return;
			int cellID = -1;
			try
			{
				cellID = Integer.parseInt(packet.substring(5));
			}catch(Exception e){return;};
			
			_perso.get_fight().tryCaC(_perso,cellID);
		}catch(Exception e){};
	}

	private void game_tryCastSpell(String packet)
	{
		try
		{
			String[] splt = packet.split(";");
			int spellID = Integer.parseInt(splt[0].substring(5));
			int caseID = Integer.parseInt(splt[1]);
			if(_perso.get_fight() != null)
			{
				SortStats SS = _perso.getSortStatBySortIfHas(spellID);
				if(SS == null)return;
				_perso.get_fight().tryCastSpell(_perso.get_fight().getFighterByPerso(_perso),SS,caseID);
			}
		}catch(NumberFormatException e){return;};
	}

	private void game_join_fight(String packet)
	{
		String[] infos = packet.substring(5).split(";");
		if(infos.length == 1)
		{
			try
			{
				Fight F = _perso.get_curCarte().getFight(Integer.parseInt(infos[0]));
				F.joinAsSpect(_perso);
			}catch(Exception e){return;};
		}else
		{
			try
			{
				int guid = Integer.parseInt(infos[1]);
				if(_perso.is_away())
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(_out,'o',guid);
					return;
				}
				if(World.getPersonnage(guid) == null)return;
				World.getPersonnage(guid).get_fight().joinFight(_perso,guid);
			}catch(Exception e){return;};
		}
	}

	private void game_accept_duel(String packet)
	{
		int guid = -1;
		try{guid = Integer.parseInt(packet.substring(5));}catch(NumberFormatException e){return;};
		if(_perso.get_duelID() != guid || _perso.get_duelID() == -1)return;
		SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_duelID(),_perso.get_GUID());
		Fight fight = _perso.get_curCarte().newFight(World.getPersonnage(_perso.get_duelID()),_perso,Constants.FIGHT_TYPE_CHALLENGE);
		_perso.set_fight(fight);
		World.getPersonnage(_perso.get_duelID()).set_fight(fight);
		
	}

	private void game_cancel_duel(String packet)
	{
		try
		{
			if(_perso.get_duelID() == -1)return;
			SocketManager.GAME_SEND_CANCEL_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_duelID(),_perso.get_GUID());
			World.getPersonnage(_perso.get_duelID()).set_away(false);
			World.getPersonnage(_perso.get_duelID()).set_duelID(-1);
			_perso.set_away(false);
			_perso.set_duelID(-1);	
		}catch(NumberFormatException e){return;};
	}

	private void game_ask_duel(String packet)
	{
		if(_perso.get_curCarte().get_placesStr().equalsIgnoreCase("|"))
		{
			SocketManager.GAME_SEND_DUEL_Y_AWAY(_out, _perso.get_GUID());
			return;
		}
		try
		{
			int guid = Integer.parseInt(packet.substring(5));
			if(_perso.is_away() || _perso.get_fight() != null){SocketManager.GAME_SEND_DUEL_Y_AWAY(_out, _perso.get_GUID());return;}
			Personnage Target = World.getPersonnage(guid);
			if(Target == null) return;
			if(Target.is_away() || Target.get_fight() != null || Target.get_curCarte().get_id() != _perso.get_curCarte().get_id()){SocketManager.GAME_SEND_DUEL_E_AWAY(_out, _perso.get_GUID());return;}
			_perso.set_duelID(guid);
			_perso.set_away(true);
			World.getPersonnage(guid).set_duelID(_perso.get_GUID());
			World.getPersonnage(guid).set_away(true);
			SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_GUID(),guid);
		}catch(NumberFormatException e){return;}
	}

	private void game_parseDeplacementPacket(GameAction GA)
	{
		String path = GA._packet.substring(5);
		if(_perso.get_fight() == null)
		{
			if(_perso.getPodUsed() > _perso.getMaxPod())
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "112");
				SocketManager.GAME_SEND_GA_PACKET(_out, "", "0", "", "");
				removeAction(GA);
				return;
			}
			AtomicReference<String> pathRef = new AtomicReference<String>(path);
			int result = Pathfinding.isValidPath(_perso.get_curCarte(),_perso.get_curCell().getID(),pathRef, null);
			
			//Si déplacement inutile
			if(result == 0)
			{
				SocketManager.GAME_SEND_GA_PACKET(_out, "", "0", "", "");
				removeAction(GA);
				return;
			}
			if(result != -1000 && result < 0)result = -result;
			
			//On prend en compte le nouveau path
			path = pathRef.get();
			//Si le path est invalide
			if(result == -1000)
			{
				GameServer.addToLog(_perso.get_name()+"("+_perso.get_GUID()+") Tentative de  deplacement avec un path invalide");
				path = CryptManager.getHashedValueByInt(_perso.get_orientation())+CryptManager.cellID_To_Code(_perso.get_curCell().getID());	
			}
			//On sauvegarde le path dans la variable
			GA._args = path;
			
			SocketManager.GAME_SEND_GA_PACKET_TO_MAP(_perso.get_curCarte(), ""+GA._id, 1, _perso.get_GUID()+"", "a"+CryptManager.cellID_To_Code(_perso.get_curCell().getID())+path);
			addAction(GA);
			if(_perso.isSitted())_perso.setSitted(false);
			_perso.set_away(true);
		}else
		{
			Fighter F = _perso.get_fight().getFighterByPerso(_perso);
			if(F == null)return;
			GA._args = path;
			_perso.get_fight().fighterDeplace(F,GA);
		}
	}

	public PrintWriter get_out() {
		return _out;
	}
	
	public void kick()
	{
		try
		{
			Ancestra.gameServer.delClient(this);
			
    		if(_compte != null)
    		{
    			_compte.deconnexion();
    		}
    		if(!_s.isClosed())
    		_s.close();
    		_in.close();
    		_out.close();
    		_t.interrupt();
		}catch(IOException e1){e1.printStackTrace();};
	}

	private void parseAccountPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A':
				String[] infos = packet.substring(2).split("\\|");
				if(SQLManager.persoExist(infos[0]))
				{
					SocketManager.GAME_SEND_NAME_ALREADY_EXIST(_out);
					return;
				}
				//Validation du nom du personnage
				boolean isValid = true;
				String name = infos[0].toLowerCase();
				//Vérifie d'abord si il contient des termes définit
				if(name.length() > 20
						|| name.contains("mj")
						|| name.contains("modo")
						|| name.contains("admin"))
				{
					isValid = false;
				}
				//Si le nom passe le test, on vérifie que les caractère entré sont correct.
				if(isValid)
				{
					int tiretCount = 0;
					char exLetterA = ' ';
					char exLetterB = ' ';
					for(char curLetter : name.toCharArray())
					{
						if(!((curLetter >= 'a' && curLetter <= 'z') || curLetter == '-'))
						{
							isValid = false;
							break;
						}
						if(curLetter == exLetterA && curLetter == exLetterB)
						{
							isValid = false;
							break;
						}
						if(curLetter >= 'a' && curLetter <= 'z')
						{
							exLetterA = exLetterB;
							exLetterB = curLetter;
						}
						if(curLetter == '-')
						{
							if(tiretCount >= 1)
							{
								isValid = false;
								break;
							}
							else
							{
								tiretCount++;
							}
						}
					}
				}
				//Si le nom est invalide
				if(!isValid)
				{
					SocketManager.GAME_SEND_NAME_ALREADY_EXIST(_out);
					return;
				}
				if(_compte.GET_PERSO_NUMBER() >= Ancestra.CONFIG_MAX_PERSOS)
				{
					SocketManager.GAME_SEND_CREATE_PERSO_FULL(_out);
					return;
				}
				if(_compte.createPerso(infos[0], Integer.parseInt(infos[2]), Integer.parseInt(infos[1]), Integer.parseInt(infos[3]),Integer.parseInt(infos[4]), Integer.parseInt(infos[5])))
				{
					SocketManager.GAME_SEND_CREATE_OK(_out);
					SocketManager.GAME_SEND_PERSO_LIST(_out, _compte.get_persos());
				}else
				{
					SocketManager.GAME_SEND_CREATE_FAILED(_out);
				}
				
			break;
			
			case 'B':
				int stat = -1;
				try
				{
					stat = Integer.parseInt(packet.substring(2).split("/u000A")[0]);
					_perso.boostStat(stat);
				}catch(NumberFormatException e){return;};
			break;
			case 'D':
				String[] split = packet.substring(2).split("\\|");
				int GUID = Integer.parseInt(split[0]);
				String reponse = split.length>1?split[1]:"";
				
				if(_compte.get_persos().containsKey(GUID))
				{
					if(_compte.get_persos().get(GUID).get_lvl() <20 ||(_compte.get_persos().get(GUID).get_lvl() >=20 && reponse.equals(_compte.get_reponse())))
					{
						_compte.deletePerso(GUID);
						SocketManager.GAME_SEND_PERSO_LIST(_out, _compte.get_persos());
					}
					else
						SocketManager.GAME_SEND_DELETE_PERSO_FAILED(_out);
				}else
					SocketManager.GAME_SEND_DELETE_PERSO_FAILED(_out);
			break;
			
			case 'f':
				int queueID = 1;
				int position = 1;
				SocketManager.MULTI_SEND_Af_PACKET(_out,position,1,1,""+1,queueID);
			break;
			
			case 'i':
				_compte.setClientKey(packet.substring(2));
			break;
			
			case 'L':
				SocketManager.GAME_SEND_PERSO_LIST(_out, _compte.get_persos());
				//SocketManager.GAME_SEND_HIDE_GENERATE_NAME(_out);
			break;
			
			case 'S':
				int charID = Integer.parseInt(packet.substring(2));
				if(_compte.get_persos().get(charID) != null)
				{
					_compte.setGameThread(this);
					_perso = _compte.get_persos().get(charID);
					if(_perso != null)
					{
						_perso.OnJoinGame();
						return;
					}
				}
				SocketManager.GAME_SEND_PERSO_SELECTION_FAILED(_out);
			break;
				
			case 'T':
				int guid = Integer.parseInt(packet.substring(2));
				_compte = Ancestra.gameServer.getWaitingCompte(guid);
				if(_compte != null)
				{
					String ip = _s.getInetAddress().getHostAddress();
					
					_compte.setGameThread(this);
					_compte.setCurIP(ip);
					Ancestra.gameServer.delWaitingCompte(_compte);
					SocketManager.GAME_SEND_ATTRIBUTE_SUCCESS(_out);
				}else
				{
					SocketManager.GAME_SEND_ATTRIBUTE_FAILED(_out);
				}
			break;
			
			case 'V':
				SocketManager.GAME_SEND_AV0(_out);
			break;
			
			case 'P':
				SocketManager.REALM_SEND_REQUIRED_APK(_out);
				break;
		}
	}

	public Thread getThread()
	{
		return _t;
	}

	public void removeAction(GameAction GA)
	{
		//* DEBUG
		System.out.println("Supression de la GameAction id = "+GA._id);
		//*/
		_actions.remove(GA._id);
	}
	
	public void addAction(GameAction GA)
	{
		_actions.put(GA._id, GA);
		//* DEBUG
		System.out.println("Ajout de la GameAction id = "+GA._id);
		System.out.println("Packet: "+GA._packet);
		//*/
	}

}
