package objects;

import java.util.ArrayList;


public class SuperArea
{
	private int _id;
	private ArrayList<Area> _areas = new ArrayList<Area>();
	
	public SuperArea(int a_id)
	{
		_id = a_id;
	}
	
	public void addArea(Area A)
	{
		_areas.add(A);
	}
	
	public int get_id()
	{
		return _id;
	}
}