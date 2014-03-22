package objects;

import game.GameServer;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import objects.Carte.Case;
import objects.Fight.Fighter;
import objects.Fight.Glyphe;
import objects.Fight.Piege;
import objects.Monstre.MobGrade;
import objects.Sort.SortStats;

import common.Constants;
import common.CryptManager;
import common.Formulas;
import common.Pathfinding;
import common.SocketManager;
import common.World;

public class SpellEffect
	{
		private int effectID;
		private int turns = 0;
		private String jet = "0d0+0";
		private int chance = 100;
		private String args;
		private int value = 0;
		private Fighter caster = null;
		private int spell = 0;
		private int spellLvl = 1;
		private boolean debuffable = true;
		private int duration = 0;
		private Case cell = null;
		
		public SpellEffect(int aID,String aArgs,int aSpell,int aSpellLevel)
		{
			effectID = aID;
			args = aArgs;
			spell = aSpell;
			spellLvl = aSpellLevel;
			try
			{
				value = Integer.parseInt(args.split(";")[0]);
				
				turns = Integer.parseInt(args.split(";")[3]);
				chance= Integer.parseInt(args.split(";")[4]);
				jet = args.split(";")[5];
				
			}catch(Exception e){};
		}
				
		public SpellEffect(int id, int value2, int aduration, int turns2, boolean debuff,Fighter aCaster, String args2, int aspell)
		{
			effectID = id;
			value = value2;
			turns = turns2;
			debuffable = debuff;
			caster = aCaster;
			duration = aduration;
			args = args2;
			spell = aspell;
			try
			{
				jet = args.split(";")[5];
			}catch(Exception e){};
		}

		public boolean getSpell2(int id)
		{
			if(spell == id)
			{
			return true;
			}
			else
			{
			return false;
			}
		}
		
		public int getDuration()
		{
			return duration;
		}
		
		public int getTurn() {
			return turns;
		}
		
		public boolean isDebuffabe()
		{
			return debuffable;
		}
		
		public void setTurn(int turn) {
			this.turns = turn;
		}

		public int getEffectID() {
			return effectID;
		}

		public String getJet() {
			return jet;
		}

		public int getValue() {
			return value;
		}
		public int getChance() {
			return chance;
		}

		public String getArgs() {
			return args;
		}

		public static ArrayList<Fighter> getTargets(SpellEffect SE,Fight fight, ArrayList<Case> cells)
		{
			ArrayList<Fighter> cibles = new ArrayList<Fighter>(); 
			for(Case aCell : cells)
			{
				if(aCell == null)continue;
				Fighter f = aCell.getFirstFighter();
				if(f == null)continue;
				cibles.add(f);
			}
			return cibles;
		}
		
		public void setValue(int i)
		{
			value = i;
		}

		public int decrementDuration()
		{
			duration -= 1;
			return duration;
		}

		public void applyBeginingBuff(Fight _fight, Fighter fighter)
		{
			ArrayList<Fighter> cible = new ArrayList<Fighter>();
			cible.add(fighter);
			turns = -1;
			applyToFight(_fight,caster,cible,false);
		}
		
		public void applyToFight(Fight fight, Fighter perso,Case Cell,ArrayList<Fighter> cibles)
		{
			cell = Cell;
			applyToFight(fight,perso,cibles,false);
		}

		public static int applyOnHitBuffs(int finalDommage,Fighter target,Fighter caster,Fight fight)
		{
			for(int id : Constants.ON_HIT_BUFFS)
			{
				for(SpellEffect buff : target.getBuffsByEffectID(id))
				{
					switch(id)
					{
						case 9://Derobade
							//Si pas au cac (distance == 1)
							int d = Pathfinding.getDistanceBetween(fight.get_map(), target.get_fightCell().getID(), caster.get_fightCell().getID());
							if(d >1)continue;
							int chan = buff.getValue();
							int c = Formulas.getRandomValue(0, 99);
							if(c+1 >= chan)continue;//si le deplacement ne s'applique pas
							int nbrCase = 0;
							try
							{
								nbrCase = Integer.parseInt(buff.getArgs().split(";")[1]);	
							}catch(Exception e){};
							if(nbrCase == 0)continue;
							int exCase = target.get_fightCell().getID();
							int newCellID = Pathfinding.newCaseAfterPush(fight.get_map(), caster.get_fightCell(), target.get_fightCell(), nbrCase);
							if(newCellID <0)//S'il a été bloqué
							{
								int a = -newCellID;
								a = nbrCase-a;
								newCellID =	Pathfinding.newCaseAfterPush(fight.get_map(),caster.get_fightCell(),target.get_fightCell(),a);
								if(newCellID == 0)
									continue;
								if(fight.get_map().getCase(newCellID) == null)
									continue;
							}
							target.get_fightCell().getFighters().clear();
							target.set_fightCell(fight.get_map().getCase(newCellID));
							target.get_fightCell().addFighter(target);
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 5, target.getGUID()+"", target.getGUID()+","+newCellID);
							
							ArrayList<Piege> P = (new ArrayList<Piege>());
							P.addAll(fight.get_traps());
							for(Piege p : P)
							{
								int dist = Pathfinding.getDistanceBetween(fight.get_map(),p.get_cell().getID(),target.get_fightCell().getID());
								//on active le piege
								if(dist <= p.get_size())p.onTraped(target);
							}
							//si le joueur a bouger
							if(exCase != newCellID)
								finalDommage = 0;
						break;
							
						case 79://chance éca
							try
							{
								String[] infos = buff.getArgs().split(";");
								int coefDom = Integer.parseInt(infos[0]);
								int coefHeal = Integer.parseInt(infos[1]);
								int chance = Integer.parseInt(infos[2]);
								int jet = Formulas.getRandomValue(0, 99);
								
								if(jet < chance)//Soin
								{
									finalDommage = -(finalDommage*coefHeal);
									if(-finalDommage > (target.getPDVMAX() - target.getPDV()))finalDommage = -(target.getPDVMAX() - target.getPDV());
								}else//Dommage
									finalDommage = finalDommage*coefDom;
							}catch(Exception e){};
						break;
						
						case 107://renvoie Dom
							String[] args = buff.getArgs().split(";");
							float coef = 1+(target.getTotalStats().getEffect(Constants.STATS_ADD_SAGE)/100);
							int renvoie = 0;
							try
							{
								if(Integer.parseInt(args[1]) != -1)
								{
									renvoie = (int)(coef * Formulas.getRandomValue(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
								}else
								{
									renvoie = (int)(coef * Integer.parseInt(args[0]));
								}
							}catch(Exception e){return finalDommage;};
							if(renvoie > finalDommage)renvoie = finalDommage;
							finalDommage -= renvoie;
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 107, "-1", target.getGUID()+","+renvoie);
							if(renvoie>caster.getPDV())renvoie = caster.getPDV();
							if(finalDommage<0)finalDommage =0;
							caster.removePDV(renvoie);
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", caster.getGUID()+",-"+renvoie);
						break;
						
						case 788://Chatiments
							int taux = (caster.getPersonnage() == null?1:2);
							int gain = finalDommage / taux;
							int stat = buff.getValue();
							int max = 0;
							try
							{
								max = Integer.parseInt(buff.getArgs().split(";")[1]);
							}catch(Exception e){};
							if(max == 0)continue;
							if(stat == 108)
							{
								target.addBuff(stat, max, 5, 1, false, buff.getSpell(), buff.getArgs(), caster);
								SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, stat, caster.getGUID()+"", target.getGUID()+","+max+","+5);
								target.addPDV(max);
								target.get_chatiValue().put(stat, max);
								break;
							}
							//on retire au max possible la valeur déjà gagné sur le chati
							int a = (target.get_chatiValue().get(stat)==null?0:target.get_chatiValue().get(stat));
							max -= a;
							//Si gain trop grand, on le reduit au max
							if(gain > max)gain = max;
							
							//on ajoute le buff
							target.addBuff(stat, gain, 5, 1, false, buff.getSpell(), buff.getArgs(), caster);
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, stat, caster.getGUID()+"", target.getGUID()+","+gain+","+5);
							//On met a jour les valeurs des chatis
							int value = a + gain;
							target.get_chatiValue().put(stat, value);
						break;
						
						default:
							GameServer.addToLog("Effect id "+id+" definie comme ON_HIT_BUFF mais n'a pas d'effet definie dans ce gestionnaire.");
						break;
					}
				}
			}
			
			return finalDommage;
		}
		
		public Fighter getCaster() {
			return caster;
		}

		public int getSpell() {
			return spell;
		}
		
		public void applyToFight(Fight fight,Fighter acaster, ArrayList<Fighter> cibles, boolean isCaC)
		{
			GameServer.addToLog("Effet id: "+effectID+" Args: "+args+" turns: "+turns+" cibles: "+cibles.size()+" chance: "+chance);
			try
			{
				if(turns != -1)//Si ce n'est pas un buff qu'on applique en début de tour
					turns = Integer.parseInt(args.split(";")[3]);
			}catch(NumberFormatException e){}
			caster = acaster;
			switch(effectID)
			{
				case 4://Fuite/Bond du félin/ Bond du iop / téléport
					applyEffect_4(fight,cibles);
				break;
				case 5://Repousse de X case
					applyEffect_5(cibles,fight);
				break;
				case 6://Attire de X case
					applyEffect_6(cibles,fight);
				break;
				
				case 8://Echange les place de 2 joueur
					applyEffect_8(cibles,fight);
				break;
				case 9://Esquive une attaque en reculant de 1 case
					applyEffect_9(cibles,fight);
				break;
				
				case 50://Porter
					applyEffect_50(fight);
				break;
				case 51://jeter
					applyEffect_51(fight);
				break;
				
				case 77://Vol de PM
					applyEffect_77(cibles,fight);
				break;
				case 78://Bonus PM
					applyEffect_78(cibles,fight);
				break;
				case 79:// + X chance(%) dommage subis * Y sinon soigné de dommage *Z
					applyEffect_79(cibles,fight);
				break;
				
				case 82://Vol de Vie fixe
					applyEffect_82(cibles,fight);
				break;
				
				case 84://Vol de PA
					applyEffect_84(cibles,fight);
				break;
				case 85://Dommage Eau %vie
					applyEffect_85(cibles,fight);
				break;
				case 86://Dommage Terre %vie
					applyEffect_86(cibles,fight);
				break;
				case 87://Dommage Air %vie
					applyEffect_87(cibles,fight);
				break;
				case 88://Dommage feu %vie
					applyEffect_88(cibles,fight);
				break;
				case 89://Dommage neutre %vie
					applyEffect_89(cibles,fight);
				break;
				case 90://Donne X% de sa vie
					applyEffect_90(cibles,fight);
				break;
				case 91://Vol de Vie Eau
					applyEffect_91(cibles,fight,isCaC);
				break;
				case 92://Vol de Vie Terre
					applyEffect_92(cibles,fight,isCaC);
				break;
				case 93://Vol de Vie Air
					applyEffect_93(cibles,fight,isCaC);
				break;
				case 94://Vol de Vie feu
					applyEffect_94(cibles,fight,isCaC);
				break;
				case 95://Vol de Vie neutre
					applyEffect_95(cibles,fight,isCaC);
				break;
				case 96://Dommage Eau
					applyEffect_96(cibles,fight,isCaC);
				break;
				case 97://Dommage Terre 
					applyEffect_97(cibles,fight,isCaC);
				break; 
				case 98://Dommage Air 
					applyEffect_98(cibles,fight,isCaC);
				break;
				case 99://Dommage feu 
					applyEffect_99(cibles,fight,isCaC);
				break;
				case 100://Dommage neutre
					applyEffect_100(cibles,fight,isCaC);
				break;
				case 101://Retrait PA
					applyEffect_101(cibles,fight);
				break;
				
				case 105://Dommages réduits de X
					applyEffect_105(cibles,fight);
				break;
				case 106://Renvoie de sort
					applyEffect_106(cibles,fight);
				break;
				case 107://Renvoie de dom
					applyEffect_107(cibles,fight);
				break;
				case 108://Soin
					applyEffect_108(cibles,fight);
				break;
				case 109://Dommage pour le lanceur
					applyEffect_109(fight);
				break;
				case 110://+ X vie
					applyEffect_110(cibles,fight);
				break;
				case 111://+ X PA
					applyEffect_111(cibles,fight);
				break;
				case 112://+Dom
					applyEffect_112(cibles,fight);
				break;
				
				case 114://Multiplie les dommages par X
					applyEffect_114(cibles,fight);
				break;
				case 115://+Cc
					applyEffect_115(cibles,fight);
				break;
				case 116://Malus PO
					applyEffect_116(cibles,fight);
				break;
				case 117://Bonus PO
					applyEffect_117(cibles,fight);
				break;
				case 118://Bonus force
					applyEffect_118(cibles,fight);
				break;
				case 119://Bonus Agilité
					applyEffect_119(cibles,fight);
				break;
				case 120://Bonus PA
					applyEffect_120(cibles,fight);
				break;
				case 121://+Dom
					applyEffect_121(cibles,fight);
				break;
				case 122://+EC
					applyEffect_122(cibles,fight);
				break;
				case 123://+Chance
					applyEffect_123(cibles,fight);
				break;
				case 124://+Sagesse
					applyEffect_124(cibles,fight);
				break;
				case 125://+Vitalité
					applyEffect_125(cibles,fight);
				break;
				case 126://+Intelligence
					applyEffect_126(cibles,fight);
				break;
				case 127://Retrait PM
					applyEffect_127(cibles,fight);
				break;
				case 128://+PM
					applyEffect_128(cibles,fight);
				break;
				
				case 131://Poison : X Pdv  par PA
					applyEffect_131(cibles,fight);
				break;
				case 132://Enleve les envoutements
					applyEffect_132(cibles,fight);
				break;
				
				case 138://%dom
					applyEffect_138(cibles,fight);
				break;
				
				case 140://Passer le tour
					applyEffect_140(cibles,fight);
				break;
				case 141://Tue la cible
					applyEffect_141(fight,cibles);
				break;
				case 142://Dommages physique
					applyEffect_142(fight,cibles);
				break;
				
				case 145://Malus Dommage
					applyEffect_145(fight,cibles);
				break;
				
				case 149://Change l'apparence
					applyEffect_149(fight,cibles);
				break;
				case 150://Invisibilité
					applyEffect_150(fight,cibles);
				break;
				
				case 155:// - Intell
					applyEffect_155(fight,cibles);
				break;
				
				case 160:// + Esquive PA
					applyEffect_160(fight,cibles);
				break;
				case 161:// + Esquive PM
					applyEffect_161(fight,cibles);
				break;
				case 162:// - Esquive PA
					applyEffect_162(fight,cibles);
				break;
				case 163:// - Esquive PM
					applyEffect_163(fight,cibles);
				break;
				case 165:// Maîtrises
					applyEffect_165(fight,cibles);
				break;
				
				case 168://Perte PA non esquivable
					applyEffect_168(fight,cibles);
				break;
				case 169://Perte PM non esquivable
					applyEffect_169(fight,cibles);
				break;
				
				case 171://Malus CC
					applyEffect_171(fight,cibles);
				break;

				case 180://Double du sram
					applyEffect_180(fight);
				break;
				case 181://Invoque une créature
					applyEffect_181(fight);
				break;
				case 182://+ Crea Invoc
					applyEffect_182(fight,cibles);
				break;
				case 183://Resist Magique
					applyEffect_183(fight,cibles);
				break;
				case 184://Resist Physique
					applyEffect_184(fight,cibles);
				break;
				case 185://Invoque une creature statique
					applyEffect_185(fight);
				break;
				
				case 202://Perception
					applyEffect_202(fight, cibles);
				break;
				
				case 210://Resist % terre
					applyEffect_210(fight,cibles);
				break;
				case 211://Resist % eau
					applyEffect_211(fight,cibles);
				break;
				case 212://Resist % air
					applyEffect_212(fight,cibles);
				break;
				case 213://Resist % feu
					applyEffect_213(fight,cibles);
				break;
				case 214://Resist % neutre
					applyEffect_214(fight,cibles);
				break;
				case 215://Faiblesse % terre
					applyEffect_215(fight,cibles);
				break;
				case 216://Faiblesse % eau
					applyEffect_216(fight,cibles);
				break;
				case 217://Faiblesse % air
					applyEffect_217(fight,cibles);
				break;
				case 218://Faiblesse % feu
					applyEffect_218(fight,cibles);
				break;
				case 219://Faiblesse % neutre
					applyEffect_219(fight,cibles);
				break;
				
				case 265://Reduit les Dom de X
					applyEffect_265(fight,cibles);
				break;
				case 266://Vol Chance
					applyEffect_266(fight,cibles);
				break;
				case 267://Vol vitalité
					applyEffect_267(fight,cibles);
				break;
				case 268://Vol agitlité
					applyEffect_268(fight,cibles);
				break;
				case 269://Vol intell
					applyEffect_269(fight,cibles);
				break;
				case 270://Vol sagesse
					applyEffect_270(fight,cibles);
				break;
				case 271://Vol force
					applyEffect_271(fight,cibles);
				break;
				
				case 293://Augmente les dégâts de base du sort X de Y
					applyEffect_293(fight);
				break;
				
				case 320://Vol de PO
					applyEffect_320(fight,cibles);
				break;
				
				case 400://Créer un  piège
					applyEffect_400(fight);
				break;
				case 401://Créer un glyphe
					applyEffect_401(fight);
				break;
				
				case 402://Glyphe des Blop
					applyEffect_402(fight);
				break;
				
				case 666://Pas d'effet complémentaire
				break;
				
				case 672://Dommages : X% de la vie de l'attaquant (neutre)
					applyEffect_672(cibles,fight);
				break;
				
				case 765://sacrifice
					applyEffect_765(cibles,fight);
				break;
				
				case 780://laisse spirituelle
					applyEffect_780(fight);
				break;
				
				case 782://Maximise les effets aléatoires
					applyEffect_782(cibles,fight);
				break;
				
				case 783://Pousse jusqu'a la case visé
					applyEffect_783(cibles,fight);
				break;

				case 788://Chatiment de X sur Y tours
					applyEffect_788(cibles,fight);
				break;
				
				case 950://Etat X
					applyEffect_950(fight,cibles);
				break;
				case 951://Enleve l'Etat X
					applyEffect_951(fight,cibles);
				break;
				
				default:
					GameServer.addToLog("effet non implante : "+effectID+" args: "+args);
				break;
			}
		}

		private void applyEffect_202(Fight fight, ArrayList<Fighter> cibles) 
		{
			// TODO A tester !
			if(spell == 113)
			{
				//unhide des personnages
				for(Fighter target : cibles)
				{
					if(target.isHide()) target.unHide(spell);
				}
				//unhide des pièges
				for(Piege p : fight.get_traps())
				{
					p.set_isunHide(caster);
					p.appear(caster);
				}
			}
		}

		private void applyEffect_782(ArrayList<Fighter> cibles, Fight fight) 
		{
			// TODO : Brokle ?
			caster.addBuff(effectID, value, turns, 1, debuffable, spell, args, caster);
			
		}

		private void applyEffect_165(Fight fight, ArrayList<Fighter> cibles) 
		{
			int value = -1;
			try
			{
				value = Integer.parseInt(args.split(";")[1]);
			}catch(Exception e){}
			if(value == -1)return;
			caster.addBuff(effectID, value, turns, 1, true, spell, args, caster);
		}

		private void applyEffect_51(Fight fight)
		{
			//Si case pas libre
			if(!cell.isWalkable(true) || cell.getFighters().size() >0)return;
			Fighter target = caster.get_isHolding();
			if(target == null)return;
			if(target.isState(6))return;//Stabilisation
			
			//on ajoute le porté a sa case
			target.set_fightCell(cell);
			target.get_fightCell().addFighter(target);
			//on enleve les états
			target.setState(Constants.ETAT_PORTE, 0);
			caster.setState(Constants.ETAT_PORTEUR, 0);
			//on dé-lie les 2 Fighter
			target.set_holdedBy(null);
			caster.set_isHolding(null);
			
			//on envoie les packets
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 51, caster.getGUID()+"", cell.getID()+"");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, target.getGUID()+"", target.getGUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster.getGUID()+"", caster.getGUID()+","+Constants.ETAT_PORTEUR+",0");
		}

		private void applyEffect_950(Fight fight, ArrayList<Fighter> cibles)
		{
			int id = -1;
			try
			{
				id = Integer.parseInt(args.split(";")[2]);
			}catch(Exception e){}
			if(id == -1)return;

			for(Fighter target : cibles)
			{
				if(spell==139 && target.getTeam()!= caster.getTeam())//Mot d'altruisme on saute les ennemis ?
				{
					continue;
				}
				if(turns <= 0)
				{
					target.setState(id, turns);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster.getGUID()+"", target.getGUID()+","+id+",1");
				}else
				{
					target.setState(id, turns);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster.getGUID()+"", target.getGUID()+","+id+",1");
					target.addBuff(effectID, value, turns, 1, false, spell, args, target);
				}
			}
		}
		
		private void applyEffect_951(Fight fight, ArrayList<Fighter> cibles)
		{
			int id = -1;
			try
			{
				id = Integer.parseInt(args.split(";")[2]);
			}catch(Exception e){}
			if(id == -1)return;
			
			for(Fighter target : cibles)
			{
				//Si la cible n'a pas l'état
				if(!target.isState(id))continue;
				//on enleve l'état
				target.setState(id, 0);
				//on envoie le packet
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster.getGUID()+"", target.getGUID()+","+id+",0");
			}
		}
		
		private void applyEffect_50(Fight fight)
		{
			//Porter
			Fighter target = cell.getFirstFighter();
			if(target == null)return;
			if(target.isState(6))return;//Stabilisation
			
			//on enleve le porté de sa case
			target.get_fightCell().getFighters().clear();
			//on lui définie sa nouvelle case
			target.set_fightCell(caster.get_fightCell());
			
			//on applique les états
			target.setState(Constants.ETAT_PORTE, -1);
			caster.setState(Constants.ETAT_PORTEUR, -1);
			//on lie les 2 Fighter
			target.set_holdedBy(caster);
			caster.set_isHolding(target);
			
			//on envoie les packets
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, target.getGUID()+"", target.getGUID()+","+Constants.ETAT_PORTE+",1");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 950, caster.getGUID()+"", caster.getGUID()+","+Constants.ETAT_PORTEUR+",1");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 50, caster.getGUID()+"", ""+target.getGUID());
		}

		private void applyEffect_788(ArrayList<Fighter> cibles, Fight fight)
		{
			//caster.addBuff(effectID, value, turns, 1, false, spell, args, caster);
			for(Fighter target : cibles) 
			{ 
				target.addBuff(effectID, value, turns, 1, false, spell, args, target); 
			}
		}

		private void applyEffect_131(ArrayList<Fighter> cibles, Fight fight)
		{
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, value, turns, 1, false, spell, args, caster);
			}
		}

		private void applyEffect_185(Fight fight)
		{
			int cellID = cell.getID();
			int mobID = -1;
			int level = -1;
			try
			{
				mobID = Integer.parseInt(args.split(";")[0]);
				level = Integer.parseInt(args.split(";")[1]);
			}catch(Exception e){}
			MobGrade MG = null;
			try{
				
				MG = World.getMonstre(mobID).getGradeByLevel(level).getCopy();
			}catch(Exception e1){
				GameServer.addToLog("Erreur sur le monstre id:"+mobID);
				return;
			};
			if(mobID == -1 || level == -1 || MG == null)return;
			int id = fight.getNextLowerFighterGuid();
			MG.setInFightID(id);
			Fighter F = new Fighter(fight,MG);
			F.setTeam(caster.getTeam());
			F.setInvocator(caster);
			fight.get_map().getCase(cellID).addFighter(F);
			F.set_fightCell(fight.get_map().getCase(cellID));
			fight.addFighterInTeam(F,caster.getTeam());
			String gm = F.getGmPacket('+').substring(3);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 181, caster.getGUID() + "", gm);
			
		}

		private void applyEffect_293(Fight fight)
		{
			caster.addBuff(effectID, value, turns, 1, false, spell, args, caster);
		}

		private void applyEffect_672(ArrayList<Fighter> cibles, Fight fight)
		{
			//Punition
			//Formule de barge ? :/ Clair que ca punie ceux qui veulent l'utiliser x_x
			double val = ((double)Formulas.getRandomJet(jet)/(double)100);
			int pdvMax = caster.getPdvMaxOutFight();
			double pVie = (double)caster.getPDV() / (double)caster.getPDVMAX();
			double rad = (double)2 * Math.PI * (double)(pVie - 0.5);
			double cos = Math.cos(rad);
			double taux = (Math.pow((cos+1),2))/(double)4;
			double dgtMax = val * pdvMax;
			int dgt = (int) (taux * dgtMax);
			
			for(Fighter target : cibles)
			{
				//si la cible a le buff renvoie de sort
				if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl )
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
					//le lanceur devient donc la cible
					target = caster;
				}
				if(target.hasBuff(765))//sacrifice
				{
					if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
					{
						applyEffect_765B(fight,target);
						target = target.getBuff(765).getCaster();
					}
				}
				
				int finalDommage = applyOnHitBuffs(dgt,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				if(target.getPDV() <=0)
					fight.onFighterDie(target);
			}
		}

		private void applyEffect_783(ArrayList<Fighter> cibles, Fight fight)
		{
			//Pousse jusqu'a la case visée
			Case ccase = caster.get_fightCell();
			//On calcule l'orientation entre les 2 cases
			char d = Pathfinding.getDirBetweenTwoCase(ccase.getID(),cell.getID(), fight.get_map(), true);
			//On calcule l'id de la case a coté du lanceur dans la direction obtenue
			int tcellID = Pathfinding.GetCaseIDFromDirrection(ccase.getID(), d, fight.get_map(), true);
			//on prend la case corespondante
			Case tcase = fight.get_map().getCase(tcellID);
			if(tcase == null)return;
			//S'il n'y a personne sur la case, on arrete
			if(tcase.getFighters().isEmpty())return;
			//On prend le Fighter ciblé
			Fighter target = tcase.getFirstFighter();
			//On verifie qu'il peut aller sur la case ciblé en ligne droite
			int c1 = tcellID;
			int limite = 0;
			while(true)
			{
				if(Pathfinding.GetCaseIDFromDirrection(c1, d, fight.get_map(), true) == cell.getID())
					break;
				if(Pathfinding.GetCaseIDFromDirrection(c1, d, fight.get_map(), true) == -1)
					return;
				c1 = Pathfinding.GetCaseIDFromDirrection(c1, d, fight.get_map(), true);
				limite++;
				if(limite > 50)return;
			}
			
			target.get_fightCell().getFighters().clear();
			target.set_fightCell(cell);
			target.get_fightCell().addFighter(target);
			
			ArrayList<Piege> P = (new ArrayList<Piege>());
			P.addAll(fight.get_traps());
			for(Piege p : P)
			{
				int dist = Pathfinding.getDistanceBetween(fight.get_map(),p.get_cell().getID(),target.get_fightCell().getID());
				//on active le piege
				if(dist <= p.get_size())p.onTraped(target);
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 5, caster.getGUID()+"", target.getGUID()+","+cell.getID());
		}

		private void applyEffect_9(ArrayList<Fighter> cibles, Fight fight)
		{
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, value, turns, 1, false, spell, args, caster);
			}
		}

		private void applyEffect_8(ArrayList<Fighter> cibles, Fight fight)
		{
			if(cibles.isEmpty())return;
			Fighter target = cibles.get(0);
			if(target == null)return;//ne devrait pas arriver
			if(target.isState(6))return;//Stabilisation
			switch(spell)
			{
				case 438://Transpo
					//si les 2 joueurs ne sont pas dans la meme team, on ignore
					if(target.getTeam() != caster.getTeam())return;
				break;
				
				case 445://Coop
					//si les 2 joueurs sont dans la meme team, on ignore
					if(target.getTeam() == caster.getTeam())return;
				break;
				
				case 449://Détour
				default:
				break;
			}
			//on enleve les persos des cases
			target.get_fightCell().getFighters().clear();
			caster.get_fightCell().getFighters().clear();
			//on retient les cases
			Case exTarget = target.get_fightCell();
			Case exCaster = caster.get_fightCell();
			//on échange les cases
			target.set_fightCell(exCaster);
			caster.set_fightCell(exTarget);
			//on ajoute les fighters aux cases
			target.get_fightCell().addFighter(target);
			caster.get_fightCell().addFighter(caster);
			ArrayList<Piege> P = (new ArrayList<Piege>());
			P.addAll(fight.get_traps());
			for(Piege p : P)
			{
				int dist = Pathfinding.getDistanceBetween(fight.get_map(),p.get_cell().getID(),target.get_fightCell().getID());
				int dist2 = Pathfinding.getDistanceBetween(fight.get_map(),p.get_cell().getID(),caster.get_fightCell().getID());
				//on active le piege
				if(dist <= p.get_size())p.onTraped(target);
				else if(dist2 <= p.get_size())p.onTraped(caster);
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, caster.getGUID()+"", target.getGUID()+","+exCaster.getID());
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, caster.getGUID()+"", caster.getGUID()+","+exTarget.getID());
			
		}

		private void applyEffect_266(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			int vol = 0;
			for(Fighter target : cibles)
			{
				target.addBuff(Constants.STATS_REM_CHAN, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_REM_CHAN, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
				vol += val;
			}
			if(vol == 0)return;
			//on ajoute le buff
			caster.addBuff(Constants.STATS_ADD_CHAN, vol, turns, 1, false, spell, args, caster);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_ADD_CHAN, caster.getGUID()+"", caster.getGUID()+","+vol+","+turns);
		}

		private void applyEffect_267(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			int vol = 0;
			for(Fighter target : cibles)
			{
				target.addBuff(Constants.STATS_REM_VITA, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_REM_VITA, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
				vol += val;
			}
			if(vol == 0)return;
			//on ajoute le buff
			caster.addBuff(Constants.STATS_ADD_VITA, vol, turns, 1, false, spell, args, caster);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_ADD_VITA, caster.getGUID()+"", caster.getGUID()+","+vol+","+turns);
		}
		
		private void applyEffect_268(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			int vol = 0;
			for(Fighter target : cibles)
			{
				target.addBuff(Constants.STATS_REM_AGIL, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_REM_AGIL, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
				vol += val;
			}
			if(vol == 0)return;
			//on ajoute le buff
			caster.addBuff(Constants.STATS_ADD_AGIL, vol, turns, 1, false, spell, args, caster);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_ADD_AGIL, caster.getGUID()+"", caster.getGUID()+","+vol+","+turns);
		}
		
		private void applyEffect_269(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			int vol = 0;
			for(Fighter target : cibles)
			{
				target.addBuff(Constants.STATS_REM_INTE, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_REM_INTE, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
				vol += val;
			}
			if(vol == 0)return;
			//on ajoute le buff
			caster.addBuff(Constants.STATS_ADD_INTE, vol, turns, 1, false, spell, args, caster);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_ADD_INTE, caster.getGUID()+"", caster.getGUID()+","+vol+","+turns);
		}
		
		private void applyEffect_270(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			int vol = 0;
			for(Fighter target : cibles)
			{
				target.addBuff(Constants.STATS_REM_SAGE, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_REM_SAGE, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
				vol += val;
			}
			if(vol == 0)return;
			//on ajoute le buff
			caster.addBuff(Constants.STATS_ADD_SAGE, vol, turns, 1, false, spell, args, caster);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_ADD_SAGE, caster.getGUID()+"", caster.getGUID()+","+vol+","+turns);
		}
		
		private void applyEffect_271(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			int vol = 0;
			for(Fighter target : cibles)
			{
				target.addBuff(Constants.STATS_REM_FORC, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_REM_FORC, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
				vol += val;
			}
			if(vol == 0)return;
			//on ajoute le buff
			caster.addBuff(Constants.STATS_ADD_FORC, vol, turns, 1, false, spell, args, caster);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, Constants.STATS_ADD_FORC, caster.getGUID()+"", caster.getGUID()+","+vol+","+turns);
		}
		
		private void applyEffect_210(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_211(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_212(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_213(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_214(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_215(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_216(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_217(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_218(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_219(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}
		private void applyEffect_106(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = -1;
			try
			{
				val = Integer.parseInt(args.split(";")[1]);//Niveau de sort max
			}catch(Exception e){};
			if(val == -1)return;
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}

		private void applyEffect_105(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
					target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
			}
		}

		private void applyEffect_265(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}

		private void applyEffect_155(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}
		private void applyEffect_163(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}
		private void applyEffect_162(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}
		private void applyEffect_161(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}
		private void applyEffect_160(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}
		
		private void applyEffect_149(Fight fight, ArrayList<Fighter> cibles)
		{
			int id = -1;
			try
			{
				id = Integer.parseInt(args.split(";")[2]);
			}catch(Exception e){};
			for(Fighter target : cibles)
			{
				if(id == -1)id = target.getDefaultGfx();
				target.addBuff(effectID, id, turns, 1, false, spell, args, caster);
				int defaut = target.getDefaultGfx();
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+defaut+","+id+","+turns);
			}	
		}

		private void applyEffect_182(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}

		private void applyEffect_184(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}
		
		private void applyEffect_183(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}

		private void applyEffect_145(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}

		private void applyEffect_171(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}

		private void applyEffect_142(Fight fight, ArrayList<Fighter> cibles)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}
		}

		private void applyEffect_150(Fight fight, ArrayList<Fighter> cibles)
		{
			if(turns == 0)return;
			for(Fighter target : cibles)
			{
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 150, caster.getGUID()+"", target.getGUID()+",4");
				target.addBuff(effectID, 0, turns, 0, true,spell, args, caster);
			}
		}
		
		private void applyEffect_402(Fight fight)
		{
			if(!cell.isWalkable(true))return;//Si case pas marchable
			
			String[] infos = args.split(";");
			int spellID = Short.parseShort(infos[0]);
			int level = Byte.parseByte(infos[1]);
			byte duration = Byte.parseByte(infos[3]);
			String po = World.getSort(spell).getStatsByLevel(spellLvl).getPorteeType();
			byte size = (byte) CryptManager.getIntByHashedValue(po.charAt(1));
			SortStats TS = World.getSort(spellID).getStatsByLevel(level);
			Glyphe g = new Glyphe(fight,caster,cell,size,TS,duration,spell);
			fight.get_glyphs().add(g);
			int unk = g.get_color();
			String str = "GDZ+"+cell.getID()+";"+size+";"+unk;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster.getGUID()+"", str);
			str = "GDC"+cell.getID()+";Haaaaaaaaa3005;";
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster.getGUID()+"", str);
		}

		private void applyEffect_401(Fight fight)
		{
			if(!cell.isWalkable(true))return;//Si case pas marchable
			if(cell.getFirstFighter() != null)return;//Si la case est prise par un joueur
			
			String[] infos = args.split(";");
			int spellID = Short.parseShort(infos[0]);
			int level = Byte.parseByte(infos[1]);
			byte duration = Byte.parseByte(infos[3]);
			String po = World.getSort(spell).getStatsByLevel(spellLvl).getPorteeType();
			byte size = (byte) CryptManager.getIntByHashedValue(po.charAt(1));
			SortStats TS = World.getSort(spellID).getStatsByLevel(level);
			Glyphe g = new Glyphe(fight,caster,cell,size,TS,duration,spell);
			fight.get_glyphs().add(g);
			int unk = g.get_color();
			String str = "GDZ+"+cell.getID()+";"+size+";"+unk;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster.getGUID()+"", str);
			str = "GDC"+cell.getID()+";Haaaaaaaaa3005;";
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster.getGUID()+"", str);
		}

		private void applyEffect_400(Fight fight)
		{
			if(!cell.isWalkable(true))return;//Si case pas marchable
			if(cell.getFirstFighter() != null)return;//Si la case est prise par un joueur
			
			//Si la case est prise par le centre d'un piege
			for(Piege p :fight.get_traps())if(p.get_cell().getID() == cell.getID())return;

			String[] infos = args.split(";");
			int spellID = Short.parseShort(infos[0]);
			int level = Byte.parseByte(infos[1]);
			String po = World.getSort(spell).getStatsByLevel(spellLvl).getPorteeType();
			byte size = (byte) CryptManager.getIntByHashedValue(po.charAt(1));
			SortStats TS = World.getSort(spellID).getStatsByLevel(level);
			Piege g = new Piege(fight,caster,cell,size,TS,spell);
			fight.get_traps().add(g);
			int unk = g.get_color();
			int team = caster.getTeam()+1;
			String str = "GDZ+"+cell.getID()+";"+size+";"+unk;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, team, 999, caster.getGUID()+"", str);
			str = "GDC"+cell.getID()+";Haaaaaaaaz3005;";
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, team, 999, caster.getGUID()+"", str);	
		}
		
		private void applyEffect_116(ArrayList<Fighter> cibles, Fight fight)//Malus PO
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}		
		}
		
		private void applyEffect_117(ArrayList<Fighter> cibles, Fight fight)//Bonus PO
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
				//Gain de PO pendant le tour de jeu
				if(target.canPlay() && target == caster) target.getTotalStats().addOneStat(Constants.STATS_ADD_PO, val);
			}		
		}
		
		private void applyEffect_118(ArrayList<Fighter> cibles, Fight fight)//Bonus Force
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}		
		}

		private void applyEffect_119(ArrayList<Fighter> cibles, Fight fight)//Bonus Agilité
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}		
		}
		
		private void applyEffect_120(ArrayList<Fighter> cibles, Fight fight)//Bonus PA
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
				caster.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", caster.getGUID()+","+val+","+turns);	
		}
		
		private void applyEffect_78(ArrayList<Fighter> cibles, Fight fight)//Bonus PA
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}		
		}
		
		private void applyEffect_180(Fight fight)//invocation
		{
			int cell = this.cell.getID();
			int id = fight.getNextLowerFighterGuid();
			Personnage Clone = Personnage.ClonePerso(caster.getPersonnage(), id);
			Fighter F = new Fighter(fight,Clone);
			F.setTeam(caster.getTeam());
			F.setInvocator(caster);
			fight.get_map().getCase(cell).addFighter(F);
			F.set_fightCell(fight.get_map().getCase(cell));
			fight.get_ordreJeu().add((fight.get_ordreJeu().indexOf(caster)+1),F);
			fight.addFighterInTeam(F,caster.getTeam());
			String gm = F.getGmPacket('+').substring(3);
			String gtl = fight.getGTL();
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 180, caster.getGUID() + "", gm);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster.getGUID()+"", gtl);
			ArrayList<Piege> P = (new ArrayList<Piege>());
			P.addAll(fight.get_traps());
			for(Piege p : P)
			{
				int dist = Pathfinding.getDistanceBetween(fight.get_map(),p.get_cell().getID(),F.get_fightCell().getID());
				//on active le piege
				if(dist <= p.get_size())p.onTraped(F);
			}
		}
		
		private void applyEffect_181(Fight fight)//invocation
		{
			int cell = this.cell.getID();
			int mobID = -1;
			int level = -1;
			try
			{
				mobID = Integer.parseInt(args.split(";")[0]);
				level = Integer.parseInt(args.split(";")[1]);
			}catch(Exception e){}
			
			MobGrade MG = null;
			try{
				
				MG = World.getMonstre(mobID).getGradeByLevel(level).getCopy();
			}catch(Exception e1){
				GameServer.addToLog("Erreur sur le monstre id:"+mobID);
				return;
			};
			
			if(mobID == -1 || level == -1 || MG == null)return;
            int id = fight.getNextLowerFighterGuid()-caster._nbInvoc;
			MG.setInFightID(id);
			MG.modifStatByInvocator(caster);
			Fighter F = new Fighter(fight,MG);
			F.setTeam(caster.getTeam());
			F.setInvocator(caster);
			fight.get_map().getCase(cell).addFighter(F);
			F.set_fightCell(fight.get_map().getCase(cell));
			fight.get_ordreJeu().add((fight.get_ordreJeu().indexOf(caster)+1),F);
			fight.addFighterInTeam(F,caster.getTeam());
			String gm = F.getGmPacket('+').substring(3);
			String gtl = fight.getGTL();
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 181, caster.getGUID() + "", gm);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, caster.getGUID()+"", gtl);
			caster._nbInvoc++;
			ArrayList<Piege> P = (new ArrayList<Piege>());
			P.addAll(fight.get_traps());
			for(Piege p : P)
			{
				int dist = Pathfinding.getDistanceBetween(fight.get_map(),p.get_cell().getID(),F.get_fightCell().getID());
				//on active le piege
				if(dist <= p.get_size())p.onTraped(F);
			}
		}

		private void applyEffect_110(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_111(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				if(spell == 89 && target.getTeam() != caster.getTeam())
				{
					continue;
				}
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				//Gain de PA pendant le tour de jeu
				if(target.canPlay() && target == caster) target.setCurPA(fight, target.getPA()+val);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}

		private void applyEffect_112(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}

		private void applyEffect_121(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_122(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_123(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_124(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_125(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_126(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_128(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
                target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				//Gain de PM pendant le tour de jeu
				if(target.canPlay() && target == caster) target.setCurPM(fight, target.getPM()+val);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_138(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_114(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_"+effectID+")");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}			
		}
		
		private void applyEffect_115(ArrayList<Fighter> cibles, Fight fight)
		{
			int val = Formulas.getRandomJet(jet);
			if(val == -1)
			{
				GameServer.addToLog("Erreur de valeur pour getRandomJet (applyEffect_115)");
				return;
			}
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, val, turns, 1, false, spell, args, caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, effectID, caster.getGUID()+"", target.getGUID()+","+val+","+turns);
			}		
		}
		
		private void applyEffect_77(ArrayList<Fighter> cibles, Fight fight)
		{
			int value = 1;
			try
			{
				value = Integer.parseInt(args.split(";")[0]);
			}catch(NumberFormatException e){};
			int num = 0;
			for(Fighter target : cibles)
			{
				int val = Formulas.getPointsLost('m', value, caster, target);
				if(val < value)
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 309, caster.getGUID()+"", target.getGUID()+","+(value-val));
				}
				if(val < 1)continue;
				target.addBuff(Constants.STATS_REM_PM, val, turns,0, true, spell,args,caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7,Constants.STATS_REM_PM, caster.getGUID()+"", target.getGUID()+",-"+val+","+turns);
				num += val;
				//Gain de PM pendant le tour de jeu
				if(target.canPlay() && target == caster) target.setCurPM(fight, target.getPM()+val);
			}
			if(num != 0)
			{
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7,Constants.STATS_ADD_PM, caster.getGUID()+"", caster.getGUID()+","+num+","+turns);
				caster.addBuff(Constants.STATS_ADD_PM, num, 1, 0, true, spell,args,caster);
			}
		}

		private void applyEffect_84(ArrayList<Fighter> cibles, Fight fight)
		{
			int value = 1;
			try
			{
				value = Integer.parseInt(args.split(";")[0]);
			}catch(NumberFormatException e){};
			int num = 0;
			for(Fighter target : cibles)
			{
				int val = Formulas.getPointsLost('m', value, caster, target);
				if(val < value)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 308, caster.getGUID()+"", target.getGUID()+","+(value-val));

				if(val < 1)continue;
				if(spell == 95)
				{
					target.addBuff(Constants.STATS_REM_PA, val, 1,1, true, spell,args,caster);	
				}else
				{
					target.addBuff(Constants.STATS_REM_PA, val, turns,0, true, spell,args,caster);
				}
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7,Constants.STATS_REM_PA, caster.getGUID()+"", target.getGUID()+",-"+val+","+turns);
				num += val;
			}
			if(num != 0)
			{
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7,Constants.STATS_ADD_PA, caster.getGUID()+"", caster.getGUID()+","+num+","+turns);
				caster.addBuff(Constants.STATS_ADD_PA, num, 0, 0, true, spell,args,caster);
				//Gain de PA pendant le tour de jeu
				if(caster.canPlay()) caster.setCurPA(fight, caster.getPA()+num);
			}
		}
		
		private void applyEffect_168(Fight fight, ArrayList<Fighter> cibles)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, value, 1, 1, false, spell, args, caster);
					if(turns <= 1 || duration <= 1)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 168,target.getGUID()+"",target.getGUID()+",-"+value);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					if(spell == 197 || spell == 112)
					{
						target.addBuff(effectID, value, turns, turns, false, spell, args, caster);
					}
					else if(spell == 115)//Odorat
                    {
                        int lostPa = Formulas.getRandomJet(jet);
                        if(lostPa == -1)
                                continue;
                       
                        target.addBuff(effectID, lostPa, turns, turns, false, spell, args, caster);
                        if(turns <= 1 || duration <= 1)
                                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 168,target.getGUID()+"",target.getGUID()+",-"+lostPa);
                    }
					else
					{
						target.addBuff(effectID, value, 1, 1, false, spell, args, caster);
					}
					if(turns <= 1 || duration <= 1)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 168,target.getGUID()+"",target.getGUID()+",-"+value);
				}
			}
		}
		private void applyEffect_169(Fight fight, ArrayList<Fighter> cibles)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, value, 1, 1, false, spell, args, caster);
					if(turns <= 1 || duration <= 1)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 169,target.getGUID()+"",target.getGUID()+",-"+value);
				}
			}else
			{
				if(cibles.isEmpty() && spell == 120 && caster.get_oldCible() != null)
				{
					caster.get_oldCible().addBuff(effectID, value, turns, turns, false, spell, args, caster);
					if(turns <= 1 || duration <= 1)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 169,caster.get_oldCible().getGUID()+"",caster.get_oldCible().getGUID()+",-"+value);
				}
				for(Fighter target : cibles)
				{
					if(spell == 192)//Ronce apaisante
					{
						target.addBuff(effectID, value, turns, 0, debuffable, spell, args, caster);
					}
					else if(spell == 197)
					{
						target.addBuff(effectID, value, turns, turns, false, spell, args, caster);
					}
					else if(spell == 115)//Odorat
                    {
                        int lostPm = Formulas.getRandomJet(jet);
                        if(lostPm == -1)
                               continue;
                       
                        target.addBuff(effectID, lostPm, turns, turns, false, spell, args, caster);
                        if(turns <= 1 || duration <= 1)
                                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 169,target.getGUID()+"",target.getGUID()+",-"+lostPm);
                    }
					else
					{
						target.addBuff(effectID, value, 1, 1, false, spell, args, caster);
					}
					if(turns <= 1 || duration <= 1)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 169,target.getGUID()+"",target.getGUID()+",-"+value);
				}
			}
		}


		private void applyEffect_101(ArrayList<Fighter> cibles, Fight fight)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					int retrait = Formulas.getPointsLost('a',value,caster,target);
					if((value -retrait) > 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 308, caster.getGUID()+"", target.getGUID()+","+(value-retrait));
					if(retrait > 0)
					{
						target.addBuff(Constants.STATS_REM_PA, retrait, 1, 1, false, spell, args, caster);
						if(turns <= 1 || duration <= 1)
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 101,target.getGUID()+"",target.getGUID()+",-"+retrait);
					}
				}
			}else
			{
				for(Fighter target : cibles)
				{
					int retrait = Formulas.getPointsLost('a',value,caster,target);
					if((value -retrait) > 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 308, caster.getGUID()+"", target.getGUID()+","+(value-retrait));
					if(retrait > 0)
					{
						if(spell == 89)
						{
							target.addBuff(effectID, retrait, 0, 1, false, spell, args, caster);
						}else
						{
							target.addBuff(effectID, retrait, 1, 1, false, spell, args, caster);
						}
						if(turns <= 1 || duration <= 1)
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 101,target.getGUID()+"",target.getGUID()+",-"+retrait);
					}
				}
			}
		}

		private void applyEffect_127(ArrayList<Fighter> cibles, Fight fight)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					int retrait = Formulas.getPointsLost('m',value,caster,target);
					if((value -retrait) > 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 309, caster.getGUID()+"", target.getGUID()+","+(value-retrait));
					if(retrait > 0)
					{
						target.addBuff(Constants.STATS_REM_PM, retrait, 1, 1, false, spell, args, caster);
						if(turns <= 1 || duration <= 1)
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 127,target.getGUID()+"",target.getGUID()+",-"+retrait);
					}
				}
			}else
			{
				for(Fighter target : cibles)
				{
					int retrait = Formulas.getPointsLost('m',value,caster,target);
					if((value -retrait) > 0)
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 309, caster.getGUID()+"", target.getGUID()+","+(value-retrait));
					if(retrait > 0)
					{
						if(spell == 136)//Mot d'immobilisation
						{
							target.addBuff(effectID, retrait, turns, turns, false, spell, args, caster);
						}else
						{
							target.addBuff(effectID, retrait, 1, 1, false, spell, args, caster);
						}
						if(turns <= 1 || duration <= 1)
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 127,target.getGUID()+"",target.getGUID()+",-"+retrait);
					}
				}
			}
		}
		
		private void applyEffect_107(ArrayList<Fighter> cibles, Fight fight)
		{
			if(turns<1)return;//Je vois pas comment, vraiment ...
			else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}

		
		private void applyEffect_79(ArrayList<Fighter> cibles, Fight fight)
		{
			if(turns<1)return;//Je vois pas comment, vraiment ...
			else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, -1, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}


		private void applyEffect_4(Fight fight,ArrayList<Fighter> cibles)
		{
			if(turns >1)return;//Olol bondir 3 tours apres ?
			
			if(cell.isWalkable(true) && !fight.isOccuped(cell.getID()))//Si la case est prise, on va éviter que les joueurs se montent dessus *-*
			{
				caster.get_fightCell().getFighters().clear();
				caster.set_fightCell(cell);
				caster.get_fightCell().addFighter(caster);
				
				ArrayList<Piege> P = (new ArrayList<Piege>());
				P.addAll(fight.get_traps());
				for(Piege p : P)
				{
					int dist = Pathfinding.getDistanceBetween(fight.get_map(),p.get_cell().getID(),caster.get_fightCell().getID());
					//on active le piege
					if(dist <= p.get_size())p.onTraped(caster);
				}
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, caster.getGUID()+"", caster.getGUID()+","+cell.getID());
			}else
			{
				GameServer.addToLog("Tentative de teleportation echouee : case non libre:");
				GameServer.addToLog("IsOccuped: "+fight.isOccuped(cell.getID()));
				GameServer.addToLog("Walkable: "+cell.isWalkable(true));
			}
		}
		
		private void applyEffect_765B(Fight fight,Fighter target)
		{
			Fighter sacrified = target.getBuff(765).getCaster();
			Case cell1 = sacrified.get_fightCell();
			Case cell2 = target.get_fightCell();
			
			sacrified.get_fightCell().getFighters().clear();
			target.get_fightCell().getFighters().clear();
			sacrified.set_fightCell(cell2);
			sacrified.get_fightCell().addFighter(sacrified);
			target.set_fightCell(cell1);
			target.get_fightCell().addFighter(target);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, target.getGUID()+"", target.getGUID()+","+cell1.getID());
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 4, sacrified.getGUID()+"", sacrified.getGUID()+","+cell2.getID());
			
		}
		
		private void applyEffect_109(Fight fight)//Dommage pour le lanceur (fixes)
		{
			if(turns <= 0)
			{
				int dmg = Formulas.getRandomJet(args.split(";")[5]);
				int finalDommage = Formulas.calculFinalDommage(fight,caster, caster,Constants.ELEMENT_NULL, dmg, false, false, spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,caster,caster,fight);//S'il y a des buffs spéciaux
				if(finalDommage>caster.getPDV())finalDommage = caster.getPDV();//Caster va mourrir
				caster.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", caster.getGUID()+","+finalDommage);
				
				if(caster.getPDV() <=0)
					fight.onFighterDie(caster);
			}else
			{
				caster.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
			}
		}
		
		private void applyEffect_82(ArrayList<Fighter> cibles,Fight fight)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					//si la cible a le buff renvoie de sort et que le sort peut etre renvoyer
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_NULL, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
					//Vol de vie
					int heal = (int)(-finalDommage)/2;
					if((caster.getPDV()+heal) > caster.getPDVMAX())
						heal = caster.getPDVMAX()-caster.getPDV();
					caster.removePDV(-heal);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
					
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_6(ArrayList<Fighter> cibles,Fight fight)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					if(target.isState(6)) continue;
					Case eCell = cell;
					//Si meme case
					if(target.get_fightCell().getID() == cell.getID())
					{
						//on prend la cellule caster
						eCell = caster.get_fightCell();
					}
					int newCellID =	Pathfinding.newCaseAfterPush(fight.get_map(),eCell,target.get_fightCell(),-value);
					if(newCellID == 0)
						return;
					
					if(newCellID <0)//S'il a été bloqué
					{
						int a = -(value + newCellID);
						newCellID =	Pathfinding.newCaseAfterPush(fight.get_map(),caster.get_fightCell(),target.get_fightCell(),a);
						if(newCellID == 0)
							return;
						if(fight.get_map().getCase(newCellID) == null)
							return;
					}
					
					target.get_fightCell().getFighters().clear();
					target.set_fightCell(fight.get_map().getCase(newCellID));
					target.get_fightCell().addFighter(target);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 5, caster.getGUID()+"", target.getGUID()+","+newCellID);
					
					ArrayList<Piege> P = (new ArrayList<Piege>());
					P.addAll(fight.get_traps());
					for(Piege p : P)
					{
						int dist = Pathfinding.getDistanceBetween(fight.get_map(),p.get_cell().getID(),target.get_fightCell().getID());
						//on active le piege
						if(dist <= p.get_size())p.onTraped(target);
					}
				}
			}
		}
		
		private void applyEffect_5(ArrayList<Fighter> cibles,Fight fight)
		{
			if(cibles.size() == 1 && spell == 120)
			{
				if(!cibles.get(0).isDead())
				{
					caster.set_oldCible(cibles.get(0));
				}
			}
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					if(target.isState(6)) continue;
					Case eCell = cell;
					//Si meme case
					if(target.get_fightCell().getID() == cell.getID())
					{
						//on prend la cellule caster
						eCell = caster.get_fightCell();
					}
					int newCellID =	Pathfinding.newCaseAfterPush(fight.get_map(),eCell,target.get_fightCell(),value);
					if(newCellID == 0)
						return;
					if(newCellID <0)//S'il a été bloqué
					{
						int a = -newCellID;
						int coef = Formulas.getRandomJet("1d8+8");
						double b = (caster.get_lvl()/(double)(50.00));
						if(b<0.1)b= 0.1;
						double c = b*a;//Calcule des dégats de poussé
						int finalDommage = (int)(coef * c);
						if(finalDommage < 1)finalDommage = 1;
						if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
						
						if(target.hasBuff(184)) 
						{
							finalDommage = finalDommage-target.getBuff(184).getValue();//Réduction physique
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(184).getValue());
						}
						if(target.hasBuff(105))
						{
							finalDommage = finalDommage-target.getBuff(105).getValue();//Immu
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(105).getValue());
						}
						if(finalDommage > 0)
						{
							target.removePDV(finalDommage);
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+",-"+finalDommage);
							if(target.getPDV() <=0)
							{
								fight.onFighterDie(target);
								return;
							}
						}
							a = value-a;
							newCellID =	Pathfinding.newCaseAfterPush(fight.get_map(),caster.get_fightCell(),target.get_fightCell(),a);
							if(newCellID == 0)
								return;
							if(fight.get_map().getCase(newCellID) == null)
								return;
					}
					target.get_fightCell().getFighters().clear();
					target.set_fightCell(fight.get_map().getCase(newCellID));
					target.get_fightCell().addFighter(target);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 5, caster.getGUID()+"", target.getGUID()+","+newCellID);
					
					ArrayList<Piege> P = (new ArrayList<Piege>());
					P.addAll(fight.get_traps());
					for(Piege p : P)
					{
						int dist = Pathfinding.getDistanceBetween(fight.get_map(),p.get_cell().getID(),target.get_fightCell().getID());
						//on active le piege
						if(dist <= p.get_size())p.onTraped(target);
					}
				}
			}
		}
		
		private void applyEffect_91(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(isCaC)
			{
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_EAU, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				int heal = (int)(-finalDommage)/2;
				if((caster.getPDV()+heal) > caster.getPDVMAX())
					heal = caster.getPDVMAX()-caster.getPDV();
				caster.removePDV(-heal);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				if(caster.isHide())caster.unHide(spell);
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_EAU, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
					int heal = (int)(-finalDommage)/2;
					if((caster.getPDV()+heal) > caster.getPDVMAX())
						heal = caster.getPDVMAX()-caster.getPDV();
					caster.removePDV(-heal);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
					
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}

		private void applyEffect_92(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(caster.isHide())caster.unHide(spell);
			if(isCaC)
			{
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_TERRE, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				int heal = (int)(-finalDommage)/2;
				if((caster.getPDV()+heal) > caster.getPDVMAX())
					heal = caster.getPDVMAX()-caster.getPDV();
				caster.removePDV(-heal);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_TERRE, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
					
					int heal = (int)(-finalDommage)/2;
					if((caster.getPDV()+heal) > caster.getPDVMAX())
						heal = caster.getPDVMAX()-caster.getPDV();
					caster.removePDV(-heal);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
					
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_93(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(caster.isHide())caster.unHide(spell);
			if(isCaC)
			{
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_AIR, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				int heal = (int)(-finalDommage)/2;
				if((caster.getPDV()+heal) > caster.getPDVMAX())
					heal = caster.getPDVMAX()-caster.getPDV();
				caster.removePDV(-heal);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_AIR, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
					
					int heal = (int)(-finalDommage)/2;
					if((caster.getPDV()+heal) > caster.getPDVMAX())
						heal = caster.getPDVMAX()-caster.getPDV();
					caster.removePDV(-heal);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
					
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_94(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(caster.isHide())caster.unHide(spell);
			if(isCaC)//CaC Eau
			{
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_FEU, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				int heal = (int)(-finalDommage)/2;
				if((caster.getPDV()+heal) > caster.getPDVMAX())
					heal = caster.getPDVMAX()-caster.getPDV();
				caster.removePDV(-heal);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_FEU, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
					int heal = (int)(-finalDommage)/2;
					if((caster.getPDV()+heal) > caster.getPDVMAX())
						heal = caster.getPDVMAX()-caster.getPDV();
					caster.removePDV(-heal);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
					
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}

		private void applyEffect_95(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(caster.isHide())caster.unHide(spell);
			if(isCaC)//CaC Eau
			{
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_NEUTRE, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				int heal = (int)(-finalDommage)/2;
				if((caster.getPDV()+heal) > caster.getPDVMAX())
					heal = caster.getPDVMAX()-caster.getPDV();
				caster.removePDV(-heal);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_NEUTRE, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);

					int heal = (int)(-finalDommage)/2;
					if((caster.getPDV()+heal) > caster.getPDVMAX())
						heal = caster.getPDVMAX()-caster.getPDV();
					caster.removePDV(-heal);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, target.getGUID()+"", caster.getGUID()+","+heal);
					
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_85(ArrayList<Fighter> cibles,Fight fight)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int resP = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_EAU);
					int resF = target.getTotalStats().getEffect(Constants.STATS_ADD_R_EAU);
					if(target.getPersonnage() != null)//Si c'est un joueur, on ajoute les resists bouclier
					{
						resP += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_EAU);
						resF += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_EAU);
					}
					int dmg = Formulas.getRandomJet(args.split(";")[5]);//%age de pdv infligé
					int val = caster.getPDV()/100*dmg;//Valeur des dégats
					//retrait de la résist fixe
					val -= resF;
					int reduc =	(int)(((float)val)/(float)100)*resP;//Reduc %resis
					val -= reduc;
					if(val <0)val = 0;
					
					val = applyOnHitBuffs(val,target,caster,fight);//S'il y a des buffs spéciaux
					
					if(val>target.getPDV())val = target.getPDV();//Target va mourrir
					target.removePDV(val);
					val = -(val);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+val);
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_86(ArrayList<Fighter> cibles,Fight fight)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int resP = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_TER);
					int resF = target.getTotalStats().getEffect(Constants.STATS_ADD_R_TER);
					if(target.getPersonnage() != null)//Si c'est un joueur, on ajoute les resists bouclier
					{
						resP += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_TER);
						resF += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_TER);
					}
					int dmg = Formulas.getRandomJet(args.split(";")[5]);//%age de pdv infligé
					int val = caster.getPDV()/100*dmg;//Valeur des dégats
					//retrait de la résist fixe
					val -= resF;
					int reduc =	(int)(((float)val)/(float)100)*resP;//Reduc %resis
					val -= reduc;
					if(val <0)val = 0;
					
					val = applyOnHitBuffs(val,target,caster,fight);//S'il y a des buffs spéciaux
					
					if(val>target.getPDV())val = target.getPDV();//Target va mourrir
					target.removePDV(val);
					val = -(val);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+val);
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_87(ArrayList<Fighter> cibles,Fight fight)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int resP = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_AIR);
					int resF = target.getTotalStats().getEffect(Constants.STATS_ADD_R_AIR);
					if(target.getPersonnage() != null)//Si c'est un joueur, on ajoute les resists bouclier
					{
						resP += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_AIR);
						resF += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_AIR);
					}
					int dmg = Formulas.getRandomJet(args.split(";")[5]);//%age de pdv infligé
					int val = caster.getPDV()/100*dmg;//Valeur des dégats
					//retrait de la résist fixe
					val -= resF;
					int reduc =	(int)(((float)val)/(float)100)*resP;//Reduc %resis
					val -= reduc;
					if(val <0)val = 0;
					
					val = applyOnHitBuffs(val,target,caster,fight);//S'il y a des buffs spéciaux
					
					if(val>target.getPDV())val = target.getPDV();//Target va mourrir
					target.removePDV(val);
					val = -(val);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+val);
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_88(ArrayList<Fighter> cibles,Fight fight)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int resP = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_FEU);
					int resF = target.getTotalStats().getEffect(Constants.STATS_ADD_R_FEU);
					if(target.getPersonnage() != null)//Si c'est un joueur, on ajoute les resists bouclier
					{
						resP += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_FEU);
						resF += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_FEU);
					}
					int dmg = Formulas.getRandomJet(args.split(";")[5]);//%age de pdv infligé
					int val = caster.getPDV()/100*dmg;//Valeur des dégats
					//retrait de la résist fixe
					val -= resF;
					int reduc =	(int)(((float)val)/(float)100)*resP;//Reduc %resis
					val -= reduc;
					if(val <0)val = 0;
					
					val = applyOnHitBuffs(val,target,caster,fight);//S'il y a des buffs spéciaux
					
					if(val>target.getPDV())val = target.getPDV();//Target va mourrir
					target.removePDV(val);
					val = -(val);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+val);
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_89(ArrayList<Fighter> cibles,Fight fight)
		{
			if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int resP = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_NEU);
					int resF = target.getTotalStats().getEffect(Constants.STATS_ADD_R_NEU);
					if(target.getPersonnage() != null)//Si c'est un joueur, on ajoute les resists bouclier
					{
						resP += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_NEU);
						resF += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_NEU);
					}
					int dmg = Formulas.getRandomJet(args.split(";")[5]);//%age de pdv infligé
					int val = caster.getPDV()/100*dmg;//Valeur des dégats
					//retrait de la résist fixe
					val -= resF;
					int reduc =	(int)(((float)val)/(float)100)*resP;//Reduc %resis
					val -= reduc;
					if(val <0)val = 0;
					
					val = applyOnHitBuffs(val,target,caster,fight);//S'il y a des buffs spéciaux
					
					if(val>target.getPDV())val = target.getPDV();//Target va mourrir
					target.removePDV(val);
					val = -(val);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+val);
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_96(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(isCaC)//CaC Eau
			{
				if(caster.isHide())caster.unHide(spell);
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);

					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
				int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_EAU, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				if(caster.isHide())caster.unHide(spell);
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);

					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
					
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_EAU, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}

		private void applyEffect_97(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(isCaC)//CaC Terre
			{
				if(caster.isHide())caster.unHide(spell);
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);

					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
				int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_TERRE, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				if(caster.isHide())caster.unHide(spell);
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);

					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
					if(spell==160 && target==caster)
					{
						continue;//Epée de Iop ne tape pas le lanceur.
					}else if(chance > 0 && spell==108)//Esprit félin ?
					{
						int fDommage = Formulas.calculFinalDommage(fight,caster, caster,Constants.ELEMENT_TERRE, dmg,false,false,spell);
						fDommage = applyOnHitBuffs(fDommage,caster,caster,fight);//S'il y a des buffs spéciaux
						if(fDommage>caster.getPDV())fDommage = caster.getPDV();//Target va mourrir
						caster.removePDV(fDommage);
						fDommage = -(fDommage);
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", caster.getGUID()+","+fDommage);
						if(caster.getPDV() <=0)
							fight.onFighterDie(caster);
					}else
					{
						int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_TERRE, dmg,false,false,spell);
						finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
						if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
						target.removePDV(finalDommage);
						finalDommage = -(finalDommage);
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
						if(target.getPDV() <=0)
							fight.onFighterDie(target);
					}
					
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_98(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(isCaC)//CaC Air
			{
				if(caster.isHide())caster.unHide(spell);
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);

					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
				int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_AIR, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				if(caster.isHide())caster.unHide(spell);
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);

					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
					
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_AIR, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_99(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(caster.isHide())caster.unHide(spell);
			
			if(isCaC)//CaC Feu
			{
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);

					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
				int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_FEU, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					if(spell == 36 && target == caster)//Frappe du Craqueleur ne tape pas l'osa
					{
						continue;
					}
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					
					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
					
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_FEU, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}

		private void applyEffect_100(ArrayList<Fighter> cibles,Fight fight, boolean isCaC)
		{
			if(caster.isHide())caster.unHide(spell);
			if(isCaC)//CaC Neutre
			{
				for(Fighter target : cibles)
				{
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);

					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
				int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_NEUTRE, dmg,false,true,spell);
				
				finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
				if(target.getPDV() <=0) fight.onFighterDie(target);
				}
			}else if(turns <= 0)
			{
				for(Fighter target : cibles)
				{
					//si la cible a le buff renvoie de sort
					if(target.hasBuff(106) && target.getBuffValue(106) >= spellLvl && spell != 0)
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 106, target.getGUID()+"", target.getGUID()+",1");
						//le lanceur devient donc la cible
						target = caster;
					}
					if(target.hasBuff(765))//sacrifice
					{
						if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
						{
							applyEffect_765B(fight,target);
							target = target.getBuff(765).getCaster();
						}
					}
					
					int dmg = Formulas.getRandomJet(args.split(";")[5]);
					
					//Si le sort est boosté par un buff spécifique
					for(SpellEffect SE : caster.getBuffsByEffectID(293))
					{
						if(SE.getValue() == spell)
						{
							int add = -1;
							try
							{
								add = Integer.parseInt(SE.getArgs().split(";")[2]);
							}catch(Exception e){};
							if(add <= 0)continue;
							dmg += add;
						}
					}
				
					
					int finalDommage = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_NEUTRE, dmg,false,false,spell);
					
					finalDommage = applyOnHitBuffs(finalDommage,target,caster,fight);//S'il y a des buffs spéciaux
					
					if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
					target.removePDV(finalDommage);
					finalDommage = -(finalDommage);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
					if(target.getPDV() <=0)
						fight.onFighterDie(target);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}

		private void applyEffect_132(ArrayList<Fighter> cibles,Fight fight)
		{
			for(Fighter target : cibles)
			{
				target.debuff();
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 132, caster.getGUID()+"", target.getGUID()+"");
			}
		}
		
		private void applyEffect_140(ArrayList<Fighter> cibles,Fight fight)
		{
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, 0, 1, 0, true,spell, args, caster);
			}
		}

		private void applyEffect_765(ArrayList<Fighter> cibles,Fight fight)
		{
			for(Fighter target : cibles)
			{
				target.addBuff(effectID, 0, turns, 1, true, spell, args, caster);
			}
		}
		
		private void applyEffect_90(ArrayList<Fighter> cibles, Fight fight)
		{
			if(turns <= 0)//Si Direct
			{
				int pAge = Formulas.getRandomJet(args.split(";")[5]);
				int val = pAge * (caster.getPDV()/100);
				//Calcul des Doms recus par le lanceur
				int finalDommage = applyOnHitBuffs(val,caster,caster,fight);//S'il y a des buffs spéciaux
				
				if(finalDommage>caster.getPDV())finalDommage = caster.getPDV();//Caster va mourrir
				caster.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", caster.getGUID()+","+finalDommage);
				
				//Application du soin
				for(Fighter target : cibles)
				{
					if((val+target.getPDV())> target.getPDVMAX())val = target.getPDVMAX()-target.getPDV();//Target va mourrir
					target.removePDV(-val);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+",+"+val);
				}
				if(caster.getPDV() <=0)
					fight.onFighterDie(caster);
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_108(ArrayList<Fighter> cibles, Fight fight)
		{
			if(spell == 441) 
			{
				return;
			}
			if(turns <= 0)
			{
				String [] jet = args.split(";");
				int dmg = 0;
				if(jet.length < 6)
				{
					dmg = 1;
				}else
				{
					dmg = Formulas.getRandomJet(jet[5]);
				}
				for(Fighter target : cibles)
				{
					if(spell == 139 && target.getTeam() != caster.getTeam())//Mot d'altruisme on saute les ennemis ?
					{
						continue;
					}
					if(spell == 59 && Integer.parseInt(args.split(";")[0]) == 200 && caster.getTeam() != target.getTeam())
					{
						continue;
					}
					if(spell == 59 && Integer.parseInt(args.split(";")[0]) == 100 && caster.getTeam() == target.getTeam())
					{
						continue;
					}
					int finalSoin = Formulas.calculFinalDommage(fight,caster, target,Constants.ELEMENT_FEU,  dmg, true,false,spell);
					if((finalSoin+target.getPDV())> target.getPDVMAX())finalSoin = target.getPDVMAX()-target.getPDV();//Target va mourrir
					target.removePDV(-finalSoin);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", target.getGUID()+",+"+finalSoin);
				}
			}else
			{
				for(Fighter target : cibles)
				{
					target.addBuff(effectID, 0, turns, 0, true, spell, args, caster);//on applique un buff
				}
			}
		}
		
		private void applyEffect_141(Fight fight,ArrayList<Fighter> cibles)
		{
			for(Fighter target : cibles)
			{
				if(target.hasBuff(765))//sacrifice
				{
					if(target.getBuff(765) != null && !target.getBuff(765).getCaster().isDead())
					{
						applyEffect_765B(fight,target);
						target = target.getBuff(765).getCaster();
					}
				}
				try{
					Thread.sleep(1500);
				}catch (InterruptedException e) {
					e.printStackTrace();
				}
				fight.onFighterDie(target);
			}
		}

		private void applyEffect_320(Fight fight, ArrayList<Fighter> cibles)
		{
			int value = 1;
			try
			{
				value = Integer.parseInt(args.split(";")[0]);
			}catch(NumberFormatException e){};
			int num = 0;
			for(Fighter target : cibles)
			{
				target.addBuff(Constants.STATS_REM_PO, value, turns,0, true, spell,args,caster);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7,Constants.STATS_REM_PO, caster.getGUID()+"", target.getGUID()+","+value+","+turns);
				num += value;
			}
			if(num != 0)
			{
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7,Constants.STATS_ADD_PO, caster.getGUID()+"", caster.getGUID()+","+num+","+turns);
				caster.addBuff(Constants.STATS_ADD_PO, num, 1, 0, true, spell,args,caster);
				//Gain de PO pendant le tour de jeu
				if(caster.canPlay()) caster.getTotalStats().addOneStat(Constants.STATS_ADD_PO, num);
			}
		}
		

		private void applyEffect_780(Fight fight)
		{
			Map<Integer,Fighter> deads = fight.getDeadList();
			Fighter target = null;
			for(Entry<Integer,Fighter> entry : deads.entrySet())
			{
				if(entry.getValue().hasLeft()) continue;
				if(entry.getValue().getTeam() == caster.getTeam())
					target = entry.getValue();
					 if(entry.getValue().isInvocation()) 
						if(entry.getValue().getInvocator().isDead()) 
							continue; 
			}
			if(target == null)
				return;
					
			fight.addFighterInTeam(target, target.getTeam());
			target.setIsDead(false);
			target.get_fightBuff().clear();
			if(!target.isInvocation()) 
				SocketManager.GAME_SEND_ILF_PACKET(target.getPersonnage(), 0); 
			else 
				fight.get_ordreJeu().add((fight.get_ordreJeu().indexOf(target.getInvocator())+1),target); 
				
			target.set_fightCell(cell);
			target.get_fightCell().addFighter(target);
					
			target.fullPDV();
			int percent = (100-value)*target.getPDVMAX()/100;
			target.removePDV(percent);
					
			String gm = target.getGmPacket('+').substring(3);
			String gtl = fight.getGTL();
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 181, target.getGUID() + "", gm);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 999, target.getGUID()+"", gtl);
			if(!target.isInvocation()) 
				SocketManager.GAME_SEND_STATS_PACKET(target.getPersonnage()); 
					
			fight.delOneDead(target);
		}

		public void setArgs(String newArgs)
		{
			args = newArgs;
		}
		public void setEffectID(int id)
		{
			effectID = id;
		}
	}