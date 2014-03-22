package objects;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import objects.Fight.Fighter;

import common.SQLManager;
import common.SocketManager;
import common.World;

public class Percepteur
{
	private int _guid;
	private short _MapID;
	private int _cellID;
	private byte _orientation;
	private int _GuildID = 0;
	private short _N1 = 0;
	private short _N2 = 0;
	private byte _inFight = 0;
	private int _inFightID = -1;
	private Map<Integer,Objet> _objets = new TreeMap<Integer,Objet>();
	private long _kamas = 0;
	private long _xp = 0;
	private boolean _inExchange = false;
	//Timer
	private int _timeTurn = 45000;
	//Les logs
	private Map<Integer,Objet> _LogObjets = new TreeMap<Integer,Objet>();
	private long _LogXP = 0;
	
	public Percepteur(int guid, short map, int cellID, byte orientation, int GuildID, 
			short N1, short N2, String items, long kamas, long xp)
	{
		_guid = guid;
		_MapID = map;
		_cellID = cellID;
		_orientation = orientation;
		_GuildID = GuildID;
		_N1 = N1;
		_N2 = N2;
		//Mise en place de son inventaire
		for(String item : items.split("\\|"))
		{
			if(item.equals(""))continue;
			String[] infos = item.split(":");
			int id = Integer.parseInt(infos[0]);
			Objet obj = World.getObjet(id);
			if(obj == null)continue;
			_objets.put(obj.getGuid(), obj);
		}
		_xp = xp;
		_kamas = kamas;
	}
	
	public long getKamas() 
	{
		return _kamas;
	}
	
	public void setKamas(long kamas) 
	{
		this._kamas = kamas;
	}
	
	public long getXp() 
	{
		return _xp;
	}
	
	public void setXp(long xp) 
	{
		this._xp = xp;
	}
	
	public Map<Integer, Objet> getObjets() 
	{
		return _objets;
	}
	
	public void removeObjet(int guid)
	{
		_objets.remove(guid);
	}
	
	public boolean HaveObjet(int guid)
	{
		if(_objets.get(guid) != null)
		{
			return true;
		}else
		{
			return false;
		}
	}
	
	public void remove_timeTurn(int time)
	{
		_timeTurn -= time;
	}
	
	public void set_timeTurn(int time)
	{
		_timeTurn = time;
	}
	
	public int get_turnTimer()
	{
		return _timeTurn;
	}
	
	public static String parseGM(Carte map)
	{
		StringBuilder sock = new StringBuilder();
		sock.append("GM|");
		boolean isFirst = true;
		for(Entry<Integer, Percepteur> perco :  World.getPercos().entrySet())
		{
			if(perco.getValue()._inFight > 0) continue;//On affiche pas le perco si il est en combat
			if(perco.getValue()._MapID == map.get_id())
			{
				if(!isFirst) sock.append("|");
				sock.append("+");
				sock.append(perco.getValue()._cellID).append(";");
				sock.append(perco.getValue()._orientation).append(";");
				sock.append("0").append(";");
				sock.append(perco.getValue()._guid).append(";");
				sock.append(perco.getValue()._N1).append(",").append(perco.getValue()._N2).append(";");
				sock.append("-6").append(";");
				sock.append("6000^100;");
				Guild G = World.getGuild(perco.getValue()._GuildID);
				sock.append(G.get_lvl()).append(";");
				sock.append(G.get_name()).append(";"+G.get_emblem());
				isFirst = false;
			}else
			{
				continue;
			}
		}
		return sock.toString();
	}
	
	public int get_guildID() {
		return _GuildID;
	}
	
	public void DelPerco(int percoGuid)
	{
		for(Objet obj : _objets.values())
		{
			//On supprime les objets non ramasser/drop
			World.removeItem(obj.guid);
		}
		World.getPercos().remove(percoGuid);
	}
	
	public int get_inFight()
	{
		return _inFight;
	}
	
	public void set_inFight(byte fight)
	{
		_inFight = fight;
	}
	
	public int getGuid()
	{
		return _guid;
	}
	
	public int get_cellID()
	{
		return _cellID;
	}
	
	public void set_inFightID(int ID)
	{
		_inFightID = ID;
	}
	
	public int get_inFightID()
	{
		return _inFightID;
	}
	
	public short get_mapID()
	{
		return _MapID;
	}
	
	public int get_N1()
	{
		return _N1;
	}
	
	public int get_N2()
	{
		return _N2;
	}
	
	public static String parsetoGuild(int GuildID)
	{
		StringBuilder packet = new StringBuilder();
		boolean isFirst = true;
		for(Entry<Integer, Percepteur> perco : World.getPercos().entrySet())
		{
			 if(perco.getValue().get_guildID() == GuildID)
    		 {
				 	Carte map = World.getCarte((short)perco.getValue().get_mapID());
				 	if(isFirst) packet.append("+");
	    			if(!isFirst) packet.append("|");
	    			packet.append(perco.getValue().getGuid()).append(";").append(perco.getValue().get_N1()).append(",").append(perco.getValue().get_N2()).append(";");
	    			
	    			packet.append(Integer.toString(map.get_id(), 36)).append(",").append(map.getX()).append(",").append(map.getY()).append(";");
	    			packet.append(perco.getValue().get_inFight()).append(";");
	    			if(perco.getValue().get_inFight() == 1)
	    			{
	    				if(map.getFight(perco.getValue().get_inFightID()) == null)
	    				{
	    					packet.append("45000;");//TimerActuel
	    				}else
	    				{
	    					packet.append(perco.getValue().get_turnTimer()).append(";");//TimerActuel
	    				}
	    				packet.append("45000;");//TimerInit
	    				packet.append("7;");//Nombre de place maximum FIXME : En fonction de la map
	    				packet.append("?,?,");//?
	    			}else
	    			{
	    				packet.append("0;");
	    				packet.append("45000;");
	    				packet.append("7;");
	    				packet.append("?,?,");
	    			}
	    			packet.append("1,2,3,4,5");
	    			
	    			//	?,?,callername,startdate(Base 10),lastHarvesterName,lastHarvestDate(Base 10),nextHarvestDate(Base 10)
	    			isFirst = false;
    		 }else
    		 {
    			 continue;
    		 }
   	 	}
		if(packet.length() == 0) packet = new StringBuilder("null");
		return packet.toString();
	}
	
	public static int GetPercoGuildID(int _id) {
		
		for(Entry<Integer, Percepteur> perco :  World.getPercos().entrySet())
		{
			if(perco.getValue().get_mapID() == _id)
			{
				return perco.getValue().get_guildID();
			}
		}
		return 0;
	}
	
	public int GetPercoGuildID() {
		
		return get_guildID();
	}
	
	public static Percepteur GetPercoByMapID(short _id) {
		
		for(Entry<Integer, Percepteur> perco :  World.getPercos().entrySet())
		{
			if(perco.getValue().get_mapID() == _id)
			{
				return  World.getPercos().get(perco.getValue().getGuid());
			}
		}
		return null;
	}
	
	public static int CountPercoGuild(int GuildID) {
		int i = 0;
		for(Entry<Integer, Percepteur> perco :  World.getPercos().entrySet())
		{
			if(perco.getValue().get_guildID() == GuildID)
			{
				i++;
			}
		}
		return i;
	}
	
	public static void parseAttaque(Personnage perso, int guildID)
	{
		for(Entry<Integer, Percepteur> perco :  World.getPercos().entrySet()) 
		{
			if(perco.getValue().get_inFight() > 0 && perco.getValue().get_guildID() == guildID)
			{
				SocketManager.GAME_SEND_gITp_PACKET(perso, parseAttaqueToGuild(perco.getValue().getGuid(), perco.getValue().get_mapID(), perco.getValue().get_inFightID()));
			}
		}
	}
	
	public static void parseDefense(Personnage perso, int guildID)
	{
		for(Entry<Integer, Percepteur> perco :  World.getPercos().entrySet()) 
		{
			if(perco.getValue().get_inFight() > 0 && perco.getValue().get_guildID() == guildID)
			{
				SocketManager.GAME_SEND_gITP_PACKET(perso, parseDefenseToGuild(perco.getValue().getGuid(), perco.getValue().get_mapID(), perco.getValue().get_inFightID()));
			}
		}
	}
	
	public static String parseAttaqueToGuild(int guid, short mapid, int fightid)
	{
		StringBuilder str = new StringBuilder();
		str.append("+").append(guid);
			
		for(Entry<Integer, Fight> F : World.getCarte(mapid).get_fights().entrySet())
		{
			//Je boucle les combats de la map bien qu'inutile :/
			//Mais cela éviter le bug F.getValue().getFighters(1) == null
				if(F.getValue().get_id() == fightid)
				{
					for(Fighter f : F.getValue().getFighters(1))//Attaque
					{
						str.append("|");
						str.append(Integer.toString(f.getPersonnage().get_GUID(), 36)).append(";");
						str.append(f.getPersonnage().get_name()).append(";");
						str.append(f.getPersonnage().get_lvl()).append(";");
						str.append("0;");
					}
				}
		}
		return str.toString();
	}
	
	public static String parseDefenseToGuild(int guid, short mapid, int fightid)
	{
		StringBuilder str = new StringBuilder();
		str.append("+").append(guid);
			
		for(Entry<Integer, Fight> F : World.getCarte(mapid).get_fights().entrySet())
		{
			//Je boucle les combats de la map bien qu'inutile :/
			//Mais cela éviter le bug F.getValue().getFighters(2) == null
				if(F.getValue().get_id() == fightid)
				{
					for(Fighter f : F.getValue().getFighters(2))//Defense
					{
						if(f.getPersonnage() == null) continue;//On sort le percepteur
						str.append("|");
						str.append(Integer.toString(f.getPersonnage().get_GUID(), 36)).append(";");
						str.append(f.getPersonnage().get_name()).append(";");
						str.append(f.getPersonnage().get_gfxID()).append(";");
						str.append(f.getPersonnage().get_lvl()).append(";");
						str.append(Integer.toString(f.getPersonnage().get_color1(), 36)).append(";");
						str.append(Integer.toString(f.getPersonnage().get_color2(), 36)).append(";");
						str.append(Integer.toString(f.getPersonnage().get_color3(), 36)).append(";");
						str.append("0;");
					}
				}
		}
		return str.toString();
	}
	
	public String getItemPercepteurList()
	{
		StringBuilder items = new StringBuilder();
		if(!_objets.isEmpty())
		{
			for(Objet obj : _objets.values())
			{
				items.append("O").append(obj.parseItem()).append(";");
			}
		}
		if(_kamas != 0) items.append("G").append(_kamas);
		return items.toString();
	}
	
	public String parseItemPercepteur()
	{
		String items = "";
		for(Objet obj : _objets.values())
		{
			items+= obj.guid+"|";
		}
		return items;
	}
	
	
	public void removeFromPercepteur(Personnage P, int guid, int qua)
	{
		Objet PercoObj = World.getObjet(guid);
		Objet PersoObj = P.getSimilarItem(PercoObj);
		
		int newQua = PercoObj.getQuantity() - qua;
		
		if(PersoObj == null)//Si le joueur n'avait aucun item similaire
		{
			//S'il ne reste rien
			if(newQua <= 0)
			{
				//On retire l'item
				removeObjet(guid);
				//On l'ajoute au joueur
				P.addObjet(PercoObj);
				
				//On envoie les packets
				SocketManager.GAME_SEND_OAKO_PACKET(P,PercoObj);
				String str = "O-"+guid;
				SocketManager.GAME_SEND_EsK_PACKET(P, str);
				
			}else //S'il reste des objets
			{
				//On crée une copy de l'item
				PersoObj = Objet.getCloneObjet(PercoObj, qua);
				//On l'ajoute au monde
				World.addObjet(PersoObj, true);
				//On retire X objet
				PercoObj.setQuantity(newQua);
				//On l'ajoute au joueur
				P.addObjet(PersoObj);
				
				//On envoie les packets
				SocketManager.GAME_SEND_OAKO_PACKET(P,PersoObj);
				String str = "O+"+PercoObj.getGuid()+"|"+PercoObj.getQuantity()+"|"+PercoObj.getTemplate().getID()+"|"+PercoObj.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(P, str);
				
			}
		}
		else
		{
			//S'il ne reste rien
			if(newQua <= 0)
			{
				//On retire l'item
				this.removeObjet(guid);
				World.removeItem(PercoObj.getGuid());
				//On Modifie la quantité de l'item du sac du joueur
				PersoObj.setQuantity(PersoObj.getQuantity() + PercoObj.getQuantity());
				
				//On envoie les packets
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, PersoObj);
				String str = "O-"+guid;
				SocketManager.GAME_SEND_EsK_PACKET(P, str);
				
			}
			else//S'il reste des objets
			{
				//On retire X objet
				PercoObj.setQuantity(newQua);
				//On ajoute X objets
				PersoObj.setQuantity(PersoObj.getQuantity() + qua);
				
				//On envoie les packets
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P,PersoObj);
				String str = "O+"+PercoObj.getGuid()+"|"+PercoObj.getQuantity()+"|"+PercoObj.getTemplate().getID()+"|"+PercoObj.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(P, str);
				
			}
		}
		SocketManager.GAME_SEND_Ow_PACKET(P);
		SQLManager.SAVE_PERSONNAGE(P, true);
	}
	
	public void LogXpDrop(long Xp)
	{
		_LogXP += Xp;
	}
	
	public void LogObjetDrop(int guid, Objet obj)
	{
		_LogObjets.put(guid, obj);
	}
	
	public long get_LogXp()
	{
		return _LogXP;
	}
	
	public String get_LogItems()
	{
		StringBuilder str = new StringBuilder();
		boolean isFirst = true;
		if(_LogObjets.isEmpty()) return "";
		for(Objet obj : _LogObjets.values())
		{
			if(!isFirst) str.append(";");
			 str.append(obj.getTemplate().getID()).append(",").append(obj.getQuantity());
			isFirst = false;
		}
		return str.toString();
	}
	
	public void addObjet(Objet newObj)
	{
		_objets.put(newObj.getGuid(), newObj);
	}
	
	public void set_Exchange(boolean Exchange)
	{
		_inExchange = Exchange;
	}
	
	public boolean get_Exchange()
	{
		return _inExchange;
	}
	
	public static void removePercepteur(int GuildID)
	{
		for(Entry<Integer, Percepteur> perco : World.getPercos().entrySet())
		{
			if(perco.getValue().get_guildID() == GuildID)
			{
				World.getPercos().remove(perco.getKey());
				for(Personnage p : World.getCarte((short) perco.getValue().get_mapID()).getPersos())
				{
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(p.get_curCarte(), perco.getValue().getGuid());//Suppression visuelle
				}
				SQLManager.DELETE_PERCO(perco.getKey());//Supprime les percepteurs
			}else
			{
				continue;
			}
		}
	}
}