package objects;

import java.util.ArrayList;

import common.ConditionParser;
import common.World;

import objects.Objet.ObjTemplate;

public class NPC_tmpl {
	private int _id;
	private int _bonusValue;
	private int _gfxID;
	private int _scaleX;
	private int _scaleY;
	private int _sex;
	private int _color1;
	private int _color2;
	private int _color3;
	private String _acces;
	private int _extraClip;
	private int _customArtWork;
	private int _initQuestionID;
	private ArrayList<ObjTemplate> _ventes = new ArrayList<ObjTemplate>();
	
	public static class NPC_question
	{
		private int _id;
		private String _reponses;
		private String _args;
		
		private String _cond;
		private int falseQuestion;
		
		public NPC_question(int _id, String _reponses, String _args, String _cond, int falseQuestion) {
			this._id = _id;
			this._reponses = _reponses;
			this._args = _args;
			this._cond = _cond;
			this.falseQuestion = falseQuestion;
		}
		
		public int get_id()
		{
			return _id;
		}
		
		public String parseToDQPacket(Personnage perso)
		{
			if(!ConditionParser.validConditions(perso, _cond))
				return World.getNPCQuestion(falseQuestion).parseToDQPacket(perso);
			
			String str = _id+"";
			if(!_args.equals(""))
				str += ";"+parseArguments(_args,perso);
			str += "|"+_reponses;
			return str;
		}
		
		public String getReponses()
		{
			return _reponses;
		}
		
		private String parseArguments(String args, Personnage perso)
		{
			String arg = args;
			arg = arg.replace("[name]", perso.getStringVar("name"));
			arg = arg.replace("[bankCost]", perso.getStringVar("bankCost"));
			/*TODO*/
			return arg;
		}

		public void setReponses(String reps)
		{
			_reponses = reps;
		}
	}
	
	public static class NPC
	{
		private NPC_tmpl _template;
		private int _cellID;
		private int _guid;
		private byte _orientation;
		
		public NPC (NPC_tmpl temp,int guid,int cell, byte o)
		{
			_template = temp;
			_guid = guid;
			_cellID = cell;
			_orientation  = o;
		}

		public NPC_tmpl get_template() {
			return _template;
		}

		public int get_cellID() {
			return _cellID;
		}

		public int get_guid() {
			return _guid;
		}

		public int get_orientation() {
			return _orientation;
		}

		public String parseGM()
		{
			StringBuilder sock = new StringBuilder();
			sock.append("+");
			sock.append(_cellID).append(";");
			sock.append(_orientation).append(";");
			sock.append("0").append(";");
			sock.append(_guid).append(";");
			sock.append(_template.get_id()).append(";");
			sock.append("-4").append(";");//type = NPC
			
			StringBuilder taille = new StringBuilder();
			if(_template.get_scaleX() == _template.get_scaleY())
			{
				taille.append(_template.get_scaleY());
			}else
			{
				taille.append(_template.get_scaleX()).append("x").append(_template.get_scaleY());
			}
			sock.append(_template.get_gfxID()).append("^").append(taille.toString()).append(";");
			sock.append(_template.get_sex()).append(";");
			sock.append(( _template.get_color1() != -1?Integer.toHexString( _template.get_color1()):"-1")).append(";");
			sock.append(( _template.get_color2() != -1?Integer.toHexString( _template.get_color2()):"-1")).append(";");
			sock.append(( _template.get_color3() != -1?Integer.toHexString( _template.get_color3()):"-1")).append(";");
			sock.append(_template.get_acces()).append(";");
			sock.append((_template.get_extraClip()!=-1?(_template.get_extraClip()):(""))).append(";");
			sock.append(_template.get_customArtWork());
			return sock.toString();
		}

		public void setCellID(int id)
		{
			_cellID = id;
		}

		public void setOrientation(byte o)
		{
			_orientation = o;
		}
		
	}
	
	public static class NPC_reponse
	{
		private int _id;
		private ArrayList<Action> _actions = new ArrayList<Action>();
		
		public NPC_reponse(int id)
		{
			_id = id;
		}
		
		public int get_id()
		{
			return _id;
		}
		
		public void addAction(Action act)
		{
			ArrayList<Action> c = new ArrayList<Action>();
			c.addAll(_actions);
			for(Action a : c)if(a.getID() == act.getID())_actions.remove(a);
			_actions.add(act);
		}
		
		public void apply(Personnage perso)
		{
			for(Action act : _actions)
			act.apply(perso, null, -1, -1);
		}
		
		public boolean isAnotherDialog()
		{
			for(Action curAct : _actions)
			{
				if(curAct.getID() == 1) //1 = Discours NPC
					return true;
			}
			
			return false;
		}
	}
	
	public NPC_tmpl(int _id, int value, int _gfxid, int _scalex, int _scaley,
			int _sex, int _color1, int _color2, int _color3, String _acces,
			int clip, int artWork, int questionID,String ventes) {
		super();
		this._id = _id;
		_bonusValue = value;
		_gfxID = _gfxid;
		_scaleX = _scalex;
		_scaleY = _scaley;
		this._sex = _sex;
		this._color1 = _color1;
		this._color2 = _color2;
		this._color3 = _color3;
		this._acces = _acces;
		_extraClip = clip;
		_customArtWork = artWork;
		_initQuestionID = questionID;
		if(ventes.equals(""))return;
		for(String obj : ventes.split("\\,"))
		{
			try
			{
				int tempID = Integer.parseInt(obj);
				ObjTemplate temp = World.getObjTemplate(tempID);
				if(temp == null)continue;
				_ventes.add(temp);
			}catch(NumberFormatException e){continue;};
		}
	}

	public int get_id() {
		return _id;
	}

	public int get_bonusValue() {
		return _bonusValue;
	}

	public int get_gfxID() {
		return _gfxID;
	}

	public int get_scaleX() {
		return _scaleX;
	}

	public int get_scaleY() {
		return _scaleY;
	}

	public int get_sex() {
		return _sex;
	}

	public int get_color1() {
		return _color1;
	}

	public int get_color2() {
		return _color2;
	}

	public int get_color3() {
		return _color3;
	}

	public String get_acces() {
		return _acces;
	}

	public int get_extraClip() {
		return _extraClip;
	}

	public int get_customArtWork() {
		return _customArtWork;
	}

	public int get_initQuestionID() {
		return _initQuestionID;
	}
	
	public String getItemVendorList()
	{
		StringBuilder items = new StringBuilder();
		if(_ventes.isEmpty())return "";
		for(ObjTemplate obj : _ventes)
		{
			items.append(obj.parseItemTemplateStats()).append("|");
		}
		return items.toString();
	}

	public boolean addItemVendor(ObjTemplate T)
	{
		if(_ventes.contains(T))return false;
		_ventes.add(T);
		return true;
	}
	public boolean delItemVendor(int tID)
	{
		ArrayList<ObjTemplate> newVentes = new ArrayList<ObjTemplate>();
		boolean remove = false;
		for(ObjTemplate T : _ventes)
		{
			if(T.getID() == tID)
			{
				remove = true;
				continue;
			}
			newVentes.add(T);
		}
		_ventes = newVentes;
		return remove;
	}

	public void setInitQuestion(int q)
	{
		_initQuestionID = q;
	}
	
	public boolean haveItem(int templateID)
	{
		for(ObjTemplate curTemp : _ventes)
		{
			if(curTemp.getID() == templateID)
				return true;
		}
		
		return false;
	}
}
