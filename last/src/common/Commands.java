package common;

import game.GameThread;
import game.GameServer.SaveThread;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.Map.Entry;

import javax.swing.Timer;

import common.World.ItemSet;

import objects.Action;
import objects.Carte;
import objects.Compte;
import objects.NPC_tmpl;
import objects.Objet;
import objects.Personnage;
import objects.Carte.MountPark;
import objects.HDV.HdvEntry;
import objects.Metier.StatsMetier;
import objects.Monstre.MobGroup;
import objects.NPC_tmpl.NPC;
import objects.NPC_tmpl.NPC_question;
import objects.NPC_tmpl.NPC_reponse;
import objects.Objet.ObjTemplate;


public class Commands {
	Compte _compte;
	Personnage _perso;
	PrintWriter _out;
	//Sauvegarde
	private boolean _TimerStart = false;
	Timer _timer;
	
	private Timer createTimer(final int time)
	{
	    ActionListener action = new ActionListener ()
	      {
	    	int Time = time;
	        public void actionPerformed (ActionEvent event)
	        {
	        	Time = Time-1;
	        	if(Time == 1)
	        	{
	        		SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;"+Time+" minute");
	        	}else
	        	{
		        	SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;"+Time+" minutes");
	        	}
	        	if(Time <= 0)
	        	{
	        		for(Personnage perso : World.getOnlinePersos())
	        		{
	        			perso.get_compte().getGameThread().kick();
	        		}
	    			System.exit(0);
	        	}
	        }
	      };
	    // Génération du repeat toutes les minutes.
	    return new Timer (60000, action);//60000
	}
	
	public Commands(Personnage perso)
	{
		this._compte = perso.get_compte();
		this._perso = perso;
		this._out = _compte.getGameThread().get_out();
	}
	
	public void consoleCommand(String packet)
	{
		
		if(_compte.get_gmLvl() < 1)
		{
			_compte.getGameThread().closeSocket();
			return;
		}
		
		String msg = packet.substring(2);
		String[] infos = msg.split(" ");
		if(infos.length == 0)return;
		String command = infos[0];
		
		if(Ancestra.canLog)
		{
			Ancestra.addToMjLog(_compte.get_curIP()+": "+_compte.get_name()+" "+_perso.get_name()+"=>"+msg);
		}
		
		if(_compte.get_gmLvl() == 1)
		{
			commandGmOne(command, infos, msg);
		}else
		if(_compte.get_gmLvl() == 2)
		{
			commandGmTwo(command, infos, msg);
		}
		else
		if(_compte.get_gmLvl() == 3)
		{
			commandGmThree(command, infos, msg);
		}
		else
		if(_compte.get_gmLvl() >= 4)
		{
			commandGmFour(command, infos, msg);
		}
	}
	
	public void commandGmOne(String command, String[] infos, String msg)
	{
		if(_compte.get_gmLvl() < 1)
		{
			_compte.getGameThread().closeSocket();
			return;
		}
		if(command.equalsIgnoreCase("INFOS"))
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
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}else
		if(command.equalsIgnoreCase("REFRESHMOBS"))
		{
			_perso.get_curCarte().refreshSpawns();
			String mess = "Mob Spawn refreshed!";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}if(command.equalsIgnoreCase("MAPINFO"))
		{
			String mess = 	"==========\n"
						+	"Liste des Npcs de la carte:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			Carte map = _perso.get_curCarte();
			for(Entry<Integer,NPC> entry : map.get_npcs().entrySet())
			{
				mess = entry.getKey()+" "+entry.getValue().get_template().get_id()+" "+entry.getValue().get_cellID()+" "+entry.getValue().get_template().get_initQuestionID();
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			mess = "Liste des groupes de monstres:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			for(Entry<Integer,MobGroup> entry : map.getMobGroups().entrySet())
			{
				mess = entry.getKey()+" "+entry.getValue().getCellID()+" "+entry.getValue().getAlignement()+" "+entry.getValue().getSize();
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			mess = "==========";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}else
		if(command.equalsIgnoreCase("WHO"))
		{
			String mess = 	"==========\n"
				+			"Liste des joueurs en ligne:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			int diff = Ancestra.gameServer.getClients().size() -  30;
			for(byte b = 0; b < 30; b++)
			{
				if(b == Ancestra.gameServer.getClients().size())break;
				GameThread GT = Ancestra.gameServer.getClients().get(b);
				Personnage P = GT.getPerso();
				if(P == null)continue;
				mess = P.get_name()+"("+P.get_GUID()+") ";
				
				switch(P.get_classe())
				{
					case Constants.CLASS_FECA:
						mess += "Fec";
					break;
					case Constants.CLASS_OSAMODAS:
						mess += "Osa";
					break;
					case Constants.CLASS_ENUTROF:
						mess += "Enu";
					break;
					case Constants.CLASS_SRAM:
						mess += "Sra";
					break;
					case Constants.CLASS_XELOR:
						mess += "Xel";
					break;
					case Constants.CLASS_ECAFLIP:
						mess += "Eca";
					break;
					case Constants.CLASS_ENIRIPSA:
						mess += "Eni";
					break;
					case Constants.CLASS_IOP:
						mess += "Iop";
					break;
					case Constants.CLASS_CRA:
						mess += "Cra";
					break;
					case Constants.CLASS_SADIDA:
						mess += "Sad";
					break;
					case Constants.CLASS_SACRIEUR:
						mess += "Sac";
					break;
					case Constants.CLASS_PANDAWA:
						mess += "Pan";
					break;
					default:
						mess += "Unk";
				}
				mess += " ";
				mess += (P.get_sexe()==0?"M":"F")+" ";
				mess += P.get_lvl()+" ";
				mess += P.get_curCarte().get_id()+"("+P.get_curCarte().getX()+"/"+P.get_curCarte().getY()+") ";
				mess += P.get_fight()==null?"":"Combat ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			if(diff >0)
			{
				mess = 	"Et "+diff+" autres personnages";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			mess = 	"==========\n";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}else
		if(command.equalsIgnoreCase("SHOWFIGHTPOS"))
		{
			String mess = "Liste des StartCell [teamID][cellID]:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			String places = _perso.get_curCarte().get_placesStr();
			if(places.indexOf('|') == -1 || places.length() <2)
			{
				mess = "Les places n'ont pas ete definies";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
				return;
			}
			String team0 = "",team1 = "";
			String[] p = places.split("\\|");
			try
			{
				team0 = p[0];
			}catch(Exception e){};
			try
			{
				team1 = p[1];
			}catch(Exception e){};
			mess = "Team 0:\n";
			for(int a = 0;a <= team0.length()-2; a+=2)
			{
				String code = team0.substring(a,a+2);
				mess += CryptManager.cellCode_To_ID(code);
			}
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			mess = "Team 1:\n";
			for(int a = 0;a <= team1.length()-2; a+=2)
			{
				String code = team1.substring(a,a+2);
				mess += CryptManager.cellCode_To_ID(code)+" , ";
			}
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}else
		if(command.equalsIgnoreCase("CREATEGUILD"))
		{
			Personnage perso = _perso;
			if(infos.length >1)
			{
				perso = World.getPersoByName(infos[1]);
			}
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			if(!perso.isOnline())
			{
				String mess = "Le personnage "+perso.get_name()+" n'etait pas connecte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			if(perso.get_guild() != null || perso.getGuildMember() != null)
			{
				String mess = "Le personnage "+perso.get_name()+" a deja une guilde";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			SocketManager.GAME_SEND_gn_PACKET(perso);
			String mess = perso.get_name()+": Panneau de creation de guilde ouvert";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			return;
		}else
		if(command.equalsIgnoreCase("TOOGLEAGGRO"))
		{
			Personnage perso = _perso;
			
			String name = null;
			try
			{
				name = infos[1];
			}catch(Exception e){};
			
			perso = World.getPersoByName(name);
			
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			perso.set_canAggro(!perso.canAggro());
			String mess = perso.get_name();
			if(perso.canAggro()) mess += " peut maintenant etre aggresser";
			else mess += " ne peut plus etre agresser";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			if(!perso.isOnline())
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
		}else
		if(command.equalsIgnoreCase("ANNOUNCE"))
		{
			infos = msg.split(" ",2);
			SocketManager.GAME_SEND_MESSAGE_TO_ALL(infos[1], Ancestra.CONFIG_MOTD_COLOR);
			return;
		}else
		if(command.equalsIgnoreCase("DEMORPH"))
		{
			Personnage target = _perso;
			if(infos.length > 1)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[1]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			int morphID = target.get_classe()*10 + target.get_sexe();
			target.set_gfxID(morphID);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.get_curCarte(), target.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.get_curCarte(), target);
			String str = "Le joueur a ete transforme";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("GONAME") || command.equalsIgnoreCase("JOIN"))
		{
			Personnage P = World.getPersoByName(infos[1]);
			if(P == null)
			{
				String str = "Le personnage n'existe pas";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			short mapID = P.get_curCarte().get_id();
			int cellID = P.get_curCell().getID();
			
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
				if(target.get_fight() != null)
				{
					String str = "La cible est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.teleport(mapID, cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("NAMEGO"))
		{
			Personnage target = World.getPersoByName(infos[1]);
			if(target == null)
			{
				String str = "Le personnage n'existe pas";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(target.get_fight() != null)
			{
				String str = "La cible est en combat";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage P = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				P = World.getPersoByName(infos[2]);
				if(P == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			if(P.isOnline())
			{
				short mapID = P.get_curCarte().get_id();
				int cellID = P.get_curCell().getID();
				target.teleport(mapID, cellID);
				String str = "Le joueur a ete teleporte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			}else
			{
				String str = "Le joueur n'est pas en ligne";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			}
		}else
		if(command.equalsIgnoreCase("NAMEANNOUNCE"))
		{
			infos = msg.split(" ",2);
			String prefix = "["+_perso.get_name()+"]";
			SocketManager.GAME_SEND_MESSAGE_TO_ALL(prefix+infos[1], Ancestra.CONFIG_MOTD_COLOR);
			return;
		}else
		if(command.equalsIgnoreCase("TELEPORT"))
		{
			short mapID = -1;
			int cellID = -1;
			try
			{
				mapID = Short.parseShort(infos[1]);
				cellID = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if(mapID == -1 || cellID == -1 || World.getCarte(mapID) == null)
			{
				String str = "MapID ou cellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(World.getCarte(mapID).getCase(cellID) == null)
			{
				String str = "MapID ou cellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 3)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[3]);
				if(target == null  || target.get_fight() != null)
				{
					String str = "Le personnage n'a pas ete trouve ou est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.teleport(mapID, cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("GOMAP"))
		{
			int mapX = 0;
			int mapY = 0;
			int cellID = 0;
			int contID = 0;//Par défaut Amakna
			try
			{
				mapX = Integer.parseInt(infos[1]);
				mapY = Integer.parseInt(infos[2]);
				cellID = Integer.parseInt(infos[3]);
				contID = Integer.parseInt(infos[4]);
			}catch(Exception e){};
			Carte map = World.getCarteByPosAndCont(mapX,mapY,contID);
			if(map == null)
			{
				String str = "Position ou continent invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(map.getCase(cellID) == null)
			{
				String str = "CellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 5)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[5]);
				if(target == null || target.get_fight() != null)
				{
					String str = "Le personnage n'a pas ete trouve ou est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
				if(target.get_fight() != null)
				{
					String str = "La cible est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.teleport(map.get_id(), cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("DOACTION"))
		{
			//DOACTION NAME TYPE ARGS COND
			if(infos.length < 4)
			{
				String mess = "Nombre d'argument de la commande incorect !";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			int type = -100;
			String args = "",cond = "";
			Personnage perso = _perso;
			try
			{
				perso = World.getPersoByName(infos[1]);
				if(perso == null)perso = _perso;
				type = Integer.parseInt(infos[2]);
				args = infos[3];
				if(infos.length >4)
				cond = infos[4];
			}catch(Exception e)
			{
				String mess = "Arguments de la commande incorect !";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			(new Action(type,args,cond)).apply(perso, null, -1, -1);
			String mess = "Action effectuee !";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		{
			String mess = "Commande non reconnue";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}
	}
	
	public void commandGmTwo(String command, String[] infos, String msg)
	{
		if(_compte.get_gmLvl() < 2)
		{
			_compte.getGameThread().closeSocket();
			return;
		}
		
		if(command.equalsIgnoreCase("MUTE"))
		{
			Personnage perso = _perso;
			String name = null;
			try
			{
				name = infos[1];
			}catch(Exception e){};
			int time = 0;
			try
			{
				time = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			
			perso = World.getPersoByName(name);
			if(perso == null || time < 0)
			{
				String mess = "Le personnage n'existe pas ou la duree est invalide.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			String mess = "Vous avez mute "+perso.get_name()+" pour "+time+" secondes";
			if(perso.get_compte() == null)
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			perso.get_compte().mute(true,time);
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			if(!perso.isOnline())
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}else
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1124;"+time);
			}
			return;
		}else
		if(command.equalsIgnoreCase("UNMUTE"))
		{
			Personnage perso = _perso;
			
			String name = null;
			try
			{
				name = infos[1];
			}catch(Exception e){};
			
			perso = World.getPersoByName(name);
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			perso.get_compte().mute(false,0);
			String mess = "Vous avez unmute "+perso.get_name();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			if(!perso.isOnline())
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
		}else
		if(command.equalsIgnoreCase("KICK"))
		{
			Personnage perso = _perso;
			String name = null;
			try
			{
				name = infos[1];
			}catch(Exception e){};
			perso = World.getPersoByName(name);
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			if(perso.isOnline())
			{
				perso.get_compte().getGameThread().kick();
				String mess = "Vous avez kick "+perso.get_name();
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
			else
			{
				String mess = "Le personnage "+perso.get_name()+" n'est pas connecte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
		}else
		if(command.equalsIgnoreCase("SPELLPOINT"))
		{
			int pts = -1;
			try
			{
				pts = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(pts == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.addSpellPoint(pts);
			SocketManager.GAME_SEND_STATS_PACKET(target);
			String str = "Le nombre de point de sort a ete modifiee";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("LEARNSPELL"))
		{
			int spell = -1;
			try
			{
				spell = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(spell == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			
			target.learnSpell(spell, 1, true,true);
			
			String str = "Le sort a ete appris";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("SETALIGN"))
		{
			byte align = -1;
			try
			{
				align = Byte.parseByte(infos[1]);
			}catch(Exception e){};
			if(align < Constants.ALIGNEMENT_NEUTRE || align >Constants.ALIGNEMENT_MERCENAIRE)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			
			target.modifAlignement(align);
			
			String str = "L'alignement du joueur a ete modifie";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("SETREPONSES"))
		{
			if(infos.length <3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Il manque un/des arguments");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			String reps = infos[2];
			NPC_question Q = World.getNPCQuestion(id);
			String str = "";
			if(id == 0 || Q == null)
			{
				str = "QuestionID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Q.setReponses(reps);
			boolean a= SQLManager.UPDATE_NPCREPONSES(id,reps);
			str = "Liste des reponses pour la question "+id+": "+Q.getReponses();
			if(a)str += "(sauvegarde dans la BDD)";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			return;
		}else
		if(command.equalsIgnoreCase("SHOWREPONSES"))
		{
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			NPC_question Q = World.getNPCQuestion(id);
			String str = "";
			if(id == 0 || Q == null)
			{
				str = "QuestionID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			str = "Liste des reponses pour la question "+id+": "+Q.getReponses();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			return;
		}else
		if(command.equalsIgnoreCase("HONOR"))
		{
			int honor = 0;
			try
			{
				honor = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			String str = "Vous avez ajouter "+honor+" honneur a "+target.get_name();
			if(target.get_align() == Constants.ALIGNEMENT_NEUTRE)
			{
				str = "Le joueur est neutre ...";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			target.addHonor(honor);
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			
		}else
		if(command.equalsIgnoreCase("ADDJOBXP"))
		{
			int job = -1;
			int xp = -1;
			try
			{
				job = Integer.parseInt(infos[1]);
				xp = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if(job == -1 || xp < 0)
			{
				String str = "Valeurs invalides";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
				Personnage target = _perso;
			if(infos.length > 3)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[3]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			StatsMetier SM = target.getMetierByID(job);
			if(SM== null)
			{
				String str = "Le joueur ne connais pas le metier demande";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
				
			SM.addXp(target, xp);
			
			String str = "Le metier a ete experimenter";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("LEARNJOB"))
		{
			int job = -1;
			try
			{
				job = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(job == -1 || World.getMetier(job) == null)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			
			target.learnJob(World.getMetier(job));
			
			String str = "Le metier a ete appris";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("CAPITAL"))
		{
			int pts = -1;
			try
			{
				pts = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(pts == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.addCapital(pts);
			SocketManager.GAME_SEND_STATS_PACKET(target);
			String str = "Le capital a ete modifiee";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		if(command.equalsIgnoreCase("SIZE"))
		{
			int size = -1;
			try
			{
				size = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(size == -1)
			{
				String str = "Taille invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.set_size(size);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.get_curCarte(), target.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.get_curCarte(), target);
			String str = "La taille du joueur a ete modifiee";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("MORPH"))
		{
			int morphID = -1;
			try
			{
				morphID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(morphID == -1)
			{
				String str = "MorphID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.set_gfxID(morphID);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.get_curCarte(), target.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.get_curCarte(), target);
			String str = "Le joueur a ete transforme";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}if(command.equalsIgnoreCase("MOVENPC"))
		{
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			NPC npc = _perso.get_curCarte().getNPC(id);
			if(id == 0 || npc == null)
			{
				String str = "Npc GUID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			int exC = npc.get_cellID();
			//on l'efface de la map
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), id);
			//on change sa position/orientation
			npc.setCellID(_perso.get_curCell().getID());
			npc.setOrientation((byte)_perso.get_orientation());
			//on envoie la modif
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.get_curCarte(),npc);
			String str = "Le PNJ a ete deplace";
			if(_perso.get_orientation() == 0
			|| _perso.get_orientation() == 2
			|| _perso.get_orientation() == 4
			|| _perso.get_orientation() == 6)
				str += " mais est devenu invisible (orientation diagonale invalide).";
			if(SQLManager.DELETE_NPC_ON_MAP(_perso.get_curCarte().get_id(),exC)
			&& SQLManager.ADD_NPC_ON_MAP(_perso.get_curCarte().get_id(),npc.get_template().get_id(),_perso.get_curCell().getID(),_perso.get_orientation()))
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			else
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Erreur au moment de sauvegarder la position");
		}else	
		if(command.equalsIgnoreCase("ITEMSET"))
		{
			int tID = 0;
			try
			{
				tID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			ItemSet IS = World.getItemSet(tID);
			if(tID == 0 || IS == null)
			{
				String mess = "La panoplie "+tID+" n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			boolean useMax = false;
			if(infos.length == 3)useMax = infos[2].equals("MAX");//Si un jet est spécifié

			
			for(ObjTemplate t : IS.getItemTemplates())
			{
				Objet obj = t.createNewItem(1,useMax);
				if(_perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
					World.addObjet(obj,true);
			}
			String str = "Creation de la panoplie "+tID+" reussie";
			if(useMax) str += " avec des stats maximums";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("LEVEL"))
		{
			int count = 0;
			try
			{
				count = Integer.parseInt(infos[1]);
				if(count < 1)	count = 1;
				if(count > World.getExpLevelSize())	count = World.getExpLevelSize();
				Personnage perso = _perso;
				if(infos.length == 3)//Si le nom du perso est spécifié
				{
					String name = infos[2];
					perso = World.getPersoByName(name);
					if(perso == null)
						perso = _perso;
				}
				if(perso.get_lvl() < count)
				{
					while(perso.get_lvl() < count)
					{
						perso.levelUp(false,true);
					}
					if(perso.isOnline())
					{
						SocketManager.GAME_SEND_SPELL_LIST(perso);
						SocketManager.GAME_SEND_NEW_LVL_PACKET(perso.get_compte().getGameThread().get_out(),perso.get_lvl());
						SocketManager.GAME_SEND_STATS_PACKET(perso);
					}
				}
				String mess = "Vous avez fixer le niveau de "+perso.get_name()+" a "+count;
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			};
		}else
		if(command.equalsIgnoreCase("PDVPER"))
		{
			int count = 0;
			try
			{
				count = Integer.parseInt(infos[1]);
				if(count < 0)	count = 0;
				if(count > 100)	count = 100;
				Personnage perso = _perso;
				if(infos.length == 3)//Si le nom du perso est spécifié
				{
					String name = infos[2];
					perso = World.getPersoByName(name);
					if(perso == null)
						perso = _perso;
				}
				int newPDV = perso.get_PDVMAX() * count / 100;
				perso.set_PDV(newPDV);
				if(perso.isOnline())
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				String mess = "Vous avez fixer le pourcentage de pdv de "+perso.get_name()+" a "+count;
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			};
		}else
		if(command.equalsIgnoreCase("KAMAS"))
		{
			int count = 0;
			try
			{
				count = Integer.parseInt(infos[1]);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			};
			if(count == 0)return;
			
			Personnage perso = _perso;
			if(infos.length == 3)//Si le nom du perso est spécifié
			{
				String name = infos[2];
				perso = World.getPersoByName(name);
				if(perso == null)
					perso = _perso;
			}
			long curKamas = perso.get_kamas();
			long newKamas = curKamas + count;
			if(newKamas <0) newKamas = 0;
			if(newKamas > 1000000000) newKamas = 1000000000;
			perso.set_kamas(newKamas);
			if(perso.isOnline())
				SocketManager.GAME_SEND_STATS_PACKET(perso);
			String mess = "Vous avez ";
			mess += (count<0?"retirer":"ajouter")+" ";
			mess += Math.abs(count)+" kamas a "+perso.get_name();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("ITEM") || command.equalsIgnoreCase("!getitem"))
		{
			boolean isOffiCmd = command.equalsIgnoreCase("!getitem");
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int tID = 0;
			try
			{
				tID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(tID == 0)
			{
				String mess = "Le template "+tID+" n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			int qua = 1;
			if(infos.length == 3)//Si une quantité est spécifiée
			{
				try
				{
					qua = Integer.parseInt(infos[2]);
				}catch(Exception e){};
			}
			boolean useMax = false;
			if(infos.length == 4 && !isOffiCmd)//Si un jet est spécifié
			{
				if(infos[3].equalsIgnoreCase("MAX"))useMax = true;
			}
			ObjTemplate t = World.getObjTemplate(tID);
			if(t == null)
			{
				String mess = "Le template "+tID+" n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			if(qua <1)qua =1;
			Objet obj = t.createNewItem(qua,useMax);
			if(_perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
				World.addObjet(obj,true);
			String str = "Creation de l'item "+tID+" reussie";
			if(useMax) str += " avec des stats maximums";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
		}else 
		if (command.equalsIgnoreCase("SPAWN"))
		{			
			String Mob = null;
			try
			{
				Mob = infos[1];
			}catch(Exception e){};
            if(Mob == null) return;
			_perso.get_curCarte().spawnGroupOnCommand(_perso.get_curCell().getID(), Mob);
		}else
		if (command.equalsIgnoreCase("TITLE"))
		{
			Personnage target = null; 
			byte TitleID = 0;
			try
			{
				target = World.getPersoByName(infos[1]);
				TitleID = Byte.parseByte(infos[2]);
			}catch(Exception e){};
			
			if(target == null)
			{
				String str = "Le personnage n'a pas ete trouve";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			target.set_title(TitleID);
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Titre mis en place.");
			SQLManager.SAVE_PERSONNAGE(target, false);
			if(target.get_fight() == null) SocketManager.GAME_SEND_ALTER_GM_PACKET(target.get_curCarte(), target);
		}else
		{
			this.commandGmOne(command, infos, msg);
		}
	}
	
	public void commandGmThree(String command, String[] infos, String msg)
	{
		if(_compte.get_gmLvl() < 3)
		{
			_compte.getGameThread().closeSocket();
			return;
		}
		
		if(command.equalsIgnoreCase("EXIT"))
		{
			System.exit(0);
		}else
		if(command.equalsIgnoreCase("SAVE") && !Ancestra.isSaving)
		{
			Thread t = new Thread(new SaveThread());
			t.start();
			String mess = "Sauvegarde lancee!";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}else
		if(command.equalsIgnoreCase("DELFIGHTPOS"))
		{
			int cell = -1;
			try
			{
				cell = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if(cell < 0 || _perso.get_curCarte().getCase(cell) == null)
			{
				cell = _perso.get_curCell().getID();
			}
			String places = _perso.get_curCarte().get_placesStr();
			String[] p = places.split("\\|");
			String newPlaces = "";
			String team0 = "",team1 = "";
			try
			{
				team0 = p[0];
			}catch(Exception e){};
			try
			{
				team1 = p[1];
			}catch(Exception e){};
			
			for(int a = 0;a<=team0.length()-2;a+=2)
			{
				String c = p[0].substring(a,a+2);
				if(cell == CryptManager.cellCode_To_ID(c))continue;
				newPlaces += c;
			}
			newPlaces += "|";
			for(int a = 0;a<=team1.length()-2;a+=2)
			{
				String c = p[1].substring(a,a+2);
				if(cell == CryptManager.cellCode_To_ID(c))continue;
				newPlaces += c;
			}
			_perso.get_curCarte().setPlaces(newPlaces);
			if(!SQLManager.SAVE_MAP_DATA(_perso.get_curCarte()))return;
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Les places ont ete modifiees ("+newPlaces+")");
			return;
		}else
		if(command.equalsIgnoreCase("BAN"))
		{
			Personnage P = World.getPersoByName(infos[1]);
			if(P == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Personnage non trouve");
				return;
			}
			if(P.get_compte() == null)SQLManager.LOAD_ACCOUNT_BY_GUID(P.getAccID());
			if(P.get_compte() == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur");
				return;
			}
			P.get_compte().setBanned(true);
			SQLManager.UPDATE_ACCOUNT_DATA(P.get_compte());
			if(P.get_compte().getGameThread() != null)P.get_compte().getGameThread().kick();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez banni "+P.get_name());
			return;
		}else
		if(command.equalsIgnoreCase("UNBAN"))
		{
			Personnage P = World.getPersoByName(infos[1]);
			if(P == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Personnage non trouve");
				return;
			}
			if(P.get_compte() == null)SQLManager.LOAD_ACCOUNT_BY_GUID(P.getAccID());
			if(P.get_compte() == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur");
				return;
			}
			P.get_compte().setBanned(false);
			SQLManager.UPDATE_ACCOUNT_DATA(P.get_compte());
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez debanni "+P.get_name());
			return;
		}else
		if(command.equalsIgnoreCase("ADDFIGHTPOS"))
		{
			int team = -1;
			int cell = -1;
			try
			{
				team = Integer.parseInt(infos[1]);
				cell = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if( team < 0 || team>1)
			{
				String str = "Team ou cellID incorects";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(cell <0 || _perso.get_curCarte().getCase(cell) == null || !_perso.get_curCarte().getCase(cell).isWalkable(true))
			{
				cell = _perso.get_curCell().getID();
			}
			String places = _perso.get_curCarte().get_placesStr();
			String[] p = places.split("\\|");
			boolean already = false;
			String team0 = "",team1 = "";
			try
			{
				team0 = p[0];
			}catch(Exception e){};
			try
			{
				team1 = p[1];
			}catch(Exception e){};
			
			//Si case déjà utilisée
			System.out.println("0 => "+team0+"\n1 =>"+team1+"\nCell: "+CryptManager.cellID_To_Code(cell));
			for(int a = 0; a <= team0.length()-2;a+=2)if(cell == CryptManager.cellCode_To_ID(team0.substring(a,a+2)))already = true;
			for(int a = 0; a <= team1.length()-2;a+=2)if(cell == CryptManager.cellCode_To_ID(team1.substring(a,a+2)))already = true;
			if(already)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"La case est deja dans la liste");
				return;
			}
			if(team == 0)team0 += CryptManager.cellID_To_Code(cell);
			else if(team == 1)team1 += CryptManager.cellID_To_Code(cell);
			
			String newPlaces = team0+"|"+team1;
			
			_perso.get_curCarte().setPlaces(newPlaces);
			if(!SQLManager.SAVE_MAP_DATA(_perso.get_curCarte()))return;
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Les places ont ete modifiees ("+newPlaces+")");
			return;
		}else
		if(command.equalsIgnoreCase("SETMAXGROUP"))
		{
			infos = msg.split(" ",4);
			byte id = -1;
			try
			{
				id = Byte.parseByte(infos[1]);
			}catch(Exception e){};
			if(id == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "Le nombre de groupe a ete fixe";
			_perso.get_curCarte().setMaxGroup(id);
			boolean ok = SQLManager.SAVE_MAP_DATA(_perso.get_curCarte());
			if(ok)mess += " et a ete sauvegarder a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("ADDREPONSEACTION"))
		{
			infos = msg.split(" ",4);
			int id = -30;
			int repID = 0;
			String args = infos[3];
			try
			{
				repID = Integer.parseInt(infos[1]);
				id = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			NPC_reponse rep = World.getNPCreponse(repID);
			if(id == -30 || rep == null)
			{
				String str = "Au moins une des valeur est invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "L'action a ete ajoute";
			
			rep.addAction(new Action(id,args,""));
			boolean ok = SQLManager.ADD_REPONSEACTION(repID,id,args);
			if(ok)mess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("SETINITQUESTION"))
		{
			infos = msg.split(" ",4);
			int id = -30;
			int q = 0;
			try
			{
				q = Integer.parseInt(infos[2]);
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(id == -30)
			{
				String str = "NpcID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "L'action a ete ajoute";
			NPC_tmpl npc = World.getNPCTemplate(id);
			
			npc.setInitQuestion(q);
			boolean ok = SQLManager.UPDATE_INITQUESTION(id,q);
			if(ok)mess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("ADDENDFIGHTACTION"))
		{
			infos = msg.split(" ",4);
			int id = -30;
			int type = 0;
			String args = infos[3];
			String cond = infos[4];
			try
			{
				type = Integer.parseInt(infos[1]);
				id = Integer.parseInt(infos[2]);
				
			}catch(Exception e){};
			if(id == -30)
			{
				String str = "Au moins une des valeur est invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "L'action a ete ajoute";
			_perso.get_curCarte().addEndFightAction(type, new Action(id,args,cond));
			boolean ok = SQLManager.ADD_ENDFIGHTACTION(_perso.get_curCarte().get_id(),type,id,args,cond);
			if(ok)mess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			return;
		}else
		if(command.equalsIgnoreCase("SPAWNFIX"))
		{
			String groupData = infos[1];

			_perso.get_curCarte().addStaticGroup(_perso.get_curCell().getID(), groupData);
			String str = "Le grouppe a ete fixe";
			//Sauvegarde DB de la modif
			if(SQLManager.SAVE_NEW_FIXGROUP(_perso.get_curCarte().get_id(),_perso.get_curCell().getID(), groupData))
				str += " et a ete sauvegarde dans la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			return;
		}else
		if(command.equalsIgnoreCase("ADDNPC"))
		{
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(id == 0 || World.getNPCTemplate(id) == null)
			{
				String str = "NpcID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			NPC npc = _perso.get_curCarte().addNpc(id, _perso.get_curCell().getID(), _perso.get_orientation());
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.get_curCarte(), npc);
			String str = "Le PNJ a ete ajoute";
			if(_perso.get_orientation() == 0
					|| _perso.get_orientation() == 2
					|| _perso.get_orientation() == 4
					|| _perso.get_orientation() == 6)
						str += " mais est invisible (orientation diagonale invalide).";
			
			if(SQLManager.ADD_NPC_ON_MAP(_perso.get_curCarte().get_id(), id, _perso.get_curCell().getID(), _perso.get_orientation()))
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			else
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Erreur au moment de sauvegarder la position");
		}else
		if(command.equalsIgnoreCase("DELNPC"))
		{
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			NPC npc = _perso.get_curCarte().getNPC(id);
			if(id == 0 || npc == null)
			{
				String str = "Npc GUID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			int exC = npc.get_cellID();
			//on l'efface de la map
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), id);
			_perso.get_curCarte().removeNpcOrMobGroup(id);
			
			String str = "Le PNJ a ete supprime";
			if(SQLManager.DELETE_NPC_ON_MAP(_perso.get_curCarte().get_id(),exC))
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			else
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Erreur au moment de sauvegarder la position");
		}else
		if(command.equalsIgnoreCase("DELTRIGGER"))
		{
			int cellID = -1;
			try
			{
				cellID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(cellID == -1 || _perso.get_curCarte().getCase(cellID) == null)
			{
				String str = "CellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			_perso.get_curCarte().getCase(cellID).clearOnCellAction();
			boolean success = SQLManager.REMOVE_TRIGGER(_perso.get_curCarte().get_id(),cellID);
			String str = "";
			if(success)	str = "Le trigger a ete retire";
			else 		str = "Le trigger n'a pas ete retire";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("ADDTRIGGER"))
		{
			int actionID = -1;
			String args = "",cond = "";
			try
			{
				actionID = Integer.parseInt(infos[1]);
				args = infos[2];
				cond = infos[3];
			}catch(Exception e){};
			if(args.equals("") || actionID <= -3)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			_perso.get_curCell().addOnCellStopAction(actionID,args, cond);
			boolean success = SQLManager.SAVE_TRIGGER(_perso.get_curCarte().get_id(),_perso.get_curCell().getID(),actionID,1,args,cond);
			String str = "";
			if(success)	str = "Le trigger a ete ajoute";
			else 		str = "Le trigger n'a pas ete ajoute";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("DELNPCITEM"))
		{
			if(_compte.get_gmLvl() <3)return;
			int npcGUID = 0;
			int itmID = -1;
			try
			{
				npcGUID = Integer.parseInt(infos[1]);
				itmID = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			NPC_tmpl npc =  _perso.get_curCarte().getNPC(npcGUID).get_template();
			if(npcGUID == 0 || itmID == -1 || npc == null)
			{
				String str = "NpcGUID ou itmID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			
			String str = "";
			if(npc.delItemVendor(itmID))str = "L'objet a ete retire";
			else str = "L'objet n'a pas ete retire";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("ADDNPCITEM"))
		{
			if(_compte.get_gmLvl() <3)return;
			int npcGUID = 0;
			int itmID = -1;
			try
			{
				npcGUID = Integer.parseInt(infos[1]);
				itmID = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			NPC_tmpl npc =  _perso.get_curCarte().getNPC(npcGUID).get_template();
			ObjTemplate item =  World.getObjTemplate(itmID);
			if(npcGUID == 0 || itmID == -1 || npc == null || item == null)
			{
				String str = "NpcGUID ou itmID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			
			String str = "";
			if(npc.addItemVendor(item))str = "L'objet a ete rajoute";
			else str = "L'objet n'a pas ete rajoute";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("ADDMOUNTPARK"))
		{
			int size = -1;
			int owner = -2;
			int price = -1;
			try
			{
				size = Integer.parseInt(infos[1]);
				owner = Integer.parseInt(infos[2]);
				price = Integer.parseInt(infos[3]);
				if(price > 20000000)price = 20000000;
				if(price <0)price = 0;
			}catch(Exception e){};
			if(size == -1 || owner == -2 || price == -1 || _perso.get_curCarte().getMountPark() != null)
			{
				String str = "Infos invalides ou map deja config.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			MountPark MP = new MountPark(owner, _perso.get_curCarte(), _perso.get_curCell().getID(), size, "", -1, price);
			_perso.get_curCarte().setMountPark(MP);
			SQLManager.SAVE_MOUNTPARK(MP);
			String str = "L'enclos a ete config. avec succes";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else 
		if (command.equalsIgnoreCase("SHUTDOWN"))
		{
			int time = 30, OffOn = 0;
			try
			{
				OffOn = Integer.parseInt(infos[1]);
				time = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			
			if(OffOn == 1 && _TimerStart)// demande de démarer le reboot
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Un shutdown est deja programmer.");
			}else if(OffOn == 1 && !_TimerStart)
			{
				_timer = createTimer(time);
				_timer.start();
				_TimerStart = true;
				String timeMSG = "minutes";
				if(time <= 1)
				{
					timeMSG = "minute";
				}
				SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;"+time+" "+timeMSG);
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Shutdown lance.");
			}else if(OffOn == 0 && _TimerStart)
			{
				_timer.stop();
				_TimerStart = false;
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Shutdown arrete.");
			}else if(OffOn == 0 && !_TimerStart)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Aucun shutdown n'est lance.");
			}
		}else
		{
			this.commandGmTwo(command, infos, msg);
		}
	}
	
	public void commandGmFour(String command, String[] infos, String msg)
	{
		if(_compte.get_gmLvl() < 4)
		{
			_compte.getGameThread().closeSocket();
			return;
		}
		
		if(command.equalsIgnoreCase("SETADMIN"))
		{
			int gmLvl = -100;
			try
			{
				gmLvl = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(gmLvl == -100)
			{
				String str = "Valeur incorrecte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.get_compte().setGmLvl(gmLvl);
			SQLManager.UPDATE_ACCOUNT_DATA(target.get_compte());
			String str = "Le niveau GM du joueur a ete modifie";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("LOCK"))
		{
			byte LockValue = 1;//Accessible
			try
			{
				LockValue = Byte.parseByte(infos[1]);
			}catch(Exception e){};
			
			if(LockValue > 2) LockValue = 2;
			if(LockValue < 0) LockValue = 0;
			World.set_state((short)LockValue);
			if(LockValue == 1)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Serveur accessible.");
			}else if(LockValue == 0)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Serveur inaccessible.");
			}else if(LockValue == 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Serveur en sauvegarde.");
			}
		}else
		if(command.equalsIgnoreCase("BLOCK"))
		{
			byte GmAccess = 0;
			byte KickPlayer = 0;
			try
			{
				GmAccess = Byte.parseByte(infos[1]);
				KickPlayer = Byte.parseByte(infos[2]);
			}catch(Exception e){};
			
			World.setGmAccess(GmAccess);
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Serveur bloque au GmLevel : "+GmAccess);
			if(KickPlayer > 0)
			{
				for(Personnage z : World.getOnlinePersos()) 
				{
					if(z.get_compte().get_gmLvl() < GmAccess)
						z.get_compte().getGameThread().closeSocket();
				}
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Les joueurs de GmLevel inferieur a "+GmAccess+" ont ete kicks.");
			}
		}else
		if(command.equalsIgnoreCase("BANIP"))
		{
			Personnage P = null;
			try
			{
				P = World.getPersoByName(infos[1]);
			}catch(Exception e){};
			if(P == null || !P.isOnline())
			{
				String str = "Le personnage n'a pas ete trouve.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			if(!Constants.IPcompareToBanIP(P.get_compte().get_curIP()))
			{
				Constants.BAN_IP += ","+P.get_compte().get_curIP();
				if(SQLManager.ADD_BANIP(P.get_compte().get_curIP()))
				{
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "L'IP a ete banni.");
				}
				if(P.isOnline()){
					P.get_compte().getGameThread().kick();
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le joueur a ete kick.");
				}
			}else
			{
				String str = "L'IP existe deja.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
		}else
		if(command.equalsIgnoreCase("FULLHDV"))
		{
			int numb = 1;
			try
			{
				numb = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			fullHdv(numb);
		}else
		{
			this.commandGmThree(command, infos, msg);
		}
	}
	
	private void fullHdv(int ofEachTemplate)
	{
		SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Démarrage du remplissage!");
		
		Objet objet = null;
		HdvEntry entry = null;
		byte amount = 0;
		int hdv = 0;
		
		int lastSend = 0;
		long time1 = System.currentTimeMillis();//TIME
		for (ObjTemplate curTemp : World.getObjTemplates())//Boucler dans les template
		{
			try
			{
				if(Ancestra.NOTINHDV.contains(curTemp.getID())) continue;
				for (int j = 0; j < ofEachTemplate; j++)//Ajouter plusieur fois le template
				{
					if(curTemp.getType() == 85) break;
					
					objet = curTemp.createNewItem(1, false);
					hdv = getHdv(objet.getTemplate().getType());
					
					if(hdv < 0) break;
						
					amount = (byte) Formulas.getRandomValue(1, 3);
					
					
					entry = new HdvEntry(calculPrice(objet,amount), amount, -1, objet);
					objet.setQuantity(entry.getAmount(true));
					
					
					World.getHdv(hdv).addEntry(entry);
					World.addObjet(objet, false);
				}
			}catch (Exception e)
			{
				continue;
			}
			
			if((System.currentTimeMillis() - time1)/1000 != lastSend
				&& (System.currentTimeMillis() - time1)/1000 % 3 == 0)
			{
				lastSend = (int) ((System.currentTimeMillis() - time1)/1000);
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,(System.currentTimeMillis() - time1)/1000 + "sec Template: "+curTemp.getID());
			}
		}
		SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Remplissage fini en "+(System.currentTimeMillis() - time1) + "ms");
		World.saveAll(null);
		SocketManager.GAME_SEND_MESSAGE_TO_ALL("HDV remplis!",Ancestra.CONFIG_MOTD_COLOR);
	}
	private int getHdv(int type)
	{
		int rand = Formulas.getRandomValue(1, 4);
		int map = -1;
		
		switch(type)
		{
			case 12:
			case 14: 
			case 26: 
			case 43: 
			case 44: 
			case 45: 
			case 66: 
			case 70: 
			case 71: 
			case 86:
				if(rand == 1)
				{
					map = 4271;
				}else
				if(rand == 2)
				{
					map = 4607;
				}else
				{
					map = 7516;
				}
				return map;
			case 1:
			case 9:
				if(rand == 1)
				{
					map = 4216;
				}else
				if(rand == 2)
				{
					map = 4622;
				}else
				{
					map = 7514;
				}
				return map;
			case 18: 
			case 72: 
			case 77: 
			case 90: 
			case 97: 
			case 113: 
			case 116:
				if(rand == 1)
				{
					map = 8759;
				}else
				{
					map = 8753;
				}
				return map;
			case 63:
			case 64:
			case 69:
				if(rand == 1)
				{
					map = 4287;
				}else
				if(rand == 2)
				{
					map = 4595;
				}else
				if(rand == 3)
				{
					map = 7515;
				}else
				{
					map = 7350;
				}
				return map;
			case 33:
			case 42:
				if(rand == 1)
				{
					map = 2221;
				}else
				if(rand == 2)
				{
					map = 4630;
				}else
				{
					map = 7510;
				}
				return map;
			case 84: 
			case 93: 
			case 112: 
			case 114:
				if(rand == 1)
				{
					map = 4232;
				}else
				if(rand == 2)
				{
					map = 4627;
				}else
				{
					map = 12262;
				}
				return map;
			case 38: 
			case 95: 
			case 96: 
			case 98: 
			case 108:
				if(rand == 1)
				{
					map = 4178;
				}else
				if(rand == 2)
				{
					map = 5112;
				}else
				{
					map = 7289;
				}
				return map;
			case 10:
			case 11:
				if(rand == 1)
				{
					map = 4183;
				}else
				if(rand == 2)
				{
					map = 4562;
				}else
				{
					map = 7602;
				}
				return map;
			case 13: 
			case 25: 
			case 73: 
			case 75: 
			case 76:
				if(rand == 1)
				{
					map = 8760;
				}else
				{
					map = 8754;
				}
				return map;
			case 5: 
			case 6: 
			case 7: 
			case 8: 
			case 19: 
			case 20: 
			case 21: 
			case 22:
				if(rand == 1)
				{
					map = 4098;
				}else
				if(rand == 2)
				{
					map = 5317;
				}else
				{
					map = 7511;
				}
				return map;
			case 39: 
			case 40: 
			case 50: 
			case 51: 
			case 88:
				if(rand == 1)
				{
					map = 4179;
				}else
				if(rand == 2)
				{
					map = 5311;
				}else
				{
					map = 7443;
				}
				return map;
			case 87:
				if(rand == 1)
				{
					map = 6159;
				}else
				{
					map = 6167;
				}
				return map;
			case 34:
			case 52:
			case 60:
				if(rand == 1)
				{
					map = 4299;
				}else
				if(rand == 2)
				{
					map = 4629;
				}else
				{
					map = 7397;
				}
				return map;
			case 41:
			case 49:
			case 62:
				if(rand == 1)
				{
					map = 4247;
				}else
				if(rand == 2)
				{
					map = 4615;
				}else
				if(rand == 3)
				{
					map = 7501;
				}else
				{
					map = 7348;
				}
				return map;
			case 15: 
			case 35: 
			case 36: 
			case 46: 
			case 47: 
			case 48: 
			case 53: 
			case 54: 
			case 55: 
			case 56: 
			case 57: 
			case 58: 
			case 59: 
			case 65: 
			case 68: 
			case 103: 
			case 104: 
			case 105: 
			case 106: 
			case 107: 
			case 109: 
			case 110: 
			case 111:
				if(rand == 1)
				{
					map = 4262;
				}else
				if(rand == 2)
				{
					map = 4646;
				}else
				{
					map = 7413;
				}
				return map;
			case 78:
				if(rand == 1)
				{
					map = 8757;
				}else
				{
					map = 8756;
				}
				return map;
			case 2:
			case 3:
			case 4:
				if(rand == 1)
				{
					map = 4174;
				}else
				if(rand == 2)
				{
					map = 4618;
				}else
				{
					map = 7512;
				}
				return map;
			case 16:
			case 17:
			case 81:
				if(rand == 1)
				{
					map = 4172;
				}else
				if(rand == 2)
				{
					map = 4588;
				}else
				{
					map = 7513;
				}
				return map;
			case 83:
				if(rand == 1)
				{
					map = 10129;
				}else
				{
					map = 8482;
				}
				return map;
			case 82:
				return 8039;
			default:
				return -1;
		}
	}
	private int calculPrice(Objet obj, int logAmount)
	{
		int amount = (byte)(Math.pow(10,(double)logAmount) / 10);
		int stats = 0;
		
		for(int curStat : obj.getStats().getMap().values())
		{
			stats += curStat;
		}
		if(stats > 0)
			return (int) (((Math.cbrt(stats) * Math.pow(obj.getTemplate().getLevel(), 2)) * 10 + Formulas.getRandomValue(1, obj.getTemplate().getLevel()*100)) * amount);
		else
			return (int) ((Math.pow(obj.getTemplate().getLevel(),2) * 10 + Formulas.getRandomValue(1, obj.getTemplate().getLevel()*100))*amount);
	}
}