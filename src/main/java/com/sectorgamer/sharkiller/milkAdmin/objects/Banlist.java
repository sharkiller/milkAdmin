package com.sectorgamer.sharkiller.milkAdmin.objects;

import java.io.File;
import java.io.IOException;

import com.sectorgamer.sharkiller.milkAdmin.MilkAdmin;
import com.sectorgamer.sharkiller.milkAdmin.util.MilkAdminLog;
import com.sectorgamer.sharkiller.milkAdmin.util.PropertiesFile;

/**
 * Handle milkAdmin Ban list system.
 * @author Sharkiller
 */
public class Banlist {
	public PropertiesFile banListName;
	public PropertiesFile banListIp;
	private MilkAdmin plugin;
	
	public Banlist(MilkAdmin i){
		this.plugin = i;
		banListName = new PropertiesFile(plugin.BLDir + File.separator + "banlistname.ini");
		banListIp = new PropertiesFile(plugin.BLDir + File.separator + "banlistip.ini");
		
		try {
			banListName.load();
			banListIp.load();
		} catch (IOException e) {
			MilkAdminLog.severe("Could not load banlist files.", e);
		}
	}
	
	public String count(boolean nick) throws Exception{
		if(nick)
			return String.valueOf(banListName.returnMap().size());
		else
			return String.valueOf(banListIp.returnMap().size());
	}
	
	/**
	 * Check if player name or ip is in a banlist.
	 * 
	 * @param name Player name to check 
	 * @param ip Ip address to check
	 * @return Ban cause or null if not banned
	 */
	public String isBanned(String name, String ip){
		String ret = null;
		if(banListName.keyExists(name)){
			ret = banListName.getString(name, plugin.BLMessage);
		}else if(banListIp.keyExists(ip)){
			ret = banListIp.getString(ip, plugin.BLMessage);
		}

		return ret;
	}
}
