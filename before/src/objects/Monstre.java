package objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Map.Entry;

import java.util.Timer;

import objects.Carte.Case;
import objects.Fight.Fighter;
import objects.Personnage.Stats;
import objects.Sort.SortStats;
import common.*;
import common.World.*;

public class Monstre
{
	private int ID;
	private int gfxID;
	private int align;
	private String colors;
	private int IAType = 0;
	private int minKamas;
	private int maxKamas;
	private Map<Integer,MobGrade> grades = new TreeMap<Integer,MobGrade>();
	private ArrayList<Drop> drops = new ArrayList<Drop>();
	private boolean isCapturable;
	
	public static class MobGroup
	{
		private int id;
		private int cellID;
		private int orientation = 2;
		private int align = -1;
		private int aggroDistance = 0;
		private boolean isFix = false;
		private Map<Integer,MobGrade> _Mobs = new TreeMap<Integer,MobGrade>();
		private String condition = "";
		private Timer _condTimer;

		public MobGroup(int Aid,int Aalign, ArrayList<MobGrade> possibles,Carte Map,int cell,int maxSize)
		{
			id = Aid;
			align = Aalign;
			//Détermination du nombre de mob du groupe

			int rand = 0;
			int nbr = 0;
			
			switch (maxSize)
			{
				case 0:
					return;
				case 1:
					nbr = 1;
					break;
				case 2:
					nbr = Formulas.getRandomValue(1,2);	//1:50%	2:50%
					break;
				case 3:
					nbr = Formulas.getRandomValue(1,3);	//1:33.3334%	2:33.3334%	3:33.3334%
					break;
				case 4:
					rand = Formulas.getRandomValue(0, 99);
					if(rand < 22)		//1:22%
						nbr = 1;
					else if(rand < 48)	//2:26%
						nbr = 2;
					else if(rand < 74)	//3:26%
						nbr = 3;

					else				//4:26%
						nbr = 4;
					break;
				case 5:
					rand = Formulas.getRandomValue(0, 99);
					if(rand < 15)		//1:15%
						nbr = 1;
					else if(rand < 35)	//2:20%
						nbr = 2;
					else if(rand < 60)	//3:25%
						nbr = 3;
					else if(rand < 85)	//4:25%
						nbr = 4;
					else				//5:15%
						nbr = 5;
					break;
				case 6:
					rand = Formulas.getRandomValue(0, 99);
					if(rand < 10)		//1:10%
						nbr = 1;
					else if(rand < 25)	//2:15%
						nbr = 2;
					else if(rand < 45)	//3:20%
						nbr = 3;
					else if(rand < 65)	//4:20%
						nbr = 4;
					else if(rand < 85)	//5:20%
						nbr = 5;
					else				//6:15%
						nbr = 6;
					break;
				case 7:
					rand = Formulas.getRandomValue(0, 99);
					if(rand < 9)		//1:9%
						nbr = 1;
					else if(rand < 20)	//2:11%
						nbr = 2;
					else if(rand < 35)	//3:15%
						nbr = 3;
					else if(rand < 55)	//4:20%
						nbr = 4;
					else if(rand < 75)	//5:20%
						nbr = 5;
					else if(rand < 91)	//6:16%
						nbr = 6;
					else				//7:9%
						nbr = 7;
					break;
				default:
					rand = Formulas.getRandomValue(0, 99);
					if(rand < 9)		//1:9%
						nbr = 1;
					else if(rand<20)	//2:11%
						nbr = 2;
					else if(rand<33)	//3:13%
						nbr = 3;
					else if(rand<50)	//4:17%
						nbr = 4;
					else if(rand<67)	//5:17%
						nbr = 5;
					else if(rand<80)	//6:13%
						nbr = 6;
					else if(rand<91)	//7:11%
						nbr = 7;
					else				//8:9%
						nbr = 8;
					break;
			}
			//On vérifie qu'il existe des monstres de l'alignement demandé pour éviter les boucles infinies
			boolean haveSameAlign = false;
			for(MobGrade mob : possibles)
			{
				if(mob.getTemplate().getAlign() == align)
					haveSameAlign = true;
			}
			
			if(!haveSameAlign)return;//S'il n'y en a pas
			int guid = -1;
			
			int maxLevel = 0;
			for(int a =0; a<nbr;a++)
			{
				MobGrade Mob = null;
				do
				{
					int random = Formulas.getRandomValue(0, possibles.size()-1);//on prend un mob au hasard dans le tableau
					Mob = possibles.get(random).getCopy();	
				}while(Mob.getTemplate().getAlign() != align);
				
				if(Mob.getLevel() > maxLevel)
					maxLevel = Mob.getLevel();
				
				_Mobs.put(guid, Mob);
				guid--;
			}
			aggroDistance = Constants.getAggroByLevel(maxLevel);
			
			if(align != Constants.ALIGNEMENT_NEUTRE)
				aggroDistance = 15;
			
			cellID = (cell==-1?Map.getRandomFreeCellID():cell);
			if(cellID == 0)return;
			
			orientation = Formulas.getRandomValue(0, 3)*2;
			isFix = false;
		}
		
		public MobGroup(int Aid, int cID, String groupData)
		{
			int maxLevel = 0;
			id = Aid;
			align = Constants.ALIGNEMENT_NEUTRE;
			cellID = cID;
			aggroDistance = Constants.getAggroByLevel(maxLevel);
			isFix = true;
			int guid = -1;
			for(String data : groupData.split(";"))
			{
				String[] infos = data.split(",");
				try
				{
					int id = Integer.parseInt(infos[0]);
					int min = Integer.parseInt(infos[1]);
					int max = Integer.parseInt(infos[2]);
					Monstre m = World.getMonstre(id);
					List<MobGrade> mgs = new ArrayList<MobGrade>();
					//on ajoute a la liste les grades possibles
					for(MobGrade MG : m.getGrades().values())if(MG.level >=min && MG.level<=max)mgs.add(MG);
					if(mgs.isEmpty())continue;
					//On prend un grade au hasard entre 0 et size -1 parmis les mobs possibles
					_Mobs.put(guid, mgs.get(Formulas.getRandomValue(0, mgs.size()-1)));
					guid--;
				}catch(Exception e){continue;};
			}
			orientation = (Formulas.getRandomValue(0, 3)*2)+1;
		}

		public int getID()
		{
			return id;
		}
		
		public int getCellID()
		{
			return cellID;
		}
		
		public int getOrientation()
		{
			return orientation;
		}
		
		public int getAggroDistance()
		{
			return aggroDistance;
		}
		public boolean isFix()
		{
			return isFix;
		}
		public void setOrientation(int o)
		{
			orientation = o;
		}
		
		public void setCellID(int id)
		{
			cellID = id;
		}
		
		public int getAlignement()
		{
			return align;
		}
		
		public MobGrade getMobGradeByID(int id)
		{
			return _Mobs.get(id);
		}
		
		public int getSize()
		{
			return _Mobs.size();
		}

		public String parseGM()
		{
			StringBuilder mobIDs = new StringBuilder();
			StringBuilder mobGFX = new StringBuilder();
			StringBuilder mobLevels = new StringBuilder();
			StringBuilder colors = new StringBuilder();
			StringBuilder toreturn = new StringBuilder();
			
			boolean isFirst = true;
			if(_Mobs.isEmpty())return "";
			
			for(Entry<Integer,MobGrade> entry : _Mobs.entrySet())
			{
				if(!isFirst)
				{
					mobIDs.append(",");
					mobGFX.append(",");
					mobLevels.append(",");
				}
				mobIDs.append(entry.getValue().getTemplate().getID());
				mobGFX.append(entry.getValue().getTemplate().getGfxID()).append("^100");
				mobLevels.append(entry.getValue().getLevel());
				colors.append(entry.getValue().getTemplate().getColors()).append(";0,0,0,0;");
				
				isFirst = false;
			}
			toreturn.append("+").append(cellID).append(";").append(orientation).append(";0;").append(id).append(";").append(mobIDs).append(";-3;").append(mobGFX).append(";").append(mobLevels).append(";").append(colors);
			return toreturn.toString();
		}

		public Map<Integer, MobGrade> getMobs() {
			return _Mobs;
		}
		public void setCondition(String cond)
		{
			this.condition = cond;
		}
		public String getCondition()
		{
			return this.condition;
		}
		public void setIsFix(boolean fix)
		{
			this.isFix = fix;
		}
		public void startCondTimer()
		{
			this._condTimer = new Timer();
			_condTimer.schedule(new TimerTask() {
				
				public void run() {
					condition = "";
					
				}
			}, Ancestra.CONFIG_ARENA_TIMER);
		}
		public void stopConditionTimer()
		{
			try
			{
				this._condTimer.cancel();
			}catch(Exception e)
			{
				
			}
		}
			
	}
	
	public static class MobGrade
	{
		private Monstre template;
		private int grade;
		private int level;
		private int PDV;
		private int inFightID;
		private int PDVMAX;
		private int init;
		private int PA;
		private int PM;
		private Case fightCell;
		private int baseXp = 10;
		private ArrayList<SpellEffect> _fightBuffs = new ArrayList<SpellEffect>();
		private Map<Integer,Integer> stats = new TreeMap<Integer,Integer>();
		private Map<Integer,SortStats> spells = new TreeMap<Integer,SortStats>();
		
		public MobGrade(Monstre aTemp, int Agrade, int Alevel,int aPA,int aPM, String Aresist, String Astats, String Aspells,int pdvMax,int aInit, int xp)
		{
			template = aTemp;
			grade = Agrade;
			level = Alevel;
			PDVMAX = pdvMax;
			PDV = PDVMAX;
			PA = aPA;
			PM = aPM;
			baseXp = xp;
			init = aInit;
			String[] resists = Aresist.split(";");
			String[] statsArray = Astats.split(",");
			int RN = 0,RF = 0, RE = 0, RA = 0, RT = 0, AF = 0, MF = 0,force = 0, intell = 0, sagesse = 0,chance = 0, agilite = 0;
			try
			{
				RN = Integer.parseInt(resists[0]);
				RT = Integer.parseInt(resists[1]);
				RF = Integer.parseInt(resists[2]);
				RE = Integer.parseInt(resists[3]);
				RA = Integer.parseInt(resists[4]);
				AF = Integer.parseInt(resists[5]);
				MF = Integer.parseInt(resists[6]);
				force = Integer.parseInt(statsArray[0]);
				sagesse = Integer.parseInt(statsArray[1]);
				intell = Integer.parseInt(statsArray[2]);
				chance = Integer.parseInt(statsArray[3]);
				agilite = Integer.parseInt(statsArray[4]);
			}catch(Exception e){e.printStackTrace();};
			
			stats.clear();
			stats.put(Constants.STATS_ADD_FORC, force);
			stats.put(Constants.STATS_ADD_SAGE, sagesse);
			stats.put(Constants.STATS_ADD_INTE, intell);
			stats.put(Constants.STATS_ADD_CHAN, chance);
			stats.put(Constants.STATS_ADD_AGIL, agilite);
			stats.put(Constants.STATS_ADD_RP_NEU, RN);
			stats.put(Constants.STATS_ADD_RP_FEU, RF);
			stats.put(Constants.STATS_ADD_RP_EAU, RE);
			stats.put(Constants.STATS_ADD_RP_AIR, RA);
			stats.put(Constants.STATS_ADD_RP_TER, RT);
			stats.put(Constants.STATS_ADD_AFLEE, AF);
			stats.put(Constants.STATS_ADD_MFLEE, MF);
			
			spells.clear();
			String[] spellsArray = Aspells.split(";");
			for(String str : spellsArray)
			{
				if(str.equals(""))continue;
				String[] spellInfo = str.split("@");
				int spellID = 0;
				int spellLvl = 0;
				try
				{
					spellID = Integer.parseInt(spellInfo[0]);
					spellLvl = Integer.parseInt(spellInfo[1]);
				}catch(Exception e){continue;};
				if(spellID == 0 || spellLvl == 0)continue;
				
				Sort sort = World.getSort(spellID);
				if(sort == null)continue;
				SortStats SpellStats = sort.getStatsByLevel(spellLvl);
				if(SpellStats == null)continue;
				
				spells.put(spellID, SpellStats);
			}
		}
	
		private MobGrade(Monstre template2, int grade2, int level2, int pdv2,int pdvmax2,int aPA,int aPM, Map<Integer, Integer> stats2,Map<Integer, SortStats> spells2,int xp)
		{
			template = template2;
			grade = grade2;
			level = level2;
			PDV = pdv2;
			PDVMAX = pdvmax2;
			PA = aPA;
			PM = aPM;
			stats = stats2;
			spells = spells2;
			inFightID = -1;
			baseXp = xp;
		}
		public ArrayList<Drop> getDrops()
		{
			return template.getDrops();
		}
		public int getBaseXp()
		{
			return baseXp;
		}
		public int getInit() {
			return init;
		}

		public MobGrade getCopy()
		{
			Map<Integer,Integer> newStats = new TreeMap<Integer,Integer>();
			newStats.putAll(stats);
			return new MobGrade(template,grade,level,PDV,PDVMAX,PA,PM,newStats,spells,baseXp);
		}

		public Stats getStats()
		{
			return new Stats(stats);
		}
		
		public int getLevel()
		{
			return level;
		}
		
		public ArrayList<SpellEffect> getBuffs()
		{
			return _fightBuffs;
		}
		
		public Case getFightCell()
		{
			return fightCell;
		}
		
		public void setFightCell(Case cell)
		{
			fightCell = cell;
		}
		
		public Map<Integer,SortStats> getSpells()
		{
			return spells;
		}
		
		public Monstre getTemplate()
		{
			return template;
		}
		
		public int getPDV() {
			return PDV;
		}

		public void setPDV(int pdv) {
			PDV = pdv;
		}

		public int getPDVMAX() {
			return PDVMAX;
		}

		public int getGrade()
		{
			return grade;
		}

		public void setInFightID(int i)
		{
			inFightID = i;
		}
		public int getInFightID()
		{
			return inFightID;
		}

		public int getPA()
		{
			return PA;
		}
		public int getPM()
		{
			return PM;
		}

		public void modifStatByInvocator(Fighter caster)
		{
			int coef = (1 + (caster.get_lvl())/100);
			PDV = (PDVMAX)*coef;
			PDVMAX = PDV;
			int force = stats.get(Constants.STATS_ADD_FORC)*coef;
			int intel = stats.get(Constants.STATS_ADD_INTE)*coef;
			int agili = stats.get(Constants.STATS_ADD_AGIL)*coef;
			int sages = stats.get(Constants.STATS_ADD_SAGE)*coef;
			int chanc = stats.get(Constants.STATS_ADD_CHAN)*coef;
			stats.put(Constants.STATS_ADD_FORC, force);
			stats.put(Constants.STATS_ADD_INTE, intel);
			stats.put(Constants.STATS_ADD_AGIL, agili);
			stats.put(Constants.STATS_ADD_SAGE, sages);
			stats.put(Constants.STATS_ADD_CHAN, chanc);	
		}
	}

	public Monstre(int Aid, int agfx, int Aalign, String Acolors, String Agrades, String Aspells,String Astats,String aPdvs,String aPoints,String aInit,int mK,int MK,String xpstr,int IAtype,boolean capturable)
	{
		ID = Aid;
		gfxID = agfx;
		align = Aalign;
		colors = Acolors;
		minKamas = mK;
		maxKamas = MK;
		IAType = IAtype;
		isCapturable = capturable;
		int G = 1;
		for(int n = 0; n<11; n++)
		{
			try
			{
				//Grades
				String grade = Agrades.split("\\|")[n];
				String[] infos = grade.split("@");
				int level = Integer.parseInt(infos[0]);
				String resists = infos[1];
				//Stats
				String stats =  Astats.split("\\|")[n];
				//Spells
				String spells =  Aspells.split("\\|")[n];
				if(spells.equals("-1"))spells ="";
				//PDVMax//init
				int pdvmax = 1;
				int init = 1;
				try
				{
					pdvmax = Integer.parseInt(aPdvs.split("\\|")[n]);
					init = Integer.parseInt(aInit.split("\\|")[n]);
				}catch(Exception e){};
				//PA / PM
				int PA = 3;
				int PM = 3;
				int xp = 10;
				try
				{
					String[] pts = aPoints.split("\\|")[n].split(";");
					try
					{
						PA = Integer.parseInt(pts[0]);
					}catch(Exception e1){};
					try
					{
						PM = Integer.parseInt(pts[1]);
					}catch(Exception e1){};
					try
					{
						xp = Integer.parseInt(xpstr.split("\\|")[n]);
					}catch(Exception e1){e1.printStackTrace();};
				}catch(Exception e){e.printStackTrace();};
				grades.put
					(G,
						new MobGrade
						(
							this,
							G,
							level,
							PA,
							PM,
							resists,
							stats,
							spells,
							pdvmax,
							init,
							xp
						)
					);
				G++;
			}catch(Exception e){continue;};	
		}	
	}
	
	public int getID() {
		return ID;
	}
	public void addDrop(Drop D)
	{
		drops.add(D);
	}
	public ArrayList<Drop> getDrops()
	{
		return drops;
	}
	public int getGfxID() {
		return gfxID;
	}
	
	public int getMinKamas() {
		return minKamas;
	}

	public int getMaxKamas() {
		return maxKamas;
	}

	public int getAlign() {
		return align;
	}
	
	public String getColors() {
		return colors;
	}
	
	public int getIAType() {
		return IAType;
	}
	
	public Map<Integer, MobGrade> getGrades() {
		return grades;
	}

	public MobGrade getGradeByLevel(int lvl)
	{
		for(Entry<Integer,MobGrade> grade : grades.entrySet())
		{
			if(grade.getValue().getLevel() == lvl)
				return grade.getValue();
		}
		return null;
	}
	
	public MobGrade getRandomGrade()
	{
		int randomgrade = (int)(Math.random() * (6-1)) + 1; 
		int graderandom=1;
		for(Entry<Integer,MobGrade> grade : grades.entrySet())
		{
			if(graderandom == randomgrade)
			{
				return grade.getValue();
			}
			else{
				graderandom++;
				}
		}
		return null;
	}
	
	public boolean isCapturable()
	{
		return this.isCapturable;
	}
}
