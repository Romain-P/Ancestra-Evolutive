package database.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import objects.Objet;
import objects.Objet.ObjTemplate;
import objects.Personnage;

import common.Constants;
import common.Couple;
import common.SocketManager;
import common.World;

import core.Console;
import core.Log;
import database.AbstractDAO;

public class OtherData extends AbstractDAO<Object>{

	public OtherData(Connection connection, ReentrantLock locker) {
		super(connection, locker);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean create(Object obj) { return false; }
	@Override
	public boolean delete(Object obj) { return false; }
	@Override
	public boolean update(Object obj) { return false; }
	@Override
	public Object load(int id) { return null; }
	
	public void loadZaaps() {
		try {
			ResultSet result = getData("SELECT * FROM zaaps");
			while (result.next()) {
				Constants.ZAAPS.put(result.getInt("mapID"), result.getInt("cellID"));
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("LoadZaaps: "+e.getMessage());
		}
	}
	
	public void loadZaapis() {
		try {
			String bonta = "", brakmar = "", neutre = "";
			ResultSet result = getData("SELECT * FROM zaapi");
			
			while (result.next()) {
				if (result.getInt("align") == Constants.ALIGNEMENT_BONTARIEN) {
					bonta += result.getString("mapid");
					if (!result.isLast())
						bonta += ",";
				} else if (result.getInt("align") == Constants.ALIGNEMENT_BRAKMARIEN) {
					brakmar += result.getString("mapid");
					if (!result.isLast())
						brakmar += ",";
				} else {
					neutre += result.getString("mapid");
					if (!result.isLast())
						neutre += ",";
				}
			}
			Constants.ZAAPI.put(Constants.ALIGNEMENT_BONTARIEN, bonta);
			Constants.ZAAPI.put(Constants.ALIGNEMENT_BRAKMARIEN, brakmar);
			Constants.ZAAPI.put(Constants.ALIGNEMENT_NEUTRE, neutre);
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(OtherData): "+e.getMessage());
		}
	}
	
	public void loadBannedIps() {
		try {
			ResultSet result = getData("SELECT * FROM banip");
			while (result.next()) {
				Constants.BAN_IP += result.getString("ip");
				if (!result.isLast())
					Constants.BAN_IP += ",";
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(OtherData): "+e.getMessage());
		}
	}
	
	public boolean addBannedIp(String ip) {
		try {
			String baseQuery = "INSERT INTO `banip`" + " VALUES (?);";
			PreparedStatement statement = connection.prepareStatement(baseQuery);
			statement.setString(1, ip);
			
			execute(statement);
			return true;
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(OtherData): "+e.getMessage());
		}
		return false;
	}
	
	public void loadCrafts() {
		try {
			ResultSet result = getData("SELECT * from crafts;");
			while (result.next()) {
				ArrayList<Couple<Integer, Integer>> m = new ArrayList<Couple<Integer, Integer>>();

				boolean cont = true;
				for (String str : result.getString("craft").split(";")) {
					try {
						int tID = Integer.parseInt(str.split("\\*")[0]);
						int qua = Integer.parseInt(str.split("\\*")[1]);
						m.add(new Couple<Integer, Integer>(tID, qua));
					} catch (Exception e) {
						e.printStackTrace();
						cont = false;
					}
					;
				}
				// s'il y a eu une erreur de parsing, on ignore cette recette
				if (!cont)
					continue;

				World.data.addCraft(result.getInt("id"), m);
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(OtherData): "+e.getMessage());
		}
	}
	
	public void reloadLiveActions() {
		/* Variables représentant les champs de la base */
		Personnage perso;
		int action, nombre, id;
		String sortie, couleur = "DF0101";
									
		ObjTemplate t;
		Objet obj;
		/* FIN */
		try {
			ResultSet result = getData("SELECT * from live_action;");

			while (result.next()) {
				perso = World.data.getPersonnage(result.getInt("PlayerID"));
				if (perso == null) {
					Log.addToShopLog("Personnage " + result.getInt("PlayerID")
							+ " non trouve, personnage non charge ?");
					continue;
				}
				if (!perso.isOnline()) {
					Log.addToShopLog("Personnage " + result.getInt("PlayerID")
							+ " hors ligne");
					continue;
				}
				if (perso.get_compte() == null) {
					Log.addToShopLog("Le Personnage " + result.getInt("PlayerID")
							+ " n'est attribue a aucun compte charge");
					continue;
				}
				if (perso.get_compte().getGameClient() == null) {
					Log.addToShopLog("Le Personnage "
							+ result.getInt("PlayerID")
							+ " n'a pas thread associe, le personnage est il hors ligne ?");
					continue;
				}
				if (perso.get_fight() != null)
					continue; // Perso en combat @ Nami-Doc
				action = result.getInt("Action");
				nombre = result.getInt("Nombre");
				id = result.getInt("ID");
				sortie = "+";

				switch (action) {
				case 1: // Monter d'un level
					if (perso.get_lvl() == World.data.getExpLevelSize())
						continue;
					for (int n = nombre; n > 1; n--)
						perso.levelUp(false, true);
					perso.levelUp(true, true);
					sortie += nombre + " Niveau(x)";
					break;
				case 2: // Ajouter X point d'experience
					if (perso.get_lvl() == World.data.getExpLevelSize())
						continue;
					perso.addXp(nombre);
					sortie += nombre + " Xp";
					break;
				case 3: // Ajouter X kamas
					perso.addKamas(nombre);
					sortie += nombre + " Kamas";
					break;
				case 4: // Ajouter X point de capital
					perso.addCapital(nombre);
					sortie += nombre + " Point(s) de capital";
					break;
				case 5: // Ajouter X point de sort
					perso.addSpellPoint(nombre);
					sortie += nombre + " Point(s) de sort";
					break;
				case 20: // Ajouter un item avec des jets aléatoire
					t = World.data.getObjTemplate(nombre);
					if (t == null)
						continue;
					obj = t.createNewItem(1, false); // Si mis à "true" l'objet
														// à des jets max. Sinon
														// ce sont des jets
														// aléatoire
					if (obj == null)
						continue;
					if (perso.addObjet(obj, true))// Si le joueur n'avait pas
													// d'item similaire
						World.data.addObjet(obj, true);
					Log.addToSockLog("Objet " + nombre + " ajouter a "
							+ perso.get_name() + " avec des stats aleatoire");
					SocketManager
							.GAME_SEND_MESSAGE(
									perso,
									"L'objet \""
											+ t.getName()
											+ "\" viens d'etre ajouter a votre personnage",
									couleur);
					break;
				case 21: // Ajouter un item avec des jets MAX
					t = World.data.getObjTemplate(nombre);
					if (t == null)
						continue;
					obj = t.createNewItem(1, true); // Si mis à "true" l'objet à
													// des jets max. Sinon ce
													// sont des jets aléatoire
					if (obj == null)
						continue;
					if (perso.addObjet(obj, true))// Si le joueur n'avait pas
													// d'item similaire
						World.data.addObjet(obj, true);
					Log.addToSockLog("Objet " + nombre + " ajoute a "
							+ perso.get_name() + " avec des stats MAX");
					SocketManager
							.GAME_SEND_MESSAGE(
									perso,
									"L'objet \""
											+ t.getName()
											+ "\" avec des stats maximum, viens d'etre ajoute a votre personnage",
									couleur);
					break;
				case 118:// Force
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " force";
					break;
				case 119:// Agilite
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " agilite";
					break;
				case 123:// Chance
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " chance";
					break;
				case 124:// Sagesse
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " sagesse";
					break;
				case 125:// Vita
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " vita";
					break;
				case 126:// Intelligence
					int statID = action;
					perso.get_baseStats().addOneStat(statID, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " intelligence";
					break;
				}
				SocketManager.GAME_SEND_STATS_PACKET(perso);
				if (action < 20 || action > 100)
					SocketManager.GAME_SEND_MESSAGE(perso, sortie
							+ " a votre personnage", couleur); // Si l'action
																// n'est pas un
																// ajout d'objet
																// on envoye un
																// message a
																// l'utilisateur

				Log.addToShopLog("(Commande " + id + ")Action " + action
						+ " Nombre: " + nombre
						+ " appliquee sur le personnage "
						+ result.getInt("PlayerID") + "(" + perso.get_name() + ")");
				try {
					PreparedStatement statement = connection.prepareStatement("DELETE FROM live_action WHERE ID=" + id);
					execute(statement);
					closeStatement(statement);
					Log.addToShopLog("Commande " + id + " supprimee.");
				} catch (Exception e) {
					Console.instance.writeln("SQL ERROR(OtherData): "+e.getMessage());
				}
				perso.save();
			}
			closeResultSet(result);
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(OtherData): "+e.getMessage());
		}
	}
	
	public String getNaturalStats(int id) {
		String stats = null;
		try {
			ResultSet result = getData("SELECT statsTemplate from `item_template` WHERE `id`='"+id+"';");
			if(result.next())
				stats = result.getString("statsTemplate");
		} catch (Exception e) {
			Console.instance.writeln("SQL ERROR(OtherData): "+e.getMessage());
		}
		return stats;
	}
}	
