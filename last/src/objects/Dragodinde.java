package objects;

import java.util.ArrayList;
import java.util.Map.Entry;

import common.Constants;
import common.SQLManager;
import common.World;

import objects.Personnage.Stats;

public class Dragodinde {

	private int _id;
	private int _color;
	private int _sexe;
	private int _amour;
	private int _endurance;
	private int _level;
	private long _exp;
	private String _nom;
	private int _fatigue;
	private int _energie;
	private int _reprod;
	private int _maturite;
	private int _serenite;
	private Stats _stats = new Stats();
	private String _ancetres = ",,,,,,,,,,,,,";
	private ArrayList<Objet> _items = new ArrayList<Objet>();
	//TODO: Capacités
	
	public Dragodinde(int color)
	{
		_id = World.getNextIdForMount();
		_color = color;
		_level = 1;
		_exp = 0;
		_nom = "SansNom";
		_fatigue = 0;
		_energie = getMaxEnergie();
		_reprod = 0;
		_maturite = getMaxMatu();
		_serenite = 0;
		_stats = Constants.getMountStats(_color,_level);
		_ancetres = ",,,,,,,,,,,,,";
		
		World.addDragodinde(this);
		SQLManager.CREATE_MOUNT(this);
	}
	
	public Dragodinde(int id, int color, int sexe, int amour, int endurance,
			int level, long exp, String nom, int fatigue,
			int energie, int reprod, int maturite, int serenite,String items,String anc)
	{
		_id = id;
		_color = color;
		_sexe = sexe;
		_amour = amour;
		_endurance = endurance;
		_level = level;
		_exp = exp;
		_nom = nom;
		_fatigue = fatigue;
		_energie = energie;
		_reprod = reprod;
		_maturite = maturite;
		_serenite = serenite;
		_ancetres = anc;
		_stats = Constants.getMountStats(_color,_level);
		for(String str : items.split(";"))
		{
			try
			{
				Objet obj = World.getObjet(Integer.parseInt(str));
				if(obj != null)_items.add(obj);
			}catch(Exception e){continue;}
		}
	}

	public int get_id() {
		return _id;
	}

	public int get_color() {
		return _color;
	}

	public int get_sexe() {
		return _sexe;
	}

	public int get_amour() {
		return _amour;
	}

	public String get_ancetres() {
		return _ancetres;
	}

	public int get_endurance() {
		return _endurance;
	}
	public int get_level() {
		return _level;
	}

	public long get_exp() {
		return _exp;
	}

	public String get_nom() {
		return _nom;
	}

	public int get_fatigue() {
		return _fatigue;
	}

	public int get_energie() {
		return _energie;
	}

	public int get_reprod() {
		return _reprod;
	}

	public int get_maturite() {
		return _maturite;
	}

	public int get_serenite() {
		return _serenite;
	}

	public Stats get_stats() {
		return _stats;
	}

	public ArrayList<Objet> get_items() {
		return _items;
	}
	
	public String parse()
	{
		StringBuilder str = new StringBuilder();
		str.append(_id).append(":");
		str.append(_color).append(":");
		str.append(_ancetres).append(":");
		str.append(",").append(":");//FIXME capacités
		str.append(_nom).append(":");
		str.append(_sexe).append(":");
		str.append(parseXpString()).append(":");
		str.append(_level).append(":");
		str.append("1").append(":");//FIXME
		str.append(getTotalPod()).append(":");
		str.append("0").append(":");//FIXME podActuel?
		str.append(_endurance).append(",10000:");
		str.append(_maturite).append(",").append(getMaxMatu()).append(":");
		str.append(_energie).append(",").append(getMaxEnergie()).append(":");
		str.append(_serenite).append(",-10000,10000:");
		str.append(_amour).append(",10000:");
		str.append("-1").append(":");//FIXME
		str.append("0").append(":");//FIXME
		str.append(parseStats()).append(":");
		str.append(_fatigue).append(",240:");
		str.append(_reprod).append(",20:");
		return str.toString();
	}

	private String parseStats()
	{
		String stats = "";
		for(Entry<Integer,Integer> entry : _stats.getMap().entrySet())
		{
			if(entry.getValue() <= 0)continue;
			if(stats.length() >0)stats += ",";
			stats += Integer.toHexString(entry.getKey())+"#"+Integer.toHexString(entry.getValue())+"#0#0";
		}
		return stats;
	}

	private int getMaxEnergie()
	{
		int energie = 10000;
		return energie;
	}

	private int getMaxMatu()
	{
		int matu = 1000;
		return matu;
	}

	private int getTotalPod()
	{
		int pod = 1000;
		
		return pod;
	}

	private String parseXpString()
	{
		return _exp+","+World.getExpLevel(_level).dinde+","+World.getExpLevel(_level+1).dinde;
	}

	public boolean isMountable()
	{
		if(_energie <10
		|| _maturite < getMaxMatu()
		|| _fatigue == 240)return false;
		return true;
	}

	public String getItemsId()
	{
		String str = "";
		for(Objet obj : _items)str += (str.length()>0?";":"")+obj.getGuid();
		return str;
	}

	public void setName(String packet)
	{
		_nom = packet;
		SQLManager.UPDATE_MOUNT_INFOS(this);
	}
	
	public void addXp(long amount)
	{
		_exp += amount;

		while(_exp >= World.getExpLevel(_level+1).dinde && _level<100)
			levelUp();
		
	}
	
	public void levelUp()
	{
		_level++;
		_stats = Constants.getMountStats(_color,_level);
	}
}
