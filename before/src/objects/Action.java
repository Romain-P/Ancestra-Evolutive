package objects;

import java.io.PrintWriter;

import objects.Metier.StatsMetier;
import objects.Monstre.MobGroup;
import objects.NPC_tmpl.NPC_question;
import objects.Objet.ObjTemplate;
import objects.Personnage.traque;

import common.Ancestra;
import common.ConditionParser;
import common.Constants;
import common.Formulas;
import common.SQLManager;
import common.SocketManager;
import common.World;

import game.GameServer;
import game.GameThread;

public class Action {

	private int ID;
	private String args;
	private String cond;
	
	public Action(int id, String args, String cond)
	{
		this.ID = id;
		this.args = args;
		this.cond = cond;
	}


	public void apply(Personnage perso, Personnage target, int itemID, int cellid)
	{
		if(perso == null)return;
		if(!cond.equalsIgnoreCase("") && !cond.equalsIgnoreCase("-1")&& !ConditionParser.validConditions(perso,cond))
		{
			SocketManager.GAME_SEND_Im_PACKET(perso, "119");
			return;
		}
		if(perso.get_compte().getGameThread() == null) return;
		PrintWriter out = perso.get_compte().getGameThread().get_out();	
		switch(ID)
		{
			case -2://créer guilde
				if(perso.is_away())return;
				if(perso.get_guild() != null || perso.getGuildMember() != null)
				{
					SocketManager.GAME_SEND_gC_PACKET(perso, "Ea");
					return;
				}
				SocketManager.GAME_SEND_gn_PACKET(perso);
			break;
			case -1://Ouvrir banque
				//Sauvagarde du perso et des item avant.
				SQLManager.SAVE_PERSONNAGE(perso,true);
				if(perso.getDeshonor() >= 1) 
				{
					SocketManager.GAME_SEND_Im_PACKET(perso, "183");
					return;
				}
				int cost = perso.getBankCost();
				if(cost > 0)
				{
					long nKamas = perso.get_kamas() - cost;
					if(nKamas <0)//Si le joueur n'a pas assez de kamas pour ouvrir la banque
					{
						SocketManager.GAME_SEND_Im_PACKET(perso, "1128;"+cost);
						return;
					}
					perso.set_kamas(nKamas);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					SocketManager.GAME_SEND_Im_PACKET(perso, "020;"+cost);
				}
				SocketManager.GAME_SEND_ECK_PACKET(perso.get_compte().getGameThread().get_out(), 5, "");
				SocketManager.GAME_SEND_EL_BANK_PACKET(perso);
				perso.set_away(true);
				perso.setInBank(true);
			break;
			
			case 0://Téléportation
				try
				{
					short newMapID = Short.parseShort(args.split(",",2)[0]);
					int newCellID = Integer.parseInt(args.split(",",2)[1]);
					
					perso.teleport(newMapID,newCellID);	
				}catch(Exception e ){return;};
			break;
			
			case 1://Discours NPC
				out = perso.get_compte().getGameThread().get_out();
				if(args.equalsIgnoreCase("DV"))
				{
					SocketManager.GAME_SEND_END_DIALOG_PACKET(out);
					perso.set_isTalkingWith(0);
				}else
				{
					int qID = -1;
					try
					{
						qID = Integer.parseInt(args);
					}catch(NumberFormatException e){};
					
					NPC_question  quest = World.getNPCQuestion(qID);
					if(quest == null)
					{
						SocketManager.GAME_SEND_END_DIALOG_PACKET(out);
						perso.set_isTalkingWith(0);
						return;
					}
					SocketManager.GAME_SEND_QUESTION_PACKET(out, quest.parseToDQPacket(perso));
				}
			break;
			
			case 4://Kamas
				try
				{
					int count = Integer.parseInt(args);
					long curKamas = perso.get_kamas();
					long newKamas = curKamas + count;
					if(newKamas <0) newKamas = 0;
					perso.set_kamas(newKamas);
					
					//Si en ligne (normalement oui)
					if(perso.isOnline())
						SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 5://objet
				try
				{
					int tID = Integer.parseInt(args.split(",")[0]);
					int count = Integer.parseInt(args.split(",")[1]);
					boolean send = true;
					if(args.split(",").length >2)send = args.split(",")[2].equals("1");
					
					//Si on ajoute
					if(count > 0)
					{
						ObjTemplate T = World.getObjTemplate(tID);
						if(T == null)return;
						Objet O = T.createNewItem(count, false);
						//Si retourne true, on l'ajoute au monde
						if(perso.addObjet(O, true))
							World.addObjet(O, true);
					}else
					{
						perso.removeByTemplateID(tID,-count);
					}
					//Si en ligne (normalement oui)
					if(perso.isOnline())//on envoie le packet qui indique l'ajout//retrait d'un item
					{
						SocketManager.GAME_SEND_Ow_PACKET(perso);
						if(send)
						{
							if(count >= 0){
								SocketManager.GAME_SEND_Im_PACKET(perso, "021;"+count+"~"+tID);
							}
							else if(count < 0){
								SocketManager.GAME_SEND_Im_PACKET(perso, "022;"+-count+"~"+tID);
							}
						}
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 6://Apprendre un métier
				try
				{
					int mID = Integer.parseInt(args);
					if(World.getMetier(mID) == null)return;
					// Si c'est un métier 'basic' :
					if(mID == 	2 || mID == 11 ||
					   mID == 13 || mID == 14 ||
					   mID == 15 || mID == 16 ||
					   mID == 17 || mID == 18 ||
					   mID == 19 || mID == 20 ||
					   mID == 24 || mID == 25 ||
					   mID == 26 || mID == 27 ||
					   mID == 28 || mID == 31 ||
					   mID == 36 || mID == 41 ||
					   mID == 56 || mID == 58 ||
					   mID == 60 || mID == 65)
					{
						if(perso.getMetierByID(mID) != null)//Métier déjà appris
						{
							SocketManager.GAME_SEND_Im_PACKET(perso, "111");
						}
						
						if(perso.totalJobBasic() > 2)//On compte les métiers déja acquis si c'est supérieur a 2 on ignore
						{
							SocketManager.GAME_SEND_Im_PACKET(perso, "19");
						}else//Si c'est < ou = à 2 on apprend
						{
							perso.learnJob(World.getMetier(mID));
						}
					}
					// Si c'est une specialisations 'FM' :
					if(mID == 	43 || mID == 44 ||
					   mID == 45 || mID == 46 ||
					   mID == 47 || mID == 48 ||
					   mID == 49 || mID == 50 ||
					   mID == 62 || mID == 63 ||
					   mID == 64)
					{
						//Métier simple level 65 nécessaire
						if(perso.getMetierByID(17) != null && perso.getMetierByID(17).get_lvl() >= 65 && mID == 43
						|| perso.getMetierByID(11) != null && perso.getMetierByID(11).get_lvl() >= 65 && mID == 44
						|| perso.getMetierByID(14) != null && perso.getMetierByID(14).get_lvl() >= 65 && mID == 45
						|| perso.getMetierByID(20) != null && perso.getMetierByID(20).get_lvl() >= 65 && mID == 46
						|| perso.getMetierByID(31) != null && perso.getMetierByID(31).get_lvl() >= 65 && mID == 47
						|| perso.getMetierByID(13) != null && perso.getMetierByID(13).get_lvl() >= 65 && mID == 48
						|| perso.getMetierByID(19) != null && perso.getMetierByID(19).get_lvl() >= 65 && mID == 49
						|| perso.getMetierByID(18) != null && perso.getMetierByID(18).get_lvl() >= 65 && mID == 50
						|| perso.getMetierByID(15) != null && perso.getMetierByID(15).get_lvl() >= 65 && mID == 62
						|| perso.getMetierByID(16) != null && perso.getMetierByID(16).get_lvl() >= 65 && mID == 63
						|| perso.getMetierByID(27) != null && perso.getMetierByID(27).get_lvl() >= 65 && mID == 64)
						{
							//On compte les specialisations déja acquis si c'est supérieur a 2 on ignore
							if(perso.getMetierByID(mID) != null)//Métier déjà appris
							{
								SocketManager.GAME_SEND_Im_PACKET(perso, "111");
							}
							
							if(perso.totalJobFM() > 2)//On compte les métiers déja acquis si c'est supérieur a 2 on ignore
							{
								SocketManager.GAME_SEND_Im_PACKET(perso, "19");
							}
							else//Si c'est < ou = à 2 on apprend
							{
								perso.learnJob(World.getMetier(mID));
								perso.getMetierByID(mID).addXp(perso, 582000);//Level 100 direct
							}	
						}else
						{
							SocketManager.GAME_SEND_Im_PACKET(perso, "12");
						}
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 7://retour au point de sauvegarde
				perso.warpToSavePos();
			break;
			case 8://Ajouter une Stat
				try
				{
					int statID = Integer.parseInt(args.split(",",2)[0]);
					int number = Integer.parseInt(args.split(",",2)[1]);
					perso.get_baseStats().addOneStat(statID, number);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					int messID = 0;
					switch(statID)
					{
						case Constants.STATS_ADD_INTE: messID = 14;break;
					}
					if(messID>0)
						SocketManager.GAME_SEND_Im_PACKET(perso, "0"+messID+";"+number);
				}catch(Exception e ){return;};
			break;
			case 9://Apprendre un sort
				try
				{
					int sID = Integer.parseInt(args);
					if(World.getSort(sID) == null)return;
					perso.learnSpell(sID,1, true,true);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 10://Pain/potion/viande/poisson
				try
				{
					int min = Integer.parseInt(args.split(",",2)[0]);
					int max = Integer.parseInt(args.split(",",2)[1]);
					if(max == 0) max = min;
					int val = Formulas.getRandomValue(min, max);
					if(target != null)
					{
						if(target.get_PDV() + val > target.get_PDVMAX())val = target.get_PDVMAX()-target.get_PDV();
						target.set_PDV(target.get_PDV()+val);
						SocketManager.GAME_SEND_STATS_PACKET(target);
					}
					else
					{
						if(perso.get_PDV() + val > perso.get_PDVMAX())val = perso.get_PDVMAX()-perso.get_PDV();
						perso.set_PDV(perso.get_PDV()+val);
						SocketManager.GAME_SEND_STATS_PACKET(perso);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 11://Definir l'alignement
				try
				{
					byte newAlign = Byte.parseByte(args.split(",",2)[0]);
					boolean replace = Integer.parseInt(args.split(",",2)[1]) == 1;
					//Si le perso n'est pas neutre, et qu'on doit pas remplacer, on passe
					if(perso.get_align() != Constants.ALIGNEMENT_NEUTRE && !replace)return;
					perso.modifAlignement(newAlign);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			/* TODO: autres actions */
			case 12://Spawn d'un groupe de monstre
				try
				{
					boolean delObj = args.split(",")[0].equals("true");
					boolean inArena = args.split(",")[1].equals("true");

					if(inArena && !World.isArenaMap(perso.get_curCarte().get_id()))return;	//Si la map du personnage n'est pas classé comme étant dans l'arène

					PierreAme pierrePleine = (PierreAme)World.getObjet(itemID);

					String groupData = pierrePleine.parseGroupData();
					String condition = "MiS = "+perso.get_GUID();	//Condition pour que le groupe ne soit lançable que par le personnage qui à utiliser l'objet
					perso.get_curCarte().spawnNewGroup(true, perso.get_curCell().getID(), groupData,condition);

					if(delObj)
					{
						perso.removeItem(itemID, 1, true, true);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
		    case 13: //Reset Carac
		        try
		        {
		          perso.get_baseStats().addOneStat(125, -perso._baseStats.getEffect(125));
		          perso.get_baseStats().addOneStat(124, -perso._baseStats.getEffect(124));
		          perso.get_baseStats().addOneStat(118, -perso._baseStats.getEffect(118));
		          perso.get_baseStats().addOneStat(123, -perso._baseStats.getEffect(123));
		          perso.get_baseStats().addOneStat(119, -perso._baseStats.getEffect(119));
		          perso.get_baseStats().addOneStat(126, -perso._baseStats.getEffect(126));
		          perso.addCapital((perso.get_lvl() - 1) * 5 - perso.get_capital());

		          SocketManager.GAME_SEND_STATS_PACKET(perso);
		        }catch(Exception e){GameServer.addToLog(e.getMessage());};
		    break;
		    case 14://Ouvrir l'interface d'oublie de sort
		    	perso.setisForgetingSpell(true);
				SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', perso);
			break;
			case 15://Téléportation donjon
				try
				{
					short newMapID = Short.parseShort(args.split(",")[0]);
					int newCellID = Integer.parseInt(args.split(",")[1]);
					int ObjetNeed = Integer.parseInt(args.split(",")[2]);
					int MapNeed = Integer.parseInt(args.split(",")[3]);
					if(ObjetNeed == 0)
					{
						//Téléportation sans objets
						perso.teleport(newMapID,newCellID);
					}else if(ObjetNeed > 0)
					{
					if(MapNeed == 0)
					{
						//Téléportation sans map
						perso.teleport(newMapID,newCellID);
					}else if(MapNeed > 0)
					{
					if (perso.hasItemTemplate(ObjetNeed, 1) && perso.get_curCarte().get_id() == MapNeed)
					{
						//Le perso a l'item
						//Le perso est sur la bonne map
						//On téléporte, on supprime après
						perso.teleport(newMapID,newCellID);
						perso.removeByTemplateID(ObjetNeed, 1);
						SocketManager.GAME_SEND_Ow_PACKET(perso);
					}
					else if(perso.get_curCarte().get_id() != MapNeed)
					{
						//Le perso n'est pas sur la bonne map
						SocketManager.GAME_SEND_MESSAGE(perso, "Vous n'etes pas sur la bonne map du donjon pour etre teleporter.", "009900");
					}
					else
					{
						//Le perso ne possède pas l'item
						SocketManager.GAME_SEND_MESSAGE(perso, "Vous ne possedez pas la clef necessaire.", "009900");
					}
					}
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 16://Ajout d'honneur HonorValue
				try
				{
					if(perso.get_align() != 0)
					{
						int AddHonor = Integer.parseInt(args);
						int ActualHonor = perso.get_honor();
						perso.set_honor(ActualHonor+AddHonor);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 17://Xp métier JobID,XpValue
				try
				{
					int JobID = Integer.parseInt(args.split(",")[0]);
					int XpValue = Integer.parseInt(args.split(",")[1]);
					if(perso.getMetierByID(JobID) != null)
					{
						perso.getMetierByID(JobID).addXp(perso, XpValue);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 18://Téléportation chez sois
				if(House.AlreadyHaveHouse(perso))//Si il a une maison
				{
					Objet obj = World.getObjet(itemID);
					if (perso.hasItemTemplate(obj.getTemplate().getID(), 1))
					{
						perso.removeByTemplateID(obj.getTemplate().getID(),1);
						House h = House.get_HouseByPerso(perso);
						if(h == null) return;
						perso.teleport((short)h.get_mapid(), h.get_caseid());
					}
				}
			break;
			case 19://Téléportation maison de guilde (ouverture du panneau de guilde)
				SocketManager.GAME_SEND_GUILDHOUSE_PACKET(perso);
			break;
			case 20://+Points de sorts
				try
				{
					int pts = Integer.parseInt(args);
					if(pts < 1) return;
					perso.addSpellPoint(pts);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 21://+Energie
				try
				{
					int Energy = Integer.parseInt(args);
					if(Energy < 1) return;
					
					int EnergyTotal = perso.get_energy()+Energy;
					if(EnergyTotal > 10000) EnergyTotal = 10000;
					
					perso.set_energy(EnergyTotal);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 22://+Xp
				try
				{
					long XpAdd = Integer.parseInt(args);
					if(XpAdd < 1) return;
					
					long TotalXp = perso.get_curExp()+XpAdd;
					perso.set_curExp(TotalXp);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 23://UnlearnJob
				try
				{
					int Job = Integer.parseInt(args);
					if(Job < 1) return;
					StatsMetier m = perso.getMetierByID(Job);
					if(m == null) return;
					perso.unlearnJob(m.getID());
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					SQLManager.SAVE_PERSONNAGE(perso, false);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 24://SimpleMorph
				try
				{
					int morphID = Integer.parseInt(args);
					if(morphID < 0)return;
					perso.set_gfxID(morphID);
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
					SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.get_curCarte(), perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 25://SimpleUnMorph
				int UnMorphID = perso.get_classe()*10 + perso.get_sexe();
				perso.set_gfxID(UnMorphID);
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
				SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.get_curCarte(), perso);
			break;
			case 26://Téléportation enclo de guilde (ouverture du panneau de guilde)
				SocketManager.GAME_SEND_GUILDENCLO_PACKET(perso);
			break;
			case 27://startFigthVersusMonstres args : monsterID,monsterLevel| ...
				String ValidMobGroup = "";
				try
		        {
					for(String MobAndLevel : args.split("\\|"))
					{
						int monsterID = -1;
						int monsterLevel = -1;
						String[] MobOrLevel = MobAndLevel.split(",");
						monsterID = Integer.parseInt(MobOrLevel[0]);
						monsterLevel = Integer.parseInt(MobOrLevel[1]);
						
						if(World.getMonstre(monsterID) == null || World.getMonstre(monsterID).getGradeByLevel(monsterLevel) == null)
						{
							if(Ancestra.CONFIG_DEBUG) GameServer.addToLog("Monstre invalide : monsterID:"+monsterID+" monsterLevel:"+monsterLevel);
							continue;
						}
						ValidMobGroup += monsterID+","+monsterLevel+","+monsterLevel+";";
					}
					if(ValidMobGroup.isEmpty()) return;
					MobGroup group  = new MobGroup(perso.get_curCarte()._nextObjectID,perso.get_curCell().getID(),ValidMobGroup);
					perso.get_curCarte().startFigthVersusMonstres(perso, group);
		        }catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 50://Traque
				if(perso.get_traque() == null)
				{
					traque traq = new traque(0, null);
					perso.set_traque(traq);
				}
				if(perso.get_traque().get_time() < System.currentTimeMillis() - 600000 || perso.get_traque().get_time() == 0)
				{
					Personnage tempP = null;
					int tmp = 15;
					int diff = 0;
					for(byte b = 0; b < 100; b++)
					{
					if(b == Ancestra.gameServer.getClients().size())break;
					GameThread GT = Ancestra.gameServer.getClients().get(b);
					Personnage P = GT.getPerso();
					if(P == null || P == perso)continue;
					if(P.get_compte().get_curIP().compareTo(perso.get_compte().get_curIP()) == 0)continue;
					//SI pas sériane ni neutre et si alignement opposé
					if(P.get_align() == perso.get_align() || P.get_align() == 0 || P.get_align() == 3)continue;
					
					if(P.get_lvl()>perso.get_lvl())diff = P.get_lvl() - perso.get_lvl();
					if(perso.get_lvl()>P.get_lvl())diff = perso.get_lvl() - P.get_lvl();
					if(diff<tmp)tempP = P; tmp = diff;
					}
					if(tempP == null)
					{
						SocketManager.GAME_SEND_MESSAGE(perso, "Nous n'avons pas trouve de cible a ta hauteur. Reviens plus tard." , "000000");
						break;
					}
					
					
					SocketManager.GAME_SEND_MESSAGE(perso, "Vous etes desormais en chasse de "+tempP.get_name()+"." , "000000");
					
					perso.get_traque().set_traqued(tempP);
					perso.get_traque().set_time(System.currentTimeMillis());
					
					
					ObjTemplate T = World.getObjTemplate(10085);
					if(T == null)return;
					perso.removeByTemplateID(T.getID(),100);
					
					Objet newObj = T.createNewItem(20, false);
					//On ajoute le nom du type à recherché
					/*
					newObj.addTxtStat(962, Integer.toString(tempP.get_lvl()));
					newObj.addTxtStat(961, Integer.toString(tempP.getGrade()));
					
					int alignid = tempP.get_align();
					String align = "";
					switch(alignid)
					{
					case 0:
					align = "Neutre";
					case 1:
					align = "Bontarien";
					break;
					case 2:
					align = "Brakmarien";
					break;
					case 3:
					align = "Sériane";
					break;
					}
					newObj.addTxtStat(960, align);
					*/
					newObj.addTxtStat(989, tempP.get_name());
					
					//Si retourne true, on l'ajoute au monde
					if(perso.addObjet(newObj, true)){
						World.addObjet(newObj, true);
			}else
			{
				perso.removeByTemplateID(T.getID(),20);
			}
			}
			else{
			SocketManager.GAME_SEND_MESSAGE(perso, "Thomas Sacre : Vous venez juste de signer un contrat, vous devez vous reposer." , "000000");
				}

			break;
			case 51://Cible sur la géoposition
				String perr = "";
				
				perr = World.getObjet(itemID).getTraquedName();
				if(perr == null)
				{
					break;	
				}
				Personnage cible = World.getPersoByName(perr);
				if(cible==null)break;
				if(!cible.isOnline())
				{
					SocketManager.GAME_SEND_MESSAGE(perso, "Ce joueur n'est pas connecte." , "000000");
					break;
				}
				SocketManager.GAME_SEND_FLAG_PACKET(perso, cible);
			break;
			case 52://recompenser pour traque
				if(perso.get_traque() != null && perso.get_traque().get_time() == -2)
				{
					int xp = Formulas.getTraqueXP(perso.get_lvl());
					perso.addXp(xp);
					perso.set_traque(null);//On supprime la traque
					SocketManager.GAME_SEND_MESSAGE(perso, "Vous venez de recevoir "+xp+" points d'experiences." , "000000");
				}
				else
				{
					SocketManager.GAME_SEND_MESSAGE(perso, "Thomas Sacre : Reviens me voir quand tu aura abatu un ennemi." , "000000");
				}

			break;
			case 101://Arriver sur case de mariage
				if((perso.get_sexe() == 0 && perso.get_curCell().getID() == 282) || (perso.get_sexe() == 1 && perso.get_curCell().getID() == 297))
				{
					World.AddMarried(perso.get_sexe(), perso);
				}else 
				{
					SocketManager.GAME_SEND_Im_PACKET(perso, "1102");
				}
			break;
			case 102://Marier des personnages
				World.PriestRequest(perso, perso.get_curCarte(), perso.get_isTalkingWith());
			break;
			case 103://Divorce
				if(perso.get_kamas() < 50000)
				{
					return;
				}else
				{
					perso.set_kamas(perso.get_kamas()-50000);
					Personnage wife = World.getPersonnage(perso.getWife());
					wife.Divorce();
					perso.Divorce();
				}
			break;
			case 228://Faire animation Hors Combat
				try
				{
					int AnimationId = Integer.parseInt(args);
					Animations animation = World.getAnimation(AnimationId);
					if(perso.get_fight() != null) return;
					perso.changeOrientation(1);
					SocketManager.GAME_SEND_GA_PACKET_TO_MAP(perso.get_curCarte(), "0", 228, perso.get_GUID()+";"+cellid+","+Animations.PrepareToGA(animation), "");
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			default:
				GameServer.addToLog("Action ID="+ID+" non implantee");
			break;
		}
	}


	public int getID()
	{
		return ID;
	}
}
