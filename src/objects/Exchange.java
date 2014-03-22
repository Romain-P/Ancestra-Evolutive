package objects;

import java.util.ArrayList;


import common.Couple;
import common.SocketManager;
import common.World;

public class Exchange
{
	private Personnage perso1;
	private Personnage perso2;
	private long kamas1 = 0;
	private long kamas2 = 0;
	private ArrayList<Couple<Integer,Integer>> items1 = new ArrayList<Couple<Integer,Integer>>();
	private ArrayList<Couple<Integer,Integer>> items2 = new ArrayList<Couple<Integer,Integer>>();
	private boolean ok1;
	private boolean ok2;
	
	public Exchange(Personnage p1, Personnage p2)
	{
		perso1 = p1;
		perso2 = p2;
	}
	
	synchronized public long getKamas(int guid)
	{
		int i = 0;
		if(perso1.get_GUID() == guid)
			i = 1;
		else if(perso2.get_GUID() == guid)
			i = 2;
		
		if(i == 1)
			return kamas1;
		else if (i == 2)
			return kamas2;
		return 0;
	}
	
	synchronized public void toogleOK(int guid)
	{
		int i = 0;
		if(perso1.get_GUID() == guid)
			i = 1;
		else if(perso2.get_GUID() == guid)
			i = 2;
		
		if(i == 1)
		{
			ok1 = !ok1;
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameClient(),ok1,guid);
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameClient(),ok1,guid);
		}
		else if (i == 2)
		{
			ok2 = !ok2;
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameClient(),ok2,guid);
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameClient(),ok2,guid);
		}
		else 
			return;
		
		
		if(ok1 && ok2)
			apply();
	}
	
	synchronized public void setKamas(int guid, long k)
	{
		ok1 = false;
		ok2 = false;
		
		int i = 0;
		if(perso1.get_GUID() == guid)
			i = 1;
		else if(perso2.get_GUID() == guid)
			i = 2;
		SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameClient(),ok1,perso1.get_GUID());
		SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameClient(),ok1,perso1.get_GUID());
		SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameClient(),ok2,perso2.get_GUID());
		SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameClient(),ok2,perso2.get_GUID());
		
		if(i == 1)
		{
			kamas1 = k;
			SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'G', "", k+"");
			SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameClient(), 'G', "", k+"");
		}else if (i == 2)
		{
			kamas2 = k;
			SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameClient(), 'G', "", k+"");
			SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'G', "", k+"");	
		}
	}
	
	synchronized public void cancel()
	{
		if(perso1.get_compte() != null)if(perso1.get_compte().getGameClient() != null)SocketManager.GAME_SEND_EV_PACKET(perso1.get_compte().getGameClient());
		if(perso2.get_compte() != null)if(perso2.get_compte().getGameClient() != null)SocketManager.GAME_SEND_EV_PACKET(perso2.get_compte().getGameClient());
		perso1.set_isTradingWith(0);
		perso2.set_isTradingWith(0);
		perso1.setCurExchange(null);
		perso2.setCurExchange(null);
	}
	
	synchronized public void apply()
	{
		//Gestion des Kamas
		perso1.addKamas((-kamas1+kamas2));
		perso2.addKamas((-kamas2+kamas1));
		for(Couple<Integer, Integer> couple : items1)
		{
			if(couple.second == 0)continue;
			if(!perso1.hasItemGuid(couple.first))//Si le perso n'a pas l'item (Ne devrait pas arriver)
			{
				couple.second = 0;//On met la quantité a 0 pour éviter les problemes
				continue;
			}	
			Objet obj = World.data.getObjet(couple.first);
			if((obj.getQuantity() - couple.second) <1)//S'il ne reste plus d'item apres l'échange
			{
				perso1.removeItem(couple.first);
				couple.second = obj.getQuantity();
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso1, couple.first);
				if(!perso2.addObjet(obj, true))//Si le joueur avait un item similaire
					World.data.removeItem(couple.first);//On supprime l'item inutile
			}else
			{
				obj.setQuantity(obj.getQuantity()-couple.second);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso1, obj);
				Objet newObj = Objet.getCloneObjet(obj, couple.second);
				if(perso2.addObjet(newObj, true))//Si le joueur n'avait pas d'item similaire
					World.data.addObjet(newObj,true);//On ajoute l'item au World
			}
		}
		for(Couple<Integer, Integer> couple : items2)
		{
			if(couple.second == 0)continue;
			if(!perso2.hasItemGuid(couple.first))//Si le perso n'a pas l'item (Ne devrait pas arriver)
			{
				couple.second = 0;//On met la quantité a 0 pour éviter les problemes
				continue;
			}	
			Objet obj = World.data.getObjet(couple.first);
			if((obj.getQuantity() - couple.second) <1)//S'il ne reste plus d'item apres l'échange
			{
				perso2.removeItem(couple.first);
				couple.second = obj.getQuantity();
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso2, couple.first);
				if(!perso1.addObjet(obj, true))//Si le joueur avait un item similaire
					World.data.removeItem(couple.first);//On supprime l'item inutile
			}else
			{
				obj.setQuantity(obj.getQuantity()-couple.second);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso2, obj);
				Objet newObj = Objet.getCloneObjet(obj, couple.second);
				if(perso1.addObjet(newObj, true))//Si le joueur n'avait pas d'item similaire
					World.data.addObjet(newObj,true);//On ajoute l'item au World
			}
		}
		//Fin
		perso1.set_isTradingWith(0);
		perso2.set_isTradingWith(0);
		perso1.setCurExchange(null);
		perso2.setCurExchange(null);
		SocketManager.GAME_SEND_Ow_PACKET(perso1);
		SocketManager.GAME_SEND_Ow_PACKET(perso2);
		SocketManager.GAME_SEND_STATS_PACKET(perso1);
		SocketManager.GAME_SEND_STATS_PACKET(perso2);
		SocketManager.GAME_SEND_EXCHANGE_VALID(perso1.get_compte().getGameClient(),'a');
		SocketManager.GAME_SEND_EXCHANGE_VALID(perso2.get_compte().getGameClient(),'a');	
		perso1.save();
		perso2.save();
	}

	synchronized public void addItem(int guid, int qua, int pguid)
	{
		ok1 = false;
		ok2 = false;
		
		Objet obj = World.data.getObjet(guid);
		int i = 0;
		
		if(perso1.get_GUID() == pguid) i = 1;
		if(perso2.get_GUID() == pguid) i = 2;
		
		if(qua == 1) qua = 1;
		String str = guid+"|"+qua;
		if(obj == null)return;
		String add = "|"+obj.getTemplate().getID()+"|"+obj.parseStatsString();
		SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameClient(),ok1,perso1.get_GUID());
		SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameClient(),ok1,perso1.get_GUID());
		SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameClient(),ok2,perso2.get_GUID());
		SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameClient(),ok2,perso2.get_GUID());
		if(i == 1)
		{
			Couple<Integer,Integer> couple = getCoupleInList(items1,guid);
			if(couple != null)
			{
				couple.second += qua;
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", ""+guid+"|"+couple.second);
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameClient(), 'O', "+", ""+guid+"|"+couple.second+add);
				return;
			}
			SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", str);
			SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameClient(), 'O', "+", str+add);	
			items1.add(new Couple<Integer,Integer>(guid,qua));
		}else if(i == 2)
		{
			Couple<Integer,Integer> couple = getCoupleInList(items2,guid);
			if(couple != null)
			{
				couple.second += qua;
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", ""+guid+"|"+couple.second);
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameClient(), 'O', "+", ""+guid+"|"+couple.second+add);
				return;
			}
			SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", str);
			SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameClient(), 'O', "+", str+add);
			items2.add(new Couple<Integer,Integer>(guid,qua));
		}
	}

	
	synchronized public void removeItem(int guid, int qua, int pguid)
	{
		int i = 0;
		if(perso1.get_GUID() == pguid)
			i = 1;
		else if(perso2.get_GUID() == pguid)
			i = 2;
		ok1 = false;
		ok2 = false;
		
		SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameClient(),ok1,perso1.get_GUID());
		SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameClient(),ok1,perso1.get_GUID());
		SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameClient(),ok2,perso2.get_GUID());
		SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameClient(),ok2,perso2.get_GUID());
		
		Objet obj = World.data.getObjet(guid);
		if(obj == null)return;
		String add = "|"+obj.getTemplate().getID()+"|"+obj.parseStatsString();
		if(i == 1)
		{
			Couple<Integer,Integer> couple = getCoupleInList(items1,guid);
			int newQua = couple.second - qua;
			if(newQua <1)//Si il n'y a pu d'item
			{
				items1.remove(couple);
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "-", ""+guid);
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameClient(), 'O', "-", ""+guid);
			}else
			{
				couple.second = newQua;
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", ""+guid+"|"+newQua);
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameClient(), 'O', "+", ""+guid+"|"+newQua+add);
			}
		}else if(i ==2)
		{
			Couple<Integer,Integer> couple = getCoupleInList(items2,guid);
			int newQua = couple.second - qua;
			
			if(newQua <1)//Si il n'y a pu d'item
			{
				items2.remove(couple);
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameClient(), 'O', "-", ""+guid);
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "-", ""+guid);
			}else
			{
				couple.second = newQua;
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameClient(), 'O', "+", ""+guid+"|"+newQua+add);
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", ""+guid+"|"+newQua);
			}
		}
	}

	synchronized private Couple<Integer, Integer> getCoupleInList(ArrayList<Couple<Integer, Integer>> items,int guid)
	{
		for(Couple<Integer, Integer> couple : items)
		{
			if(couple.first == guid)
				return couple;
		}
		return null;
	}
	
	public synchronized int getQuaItem(int itemID, int playerGuid)
	{
		ArrayList<Couple<Integer, Integer>> items;
		if(perso1.get_GUID() == playerGuid)
			items = items1;
		else
			items = items2;
		
		for(Couple<Integer, Integer> curCoupl : items)
		{
			if(curCoupl.first == itemID)
			{
				return curCoupl.second;
			}
		}
		
		return 0;
	}
	
}