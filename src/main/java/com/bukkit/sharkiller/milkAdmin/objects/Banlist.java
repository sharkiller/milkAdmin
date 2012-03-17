package com.bukkit.sharkiller.milkAdmin.objects;

import java.io.IOException;

import com.bukkit.sharkiller.milkAdmin.MilkAdmin;
import com.bukkit.sharkiller.milkAdmin.util.PropertiesFile;



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
		banListName = new PropertiesFile(plugin.BLDir+"/banlistname.ini");
		banListIp = new PropertiesFile(plugin.BLDir+"/banlistip.ini");
		
		try {
			banListName.load();
			banListIp.load();
		} catch (IOException e) {
			System.err.println("[milkAdmin] Could not load banlist files.");
		}
	}
	
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
