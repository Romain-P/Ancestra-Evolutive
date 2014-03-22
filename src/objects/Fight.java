package objects;

import game.GameAction;
import game.GameClient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Timer;

import objects.Carte.Case;
import objects.Monstre.MobGrade;
import objects.Monstre.MobGroup;
import objects.Objet.ObjTemplate;
import objects.Personnage.Group;
import objects.Sort.SortStats;
import tool.time.waiter.Waiter;

import common.Constants;
import common.Couple;
import common.CryptManager;
import common.Formulas;
import common.IA;
import common.Pathfinding;
import common.SocketManager;
import common.World;

import core.Console;
import core.Log;
import core.Server;

public class Fight
{
	private int _id;
	private Map<Integer,Fighter> _team0 = new ConcurrentHashMap<>();
	private Map<Integer,Fighter> _team1 = new ConcurrentHashMap<>();
	private Map<Integer,Fighter> deadList = new ConcurrentHashMap<>();
	private Map<Integer,Personnage> _spec  = new ConcurrentHashMap<>();
	private Carte _map, _mapOld;
	private Fighter _init0, _init1;
	private ArrayList<Case> _start0 = new ArrayList<Case>(), _start1 = new ArrayList<Case>();
	private int _state = 0;
	private int _guildID = -1;
	private int _type = -1;
	private boolean locked0, onlyGroup0, locked1 = false;
	private boolean onlyGroup1, specOk = true, help1 = false,  help2 = false;
	private int _st2, _st1, _curPlayer;
	private long _startTime = 0;
	private int _curFighterPA, _curFighterPM, _curFighterUsedPA;
	private int _curFighterUsedPM;
	private String _curAction = "";
	private List<Fighter> _ordreJeu = new ArrayList<>();
	private List<Glyphe> _glyphs = new ArrayList<>();
	private List<Piege> _traps = new ArrayList<>();
	private MobGroup _mobGroup;
	private Percepteur _perco;
	
	private List<Fighter> _captureur = new CopyOnWriteArrayList<>();	
	private boolean isCapturable = false;
	private int captWinner = -1;
	private PierreAme pierrePleine;
	//waiter
	private Waiter waiter = new Waiter();
	//protector
	private Fighter protector;
	
	private Timer _turnTimer;
	//TIMER décompte toutes les secondes
	private Timer TurnTimer (final int timer, final Percepteur perco)
	{
	    ActionListener action = new ActionListener ()
	      {
	    	int Time = timer;
	        public void actionPerformed (ActionEvent event)
	        {
	        	Time = Time-1000;
	        	if(perco != null) perco.remove_timeTurn(1000);
	        	if(Time <= 0)
	        	{
	        		startFight();
					get_turnTimer().stop();
					if(perco != null) perco.set_timeTurn(45000);
					return;
	        	}
	        }
	      };
	    return new Timer (1000, action);
	 }
	
	public Fight(int type, int id,Carte map, Personnage init1, Personnage init2, boolean init2Protected)
	{
		_type = type; //0: Défie (4: Pvm) 1:PVP (5:Perco)
		_id = id;
		_map = map.getMapCopy();
		set_mapOld(map);
		_init0 = new Fighter(Fight.this,init1);
		_init1 = new Fighter(Fight.this,init2);
		_team0.put(init1.get_GUID(), _init0);
		_team1.put(init2.get_GUID(), _init1);
		if(init2Protected) {
			protector = new Fighter(this, World.data.getMonstre(394).getGradeByLevel(Constants.getProtectorLevelByAttacker(_init0)));
			_team1.put(protector.getGUID(), protector);
		}
		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(Fight.this,7,2,_type==Constants.FIGHT_TYPE_CHALLENGE?1:0,1,0,_type==Constants.FIGHT_TYPE_CHALLENGE?0:45000,_type);
		//on desactive le timer de regen coté client
		SocketManager.GAME_SEND_ILF_PACKET(init1, 0);
		SocketManager.GAME_SEND_ILF_PACKET(init2, 0);
		if(_type!=Constants.FIGHT_TYPE_CHALLENGE)
		{
			set_turnTimer(TurnTimer(45000, null));
			get_turnTimer().start();
		}
		Random teams = new Random();
		if(teams.nextBoolean())
		{
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight.this,1,_map.get_placesStr(),0);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight.this,2,_map.get_placesStr(),1);
			_st1 = 0;
			_st2 = 1;	
		}else
		{
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight.this,1,_map.get_placesStr(),1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight.this,2,_map.get_placesStr(),0);
		}	
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, init1.get_GUID()+"", init1.get_GUID()+","+Constants.ETAT_PORTE+",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, init1.get_GUID()+"", init1.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, init2.get_GUID()+"", init2.get_GUID()+","+Constants.ETAT_PORTE+",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, init2.get_GUID()+"", init2.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
		if(init2Protected) {
			Case cell = null;
			do {	
				cell = getRandomCell(_start1);
			} while (cell == null);
			
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, protector.getGUID()+"", protector.getGUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, protector.getGUID()+"", protector.getGUID()+","+Constants.ETAT_PORTEUR+",0");
			protector.set_fightCell(cell);
			protector.get_fightCell(false).addFighter(protector);
			protector.setTeam(1);
			protector.fullPDV();
			SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this._map, protector.getGUID(), protector);
		}
		_init0.set_fightCell(getRandomCell(_start0));
		_init1.set_fightCell(getRandomCell(_start1));
		
		_init0.getPersonnage().get_curCell().removePlayer(_init0.getGUID());
		_init1.getPersonnage().get_curCell().removePlayer(_init1.getGUID());
		
		_init0.get_fightCell(false).addFighter(_init0);
		_init1.get_fightCell(false).addFighter(_init1);
		_init0.getPersonnage().set_fight(Fight.this);
		_init0.setTeam(0);
		_init1.getPersonnage().set_fight(Fight.this);
		_init1.setTeam(1);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init1.getPersonnage().get_curCarte(), _init1.getGUID());
		if(_type == 1)
		{
			SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),0,_init0.getGUID(),_init1.getGUID(),_init0.getPersonnage().get_curCell().getID(),"0;"+_init0.getPersonnage().get_align(), _init1.getPersonnage().get_curCell().getID(), "0;"+_init1.getPersonnage().get_align());
		}else
		{
			SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),0,_init0.getGUID(),_init1.getGUID(),_init0.getPersonnage().get_curCell().getID(),"0;-1", _init1.getPersonnage().get_curCell().getID(), "0;-1");
		}
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init0.getGUID(), _init0);
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init1.getGUID(), _init1);
		
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(Fight.this,7,_map);
		
		set_state(Constants.FIGHT_STATE_PLACE);
	}
	
	public Fight(int id,Carte map,Personnage init1, MobGroup group)
	{
		_mobGroup = group;
		_type = Constants.FIGHT_TYPE_PVM; //(0: Défie) 4: Pvm (1:PVP) (5:Perco)
		_id = id;
		_map = map.getMapCopy();
		set_mapOld(map);
		_init0 = new Fighter(Fight.this,init1);
		
		_team0.put(init1.get_GUID(), _init0);
		for(Entry<Integer, MobGrade> entry : group.getMobs().entrySet())
		{
			entry.getValue().setInFightID(entry.getKey());
			Fighter mob = new Fighter(Fight.this,entry.getValue());
			_team1.put(entry.getKey(), mob);
		}
		
		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(Fight.this,1,2,0,1,0,45000,_type);
		
		//on desactive le timer de regen coté client
		SocketManager.GAME_SEND_ILF_PACKET(init1, 0);
		
		set_turnTimer(TurnTimer(45000, null));
		get_turnTimer().start();
		
		Random teams = new Random();
		if(teams.nextBoolean())
		{
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight.this,1,_map.get_placesStr(),0);
			_st1 = 0;
			_st2 = 1;
		}else
		{
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight.this,1,_map.get_placesStr(),1);
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, init1.get_GUID()+"", init1.get_GUID()+","+Constants.ETAT_PORTE+",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, init1.get_GUID()+"", init1.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
		
		List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer,Fighter>>();
		e.addAll(_team1.entrySet());
		for(Entry<Integer,Fighter> entry : e)
		{
			Fighter f = entry.getValue();
			Case cell = getRandomCell(_start1);
			if(cell == null)
			{
				_team1.remove(f.getGUID());
				continue;
			}
			
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, f.getGUID()+"", f.getGUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, f.getGUID()+"", f.getGUID()+","+Constants.ETAT_PORTEUR+",0");
			f.set_fightCell(cell);
			f.get_fightCell(false).addFighter(f);
			f.setTeam(1);
			f.fullPDV();
		}
		_init0.set_fightCell(getRandomCell(_start0));
		
		_init0.getPersonnage().get_curCell().removePlayer(_init0.getPersonnage().get_GUID());
		
		_init0.get_fightCell(false).addFighter(_init0);
		
		_init0.getPersonnage().set_fight(Fight.this);
		_init0.setTeam(0);
		
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), group.getID());
		
		SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),4,_init0.getGUID(),group.getID(),(_init0.getPersonnage().get_curCell().getID()+1),"0;-1",group.getCellID(),"1;-1");
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init0.getGUID(), _init0);
		
		for(Fighter f : _team1.values())
		{
			SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),group.getID(), f);
		}
		
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(Fight.this,7,_map);
		
		set_state(Constants.FIGHT_STATE_PLACE);
	}

	public Fight(int id, Carte map, Personnage perso, Percepteur perco) 
	{	
		set_guildID(perco.get_guildID());
		perco.set_inFight((byte)1);
		perco.set_inFightID((byte)id);
		
		_type = Constants.FIGHT_TYPE_PVT; //(0: Défie) (4: Pvm) (1:PVP) 5:Perco
		_id = id;
		_map = map.getMapCopy();
		set_mapOld(map);
		_init0 = new Fighter(Fight.this,perso);
		_perco = perco;
		
		_team0.put(perso.get_GUID(), _init0);

		Fighter percoF = new Fighter(Fight.this,perco);
		_team1.put(-1, percoF);

		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(Fight.this,1,2,0,1,0,45000,_type);
		
		//on desactive le timer de regen coté client
		SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
		
		set_turnTimer(TurnTimer(45000, perco));
		get_turnTimer().start();
		
		Random teams = new Random();
		if(teams.nextBoolean())
		{
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight.this,1,_map.get_placesStr(),0);
			_st1 = 0;
			_st2 = 1;
		}else
		{
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight.this,1,_map.get_placesStr(),1);
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTE+",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
		
		List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer,Fighter>>();
		e.addAll(_team1.entrySet());
		for(Entry<Integer,Fighter> entry : e)
		{
			Fighter f = entry.getValue();
			Case cell = getRandomCell(_start1);
			if(cell == null)
			{
				_team1.remove(f.getGUID());
				continue;
			}
			
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, f.getGUID()+"", f.getGUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, f.getGUID()+"", f.getGUID()+","+Constants.ETAT_PORTEUR+",0");
			f.set_fightCell(cell);
			f.get_fightCell(false).addFighter(f);
			f.setTeam(1);
			f.fullPDV();
		}
		_init0.set_fightCell(getRandomCell(_start0));
		
		_init0.getPersonnage().get_curCell().removePlayer(_init0.getPersonnage().get_GUID());
		
		_init0.get_fightCell(false).addFighter(_init0);
		
		_init0.getPersonnage().set_fight(Fight.this);
		_init0.setTeam(0);
		
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), perco.getGuid());
		
		SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),5,_init0.getGUID(),perco.getGuid(),(_init0.getPersonnage().get_curCell().getID()+1),"0;-1",perco.get_cellID(),"3;-1");
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init0.getGUID(), _init0);
		
		for(Fighter f : _team1.values())
		{
			SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),perco.getGuid(), f);
		}

		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(Fight.this,7,_map);
		set_state(Constants.FIGHT_STATE_PLACE);
		
		//On actualise la guilde+Message d'attaque FIXME
		for(Personnage z : World.data.getGuild(_guildID).getMembers())
		{
			if(z == null) continue;
			if(z.isOnline())
			{
				SocketManager.GAME_SEND_gITM_PACKET(z, Percepteur.parsetoGuild(z.get_guild().get_id()));
				Percepteur.parseAttaque(z, _guildID);
				Percepteur.parseDefense(z, _guildID);
				SocketManager.GAME_SEND_MESSAGE(z, "Un de vos percepteurs a ete attaque.", Server.config.getMotdColor());
			}
		}
	}
	
	public Carte get_map() {
		return _map;
	}

	public List<Piege> get_traps() {
		return _traps;
	}

	public List<Glyphe> get_glyphs() {
		return _glyphs;
	}

	private Case getRandomCell(List<Case> cells)
	{
		Random rand = new Random();
		Case cell;
		if(cells.isEmpty())return null;
		int limit = 0;
		do
		{
			int id = rand.nextInt(cells.size()-1);
			cell = cells.get(id);
			limit++;
		}while((cell == null || !cell.getFighters().isEmpty()) && limit < 80);
		if(limit == 80)
		{
			if(Server.config.isDebug()) Log.addToLog("Case non trouve dans la liste");
			return null;
		}
		return cell;		
	}
	
	private ArrayList<Case> parsePlaces(int num)
	{
		return CryptManager.parseStartCell(_map, num);
	}
	
	public int get_id() {
		return _id;
	}

	public ArrayList<Fighter> getFighters(int teams)//teams entre 0 et 7, binaire([spec][t2][t1]);
	{
		ArrayList<Fighter> fighters = new ArrayList<Fighter>();
		
		if(teams - 4 >= 0)
		{
			for(Entry<Integer,Personnage> entry : _spec.entrySet())
			{
				fighters.add(new Fighter(Fight.this,entry.getValue()));
			}
			teams -= 4;
		}
		if(teams -2 >= 0)
		{
			for(Entry<Integer,Fighter> entry : _team1.entrySet())
			{
				fighters.add(entry.getValue());
			}
			teams -= 2;
		}
		if(teams -1 >=0)
		{	
			for(Entry<Integer,Fighter> entry : _team0.entrySet())
			{
				fighters.add(entry.getValue());
			}
		}
		return fighters;
	}
	
	public synchronized void changePlace(Personnage perso,int cell)
	{
		Fighter fighter = getFighterByPerso(perso);
		int team = getTeamID(perso.get_GUID()) -1;
		if(fighter == null)return;
		if(get_state() != 2 || isOccuped(cell) || perso.is_ready() || (team == 0 && !groupCellContains(_start0,cell)) || (team == 1 && !groupCellContains(_start1,cell)))return;

		fighter.get_fightCell(false).getFighters().clear();
		fighter.set_fightCell(_map.getCase(cell));
		
		_map.getCase(cell).addFighter(fighter);
		SocketManager.GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(Fight.this,3,_map,perso.get_GUID(),cell);
	}

	public boolean isOccuped(int cell)
	{
		/* ex Code
		for(Entry<Integer,Fighter> entry : _team0.entrySet())
		{
			if(entry.getValue().getPDV() <= 0)continue;
			if(entry.getValue().get_fightCell().getID() == cell)
				return true;
		}
		for(Entry<Integer,Fighter> entry : _team1.entrySet())
		{
			if(entry.getValue().getPDV() <= 0)continue;
			if(entry.getValue().get_fightCell().getID() == cell)
				return true;
		}
		//*/
		return _map.getCase(cell).getFighters().size() > 0;
	}

	private boolean groupCellContains(ArrayList<Case> cells, int cell)
	{
		for(int a = 0; a<cells.size();a++)
		{
			if(cells.get(a).getID() == cell)
				return true;
		}
		return false;
	}

	public void verifIfAllReady()
	{
		boolean val = true;
		for(int a=0;a<_team0.size();a++)
		{
			if(!_team0.get(_team0.keySet().toArray()[a]).getPersonnage().is_ready())
				val = false;
		}
		if(_type != Constants.FIGHT_TYPE_PVM && _type != Constants.FIGHT_TYPE_PVT && protector == null)
		{
			for(int a=0;a<_team1.size();a++)
			{
				if(!_team1.get(_team1.keySet().toArray()[a]).getPersonnage().is_ready())
					val = false;
			}
		}
		if(_type == Constants.FIGHT_TYPE_PVT) val = false;//Evite de lancer le combat trop vite
		if(val)
		{
			startFight();
		}
	}

	private void startFight()
	{
		if(_state >= Constants.FIGHT_STATE_ACTIVE)return;
		SocketManager.GAME_SEND_GDF_PACKET_TO_MAP_TO_FIGHT(this);
		if(_type == Constants.FIGHT_TYPE_PVT)
		{
			_perco.set_inFight((byte)2);
			//On actualise la guilde+Message d'attaque FIXME
			for(Personnage z : World.data.getGuild(_guildID).getMembers())
			{
				if(z == null) continue;
				if(z.isOnline())
				{
					SocketManager.GAME_SEND_gITM_PACKET(z, Percepteur.parsetoGuild(z.get_guild().get_id()));
					Percepteur.parseAttaque(z, _guildID);
					Percepteur.parseDefense(z, _guildID);
					SocketManager.GAME_SEND_MESSAGE(z, "Un de vos percepteurs est rentre en combat.", Server.config.getMotdColor());
				}
			}
		}
		_state = Constants.FIGHT_STATE_ACTIVE;
		_startTime = System.currentTimeMillis();
		SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init0.getGUID());
		if(_type == Constants.FIGHT_TYPE_PVM)
		{
			int align = -1;
			if(_team1.size() >0)
			{
				 _team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign();
			}
			//Si groupe non fixe
			if(!_mobGroup.isFix())World.data.getCarte(_map.get_id()).spawnGroup(align, 1, true,_mobGroup.getCellID());//Respawn d'un groupe
		}
		SocketManager.GAME_SEND_GIC_PACKETS_TO_FIGHT(Fight.this, 7);
		SocketManager.GAME_SEND_GS_PACKET_TO_FIGHT(Fight.this, 7);
		InitOrdreJeu();
		_curPlayer = -1;
		SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(Fight.this,7);
		SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(Fight.this, 7);
		if(get_turnTimer()  != null)get_turnTimer().stop();
		set_turnTimer(null);
		set_turnTimer(new Timer(Constants.TIME_BY_TURN,new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					endTurn();
				}
			}));
		if(Server.config.isDebug()) Log.addToLog("Debut du combat");
		for(Fighter F : getFighters(3))
		{
			Personnage perso = F.getPersonnage();
			if(perso == null)continue;
			if(perso.isOnMount())
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_CHEVAUCHANT+",1");
			
		}
		getWaiter().addNext(new Runnable() {
			public void run() {
				startTurn();
			}
		}, 100);
	}

	private void startTurn()
	{
		if(!verifyStillInFight()) verifIfTeamAllDead();
		
		if(_state >= Constants.FIGHT_STATE_FINISHED)return;
		
		_curPlayer++;
		set_curAction("");
		if(_curPlayer >= _ordreJeu.size())_curPlayer = 0;
		
		set_curFighterPA(_ordreJeu.get(_curPlayer).getPA());
		set_curFighterPM(_ordreJeu.get(_curPlayer).getPM());
		_curFighterUsedPA = 0;
		_curFighterUsedPM = 0;
		
		if(_ordreJeu.get(_curPlayer).hasLeft() || _ordreJeu.get(_curPlayer).isDead())//Si joueur mort
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Fighter ID=  "+_ordreJeu.get(_curPlayer).getGUID()+" est mort");
			endTurn();
			return;
		}
		
		_ordreJeu.get(_curPlayer).applyBeginningTurnBuff(Fight.this);
		if(_state == Constants.FIGHT_STATE_FINISHED)return;
		if(_ordreJeu.get(_curPlayer).getPDV()<=0)onFighterDie(_ordreJeu.get(_curPlayer));
		
		//On actualise les sorts launch
		_ordreJeu.get(_curPlayer).ActualiseLaunchedSort();
		//reset des Max des Chatis
		_ordreJeu.get(_curPlayer).get_chatiValue().clear();
		//Gestion des glyphes
		ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();//Copie du tableau
		glyphs.addAll(_glyphs);
		
		for(Glyphe g : glyphs)
		{
			if(_state >= Constants.FIGHT_STATE_FINISHED)return;
			//Si c'est ce joueur qui l'a lancé
			if(g.get_caster().getGUID() == _ordreJeu.get(_curPlayer).getGUID())
			{
				//on réduit la durée restante, et si 0, on supprime
				if(g.decrementDuration() == 0)
				{
					_glyphs.remove(g);
					g.desapear();
					continue;//Continue pour pas que le joueur active le glyphe s'il était dessus
				}
			}
			//Si dans le glyphe
			int dist = Pathfinding.getDistanceBetween(_map,_ordreJeu.get(_curPlayer).get_fightCell(false).getID() , g.get_cell().getID());
			if(dist <= g.get_size() && g._spell != 476)//476 a effet en fin de tour
			{
				//Alors le joueur est dans le glyphe
				g.onTraped(_ordreJeu.get(_curPlayer));
			}
		}
		if(_ordreJeu == null)return;
		if(_ordreJeu.size() < _curPlayer)return;
		if(_ordreJeu.get(_curPlayer) == null)return;
		if(_ordreJeu.get(_curPlayer).isDead())//Si joueur mort
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Fighter ID=  "+_ordreJeu.get(_curPlayer).getGUID()+" est mort");
			endTurn();
			return;
		}
		if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
		{
			SocketManager.GAME_SEND_STATS_PACKET(_ordreJeu.get(_curPlayer).getPersonnage());
		}
		if(_ordreJeu.get(_curPlayer).hasBuff(Constants.EFFECT_PASS_TURN))//Si il doit passer son tour
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID()+" passe son tour");
			endTurn();
			return;
		}
		if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+")Debut du tour de Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID());
		SocketManager.GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(Fight.this,7,_ordreJeu.get(_curPlayer).getGUID(),Constants.TIME_BY_TURN);
		get_turnTimer().restart();
		
		_ordreJeu.get(_curPlayer).setCanPlay(true);

		if(_ordreJeu.get(_curPlayer).getPersonnage() == null || _ordreJeu.get(_curPlayer)._double != null || _ordreJeu.get(_curPlayer)._Perco != null)//Si ce n'est pas un joueur
		{
			if(_ordreJeu.get(_curPlayer) == null || _ordreJeu.get(_curPlayer).isDead())
				return;
			
			IA ia = new IA(_ordreJeu.get(_curPlayer), Fight.this);
			World.data.getIaWorker().execute(ia);
		}
		
	}

	public synchronized void endTurn() {
		try {
			if(_curPlayer == -1)return;
			if(_ordreJeu == null || _ordreJeu.get(_curPlayer) == null)return;
			if(_state >= Constants.FIGHT_STATE_FINISHED)return;
			if(!_turnTimer.isRunning() && !this._curAction.isEmpty()) return;
			if(_ordreJeu.get(_curPlayer).hasLeft() || _ordreJeu.get(_curPlayer).isDead()) {
				startTurn();
				return;
			}
			get_turnTimer().stop();
			_ordreJeu.get(_curPlayer).setCanPlay(false);
			
			if(!get_curAction().equals("") 
					 && _ordreJeu.get(_curPlayer).getPersonnage() != null)
				return;
			
			getWaiter().addNext(new Runnable() {
				public void run() {
					try {
					/**	_ordreJeu.get(_curPlayer).setCanPlay(false);
						if(!_curAction.equals("") && _ordreJeu.get(_curPlayer).getPersonnage() != null) {
							long l = System.currentTimeMillis();
							while(!_curAction.isEmpty()
									&& System.currentTimeMillis() - l < 5000){} //eviter une boucle infini
						} **/
						
						SocketManager.GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(Fight.this,7,_ordreJeu.get(_curPlayer).getGUID());
						
						set_curAction("");
						
						//Si empoisonné (Créer une fonction applyEndTurnbuff si d'autres effets existent)
						for(SpellEffect SE : _ordreJeu.get(_curPlayer).getBuffsByEffectID(131)) {
							int pas = SE.getValue();
							int val = -1;
							try {
								val = Integer.parseInt(SE.getArgs().split(";")[1]);
							} catch(Exception e){};
							if(val == -1)continue;
							
							int nbr = (int) Math.floor((double)_curFighterUsedPA/(double)pas);
							int dgt = val * nbr;
							//Si poison paralysant
							if(SE.getSpell() == 200) {
								int inte = SE.getCaster().getTotalStats().getEffect(Constants.STATS_ADD_INTE);
								if(inte < 0)inte = 0;
								int pdom = SE.getCaster().getTotalStats().getEffect(Constants.STATS_ADD_PERDOM);
								if(pdom < 0)pdom = 0;
								//on applique le boost
								dgt = (int)(((100+inte+pdom)/100) * dgt);
							}
							if(_ordreJeu.get(_curPlayer).hasBuff(184)) {
								SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 105, _ordreJeu.get(_curPlayer).getGUID()+"", _ordreJeu.get(_curPlayer).getGUID()+","+_ordreJeu.get(_curPlayer).getBuff(184).getValue());
								dgt = dgt-_ordreJeu.get(_curPlayer).getBuff(184).getValue();//Réduction physique
							}
							if(_ordreJeu.get(_curPlayer).hasBuff(105)) {
								SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 105, _ordreJeu.get(_curPlayer).getGUID()+"", _ordreJeu.get(_curPlayer).getGUID()+","+_ordreJeu.get(_curPlayer).getBuff(105).getValue());
								dgt = dgt-_ordreJeu.get(_curPlayer).getBuff(105).getValue();//Immu
							}
							if(dgt <= 0)continue;
							
							if(dgt>_ordreJeu.get(_curPlayer).getPDV())dgt = _ordreJeu.get(_curPlayer).getPDV();//va mourrir
							_ordreJeu.get(_curPlayer).removePDV(dgt);
							dgt = -(dgt);
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 100, SE.getCaster().getGUID()+"", _ordreJeu.get(_curPlayer).getGUID()+","+dgt);
						}
						ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();//Copie du tableau
						glyphs.addAll(_glyphs);
						for(Glyphe g : glyphs) {
							if(_state >= Constants.FIGHT_STATE_FINISHED)return;
							//Si dans le glyphe
							int dist = Pathfinding.getDistanceBetween(_map,_ordreJeu.get(_curPlayer).get_fightCell(false).getID() , g.get_cell().getID());
							if(dist <= g.get_size() && g._spell == 476)//476 a effet en fin de tour
							{
								//Alors le joueur est dans le glyphe
								g.onTraped(_ordreJeu.get(_curPlayer));
							}
						}
						if(_ordreJeu.get(_curPlayer).getPDV() <= 0)
							onFighterDie(_ordreJeu.get(_curPlayer));
						
						//reset des valeurs
						_curFighterUsedPA = 0;
						_curFighterUsedPM = 0;
						set_curFighterPA(_ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constants.STATS_ADD_PA));
						set_curFighterPM(_ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constants.STATS_ADD_PM));
						_ordreJeu.get(_curPlayer).refreshfightBuff();
						
						if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
							if(_ordreJeu.get(_curPlayer).getPersonnage().isOnline())
								SocketManager.GAME_SEND_STATS_PACKET(_ordreJeu.get(_curPlayer).getPersonnage());
						
						SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(Fight.this, 7);
						SocketManager.GAME_SEND_GTR_PACKET_TO_FIGHT(Fight.this, 7, _ordreJeu.get(_curPlayer==_ordreJeu.size()?0:_curPlayer).getGUID());
						if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+")Fin du tour de Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID());
						startTurn();
					} catch(NullPointerException e) {
						e.printStackTrace();
						endTurn();
					}
				}
			}, 100);
			
		} catch(Exception e) {
			e.printStackTrace();
			endTurn();
		}
			
	}

	private void InitOrdreJeu()
	{
		int curMaxIni = 0;
		Fighter curMax = null;
		boolean team1_ready = false;
		boolean team2_ready = false;
		byte actTeam = -1;
		do
		{
			if((actTeam == -1 || actTeam == 0 || team2_ready) && !team1_ready) 
			{
				team1_ready = true;
				for(Entry<Integer,Fighter> entry : _team0.entrySet())
				{
					if(_ordreJeu.contains(entry.getValue()))continue;
					team1_ready = false;
					if(entry.getValue().getInitiative() >= curMaxIni)
					{
						curMaxIni = entry.getValue().getInitiative();
						curMax = entry.getValue();
					}
				}
			}		
			if((actTeam == -1 || actTeam == 1 || team1_ready) && !team2_ready) 
			{
				team2_ready = true;
				for(Entry<Integer,Fighter> entry : _team1.entrySet())
				{
					if(_ordreJeu.contains(entry.getValue()))continue;
					team2_ready = false;
					if(entry.getValue().getInitiative() >= curMaxIni)
					{
						curMaxIni = entry.getValue().getInitiative();
						curMax = entry.getValue();
					}
				}
			}
				if(curMax == null)return;
				_ordreJeu.add(curMax);
				if(curMax.getTeam() == 0) 
					actTeam = 1; 
				else 
					actTeam = 0; 
				curMaxIni = 0;
				curMax = null;
		}while(_ordreJeu.size() != getFighters(3).size());
	}

	public void joinFight(Personnage perso, int guid)
	{	
		Fighter current_Join = null;
		if(_team0.containsKey(guid))
		{
			Case cell = getRandomCell(_start0);
			if(cell == null)return;
			
			if(onlyGroup0)
			{
				Group g = _init0.getPersonnage().getGroup();
				if(g != null)
				{
					if(!g.getPersos().contains(perso))
					{
						SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
						return;
					}
				}
			}
			if(_type == Constants.FIGHT_TYPE_AGRESSION)
			{
				if(perso.get_align() == Constants.ALIGNEMENT_NEUTRE)
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
					return;
				}
				if(_init0.getPersonnage().get_align() != perso.get_align())
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
					return;
				}
			}
			if(_guildID > -1 && perso.get_guild() != null)
			{
				if(get_guildID() == perso.get_guild().get_id()) 
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
					return;
				}
			}
			if(locked0)
			{
				SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
				return;
			}
			if(_type == Constants.FIGHT_TYPE_CHALLENGE)
			{
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,1,1,0,0,_type);
			}else
			{
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,0,1,0,0,_type);
			}
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameClient(), _map.get_placesStr(), _st1);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
			
			Fighter f = new Fighter(Fight.this, perso);
			current_Join = f;
			f.setTeam(0);
			_team0.put(perso.get_GUID(), f);
			perso.set_fight(Fight.this);
			f.set_fightCell(cell);
			f.get_fightCell(false).addFighter(f);
			//Désactive le timer de regen
			SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
		}else if(_team1.containsKey(guid))
		{
			Case cell = getRandomCell(_start1);
			if(cell == null)return;
			
			if(onlyGroup1)
			{
				Group g = _init1.getPersonnage().getGroup();
				if(g != null)
				{
					if(!g.getPersos().contains(perso))
					{
						SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
						return;
					}
				}
			}
			if(_type == Constants.FIGHT_TYPE_AGRESSION)
			{
				if(perso.get_align() == Constants.ALIGNEMENT_NEUTRE)
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
					return;
				}
				if(_init1.getPersonnage().get_align() != perso.get_align())
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
					return;
				}
			}
			if(_guildID > -1 && perso.get_guild() != null)
			{
				if(get_guildID() == perso.get_guild().get_id()) 
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
					return;
				}
			}
			if(locked1)
			{
				SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameClient(),'f',guid);
				return;
			}
			if(_type == Constants.FIGHT_TYPE_CHALLENGE)
			{
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,1,1,0,0,_type);
			}else
			{
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,0,1,0,0,_type);
			}
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameClient(), _map.get_placesStr(), _st2);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
			Fighter f = new Fighter(Fight.this, perso);
			current_Join = f;
			f.setTeam(1);
			_team1.put(perso.get_GUID(), f);
			perso.set_fight(Fight.this);
			f.set_fightCell(cell);
			f.get_fightCell(false).addFighter(f);
		}
		perso.get_curCell().removePlayer(perso.get_GUID());
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.get_curCarte(),(current_Join.getTeam()==0?_init0:_init1).getGUID(), current_Join);
		SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(Fight.this,7,current_Join);
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(Fight.this,_map,perso);
		if(_perco != null)
		{
			for(Personnage z : World.data.getGuild(_guildID).getMembers())
			{
				if(z.isOnline())
				{
					Percepteur.parseAttaque(z, _guildID);
					Percepteur.parseDefense(z, _guildID);
				}
			}
		}
	}
	
	public void joinPercepteurFight(final Personnage perso, int guid, final int percoID) {	
		perso.getWaiter().addNext(new Runnable() {
			public void run() {
				Fighter current_Join = null;
				Case cell = getRandomCell(_start1);
				if(cell == null)return;
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,0,1,0,0,_type);
				SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameClient(), _map.get_placesStr(), _st2);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTE+",0");
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
				Fighter f = new Fighter(Fight.this, perso);
				current_Join = f;
				f.setTeam(1);
				_team1.put(perso.get_GUID(), f);
				perso.set_fight(Fight.this);
				f.set_fightCell(cell);
				f.get_fightCell(false).addFighter(f);
				SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
				
				perso.get_curCell().removePlayer(perso.get_GUID());
				SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.get_curCarte(), percoID, current_Join);
				SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(Fight.this,7,current_Join);
				SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(Fight.this,_map,perso);
			}
		}, 700);
	}

	public void toggleLockTeam(int guid)
	{
		if(_init0 != null && _init0.getGUID() == guid)
		{
			locked0 = !locked0;
			if(Server.config.isDebug()) Log.addToLog(locked0?"L'equipe 1 devient bloquee":"L'equipe 1 n'est plus bloquee");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), locked0?'+':'-', 'A', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(Fight.this,1,locked0?"095":"096");
		}else if(_init1 != null && _init1.getGUID() == guid)
		{
			locked1 = !locked1;
			if(Server.config.isDebug()) Log.addToLog(locked1?"L'equipe 2 devient bloquee":"L'equipe 2 n'est plus bloquee");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(), locked1?'+':'-', 'A', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(Fight.this,2,locked1?"095":"096");
		}
	}
	
	public void toggleOnlyGroup(int guid)
	{
		if(_init0 != null && _init0.getGUID() == guid)
		{
			onlyGroup0 = !onlyGroup0;
			if(Server.config.isDebug()) Log.addToLog(locked0?"L'equipe 1 n'accepte que les membres du groupe":"L'equipe 1 n'est plus bloquee");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), onlyGroup0?'+':'-', 'P', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(Fight.this,1,onlyGroup0?"093":"094");
		}else if(_init1 != null && _init1.getGUID() == guid)
		{
			onlyGroup1 = !onlyGroup1;
			if(Server.config.isDebug()) Log.addToLog(locked1?"L'equipe 2 n'accepte que les membres du groupe":"L'equipe 2 n'est plus bloquee");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(), onlyGroup1?'+':'-', 'P', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(Fight.this,2,onlyGroup1?"095":"096");
		}
	}
	
	public void toggleLockSpec(int guid)
	{
		if((_init0 != null && _init0.getGUID() == guid) || (_init1 != null &&  _init1.getGUID() == guid))
		{
			specOk = !specOk;
			if(Server.config.isDebug()) Log.addToLog(specOk?"Le combat accepte les spectateurs":"Le combat n'accepte plus les spectateurs");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), specOk?'+':'-', 'S', _init0.getGUID());
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), specOk?'+':'-', 'S', _init1.getGUID());
			SocketManager.GAME_SEND_Im_PACKET_TO_MAP(_map,specOk?"039":"040");
		}
	}

	public void toggleHelp(int guid)
	{
		if(_init0 != null && _init0.getGUID() == guid)
		{
			help1 = !help1;
			if(Server.config.isDebug()) Log.addToLog(help2?"L'equipe 1 demande de l'aide":"L'equipe 1s ne demande plus d'aide");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), locked0?'+':'-', 'H', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(Fight.this,1,help1?"0103":"0104");
		}else if(_init1 != null && _init1.getGUID() == guid)
		{
			help2 = !help2;
			if(Server.config.isDebug()) Log.addToLog(help2?"L'equipe 2 demande de l'aide":"L'equipe 2 ne demande plus d'aide");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(), locked1?'+':'-', 'H', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(Fight.this,2,help2?"0103":"0104");
		}
	}
	
	private void set_state(int _state) {
		Fight.this._state = _state;
	}
	
	private void set_guildID(int guildID) {
		Fight.this._guildID = guildID;
	}

	public int get_state() {
		return _state;
	}
	
	public int get_guildID() {
		return _guildID;
	}
	
	public int get_type() {
		return _type;
	}

	public List<Fighter> get_ordreJeu() {
		return _ordreJeu;
	}

	public boolean fighterDeplace(Fighter f, GameAction GA)
	{
		String path = GA._args;
		if(path.equals(""))
		{
			if(Server.config.isDebug()) Log.addToLog("Echec du deplacement: chemin vide");
			return false;
		}
		if(_ordreJeu.size() <= _curPlayer)return false;
		if(_ordreJeu.get(_curPlayer) == null)return false;
		if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+")Tentative de deplacement de Fighter ID= "+f.getGUID()+" a partir de la case "+f.get_fightCell(false).getID());
		if(Server.config.isDebug()) Log.addToLog("Path: "+path);
		if(!get_curAction().equals("")|| _ordreJeu.get(_curPlayer).getGUID() != f.getGUID() || _state != Constants.FIGHT_STATE_ACTIVE)
		{
			if(!get_curAction().equals(""))
				if(Server.config.isDebug()) Log.addToLog("Echec du deplacement: il y deja une action en cours");
			if(_ordreJeu.get(_curPlayer).getGUID() != f.getGUID())
				if(Server.config.isDebug()) Log.addToLog("Echec du deplacement: ce n'est pas a ce joueur de jouer");
			if(_state != Constants.FIGHT_STATE_ACTIVE)
				if(Server.config.isDebug()) Log.addToLog("Echec du deplacement: le combat n'est pas en cours");
			return false;
		}
		
		ArrayList<Fighter> tacle = Pathfinding.getEnemyFighterArround(f.get_fightCell(false).getID(), _map, Fight.this);
		if(tacle != null && !f.isState(6))//Tentative de Tacle : Si stabilisation alors pas de tacle possible
		{
			for(Fighter T : tacle)//Les stabilisés ne taclent pas
			{ 
				if(T.isState(6)) 
				{ 
					tacle.remove(T); 
				} 
			}
			if(!tacle.isEmpty())//Si tous les tacleur ne sont pas stabilisés
			{
				if(Server.config.isDebug()) Log.addToLog("Le personnage est a cote de ("+tacle.size()+") ennemi(s)");// ("+tacle.getPacketsName()+","+tacle.get_fightCell().getID()+") => Tentative de tacle:");
				int chance = Formulas.getTacleChance(f, tacle);
				int rand = Formulas.getRandomValue(0, 99);
				if(rand > chance)
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7,GA._id, "104",_ordreJeu.get(_curPlayer).getGUID()+";", "");//Joueur taclé
					int pertePA = get_curFighterPA()*chance/100;
					
					if(pertePA  < 0)pertePA = -pertePA;
					if(get_curFighterPM() < 0)set_curFighterPM(0); // -_curFighterPM :: 0 c'est plus simple :)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7,GA._id,"129", f.getGUID()+"", f.getGUID()+",-"+get_curFighterPM());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7,GA._id,"102", f.getGUID()+"", f.getGUID()+",-"+pertePA);
					
					set_curFighterPM(0);
					set_curFighterPA(get_curFighterPA() - pertePA);
					if(Server.config.isDebug()) Log.addToLog("Echec du deplacement: fighter tacle");
					return false;
				}
			}
		}
		
		//*
		AtomicReference<String> pathRef = new AtomicReference<String>(path);
		int nStep = Pathfinding.isValidPath(_map, f.get_fightCell(false).getID(), pathRef, Fight.this);
		String newPath = pathRef.get();
		if( nStep > get_curFighterPM() || nStep == -1000)
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID()+" a demander un chemin inaccessible ou trop loin");
			return false;
		}
		
		set_curFighterPM(get_curFighterPM() - nStep);
		_curFighterUsedPM += nStep;
		
		int nextCellID = CryptManager.cellCode_To_ID(newPath.substring(newPath.length() - 2));
		//les monstres n'ont pas de GAS//GAF
		if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(Fight.this,7,_ordreJeu.get(_curPlayer).getGUID());
		//else
			//SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(Fight.this,7,_ordreJeu.get(_curPlayer).getGUID());
        //Si le joueur n'est pas invisible
        if(!_ordreJeu.get(_curPlayer).isHide()) {
	        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, GA._id, "1", _ordreJeu.get(_curPlayer).getGUID()+"", "a"+CryptManager.cellID_To_Code(f.get_fightCell(false).getID())+newPath);
        } else//Si le joueur est planqué x)
        {
        	if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
        	{
        		//On envoie le path qu'au joueur qui se déplace
        		GameClient out = _ordreJeu.get(_curPlayer).getPersonnage().get_compte().getGameClient();
        		SocketManager.GAME_SEND_GA_PACKET(out,  GA._id+"", "1", _ordreJeu.get(_curPlayer).getGUID()+"", "a"+CryptManager.cellID_To_Code(f.get_fightCell(false).getID())+newPath);
        	}
        }
       
        //Si porté
        Fighter po = _ordreJeu.get(_curPlayer).get_holdedBy();
        if(po != null
        && _ordreJeu.get(_curPlayer).isState(Constants.ETAT_PORTE)
        && po.isState(Constants.ETAT_PORTEUR))
        {
        	Console.instance.println("Porteur: "+po.getPacketsName());
        	Console.instance.println("NextCellID "+nextCellID);
        	Console.instance.println("Cell du Porteur "+po.get_fightCell(false).getID());
        	
        	//si le joueur va bouger
       		if(nextCellID != po.get_fightCell(false).getID())
       		{
       			//on retire les états
       			po.setState(Constants.ETAT_PORTEUR, 0);
       			_ordreJeu.get(_curPlayer).setState(Constants.ETAT_PORTE,0);
       			//on retire dé lie les 2 fighters
       			po.set_isHolding(null);
       			_ordreJeu.get(_curPlayer).set_holdedBy(null);
       			//La nouvelle case sera définie plus tard dans le code
       			//On envoie les packets
       			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 950, po.getGUID()+"", po.getGUID()+","+Constants.ETAT_PORTEUR+",0");
    			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 950, _ordreJeu.get(_curPlayer).getGUID()+"", _ordreJeu.get(_curPlayer).getGUID()+","+Constants.ETAT_PORTE+",0");
       		}
      	}
        
		_ordreJeu.get(_curPlayer).get_fightCell(false).getFighters().clear();
		if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Fighter ID= "+f.getGUID()+" se deplace de la case "+_ordreJeu.get(_curPlayer).get_fightCell(false).getID()+" vers "+CryptManager.cellCode_To_ID(newPath.substring(newPath.length() - 2)));
        _ordreJeu.get(_curPlayer).set_fightCell(_map.getCase(nextCellID));
        _ordreJeu.get(_curPlayer).get_fightCell(false).addFighter(_ordreJeu.get(_curPlayer));
        if(po != null) po.get_fightCell(false).addFighter(po);// même erreur que tantôt, bug ou plus de fighter sur la case
       if(nStep < 0) 
       {
    	   if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Fighter ID= "+f.getGUID()+" nStep negatives, reconversion");
    	   nStep = nStep*(-1);
       }
        set_curAction("GA;129;"+_ordreJeu.get(_curPlayer).getGUID()+";"+_ordreJeu.get(_curPlayer).getGUID()+",-"+nStep);
        
        //Si porteur
        po = _ordreJeu.get(_curPlayer).get_isHolding();
        if(po != null
        && _ordreJeu.get(_curPlayer).isState(Constants.ETAT_PORTEUR)
        && po.isState(Constants.ETAT_PORTE))
        {
       		//on déplace le porté sur la case
        	po.set_fightCell(_ordreJeu.get(_curPlayer).get_fightCell(false));
        	if(Server.config.isDebug()) Log.addToLog(po.getPacketsName()+" se deplace vers la case "+nextCellID);
      	}
        if(f.getPersonnage() == null)
        {
        	try {
    			Thread.sleep(900+300*nStep);//Estimation de la durée du déplacement
    		} catch (InterruptedException e) {};
        	SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(Fight.this,7,get_curAction());
        	
    		set_curAction("");
    		ArrayList<Piege> P = new ArrayList<Piege>();
    		P.addAll(_traps);
    		for(Piege p : P)
    		{
    			Fighter F = _ordreJeu.get(_curPlayer);
    			int dist = Pathfinding.getDistanceBetween(_map,p.get_cell().getID(),F.get_fightCell(false).getID());
    			//on active le piege
    			if(dist <= p.get_size())p.onTraped(F);
    		}
    		return true;
        }
        //*/
        f.getPersonnage().get_compte().getGameClient().addAction(GA);
        return true;
    }

	public void onGK(Personnage perso) //TODO AAHH
	{
		if(get_curAction().equals("")|| _ordreJeu.get(_curPlayer).getGUID() != perso.get_GUID() || _state!= Constants.FIGHT_STATE_ACTIVE)return;
		if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+")Fin du deplacement de Fighter ID= "+perso.get_GUID());
		SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(Fight.this,7,get_curAction());
		SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(Fight.this,7,2,_ordreJeu.get(_curPlayer).getGUID());
		//copie
		ArrayList<Piege> P = (new ArrayList<Piege>());
		P.addAll(_traps);
		for(Piege p : P)
		{
			Fighter F = getFighterByPerso(perso);
			int dist = Pathfinding.getDistanceBetween(_map,p.get_cell().getID(),F.get_fightCell(false).getID());
			//on active le piege
			if(dist <= p.get_size())p.onTraped(F);
			if(_state == Constants.FIGHT_STATE_FINISHED)break;
		}
		set_curAction("");
		if(!this.get_turnTimer().isRunning())
			endTurn();
	}

	public void playerPass(Personnage _perso)
	{
		Fighter f = getFighterByPerso(_perso);
		if(f == null)return;
		if(!f.canPlay())return;
		if(!get_curAction().equals("")) return;//TODO
		endTurn();
	}

	public int tryCastSpell(Fighter fighter,SortStats Spell, int caseID)
	{
		if(!get_curAction().equals(""))return 10;
		if(Spell == null)return 10;
		
		Case Cell = _map.getCase(caseID);
		set_curAction("casting");
		
		if(CanCastSpell(fighter,Spell,Cell, -1))
		{
			if(fighter.getPersonnage() != null)
				SocketManager.GAME_SEND_STATS_PACKET(fighter.getPersonnage());
			
			if(Server.config.isDebug()) Log.addToLog(fighter.getPacketsName()+" tentative de lancer le sort "+Spell.getSpellID()+" sur la case "+caseID);
			set_curFighterPA(get_curFighterPA() - Spell.getPACost());
			_curFighterUsedPA += Spell.getPACost();
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(Fight.this, 7, fighter.getGUID());
			boolean isEc = Spell.getTauxEC() != 0 && Formulas.getRandomValue(1, Spell.getTauxEC()) == Spell.getTauxEC();
			if(isEc)
			{
				if(Server.config.isDebug()) Log.addToLog(fighter.getPacketsName()+" Echec critique sur le sort "+Spell.getSpellID());
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 302, fighter.getGUID()+"", Spell.getSpellID()+"");
			}else
			{
				boolean isCC = fighter.testIfCC(Spell.getTauxCC());
				String sort = Spell.getSpellID()+","+caseID+","+Spell.getSpriteID()+","+Spell.getLevel()+","+Spell.getSpriteInfos();
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 300, fighter.getGUID()+"", sort);	
				if(isCC)
				{
					if(Server.config.isDebug()) Log.addToLog(fighter.getPacketsName()+" Coup critique sur le sort "+Spell.getSpellID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 301, fighter.getGUID()+"", sort);
				}
				//Si le joueur est invi, on montre la case
				if(fighter.isHide()) {
					fighter.setFakeCell(fighter.get_fightCell(false));
					showCaseToAll(fighter.getGUID(), fighter.get_fightCell(false).getID());
				}
				//on applique les effets de l'arme
				Spell.applySpellEffectToFight(Fight.this,fighter,Cell,isCC);
				
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 102,fighter.getGUID()+"",fighter.getGUID()+",-"+Spell.getPACost());
			SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(Fight.this, 7, 0, fighter.getGUID());
			//Refresh des Stats
			//refreshCurPlayerInfos();
			fighter.addLaunchedSort(Cell.getFirstFighter(),Spell);
			
			if((isEc && Spell.isEcEndTurn()))
			{
				set_curAction("");
				if(fighter.getMob() != null || fighter.isInvocation())//Mob, Invoque
				{
					return 5;
				}else
				{
					endTurn();
					return 5;
				}
			} else if (fighter.getMob() != null) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			verifIfTeamAllDead();
		}else if (fighter.getMob() != null || fighter.isInvocation())
		{
			return 10;
		}
		set_curAction("");
		return 0;
	}

	public boolean CanCastSpell(Fighter fighter, SortStats spell, Case cell, int launchCase)
	{
		int ValidlaunchCase;
		if(launchCase <= -1)
		{
			ValidlaunchCase = fighter.get_fightCell(false).getID();
		}else
		{
			ValidlaunchCase = launchCase;
		}
		
		Fighter f = _ordreJeu.get(_curPlayer);
		Personnage perso = fighter.getPersonnage();
		//Si le sort n'est pas existant
		if(spell == null)
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Sort non existant");
			if(perso != null)
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1169");
			}
			return false;
		}
		//Si ce n'est pas au joueur de jouer
		if (f == null || f.getGUID() != fighter.getGUID()) 
		{
			if(Server.config.isDebug()) Log.addToLog("Ce n'est pas au joueur. Doit jouer :("+f.getGUID()+"). Fautif :("+fighter.getGUID()+")");
			if(perso != null)
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1175");
			}
			return false;	
		}
		//Si le joueur n'a pas assez de PA
		if(get_curFighterPA() < spell.getPACost())
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Le joueur n'a pas assez de PA ("+get_curFighterPA()+"/"+spell.getPACost()+")");
			if(perso != null)
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1170;" + get_curFighterPA() + "~" + spell.getPACost());
			}
			return false;
		}
		//Si la cellule visée n'existe pas
		if(cell == null)
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") La cellule visee n'existe pas");
			if(perso != null)
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1172");
			}
			return false;
		}
		//Si la cellule visée n'est pas alignée avec le joueur alors que le sort le demande
		if(spell.isLineLaunch() && !Pathfinding.casesAreInSameLine(_map, ValidlaunchCase, cell.getID(), 'z'))
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Le sort demande un lancer en ligne, or la case n'est pas alignee avec le joueur");
			if(perso != null)
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1173");
			}
			return false;
		}
		//Si le sort demande une ligne de vue et que la case demandée n'en fait pas partie
		if(spell.hasLDV() && !Pathfinding.checkLoS(_map, ValidlaunchCase, cell.getID(), fighter))
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") Le sort demande une ligne de vue, mais la case visee n'est pas visible pour le joueur");
			if(perso != null)
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1174");
			}
			return false;
		}
		
		int dist = Pathfinding.getDistanceBetween(_map, ValidlaunchCase, cell.getID());
		int MaxPO = spell.getMaxPO();
		if(spell.isModifPO()) {
			MaxPO += fighter.getTotalStats().getEffect(Constants.STATS_ADD_PO);
			MaxPO = MaxPO <= 0 ? 1 : MaxPO;
		}
		//Vérification Portée mini / maxi
		if(dist < spell.getMinPO() || dist > MaxPO)
		{
			if(Server.config.isDebug()) Log.addToLog("("+_curPlayer+") La case est trop proche ou trop eloignee Min: "+spell.getMinPO()+" Max: "+spell.getMaxPO()+" Dist: "+dist);
			if(perso != null)
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1171;" + spell.getMinPO() + "~" + spell.getMaxPO() + "~" + dist);
			}
			return false;
		}
		//vérification cooldown
		if(!LaunchedSort.coolDownGood(fighter,spell.getSpellID()))
		{
			return false;
		}
		//vérification nombre de lancer par tour
		int nbLancer = spell.getMaxLaunchbyTurn();
		if(nbLancer - LaunchedSort.getNbLaunch(fighter, spell.getSpellID()) <= 0 && nbLancer > 0)
		{
			return false;
		}
		//vérification nombre de lancer par cible
		Fighter target = cell.getFirstFighter();
		int nbLancerT = spell.getMaxLaunchbyByTarget();
		if(nbLancerT - LaunchedSort.getNbLaunchTarget(fighter, target, spell.getSpellID()) <= 0 && nbLancerT > 0)
		{
			return false;
		}
		return true;
	}
	
	public String GetGE(int win)
    {
		long time = System.currentTimeMillis() - _startTime;
		int initGUID = _init0.getGUID();
		
		int type = Constants.FIGHT_TYPE_CHALLENGE;// toujours 0
		if(_type == Constants.FIGHT_TYPE_AGRESSION)//Sauf si gain d'honneur
			type = _type;
		
		StringBuilder Packet = new StringBuilder();
        Packet.append("GE").append(time).append("|").append(initGUID).append("|").append(type).append("|");
        ArrayList<Fighter> TEAM1 = new ArrayList<Fighter>();
        ArrayList<Fighter> TEAM2 = new ArrayList<Fighter>();
        if(win == 1)
        {
        	TEAM1.addAll(_team0.values());
        	TEAM2.addAll(_team1.values());
        }
        else
        {
        	TEAM1.addAll(_team1.values());
        	TEAM2.addAll(_team0.values());
        }
        //Traque
        Personnage curp = null; 
        for(Fighter F : TEAM1)
        {
        	if(F.isInvocation())continue;
        	if(TEAM1.size() == 1) curp = F.getPersonnage();
        }
        for(Fighter F : TEAM2)
        {
        	if(F.isInvocation())continue;
        	if(curp != null && curp.get_traque() != null && curp.get_traque().get_traqued() == F.getPersonnage())
        	{ 
        		SocketManager.GAME_SEND_MESSAGE(curp, "Thomas Sacre : Contrat fini, reviens me voir pour recuperer ta recompense.", "000000"); 
        		curp.get_traque().set_traqued(null); 
        		curp.get_traque().set_time(-2); 
        	} 
        }
        //fin
        /* DEBUG
        Console.instance.println("TEAM1: lvl="+TEAM1lvl);
        Console.instance.println("TEAM2: lvl="+TEAM2lvl);
        //*/
        //DROP SYSTEM
        	//Calcul de la PP de groupe
	        int groupPP = 0,minkamas = 0,maxkamas = 0;
	        for(Fighter F : TEAM1)if(!F.isInvocation() || (F.getMob() != null && F.getMob().getTemplate().getID() ==258))groupPP += F.getTotalStats().getEffect(Constants.STATS_ADD_PROS);
	        if(groupPP <0)groupPP =0;
        	//Calcul des drops possibles
	        ArrayList<Drop> possibleDrops = new ArrayList<Drop>();
	        for(Fighter F : TEAM2)
	        {
	        	if(F.isInvocation() || F.getMob() == null)continue;
	        	minkamas += F.getMob().getTemplate().getMinKamas();
	        	maxkamas += F.getMob().getTemplate().getMaxKamas();
	        	for(Drop D : F.getMob().getDrops())
	        	{
	        		if(D.getMinProsp() <= groupPP)
	        		{
	        			//On augmente le taux en fonction de la PP
	        			int taux = (int)((groupPP * D.get_taux()*Server.config.getRateDrop())/100);
	        			possibleDrops.add(new Drop(D.get_itemID(),0,taux,D.get_max()));
	        		}
	        	}
	        }
	        //On Réordonne la liste en fonction de la PP
	        ArrayList<Fighter> Temp = new ArrayList<Fighter>();
	        Fighter curMax = null;
	        while(Temp.size() < TEAM1.size())
	        {
	        	int curPP = -1;
		        for(Fighter F : TEAM1)
		        {
	        		//S'il a plus de PP et qu'il n'est pas listé
		        	if(F.getTotalStats().getEffect(Constants.STATS_ADD_PROS) > curPP && !Temp.contains(F))
		        	{
		        		curMax = F;
		        		curPP = F.getTotalStats().getEffect(Constants.STATS_ADD_PROS);
		        	}
		        }
	        	Temp.add(curMax);
	        }
	        //On enleve les invocs
	        TEAM1.clear();
	        TEAM1.addAll(Temp);
	        /* DEBUG
	        Console.instance.println("DROP: PP ="+groupPP);
	        Console.instance.println("DROP: nbr="+possibleDrops.size());
	        Console.instance.println("DROP: Kam="+totalkamas);
	        //*/
	    //FIN DROP SYSTEM
	    //XP SYSTEM
	        long totalXP = 0;
	        for(Fighter F : TEAM2)
	        {
	        	if(F.isInvocation() || F.getMob() == null)continue;
	        	totalXP += F.getMob().getBaseXp();
	        }
	        /* DEBUG
	        Console.instance.println("TEAM1: xpTotal="+totalXP);
	        //*/
	    //FIN XP SYSTEM
		//Capture d'âmes
	        boolean mobCapturable = true;
	        for(Fighter F : TEAM2)
	        {
	        	try
	        	{
	        		mobCapturable &= F.getMob().getTemplate().isCapturable();
	        	}catch (Exception e) {
					mobCapturable = false;
					break;
				}
	        }
	        isCapturable |= mobCapturable;
	        
	        if(isCapturable)
	        {
		        boolean isFirst = true;
		        int maxLvl = 0;
		        String pierreStats = "";

		        
		        for(Fighter F : TEAM2)	//Création de la pierre et verifie si le groupe peut être capturé
		        {
		        	if(!isFirst)
		        		pierreStats += "|";
		        	
		        	pierreStats += F.getMob().getTemplate().getID() + "," + F.get_lvl();//Converti l'ID du monstre en Hex et l'ajoute au stats de la futur pierre d'âme
		        	
		        	isFirst = false;
		        	
		        	if(F.get_lvl() > maxLvl)	//Trouve le monstre au plus haut lvl du groupe (pour la puissance de la pierre)
		        		maxLvl = F.get_lvl();
		        }
		        pierrePleine = new PierreAme(World.data.getNewItemGuid(),1,7010,Constants.ITEM_POS_NO_EQUIPED,pierreStats);	//Crée la pierre d'âme
		        
		        for(Fighter F : TEAM1)	//Récupère les captureur
		        {
		        	if(!F.isInvocation() && F.isState(Constants.ETAT_CAPT_AME))
		        	{
		        		_captureur.add(F);
		        	}
		        }
		        if(_captureur.size() > 0 && !World.data.isArenaMap(get_map().get_id()))	//S'il y a des captureurs
	    		{
	    			for (int i = 0; i < _captureur.size(); i++)
	    			{
	    				try
	    				{
			        		Fighter f = _captureur.get(Formulas.getRandomValue(0, _captureur.size()-1));	//Récupère un captureur au hasard dans la liste
			        		if(!(f.getPersonnage().getObjetByPos(Constants.ITEM_POS_ARME).getTemplate().getType() == Constants.ITEM_TYPE_PIERRE_AME))
		    				{
			    				_captureur.remove(f);
		    					continue;
		    				}
			    			Couple<Integer,Integer> pierreJoueur = Formulas.decompPierreAme(f.getPersonnage().getObjetByPos(Constants.ITEM_POS_ARME));//Récupère les stats de la pierre équippé
			    			
			    			if(pierreJoueur.second < maxLvl)	//Si la pierre est trop faible
			    			{
			    				_captureur.remove(f);
		    					continue;
		    				}
			    			
			    			int captChance = Formulas.totalCaptChance(pierreJoueur.first, f.getPersonnage());
			    			
			    			if(Formulas.getRandomValue(1, 100) <= captChance)	//Si le joueur obtiens la capture
			    			{
			    				//Retire la pierre vide au personnage et lui envoie ce changement
			    				int pierreVide = f.getPersonnage().getObjetByPos(Constants.ITEM_POS_ARME).getGuid();
			    				f.getPersonnage().deleteItem(pierreVide);
			    				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(f.getPersonnage(), pierreVide);
			    				
			    				captWinner = f._id;
			    				break;
			    			}
		    			}
	    				catch(NullPointerException e)
	    				{
	    					continue;
	    				}
	    			}
	    		}
	        }
	    //Fin Capture
	    for(Fighter i : TEAM1)
		{
	    	if(i.hasLeft()) continue;//Si il abandonne, il ne gagne pas d'xp
	    	if(i._double != null)continue;//Pas de double dans les gains
        	if(type == Constants.FIGHT_TYPE_CHALLENGE)
        	{
        		if(i.isInvocation() && i.getMob() != null && i.getMob().getTemplate().getID() != 258)continue;
        		long winxp 	= Formulas.getXpWinPvm2(i,TEAM1,TEAM2,totalXP);
        		AtomicReference<Long> XP = new AtomicReference<Long>();
        		XP.set(winxp);
        		
        		long guildxp = Formulas.getGuildXpWin(i,XP);
        		long mountxp = 0;

        		if(i.getPersonnage() != null && i.getPersonnage().isOnMount())
        		{
        			mountxp = Formulas.getMountXpWin(i,XP);
        			i.getPersonnage().getMount().addXp(mountxp);
        			SocketManager.GAME_SEND_Re_PACKET(i.getPersonnage(),"+",i.getPersonnage().getMount());
        		}
        		int winKamas	= Formulas.getKamasWin(i,TEAM1,minkamas,maxkamas);
        		String drops = "";
        		//Drop system
        		ArrayList<Drop> temp = new ArrayList<Drop>();
        		temp.addAll(possibleDrops);
        		Map<Integer,Integer> itemWon = new TreeMap<Integer,Integer>();
        		
        		for(Drop D : temp)
        		{
        			int t = (int)(D.get_taux()*100);//Permet de gerer des taux>0.01
        			int jet = Formulas.getRandomValue(0, 100*100);
        			if(jet < t)
        			{
        				ObjTemplate OT = World.data.getObjTemplate(D.get_itemID());
        				if(OT == null)continue;
        				//on ajoute a la liste
        				itemWon.put(OT.getID(),(itemWon.get(OT.getID())==null?0:itemWon.get(OT.getID()))+1);
        				
        				D.setMax(D.get_max()-1);
        				if(D.get_max() == 0)possibleDrops.remove(D);
        			}
        		}
        		if(i._id == captWinner && pierrePleine != null)	//S'il à capturé le groupe
        		{
        			if(drops.length() >0)drops += ",";
        			drops += pierrePleine.getTemplate().getID()+"~"+1;
        			if(i.getPersonnage().addObjet(pierrePleine, false))
        				World.data.addObjet(pierrePleine, true);
        		}
        		for(Entry<Integer,Integer> entry : itemWon.entrySet())
        		{
        			ObjTemplate OT = World.data.getObjTemplate(entry.getKey());
        			if(OT == null)continue;
        			if(drops.length() >0)drops += ",";
        			drops += entry.getKey()+"~"+entry.getValue();
        			Objet obj = OT.createNewItem(entry.getValue(), false);
        			if(i.getPersonnage().addObjet(obj, true))
        				World.data.addObjet(obj, true);
        		}
        		//fin drop system
        		winxp = XP.get();
        		if(winxp != 0 && i.getPersonnage() != null)
        			i.getPersonnage().addXp(winxp);
        		if(winKamas != 0 && i.getPersonnage() != null)
        			i.getPersonnage().addKamas(winKamas);
        		if(guildxp > 0 && i.getPersonnage().getGuildMember() != null)
        			i.getPersonnage().getGuildMember().giveXpToGuild(guildxp);

        		Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";").append((i.isDead() ?  "1" : "0" )).append(";");
        		Packet.append(i.xpString(";")).append(";");
        		Packet.append((winxp == 0?"":winxp)).append(";");
        		Packet.append((guildxp == 0?"":guildxp)).append(";");
        		Packet.append((mountxp == 0?"":mountxp)).append(";");
        		Packet.append(drops).append(";");//Drop
        		Packet.append((winKamas == 0?"":winKamas)).append("|");
        	}else
        	{
        		if(i.getPersonnage() == null)
        			continue;
        		// Si c'est un neutre, on ne gagne pas de points
        		int winH = 0;
        		int winD = 0;
        		if(type == Constants.FIGHT_TYPE_AGRESSION)
        		{
	        		if(_init1.getPersonnage().get_align() != 0 && _init0.getPersonnage().get_align() != 0)
	    			{
	        			if(_init1.getPersonnage().get_compte().get_curIP().compareTo(_init0.getPersonnage().get_compte().get_curIP()) != 0 || Server.config.isMulePvp())
	        			{
	            			winH = Formulas.calculHonorWin(TEAM1,TEAM2,i);
	        			}
	        			if(i.getPersonnage().getDeshonor() > 0) winD = -1;
	    			}
        		}
        		Personnage P = i.getPersonnage();
        		if(P.get_honor()+winH<0)winH = -P.get_honor();
        		P.addHonor(winH);
        		P.setDeshonor(P.getDeshonor()+winD);
        		Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";").append((i.isDead() ?  "1" : "0" )).append(";");
        		Packet.append((P.get_align()!=Constants.ALIGNEMENT_NEUTRE?World.data.getExpLevel(P.getGrade()).pvp:0)).append(";");
        		Packet.append(P.get_honor()).append(";");
        		int maxHonor = World.data.getExpLevel(P.getGrade()+1).pvp;
        		if(maxHonor == -1)maxHonor = World.data.getExpLevel(P.getGrade()).pvp;
        		Packet.append((P.get_align()!=Constants.ALIGNEMENT_NEUTRE?maxHonor:0)).append(";");
        		Packet.append(winH).append(";");
        		Packet.append(P.getGrade()).append(";");
        		Packet.append(P.getDeshonor()).append(";");
        		Packet.append(winD);
        		Packet.append(";;0;0;0;0;0|");
        		
        	/**	if(_type != Constants.FIGHT_TYPE_CHALLENGE) { What the fuck baby ? Why here *-*
        			final Fighter F = i;
        			
        			if(F.hasLeft())continue;
					if(F.getPersonnage() == null)continue;
					if(F.isInvocation())continue;
					if(!F.getPersonnage().isOnline())continue;
					
					F.getPersonnage().getWaiter().addNext(new Runnable() {
						public void run() {
							int EnergyLoos = Formulas.getLoosEnergy(F.get_lvl(), _type==1, _type==5);
							int Energy = F.getPersonnage().get_energy() - EnergyLoos;
							if(Energy < 0) Energy = 0;
							F.getPersonnage().set_energy(Energy);
							if(Energy == 0) {
								F.getPersonnage().set_PDV(1);
								F.getPersonnage().set_Ghosts();
							} else {
								F.getPersonnage().warpToSavePos();
								F.getPersonnage().set_PDV(1);
							}
							if(F.getPersonnage().isOnline())
								SocketManager.GAME_SEND_Im_PACKET(F.getPersonnage(), "034;"+EnergyLoos);
							F.getPersonnage().refreshMapAfterFight();
						}
					}, 1000);
				} **/
        	}
		}
		for(Fighter i : TEAM2)
		{
			if(i._double != null)continue;//Pas de double dans les gains
			if(i.isInvocation() && i.getMob().getTemplate().getID() != 285)continue;//On affiche pas les invocs
			if(_type != Constants.FIGHT_TYPE_AGRESSION)
			{
				if(i.getPDV() == 0 || i.hasLeft())
				{
					Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";1").append(";").append(i.xpString(";")).append(";;;;|");
				}else
				{
					Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";0").append(";").append(i.xpString(";")).append(";;;;|");
				}
			}else
        	{
				if(i.getPersonnage() == null)
					continue;
        		// Si c'est un neutre, on ne gagne pas de points
        		int winH = 0;
        		int winD = 0;
        		if(_init1.getPersonnage().get_align() != 0 && _init0.getPersonnage().get_align() != 0)
    			{
        			if(_init1.getPersonnage().get_compte().get_curIP().compareTo(_init0.getPersonnage().get_compte().get_curIP()) != 0 || Server.config.isMulePvp())
            		{
            			winH = Formulas.calculHonorWin(TEAM1,TEAM2,i);
        			}
    			}
        		
        		Personnage P = i.getPersonnage();
        		if(P.get_honor()+winH<0)winH = -P.get_honor();
        		P.addHonor(winH);
        		if(P.getDeshonor()-winD<0) winD = 0;
        		P.setDeshonor(P.getDeshonor()-winD);
        		Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";").append((i.isDead() ?  "1" : "0" )).append(";");
        		Packet.append((P.get_align()!=Constants.ALIGNEMENT_NEUTRE?World.data.getExpLevel(P.getGrade()).pvp:0)).append(";");
        		Packet.append(P.get_honor()).append(";");
        		int maxHonor = World.data.getExpLevel(P.getGrade()+1).pvp;
        		if(maxHonor == -1)maxHonor = World.data.getExpLevel(P.getGrade()).pvp;
        		Packet.append((P.get_align()!=Constants.ALIGNEMENT_NEUTRE?maxHonor:0)).append(";");
        		Packet.append(winH).append(";");
        		Packet.append(P.getGrade()).append(";");
        		Packet.append(P.getDeshonor()).append(";");
        		Packet.append(winD);
        		Packet.append(";;0;0;0;0;0|");
        	}
		}
		if(Percepteur.GetPercoByMapID(_map.get_id()) != null && _type == 4)//On a un percepteur ONLY PVM ?
		{
			Percepteur p = Percepteur.GetPercoByMapID(_map.get_id());
			long winxp 	= (int)Math.floor(Formulas.getXpWinPerco(p,TEAM1,TEAM2,totalXP)/100);
			long winkamas 	= (int)Math.floor(Formulas.getKamasWinPerco(minkamas,maxkamas)/100);
			p.setXp(p.getXp()+winxp);
			p.setKamas(p.getKamas()+winkamas);
			Packet.append("5;").append(p.getGuid()).append(";").append(p.get_N1()).append(",").append(p.get_N2()).append(";").append(World.data.getGuild(p.get_guildID()).get_lvl()).append(";0;");
			Guild G = World.data.getGuild(p.get_guildID());
			Packet.append(G.get_lvl()).append(";");
			Packet.append(G.get_xp()).append(";");
			Packet.append(World.data.getGuildXpMax(G.get_lvl())).append(";");
			Packet.append(";");//XpGagner
			Packet.append(winxp).append(";");//XpGuilde
			Packet.append(";");//Monture
			
			String drops = "";
    		ArrayList<Drop> temp = new ArrayList<Drop>();
    		temp.addAll(possibleDrops);
    		Map<Integer,Integer> itemWon = new TreeMap<Integer,Integer>();
    		
    		for(Drop D : temp)
    		{
    			int t = (int)(D.get_taux()*100);//Permet de gerer des taux>0.01
    			int jet = Formulas.getRandomValue(0, 100*100);
    			if(jet < t)
    			{
    				ObjTemplate OT = World.data.getObjTemplate(D.get_itemID());
    				if(OT == null)continue;
    				//on ajoute a la liste
    				itemWon.put(OT.getID(),(itemWon.get(OT.getID())==null?0:itemWon.get(OT.getID()))+1);
    				
    				D.setMax(D.get_max()-1);
    				if(D.get_max() == 0)possibleDrops.remove(D);
    			}
    		}
    		for(Entry<Integer,Integer> entry : itemWon.entrySet())
    		{
    			ObjTemplate OT = World.data.getObjTemplate(entry.getKey());
    			if(OT == null)continue;
    			if(drops.length() >0)drops += ",";
    			drops += entry.getKey()+"~"+entry.getValue();
    			Objet obj = OT.createNewItem(entry.getValue(), false);
    			p.addObjet(obj);
    			World.data.addObjet(obj, true);
    		}
    		Packet.append(drops).append(";");//Drop
    		Packet.append(winkamas).append("|");
			
			World.database.getCollectorData().update(p);
		}
        return Packet.toString();
    }
    
	public synchronized void verifIfTeamAllDead()
	{
		if(_state >=Constants.FIGHT_STATE_FINISHED)return;
		boolean team0 = true;
		boolean team1 = true;
		for(Entry<Integer,Fighter> entry : _team0.entrySet())
		{
			if(entry.getValue().isInvocation())continue;
			if(!entry.getValue().isDead())
			{
				team0 = false;
				break;
			}
		}
		for(Entry<Integer,Fighter> entry : _team1.entrySet())
		{
			if(entry.getValue().isInvocation())continue;
			if(!entry.getValue().isDead())
			{
				team1 = false;
				break;
			}
		}
		if(team0 || team1 || !verifyStillInFight())
		{
			_state = Constants.FIGHT_STATE_FINISHED;
			int winner = team0?2:1;
			if(Server.config.isDebug()) Log.addToLog("L'equipe "+winner+" gagne !");

			get_turnTimer().stop();
			//On despawn tous le monde
			_curPlayer = -1;
			for(Entry<Integer, Fighter> entry : _team0.entrySet())
			{
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, entry.getValue().getGUID());
			}
			for(Entry<Integer, Fighter> entry : _team1.entrySet())
			{
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, entry.getValue().getGUID());
			}
			Fight.this._init0.getPersonnage().get_curCarte().removeFight(Fight.this._id);
			SocketManager.GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(Fight.this,7,winner);
			
			for(Entry<Integer, Fighter> entry : _team0.entrySet())//Team mob sauf en défie/aggro
			{
				Personnage perso = entry.getValue().getPersonnage();
				if(perso == null)continue;
				perso.set_duelID(-1);
				perso.set_ready(false);
				perso.set_fight(null);
			}
			switch(_type)//Team joueurs
			{
				case Constants.FIGHT_TYPE_CHALLENGE://Défie
				case Constants.FIGHT_TYPE_AGRESSION://Aggro
					for(Entry<Integer, Fighter> entry : _team1.entrySet())
					{
						Personnage perso = entry.getValue().getPersonnage();
						if(perso == null)continue;
						perso.set_duelID(-1);
						perso.set_ready(false);
						perso.set_fight(null);
					}
				break;
				case Constants.FIGHT_TYPE_PVM://PvM
					if(_team1.get(-1) == null)return;
				break;	
			}
			
			//on vire les spec du combat
			for(Personnage perso: _spec.values())
			{
				//on remet le perso sur la map
				perso.get_curCarte().addPlayer(perso);
				//SocketManager.GAME_SEND_GV_PACKET(perso);	//Mauvaise ligne apparemment
				perso.refreshMapAfterFight();
			}
			
			World.data.getCarte(_map.get_id()).removeFight(_id);
			SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.data.getCarte(_map.get_id()));
			_map = null;
			_ordreJeu = null;
			ArrayList<Fighter> winTeam = new ArrayList<Fighter>();
			ArrayList<Fighter> looseTeam = new ArrayList<Fighter>();
			if(team0)
			{
				looseTeam.addAll(_team0.values());
				winTeam.addAll(_team1.values());
			}
			else
			{
				winTeam.addAll(_team0.values());
				looseTeam.addAll(_team1.values());
			}
			
			final ArrayList<Fighter> fWinTeam = winTeam;
			final ArrayList<Fighter> fLooseTeam = looseTeam;
			getWaiter().addNext(new Runnable() {
				public void run() {
					//Pour les gagnants, on active les endFight actions
					for(final Fighter F : fWinTeam)
					{
						if(F._Perco != null)
						{
							//On actualise la guilde+Message d'attaque FIXME
							for(Personnage z : World.data.getGuild(_guildID).getMembers())
							{
								if(z == null) continue;
								if(z.isOnline())
								{
									SocketManager.GAME_SEND_gITM_PACKET(z, Percepteur.parsetoGuild(z.get_guild().get_id()));
									SocketManager.GAME_SEND_MESSAGE(z, "Votre percepteur remporte la victioire.", Server.config.getMotdColor());
								}
							}
							F._Perco.set_inFight((byte)0);
							F._Perco.set_inFightID((byte)-1);
							for(Personnage z : World.data.getCarte((short)F._Perco.get_mapID()).getPersos())
							{
								if(z == null) continue;
								SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(z.get_compte().getGameClient(), z.get_curCarte());
							}
						}
						if(F.hasLeft())continue;
						if(F.getPersonnage() == null)continue;
						if(F.isInvocation())continue;
						if(!F.getPersonnage().isOnline())continue;
						
						if(_type != Constants.FIGHT_TYPE_CHALLENGE)
						{
							if(F.getPDV() <= 0)
							{
								F.getPersonnage().set_PDV(1);
							}
						}
						F.getPersonnage().setNeedEndFightAction(true);
					/**	F.getPersonnage().getWaiter().addNext(new Runnable() {
							public void run() {
							//	if(_type != Constants.FIGHT_TYPE_CHALLENGE) F.getPersonnage().get_curCarte().applyEndFightAction(_type, F.getPersonnage());
								
								F.getPersonnage().refreshMapAfterFight();
							}
						}, 1000); **/
					}
					//Pour les perdant ont TP au point de sauvegarde
					for(final Fighter F : fLooseTeam)
					{
						if(F._Perco != null)
						{
							get_mapOld().RemoveNPC(F._Perco.getGuid());
							SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(get_mapOld(), F._Perco.getGuid());
							_perco.DelPerco(F._Perco.getGuid());
							World.database.getCollectorData().delete(F._Perco);
							//On actualise la guilde+Message d'attaque FIXME
							for(Personnage z : World.data.getGuild(_guildID).getMembers())
							{
								if(z == null) continue;
								if(z.isOnline())
								{
									SocketManager.GAME_SEND_gITM_PACKET(z, Percepteur.parsetoGuild(z.get_guild().get_id()));
									SocketManager.GAME_SEND_MESSAGE(z, "Votre percepteur est mort.", Server.config.getMotdColor());
								}
							}
						}
						if(F.hasLeft())continue;
						if(F.getPersonnage() == null)continue;
						if(F.isInvocation())continue;
						if(!F.getPersonnage().isOnline())continue;
						
						if(_type != Constants.FIGHT_TYPE_CHALLENGE)
						{
							F.getPersonnage().getWaiter().addNext(new Runnable() {
								public void run() {
									int EnergyLoos = Formulas.getLoosEnergy(F.get_lvl(), _type==1, _type==5);
									int Energy = F.getPersonnage().get_energy() - EnergyLoos;
									if(Energy < 0) Energy = 0;
									F.getPersonnage().set_energy(Energy);
									if(Energy == 0)
									{
										F.getPersonnage().set_PDV(1);
										F.getPersonnage().set_Ghosts();
									}else
									{
										F.getPersonnage().warpToSavePos();
										F.getPersonnage().set_PDV(1);
									}
									if(F.getPersonnage().isOnline())
										SocketManager.GAME_SEND_Im_PACKET(F.getPersonnage(), "034;"+EnergyLoos);
									F.getPersonnage().refreshMapAfterFight();
								}
							}, 1000);
							
						}
					} 
				}
			}, 1600);
		}
	}

	public void onFighterDie(Fighter target) 
	{ 
		target.setIsDead(true);
		if(!target.hasLeft()) deadList.put(target.getGUID(), target);//on ajoute le joueur à la liste des cadavres ;)
		SocketManager.GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(Fight.this,7,target.getGUID());
		target.get_fightCell(false).getFighters().clear();// Supprime tout causait bug si porté/porteur
		
		if(target.isState(Constants.ETAT_PORTEUR)) 
		{ 
			Fighter f = target.get_isHolding();
			f.set_fightCell(f.get_fightCell(false));
			f.get_fightCell(false).addFighter(f);//Le bug venait par manque de ceci, il ni avait plus de firstFighter
			f.setState(Constants.ETAT_PORTE, 0);//J'ajoute ceci quand même pour signaler qu'ils ne sont plus en état porté/porteur
			target.setState(Constants.ETAT_PORTEUR, 0);
			f.set_holdedBy(null);
			target.set_isHolding(null);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 950, f.getGUID()+"", f.getGUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 950, target.getGUID()+"", target.getGUID()+","+Constants.ETAT_PORTEUR+",0"); 
		}
		
		if(target.getTeam() == 0)
		{
			TreeMap<Integer,Fighter> team = new TreeMap<Integer,Fighter>();
			team.putAll(_team0);
			for(Entry<Integer,Fighter> entry : team.entrySet())
			{
				if(entry.getValue().getInvocator() == null)continue;
				if(entry.getValue().getPDV() == 0)continue;
				if(entry.getValue().isDead())continue;
				if(entry.getValue().getInvocator().getGUID() == target.getGUID())//si il a été invoqué par le joueur mort
				{
					onFighterDie(entry.getValue());
					
					int index = _ordreJeu.indexOf(entry.getValue());
					if(index != -1)_ordreJeu.remove(index);
					
					if(_team0.containsKey(entry.getValue().getGUID()))_team0.remove(entry.getValue().getGUID());
					else if (_team1.containsKey(entry.getValue().getGUID()))_team1.remove(entry.getValue().getGUID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 999, target.getGUID()+"", getGTL());
				}
			}
		}else if(target.getTeam() == 1)
		{
			TreeMap<Integer,Fighter> team = new TreeMap<Integer,Fighter>();
			team.putAll(_team1);
			for(Entry<Integer,Fighter> entry : team.entrySet())
			{
				if(entry.getValue().getInvocator() == null)continue;
				if(entry.getValue().getPDV() == 0)continue;
				if(entry.getValue().isDead())continue;
				if(entry.getValue().getInvocator().getGUID() == target.getGUID())//si il a été invoqué par le joueur mort
				{
					onFighterDie(entry.getValue());
					
					int index = _ordreJeu.indexOf(entry.getValue());
					if(index != -1)_ordreJeu.remove(index);
					
					if(_team0.containsKey(entry.getValue().getGUID()))_team0.remove(entry.getValue().getGUID());
					else if (_team1.containsKey(entry.getValue().getGUID()))_team1.remove(entry.getValue().getGUID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 999, target.getGUID()+"", getGTL());
              	}
			}
		}
		if(target.getMob() != null)
		{
			//Si c'est une invocation, on la retire de la liste
			try
			{
				boolean isStatic = false;
				for(int id : Constants.STATIC_INVOCATIONS)if(id == target.getMob().getTemplate().getID())isStatic = true;
				if(target.isInvocation() && !isStatic)
				{
					//Il ne peut plus jouer, et est mort on revient au joueur précedent pour que le startTurn passe au suivant
					if(!target.canPlay() && _ordreJeu.get(_curPlayer).getGUID() == target.getGUID())
					{
						_curPlayer--;
					}
					//Il peut jouer, et est mort alors on passe son tour pour que l'autre joue, puis on le supprime de l'index sans problèmes
					if(target.canPlay() && _ordreJeu.get(_curPlayer).getGUID() == target.getGUID())
					{
	    				endTurn();
					}
					
					//On ne peut pas supprimer l'index tant que le tour du prochain joueur n'est pas lancé
					int index = _ordreJeu.contains(target) ? _ordreJeu.indexOf(target) : -1; //TODO: to try john
					
					//Si le joueur courant a un index plus élevé, on le diminue pour éviter le outOfBound
					if(_curPlayer > index) _curPlayer--;
					
					if(index != -1)_ordreJeu.remove(index);
					
					
					if(_team0.containsKey(target.getGUID()))_team0.remove(target.getGUID());
					else if (_team1.containsKey(target.getGUID()))_team1.remove(target.getGUID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 999, target.getGUID()+"", getGTL());
				}
			}catch(Exception e){e.printStackTrace();};
		}
		//on supprime les glyphes du joueur
		ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();//Copie du tableau
		glyphs.addAll(_glyphs);
		for(Glyphe g : glyphs)
		{
			//Si c'est ce joueur qui l'a lancé
			if(g.get_caster().getGUID() == target.getGUID())
			{
				SocketManager.GAME_SEND_GDZ_PACKET_TO_FIGHT(Fight.this, 7, "-", g.get_cell().getID(), g.get_size(), 4);
				SocketManager.GAME_SEND_GDC_PACKET_TO_FIGHT(Fight.this, 7, g.get_cell().getID());
				_glyphs.remove(g);
			}
		}
		
		//on supprime les pieges du joueur
		ArrayList<Piege> Ps = new ArrayList<Piege>();
		Ps.addAll(_traps);
		for(Piege p : Ps)
		{
			if(p.get_caster().getGUID() == target.getGUID())
			{
				p.desappear();
				_traps.remove(p);
			}
		}
		verifIfTeamAllDead();
	}

	public int getTeamID(int guid)
	{
		if(_team0.containsKey(guid))
			return 1;
		if(_team1.containsKey(guid))
			return 2;
		if(_spec.containsKey(guid))
			return 4;
		return -1;
	}
	
	public int getOtherTeamID(int guid)
	{
		if(_team0.containsKey(guid))
			return 2;
		if(_team1.containsKey(guid))
			return 1;
		return -1;
	}

	public void tryCaC(Personnage perso, int cellID)
	{
		Fighter caster = getFighterByPerso(perso);
		
		if(caster == null)return;
		
		if(_ordreJeu.get(_curPlayer).getGUID() != caster.getGUID())//Si ce n'est pas a lui de jouer
			return;
		
		if(perso.getObjetByPos(Constants.ITEM_POS_ARME) == null)//S'il n'a pas de CaC
		{
			if(get_curFighterPA() < 4)//S'il n'a pas assez de PA
				return;
			
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(Fight.this, 7, perso.get_GUID());
			
			//Si le joueur est invisible
			if(caster.isHide())caster.unHide(-1);
			
			Fighter target = _map.getCase(cellID).getFirstFighter();
			
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 303, perso.get_GUID()+"", cellID+"");
			
			if(target != null)
			{
				int dmg = Formulas.getRandomJet("1d5+0");
				//et pour les item type feu eau agi ?
				int finalDommage = Formulas.calculFinalDommage(Fight.this,caster, target,Constants.ELEMENT_NEUTRE, dmg,false,true, -1);
				finalDommage = SpellEffect.applyOnHitBuffs(finalDommage,target,caster,Fight.this);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
			}
			set_curFighterPA(get_curFighterPA() - 4);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 102,perso.get_GUID()+"",perso.get_GUID()+",-4");
			SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(Fight.this, 7, 0, perso.get_GUID());
			
			if(target.getPDV() <=0)
				onFighterDie(target);
			verifIfTeamAllDead();
		}else
		{
			Objet arme = perso.getObjetByPos(Constants.ITEM_POS_ARME);
			
			//Pierre d'âmes = EC
			if(arme.getTemplate().getType() == 83)
			{
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 305, perso.get_GUID()+"", "");//Echec Critique Cac
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(Fight.this, 7, 0, perso.get_GUID());//Fin de l'action
				endTurn();
			
				return;
			}
			
			int PACost = arme.getTemplate().getPACost();
			
			if(get_curFighterPA() < PACost)//S'il n'a pas assez de PA
			{
				
				return;
			}
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(Fight.this, 7, perso.get_GUID());
			
			boolean isEc = arme.getTemplate().getTauxEC() != 0 && Formulas.getRandomValue(1, arme.getTemplate().getTauxEC()) == arme.getTemplate().getTauxEC();
			if(isEc)
			{
				if(Server.config.isDebug()) Log.addToLog(perso.get_name()+" Echec critique sur le CaC ");
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 305, perso.get_GUID()+"", "");//Echec Critique Cac
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(Fight.this, 7, 0, perso.get_GUID());//Fin de l'action
				endTurn();
			}else
			{
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 303, perso.get_GUID()+"", cellID+"");
				boolean isCC = caster.testIfCC(arme.getTemplate().getTauxCC());
				if(isCC)
				{
					if(Server.config.isDebug()) Log.addToLog(perso.get_name()+" Coup critique sur le CaC");
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 301, perso.get_GUID()+"", "0");
				}
				
				//Si le joueur est invisible
				if(caster.isHide())caster.unHide(-1);
				
				ArrayList<SpellEffect> effets = arme.getEffects();
				if(isCC)
				{
					effets = arme.getCritEffects();
				}
				for(SpellEffect SE : effets)
				{
					if(_state != Constants.FIGHT_STATE_ACTIVE)break;
					ArrayList<Fighter> cibles = Pathfinding.getCiblesByZoneByWeapon(Fight.this,arme.getTemplate().getType(),_map.getCase(cellID),caster.get_fightCell(false).getID());
					SE.setTurn(0);
					SE.applyToFight(Fight.this, caster, cibles, true);
				}
				set_curFighterPA(get_curFighterPA() - PACost);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 7, 102,perso.get_GUID()+"",perso.get_GUID()+",-"+PACost);
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(Fight.this, 7, 0, perso.get_GUID());
				verifIfTeamAllDead();
			}
		}
	}
	
	public Fighter getFighterByPerso(Personnage perso)
	{
		Fighter fighter = null;
		if(_team0.get(perso.get_GUID()) != null)
			fighter = _team0.get(perso.get_GUID());
		if(_team1.get(perso.get_GUID()) != null)
			fighter = _team1.get(perso.get_GUID());
		return fighter;
	}

	public Fighter getCurFighter()
	{
		return _ordreJeu.get(_curPlayer);
	}

	public void refreshCurPlayerInfos()
	{
		set_curFighterPA(_ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constants.STATS_ADD_PA) - _curFighterUsedPA);
		set_curFighterPM(_ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constants.STATS_ADD_PM) - _curFighterUsedPM);
	}

	public void leftFight(Personnage perso, Personnage target)
	{
		if(perso == null)return;
		Fighter F = Fight.this.getFighterByPerso(perso);
		Fighter T = null;
		if(target != null) T = Fight.this.getFighterByPerso(target);
		
		if(Server.config.isDebug())
		{
			if(target != null && T != null) 
			{
				Log.addToLog(perso.get_name()+" expulse "+T.getPersonnage().get_name());
			}else
			{
				Log.addToLog(perso.get_name()+" a quitter le combat");
			}
		}
		
		if(F != null)
		{
			
			switch(_type)
			{
				case Constants.FIGHT_TYPE_CHALLENGE://Défie
				case Constants.FIGHT_TYPE_AGRESSION://PVP
				case Constants.FIGHT_TYPE_PVM://PVM
				case Constants.FIGHT_TYPE_PVT://Perco
					if(_state >= Constants.FIGHT_STATE_ACTIVE)
					{
						onFighterDie(F);
						boolean StillInFight = false;
						if(_type == Constants.FIGHT_TYPE_CHALLENGE || _type == Constants.FIGHT_TYPE_AGRESSION || _type == Constants.FIGHT_TYPE_PVT)
						{
							StillInFight = verifyStillInFightTeam(F.getGUID());
						}else
						{
							StillInFight = verifyStillInFight();
						}
						
						if(!StillInFight)//S'arrête ici si il ne reste plus personne dans le combat et dans la team
						{
							//Met fin au combat
							verifIfTeamAllDead();
						}else
						{
							F.setLeft(true);
							SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, F.getGUID());
								
							Personnage P = F.getPersonnage();
							P.set_duelID(-1);
							P.set_ready(false);
							P.fullPDV();
							P.set_fight(null);
							P.setSitted(false);
							P.set_away(false);
							
							if(_type == Constants.FIGHT_TYPE_AGRESSION || _type == Constants.FIGHT_TYPE_PVM || _type == Constants.FIGHT_TYPE_PVT)
							{
								int EnergyLoos = Formulas.getLoosEnergy(P.get_lvl(), _type==1, _type==5);
								int Energy = P.get_energy() - EnergyLoos;
								if(Energy < 0) Energy = 0;
								P.set_energy(Energy);
								if(P.isOnline())
									SocketManager.GAME_SEND_Im_PACKET(P, "034;"+EnergyLoos);
								
								if(_type == Constants.FIGHT_TYPE_AGRESSION)
								{
									int honor = P.get_honor()-500;
									if(honor < 0) honor = 0;
									P.set_honor(honor);
									if(P.isOnline())
										SocketManager.GAME_SEND_Im_PACKET(P, "076;"+honor);
								}
								
								/**
								try
								{
									Thread.sleep(1000);
								}catch(Exception E){};
								**/
								if(Energy == 0)
								{
									P.set_PDV(1);
									P.set_Ghosts();
								}else
								{
									P.warpToSavePos();
									P.set_PDV(1);
								} 
							}
							
							if(P.isOnline())
							{/**
								try
								{
									Thread.sleep(200);
								}catch(Exception E){};**/
								SocketManager.GAME_SEND_GV_PACKET(P);
								P.refreshMapAfterFight();
							}
							
							//si c'était a son tour de jouer
							if(_ordreJeu.get(_curPlayer) == null)return;
							if(_ordreJeu.get(_curPlayer).getGUID() == F.getGUID())
							{
								endTurn();
							}
						}
					}else if(_state == Constants.FIGHT_STATE_PLACE)
					{
						boolean isValid1 = false;
						if(T != null)
						{
							if(_init0 != null &&_init0.getPersonnage() != null)
							{
								if(F.getPersonnage().get_GUID() == _init0.getPersonnage().get_GUID())
								{
									isValid1 = true;
								}
							}
							if(_init1 != null &&_init1.getPersonnage() != null)
							{
								if(F.getPersonnage().get_GUID() == _init1.getPersonnage().get_GUID())
								{
									isValid1 = true;
								}
							}
						}
						
						if(isValid1)//Celui qui fait l'action a lancer le combat et leave un autre personnage
						{
							if((T.getTeam() == F.getTeam()) && (T.getGUID() != F.getGUID()))
							{
								if(Server.config.isDebug()) Console.instance.println("EXULSION DE : "+T.getPersonnage().get_name());
								SocketManager.GAME_SEND_ON_FIGHTER_KICK(Fight.this, T.getPersonnage().get_GUID(), getTeamID(T.getGUID()));
								if(_type == Constants.FIGHT_TYPE_AGRESSION || _type == Constants.FIGHT_TYPE_CHALLENGE || _type == Constants.FIGHT_TYPE_PVT) SocketManager.GAME_SEND_ON_FIGHTER_KICK(Fight.this, T.getPersonnage().get_GUID(), getOtherTeamID(T.getGUID()));
								Personnage P = T.getPersonnage();
								P.set_duelID(-1);
								P.set_ready(false);
								P.fullPDV();
								P.set_fight(null);
								P.setSitted(false);
								P.set_away(false);
								
								if(P.isOnline())
								{/**
									try
									{
										Thread.sleep(200);
									}catch(Exception E){};**/
									SocketManager.GAME_SEND_GV_PACKET(P);
									P.refreshMapAfterFight();
								}
								
								//On le supprime de la team
								if(_team0.containsKey(T.getGUID()))
								{
									T.get_cell().removeFighter(T);
									_team0.remove(T.getGUID());
								}
								else if(_team1.containsKey(T.getGUID()))
								{
									T.get_cell().removeFighter(T);
									_team1.remove(T.getGUID());
								}
								for(Personnage z : get_mapOld().getPersos()) FightStateAddFlag(Fight.this.get_mapOld(), z);
							}
						}else if(T == null)//Il leave de son plein gré donc (T = null)
						{
							boolean isValid2 = false;
							if(_init0 != null &&_init0.getPersonnage() != null)
							{
								if(F.getPersonnage().get_GUID() == _init0.getPersonnage().get_GUID())
								{
									isValid2 = true;
								}
							}
							if(_init1 != null &&_init1.getPersonnage() != null)
							{
								if(F.getPersonnage().get_GUID() == _init1.getPersonnage().get_GUID())
								{
									isValid2 = true;
								}
							}
							
							if(isValid2)//Soit il a lancer le combat => annulation du combat
							{
								for(Fighter f : Fight.this.getFighters(F.getTeam2()))
								{
									Personnage P = f.getPersonnage();
									P.set_duelID(-1);
									P.set_ready(false);
									P.fullPDV();
									P.set_fight(null);
									P.setSitted(false);
									P.set_away(false);
									
									if(F.getPersonnage().get_GUID() != f.getPersonnage().get_GUID())//Celui qui a join le fight revient sur la map
									{
										if(P.isOnline())
										{ /**
											try
											{
												Thread.sleep(200);
											}catch(Exception E){};**/
											SocketManager.GAME_SEND_GV_PACKET(P);
											P.refreshMapAfterFight();
										}
									}else//Celui qui a fait le fight meurt + perte honor
									{
										if(_type == Constants.FIGHT_TYPE_AGRESSION || _type == Constants.FIGHT_TYPE_PVM || _type == Constants.FIGHT_TYPE_PVT)
										{
											int EnergyLoos = Formulas.getLoosEnergy(P.get_lvl(), _type==1, _type==5);
											int Energy = P.get_energy() - EnergyLoos;
											if(Energy < 0) Energy = 0;
											P.set_energy(Energy);
											if(P.isOnline())
												SocketManager.GAME_SEND_Im_PACKET(P, "034;"+EnergyLoos);
											
											if(_type == Constants.FIGHT_TYPE_AGRESSION)
											{
												int honor = P.get_honor()-500;
												if(honor < 0) honor = 0;
												P.set_honor(honor);
												if(P.isOnline())
													SocketManager.GAME_SEND_Im_PACKET(P, "076;"+honor);
											}
											
											/**
											try
											{
												Thread.sleep(1000);
											}catch(Exception E){};**/
											if(Energy == 0)
											{
												P.set_PDV(1);
												P.set_Ghosts();
											}else
											{
												P.warpToSavePos();
												P.set_PDV(1);
											}
										}
										
										if(P.isOnline())
										{	/**
											try
											{
												Thread.sleep(200);
											}catch(Exception E){};**/
											SocketManager.GAME_SEND_GV_PACKET(P);
											P.refreshMapAfterFight();
										}
									}
								}
								if(_type == Constants.FIGHT_TYPE_AGRESSION || _type == Constants.FIGHT_TYPE_CHALLENGE || _type == Constants.FIGHT_TYPE_PVT)
								{
									for(Fighter f : Fight.this.getFighters(F.getOtherTeam()))
									{
										if(f.getPersonnage() == null) continue;
										Personnage P = f.getPersonnage();
										P.set_duelID(-1);
										P.set_ready(false);
										P.fullPDV();
										P.set_fight(null);
										P.setSitted(false);
										P.set_away(false);
										
										if(P.isOnline())
										{
											SocketManager.GAME_SEND_GV_PACKET(P);
											P.refreshMapAfterFight();
										}
									}
								}
								_state = 4;//Nous assure de ne pas démarrer le combat
								World.data.getCarte(_map.get_id()).removeFight(_id);
								SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.data.getCarte(_map.get_id()));
								SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(Fight.this.get_mapOld(),_init0.getGUID());
								if(_type == Constants.FIGHT_TYPE_PVT)
								{
									//On actualise la guilde+Message d'attaque FIXME
									for(Personnage z : World.data.getGuild(_guildID).getMembers())
									{
										if(z == null) continue;
										if(z.isOnline())
										{
											SocketManager.GAME_SEND_gITM_PACKET(z, Percepteur.parsetoGuild(z.get_guild().get_id()));
											SocketManager.GAME_SEND_MESSAGE(z, "Votre percepteur remporte la victioire.", Server.config.getMotdColor());
										}
									}
									_perco.set_inFight((byte)0);
									_perco.set_inFightID((byte)-1);
									for(Personnage z : World.data.getCarte((short)_perco.get_mapID()).getPersos())
									{
										if(z == null) continue;
										SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(z.get_compte().getGameClient(), z.get_curCarte());
									}
								}
								if(_type == Constants.FIGHT_TYPE_PVM)
								{			
									int align = -1;
									if(_team1.size() >0)
									{
										 _team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign();
									}
									//Si groupe non fixe
									if(!_mobGroup.isFix())World.data.getCarte(_map.get_id()).spawnGroup(align, 1, true,_mobGroup.getCellID());//Respawn d'un groupe
								}
								_map = null;
								_ordreJeu = null;
							}else//Soit il a rejoin le combat => Left de lui seul
							{
								SocketManager.GAME_SEND_ON_FIGHTER_KICK(Fight.this, F.getPersonnage().get_GUID(), getTeamID(F.getGUID()));
								if(_type == Constants.FIGHT_TYPE_AGRESSION || _type == Constants.FIGHT_TYPE_CHALLENGE || _type == Constants.FIGHT_TYPE_PVT) SocketManager.GAME_SEND_ON_FIGHTER_KICK(Fight.this, F.getPersonnage().get_GUID(), getOtherTeamID(F.getGUID()));
								Personnage P = F.getPersonnage();
								P.set_duelID(-1);
								P.set_ready(false);
								P.fullPDV();
								P.set_fight(null);
								P.setSitted(false);
								P.set_away(false);
								
								if(_type == Constants.FIGHT_TYPE_AGRESSION || _type == Constants.FIGHT_TYPE_PVM || _type == Constants.FIGHT_TYPE_PVT)
								{
									int EnergyLoos = Formulas.getLoosEnergy(P.get_lvl(), _type==1, _type==5);
									int Energy = P.get_energy() - EnergyLoos;
									if(Energy < 0) Energy = 0;
									P.set_energy(Energy);
									if(P.isOnline())
										SocketManager.GAME_SEND_Im_PACKET(P, "034;"+EnergyLoos);
									
									if(_type == Constants.FIGHT_TYPE_AGRESSION)
									{
										int honor = P.get_honor()-500;
										if(honor < 0) honor = 0;
										P.set_honor(honor);
										if(P.isOnline())
											SocketManager.GAME_SEND_Im_PACKET(P, "076;"+honor);
									}
									/**
									try
									{
										Thread.sleep(1000);
									}catch(Exception E){};**/
									if(Energy == 0)
									{
										P.set_PDV(1);
										P.set_Ghosts();
									}else
									{
										P.warpToSavePos();
										P.set_PDV(1);
									} 
								}
								
								if(P.isOnline())
								{/**
									try
									{
										Thread.sleep(200);
									}catch(Exception E){};**/
									SocketManager.GAME_SEND_GV_PACKET(P);
									P.refreshMapAfterFight();
								}
								
								//On le supprime de la team
								if(_team0.containsKey(F.getGUID()))
								{
									F.get_cell().removeFighter(F);
									_team0.remove(F.getGUID());
								}
								else if(_team1.containsKey(F.getGUID()))
								{
									F.get_cell().removeFighter(F);
									_team1.remove(F.getGUID());
								}
								for(Personnage z : get_mapOld().getPersos()) FightStateAddFlag(Fight.this.get_mapOld(), z);
							}
						}
					}else
					{
						if(Server.config.isDebug()) Log.addToLog("Phase de combat non geree, type de combat:"+_type+" T:"+T+" F:"+F);
					}
				break;
				default:
					if(Server.config.isDebug()) Log.addToLog("Type de combat non geree, type de combat:"+_type+" T:"+T+" F:"+F);
				break;
			}
		}else//Si perso en spec
		{
			SocketManager.GAME_SEND_GV_PACKET(perso);
			_spec.remove(perso.get_GUID());
			perso.setSitted(false);
			perso.set_fight(null);
			perso.set_away(false);
		}
	}
	
	public String getGTL()
	{
		String packet = "GTL";
		for(Fighter f: get_ordreJeu())
		{
			packet += "|"+f.getGUID();
		}
		return packet+(char)0x00;
	}

	public int getNextLowerFighterGuid()
	{
		int g = -1;
		for(Fighter f : getFighters(3))
		{
			if(f.getGUID() < g)
				g = f.getGUID();
		}
		g--;
		return g;
	}

	public void addFighterInTeam(Fighter f, int team)
	{
		if(team == 0)
			_team0.put(f.getGUID(), f);
		else if (team == 1)
			_team1.put(f.getGUID(), f);
	}

	public String parseFightInfos()
	{
		StringBuilder infos = new StringBuilder();
		infos.append(_id).append(";");
		long time = System.nanoTime()-_startTime;
		infos.append((_startTime  == 0?"-1":time)).append(";");
		//Team1
		infos.append("0,");//0 car toujours joueur :)
		switch(_type)
		{
			case Constants.FIGHT_TYPE_CHALLENGE:
				infos.append("0,");
				infos.append(_team0.size()).append(";");
				//Team2
				infos.append("0,");
				infos.append("0,");
				infos.append(_team1.size()).append(";");
			break;
			
			case Constants.FIGHT_TYPE_AGRESSION:
				infos.append(_init0.getPersonnage().get_align()).append(",");
				infos.append(_team0.size()).append(";");
				//Team2
				infos.append("0,");
				infos.append(_init1.getPersonnage().get_align()).append(",");
				infos.append(_team1.size()).append(";");
			break;
			
			case Constants.FIGHT_TYPE_PVM:
				infos.append("0,");
				infos.append(_team0.size()).append(";");
				//Team2
				infos.append("1,");
				infos.append(_team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign()).append(",");
				infos.append(_team1.size()).append(";");
			break;
			
			case Constants.FIGHT_TYPE_PVT:
				infos.append("0,");
				infos.append(_team0.size()).append(";");
				//Team2
				infos.append("4,");
				infos.append("0,");
				infos.append(_team1.size()).append(";");
			break;
		}
		return infos.toString();
	}

	public void showCaseToTeam(int guid, int cellID)
	{
		int teams = getTeamID(guid)-1;
		if(teams == 4)return;//Les spectateurs ne montrent pas
		ArrayList<GameClient> PWs = new ArrayList<>();
		if(teams == 0)
		{
			for(Entry<Integer,Fighter> e : _team0.entrySet())
			{
				if(e.getValue().getPersonnage() != null && e.getValue().getPersonnage().get_compte().getGameClient() != null)
					PWs.add(e.getValue().getPersonnage().get_compte().getGameClient());
			}
		}
		else if(teams == 1)
		{
			for(Entry<Integer,Fighter> e : _team1.entrySet())
			{
				if(e.getValue().getPersonnage() != null && e.getValue().getPersonnage().get_compte().getGameClient() != null)
					PWs.add(e.getValue().getPersonnage().get_compte().getGameClient());
			}
		}
		SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
	}
	
	public void showCaseToAll(int guid, int cellID)
	{
		ArrayList<GameClient> PWs = new ArrayList<>();
		for(Entry<Integer,Fighter> e : _team0.entrySet())
		{
			if(e.getValue().getPersonnage() != null && e.getValue().getPersonnage().get_compte().getGameClient() != null)
				PWs.add(e.getValue().getPersonnage().get_compte().getGameClient());
		}
		for(Entry<Integer,Fighter> e : _team1.entrySet())
		{
			if(e.getValue().getPersonnage() != null && e.getValue().getPersonnage().get_compte().getGameClient() != null)
				PWs.add(e.getValue().getPersonnage().get_compte().getGameClient());
		}
		for(Entry<Integer,Personnage> e : _spec.entrySet())
		{
			PWs.add(e.getValue().get_compte().getGameClient());
		}
		SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
	}

	public void joinAsSpect(Personnage p)
	{
		if(!specOk  || _state != Constants.FIGHT_STATE_ACTIVE)
		{
			SocketManager.GAME_SEND_Im_PACKET(p, "157");
			return;
		}
		p.get_curCell().removePlayer(p.get_GUID());
		SocketManager.GAME_SEND_GJK_PACKET(p, _state, 0, 0, 1, 0, _type);
		SocketManager.GAME_SEND_GS_PACKET(p);
		SocketManager.GAME_SEND_GTL_PACKET(p,Fight.this);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(p.get_curCarte(), p.get_GUID());
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(Fight.this,_map,p);
		SocketManager.GAME_SEND_GAMETURNSTART_PACKET(p,_ordreJeu.get(_curPlayer).getGUID(),Constants.TIME_BY_TURN);
		_spec.put(p.get_GUID(), p);
		p.set_fight(Fight.this);
		SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(Fight.this, 7, "036;"+p.get_name());
	}

	public boolean verifyStillInFight()//Return true si au moins un joueur est encore dans le combat
	{
		for(Fighter f : _team0.values())
		{
			if(f.isPerco()) return true;
			if(f.isInvocation() 
			|| f.isDead()
			|| f.getPersonnage() == null
			|| f.getMob() != null
			|| f._double != null
			|| f.hasLeft())
			{
				continue;
			}
			if(f.getPersonnage() != null && f.getPersonnage().get_fight() != null
					&& f.getPersonnage().get_fight().get_id() == Fight.this.get_id()) //Si il n'est plus dans ce combat
			{
				return true;
			}
		}
		for(Fighter f : _team1.values())
		{
			if(f.isPerco()) return true;
			if(f.isInvocation() 
					|| f.isDead()
					|| f.getPersonnage() == null
					|| f.getMob() != null
					|| f._double != null
					|| f.hasLeft())
					{
						continue;
					}
			if(f.getPersonnage() != null && f.getPersonnage().get_fight() != null
					&& f.getPersonnage().get_fight().get_id() == Fight.this.get_id()) //Si il n'est plus dans ce combat
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean verifyStillInFightTeam(int guid)//Return true si au moins un joueur est encore dans la team
	{
		if(_team0.containsKey(guid))
		{
			for(Fighter f : _team0.values())
			{
				if(f.isPerco()) return true;
				if(f.isInvocation() 
						|| f.isDead()
						|| f.getPersonnage() == null
						|| f.getMob() != null
						|| f._double != null
						|| f.hasLeft())
						{
							continue;
						}
				if(f.getPersonnage() != null && f.getPersonnage().get_fight() != null
						&& f.getPersonnage().get_fight().get_id() == Fight.this.get_id()) //Si il n'est plus dans ce combat
				{
					return true;
				}
			}
		}else if(_team1.containsKey(guid))
		{
			for(Fighter f : _team1.values())
			{
				if(f.isPerco()) return true;
				if(!f.isInvocation() 
						|| f.isDead()
						|| f.getPersonnage() == null
						|| f.getMob() != null
						|| f._double != null
						|| f.hasLeft())
						{
							continue;
						}
				if(f.getPersonnage() != null && f.getPersonnage().get_fight() != null
						&& f.getPersonnage().get_fight().get_id() == Fight.this.get_id()) //Si il n'est plus dans ce combat
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static void FightStateAddFlag(Carte _map, Personnage P)
	{
		for(Entry<Integer, Fight> fight : _map.get_fights().entrySet())
		{
			if(fight.getValue()._state == Constants.FIGHT_STATE_PLACE)
			{
				if(fight.getValue()._type == Constants.FIGHT_TYPE_CHALLENGE)
				{
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),0,fight.getValue()._init0.getGUID(),fight.getValue()._init1.getGUID(),fight.getValue()._init0.getPersonnage().get_curCell().getID(),"0;-1", fight.getValue()._init1.getPersonnage().get_curCell().getID(), "0;-1");
					for(Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet())
					{
						if(Server.config.isDebug()) Console.instance.println(F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for(Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet())
					{
						if(Server.config.isDebug()) Console.instance.println(F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init1.getPersonnage().get_curCarte(),fight.getValue()._init1.getGUID(), fight.getValue()._init1);
					}
				}else if(fight.getValue()._type == Constants.FIGHT_TYPE_AGRESSION)
				{
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),0,fight.getValue()._init0.getGUID(),fight.getValue()._init1.getGUID(),fight.getValue()._init0.getPersonnage().get_curCell().getID(),"0;"+fight.getValue()._init0.getPersonnage().get_align(), fight.getValue()._init1.getPersonnage().get_curCell().getID(), "0;"+fight.getValue()._init1.getPersonnage().get_align());
					for(Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet())
					{
						if(Server.config.isDebug()) Console.instance.println(F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for(Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet())
					{
						if(Server.config.isDebug()) Console.instance.println(F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init1.getPersonnage().get_curCarte(),fight.getValue()._init1.getGUID(), fight.getValue()._init1);
					}
				}else if(fight.getValue()._type == Constants.FIGHT_TYPE_PVM)
				{
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),4,fight.getValue()._init0.getGUID(),fight.getValue()._mobGroup.getID(),(fight.getValue()._init0.getPersonnage().get_curCell().getID()+1),"0;-1",fight.getValue()._mobGroup.getCellID(),"1;-1");
					for(Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet())
					{
						if(Server.config.isDebug()) Console.instance.println("PVM1: "+F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for(Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet())
					{
						if(Server.config.isDebug()) Console.instance.println("PVM2: "+F.getValue());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._map,fight.getValue()._mobGroup.getID(), F.getValue());
					}
				}else if(fight.getValue()._type == Constants.FIGHT_TYPE_PVT)
				{
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),5,fight.getValue()._init0.getGUID(),fight.getValue()._perco.getGuid(),(fight.getValue()._init0.getPersonnage().get_curCell().getID()+1),"0;-1",fight.getValue()._perco.get_cellID(),"3;-1");
					for(Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet())
					{
						if(Server.config.isDebug()) Console.instance.println("PVT1: "+F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for(Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet())
					{
						if(Server.config.isDebug()) Console.instance.println("PVT2: "+F.getValue());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._map,fight.getValue()._perco.getGuid(), F.getValue());
					}
				}
			}
		}
	}
	
	public static int getFightIDByFighter(Carte _map, int guid)
	{
		for(Entry<Integer, Fight> fight : _map.get_fights().entrySet())
		{
			for(Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet())
			{
				if(F.getValue().getPersonnage() != null && F.getValue().getGUID() == guid)
				{
					return fight.getValue().get_id();
				}
			}
		}
		return 0;
	}
	
	public Map<Integer,Fighter> getDeadList()
	{
		return deadList;
	}	
		
	public void delOneDead(Fighter target)
	{
		deadList.remove(target.getGUID());
	}

	public int get_curFighterPM() {
		return _curFighterPM;
	}

	public void set_curFighterPM(int _curFighterPM) {
		Fight.this._curFighterPM = _curFighterPM;
	}

	public int get_curFighterPA() {
		return _curFighterPA;
	}

	public void set_curFighterPA(int _curFighterPA) {
		Fight.this._curFighterPA = _curFighterPA;
	}

	public Carte get_mapOld() {
		return _mapOld;
	}

	public void set_mapOld(Carte _mapOld) {
		this._mapOld = _mapOld;
	}

	public Waiter getWaiter() {
		return waiter;
	}

	public void setWaiter(Waiter waiter) {
		this.waiter = waiter;
	}

	public Timer get_turnTimer() {
		return _turnTimer;
	}

	public void set_turnTimer(Timer _turnTimer) {
		this._turnTimer = _turnTimer;
	}

	public String get_curAction() {
		return _curAction;
	}

	public void set_curAction(String _curAction) {
		this._curAction = _curAction;
	}
}
