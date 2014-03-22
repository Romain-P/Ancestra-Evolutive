package common;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.Timer;

public class SendManager {

	private static Map<Integer, Map<Long, Map<PrintWriter, String>>> PacketBuffer	= new TreeMap<Integer, Map<Long, Map<PrintWriter, String>>>();//<hachID, <PacketID, <PacketID,String>>>
	static long packetid = 1;
	private static String BufferRemove = "";
	
	public static String get_BufferRemove()
	{
		return BufferRemove;
	}
	
	public static void set_BufferRemove(String str)
	{
		BufferRemove += str;
	}
	
	public static void del_BufferRemove()
	{
		BufferRemove = "";
	}
	
	public static Map<Integer, Map<Long, Map<PrintWriter, String>>> getPacketBuffer()
	{
		return PacketBuffer;
	}
	
	public static Timer FlushTimer()
	{
	    ActionListener action = new ActionListener ()
	      {
	        public void actionPerformed (ActionEvent event)
	        {
	        	for(Entry<Integer, Map<Long, Map<PrintWriter, String>>> data : SendManager.getPacketBuffer().entrySet())
	        	{
	        		if(SendManager.getPacketBuffer().get(data.getKey()).isEmpty()) continue;
	        		StringBuilder Totaldata = new StringBuilder();
	        		PrintWriter pw = null;
	        		for(Entry<Long, Map<PrintWriter, String>> s : SendManager.getPacketBuffer().get(data.getKey()).entrySet())
	        		{
	        			for(Entry<PrintWriter, String> s2 : SendManager.getPacketBuffer().get(data.getKey()).get(s.getKey()).entrySet())
	        			{
	        				Totaldata.append((s2.getValue())).append((char)0x00);
	        				if(pw != null && (pw.hashCode() == s2.getKey().hashCode())) continue;
	        				pw = s2.getKey();
	        			}
	        			SendManager.set_BufferRemove(s.getKey()+",");
	        		}
	        		if(Totaldata.toString().isEmpty()) continue;
	        		for(String id : SendManager.get_BufferRemove().split(","))
	        		{
	        			data.getValue().remove(Long.parseLong(id));
	        		}
	        		SendManager.del_BufferRemove();
	        		pw.print(Totaldata.toString());
	        		pw.flush();
	        		if(Ancestra.CONFIG_DEBUG)
	        			System.out.println("Multi: Send>>"+Totaldata.toString());
	        	}
	        }
	      };
	    return new Timer (Ancestra.CONFIG_SOCKET_TIME_COMPACT_DATA, action);
	}
	
	public static void send(PrintWriter out, String packet)
	{
		if(!getPacketBuffer().containsKey(out.hashCode()))
		{
			Map<PrintWriter, String> firstData = new TreeMap<PrintWriter, String>();
			firstData.put(out, packet);
			Map<Long, Map<PrintWriter, String>> secondData = new TreeMap<Long, Map<PrintWriter, String>>();
			secondData.put(packetid++, firstData);
			PacketBuffer.put(out.hashCode(), secondData);
		}else
		{
			Map<PrintWriter, String> data = new TreeMap<PrintWriter, String>();
			data.put(out, packet);
			PacketBuffer.get(out.hashCode()).put(packetid++, data);
		}
	}
}