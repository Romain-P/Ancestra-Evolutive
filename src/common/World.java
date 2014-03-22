package common;

import game.GameClient;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import objects.Animations;
import objects.Area;
import objects.Carte;
import objects.Carte.MountPark;
import objects.Compte;
import objects.Dragodinde;
import objects.ExpLevel;
import objects.Guild;
import objects.HDV;
import objects.HDV.HdvEntry;
import objects.House;
import objects.IOTemplate;
import objects.ItemSet;
import objects.Monstre;
import objects.NPC_tmpl;
import objects.NPC_tmpl.NPC_question;
import objects.NPC_tmpl.NPC_reponse;
import objects.Objet;
import objects.Objet.ObjTemplate;
import objects.job.Job;
import objects.Percepteur;
import objects.Personnage;
import objects.PierreAme;
import objects.Sort;
import objects.SubArea;
import objects.SuperArea;
import objects.Trunk;
import tool.command.Command;
import core.Console;
import core.Log;
import core.Main;
import core.Server;
import database.Database;

public class World {

	/**
	 * All data on this fucking case
	 */
	public static World data = new World();
	public static Database database = new Database();
	
	
	private Map<Integer, Compte> accounts = new HashMap<>();
	private Map<Integer, Personnage> players = new HashMap<>();
	private Map<Short, Carte> maps = new HashMap<>();
	private Map<Integer, Objet> objects = new HashMap<>();
	private Map<Integer, ExpLevel> expLevels = new HashMap<>();
	private Map<Integer, Sort> spells = new HashMap<>();
	private Map<Integer, ObjTemplate> templateObjects = new HashMap<>();
	private Map<Integer, Monstre> templateMobs = new HashMap<>();
	private Map<Integer, NPC_tmpl> npcTemplates = new HashMap<>();
	private Map<Integer, NPC_question> npcQuestions = new HashMap<>();
	private Map<Integer, NPC_reponse> npcResponses = new HashMap<>();
	private Map<Integer, IOTemplate> templateIO = new HashMap<>();
	private Map<Integer, Dragodinde> mounts = new HashMap<>();
	private Map<Integer, SuperArea> superAreas = new HashMap<>();
	private Map<Integer, Area> areas = new HashMap<>();
	private Map<Integer, SubArea> subAreas = new HashMap<>();
	private Map<Integer, Job> jobs = new HashMap<>();
	private Map<Integer, ArrayList<Couple<Integer, Integer>>> crafts = new HashMap<>();
	private Map<Integer, ItemSet> setItems = new HashMap<>();
	private Map<Integer, Guild> guilds = new HashMap<>();
	private Map<Integer, HDV> hdvs = new HashMap<>();
	private Map<Integer, Map<Integer, ArrayList<HdvEntry>>> hdvItems = new HashMap<>();
	private Map<Integer, Personnage> married = new HashMap<>();
	private Map<Integer, Animations> animations = new HashMap<>();
	private Map<Short, Carte.MountPark> mountParks = new HashMap<>();
	private Map<Integer, Trunk> trunks = new HashMap<>();
	private Map<Integer, Percepteur> collectors = new ConcurrentHashMap<>();
	private Map<Integer, House> houses = new HashMap<>();
	private Map<Short, Collection<Integer>> sellers = new HashMap<>();
	private Map<String, Command<Personnage>> playerCommands = new HashMap<>();
	private Map<String, Command<Console>> consoleCommands = new HashMap<>();
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private Connection connection;
	
	private int nextHdvID;
	private int nextLigneID; 
	private int saveTries = 1; 
	private short state = 1;
	private byte gmAccess = 0; 
	private int nextObjectID; 
	
	private ExecutorService saveWorker = Executors.newCachedThreadPool();
	private ExecutorService iaWorker = Executors.newCachedThreadPool();
	
	public int initialize() {
		long startTime = System.currentTimeMillis();
		
		//chargement des données statiques
		database.getOtherData().loadBannedIps();
		database.getOtherData().loadCrafts();
		database.getOtherData().loadZaaps();
		database.getOtherData().loadZaapis();
		database.getExpData().loadAll();
		
		//truc pourri à refaire plus tard
		nextObjectID = database.getItemData().nextId();
		return (int)(System.currentTimeMillis() - startTime);
	}

	public Area getArea(int areaID) {
		Area area = areas.get(areaID);
		if(area == null)
			area = World.database.getAreaData().load(areaID);
		return area;
	}

	public SuperArea getSuperArea(int areaID) {
		return superAreas.get(areaID);
	}

	public SubArea getSubArea(int areaID) {
		SubArea subArea = subAreas.get(areaID);
		if(subArea == null)
			subArea = World.database.getAreaSubData().load(areaID);
		return subArea;
	}

	public void addArea(Area area) {
		areas.put(area.get_id(), area);
	}

	public void addSuperArea(SuperArea SA) {
		superAreas.put(SA.get_id(), SA);
	}

	public void addSubArea(SubArea SA) {
		subAreas.put(SA.get_id(), SA);
	}

	public void addNPCreponse(NPC_reponse rep) {
		npcResponses.put(rep.get_id(), rep);
	}

	public NPC_reponse getNPCreponse(int guid) {
		NPC_reponse object = npcResponses.get(guid);
		if(object == null)
			object = World.database.getNpcAnswerData().load(guid);
		return object;
	}

	public int getExpLevelSize() {
		return expLevels.size();
	}

	public void addExpLevel(int lvl, ExpLevel exp) {
		expLevels.put(lvl, exp);
	}

	public Compte getCompte(int guid) {
		return accounts.get(guid);
	}

	public void addNPCQuestion(NPC_question quest) {
		npcQuestions.put(quest.get_id(), quest);
	}

	public NPC_question getNPCQuestion(int guid) {
		NPC_question object = npcQuestions.get(guid);
		if(object == null)
			object = World.database.getNpcQuestionData().load(guid);
		return object;
	}

	public NPC_tmpl getNPCTemplate(int guid) {
		NPC_tmpl object = npcTemplates.get(guid);
		if(object == null)
			object = World.database.getNpcTemplateData().load(guid);
		return object;
	}

	public void addNpcTemplate(NPC_tmpl temp) {
		npcTemplates.put(temp.get_id(), temp);
	}

	public Carte getCarte(short id) {
		Carte map = maps.get(id);
		if(map == null)
			map = World.database.getMapData().load(id);
		return map;
	}

	public void addCarte(Carte map) {
		if (!maps.containsKey(map.get_id()))
			maps.put(map.get_id(), map);
	}

	public void delCarte(Carte map) {
		if (maps.containsKey(map.get_id()))
			maps.remove(map.get_id());
	}

	public Compte getCompteByName(String name) {
		Compte account = null;
		
		for(Compte acc: accounts.values())
			if(acc.get_name().equalsIgnoreCase(name))
				account = acc;
		
		if(account == null) 
			account = World.database.getAccountData().loadByName(name);
		return account;
	}

	public Personnage getPersonnage(int guid) {
		return players.get(guid);
	}

	public void addAccount(Compte compte) {
		accounts.put(compte.get_GUID(), compte);
	}

	public void addPersonnage(Personnage perso) {
		players.put(perso.get_GUID(), perso);
	}

	public Personnage getPersoByName(String name) {
		ArrayList<Personnage> Ps = new ArrayList<Personnage>();
		Ps.addAll(players.values());
		for (Personnage P : Ps)
			if (P.get_name().equalsIgnoreCase(name))
				return P;
		return null;
	}

	public void deletePerso(Personnage perso) {
		if (perso.get_guild() != null) {
			if (perso.get_guild().getMembers().size() <= 1)// Il est tout seul
															// dans la guilde :
															// Supression
			{
				removeGuild(perso.get_guild().get_id());
			} else if (perso.getGuildMember().getRank() == 1)// On passe les
																// pouvoir a
																// celui qui a
																// le plus de
																// droits si il
																// est meneur
			{
				int curMaxRight = 0;
				Personnage Meneur = null;
				for (Personnage newMeneur : perso.get_guild().getMembers()) {
					if (newMeneur == perso)
						continue;
					if (newMeneur.getGuildMember().getRights() < curMaxRight) {
						Meneur = newMeneur;
					}
				}
				perso.get_guild().removeMember(perso);
				Meneur.getGuildMember().setRank(1);
			} else// Supression simple
			{
				perso.get_guild().removeMember(perso);
			}
		}
		perso.remove();// Supression BDD Perso, items, monture.
		unloadPerso(perso.get_GUID());// UnLoad du perso+item
	}

	public String getSousZoneStateString() {
		String data = "";
		/* TODO: Sous Zone Alignement */
		return data;
	}

	public long getPersoXpMin(int _lvl) {
		if (_lvl > getExpLevelSize())
			_lvl = getExpLevelSize();
		if (_lvl < 1)
			_lvl = 1;
		return expLevels.get(_lvl).perso;
	}

	public long getPersoXpMax(int _lvl) {
		if (_lvl >= getExpLevelSize())
			_lvl = (getExpLevelSize() - 1);
		if (_lvl <= 1)
			_lvl = 1;
		return expLevels.get(_lvl + 1).perso;
	}

	public void addSort(Sort sort) {
		spells.put(sort.getSpellID(), sort);
	}

	public void addObjTemplate(ObjTemplate obj) {
		templateObjects.put(obj.getID(), obj);
	}

	public Sort getSort(int id) {
		Sort spell = spells.get(id);
		if(spell == null)
			spell = World.database.getSpellData().load(id);
		return spell;
	}

	public ObjTemplate getObjTemplate(int id) {
		ObjTemplate template = templateObjects.get(id);
		if(template == null)
			template = World.database.getItemTemplateData().load(id);
		return template;
	}

	public synchronized int getNewItemGuid() {
		return nextObjectID++;
	}

	public void addMobTemplate(int id, Monstre mob) {
		templateMobs.put(id, mob);
	}

	public Monstre getMonstre(int id) {
		Monstre monster = templateMobs.get(id);
		if(monster == null)
			monster = World.database.getMonsterData().load(id);
		return monster;
	}

	public List<Personnage> getOnlinePersos() {
		List<Personnage> online = new ArrayList<Personnage>();
		for (Entry<Integer, Personnage> perso : players.entrySet()) {
			if (perso.getValue().isOnline()
					&& perso.getValue().get_compte().getGameClient() != null) {
				if (perso.getValue().get_compte().getGameClient() != null) {
					online.add(perso.getValue());
				}
			}
		}
		return online;
	}

	public void addObjet(Objet item, boolean saveSQL) {
		objects.put(item.getGuid(), item);
		if (saveSQL)
			World.database.getItemData().create(item);
	}

	public Objet getObjet(int guid) {
		Objet item = objects.get(guid);
		if(item == null)
			item = World.database.getItemData().load(guid);
		return item;
	}

	public void removeItem(int guid) {
		Objet o = objects.get(guid);
		objects.remove(guid);
		database.getItemData().delete(o);
	}

	public void addIOTemplate(IOTemplate IOT) {
		templateIO.put(IOT.getId(), IOT);
	}

	public Dragodinde getDragoByID(int id) {
		Dragodinde mount = mounts.get(id);
		if(mount == null)
			mount = World.database.getMountData().load(id);
		return mount;
	}

	public void addDragodinde(Dragodinde DD) {
		mounts.put(DD.get_id(), DD);
	}

	public void removeDragodinde(int DID) {
		mounts.remove(DID);
	}

	public void saveData(final int saverID) {
		saveWorker.execute(new Runnable() {
			public void run() {
				GameClient _out = null;
				Personnage saver = saverID != -1 ? getPersonnage(saverID)
						: null;
				if (saver != null)
					_out = saver.get_compte().getGameClient();

				set_state((short) 2);

				try {
					Log.addToLog("Lancement de la sauvegarde du Monde...");
					SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1164");
					Server.config.setSaving(true);

					Log.addToLog("Sauvegarde des personnages...");
					for (Personnage perso : players.values()) {
						if (!perso.isOnline())
							continue;
						database.getCharacterData().update(perso);
						database.getCharacterData().updateItems(perso);		
					}

					Log.addToLog("Sauvegarde des guildes...");
					for (Guild guilde : guilds.values()) {
						database.getGuildData().update(guilde);
					}

					Log.addToLog("Sauvegarde des percepteurs...");
					for (Percepteur perco : collectors.values()) {
						if (perco.get_inFight() > 0)
							continue;
						database.getCollectorData().update(perco);
					}

					Log.addToLog("Sauvegarde des maisons...");
					for (House house : houses.values()) {
						if (house.get_owner_id() > 0) {
							database.getHouseData().update(house);
						}
					}

					Log.addToLog("Sauvegarde des coffres...");
					for (Trunk t : trunks.values()) {
						if (t.get_owner_id() > 0) {
							database.getTrunkData().update(t);
						}
					}

					Log.addToLog("Sauvegarde des enclos...");
					for (Carte.MountPark mp : mountParks.values()) {
						if (mp.get_owner() > 0 || mp.get_owner() == -1) {
							database.getMountparkData().update(mp);
						}
					}

					Log.addToLog("Sauvegarde des hdvs...");
					for (HDV curHdv : hdvs.values()) {
						database.getHdvData().updateHdvItems(curHdv.getHdvID());
					}

					Log.addToLog("Sauvegarde effectuee !");

					set_state((short) 1);
					// TODO : Rafraichir

				} catch (ConcurrentModificationException e) {
					if (saveTries < 10) {
						Log.addToLog("Nouvelle tentative de sauvegarde");
						if (saver != null && _out != null)
							SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(
									_out,
									"Erreur. Nouvelle tentative de sauvegarde");
						saveTries++;
						saveData(saver.get_GUID());
					} else {
						set_state((short) 1);
						// TODO : Rafraichir
						String mess = "Echec de la sauvegarde apres "
								+ saveTries + " tentatives";
						if (saver != null && _out != null)
							SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(
									_out, mess);
						Log.addToLog(mess);
					}
				} catch (Exception e) {
					Log.addToLog("Erreur lors de la sauvegarde : "
							+ e.getMessage());
					e.printStackTrace();
				} finally {
					Server.config.setSaving(false);
					saveTries = 1;
					SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1165");
				}
			}
		});
	}

	public void RefreshAllMob() {
		SocketManager.GAME_SEND_MESSAGE_TO_ALL(
				"Recharge des Mobs en cours, des latences peuvent survenir.",
				Server.config.getMotdColor());
		for (Carte map : maps.values()) {
			map.refreshSpawns();
		}
		SocketManager
				.GAME_SEND_MESSAGE_TO_ALL(
						"Recharge des Mobs finie. La prochaine recharge aura lieu dans 5heures.",
						Server.config.getMotdColor());
	}

	public ExpLevel getExpLevel(int lvl) {
		ExpLevel level = expLevels.get(lvl);
		if(level == null)
			return World.database.getExpData().load(lvl);
		return expLevels.get(lvl);
	}

	public IOTemplate getIOTemplate(int id) {
		IOTemplate template = templateIO.get(id);
		if(template == null)
			template = World.database.getIoTemplates().load(id);
		return template;
	}

	public Job getMetier(int id) {
		Job job = jobs.get(id);
		if(job == null)
			job = World.database.getJobData().load(id);
		return job;
	}

	public void addJob(Job job) {
		jobs.put(job.getId(), job);
	}

	public void addCraft(int id, ArrayList<Couple<Integer, Integer>> m) {
		crafts.put(id, m);
	}

	public ArrayList<Couple<Integer, Integer>> getCraft(int i) {
		return crafts.get(i);
	}

	public int getObjectByIngredientForJob(ArrayList<Integer> list,
			Map<Integer, Integer> ingredients) {
		if (list == null)
			return -1;
		for (int tID : list) {
			ArrayList<Couple<Integer, Integer>> craft = getCraft(tID);
			if (craft == null) {
				Log.addToLog("/!\\Recette pour l'objet " + tID
						+ " non existante !");
				continue;
			}
			if (craft.size() != ingredients.size())
				continue;
			boolean ok = true;
			for (Couple<Integer, Integer> c : craft) {
				// si ingredient non présent ou mauvaise quantité
				if (ingredients.get(c.first) != c.second)
					ok = false;
			}
			if (ok)
				return tID;
		}
		return -1;
	}

	public Compte getCompteByPseudo(String p) {
		for (Compte C : accounts.values())
			if (C.get_pseudo().equals(p))
				return C;
		return null;
	}

	public void addItemSet(ItemSet itemSet) {
		setItems.put(itemSet.getId(), itemSet);
	}

	public ItemSet getItemSet(int tID) {
		ItemSet set = setItems.get(tID);
		if(set == null)
			set = World.database.getItemSetData().load(tID);
		return set;
	}

	public int getItemSetNumber() {
		return setItems.size();
	}

	public int getNextIdForMount() {
		int max = 1;
		for (int a : mounts.keySet())
			if (a > max)
				max = a;
		return max + 1;
	}

	public Carte getCarteByPosAndCont(int mapX, int mapY, int contID) {
		for (Carte map : maps.values()) {
			if (map.getX() == mapX
					&& map.getY() == mapY
					&& map.getSubArea().get_area().get_superArea().get_id() == contID)
				return map;
		}
		return null;
	}

	public void addGuild(Guild g, boolean save) {
		guilds.put(g.get_id(), g);
		if (save)
			database.getGuildData().create(g);
	}

	public int getNextHighestGuildID() {
		if (guilds.isEmpty())
			return 1;
		int n = 0;
		for (int x : guilds.keySet())
			if (n < x)
				n = x;
		return n + 1;
	}

	public boolean guildNameIsUsed(String name) {
		return database.getGuildData().exist(name);
	}

	public boolean guildEmblemIsUsed(String emb) {
		for (Guild g : guilds.values()) {
			if (g.get_emblem().equals(emb))
				return true;
		}
		return false;
	}

	public Guild getGuild(int id) {
		Guild guild = guilds.get(id);
		if(guild == null)
			guild = World.database.getGuildData().load(id);
		return guild;
	}

	public long getGuildXpMax(int _lvl) {
		if (_lvl >= 200)
			_lvl = 199;
		if (_lvl <= 1)
			_lvl = 1;
		return expLevels.get(_lvl + 1).guilde;
	}

	public void ReassignAccountToChar(Compte C) {
		C.get_persos().clear();
		database.getCharacterData().loadByAccount(C);
		for (Personnage P : players.values()) {
			if (P.getAccID() == C.get_GUID()) {
				P.setAccount(C);
			}
		}
	}

	public int getZaapCellIdByMapId(short i) {
		for (Entry<Integer, Integer> zaap : Constants.ZAAPS.entrySet()) {
			if (zaap.getKey() == i)
				return zaap.getValue();
		}
		return -1;
	}

	public int getEncloCellIdByMapId(short i) {
		if (getCarte(i).getMountPark() != null) {
			if (getCarte(i).getMountPark().get_cellid() > 0) {
				return getCarte(i).getMountPark().get_cellid();
			}
		}

		return -1;
	}

	public void delDragoByID(int getId) {
		mounts.remove(getId);
	}

	public void removeGuild(int id) {
		// Maison de guilde+SQL
		House.removeHouseGuild(id);
		// Enclo+SQL
		Carte.MountPark.removeMountPark(id);
		// Percepteur+SQL
		Percepteur.removePercepteur(id);
		// Guilde
		Guild g = guilds.get(id);
		guilds.remove(id);
		
		database.getGuildMemberData().deleteAllByGuild(id);
		database.getGuildData().delete(g);
	}

	public boolean ipIsUsed(String ip) {
		for (Compte c : accounts.values())
			if (c.get_curIP().compareTo(ip) == 0)
				return true;
		return false;
	}

	public void unloadPerso(int g) {
		Personnage toRem = players.get(g);
		if (!toRem.getItems().isEmpty()) {
			for (Entry<Integer, Objet> curObj : toRem.getItems().entrySet()) {
				objects.remove(curObj.getKey());
			}
		}
		toRem = null;
		// players.remove(g);
	}

	public boolean isArenaMap(int mapID) {
		for (int curID : Server.config.getArenaMaps()) {
			if (curID == mapID)
				return true;
		}
		return false;
	}

	public Objet newObjet(int Guid, int template, int qua, int pos,
			String strStats) {
		if (getObjTemplate(template) == null) {
			Console.instance.println("ItemTemplate " + template
					+ " inexistant, GUID dans la table `items`:" + Guid);
			Main.closeServers();
		}

		if (getObjTemplate(template).getType() == 85)
			return new PierreAme(Guid, qua, template, pos, strStats);
		else
			return new Objet(Guid, template, qua, pos, strStats);
	}

	public short get_state() {
		return state;
	}

	public void set_state(short state) {
		this.state = state;
	}

	public byte getGmAccess() {
		return gmAccess;
	}

	public void setGmAccess(byte GmAccess) {
		gmAccess = GmAccess;
	}

	public HDV getHdv(int mapID) {
		HDV object = hdvs.get(mapID);
		if(object == null)
			object = World.database.getHdvData().load(mapID);
		return object;
	}

	public synchronized int getNextHdvID()// ATTENTION A NE PAS EXECUTER POUR
											// RIEN CETTE METHODE CHANGE LE
											// PROCHAIN ID DE L'HDV LORS DE SON
											// EXECUTION
	{
		nextHdvID++;
		return nextHdvID;
	}

	public synchronized void setNextHdvID(int nextID) {
		nextHdvID = nextID;
	}

	public synchronized int getNextLigneID() {
		nextLigneID++;
		return nextLigneID;
	}

	public synchronized void setNextLigneID(int ligneID) {
		nextLigneID = ligneID;
	}

	public void addHdvItem(int compteID, int hdvID, HdvEntry toAdd) {
		if (hdvItems.get(compteID) == null) // Si le compte n'est pas dans la
											// memoire
			hdvItems.put(compteID, new HashMap<Integer, ArrayList<HdvEntry>>()); 

		if (hdvItems.get(compteID).get(hdvID) == null)
			hdvItems.get(compteID).put(hdvID, new ArrayList<HdvEntry>());

		hdvItems.get(compteID).get(hdvID).add(toAdd);
	}

	public void removeHdvItem(int compteID, int hdvID, HdvEntry toDel) {
		hdvItems.get(compteID).get(hdvID).remove(toDel);
	}

	public int getHdvNumber() {
		return hdvs.size();
	}

	public int getHdvObjetsNumber() {
		int size = 0;

		for (Map<Integer, ArrayList<HdvEntry>> curCompte : hdvItems.values()) {
			for (ArrayList<HdvEntry> curHdv : curCompte.values()) {
				size += curHdv.size();
			}
		}
		return size;
	}

	public void addHdv(HDV toAdd) {
		hdvs.put(toAdd.getHdvID(), toAdd);
	}

	public Map<Integer, ArrayList<HdvEntry>> getMyItems(int compteID) {
		if (hdvItems.get(compteID) == null)// Si le compte n'est pas dans la
											// memoire
			hdvItems.put(compteID, new HashMap<Integer, ArrayList<HdvEntry>>());// Ajout
																				// du
																				// compte
																				// clé:compteID
																				// et
																				// un
																				// nouveau
																				// map<hdvID,items

		return hdvItems.get(compteID);
	}

	public Collection<ObjTemplate> getObjTemplates() {
		return templateObjects.values();
	}

	public Personnage getMarried(int ordre) {
		return married.get(ordre);
	}

	public void AddMarried(int ordre, Personnage perso) {
		Personnage Perso = married.get(ordre);
		if (Perso != null) {
			if (perso.get_GUID() == Perso.get_GUID()) // Si c'est le meme
														// joueur...
				return;
			if (Perso.isOnline())// Si perso en ligne...
			{
				married.remove(ordre);
				married.put(ordre, perso);
				return;
			}

			return;
		} else {
			married.put(ordre, perso);
			return;
		}
	}

	public void PriestRequest(Personnage perso, Carte carte, int IdPretre) {
		Personnage Homme = married.get(0);
		Personnage Femme = married.get(1);
		if (Homme.getWife() != 0) {
			SocketManager.GAME_SEND_MESSAGE_TO_MAP(carte, Homme.get_name()
					+ " est deja marier!", Server.config.getMotdColor());
			return;
		}
		if (Femme.getWife() != 0) {
			SocketManager.GAME_SEND_MESSAGE_TO_MAP(carte, Femme.get_name()
					+ " est deja marier!", Server.config.getMotdColor());
			return;
		}
		SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(perso.get_curCarte(), "", -1,
				"Prêtre", perso.get_name()
						+ " acceptez-vous d'épouser "
						+ getMarried((perso.get_sexe() == 1 ? 0 : 1))
								.get_name() + " ?");
		SocketManager.GAME_SEND_WEDDING(carte, 617,
				(Homme == perso ? Homme.get_GUID() : Femme.get_GUID()),
				(Homme == perso ? Femme.get_GUID() : Homme.get_GUID()),
				IdPretre);
	}

	public void Wedding(Personnage Homme, Personnage Femme, int isOK) {
		if (isOK > 0) {
			SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(Homme.get_curCarte(), "",
					-1, "Prêtre", "Je déclare " + Homme.get_name() + " et "
							+ Femme.get_name()
							+ " unis par les liens sacrés du mariage.");
			Homme.MarryTo(Femme);
			Femme.MarryTo(Homme);
		} else {
			SocketManager.GAME_SEND_Im_PACKET_TO_MAP(Homme.get_curCarte(),
					"048;" + Homme.get_name() + "~" + Femme.get_name());
		}
		married.get(0).setisOK(0);
		married.get(1).setisOK(0);
		married.clear();
	}

	public Animations getAnimation(int AnimationId) {
		Animations animation = animations.get(AnimationId);
		if(animation == null)
			animation = World.database.getAnimationData().load(AnimationId);
		return animation;
	}

	public void addAnimation(Animations animation) {
		animations.put(animation.getId(), animation);
	}

	public void addHouse(House house) {
		houses.put(house.get_id(), house);
	}

	public Map<Integer, House> getHouses() {
		return houses;
	}

	public House getHouse(int id) {
		return houses.get(id);
	}

	public void addPerco(Percepteur perco) {
		collectors.put(perco.getGuid(), perco);
	}

	public Percepteur getPerco(int percoID) {
		Percepteur spell = collectors.get(percoID);
		if(spell == null)
			spell = World.database.getCollectorData().load(percoID);
		return spell;
	}

	public Map<Integer, Percepteur> getPercos() {
		return collectors;
	}

	public void addTrunk(Trunk trunk) {
		trunks.put(trunk.get_id(), trunk);
	}

	public Trunk getTrunk(int id) {
		return trunks.get(id);
	}

	public Map<Integer, Trunk> getTrunks() {
		return trunks;
	}

	public void addMountPark(Carte.MountPark mp) {
		mountParks.put(mp.get_map().get_id(), mp);
	}

	public Map<Short, Carte.MountPark> getMountPark() {
		
		return mountParks;
	}
	
	public MountPark getMountPark(int mapid) {
		MountPark map = mountParks.get(mapid);
		if(map == null)
			map = World.database.getMountparkData().load(mapid);
		return map;
	}

	public String parseMPtoGuild(int GuildID) {
		Guild G = getGuild(GuildID);
		byte enclosMax = (byte) Math.floor(G.get_lvl() / 10);
		StringBuilder packet = new StringBuilder();
		packet.append(enclosMax);

		for (Entry<Short, Carte.MountPark> mp : mountParks.entrySet()) {
			if (mp.getValue().get_guild() != null
					&& mp.getValue().get_guild().get_id() == GuildID) {
				packet.append("|").append(mp.getValue().get_map().get_id())
						.append(";").append(mp.getValue().get_size())
						.append(";").append(mp.getValue().getObjectNumb());// Nombre
																			// d'objets
																			// pour
																			// le
																			// dernier
			} else {
				continue;
			}
		}
		return packet.toString();
	}

	public int totalMPGuild(int GuildID) {
		int i = 0;
		for (Entry<Short, Carte.MountPark> mp : mountParks.entrySet()) {
			if (mp.getValue().get_guild().get_id() == GuildID) {
				i++;
			} else {
				continue;
			}
		}
		return i;
	}

	public void addSeller(Personnage p) {
		if (sellers.get(p.get_curCarte().get_id()) == null) {
			ArrayList<Integer> PersoID = new ArrayList<Integer>();
			PersoID.add(p.get_GUID());
			sellers.put(p.get_curCarte().get_id(), PersoID);
		} else {
			ArrayList<Integer> PersoID = new ArrayList<Integer>();
			PersoID.addAll(sellers.get(p.get_curCarte().get_id()));
			PersoID.add(p.get_GUID());
			sellers.remove(p.get_curCarte().get_id());
			sellers.put(p.get_curCarte().get_id(), PersoID);
		}
	}

	public Collection<Integer> getSeller(short mapID) {
		return sellers.get(mapID);
	}

	public void removeSeller(int pID, short mapID) {
		sellers.get(mapID).remove(pID);
	}

	public Map<String, Command<Personnage>> getPlayerCommands() {
		return playerCommands;
	}

	public Map<String, Command<Console>> getConsoleCommands() {
		return consoleCommands;
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	public Map<Integer, Personnage> getPlayers() {
		return this.players;
	}

	public ExecutorService getIaWorker() {
		return iaWorker;
	}
	
	public Map<Integer, Compte> getAccounts() {
		return this.accounts;
	}
}
