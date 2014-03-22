package common;


import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import common.World.Couple;

import objects.*;
import objects.Fight.*;
import objects.Guild.GuildMember;

public class Formulas {


	public static int getRandomValue(int i1,int i2)
	{
		Random rand = new Random();
		return (rand.nextInt((i2-i1)+1))+i1;
	}
	
	public static int getRandomJet(String jet)//1d5+6
	{
		try
		{
			int num = 0;
			int des = Integer.parseInt(jet.split("d")[0]);
			int faces = Integer.parseInt(jet.split("d")[1].split("\\+")[0]);
			int add = Integer.parseInt(jet.split("d")[1].split("\\+")[1]);
			for(int a=0;a<des;a++)
			{
				num += getRandomValue(1,faces);
			}
			num += add;
			return num;
		}catch(NumberFormatException e){return -1;}
	}
	public static int getMiddleJet(String jet)//1d5+6
	{
		try
		{
			int num = 0;
			int des = Integer.parseInt(jet.split("d")[0]);
			int faces = Integer.parseInt(jet.split("d")[1].split("\\+")[0]);
			int add = Integer.parseInt(jet.split("d")[1].split("\\+")[1]);
			num += ((1+faces)/2)*des;//on calcule moyenne
			num += add;
			return num;
		}catch(NumberFormatException e){return 0;}
	}
	public static int getTacleChance(Fighter tacleur, ArrayList<Fighter> tacle)
	{
		int agiTR = tacleur.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
		int agiT = 0;
		for(Fighter T : tacle) 
		{
			agiT += T.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
		}
		int a = agiTR+25;
		int b = agiTR+agiT+50;
		int chance = (int)((long)(300*a/b)-100);
		if(chance <10)chance = 10;
		if(chance >90)chance = 90;
		return chance;
	}

	public static int calculFinalHeal(Personnage caster,int jet)
	{
		int statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
		int soins = caster.getTotalStats().getEffect(Constants.STATS_ADD_SOIN);
		if(statC<0)statC=0;
		return (int)(jet * (100 + statC) / 100 + soins);
	}
	
	public static int calculFinalDommage(Fight fight,Fighter caster,Fighter target,int statID,int jet,boolean isHeal, boolean isCaC, int spellid)
	{
		float i = 0;//Bonus maitrise
		float j = 100; //Bonus de Classe
		float a = 1;//Calcul
		float num = 0;
		float statC = 0, domC = 0, perdomC = 0, resfT = 0, respT = 0;
		int multiplier = 0;
		if(!isHeal)
		{
			domC = caster.getTotalStats().getEffect(Constants.STATS_ADD_DOMA);
			perdomC = caster.getTotalStats().getEffect(Constants.STATS_ADD_PERDOM);
			multiplier = caster.getTotalStats().getEffect(Constants.STATS_MULTIPLY_DOMMAGE);
		}else
		{
			domC = caster.getTotalStats().getEffect(Constants.STATS_ADD_SOIN);
		}
		
		switch(statID)
		{
			case Constants.ELEMENT_NULL://Fixe
				statC = 0;
				resfT = 0;
				respT = 0;
				respT = 0;
			break;
			case Constants.ELEMENT_NEUTRE://neutre
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_FORC);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_NEU);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_NEU);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_NEU);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_NEU);
				}
				//on ajoute les dom Physique
				domC += caster.getTotalStats().getEffect(142);
				//Ajout de la resist Physique
				resfT = target.getTotalStats().getEffect(184);
			break;
			case Constants.ELEMENT_TERRE://force
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_FORC);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_TER);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_TER);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_TER);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_TER);
				}
				//on ajout les dom Physique
				domC += caster.getTotalStats().getEffect(142);
				//Ajout de la resist Physique
				resfT = target.getTotalStats().getEffect(184);
			break;
			case Constants.ELEMENT_EAU://chance
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_CHAN);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_EAU);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_EAU);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_EAU);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_EAU);
				}
				//Ajout de la resist Magique
				resfT = target.getTotalStats().getEffect(183);
			break;
			case Constants.ELEMENT_FEU://intell
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_FEU);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_FEU);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_FEU);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_FEU);
				}
				//Ajout de la resist Magique
				resfT = target.getTotalStats().getEffect(183);
			break;
			case Constants.ELEMENT_AIR://agilité
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_AIR);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_AIR);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_AIR);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_AIR);
				}
				//Ajout de la resist Magique
				resfT = target.getTotalStats().getEffect(183);
			break;
		}
		//On bride la resistance a 50% si c'est un joueur 
		if(target.getMob() == null && respT >50)respT = 50;
		
		if(statC<0)statC=0;
		/* DEBUG
		System.out.println("Jet: "+jet+" Stats: "+statC+" perdomC: "+perdomC+" multiplier: "+multiplier);
		System.out.println("(100 + statC + perdomC)= "+(100 + statC + perdomC));
		System.out.println("(jet * (100 + statC + perdomC + (multiplier*100) ) / 100)= "+(jet * ((100 + statC + perdomC) / 100 )));
		System.out.println("res Fix. T "+ resfT);
		System.out.println("res %age T "+respT);
		if(target.getMob() != null)
		{
			System.out.println("resmonstre: "+target.getMob().getStats().getEffect(Constants.STATS_ADD_RP_FEU));
			System.out.println("TotalStat: "+target.getTotalStats().getEffect(Constants.STATS_ADD_RP_FEU));
			System.out.println("FightStat: "+target.getTotalStatsLessBuff().getEffect(Constants.STATS_ADD_RP_FEU));
			
		}
		//*/
			if(caster.getPersonnage() != null && isCaC)
			{
			int ArmeType = caster.getPersonnage().getObjetByPos(1).getTemplate().getType();
			
			if((caster.getSpellValueBool(392) == true) && ArmeType == 2)//ARC
			{
				i = caster.getMaitriseDmg(392);
			}
			if((caster.getSpellValueBool(390) == true) && ArmeType == 4)//BATON
			{
				i = caster.getMaitriseDmg(390);
			}
			if((caster.getSpellValueBool(391) == true) && ArmeType == 6)//EPEE
			{
				i = caster.getMaitriseDmg(391);
			}
			if((caster.getSpellValueBool(393) == true) && ArmeType == 7)//MARTEAUX
			{
				i = caster.getMaitriseDmg(393);
			}
			if((caster.getSpellValueBool(394) == true) && ArmeType == 3)//BAGUETTE
			{
				i = caster.getMaitriseDmg(394);
			}
			if((caster.getSpellValueBool(395) == true) && ArmeType == 5)//DAGUES
			{
				i = caster.getMaitriseDmg(395);
			}
			if((caster.getSpellValueBool(396) == true) && ArmeType == 8)//PELLE
			{
				i = caster.getMaitriseDmg(396);
			}
			if((caster.getSpellValueBool(397) == true) && ArmeType == 19)//HACHE
			{
				i = caster.getMaitriseDmg(397);
			}
				a = (((100+i)/100)*(j/100));
			}
			
			num = a*(jet * ((100 + statC + perdomC + (multiplier*100)) / 100 ))+ domC;//dégats bruts
			
		//Poisons
		if(spellid != -1)
		{
			switch(spellid)
			{
				/* 
				 * case [SPELLID]: 
				 * statC = caster.getTotalStats().getEffect([EFFECT]) 
				 * num = (jet * ((100 + statC + perdomC + (multiplier*100)) / 100 ))+ domC; 
				 * return (int) num; 
				 */
				case 66 : 
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
				num = (jet * ((100 + statC + perdomC + (multiplier*100)) / 100 ))+ domC;
				if(target.hasBuff(105))
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(105).getValue());
					return 0;
				}
				if(target.hasBuff(184))
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(184).getValue());
					return 0;
				}
				return (int) num;
				
				case 71 :
				case 196:
				case 219:
					statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_FORC);
					num = (jet * ((100 + statC + perdomC + (multiplier*100)) / 100 ))+ domC;
					if(target.hasBuff(105))
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(105).getValue());
						return 0;
					}
					if(target.hasBuff(184))
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(184).getValue());
						return 0;
					}
				return (int) num;
				
				case 181:
				case 200:
					statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
					num = (jet * ((100 + statC + perdomC + (multiplier*100)) / 100 ))+ domC;
					if(target.hasBuff(105))
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(105).getValue());
						return 0;
					}
					if(target.hasBuff(184))
					{
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(184).getValue());
						return 0;
					}
				return (int) num;
			}
		}
		//Renvoie
		int renvoie = target.getTotalStatsLessBuff().getEffect(Constants.STATS_RETDOM);
		if(renvoie >0 && !isHeal)
		{
			if(renvoie > num)renvoie = (int)num;
			num -= renvoie;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 107, "-1", target.getGUID()+","+renvoie);
			if(renvoie>caster.getPDV())renvoie = caster.getPDV();
			if(num<1)num =0;
			caster.removePDV(renvoie);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", caster.getGUID()+",-"+renvoie);
		}
		
		if(!isHeal)num -= resfT;//resis fixe
		int reduc =	(int)((num/(float)100)*respT);//Reduc %resis
		if(!isHeal)num -= reduc;
		
		int armor = getArmorResist(target,statID);
		if(!isHeal)num -= armor;
		if(!isHeal)if(armor > 0)SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+armor);
		//dégats finaux
		if(num < 1)num=0;
		
		// Début Formule pour les MOBs
		if(caster.getPersonnage() == null && !caster.isPerco())
		{
			if(caster.getMob().getTemplate().getID() == 116)//Sacrifié Dommage = PDV*2
			{
				return (int)((num/25)*caster.getPDVMAX());
			}else
			{
			int niveauMob = caster.get_lvl();
			double CalculCoef = ((niveauMob*0.5)/100);
			int Multiplicateur = (int) Math.ceil(CalculCoef);
			return (int)num*Multiplicateur;
			}
		}
		// Fin Formule pour les MOBs
		else
		{
			return (int)num;
		}
	}

	public static int calculZaapCost(Carte map1,Carte map2)
	{
		return (int) (10*(Math.abs(map2.getX()-map1.getX())+Math.abs(map2.getY()-map1.getY())-1));
	}
	private static int getArmorResist(Fighter target, int statID)
	{
		int armor = 0;
		for(SpellEffect SE : target.getBuffsByEffectID(265))
		{
			Fighter fighter;
			
			switch(SE.getSpell())
			{
				case 1://Armure incandescente
					//Si pas element feu, on ignore l'armure
					if(statID != Constants.ELEMENT_FEU)continue;
					//Les stats du féca sont prises en compte
					fighter = SE.getCaster();
				break;
				case 6://Armure Terrestre
					//Si pas element terre/neutre, on ignore l'armure
					if(statID != Constants.ELEMENT_TERRE && statID != Constants.ELEMENT_NEUTRE)continue;
					//Les stats du féca sont prises en compte
					fighter = SE.getCaster();
				break;
				case 14://Armure Venteuse
					//Si pas element air, on ignore l'armure
					if(statID != Constants.ELEMENT_AIR)continue;
					//Les stats du féca sont prises en compte
					fighter = SE.getCaster();
				break;
				case 18://Armure aqueuse
					//Si pas element eau, on ignore l'armure
					if(statID != Constants.ELEMENT_EAU)continue;
					//Les stats du féca sont prises en compte
					fighter = SE.getCaster();
				break;
				
				default://Dans les autres cas on prend les stats de la cible et on ignore l'element de l'attaque
					fighter = target;
				break;
			}
			int intell = fighter.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
			int carac = 0;
			switch(statID)
			{
				case Constants.ELEMENT_AIR:
					carac = fighter.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
				break;
				case Constants.ELEMENT_FEU:
					carac = fighter.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
				break;
				case Constants.ELEMENT_EAU:
					carac = fighter.getTotalStats().getEffect(Constants.STATS_ADD_CHAN);
				break;
				case Constants.ELEMENT_NEUTRE:
				case Constants.ELEMENT_TERRE:
					carac = fighter.getTotalStats().getEffect(Constants.STATS_ADD_FORC);
				break;
			}
			int value = SE.getValue();
			int a = value * (100 + (int)(intell/2) + (int)(carac/2))/100;
			armor += a;
		}
		for(SpellEffect SE : target.getBuffsByEffectID(105))
		{
			int intell = target.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
			int carac = 0;
			switch(statID)
			{
				case Constants.ELEMENT_AIR:
					carac = target.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
				break;
				case Constants.ELEMENT_FEU:
					carac = target.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
				break;
				case Constants.ELEMENT_EAU:
					carac = target.getTotalStats().getEffect(Constants.STATS_ADD_CHAN);
				break;
				case Constants.ELEMENT_NEUTRE:
				case Constants.ELEMENT_TERRE:
					carac = target.getTotalStats().getEffect(Constants.STATS_ADD_FORC);
				break;
			}
			int value = SE.getValue();
			int a = value * (100 + (int)(intell/2) + (int)(carac/2))/100;
			armor += a;
		}
		return armor;
	}

	public static int getPointsLost(char z, int value, Fighter caster,Fighter target)
	{
		float esquiveC = z=='a'?caster.getTotalStats().getEffect(Constants.STATS_ADD_AFLEE):caster.getTotalStats().getEffect(Constants.STATS_ADD_MFLEE);
		float esquiveT = z=='a'?target.getTotalStats().getEffect(Constants.STATS_ADD_AFLEE):target.getTotalStats().getEffect(Constants.STATS_ADD_MFLEE);
		float ptsMax = z=='a'?target.getTotalStatsLessBuff().getEffect(Constants.STATS_ADD_PA):target.getTotalStatsLessBuff().getEffect(Constants.STATS_ADD_PM);
		
		int retrait = 0;

		for(int i = 0; i < value;i++)
		{
			if(ptsMax == 0 && target.getMob() != null)
			{
				ptsMax= z=='a'?target.getMob().getPA():target.getMob().getPM();
			}
			
			float pts = z =='a'?target.getPA():target.getPM();
			float ptsAct = pts - retrait;
			
			if(esquiveT == 0)esquiveT=1;
			if(esquiveC == 0)esquiveC=1;

			float a = (float)(esquiveC/esquiveT);
			float b = (ptsAct/ptsMax);

			float pourcentage = (float)(a*b*50);
			int chance = (int)Math.ceil(pourcentage);
			
			/*
			System.out.println("Esquive % : "+a+" Facteur PA/PM : "+b);
			System.out.println("ptsMax : "+ptsMax+" ptsAct : "+ptsAct);
			System.out.println("Chance d'esquiver le "+(i+1)+" eme PA/PM : "+chance);
			*/
			
			if(chance <0)chance = 0;
			if(chance >100)chance = 100;

			int jet = getRandomValue(0, 99);
			if(jet<chance)
			{
				retrait++;
			}
		}
		return retrait;
	}
	
	public static long getXpWinPerco(Percepteur perco, ArrayList<Fighter> winners,ArrayList<Fighter> loosers,long groupXP)
	{
			Guild G = World.getGuild(perco.get_guildID());
			float sag = G.get_Stats(Constants.STATS_ADD_SAGE);
			float coef = (sag + 100)/100;
			int taux = Ancestra.XP_PVM;
			long xpWin = 0;
			int lvlmax = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > lvlmax)
					lvlmax = entry.get_lvl();
			}
			int nbbonus = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > (lvlmax / 3))
					nbbonus += 1;				
			}
			
			double bonus = 1;
			if(nbbonus == 2)
				bonus = 1.1;
			if(nbbonus == 3)
				bonus = 1.3;
			if(nbbonus == 4)
				bonus = 2.2;
			if(nbbonus == 5)
				bonus = 2.5;
			if(nbbonus == 6)
				bonus = 2.8;
			if(nbbonus == 7)
				bonus = 3.1;
			if(nbbonus >= 8)
				bonus = 3.5;
			
			int lvlLoosers = 0;
			for(Fighter entry : loosers)
				lvlLoosers += entry.get_lvl();
			int lvlWinners = 0;
			for(Fighter entry : winners)
				lvlWinners += entry.get_lvl();
			double rapport = 1+((double)lvlLoosers/(double)lvlWinners);
			if (rapport <= 1.3)
				rapport = 1.3;
			/*
			if (rapport > 5)
				rapport = 5;
			//*/
			int lvl = G.get_lvl();
			double rapport2 = 1 + ((double)lvl / (double)lvlWinners);

			xpWin = (long) (groupXP * rapport * bonus * taux *coef * rapport2);
			
			/*/ DEBUG XP
			System.out.println("=========");
			System.out.println("groupXP: "+groupXP);
			System.out.println("rapport1: "+rapport);
			System.out.println("bonus: "+bonus);
			System.out.println("taux: "+taux);
			System.out.println("coef: "+coef);
			System.out.println("rapport2: "+rapport2);
			System.out.println("xpWin: "+xpWin);
			System.out.println("=========");
			//*/
			return xpWin;	
	}
	
	public static long getXpWinPvm2(Fighter perso, ArrayList<Fighter> winners,ArrayList<Fighter> loosers,long groupXP)
	{
		if(perso.getPersonnage()== null)return 0;
		if(winners.contains(perso))//Si winner
		{
			float sag = perso.getTotalStats().getEffect(Constants.STATS_ADD_SAGE);
			float coef = (sag + 100)/100;
			int taux = Ancestra.XP_PVM;
			long xpWin = 0;
			int lvlmax = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > lvlmax)
					lvlmax = entry.get_lvl();
			}
			int nbbonus = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > (lvlmax / 3))
					nbbonus += 1;				
			}
			
			double bonus = 1;
			if(nbbonus == 2)
				bonus = 1.1;
			if(nbbonus == 3)
				bonus = 1.3;
			if(nbbonus == 4)
				bonus = 2.2;
			if(nbbonus == 5)
				bonus = 2.5;
			if(nbbonus == 6)
				bonus = 2.8;
			if(nbbonus == 7)
				bonus = 3.1;
			if(nbbonus >= 8)
				bonus = 3.5;
			
			int lvlLoosers = 0;
			for(Fighter entry : loosers)
				lvlLoosers += entry.get_lvl();
			int lvlWinners = 0;
			for(Fighter entry : winners)
				lvlWinners += entry.get_lvl();
			double rapport = 1+((double)lvlLoosers/(double)lvlWinners);
			if (rapport <= 1.3)
				rapport = 1.3;
			/*
			if (rapport > 5)
				rapport = 5;
			//*/
			int lvl = perso.get_lvl();
			double rapport2 = 1 + ((double)lvl / (double)lvlWinners);

			xpWin = (long) (groupXP * rapport * bonus * taux *coef * rapport2);
			
			/*/ DEBUG XP
			System.out.println("=========");
			System.out.println("groupXP: "+groupXP);
			System.out.println("rapport1: "+rapport);
			System.out.println("bonus: "+bonus);
			System.out.println("taux: "+taux);
			System.out.println("coef: "+coef);
			System.out.println("rapport2: "+rapport2);
			System.out.println("xpWin: "+xpWin);
			System.out.println("=========");
			//*/
			return xpWin;	
		}
		return 0;
	}
	
	public static long getXpWinPvm(Fighter perso, ArrayList<Fighter> team,ArrayList<Fighter> loose, long groupXP)
	{
		int lvlwin = 0;
		for(Fighter entry : team)lvlwin += entry.get_lvl();
		int lvllos = 0;
		for(Fighter entry : loose)lvllos += entry.get_lvl();
		float bonusSage = (perso.getTotalStats().getEffect(Constants.STATS_ADD_SAGE)+100)/100;
		/* Formule 1
		float taux = perso.get_lvl()/lvlwin;
		long xp = (long)(groupXP * taux * bonusSage * perso.get_lvl());
		//*/
		//* Formule 2
		long sXp = groupXP*lvllos;
		long gXp = 2 * groupXP * perso.get_lvl();
        long xp = (long)((sXp + gXp)*bonusSage);
		//*/
		return xp*Ancestra.XP_PVM;
	}
	public static long getXpWinPvP(Fighter perso, ArrayList<Fighter> winners, ArrayList<Fighter> looser)
	{
		if(perso.getPersonnage()== null)return 0;
		if(winners.contains(perso.getGUID()))//Si winner
		{
			int lvlLoosers = 0;
			for(Fighter entry : looser)
				lvlLoosers += entry.get_lvl();
		
			int lvlWinners = 0;
			for(Fighter entry : winners)
				lvlWinners += entry.get_lvl();
			int taux = Ancestra.XP_PVP;
			float rapport = (float)lvlLoosers/(float)lvlWinners;
			long xpWin = (long)(
						(
							rapport
						*	getXpNeededAtLevel(perso.getPersonnage().get_lvl())
						/	100
						)
						*	taux
					);
			//DEBUG
			System.out.println("Taux: "+taux);
			System.out.println("Rapport: "+rapport);
			System.out.println("XpNeeded: "+getXpNeededAtLevel(perso.getPersonnage().get_lvl()));
			System.out.println("xpWin: "+xpWin);
			//*/
			return xpWin;
		}
		return 0;
	}
	
	private static long getXpNeededAtLevel(int lvl)
	{
		long xp = (World.getPersoXpMax(lvl) - World.getPersoXpMin(lvl));
		System.out.println("Xp Max => "+World.getPersoXpMax(lvl));
		System.out.println("Xp Min => "+World.getPersoXpMin(lvl));
		
		return xp;
	}

	public static long getGuildXpWin(Fighter perso, AtomicReference<Long> xpWin)
	{
		if(perso.getPersonnage()== null)return 0;
		if(perso.getPersonnage().getGuildMember() == null)return 0;
		

		GuildMember gm = perso.getPersonnage().getGuildMember();
		
		double xp = (double)xpWin.get(), Lvl = perso.get_lvl(),LvlGuild = perso.getPersonnage().get_guild().get_lvl(),pXpGive = (double)gm.getPXpGive()/100;
		
		double maxP = xp * pXpGive * 0.10;	//Le maximum donné à la guilde est 10% du montant prélevé sur l'xp du combat
		double diff = Math.abs(Lvl - LvlGuild);	//Calcul l'écart entre le niveau du personnage et le niveau de la guilde
		double toGuild;
		if(diff >= 70)
		{
			toGuild = maxP * 0.10;	//Si l'écart entre les deux level est de 70 ou plus, l'experience donnée a la guilde est de 10% la valeur maximum de don
		}
		else if(diff >= 31 && diff <= 69)
		{
			toGuild = maxP - ((maxP * 0.10) * (Math.floor((diff+30)/10)));
		}
		else if(diff >= 10 && diff <= 30)
		{
			toGuild = maxP - ((maxP * 0.20) * (Math.floor(diff/10))) ;
		}
		else	//Si la différence est [0,9]
		{
			toGuild = maxP;
		}
		xpWin.set((long)(xp - xp*pXpGive));
		return (long) Math.round(toGuild);
	}
	
	public static long getMountXpWin(Fighter perso, AtomicReference<Long> xpWin)
	{
		if(perso.getPersonnage()== null)return 0;
		if(perso.getPersonnage().getMount() == null)return 0;
		

		int diff = Math.abs(perso.get_lvl() - perso.getPersonnage().getMount().get_level());
		
		double coeff = 0;
		double xp = (double) xpWin.get();
		double pToMount = (double)perso.getPersonnage().getMountXpGive() / 100 + 0.2;
		
		if(diff >= 0 && diff <= 9)
			coeff = 0.1;
		else if(diff >= 10 && diff <= 19)
			coeff = 0.08;
		else if(diff >= 20 && diff <= 29)
			coeff = 0.06;
		else if(diff >= 30 && diff <= 39)
			coeff = 0.04;
		else if(diff >= 40 && diff <= 49)
			coeff = 0.03;
		else if(diff >= 50 && diff <= 59)
			coeff = 0.02;
		else if(diff >= 60 && diff <= 69)
			coeff = 0.015;
		else
			coeff = 0.01;
		
		if(pToMount > 0.2)
			xpWin.set((long)(xp - (xp*(pToMount-0.2))));
		
		return (long)Math.round(xp * pToMount * coeff);
	}

	public static int getKamasWin(Fighter i, ArrayList<Fighter> winners, int maxk, int mink)
	{
		maxk++;
		int rkamas = (int)(Math.random() * (maxk-mink)) + mink;
		return rkamas*Ancestra.KAMAS;
	}
	
	public static int getKamasWinPerco(int maxk, int mink)
	{
		maxk++;
		int rkamas = (int)(Math.random() * (maxk-mink)) + mink;
		return rkamas*Ancestra.KAMAS;
	}
	
	public static int calculElementChangeChance(int lvlM,int lvlA,int lvlP)
	{
		int K = 350;
		if(lvlP == 1)K = 100;
		else if (lvlP == 25)K = 175;
		else if (lvlP == 50)K = 350;
		return (int)((lvlM*100)/(K + lvlA));
	}

	public static int calculHonorWin(ArrayList<Fighter> winners,ArrayList<Fighter> loosers,Fighter F)
	{
		float totalGradeWin = 0;
		float totalLevelWin = 0;
		float totalGradeLoose = 0;
		float totalLevelLoose = 0;
		for(Fighter f : winners)
		{
			if(f.getPersonnage() == null )continue;
			totalLevelWin += f.get_lvl();
			totalGradeWin += f.getPersonnage().getGrade();

		}
		for(Fighter f : loosers)
		{
			if(f.getPersonnage() == null)continue;
			totalLevelLoose += f.get_lvl();
			totalGradeLoose += f.getPersonnage().getGrade();

		}
		
		if(totalLevelWin-totalLevelLoose > Ancestra.LVL_PVP) return 0;

		int base = (int)(100 * (float)(totalGradeLoose/totalGradeWin))/winners.size();
		if(loosers.contains(F))base = -base;
		return base * Ancestra.HONOR;
	}
	
	public static Couple<Integer, Integer> decompPierreAme(Objet toDecomp)
	{
		Couple<Integer, Integer> toReturn;
		String[] stats = toDecomp.parseStatsString().split("#");
		int lvlMax = Integer.parseInt(stats[3],16);
		int chance = Integer.parseInt(stats[1],16);
		toReturn = new Couple<Integer,Integer>(chance,lvlMax);
		
		return toReturn;
	}
	
	public static int totalCaptChance(int pierreChance, Personnage p)
	{
		int sortChance = 0;

		switch(p.getSortStatBySortIfHas(413).getLevel())
		{
			case 1:
				sortChance = 1;
				break;
			case 2:
				sortChance = 3;
				break;
			case 3:
				sortChance = 6;
				break;
			case 4:
				sortChance = 10;
				break;
			case 5:
				sortChance = 15;
				break;
			case 6:
				sortChance = 25;
				break;
		}
		
		return sortChance + pierreChance;
	}
	
	public static String parseReponse(String reponse)
	{
		StringBuilder toReturn = new StringBuilder("");
		
		String[] cut = reponse.split("[%]");
		
		if(cut.length == 1)return reponse;
		
		toReturn.append(cut[0]);
		
		char charact;
		for (int i = 1; i < cut.length; i++)
		{
			charact = (char) Integer.parseInt(cut[i].substring(0, 2),16);
			toReturn.append(charact).append(cut[i].substring(2));
		}
		
		return toReturn.toString();
	}
	
	public static int spellCost(int nb)
	{
		int total = 0;
		for (int i = 1; i < nb ; i++)
		{
			total += i;
		}
		
		return total;
	}
	
	public static int ChanceFM(int poidItemBase, int poidItemActual, int poidBaseJet, int poidActualJet, double poidRune, int Puis, double Coef)
	{
		int Chance = 0;
		int a = (poidItemBase+poidBaseJet+(Puis*2));
		int b = (int) (Math.sqrt(poidItemActual+poidActualJet+poidRune));
		if(b <= 0) b = 1;
		Chance = (int) Math.floor((a/b)*Coef);
		
		//DEBUG :
		System.out.println("A : "+a);
		System.out.println("B : "+b);
		return Chance;
	}
	
	public static int getTraqueXP(int lvl)
	{
		if(lvl < 50)return 10000 * Ancestra.XP_PVM;
		if(lvl < 60)return 65000 * Ancestra.XP_PVM;
		if(lvl < 70)return 90000 * Ancestra.XP_PVM;
		if(lvl < 80)return 120000 * Ancestra.XP_PVM;
		if(lvl < 90)return 160000 * Ancestra.XP_PVM;
		if(lvl < 100)return 210000 * Ancestra.XP_PVM;
		if(lvl < 110)return 270000 * Ancestra.XP_PVM;
		if(lvl < 120)return 350000 * Ancestra.XP_PVM;
		if(lvl < 130)return 440000 * Ancestra.XP_PVM;
		if(lvl < 140)return 540000 * Ancestra.XP_PVM;
		if(lvl < 150)return 650000 * Ancestra.XP_PVM;
		if(lvl < 155)return 760000 * Ancestra.XP_PVM;
		if(lvl < 160)return 880000 * Ancestra.XP_PVM;
		if(lvl < 165)return 1000000 * Ancestra.XP_PVM;
		if(lvl < 170)return 1130000 * Ancestra.XP_PVM;
		if(lvl < 175)return 1300000 * Ancestra.XP_PVM;
		if(lvl < 180)return 1500000 * Ancestra.XP_PVM;
		if(lvl < 185)return 1700000 * Ancestra.XP_PVM;
		if(lvl < 190)return 2000000 * Ancestra.XP_PVM;
		if(lvl < 195)return 2500000 * Ancestra.XP_PVM;
		if(lvl < 200)return 3000000 * Ancestra.XP_PVM;
		return 0;
	}
	
	public static int getLoosEnergy(int lvl, boolean isAgression, boolean isPerco)
	{
		int returned = 25*lvl;
		if(isAgression) returned *= (7/4);
		if(isPerco) returned *= (3/2);
		return returned;
	}
}
