package common;

import game.GameServer;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;


import objects.Personnage;
import objects.Metier.JobAction;
import objects.Objet.ObjTemplate;
import objects.Personnage.Stats;
import objects.Sort.SortStats;

public class Constants
{
	//DEBUG
	public static int DEBUG_MAP_LIMIT 	=	20000;
	//Server
	public static final String SERVER_VERSION	=	"0.5.6";
	public static final String SERVER_MAKER		=	"DeathDown";
	//Versions
	public static final	String CLIENT_VERSION	=	"1.29.1";
	public static final boolean IGNORE_VERSION 		= false;
	//ZAAPI <alignID,{mapID,mapID,...,mapID}>
	public static Map<Integer, String> ZAAPI = new TreeMap<Integer, String>();
	//ZAAP <mapID,cellID>
	public static Map<Integer, Integer> ZAAPS = new TreeMap<Integer, Integer>();
	//BANIP
	public static String BAN_IP = "";
	
	public static boolean IPcompareToBanIP(String ip)
	{
		String[] split = BAN_IP.split(",");
		for(String ipsplit : split)
		{
			if(ip.compareTo(ipsplit) == 0) return true;
		}
		
		return false;
	}
	
	//Valeur des droits de guilde
	public static int G_BOOST = 2;			//Gérer les boost
	public static int G_RIGHT = 4;			//Gérer les droits
	public static int G_INVITE = 8;			//Inviter de nouveaux membres
	public static int G_BAN = 16;				//Bannir
	public static int G_ALLXP = 32;			//Gérer les répartitions d'xp
	public static int G_HISXP = 256;			//Gérer sa répartition d'xp
	public static int G_RANK = 64;			//Gérer les rangs
	public static int G_POSPERCO = 128;		//Poser un percepteur
	public static int G_COLLPERCO = 512;		//Collecter les percepteurs
	public static int G_USEENCLOS = 4096;		//Utiliser les enclos
	public static int G_AMENCLOS = 8192;		//Aménager les enclos
	public static int G_OTHDINDE = 16384;		//Gérer les montures des autres membres
	
	//Valeur des droits de maison
	public static int H_GBLASON = 2; //Afficher blason pour membre de la guilde
	public static int H_OBLASON = 4; //Afficher blason pour les autres
	public static int H_GNOCODE = 8; //Entrer sans code pour la guilde
	public static int H_OCANTOPEN = 16; //Entrer impossible pour les non-guildeux
	public static int C_GNOCODE = 32; //Coffre sans code pour la guilde
	public static int C_OCANTOPEN = 64; //Coffre impossible pour les non-guildeux
	public static int H_GREPOS = 256; //Guilde droit au repos
	public static int H_GTELE = 128; //Guilde droit a la TP
	
	//ETAT
	public static final int ETAT_NEUTRE				= 0;
	public static final int ETAT_SAOUL				= 1;
	public static final int ETAT_CAPT_AME			= 2;
	public static final int ETAT_PORTEUR			= 3;
	public static final int ETAT_PEUREUX			= 4;
	public static final int ETAT_DESORIENTE			= 5;
	public static final int ETAT_ENRACINE			= 6;
	public static final int ETAT_PESANTEUR			= 7;
	public static final int ETAT_PORTE				= 8;
	public static final int ETAT_MOTIV_SYLVESTRE	= 9;
	public static final int ETAT_APPRIVOISEMENT		= 10;
	public static final int ETAT_CHEVAUCHANT		= 11;
	//INTERACTIVE OBJET
	public static final int IOBJECT_STATE_FULL		= 1;
	public static final int IOBJECT_STATE_EMPTYING	= 2;
	public static final int IOBJECT_STATE_EMPTY		= 3;
	public static final int IOBJECT_STATE_EMPTY2	= 4;
	public static final int IOBJECT_STATE_FULLING	= 5;
	//FIGHT
	public static final int TIME_BY_TURN			= 29*1000;
	public static final int FIGHT_TYPE_CHALLENGE 	= 0;//Défies
	public static final int FIGHT_TYPE_AGRESSION 	= 1;//Aggros
	public static final int FIGHT_TYPE_PVMA			= 2;//??
	public static final int FIGHT_TYPE_MXVM			= 3;//??
	public static final int FIGHT_TYPE_PVM			= 4;//PvM
	public static final int FIGHT_TYPE_PVT			= 5;//Percepteur
	public static final int FIGHT_TYPE_PVMU			= 6;//??
	public static final int FIGHT_STATE_INIT		= 1;
	public static final int FIGHT_STATE_PLACE		= 2;
	public static final int FIGHT_STATE_ACTIVE 		= 3;
	public static final int FIGHT_STATE_FINISHED	= 4;
	//Jobs
	public static final int JOB_BASE				= 1;
	public static final int JOB_BUCHERON			= 2;
	public static final int JOB_F_EPEE				= 11;
	public static final int JOB_S_ARC				= 13;
	public static final int JOB_F_MARTEAU			= 14;
	public static final int JOB_CORDONIER			= 15;
	public static final int JOB_BIJOUTIER			= 16;
	public static final int JOB_F_DAGUE				= 17;
	public static final int JOB_S_BATON				= 18;
	public static final int JOB_S_BAGUETTE			= 19;
	public static final int JOB_F_PELLE				= 20;
	public static final int JOB_MINEUR				= 24;
	public static final int JOB_BOULANGER			= 25;
	public static final int JOB_ALCHIMISTE			= 26;
	public static final int JOB_TAILLEUR			= 27;
	public static final int JOB_PAYSAN				= 28;
	public static final int JOB_F_HACHES			= 31;
	public static final int JOB_PECHEUR				= 36;
	public static final int JOB_CHASSEUR			= 41;
	public static final int JOB_FM_DAGUE			= 43;
	public static final int JOB_FM_EPEE				= 44;
	public static final int JOB_FM_MARTEAU			= 45;
	public static final int JOB_FM_PELLE			= 46;
	public static final int JOB_FM_HACHES			= 47;
	public static final int JOB_SM_ARC				= 48;
	public static final int JOB_SM_BAGUETTE			= 49;
	public static final int JOB_SM_BATON			= 50;
	public static final int JOB_BOUCHER				= 56;
	public static final int JOB_POISSONNIER			= 58;
	public static final int JOB_F_BOUCLIER			= 60;
	public static final int JOB_CORDOMAGE			= 62;
	public static final int JOB_JOAILLOMAGE			= 63;
	public static final int JOB_COSTUMAGE			= 64;
	public static final int JOB_BRICOLEUR			= 65;
	public static final int JOB_JOAILLER			= 66;
	public static final int JOB_BIJOUTIER2			= 67;
	
	//Items
		//Positions
		public static final int ITEM_POS_NO_EQUIPED 	= -1;
		public static final int ITEM_POS_AMULETTE		= 0;
		public static final int ITEM_POS_ARME			= 1;
		public static final int ITEM_POS_ANNEAU1		= 2;
		public static final int ITEM_POS_CEINTURE		= 3;
		public static final int ITEM_POS_ANNEAU2		= 4;
		public static final int ITEM_POS_BOTTES			= 5;
		public static final int ITEM_POS_COIFFE		 	= 6;
		public static final int ITEM_POS_CAPE			= 7;
		public static final int ITEM_POS_FAMILIER		= 8;
		public static final int ITEM_POS_DOFUS1			= 9;
		public static final int ITEM_POS_DOFUS2			= 10;
		public static final int ITEM_POS_DOFUS3			= 11;
		public static final int ITEM_POS_DOFUS4			= 12;
		public static final int ITEM_POS_DOFUS5			= 13;
		public static final int ITEM_POS_DOFUS6			= 14;
		public static final int ITEM_POS_BOUCLIER		= 15;
		
		//Types
		public static final int ITEM_TYPE_AMULETTE			= 1;
		public static final int ITEM_TYPE_ARC				= 2;
		public static final int ITEM_TYPE_BAGUETTE			= 3;
		public static final int ITEM_TYPE_BATON				= 4;
		public static final int ITEM_TYPE_DAGUES			= 5;
		public static final int ITEM_TYPE_EPEE				= 6;
		public static final int ITEM_TYPE_MARTEAU			= 7;
		public static final int ITEM_TYPE_PELLE				= 8;
		public static final int ITEM_TYPE_ANNEAU			= 9;
		public static final int ITEM_TYPE_CEINTURE			= 10;
		public static final int ITEM_TYPE_BOTTES			= 11;
		public static final int ITEM_TYPE_POTION			= 12;
		public static final int ITEM_TYPE_PARCHO_EXP		= 13;
		public static final int ITEM_TYPE_DONS				= 14;
		public static final int ITEM_TYPE_RESSOURCE			= 15;
		public static final int ITEM_TYPE_COIFFE			= 16;
		public static final int ITEM_TYPE_CAPE				= 17;
		public static final int ITEM_TYPE_FAMILIER			= 18;
		public static final int ITEM_TYPE_HACHE				= 19;
		public static final int ITEM_TYPE_OUTIL				= 20;
		public static final int ITEM_TYPE_PIOCHE			= 21;
		public static final int ITEM_TYPE_FAUX				= 22;
		public static final int ITEM_TYPE_DOFUS				= 23;
		public static final int ITEM_TYPE_QUETES			= 24;
		public static final int ITEM_TYPE_DOCUMENT			= 25;
		public static final int ITEM_TYPE_FM_POTION			= 26;
		public static final int ITEM_TYPE_TRANSFORM			= 27;
		public static final int ITEM_TYPE_BOOST_FOOD		= 28;
		public static final int ITEM_TYPE_BENEDICTION		= 29;
		public static final int ITEM_TYPE_MALEDICTION		= 30;
		public static final int ITEM_TYPE_RP_BUFF			= 31;
		public static final int ITEM_TYPE_PERSO_SUIVEUR		= 32;
		public static final int ITEM_TYPE_PAIN				= 33;
		public static final int ITEM_TYPE_CEREALE			= 34;
		public static final int ITEM_TYPE_FLEUR				= 35;
		public static final int ITEM_TYPE_PLANTE			= 36;
		public static final int ITEM_TYPE_BIERE				= 37;
		public static final int ITEM_TYPE_BOIS				= 38;
		public static final int ITEM_TYPE_MINERAIS			= 39;
		public static final int ITEM_TYPE_ALLIAGE			= 40;
		public static final int ITEM_TYPE_POISSON			= 41;
		public static final int ITEM_TYPE_BONBON			= 42;
		public static final int ITEM_TYPE_POTION_OUBLIE		= 43;
		public static final int ITEM_TYPE_POTION_METIER		= 44;
		public static final int ITEM_TYPE_POTION_SORT		= 45;
		public static final int ITEM_TYPE_FRUIT				= 46;
		public static final int ITEM_TYPE_OS				= 47;
		public static final int ITEM_TYPE_POUDRE			= 48;
		public static final int ITEM_TYPE_COMESTI_POISSON	= 49;
		public static final int ITEM_TYPE_PIERRE_PRECIEUSE	= 50;
		public static final int ITEM_TYPE_PIERRE_BRUTE		=51;
		public static final int ITEM_TYPE_FARINE			=52;
		public static final int ITEM_TYPE_PLUME				=53;
		public static final int ITEM_TYPE_POIL				=54;
		public static final int ITEM_TYPE_ETOFFE			=55;
		public static final int ITEM_TYPE_CUIR				=56;
		public static final int ITEM_TYPE_LAINE				=57;
		public static final int ITEM_TYPE_GRAINE			=58;
		public static final int ITEM_TYPE_PEAU				=59;
		public static final int ITEM_TYPE_HUILE				=60;
		public static final int ITEM_TYPE_PELUCHE			=61;
		public static final int ITEM_TYPE_POISSON_VIDE		=62;
		public static final int ITEM_TYPE_VIANDE			=63;
		public static final int ITEM_TYPE_VIANDE_CONSERVEE	=64;
		public static final int ITEM_TYPE_QUEUE				=65;
		public static final int ITEM_TYPE_METARIA			=66;
		public static final int ITEM_TYPE_LEGUME			=68;
		public static final int ITEM_TYPE_VIANDE_COMESTIBLE	=69;
		public static final int ITEM_TYPE_TEINTURE			=70;
		public static final int ITEM_TYPE_EQUIP_ALCHIMIE	=71;
		public static final int ITEM_TYPE_OEUF_FAMILIER		=72;
		public static final int ITEM_TYPE_MAITRISE			=73;
		public static final int ITEM_TYPE_FEE_ARTIFICE		=74;
		public static final int ITEM_TYPE_PARCHEMIN_SORT	=75;
		public static final int ITEM_TYPE_PARCHEMIN_CARAC	=76;
		public static final int ITEM_TYPE_CERTIFICAT_CHANIL	=77;
		public static final int ITEM_TYPE_RUNE_FORGEMAGIE	=78;
		public static final int ITEM_TYPE_BOISSON			=79;
		public static final int ITEM_TYPE_OBJET_MISSION		=80;
		public static final int ITEM_TYPE_SAC_DOS			=81;
		public static final int ITEM_TYPE_BOUCLIER			=82;
		public static final int ITEM_TYPE_PIERRE_AME		=83;
		public static final int ITEM_TYPE_CLEFS				=84;
		public static final int ITEM_TYPE_PIERRE_AME_PLEINE	=85;
		public static final int ITEM_TYPE_POPO_OUBLI_PERCEP	=86;
		public static final int ITEM_TYPE_PARCHO_RECHERCHE	=87;
		public static final int ITEM_TYPE_PIERRE_MAGIQUE	=88;
		public static final int ITEM_TYPE_CADEAUX			=89;
		public static final int ITEM_TYPE_FANTOME_FAMILIER	=90;
		public static final int ITEM_TYPE_DRAGODINDE		=91;
		public static final int ITEM_TYPE_BOUFTOU			=92;
		public static final int ITEM_TYPE_OBJET_ELEVAGE		=93;
		public static final int ITEM_TYPE_OBJET_UTILISABLE	=94;
		public static final int ITEM_TYPE_PLANCHE			=95;
		public static final int ITEM_TYPE_ECORCE			=96;
		public static final int ITEM_TYPE_CERTIF_MONTURE	=97;
		public static final int ITEM_TYPE_RACINE			=98;
		public static final int ITEM_TYPE_FILET_CAPTURE		=99;
		public static final int ITEM_TYPE_SAC_RESSOURCE		=100;
		public static final int ITEM_TYPE_ARBALETE			=102;
		public static final int ITEM_TYPE_PATTE				=103;
		public static final int ITEM_TYPE_AILE				=104;
		public static final int ITEM_TYPE_OEUF				=105;
		public static final int ITEM_TYPE_OREILLE			=106;
		public static final int ITEM_TYPE_CARAPACE			=107;
		public static final int ITEM_TYPE_BOURGEON			=108;
		public static final int ITEM_TYPE_OEIL				=109;
		public static final int ITEM_TYPE_GELEE				=110;
		public static final int ITEM_TYPE_COQUILLE			=111;
		public static final int ITEM_TYPE_PRISME			=112;
		public static final int ITEM_TYPE_OBJET_VIVANT		=113;
		public static final int ITEM_TYPE_ARME_MAGIQUE		=114;
		public static final int ITEM_TYPE_FRAGM_AME_SHUSHU	=115;
		public static final int ITEM_TYPE_POTION_FAMILIER	=116;
		
	//Alignement
	public static final int ALIGNEMENT_NEUTRE		=	-1;
	public static final int ALIGNEMENT_BONTARIEN	=	1;
	public static final int ALIGNEMENT_BRAKMARIEN	=	2;
	public static final int ALIGNEMENT_MERCENAIRE	=	3;
	
	//Elements 
	public static final int ELEMENT_NULL		=	-1;
	public static final int ELEMENT_NEUTRE		= 	0;
	public static final int ELEMENT_TERRE		= 	1;
	public static final int ELEMENT_EAU			= 	2;
	public static final int ELEMENT_FEU			= 	3;
	public static final int ELEMENT_AIR			= 	4;
	//Classes
	public static final int CLASS_FECA			= 	1;
	public static final int CLASS_OSAMODAS		= 	2;
	public static final int CLASS_ENUTROF		= 	3;
	public static final int CLASS_SRAM			=	4;
	public static final int CLASS_XELOR			=	5;
	public static final int CLASS_ECAFLIP		=	6;
	public static final int CLASS_ENIRIPSA		=	7;
	public static final int CLASS_IOP			=	8;
	public static final int CLASS_CRA			=	9;
	public static final int CLASS_SADIDA		= 	10;
	public static final int CLASS_SACRIEUR		=	11;
	public static final int CLASS_PANDAWA		=	12;
	//Sexes
	public static final int SEX_MALE 			=	0;
	public static final int SEX_FEMALE			=	1;
	//GamePlay
	public static final int MAX_EFFECTS_ID 		=	1500;
	//Buff a vérifier en début de tour
	public static final int[] BEGIN_TURN_BUFF	=	{91,92,93,94,95,96,97,98,99,100,108};
	//Buff des Armes
	public static final int[] ARMES_EFFECT_IDS	=	{91,92,93,94,95,96,97,98,99,100,101};
	//Buff a ne pas booster en cas de CC
	public static final int[] NO_BOOST_CC_IDS	=	{101};
	//Invocation Statiques
	public static final int[] STATIC_INVOCATIONS 		= 	{282,556};//Arbre et Cawotte s'tout :p
	
	//Verif d'Etat au lancement d'un sort {spellID,stateID}, à completer avant d'activer
	public static final int[][] STATE_REQUIRED =
	{
		{699,Constants.ETAT_SAOUL},
		{690,Constants.ETAT_SAOUL}
	};
	//Action de Métier {skillID,objetRecolté,objSpécial}
	public static final int[][] JOB_ACTION =
	{
		//Bucheron
		{101},{6,303},{39,473},{40,476},{10,460},{141,2357},{139,2358},{37,471},{154,7013},{33,461},{41,474},{34,449},{174,7925},{155,7016},{38,472},{35,470},{158,7963},
		//Mineur
		{48},{32},{24,312},{25,441},{26,442},{28,443},{56,445},{162,7032},{55,444},{29,350},{31,446},{30,313},{161,7033},
		//Pêcheur
		{133},{128,598,1786},{128,1757,1759},{128,1750,1754},{124,603,1762},{124,1782,1790},{124,1844,607},{136,2187},{125,1847,1849},{125,1794,1796},{140,1799,1759},{129,600,1799},{129,1805,1807},{126,1779,1792},{130,1784,1788},{127,1801,1803},{131,602,1853},
		//Alchi
		{23},{68,421},{54,428},{71,395},{72,380},{73,593},{74,594},{160,7059},
		//Paysan
		{122},{47},{45,289,2018},{53,400,2032},{57,533,2036},{46,401,2021},{50,423,2026},{52,532,2029},{159,7018},{58,405},{54,425,2035},
		//Boulanger
		{109},{27},
		//Poissonier
		{135},
		//Boucher
		{132},
		//Chasseur
		{134},
		//Tailleur
		{64},{123},{63},
		//Bijoutier
		{11},{12},
		//Cordonnier
		{13},{14},
		//Forgeur Epée
		{145},{20},
		//Forgeur Marteau
		{144},{19},
		//Forgeur Dague
		{142},{18},
		//Forgeur Pelle
		{146},{21},
		//Forgeur Hache
		{65},{143},
		//Forgemage de Hache
		{115},
		//Forgemage de dagues
		{1},
		//Forgemage de marteau
		{116},
		//Forgemage d'épée
		{113},
		//Forgemage Pelle
		{117},
		//SculpteMage baton
		{120},
		//Sculptemage de baguette
		{119},
		//Sculptemage d'arc
		{118},
		//Costumage
		{165},{166},{167},
		//Cordomage
		{163},{164},
		//Joyaumage
		{169},{168},
		//Bricoleur
		{171},{182}
	};
	
	//Buff déclenché en cas de frappe
	public static final int[] ON_HIT_BUFFS		=	{9,79,107,788};
	
	//Effects
	public static final int STATS_ADD_PM2			= 	78;
	
	public static final int STATS_REM_PA			= 	101;
	public static final int STATS_ADD_VIE			= 	110;
	public static final int STATS_ADD_PA			= 	111;
	public static final int STATS_ADD_DOMA			=	112;
	
	public static final int STATS_MULTIPLY_DOMMAGE	=	114;
	public static final int STATS_ADD_CC			=	115;
	public static final int STATS_REM_PO			= 	116;
	public static final int STATS_ADD_PO			= 	117;
	public static final int STATS_ADD_FORC			= 	118;
	public static final int STATS_ADD_AGIL			= 	119;
	public static final int STATS_ADD_PA2			=	120;
	public static final int STATS_ADD_EC			=	122;
	public static final int STATS_ADD_CHAN			= 	123;
	public static final int STATS_ADD_SAGE			= 	124;
	public static final int STATS_ADD_VITA			= 	125;
	public static final int STATS_ADD_INTE			= 	126;
	public static final int STATS_REM_PM			= 	127;
	public static final int STATS_ADD_PM			= 	128;
	
	public static final int STATS_ADD_PERDOM		=	138;
	
	public static final int STATS_ADD_PDOM			=	142;
	
	public static final int STATS_REM_DOMA			= 	145;

	public static final int STATS_REM_CHAN			= 	152;
	public static final int STATS_REM_VITA			= 	153;
	public static final int STATS_REM_AGIL			= 	154;
	public static final int STATS_REM_INTE			= 	155;
	public static final int STATS_REM_SAGE			= 	156;
	public static final int STATS_REM_FORC			= 	157;
	public static final int STATS_ADD_PODS			= 	158;
	public static final int STATS_REM_PODS			= 	159;
	public static final int STATS_ADD_AFLEE			=	160;
	public static final int STATS_ADD_MFLEE			=	161;
	public static final int STATS_REM_AFLEE			=	162;
	public static final int STATS_REM_MFLEE			=	163;
	
	public static final int STATS_ADD_MAITRISE		=	165;
	
	public static final int STATS_REM_PA2			=	168;
	public static final int STATS_REM_PM2			=	169;
	
	public static final int STATS_REM_CC			=	171;
	
	public static final int STATS_ADD_INIT			= 	174;
	public static final int STATS_REM_INIT			= 	175;
	public static final int STATS_ADD_PROS			= 	176;
	public static final int STATS_REM_PROS			= 	177;
	public static final int STATS_ADD_SOIN			= 	178;
	public static final int STATS_REM_SOIN			= 	179;
	
	public static final int STATS_CREATURE			= 	182;
	
	public static final int STATS_ADD_RP_TER		=	210;
	public static final int STATS_ADD_RP_EAU 		=	211;
	public static final int STATS_ADD_RP_AIR		=	212;
	public static final int STATS_ADD_RP_FEU 		=	213;
	public static final int STATS_ADD_RP_NEU		= 	214;
	public static final int STATS_REM_RP_TER		=	215;
	public static final int STATS_REM_RP_EAU 		=	216;
	public static final int STATS_REM_RP_AIR		=	217;
	public static final int STATS_REM_RP_FEU 		=	218;
	public static final int STATS_REM_RP_NEU		= 	219;
	public static final int STATS_RETDOM			=	220;
	
	public static final int STATS_TRAPDOM			=	225;
	public static final int STATS_TRAPPER			=	226;
	
	public static final int STATS_ADD_R_FEU 		= 	240;
	public static final int STATS_ADD_R_NEU			=	241;
	public static final int STATS_ADD_R_TER			=	242;
	public static final int STATS_ADD_R_EAU			=	243;
	public static final int STATS_ADD_R_AIR			=	244;
	public static final int STATS_REM_R_FEU 		= 	245;
	public static final int STATS_REM_R_NEU			=	246;
	public static final int STATS_REM_R_TER			=	247;
	public static final int STATS_REM_R_EAU			=	248;
	public static final int STATS_REM_R_AIR			=	249;
	public static final int STATS_ADD_RP_PVP_TER	=	250;
	public static final int STATS_ADD_RP_PVP_EAU	=	251;
	public static final int STATS_ADD_RP_PVP_AIR	=	252;
	public static final int STATS_ADD_RP_PVP_FEU	=	253;
	public static final int STATS_ADD_RP_PVP_NEU	=	254;
	public static final int STATS_REM_RP_PVP_TER	=	255;
	public static final int STATS_REM_RP_PVP_EAU	=	256;
	public static final int STATS_REM_RP_PVP_AIR	=	257;
	public static final int STATS_REM_RP_PVP_FEU	=	258;
	public static final int STATS_REM_RP_PVP_NEU	=	259;
	public static final int STATS_ADD_R_PVP_TER		=	260;
	public static final int STATS_ADD_R_PVP_EAU		=	261;
	public static final int STATS_ADD_R_PVP_AIR		=	262;
	public static final int STATS_ADD_R_PVP_FEU		=	263;
	public static final int STATS_ADD_R_PVP_NEU		=	264;
	//Effets ID & Buffs
	public static final int EFFECT_PASS_TURN		= 	140;
	//Capture
	public static final int CAPTURE_MONSTRE			=	623;
	
	
	//Methodes
	public static short getStartMap(int classID)
	{
		short pos = 10298;
		switch(classID)
		{
			
		}
		if(Ancestra.CONFIG_CUSTOM_STARTMAP)
		{
			return Ancestra.CONFIG_START_MAP;
		}
		return pos;
	}

	public static TreeMap<Integer, Character> getStartSortsPlaces(int classID)
	{
		TreeMap<Integer,Character> start = new TreeMap<Integer,Character>();
		switch(classID)
		{
			case CLASS_FECA:
				start.put(3,'b');//Attaque Naturelle
				start.put(6,'c');//Armure Terrestre
				start.put(17,'d');//Glyphe Agressif
			break;
			case CLASS_SRAM:
				start.put(61,'b');//Sournoiserie
				start.put(72,'c');//Invisibilité
				start.put(65,'d');//Piege sournois
			break;
			case CLASS_ENIRIPSA:
				start.put(125,'b');//Mot Interdit
				start.put(128,'c');//Mot de Frayeur
				start.put(121,'d');//Mot Curatif
			break;
			case CLASS_ECAFLIP:
				start.put(102,'b');//Pile ou Face
				start.put(103,'c');//Chance d'ecaflip
				start.put(105,'d');//Bond du felin
			break;
			case CLASS_CRA:
				start.put(161,'b');//Fleche Magique
				start.put(169,'c');//Fleche de Recul
				start.put(164,'d');//Fleche Empoisonnée(ex Fleche chercheuse)
			break;
			case CLASS_IOP:
				start.put(143,'b');//Intimidation
				start.put(141,'c');//Pression
				start.put(142,'d');//Bond
			break;
			case CLASS_SADIDA:
				start.put(183,'b');//Ronce
				start.put(200,'c');//Poison Paralysant
				start.put(193,'d');//La bloqueuse
			break;
			case CLASS_OSAMODAS:
				start.put(34,'b');//Invocation de tofu
				start.put(21,'c');//Griffe Spectrale
				start.put(23,'d');//Cri de l'ours
			break;
			case CLASS_XELOR:
				start.put(82,'b');//Contre
				start.put(81,'c');//Ralentissement
				start.put(83,'d');//Aiguille
			break;
			case CLASS_PANDAWA:
				start.put(686,'b');//Picole
				start.put(692,'c');//Gueule de bois
				start.put(687,'d');//Poing enflammé
			break;
			case CLASS_ENUTROF:
				start.put(51,'b');//Lancer de Piece
				start.put(43,'c');//Lancer de Pelle
				start.put(41,'d');//Sac animé
			break;
			case CLASS_SACRIEUR:
				start.put(432,'b');//Pied du Sacrieur
				start.put(431,'c');//Chatiment Osé
				start.put(434,'d');//Attirance
			break;
		}
		return start;
	}
	
	public static TreeMap<Integer,SortStats> getStartSorts(int classID)
	{
		TreeMap<Integer,SortStats> start = new TreeMap<Integer,SortStats>();
		switch(classID)
		{
			case CLASS_FECA:
				start.put(3,World.getSort(3).getStatsByLevel(1));//Attaque Naturelle
				start.put(6,World.getSort(6).getStatsByLevel(1));//Armure Terrestre
				start.put(17,World.getSort(17).getStatsByLevel(1));//Glyphe Agressif
			break;
			case CLASS_SRAM:
				start.put(61,World.getSort(61).getStatsByLevel(1));//Sournoiserie
				start.put(72,World.getSort(72).getStatsByLevel(1));//Invisibilité
				start.put(65,World.getSort(65).getStatsByLevel(1));//Piege sournois
			break;
			case CLASS_ENIRIPSA:
				start.put(125,World.getSort(125).getStatsByLevel(1));//Mot Interdit
				start.put(128,World.getSort(128).getStatsByLevel(1));//Mot de Frayeur
				start.put(121,World.getSort(121).getStatsByLevel(1));//Mot Curatif
			break;
			case CLASS_ECAFLIP:
				start.put(102,World.getSort(102).getStatsByLevel(1));//Pile ou Face
				start.put(103,World.getSort(103).getStatsByLevel(1));//Chance d'ecaflip
				start.put(105,World.getSort(105).getStatsByLevel(1));//Bond du felin
			break;
			case CLASS_CRA:
				start.put(161,World.getSort(161).getStatsByLevel(1));//Fleche Magique
				start.put(169,World.getSort(169).getStatsByLevel(1));//Fleche de Recul
				start.put(164,World.getSort(164).getStatsByLevel(1));//Fleche Empoisonnée(ex Fleche chercheuse)
			break;
			case CLASS_IOP:
				start.put(143,World.getSort(143).getStatsByLevel(1));//Intimidation
				start.put(141,World.getSort(141).getStatsByLevel(1));//Pression
				start.put(142,World.getSort(142).getStatsByLevel(1));//Bond
			break;
			case CLASS_SADIDA:
				start.put(183,World.getSort(183).getStatsByLevel(1));//Ronce
				start.put(200,World.getSort(200).getStatsByLevel(1));//Poison Paralysant
				start.put(193,World.getSort(193).getStatsByLevel(1));//La bloqueuse
			break;
			case CLASS_OSAMODAS:
				start.put(34,World.getSort(34).getStatsByLevel(1));//Invocation de tofu
				start.put(21,World.getSort(21).getStatsByLevel(1));//Griffe Spectrale
				start.put(23,World.getSort(23).getStatsByLevel(1));//Cri de l'ours
			break;
			case CLASS_XELOR:
				start.put(82,World.getSort(82).getStatsByLevel(1));//Contre
				start.put(81,World.getSort(81).getStatsByLevel(1));//Ralentissement
				start.put(83,World.getSort(83).getStatsByLevel(1));//Aiguille
			break;
			case CLASS_PANDAWA:
				start.put(686,World.getSort(686).getStatsByLevel(1));//Picole
				start.put(692,World.getSort(692).getStatsByLevel(1));//Gueule de bois
				start.put(687,World.getSort(687).getStatsByLevel(1));//Poing enflammé
			break;
			case CLASS_ENUTROF:
				start.put(51,World.getSort(51).getStatsByLevel(1));//Lancer de Piece
				start.put(43,World.getSort(43).getStatsByLevel(1));//Lancer de Pelle
				start.put(41,World.getSort(41).getStatsByLevel(1));//Sac animé
			break;
			case CLASS_SACRIEUR:
				start.put(432,World.getSort(432).getStatsByLevel(1));//Pied du Sacrieur
				start.put(431,World.getSort(431).getStatsByLevel(1));//Chatiment Forcé
				start.put(434,World.getSort(434).getStatsByLevel(1));//Attirance
			break;
		}
		return start;
	}

	public static int getStartCell(int classID)
	{
		int pos = 314;
		switch(classID)
		{
			
		}
		if(Ancestra.CONFIG_CUSTOM_STARTMAP)
		{
			return Ancestra.CONFIG_START_CELL;
		}
		return pos;
	}
	
	public static int getBasePdv(int classID)
	{
		return 50;
	}

	public static int getReqPtsToBoostStatsByClass(int classID,int statID,int val)
	{
		switch(statID)
		{
			case 11://Vita
				return 1;
			case 12://Sage
				return 3;
			case 10://Force
				switch(classID)
				{
					case CLASS_SACRIEUR:
						return 3;
						
					case CLASS_FECA:
						if(val < 50)
							return 2;
						if(val < 150)
							return 3;
						if(val < 250)
							return 4;
						return 5;
						
					case CLASS_XELOR:
						if(val < 50)
							return 2;
						if(val < 150)
							return 3;
						if(val < 250)
							return 4;
						return 5;
						
					case CLASS_SRAM:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_OSAMODAS:
						if(val < 50)
							return 2;
						if(val < 150)
							return 3;
						if(val < 250)
							return 4;
						return 5;
					
					case CLASS_ENIRIPSA:
						if(val < 50)
							return 2;
						if(val < 150)
							return 3;
						if(val < 250)
							return 4;
						return 5;
					
					case CLASS_PANDAWA:
						if(val < 50)
							return 1;
						if(val < 200)
							return 2;
						return 3;
						
					case CLASS_SADIDA:
						if(val < 50)
							return 1;
						if(val < 250)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_CRA:
						if(val < 50)
							return 1;
						if(val < 150)
							return 2;
						if(val < 250)
							return 3;
						if(val < 350)
							return 4;
						return 5;
						
					case CLASS_ENUTROF:
						if(val < 50)
							return 1;
						if(val < 150)
							return 2;
						if(val < 250)
							return 3;
						if(val < 350)
							return 4;
						return 5;	
						
					case CLASS_ECAFLIP:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_IOP:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
				}
			break;
			case 13://Chance
				switch(classID)
				{
					case CLASS_FECA:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
						
					case CLASS_XELOR:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
						
					case CLASS_SACRIEUR:
						return 3;
						
					case CLASS_SRAM:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
						
					case CLASS_SADIDA:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_PANDAWA:
						if(val < 50)
							return 1;
						if(val < 200)
							return 2;
						return 3;
						
					case CLASS_IOP:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
					
					case CLASS_ENUTROF:
						if(val < 100)
							return 1;
						if(val < 150)
							return 2;
						if(val < 230)
							return 3;
						if(val < 330)
							return 4;
						return 5;
						
					case CLASS_OSAMODAS:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_ECAFLIP:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
						
					case CLASS_ENIRIPSA:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
						
					case CLASS_CRA:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
				}
			break;
			case 14://Agilité
				switch(classID)
				{
					case CLASS_FECA:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
					
					case CLASS_XELOR:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
						
					case CLASS_SACRIEUR:
						return 3;
						
					case CLASS_SRAM:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_SADIDA:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
						
					case CLASS_PANDAWA:
						if(val < 50)
							return 1;
						if(val < 200)
							return 2;
						return 3;
						
					case CLASS_ENIRIPSA:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
					
					case CLASS_IOP:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
					
					case CLASS_ENUTROF:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;	
					
					case CLASS_ECAFLIP:
						if(val < 50)
							return 1;
						if(val < 100)
							return 2;
						if(val < 150)
							return 3;
						if(val < 200)
							return 4;
						return 5;
						
					case CLASS_CRA:
						if(val < 50)
							return 1;
						if(val < 100)
							return 2;
						if(val < 150)
							return 3;
						if(val < 200)
							return 4;
						return 5;
					
					case CLASS_OSAMODAS:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
				}
			break;
			case 15://Intelligence
				switch(classID)
				{
					case CLASS_XELOR:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_FECA:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_SACRIEUR:
						return 3;
					
					case CLASS_SRAM:
						if(val < 50)
							return 2;
						if(val < 150)
							return 3;
						if(val < 250)
							return 4;
						return 5;
						
					case CLASS_SADIDA:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_ENUTROF:
						if(val < 20)
							return 1;
						if(val < 60)
							return 2;
						if(val < 100)
							return 3;
						if(val < 140)
							return 4;
						return 5;	
					
					case CLASS_PANDAWA:
						if(val < 50)
							return 1;
						if(val < 200)
							return 2;
						return 3;
						
					case CLASS_IOP:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
					
					case CLASS_ENIRIPSA:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_CRA:
						if(val < 50)
							return 1;
						if(val < 150)
							return 2;
						if(val < 250)
							return 3;
						if(val < 350)
							return 4;
						return 5;
						
					case CLASS_OSAMODAS:
						if(val < 100)
							return 1;
						if(val < 200)
							return 2;
						if(val < 300)
							return 3;
						if(val < 400)
							return 4;
						return 5;
						
					case CLASS_ECAFLIP:
						if(val < 20)
							return 1;
						if(val < 40)
							return 2;
						if(val < 60)
							return 3;
						if(val < 80)
							return 4;
						return 5;
				}
			break;
		}
		return 5;
	}

	public static int getAggroByLevel(int lvl)
	{
		int aggro = 0;
		aggro = (int)(lvl/50);
		if(lvl>500)
			aggro = 3;
		return aggro;
	}

	public static boolean isValidPlaceForItem(ObjTemplate template, int place)
	{
		switch(template.getType())
		{
			case ITEM_TYPE_AMULETTE:
				if(place == ITEM_POS_AMULETTE)return true;
			break;
			
			case ITEM_TYPE_ARC:
			case ITEM_TYPE_BAGUETTE:
			case ITEM_TYPE_BATON:
			case ITEM_TYPE_DAGUES:
			case ITEM_TYPE_EPEE:
			case ITEM_TYPE_MARTEAU:
			case ITEM_TYPE_PELLE:
			case ITEM_TYPE_HACHE:
			case ITEM_TYPE_OUTIL:
			case ITEM_TYPE_PIOCHE:
			case ITEM_TYPE_FAUX:
			case ITEM_TYPE_PIERRE_AME:
				if(place == ITEM_POS_ARME)return true;
			break;
			
			case ITEM_TYPE_ANNEAU:
				if(place == ITEM_POS_ANNEAU1 || place == ITEM_POS_ANNEAU2)return true;
			break;
			
			case ITEM_TYPE_CEINTURE:
				if(place == ITEM_POS_CEINTURE)return true;
			break;
			
			case ITEM_TYPE_BOTTES:
				if(place == ITEM_POS_BOTTES)return true;
			break;
			
			case ITEM_TYPE_COIFFE:
				if(place == ITEM_POS_COIFFE)return true;
			break;
			
			case ITEM_TYPE_CAPE:
			case ITEM_TYPE_SAC_DOS:
				if(place == ITEM_POS_CAPE)return true;
			break;
			
			case ITEM_TYPE_FAMILIER:
				if(place == ITEM_POS_FAMILIER)return true;
			break;
			
			case ITEM_TYPE_DOFUS:
				if(place == ITEM_POS_DOFUS1 
				|| place == ITEM_POS_DOFUS2
				|| place == ITEM_POS_DOFUS3
				|| place == ITEM_POS_DOFUS4
				|| place == ITEM_POS_DOFUS5
				|| place == ITEM_POS_DOFUS6
				)return true;
			break;
			
			case ITEM_TYPE_BOUCLIER:
				if(place == ITEM_POS_BOUCLIER)return true;
			break;
			
			//Barre d'objets TODO : Normalement le client bloque les items interdits
			case ITEM_TYPE_POTION:
			case ITEM_TYPE_PARCHO_EXP:
			case ITEM_TYPE_BOOST_FOOD:
			case ITEM_TYPE_PAIN:
			case ITEM_TYPE_BIERE:
			case ITEM_TYPE_POISSON:
			case ITEM_TYPE_BONBON:
			case ITEM_TYPE_COMESTI_POISSON:
			case ITEM_TYPE_VIANDE:
			case ITEM_TYPE_VIANDE_CONSERVEE:
			case ITEM_TYPE_VIANDE_COMESTIBLE:
			case ITEM_TYPE_TEINTURE:
			case ITEM_TYPE_MAITRISE:
			case ITEM_TYPE_BOISSON:
			case ITEM_TYPE_PIERRE_AME_PLEINE:
			case ITEM_TYPE_PARCHO_RECHERCHE:
			case ITEM_TYPE_CADEAUX:
			case ITEM_TYPE_OBJET_ELEVAGE:
			case ITEM_TYPE_OBJET_UTILISABLE:
			case ITEM_TYPE_PRISME:
			case ITEM_TYPE_FEE_ARTIFICE:
			case ITEM_TYPE_DONS:
				if(place >= 35 && place <= 48)return true;
			break;
				
			
		}
		return false;
	}
	
	public static void onLevelUpSpells(Personnage perso,int lvl)
	{
		switch(perso.get_classe())
		{
			case CLASS_FECA:
				if(lvl == 3)
					perso.learnSpell(4, 1,true,false);//Renvoie de sort
				if(lvl == 6)
					perso.learnSpell(2, 1,true,false);//Aveuglement
				if(lvl == 9)
					perso.learnSpell(1, 1,true,false);//Armure Incandescente
				if(lvl == 13)
					perso.learnSpell(9, 1,true,false);//Attaque nuageuse
				if(lvl == 17)
					perso.learnSpell(18, 1,true,false);//Armure Aqueuse
				if(lvl == 21)
					perso.learnSpell(20, 1,true,false);//Immunité
				if(lvl == 26)
					perso.learnSpell(14, 1,true,false);//Armure Venteuse
				if(lvl == 31)
					perso.learnSpell(19, 1,true,false);//Bulle
				if(lvl == 36)
					perso.learnSpell(5, 1,true,false);//Trêve
				if(lvl == 42)
					perso.learnSpell(16, 1,true,false);//Science du bâton
				if(lvl == 48)
					perso.learnSpell(8, 1,true,false);//Retour du bâton
				if(lvl == 54)
					perso.learnSpell(12, 1,true,false);//glyphe d'Aveuglement
				if(lvl == 60)
					perso.learnSpell(11, 1,true,false);//Téléportation
				if(lvl == 70)
					perso.learnSpell(10, 1,true,false);//Glyphe Enflammé
				if(lvl == 80)
					perso.learnSpell(7, 1,true,false);//Bouclier Féca
				if(lvl == 90)
					perso.learnSpell(15, 1,true,false);//Glyphe d'Immobilisation
				if(lvl == 100)
					perso.learnSpell(13, 1,true,false);//Glyphe de Silence
				if(lvl == 200)
					perso.learnSpell(1901, 1,true,false);//Invocation de Dopeul Féca
			break;
			
			case CLASS_OSAMODAS:
				if(lvl == 3)
					perso.learnSpell(26, 1,true,false);//Bénédiction Animale
				if(lvl == 6)
					perso.learnSpell(22, 1,true,false);//Déplacement Félin
				if(lvl == 9)
					perso.learnSpell(35, 1,true,false);//Invocation de Bouftou
				if(lvl == 13)
					perso.learnSpell(28, 1,true,false);//Crapaud
				if(lvl == 17)
					perso.learnSpell(37, 1,true,false);//Invocation de Prespic
				if(lvl == 21)
					perso.learnSpell(30, 1,true,false);//Fouet
				if(lvl == 26)
					perso.learnSpell(27, 1,true,false);//Piqûre Motivante
				if(lvl == 31)
					perso.learnSpell(24, 1,true,false);//Corbeau
				if(lvl == 36)
					perso.learnSpell(33, 1,true,false);//Griffe Cinglante
				if(lvl == 42)
					perso.learnSpell(25, 1,true,false);//Soin Animal
				if(lvl == 48)
					perso.learnSpell(38, 1,true,false);//Invocation de Sanglier
				if(lvl == 54)
					perso.learnSpell(36, 1,true,false);//Frappe du Craqueleur
				if(lvl == 60)
					perso.learnSpell(32, 1,true,false);//Résistance Naturelle
				if(lvl == 70)
					perso.learnSpell(29, 1,true,false);//Crocs du Mulou
				if(lvl == 80)
					perso.learnSpell(39, 1,true,false);//Invocation de Bwork Mage
				if(lvl == 90)
					perso.learnSpell(40, 1,true,false);//Invocation de Craqueleur
				if(lvl == 100)
					perso.learnSpell(31, 1,true,false);//Invocation de Dragonnet Rouge
				if(lvl == 200)
					perso.learnSpell(1902, 1,true,false);//Invocation de Dopeul Osamodas
			break;

			case CLASS_ENUTROF:
				if(lvl == 3)
					perso.learnSpell(49, 1,true,false);//Pelle Fantomatique
				if(lvl == 6)
					perso.learnSpell(42, 1,true,false);//Chance
				if(lvl == 9)
					perso.learnSpell(47, 1,true,false);//Boîte de Pandore
				if(lvl == 13)
					perso.learnSpell(48, 1,true,false);//Remblai
				if(lvl == 17)
					perso.learnSpell(45, 1,true,false);//Clé Réductrice
				if(lvl == 21)
					perso.learnSpell(53, 1,true,false);//Force de l'Age
				if(lvl == 26)
					perso.learnSpell(46, 1,true,false);//Désinvocation
				if(lvl == 31)
					perso.learnSpell(52, 1,true,false);//Cupidité
				if(lvl == 36)
					perso.learnSpell(44, 1,true,false);//Roulage de Pelle
				if(lvl == 42)
					perso.learnSpell(50, 1,true,false);//Maladresse
				if(lvl == 48)
					perso.learnSpell(54, 1,true,false);//Maladresse de Masse
				if(lvl == 54)
					perso.learnSpell(55, 1,true,false);//Accélération
				if(lvl == 60)
					perso.learnSpell(56, 1,true,false);//Pelle du Jugement
				if(lvl == 70)
					perso.learnSpell(58, 1,true,false);//Pelle Massacrante
				if(lvl == 80)
					perso.learnSpell(59, 1,true,false);//Corruption
				if(lvl == 90)
					perso.learnSpell(57, 1,true,false);//Pelle Animée
				if(lvl == 100)
					perso.learnSpell(60, 1,true,false);//Coffre Animé
				if(lvl == 200)
					perso.learnSpell(1903, 1,true,false);//Invocation de Dopeul Enutrof
			break;

			case CLASS_SRAM:
				if(lvl == 3)
					perso.learnSpell(66, 1,true,false);//Poison insidieux
				if(lvl == 6)
					perso.learnSpell(68, 1,true,false);//Fourvoiement
				if(lvl == 9)
					perso.learnSpell(63, 1,true,false);//Coup Sournois
				if(lvl == 13)
					perso.learnSpell(74, 1,true,false);//Double
				if(lvl == 17)
					perso.learnSpell(64, 1,true,false);//Repérage
				if(lvl == 21)
					perso.learnSpell(79, 1,true,false);//Piège de Masse
				if(lvl == 26)
					perso.learnSpell(78, 1,true,false);//Invisibilité d'Autrui
				if(lvl == 31)
					perso.learnSpell(71, 1,true,false);//Piège Empoisonné
				if(lvl == 36)
					perso.learnSpell(62, 1,true,false);//Concentration de Chakra
				if(lvl == 42)
					perso.learnSpell(69, 1,true,false);//Piège d'Immobilisation
				if(lvl == 48)
					perso.learnSpell(77, 1,true,false);//Piège de Silence
				if(lvl == 54)
					perso.learnSpell(73, 1,true,false);//Piège répulsif
				if(lvl == 60)
					perso.learnSpell(67, 1,true,false);//Peur
				if(lvl == 70)
					perso.learnSpell(70, 1,true,false);//Arnaque
				if(lvl == 80)
					perso.learnSpell(75, 1,true,false);//Pulsion de Chakra
				if(lvl == 90)
					perso.learnSpell(76, 1,true,false);//Attaque Mortelle
				if(lvl == 100)
					perso.learnSpell(80, 1,true,false);//Piège Mortel
				if(lvl == 200)
					perso.learnSpell(1904, 1,true,false);//Invocation de Dopeul Sram
			break;

			case CLASS_XELOR:
				if(lvl == 3)
					perso.learnSpell(84, 1,true,false);//Gelure
				if(lvl == 6)
					perso.learnSpell(100, 1,true,false);//Sablier de Xélor
				if(lvl == 9)
					perso.learnSpell(92, 1,true,false);//Rayon Obscur
				if(lvl == 13)
					perso.learnSpell(88, 1,true,false);//Téléportation
				if(lvl == 17)
					perso.learnSpell(93, 1,true,false);//Flétrissement
				if(lvl == 21)
					perso.learnSpell(85, 1,true,false);//Flou
				if(lvl == 26)
					perso.learnSpell(96, 1,true,false);//Poussière Temporelle
				if(lvl == 31)
					perso.learnSpell(98, 1,true,false);//Vol du Temps
				if(lvl == 36)
					perso.learnSpell(86, 1,true,false);//Aiguille Chercheuse
				if(lvl == 42)
					perso.learnSpell(89, 1,true,false);//Dévouement
				if(lvl == 48)
					perso.learnSpell(90, 1,true,false);//Fuite
				if(lvl == 54)
					perso.learnSpell(87, 1,true,false);//Démotivation
				if(lvl == 60)
					perso.learnSpell(94, 1,true,false);//Protection Aveuglante
				if(lvl == 70)
					perso.learnSpell(99, 1,true,false);//Momification
				if(lvl == 80)
					perso.learnSpell(95, 1,true,false);//Horloge
				if(lvl == 90)
					perso.learnSpell(91, 1,true,false);//Frappe de Xélor
				if(lvl == 100)
					perso.learnSpell(97, 1,true,false);//Cadran de Xélor
				if(lvl == 200)
					perso.learnSpell(1905, 1,true,false);//Invocation de Dopeul Xélor
			break;

			case CLASS_ECAFLIP:
				if(lvl == 3)
					perso.learnSpell(109, 1,true,false);//Bluff
				if(lvl == 6)
					perso.learnSpell(113, 1,true,false);//Perception
				if(lvl == 9)
					perso.learnSpell(111, 1,true,false);//Contrecoup
				if(lvl == 13)
					perso.learnSpell(104, 1,true,false);//Trèfle
				if(lvl == 17)
					perso.learnSpell(119, 1,true,false);//Tout ou rien
				if(lvl == 21)
					perso.learnSpell(101, 1,true,false);//Roulette
				if(lvl == 26)
					perso.learnSpell(107, 1,true,false);//Topkaj
				if(lvl == 31)
					perso.learnSpell(116, 1,true,false);//Langue Râpeuse
				if(lvl == 36)
					perso.learnSpell(106, 1,true,false);//Roue de la Fortune
				if(lvl == 42)
					perso.learnSpell(117, 1,true,false);//Griffe Invocatrice
				if(lvl == 48)
					perso.learnSpell(108, 1,true,false);//Esprit Félin
				if(lvl == 54)
					perso.learnSpell(115, 1,true,false);//Odorat
				if(lvl == 60)
					perso.learnSpell(118, 1,true,false);//Réflexes
				if(lvl == 70)
					perso.learnSpell(110, 1,true,false);//Griffe Joueuse
				if(lvl == 80)
					perso.learnSpell(112, 1,true,false);//Griffe de Ceangal
				if(lvl == 90)
					perso.learnSpell(114, 1,true,false);//Rekop
				if(lvl == 100)
					perso.learnSpell(120, 1,true,false);//Destin d'Ecaflip
				if(lvl == 200)
					perso.learnSpell(1906, 1,true,false);//Invocation de Dopeul Ecaflip
			break;

			case CLASS_ENIRIPSA:
				if(lvl == 3)
					perso.learnSpell(124, 1,true,false);//Mot Soignant
				if(lvl == 6)
					perso.learnSpell(122, 1,true,false);//Mot Blessant
				if(lvl == 9)
					perso.learnSpell(126, 1,true,false);//Mot Stimulant
				if(lvl == 13)
					perso.learnSpell(127, 1,true,false);//Mot de Prévention
				if(lvl == 17)
					perso.learnSpell(123, 1,true,false);//Mot Drainant
				if(lvl == 21)
					perso.learnSpell(130, 1,true,false);//Mot Revitalisant
				if(lvl == 26)
					perso.learnSpell(131, 1,true,false);//Mot de Régénération
				if(lvl == 31)
					perso.learnSpell(132, 1,true,false);//Mot d'Epine
				if(lvl == 36)
					perso.learnSpell(133, 1,true,false);//Mot de Jouvence
				if(lvl == 42)
					perso.learnSpell(134, 1,true,false);//Mot Vampirique
				if(lvl == 48)
					perso.learnSpell(135, 1,true,false);//Mot de Sacrifice
				if(lvl == 54)
					perso.learnSpell(129, 1,true,false);//Mot d'Amitié
				if(lvl == 60)
					perso.learnSpell(136, 1,true,false);//Mot d'Immobilisation
				if(lvl == 70)
					perso.learnSpell(137, 1,true,false);//Mot d'Envol
				if(lvl == 80)
					perso.learnSpell(138, 1,true,false);//Mot de Silence
				if(lvl == 90)
					perso.learnSpell(139, 1,true,false);//Mot d'Altruisme
				if(lvl == 100)
					perso.learnSpell(140, 1,true,false);//Mot de Reconstitution
				if(lvl == 200)
					perso.learnSpell(1907, 1,true,false);//Invocation de Dopeul Eniripsa
			break;

			case CLASS_IOP:
				if(lvl == 3)
					perso.learnSpell(144, 1,true,false);//Compulsion
				if(lvl == 6)
					perso.learnSpell(145, 1,true,false);//Epée Divine
				if(lvl == 9)
					perso.learnSpell(146, 1,true,false);//Epée du Destin
				if(lvl == 13)
					perso.learnSpell(147, 1,true,false);//Guide de Bravoure
				if(lvl == 17)
					perso.learnSpell(148, 1,true,false);//Amplification
				if(lvl == 21)
					perso.learnSpell(154, 1,true,false);//Epée Destructrice
				if(lvl == 26)
					perso.learnSpell(150, 1,true,false);//Couper
				if(lvl == 31)
					perso.learnSpell(151, 1,true,false);//Souffle
				if(lvl == 36)
					perso.learnSpell(155, 1,true,false);//Vitalité
				if(lvl == 42)
					perso.learnSpell(152, 1,true,false);//Epée du Jugement
				if(lvl == 48)
					perso.learnSpell(153, 1,true,false);//Puissance
				if(lvl == 54)
					perso.learnSpell(149, 1,true,false);//Mutilation
				if(lvl == 60)
					perso.learnSpell(156, 1,true,false);//Tempête de Puissance
				if(lvl == 70)
					perso.learnSpell(157, 1,true,false);//Epée Céleste
				if(lvl == 80)
					perso.learnSpell(158, 1,true,false);//Concentration
				if(lvl == 90)
					perso.learnSpell(160, 1,true,false);//Epée de Iop
				if(lvl == 100)
					perso.learnSpell(159, 1,true,false);//Colère de Iop
				if(lvl == 200)
					perso.learnSpell(1908, 1,true,false);//Invocation de Dopeul Iop
			break;

			case CLASS_CRA:
				if(lvl == 3)
					perso.learnSpell(163, 1,true,false);//Flèche Glacée
				if(lvl == 6)
					perso.learnSpell(165, 1,true,false);//Flèche enflammée
				if(lvl == 9)
					perso.learnSpell(172, 1,true,false);//Tir Eloigné
				if(lvl == 13)
					perso.learnSpell(167, 1,true,false);//Flèche d'Expiation
				if(lvl == 17)
					perso.learnSpell(168, 1,true,false);//Oeil de Taupe
				if(lvl == 21)
					perso.learnSpell(162, 1,true,false);//Tir Critique
				if(lvl == 26)
					perso.learnSpell(170, 1,true,false);//Flèche d'Immobilisation
				if(lvl == 31)
					perso.learnSpell(171, 1,true,false);//Flèche Punitive
				if(lvl == 36)
					perso.learnSpell(166, 1,true,false);//Tir Puissant
				if(lvl == 42)
					perso.learnSpell(173, 1,true,false);//Flèche Harcelante
				if(lvl == 48)
					perso.learnSpell(174, 1,true,false);//Flèche Cinglante
				if(lvl == 54)
					perso.learnSpell(176, 1,true,false);//Flèche Persécutrice
				if(lvl == 60)
					perso.learnSpell(175, 1,true,false);//Flèche Destructrice
				if(lvl == 70)
					perso.learnSpell(178, 1,true,false);//Flèche Absorbante
				if(lvl == 80)
					perso.learnSpell(177, 1,true,false);//Flèche Ralentissante
				if(lvl == 90)
					perso.learnSpell(179, 1,true,false);//Flèche Explosive
				if(lvl == 100)
					perso.learnSpell(180, 1,true,false);//Maîtrise de l'Arc
				if(lvl == 200)
					perso.learnSpell(1909, 1,true,false);//Invocation de Dopeul Cra
			break;

			case CLASS_SADIDA:
				if(lvl == 3)
					perso.learnSpell(198, 1,true,false);//Sacrifice Poupesque
				if(lvl == 6)
					perso.learnSpell(195, 1,true,false);//Larme
				if(lvl == 9)
					perso.learnSpell(182, 1,true,false);//Invocation de la Folle
				if(lvl == 13)
					perso.learnSpell(192, 1,true,false);//Ronce Apaisante
				if(lvl == 17)
					perso.learnSpell(197, 1,true,false);//Puissance Sylvestre
				if(lvl == 21)
					perso.learnSpell(189, 1,true,false);//Invocation de la Sacrifiée
				if(lvl == 26)
					perso.learnSpell(181, 1,true,false);//Tremblement
				if(lvl == 31)
					perso.learnSpell(199, 1,true,false);//Connaissance des Poupées
				if(lvl == 36)
					perso.learnSpell(191, 1,true,false);//Ronce Multiples
				if(lvl == 42)
					perso.learnSpell(186, 1,true,false);//Arbre
				if(lvl == 48)
					perso.learnSpell(196, 1,true,false);//Vent Empoisonné
				if(lvl == 54)
					perso.learnSpell(190, 1,true,false);//Invocation de la Gonflable
				if(lvl == 60)
					perso.learnSpell(194, 1,true,false);//Ronces Agressives
				if(lvl == 70)
					perso.learnSpell(185, 1,true,false);//Herbe Folle
				if(lvl == 80)
					perso.learnSpell(184, 1,true,false);//Feu de Brousse
				if(lvl == 90)
					perso.learnSpell(188, 1,true,false);//Ronce Insolente
				if(lvl == 100)
					perso.learnSpell(187, 1,true,false);//Invocation de la Surpuissante
				if(lvl == 200)
					perso.learnSpell(1910, 1,true,false);//Invocation de Dopeul Sadida
			break;

			case CLASS_SACRIEUR:
				if(lvl == 3)
					perso.learnSpell(444, 1,true,false);//Dérobade
				if(lvl == 6)
					perso.learnSpell(449, 1,true,false);//Détour
				if(lvl == 9)
					perso.learnSpell(436, 1,true,false);//Assaut
				if(lvl == 13)
					perso.learnSpell(437, 1,true,false);//Châtiment Agile
				if(lvl == 17)
					perso.learnSpell(439, 1,true,false);//Dissolution
				if(lvl == 21)
					perso.learnSpell(433, 1,true,false);//Châtiment Osé
				if(lvl == 26)
					perso.learnSpell(443, 1,true,false);//Châtiment Spirituel
				if(lvl == 31)
					perso.learnSpell(440, 1,true,false);//Sacrifice
				if(lvl == 36)
					perso.learnSpell(442, 1,true,false);//Absorption
				if(lvl == 42)
					perso.learnSpell(441, 1,true,false);//Châtiment Vilatesque
				if(lvl == 48)
					perso.learnSpell(445, 1,true,false);//Coopération
				if(lvl == 54)
					perso.learnSpell(438, 1,true,false);//Transposition
				if(lvl == 60)
					perso.learnSpell(446, 1,true,false);//Punition
				if(lvl == 70)
					perso.learnSpell(447, 1,true,false);//Furie
				if(lvl == 80)
					perso.learnSpell(448, 1,true,false);//Epée Volante
				if(lvl == 90)
					perso.learnSpell(435, 1,true,false);//Tansfert de Vie
				if(lvl == 100)
					perso.learnSpell(450, 1,true,false);//Folie Sanguinaire
				if(lvl == 200)
					perso.learnSpell(1911, 1,true,false);//Invocation de Dopeul Sacrieur
			break;

			case CLASS_PANDAWA:
				if(lvl == 3)
					perso.learnSpell(689, 1,true,false);//Epouvante
				if(lvl == 6)
					perso.learnSpell(690, 1,true,false);//Souffle Alcoolisé
				if(lvl == 9)
					perso.learnSpell(691, 1,true,false);//Vulnérabilité Aqueuse
				if(lvl == 13)
					perso.learnSpell(688, 1,true,false);//Vulnérabilité Incandescente
				if(lvl == 17)
					perso.learnSpell(693, 1,true,false);//Karcham
				if(lvl == 21)
					perso.learnSpell(694, 1,true,false);//Vulnérabilité Venteuse
				if(lvl == 26)
					perso.learnSpell(695, 1,true,false);//Stabilisation
				if(lvl == 31)
					perso.learnSpell(696, 1,true,false);//Chamrak
				if(lvl == 36)
					perso.learnSpell(697, 1,true,false);//Vulnérabilité Terrestre
				if(lvl == 42)
					perso.learnSpell(698, 1,true,false);//Souillure
				if(lvl == 48)
					perso.learnSpell(699, 1,true,false);//Lait de Bambou
				if(lvl == 54)
					perso.learnSpell(700, 1,true,false);//Vague à Lame
				if(lvl == 60)
					perso.learnSpell(701, 1,true,false);//Colère de Zatoïshwan
				if(lvl == 70)
					perso.learnSpell(702, 1,true,false);//Flasque Explosive
				if(lvl == 80)
					perso.learnSpell(703, 1,true,false);//Pandatak
				if(lvl == 90)
					perso.learnSpell(704, 1,true,false);//Pandanlku
				if(lvl == 100)
					perso.learnSpell(705, 1,true,false);//Lien Spiritueux
				if(lvl == 200)
					perso.learnSpell(1912, 1,true,false);//Invocation de Dopeul Pandawa
			break;
		}
	}

	public static int getGlyphColor(int spell)
	{
		switch(spell)
		{
			case 10://Enflammé
			case 2033://Dopeul
				return 4;//Rouge
			case 12://Aveuglement
			case 2034://Dopeul
				return 3;
			case 13://Silence
			case 2035://Dopeul
				return 6;//Bleu
			case 15://Immobilisation
			case 2036://Dopeul
				return 5;//Vert
			case 17://Aggressif
			case 2037://Dopeul
				return 2;
			//case 476://Blop
			default:
				return 4;
		}
	}

	public static int getTrapsColor(int spell)
	{
		switch(spell)
		{
			case 65://Sournois
				return 7;
			case 69://Immobilisation
				return 10;
			case 71://Empoisonnée
			case 2068://Dopeul
				return 9;
			case 73://Repulsif
				return 12;
			case 77://Silence
			case 2071://Dopeul
				return 11;
			case 79://Masse
			case 2072://Dopeul
				return 8;
			case 80://Mortel
				return 13;
			default:
				return 7;
		}
	}

	public static int getTotalCaseByJobLevel(int lvl)
	{
		if(lvl <10)return 2;
		if(lvl == 100)return 9;
		return (int)(lvl/20)+3;
	}
	
	public static int getChanceForMaxCase(int lvl)
	{
		if(lvl <10)return 50;
		return  54 + (int)((lvl/10)-1)*5;
	}
	
	public static int calculXpWinCraft(int lvl,int numCase)
	{
		if(lvl == 100)return 0;
		switch(numCase)
		{
			case 1:
				if(lvl<10)return 1;
			return 0;
			case 2:
				if(lvl<60)return 10;
			return 0;
			case 3:
				if(lvl>9 && lvl<80)return 25;
			return 0;
			case 4:
				if(lvl > 19)return 50;
			return 0;
			case 5:
				if(lvl > 39)return 100;
			return 0;
			case 6:
				if(lvl > 59)return 250;
			return 0;
			case 7:
				if(lvl > 79)return 500;
			return 0;
			case 8:
				if(lvl > 99)return 1000;
			return 0;
		}
		return 0;
	}
	
	public static ArrayList<JobAction> getPosActionsToJob(int tID, int lvl)
	{
		ArrayList<JobAction> list = new ArrayList<JobAction>();
		int timeWin = lvl*100;
		int dropWin = lvl / 5;
		switch(tID)
		{
			case JOB_BIJOUTIER:
			//Faire Anneau 
			list.add(new JobAction(11,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Faire Amullette
			list.add(new JobAction(12,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_TAILLEUR:
			//Faire Sac
			list.add(new JobAction(64,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Faire Cape
			list.add(new JobAction(123,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Faire Chapeau
			list.add(new JobAction(63,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_F_BOUCLIER:
			//Forger Bouclier
			list.add(new JobAction(156,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_BRICOLEUR:
			//Faire clef
			list.add(new JobAction(171,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Faire objet brico
			list.add(new JobAction(182,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_CORDONIER:
			//Faire botte
			list.add(new JobAction(13,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Faire ceinture
			list.add(new JobAction(14,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_S_ARC:
			//Sculter Arc
			list.add(new JobAction(17,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//ReSculter Arc
			list.add(new JobAction(16,3,0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_S_BATON:
			//Sculter Baton
			list.add(new JobAction(147,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//ReSculter Baton
			list.add(new JobAction(148,3,0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_S_BAGUETTE:
			//Sculter Baguette
			list.add(new JobAction(149,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//ReSculter Baguette
			list.add(new JobAction(15,3,0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_CORDOMAGE:
				//FM Bottes
				list.add(new JobAction(163,3,0,true,lvl,0));
				//FM Ceinture
				list.add(new JobAction(164,3,0,true,lvl,0));
			break;

			case JOB_JOAILLOMAGE:
				//FM Anneau
				list.add(new JobAction(169,3,0,true,lvl,0));
				//FM  Amullette
				list.add(new JobAction(168,3,0,true,lvl,0));
			break;

			case JOB_COSTUMAGE:
				//FM Chapeau
				list.add(new JobAction(165,3,0,true,lvl,0));
				//FM Cape
				list.add(new JobAction(167,3,0,true,lvl,0));
				//FM Sac
				list.add(new JobAction(166,3,0,true,lvl,0));
			break;

			case JOB_F_EPEE:
			//Forger Epée
			list.add(new JobAction(20,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Reforger Epée
			list.add(new JobAction(146,3,0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_F_DAGUE:
			//Forger Dague
			list.add(new JobAction(142,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Reforger Dague
			list.add(new JobAction(18,3,0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_F_MARTEAU:
			//Forger Marteau
			list.add(new JobAction(19,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Reforger Marteau
			list.add(new JobAction(144,3,0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_F_PELLE:
			//Forger Pelle
			list.add(new JobAction(21,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Reforger Pelle
			list.add(new JobAction(146,3,0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_F_HACHES:
			//Forger Hache 
			list.add(new JobAction(65,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Reforger Hache
			list.add(new JobAction(143,3,0,true,getChanceForMaxCase(lvl),-1));
			break;

			case JOB_FM_HACHES:
				//Reforger une hache
				list.add(new JobAction(115,3,0,true,lvl,0));
			break;
			case JOB_FM_DAGUE:
				//Reforger une dague
				list.add(new JobAction(1,3,0,true,lvl,0));
			break;
			case JOB_FM_EPEE:
				//Reforger une épée
				list.add(new JobAction(113,3,0,true,lvl,0));
			break;
			case JOB_FM_MARTEAU:
				//Reforger une marteau
				list.add(new JobAction(116,3,0,true,lvl,0));
			break;
			case JOB_FM_PELLE:
				//Reforger une pelle
				list.add(new JobAction(117,3,0,true,lvl,0));
			break;
			case JOB_SM_ARC:
				//Resculpter un arc
				list.add(new JobAction(118,3,0,true,lvl,0));
			break;
			case JOB_SM_BATON:
				//Resculpter un baton
				list.add(new JobAction(120,3,0,true,lvl,0));
			break;
			case JOB_SM_BAGUETTE:
				//Resculpter une baguette
				list.add(new JobAction(119,3,0,true,lvl,0));
			break;
			
			case JOB_CHASSEUR:
			//Préparer une Viande
			list.add(new JobAction(134,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;
			
			case JOB_BOUCHER:
			//Préparer une Viande
			list.add(new JobAction(132,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;
			
			case JOB_POISSONNIER:
			//Preparer un Poisson
			list.add(new JobAction(135,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;
			
			case JOB_BOULANGER:
			//Cuir le Pain
			list.add(new JobAction(27,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Faire des Bonbons
			list.add(new JobAction(109,3,0,true,100,-1));
			break;
			
			case JOB_MINEUR:
			if(lvl > 99)
			{
			//Miner Dolomite
			list.add(new JobAction(161,-19 + dropWin,-18 + dropWin,false,12000-timeWin,60));
			}
			if(lvl > 79)
			{
			//Miner Or
			list.add(new JobAction(30,-15 + dropWin,-14 + dropWin,false,12000-timeWin,55));
			}
			if(lvl > 69)
			{
			//Miner Bauxite
			list.add(new JobAction(31,-13 + dropWin,-12 + dropWin,false,12000-timeWin,50));
			}
			if(lvl > 59)
			{
			//Miner Argent
			list.add(new JobAction(29,-11 + dropWin,-10 + dropWin,false,12000-timeWin,40));
			}
			if(lvl > 49)
			{
			//Miner Etain
			list.add(new JobAction(55,-9 + dropWin,-8 + dropWin,false,12000-timeWin,35));
			//Miner Silicate
			list.add(new JobAction(162,-9 + dropWin,-8 + dropWin,false,12000-timeWin,35));
			}
			if(lvl > 39)
			{
			//Miner Manganèse
			list.add(new JobAction(56,-7 + dropWin,-6 + dropWin,false,12000-timeWin,30));
			}
			if(lvl >29)
			{
			//Miner Kobalte
			list.add(new JobAction(28,-5 + dropWin,-4 + dropWin,false,12000-timeWin,25));
			}
			if(lvl >19)
			{
			//Miner Bronze
			list.add(new JobAction(26,-3 + dropWin,-2 + dropWin,false,12000-timeWin,20));
			}
			if(lvl >9)
			{
			//Miner Cuivre
			list.add(new JobAction(25,-1 + dropWin,0 + dropWin,false,12000-timeWin,15));
			}
			//Miner Fer
			list.add(new JobAction(24,1 + dropWin,2 + dropWin,false,12000-timeWin,10));
			//Fondre
			list.add(new JobAction(32,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Polir
			list.add(new JobAction(48,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;
			
			case JOB_PECHEUR:
			if(lvl > 74)
			{
			//Pêcher Poissons géants de mer
			list.add(new JobAction(131,0,1,false,12000-timeWin,35));
			}
			if(lvl > 69)
			{
			//Pêcher Poissons géants de rivière
			list.add(new JobAction(127,0,1,false,12000-timeWin,35));
			}
			if(lvl > 49)
			{
			//Pêcher Gros poissons de mers
			list.add(new JobAction(130,0,1,false,12000-timeWin,30));
			}
			if(lvl >39)
			{
			//Pêcher Gros poissons de rivière
			list.add(new JobAction(126,0,1,false,12000-timeWin,25));
			}
			if(lvl >19)
			{
			//Pêcher Poissons de mer
			list.add(new JobAction(129,0,1,false,12000-timeWin,20));
			}
			if(lvl >9)
			{
			//Pêcher Poissons de rivière
			list.add(new JobAction(125,0,1,false,12000-timeWin,15));
			}
			//Pêcher Ombre Etrange
			list.add(new JobAction(140,0,1,false,12000-timeWin,50));
			//Pêcher Pichon
			list.add(new JobAction(136,1,1,false,12000-timeWin,5));
			//Pêcher Petits poissons de rivière
			list.add(new JobAction(124,0,1,false,12000-timeWin,10));
			//Pêcher Petits poissons de mer
			list.add(new JobAction(128,0,1,false,12000-timeWin,10));
			//Vider
			list.add(new JobAction(133,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;
			
			case JOB_ALCHIMISTE:
			if(lvl > 49)
			{
			//Cueillir Graine de Pandouille
			list.add(new JobAction(160,-9 + dropWin,-8 + dropWin,false,12000-timeWin,35));
			//Cueillir Edelweiss
			list.add(new JobAction(74,-9 + dropWin,-8 + dropWin,false,12000-timeWin,35));
			}
			if(lvl > 39)
			{
			//Cueillir Orchidée
			list.add(new JobAction(73,-7 + dropWin,-6 + dropWin,false,12000-timeWin,30));
			}
			if(lvl >29)
			{
			//Cueillir Menthe
			list.add(new JobAction(72,-5 + dropWin,-4 + dropWin,false,12000-timeWin,25));
			}
			if(lvl >19)
			{
			//Cueillir Trèfle
			list.add(new JobAction(71,-3 + dropWin,-2 + dropWin,false,12000-timeWin,20));
			}
			if(lvl >9)
			{
			//Cueillir Chanvre
			list.add(new JobAction(54,-1 + dropWin,0 + dropWin,false,12000-timeWin,15));
			}
			//Cueillir Lin
			list.add(new JobAction(68,1 + dropWin,2 + dropWin,false,12000-timeWin,10));
			//Fabriquer une Potion
			list.add(new JobAction(23,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;
			
			case JOB_BUCHERON:
			if(lvl > 99)
			{
			//Couper Bambou Sacré
			list.add(new JobAction(158,-19 + dropWin,-18 + dropWin,false,12000-timeWin,75));
			}
			if(lvl > 89)
			{
			//Couper Orme
			list.add(new JobAction(35,-17 + dropWin,-16 + dropWin,false,12000-timeWin,70));
			}
			if(lvl > 79)
			{
			//Couper Charme
			list.add(new JobAction(38,-15 + dropWin,-14 + dropWin,false,12000-timeWin,65));
			//Couper Bambou Sombre
			list.add(new JobAction(155,-15 + dropWin,-14 + dropWin,false,12000-timeWin,65));
			}
			if(lvl > 74)
			{
			//Couper Kalyptus
			list.add(new JobAction(174,-14 + dropWin,-13 + dropWin,false,12000-timeWin,55));
			}
			if(lvl > 69)
			{
			//Couper Ebène
			list.add(new JobAction(34,-13 + dropWin,-12 + dropWin,false,12000-timeWin,50));
			}
			if(lvl > 59)
			{
			//Couper Merisier
			list.add(new JobAction(41,-11 + dropWin,-10 + dropWin,false,12000-timeWin,45));
			}
			if(lvl > 49)
			{
			//Couper If
			list.add(new JobAction(33,-9 + dropWin,-8 + dropWin,false,12000-timeWin,40));
			//Couper Bambou
			list.add(new JobAction(154,-9 + dropWin,-8 + dropWin,false,12000-timeWin,40));
			}
			if(lvl > 39)
			{
			//Couper Erable
			list.add(new JobAction(37,-7 + dropWin,-6 + dropWin,false,12000-timeWin,35));
			}
			if(lvl> 34)
			{
			//Couper Bombu
			list.add(new JobAction(139,-6 + dropWin,-5 + dropWin,false,12000-timeWin,30));
			//Couper Oliviolet
			list.add(new JobAction(141,-6 + dropWin,-5 + dropWin,false,12000-timeWin,30));
			}
			if(lvl >29)
			{
			//Couper Chêne
			list.add(new JobAction(10,-5 + dropWin,-4 + dropWin,false,12000-timeWin,25));
			}
			if(lvl >19)
			{
			//Couper Noyer
			list.add(new JobAction(40,-3 + dropWin,-2 + dropWin,false,12000-timeWin,20));
			}
			if(lvl >9)
			{
			//Couper Châtaignier
			list.add(new JobAction(39,-1 + dropWin,0 + dropWin,false,12000-timeWin,15));
			}
			//Couper Frêne
			list.add(new JobAction(6,1 + dropWin,2 + dropWin,false,12000-timeWin,10));
			//Scie
			list.add(new JobAction(101,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			break;
			
			case JOB_PAYSAN:
			if(lvl > 69)
			{
			//Faucher Chanvre
			list.add(new JobAction(54,-13 + dropWin,-12 + dropWin,false,12000-timeWin,45));
			}
			if(lvl > 59)
			{
			//Faucher Malt
			list.add(new JobAction(58,-11 + dropWin,-10 + dropWin,false,12000-timeWin,40));
			}
			if(lvl > 49)
			{
			//Faucher Riz
			list.add(new JobAction(159,-9 + dropWin,-8 + dropWin,false,12000-timeWin,35));
			//Faucher Seigle
			list.add(new JobAction(52,-9 + dropWin,-8 + dropWin,false,12000-timeWin,35));
			}
			if(lvl> 39)
			{
			//Faucher Lin
			list.add(new JobAction(50,-7 + dropWin,-6 + dropWin,false,12000-timeWin,30));
			}
			if(lvl >29)
			{
			//Faucher Houblon
			list.add(new JobAction(46,-5 + dropWin,-4 + dropWin,false,12000-timeWin,25));
			}
			if(lvl >19)
			{
			//Faucher Avoine
			list.add(new JobAction(57,-3 + dropWin,-2 + dropWin,false,12000-timeWin,20));
			}
			if(lvl >9)
			{
			//Faucher Orge
			list.add(new JobAction(53,-1 + dropWin,0 + dropWin,false,12000-timeWin,15));
			}
			//Faucher blé
			list.add(new JobAction(45,1 + dropWin,2 + dropWin,false,12000-timeWin,10));
			//Moudre
			list.add(new JobAction(47,getTotalCaseByJobLevel(lvl),0,true,getChanceForMaxCase(lvl),-1));
			//Egrener 100% 1 case tout le temps ?
			list.add(new JobAction(122,1,0,true,100,-1));
			break;
		}
		return list;
	}

	public static boolean isJobAction(int a)
	{
		for(int v = 0;v < JOB_ACTION.length;v++)
		{
			if(JOB_ACTION[v][0] == a)return true;
		}
		return false;
	}

	public static int getObjectByJobSkill(int skID,boolean special)
	{
		for(int v = 0;v < JOB_ACTION.length;v++)if(JOB_ACTION[v][0] == skID)return (JOB_ACTION[v].length>1 && special?JOB_ACTION[v][2]:JOB_ACTION[v][1]);
		return -1;
	}

	public static int getChanceByNbrCaseByLvl(int lvl, int nbr)
	{
		if(nbr <= getTotalCaseByJobLevel(lvl)-2)return 100;//99.999... normalement, mais osef
		return getChanceForMaxCase(lvl);
	}

	public static boolean isMageJob(int id)
	{
		if((id>12 && id <50) || (id>61 && id <65))return true;
		return false;
	}
	
	public static Stats getMountStats(int color,int lvl)
	{
		Stats stats = new Stats();
		switch(color)
		{
			//Amande sauvage
			case 1:break;
			//Ebene
			case 3:
				stats.addOneStat(STATS_ADD_VITA, lvl/2);
				stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/1.25));//100/1.25 = 80
			break;
			//Rousse |
			case 10:
			stats.addOneStat(STATS_ADD_VITA, lvl); //100/1 = 100
			break;
			//Amande
			case 20:
			stats.addOneStat(STATS_ADD_INIT, lvl*10); // 100*10 = 1000
			break;
			//Dorée
			case 18:
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2)); 
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/2.50)); // 100/2.50 = 40
			break;
			//Rousse-Amande
			case 38:
			stats.addOneStat(STATS_ADD_INIT, lvl*5); // 100*5 = 500
			stats.addOneStat(STATS_ADD_VITA, lvl); 
			stats.addOneStat(STATS_CREATURE, (int)(lvl/50)); // 100/50 = 2
			break;
			//Rousse-Dorée
			case 46:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4)); //100/4 = 25
		    break;
			//Amande-Dorée
			case 33:
			stats.addOneStat(STATS_ADD_INIT, lvl*5);
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4));
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2));
			stats.addOneStat(STATS_CREATURE, (int)(lvl/100)); // 100/100 = 1
			break;
			//Indigo |
			case 17:
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/1.25));
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2));
			break;
			//Rousse-Indigo
			case 62:
			stats.addOneStat(STATS_ADD_VITA,(int)(lvl*1.50)); // 100*1.50 = 150
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/1.65));
			break;
			//Rousse-Ebène
			case 12:
			stats.addOneStat(STATS_ADD_VITA,(int)(lvl*1.50));
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/1.65));
			break;
			//Amande-Indigo
			case 36:
			stats.addOneStat(STATS_ADD_INIT, lvl*5);
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2)); 
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/1.65));
			stats.addOneStat(STATS_CREATURE, (int)(lvl/100));
			break;
			//Pourpre | Stade 4
			case 19:
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/1.25));
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2));
			break;
			//Orchidée
			case 22:
			stats.addOneStat(STATS_ADD_INTE, (int)(lvl/1.25));
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2));
			break;
			//Dorée-Orchidée |
			case 48:
			stats.addOneStat(STATS_ADD_VITA, (lvl));
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4));
		    stats.addOneStat(STATS_ADD_INTE, (int)(lvl/1.65));
			break;
			//Indigo-Pourpre
			case 65:
			stats.addOneStat(STATS_ADD_VITA, (lvl));
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/2));
			break;
			//Indigo-Orchidée
			case 67:
			stats.addOneStat(STATS_ADD_VITA, (lvl));
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_INTE, (int)(lvl/2));
			break;
			//Ebène-Pourpre
			case 54:
			stats.addOneStat(STATS_ADD_VITA, (lvl));
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/2));
			break;
			//Ebène-Orchidée
			case 53:
			stats.addOneStat(STATS_ADD_VITA, (lvl));
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_INTE, (int)(lvl/2));
			break;
			//Pourpre-Orchidée
			case 76:
			stats.addOneStat(STATS_ADD_VITA, (lvl));
			stats.addOneStat(STATS_ADD_INTE, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/2));
			break;
			// Amande-Ebene	| Nami-start
			case 37:
			stats.addOneStat(STATS_ADD_INIT, lvl*5);
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2)); 
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/1.65));
			stats.addOneStat(STATS_CREATURE, (int)(lvl/100));
			break;
			// Amande-Rousse
			case 44:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4));
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/1.65));
			break;
			// Dorée-Ebène
			case 42:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4));
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/1.65));
			break;
			// Indigo-Ebène
			case 51:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/2));
			break;
			// Rousse-Pourpre
			case 71:
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl*1.5));
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/1.65));
			break;
			// Rousse-Orchidée
			case 70:
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl*1.5));
			stats.addOneStat(STATS_ADD_INTE, (int)(lvl/1.65));
			break;
			// Amande-Pourpre
			case 41:
			stats.addOneStat(STATS_ADD_INIT, lvl*5);
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2)); 
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/1.65));
			stats.addOneStat(STATS_CREATURE, (int)(lvl/100));
			break;
			// Amande-Orchidée
			case 40:
			stats.addOneStat(STATS_ADD_INIT, lvl*5);
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2)); 
			stats.addOneStat(STATS_ADD_INTE, (int)(lvl/1.65));
			stats.addOneStat(STATS_CREATURE, (int)(lvl/100));
			break;
			// Dorée-Pourpre
			case 49:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4));
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/1.65));
			break;
			// Ivoire
			case 16:
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2));
			break;
	        // Turquoise
			case 15:
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_PROS, (int)(lvl/1.25));
			break;
			//Rousse-Ivoire
			case 11:
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl*2)); // 100*2 = 200
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2.5)); // = 40
			break;
			//Rousse-Turquoise
			case 69:
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl*2));
			stats.addOneStat(STATS_ADD_PROS, (int)(lvl/2.50));
			break;
			//Amande-Turquoise
			case 39:
			stats.addOneStat(STATS_ADD_INIT, lvl*5);
			stats.addOneStat(STATS_ADD_VITA, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_PROS, (int)(lvl/2.50));
			stats.addOneStat(STATS_CREATURE, (int)(lvl/100));
			break;
			//Dorée-Ivoire
			case 45:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2.5));
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4));
			break;
			//Dorée-Turquoise
			case 47:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_PROS, (int)(lvl/2.50));
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4));
			break;
			//Indigo-Ivoire
			case 61:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/2.50));
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2.5));
			break;
			//Indigo-Turquoise
			case 63:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2.5));
			break;
			//Ebène-Ivoire
			case 9:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/2.50));
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2.5));
			break;
			//Ebène-Turquoise
			case 52:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PROS, (int)(lvl/2.50));
			break;
			//Pourpre-Ivoire
			case 68:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2.5));
			break;
			//Pourpre-Turquoise
			case 73:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PROS, (int)(lvl/2.50));
			break;
			//Orchidée-Turquoise
			case 72:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_INTE, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2.5));
			break;
			//Ivoire-Turquoise
			case 66:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2.5));
			stats.addOneStat(STATS_ADD_PROS, (int)(lvl/2.50));
			break;
			// Emeraude
			case 21:
			stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			break;
			// Prune
			case 23:
			stats.addOneStat(STATS_ADD_VITA, lvl*2); // 100*2 = 200
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/50));
			break;
			//Emeraude-Rousse
			case 57:
			stats.addOneStat(STATS_ADD_VITA, lvl*3); // 100*3 = 300
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			break;
			//Rousse-Prune
			case 84:
			stats.addOneStat(STATS_ADD_VITA, lvl*3);
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			break;
			//Amande-Emeraude
			case 35:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			stats.addOneStat(STATS_CREATURE, (int)(lvl/100));
			stats.addOneStat(STATS_ADD_INIT, lvl*5);
			break;
			//Amande-Prune
			case 77:
			stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_INIT, lvl*5);
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			stats.addOneStat(STATS_CREATURE, (int)(lvl/100));
			break;
			//Dorée-Emeraude
			case 43:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4));
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			break;
			//Dorée-Prune
			case 78:
			stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_SAGE, (int)(lvl/4));
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			break;
			//Indigo-Emeraude
			case 55:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/3.33));
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			break;
			//Indigo-Prune
			case 82:
			stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_CHAN, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			break;
			//Ebène-Emeraude
			case 50:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/3.33));
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			break;
			//Ebène-Prune
			case 79:
			stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_AGIL, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			break;
			//Pourpre-Emeraude
			case 60:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/3.33));
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			break;
			//Pourpre-Prune
			case 87:
			stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_FORC, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			break;
			//Orchidée-Emeraude
			case 59:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_INTE, (int)(lvl/3.33));
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			break;
			//Orchidée-Prune
			case 86:
			stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_INTE, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			break;
			//Ivoire-Emeraude
			case 56:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/3.33));
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			break;
			//Ivoire-Prune
			case 83:
			stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			break;
			//Turquoise-Emeraude
			case 58:
			stats.addOneStat(STATS_ADD_VITA, lvl);
			stats.addOneStat(STATS_ADD_PROS, (int)(lvl/3.33));
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			break;
			//Turquoise-Prune
			case 85:
			stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_PROS, (int)(lvl/1.65));
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			break;
			//Emeraude-Prune
			case 80:
		    stats.addOneStat(STATS_ADD_VITA, lvl*2);
			stats.addOneStat(STATS_ADD_PM, (int)(lvl/100));
			stats.addOneStat(STATS_ADD_PO, (int)(lvl/100));
			break;
			//Armure
			case 88:
			stats.addOneStat(STATS_ADD_PERDOM, (int)(lvl/2));
			stats.addOneStat(STATS_ADD_RP_AIR, (int)(lvl/20));
			stats.addOneStat(STATS_ADD_RP_EAU, (int)(lvl/20));
			stats.addOneStat(STATS_ADD_RP_TER, (int)(lvl/20));
			stats.addOneStat(STATS_ADD_RP_FEU, (int)(lvl/20));
			stats.addOneStat(STATS_ADD_RP_NEU, (int)(lvl/20));
			break;
		}
		return stats;
	}
	public static ObjTemplate getParchoTemplateByMountColor(int color)
	{
		switch(color)
		{
			//Ammande sauvage
			case 2: return World.getObjTemplate(7807);
			//Ebene | Page 1
			case 3: return World.getObjTemplate(7808);
			//Rousse sauvage
			case 4: return World.getObjTemplate(7809);
			//Ebene-ivoire
			case 9: return World.getObjTemplate(7810);
			//Rousse
			case 10: return World.getObjTemplate(7811);
			//Ivoire-Rousse
			case 11: return World.getObjTemplate(7812);
			//Ebene-rousse
			case 12: return World.getObjTemplate(7813);
			//Turquoise
			case 15: return World.getObjTemplate(7814);
			//Ivoire
			case 16: return World.getObjTemplate(7815);
			//Indigo
			case 17: return World.getObjTemplate(7816);
			//Dorée
			case 18: return World.getObjTemplate(7817);
			//Pourpre
			case 19: return World.getObjTemplate(7818);
			//Amande
			case 20: return World.getObjTemplate(7819);
			//Emeraude
			case 21: return World.getObjTemplate(7820);
			//Orchidée
			case 22: return World.getObjTemplate(7821);
			//Prune
			case 23: return World.getObjTemplate(7822);
			//Amande-Dorée
			case 33: return World.getObjTemplate(7823);
			//Amande-Ebene
			case 34: return World.getObjTemplate(7824);
			//Amande-Emeraude
			case 35: return World.getObjTemplate(7825);
			//Amande-Indigo
			case 36: return World.getObjTemplate(7826);
			//Amande-Ivoire
			case 37: return World.getObjTemplate(7827);
			//Amande-Rousse
			case 38: return World.getObjTemplate(7828);
			//Amande-Turquoise
			case 39: return World.getObjTemplate(7829);
			//Amande-Orchidée
			case 40: return World.getObjTemplate(7830);
			//Amande-Pourpre
			case 41: return World.getObjTemplate(7831);
			//Dorée-Ebène
			case 42: return World.getObjTemplate(7832);
			//Dorée-Emeraude
			case 43: return World.getObjTemplate(7833);
			//Dorée-Indigo
			case 44: return World.getObjTemplate(7834);
			//Dorée-Ivoire
			case 45: return World.getObjTemplate(7835);
			//Dorée-Rousse | Page 2
			case 46: return World.getObjTemplate(7836);
			//Dorée-Turquoise
			case 47: return World.getObjTemplate(7837);
			//Dorée-Orchidée
			case 48: return World.getObjTemplate(7838);
			//Dorée-Pourpre
			case 49: return World.getObjTemplate(7839);
			//Ebène-Emeraude
			case 50: return World.getObjTemplate(7840);
			//Ebène-Indigo
			case 51: return World.getObjTemplate(7841);
			//Ebène-Turquoise
			case 52: return World.getObjTemplate(7842);
			//Ebène-Orchidée
			case 53: return World.getObjTemplate(7843);
			//Ebène-Pourpre
			case 54: return World.getObjTemplate(7844);
			//Emeraude-Indigo
			case 55: return World.getObjTemplate(7845);
			//Emeraude-Ivoire
			case 56: return World.getObjTemplate(7846);
			//Emeraude-Rousse
			case 57: return World.getObjTemplate(7847);
			//Emeraude-Turquoise
			case 58: return World.getObjTemplate(7848);
			//Emeraude-Orchidée
			case 59: return World.getObjTemplate(7849);
			//Emeraude-Pourpre
			case 60: return World.getObjTemplate(7850);
			//Indigo-Ivoire
			case 61: return World.getObjTemplate(7851);
			//Indigo-Rousse
			case 62: return World.getObjTemplate(7852);
			//Indigo-Turquoise
			case 63: return World.getObjTemplate(7853);
			//Indigo-Orchidée
			case 64: return World.getObjTemplate(7854);
			//Indigo-Pourpre
			case 65: return World.getObjTemplate(7855);
			//Ivoire-Turquoise
			case 66: return World.getObjTemplate(7856);
			//Ivoire-Ochidée
			case 67: return World.getObjTemplate(7857);
			//Ivoire-Pourpre
			case 68: return World.getObjTemplate(7858);
			//Turquoise-Rousse
			case 69: return World.getObjTemplate(7859);
			//Ochidée-Rousse
			case 70: return World.getObjTemplate(7860);
			//Pourpre-Rousse
			case 71: return World.getObjTemplate(7861);
			//Turquoise-Orchidée
			case 72: return World.getObjTemplate(7862);
			//Turquoise-Pourpre
			case 73: return World.getObjTemplate(7863);
			//Dorée sauvage
			case 74: return World.getObjTemplate(7864);
			//Squelette
			case 75: return World.getObjTemplate(7865);
			//Orchidée-Pourpre
			case 76: return World.getObjTemplate(7866);
			//Prune-Amande
			case 77: return World.getObjTemplate(7867);
			//Prune-Dorée
			case 78: return World.getObjTemplate(7868);
			//Prune-Ebène
			case 79: return World.getObjTemplate(7869);
			//Prune-Emeraude
			case 80: return World.getObjTemplate(7870);
			//Prune et Indigo
			case 82: return World.getObjTemplate(7871);
			//Prune-Ivoire
			case 83: return World.getObjTemplate(7872);
			//Prune-Rousse
			case 84: return World.getObjTemplate(7873);
			//Prune-Turquoise
			case 85: return World.getObjTemplate(7874);
			//Prune-Orchidée
			case 86: return World.getObjTemplate(7875);
			//Prune-Pourpre
			case 87: return World.getObjTemplate(7876);
			//Armure
			case 88: return World.getObjTemplate(9582);		
		}
		return null;
	}
	public static int getMountColorByParchoTemplate(int tID)
	{
		for(int a = 1;a<100;a++)if(getParchoTemplateByMountColor(a)!=null)if(getParchoTemplateByMountColor(a).getID() == tID)return a; 
		return -1;
	}
	
	public static void applyPlotIOAction(Personnage perso,int mID, int cID)
	{
		//Gère les differentes actions des "bornes" (IO des émotes)
		switch(mID)
		{
		case 2196://Création de guilde
			if(perso.is_away())return;
			if(perso.get_guild() != null || perso.getGuildMember() != null)
			{
				SocketManager.GAME_SEND_gC_PACKET(perso, "Ea");
				return;
			}
			if(!perso.hasItemTemplate(1575,1))//Guildalogemme
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "14");
			}
			SocketManager.GAME_SEND_gn_PACKET(perso);
		break;
		default:
			GameServer.addToLog("PlotIOAction non gere pour la map "+mID+" cell="+cID);
		}
	}
	
	public static int getNearCellidUnused(Personnage _perso)
	{
		int cellFront = 0;
		int cellBack = 0;
		int cellRight = 0;
		int cellLeft = 0;
		if(_perso.get_curCarte().getSubArea().get_area().get_id() == 7 || _perso.get_curCarte().getSubArea().get_area().get_id() == 11)
		{
			cellFront = 19;
			cellBack = -19;
			cellRight = 18;
			cellLeft = -18;
		}else
		{
			cellFront = 15;
			cellBack = -15;
			cellRight = 14;
			cellLeft = -14;
		}
		if(_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellFront).getDroppedItem() == null
				&& _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()+cellFront).getPersos().isEmpty()
				&& _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()+cellFront).isWalkable(false))
		{
			return cellFront;
		}else
		if(_perso.get_curCarte().getCase(_perso.get_curCell().getID()-cellBack).getDroppedItem() == null
				&& _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()-cellBack).getPersos().isEmpty()
				&& _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()-cellBack).isWalkable(false))
		{
			return cellBack;
		}else
		if(_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellRight).getDroppedItem() == null
				&& _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()+cellRight).getPersos().isEmpty()
				&& _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()+cellRight).isWalkable(false))
		{
			return cellRight;
		}else
		if(_perso.get_curCarte().getCase(_perso.get_curCell().getID()-cellLeft).getDroppedItem() == null
				&& _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()-cellLeft).getPersos().isEmpty()
				&& _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()-cellLeft).isWalkable(false))
		{
			return cellLeft;
		}
		
		return -1;
	}
}