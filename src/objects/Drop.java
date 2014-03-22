package objects;

public class Drop
{
	private int _itemID;
	private int _prosp;
	private float _taux;
	private int _max;
	
	public Drop(int itm,int p,float t,int m)
	{
		_itemID = itm;
		_prosp = p;
		_taux = t;
		_max = m;
	}
	public void setMax(int m)
	{
		_max = m;
	}
	public int get_itemID() {
		return _itemID;
	}

	public int getMinProsp() {
		return _prosp;
	}

	public float get_taux() {
		return _taux;
	}

	public int get_max() {
		return _max;
	}
}