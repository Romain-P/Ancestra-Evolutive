package objects;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import common.Ancestra;
import common.Constants;
import common.SQLManager;
import common.SocketManager;
import common.World;

public class House
{
	private int _id;
	private short _map_id;
	private int _cell_id;
	private int _owner_id;
	private int _sale;
	private int _guild_id;
	private int _guildRights;
	private int _access;
	private String _key;
	private int _mapid;
	private int _caseid;
	
	//Droits de chaques maisons
	private Map<Integer,Boolean> haveRight = new TreeMap<Integer,Boolean>();

	
	public House(int id, short map_id, int cell_id, int owner_id, int sale,
			int guild_id, int access, String key, int guildrights, int mapid, int caseid) 
	{
		_id = id;
		_map_id = map_id;
		_cell_id = cell_id;
		_owner_id = owner_id;
		_sale = sale;
		_guild_id = guild_id;
		_access = access;
		_key = key;
		_guildRights = guildrights;
		parseIntToRight(guildrights);
		_mapid = mapid;
		_caseid = caseid;
	}
	
	public int get_id()
	{
		return _id;
	}
	
	public short get_map_id()
	{
		return _map_id;
	}
	
	public int get_cell_id()
	{
		return _cell_id;
	}
	
	public int get_owner_id()
	{
		return _owner_id;
	}
	
	public void set_owner_id(int id)
	{
		_owner_id = id;
	}
	
	public int get_sale()
	{
		return _sale;
	}
	
	public void set_sale(int price)
	{
		_sale = price;
	}
	
	public int get_guild_id()
	{
		return _guild_id;
	}
	
	public void set_guild_id(int GuildID)
	{
		_guild_id = GuildID;
	}
	
	public int get_guild_rights()
	{
		return _guildRights;
	}
	
	public void set_guild_rights(int GuildRights)
	{
		_guildRights = GuildRights;
	}
	
	public int get_access()
	{
		return _access;
	}
	
	public void set_access(int access)
	{
		_access = access;
	}
	
	public String get_key()
	{
		return _key;
	}
	
	public void set_key(String key)
	{
		_key = key;
	}
	
	public int get_mapid()
	{
		return _mapid;
	}
	
	public int get_caseid()
	{
		return _caseid;
	}
	
	public static House get_house_id_by_coord(int map_id, int cell_id)
	{
		for(Entry<Integer, House> house : World.getHouses().entrySet())
		{
			if(house.getValue().get_map_id() == map_id && house.getValue().get_cell_id() == cell_id)
			{
				return house.getValue();
			}
		}
		return null;
	}
	
	public static void LoadHouse(Personnage P, int newMapID)//Affichage des maison + Blason
	{
		
		for(Entry<Integer, House> house : World.getHouses().entrySet())
		{
			if(house.getValue().get_map_id() == newMapID)
			{
				StringBuilder packet = new StringBuilder();
				packet.append("P").append(house.getValue().get_id()).append("|");
				if(house.getValue().get_owner_id() > 0)
				{
					Compte C = World.getCompte(house.getValue().get_owner_id());
					if(C == null)//Ne devrait pas arriver
					{
						packet.append("undefined;");
					}else
					{
						packet.append(World.getCompte(house.getValue().get_owner_id()).get_pseudo()).append(";");
					}
				}else
				{
					packet.append(";");
				}
				if(house.getValue().get_sale() > 0)//Si prix > 0
				{
					packet.append("1");//Achetable
				}else
				{
					packet.append("0");//Non achetable
				}
				if(house.getValue().get_guild_id() > 0) //Maison de guilde
				{
					Guild G = World.getGuild(house.getValue().get_guild_id());
					if(G != null)
					{
						String Gname = G.get_name();
						String Gemblem = G.get_emblem();
						if(G.getMembers().size() < 10)//Ce n'est plus une maison de guilde
						{
							SQLManager.HOUSE_GUILD(house.getValue(), 0, 0) ;
						}else
						{
							//Affiche le blason pour les membre de guilde OU Affiche le blason pour les non membre de guilde
							if(P.get_guild() != null && P.get_guild().get_id() == house.getValue().get_guild_id() && house.getValue().canDo(Constants.H_GBLASON))//meme guilde
							{
								packet.append(";").append(Gname).append(";").append(Gemblem);
							}
							else if(house.getValue().canDo(Constants.H_OBLASON))//Pas de guilde/guilde-différente
							{
								packet.append(";").append(Gname).append(";").append(Gemblem);
							}
						}
					}
				}
				SocketManager.GAME_SEND_hOUSE(P, packet.toString());

				if(house.getValue().get_owner_id() == P.getAccID())
				{
					StringBuilder packet1 = new StringBuilder();
					packet1.append("L+|").append(house.getValue().get_id()).append(";").append(house.getValue().get_access()).append(";");
					
					if(house.getValue().get_sale() <= 0)
					{
						packet1.append("0;").append(house.getValue().get_sale());
					}
					else if(house.getValue().get_sale() > 0)
					{
						packet1.append("1;").append(house.getValue().get_sale());
					}
					SocketManager.GAME_SEND_hOUSE(P, packet1.toString());
				}
			}
		}
	}

	public void HopIn(Personnage P)//Entrer dans la maison
	{
		// En gros si il fait quelque chose :)
		if(P.get_fight() != null ||
		   P.get_isTalkingWith() != 0 ||
		   P.get_isTradingWith() != 0 ||
		   P.getCurJobAction() != null ||
		   P.get_curExchange() != null)
		{
			return;
		}
		
		House h = P.getInHouse();
		if(h == null) return;
		if(h.get_owner_id() == P.getAccID() || (P.get_guild() != null && P.get_guild().get_id() == h.get_guild_id() && canDo(Constants.H_GNOCODE)))//C'est sa maison ou même guilde + droits entrer sans pass
		{
			OpenHouse(P, "-", true);
		}
		else if(h.get_owner_id() > 0) //Une personne autre la acheter, il faut le code pour rentrer
		{
			SocketManager.GAME_SEND_KODE(P, "CK0|8");//8 étant le nombre de chiffre du code
		}
		else if(h.get_owner_id() == 0) //Maison non acheter, mais achetable, on peut rentrer sans code
		{
			OpenHouse(P, "-", false);
		}else
		{
			return;
		}
	}
	
	public static void OpenHouse(Personnage P, String packet, boolean isHome)//Ouvrir une maison ;o
	{
		
		House h = P.getInHouse();
		if((!h.canDo(Constants.H_OCANTOPEN) && (packet.compareTo(h.get_key()) == 0)) || isHome)//Si c'est chez lui ou que le mot de passe est bon
		{
			P.teleport((short)h.get_mapid(), h.get_caseid());
			closeCode(P);
		}else if((packet.compareTo(h.get_key()) != 0) || h.canDo(Constants.H_OCANTOPEN))//Mauvais code
		{
			SocketManager.GAME_SEND_KODE(P, "KE");
			SocketManager.GAME_SEND_KODE(P, "V");
		}
	}
	
	public void BuyIt(Personnage P)//Acheter une maison
	{
		House h = P.getInHouse();
		String str = "CK"+h.get_id()+"|"+h.get_sale();//ID + Prix
		SocketManager.GAME_SEND_hOUSE(P, str);
	}

	public static void HouseAchat(Personnage P)//Acheter une maison
	{
		House h = P.getInHouse();

		if(AlreadyHaveHouse(P))
		{
			SocketManager.GAME_SEND_Im_PACKET(P, "132;1");
			return;
		}
		//On enleve les kamas
		if(P.get_kamas() < h.get_sale()) return;
		long newkamas = P.get_kamas()-h.get_sale();
		P.set_kamas(newkamas);
		
		int tKamas = 0;
		for(Trunk t : Trunk.getTrunksByHouse(h))
		{
			if(h.get_owner_id() > 0)
			{
				t.moveTrunktoBank(World.getCompte(h.get_owner_id()));//Déplacement des items vers la banque
			}
			tKamas += t.get_kamas();
			t.set_kamas(0);//Retrait kamas
			t.set_key("-");//ResetPass
			t.set_owner_id(0);//ResetOwner
			SQLManager.UPDATE_TRUNK(t);
		}
		
		//Ajoute des kamas dans la banque du vendeur
		if(h.get_owner_id() > 0)
		{
			Compte Seller = World.getCompte(h.get_owner_id());
			long newbankkamas = Seller.getBankKamas()+h.get_sale()+tKamas;
			Seller.setBankKamas(newbankkamas);
			//Petit message pour le prévenir si il est on?
			if(Seller.get_curPerso() != null)
			{
				SocketManager.GAME_SEND_MESSAGE(Seller.get_curPerso(), "Une maison vous appartenant à été vendue "+h.get_sale()+" kamas.", Ancestra.CONFIG_MOTD_COLOR);
				SQLManager.SAVE_PERSONNAGE(Seller.get_curPerso(), true);
			}
			SQLManager.UPDATE_ACCOUNT_DATA(Seller);
		}
		
		//On save l'acheteur
		SQLManager.SAVE_PERSONNAGE(P, true);
		SocketManager.GAME_SEND_STATS_PACKET(P);
		closeBuy(P);

		//Achat de la maison
		SQLManager.HOUSE_BUY(P, h);

		//Rafraichir la map après l'achat
		for(Personnage z:P.get_curCarte().getPersos())
		{
			LoadHouse(z, z.get_curCarte().get_id());
		}
	}
	
	public void SellIt(Personnage P)//Vendre une maison
	{
		House h = P.getInHouse();
		if(isHouse(P, h))
		{
			String str = "CK"+h.get_id()+"|"+h.get_sale();//ID + Prix
			SocketManager.GAME_SEND_hOUSE(P, str);
				return;
		}else
		{
			return;
		}
	}
	
	public static void SellPrice(Personnage P, String packet)//Vendre une maison
	{
		House h = P.getInHouse();
		int price = Integer.parseInt(packet);	
		if(h.isHouse(P, h))
		{
			SocketManager.GAME_SEND_hOUSE(P, "V");
			SocketManager.GAME_SEND_hOUSE(P, "SK"+h.get_id()+"|"+price);
				
			//Vente de la maison
			SQLManager.HOUSE_SELL(h, price);

			//Rafraichir la map après la mise en vente
			for(Personnage z:P.get_curCarte().getPersos())
			{
				LoadHouse(z, z.get_curCarte().get_id());
			}
				
			return;
		}else
		{
			return;
		}
	}

	public boolean isHouse(Personnage P, House h)//Savoir si c'est sa maison
	{
		if(h.get_owner_id() == P.getAccID()) return true;
		else return false;
	}
	
	public static void closeCode(Personnage P)
	{
		SocketManager.GAME_SEND_KODE(P, "V");
	}
	
	public static void closeBuy(Personnage P)
	{
		SocketManager.GAME_SEND_hOUSE(P, "V");
	}
	
	public void Lock(Personnage P) 
	{
		SocketManager.GAME_SEND_KODE(P, "CK1|8");
	}
	
	public static void LockHouse(Personnage P, String packet) 
	{
		House h = P.getInHouse();
		if(h.isHouse(P, h))
		{
			SQLManager.HOUSE_CODE(P, h, packet);//Change le code
			closeCode(P);
			return;
		}else
		{
			closeCode(P);
			return;
		}
	}
	
	public static String parseHouseToGuild(Personnage P)
	{
		boolean isFirst = true;
		StringBuilder packet = new StringBuilder();
		for(Entry<Integer, House> house : World.getHouses().entrySet())
		{
			if(house.getValue().get_guild_id() == P.get_guild().get_id() && house.getValue().get_guild_rights() > 0)
			{
				if(isFirst) packet.append("+");
				if(!isFirst) packet.append("|");
				
				packet.append(house.getKey()).append(";");
				packet.append(World.getPersonnage(house.getValue().get_owner_id()).get_compte().get_pseudo()).append(";");
				packet.append(World.getCarte((short)house.getValue().get_mapid()).getX()).append(",").append(World.getCarte((short)house.getValue().get_mapid()).getY()).append(";");
				packet.append("0;");//TODO : Compétences ...
				packet.append(house.getValue().get_guild_rights());	
				isFirst = false;
			}
		}
			return packet.toString();
	}
	
	public static boolean AlreadyHaveHouse(Personnage P)
	{
		for(Entry<Integer, House> house : World.getHouses().entrySet())
		{
			if(house.getValue().get_owner_id() == P.getAccID())
			{
				return true;
			}
		}
		return false;
	}
	
	public static void parseHG(Personnage P, String packet)
	{
		House h = P.getInHouse();
		
		if(P.get_guild() == null) return;
		
		if(packet != null)
		{
			if(packet.charAt(0) == '+')
			{
				//Ajoute en guilde
				byte HouseMaxOnGuild = (byte) Math.floor(P.get_guild().get_lvl()/10);
				if(HouseOnGuild(P.get_guild().get_id()) >= HouseMaxOnGuild) return;
				if(P.get_guild().getMembers().size() < 10) return;
				SQLManager.HOUSE_GUILD(h, P.get_guild().get_id(), 0);
				parseHG(P, null);
			}
			else if(packet.charAt(0) == '-')
			{
				//Retire de la guilde
				SQLManager.HOUSE_GUILD(h, 0, 0);
				parseHG(P, null);
			}
			else
			{
				SQLManager.HOUSE_GUILD(h, h.get_guild_id(), Integer.parseInt(packet));
				h.parseIntToRight(Integer.parseInt(packet));
			}
		}
		else if(packet == null)
		{
		if(h.get_guild_id() <= 0)
		{
			SocketManager.GAME_SEND_hOUSE(P, "G"+h.get_id());
		}else if(h.get_guild_id() > 0)
		{
			SocketManager.GAME_SEND_hOUSE(P, "G"+h.get_id()+";"+P.get_guild().get_name()+";"+P.get_guild().get_emblem()+";"+h.get_guild_rights());
		}
		}
	}
	
	public static byte HouseOnGuild(int GuildID) 
	{
		byte i = 0;
		for(Entry<Integer, House> house : World.getHouses().entrySet())
		{
			if(house.getValue().get_guild_id() == GuildID)
			{
				i++;
			}
		}
		return i;
	}

	public boolean canDo(int rightValue)
	{	
		return haveRight.get(rightValue);
	}
	
	public void initRight()
	{
		haveRight.put(Constants.H_GBLASON, false);
		haveRight.put(Constants.H_OBLASON,false);
		haveRight.put(Constants.H_GNOCODE,false);
		haveRight.put(Constants.H_OCANTOPEN,false);
		haveRight.put(Constants.C_GNOCODE,false);
		haveRight.put(Constants.C_OCANTOPEN,false);
		haveRight.put(Constants.H_GREPOS,false);
		haveRight.put(Constants.H_GTELE,false);
	}
	
	public void parseIntToRight(int total)
	{
		if(haveRight.isEmpty())
		{
			initRight();
		}
		if(total == 1)
			return;

		if(haveRight.size() > 0)	//Si les droits contiennent quelque chose -> Vidage (Même si le TreeMap supprimerais les entrées doublon lors de l'ajout)
			haveRight.clear();

		initRight();	//Remplissage des droits

		Integer[] mapKey = haveRight.keySet().toArray(new Integer[haveRight.size()]);	//Récupère les clef de map dans un tableau d'Integer

		while(total > 0)
		{
			for (int i = haveRight.size()-1; i < haveRight.size(); i--)
			{
				if(mapKey[i].intValue() <= total)
				{
					total ^= mapKey[i].intValue();
					haveRight.put(mapKey[i],true);
					break;
				}
			}
		}
	}
	
	public static void Leave(Personnage P, String packet)
	{
		House h = P.getInHouse();
		if(!h.isHouse(P, h)) return;
		int Pguid = Integer.parseInt(packet);
		Personnage Target = World.getPersonnage(Pguid);
		if(Target == null || !Target.isOnline() || Target.get_fight() != null || Target.get_curCarte().get_id() != P.get_curCarte().get_id()) return;
		Target.teleport(h.get_map_id(), h.get_cell_id());
		SocketManager.GAME_SEND_Im_PACKET(Target, "018;"+P.get_name());
	}
	
	
	public static House get_HouseByPerso(Personnage P)//Connaitre la MAPID + CELLID de sa maison
	{
		for(Entry<Integer, House> house : World.getHouses().entrySet())
		{
			if(house.getValue().get_owner_id() == P.getAccID())
			{
				return house.getValue();
			}
		}
		return null;
	}
	
	public static void removeHouseGuild(int GuildID)
	{
		for(Entry<Integer, House> h : World.getHouses().entrySet())
		{
			if(h.getValue().get_guild_id() == GuildID)
			{
				h.getValue().set_guild_rights(0);
				h.getValue().set_guild_id(0);
			}else
			{
				continue;
			}
		}
		SQLManager.HOUSE_GUILD_REMOVE(GuildID);//Supprime les maisons de guilde
	}
}