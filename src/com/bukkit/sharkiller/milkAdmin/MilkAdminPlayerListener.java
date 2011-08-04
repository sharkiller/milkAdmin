package com.bukkit.sharkiller.milkAdmin;

import java.io.File;
import java.io.IOException;

//import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;
//import org.bukkit.event.player.PlayerLoginEvent;
//import org.bukkit.event.player.PlayerLoginEvent.Result;
//import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.util.config.Configuration;

import com.bukkit.sharkiller.milkAdmin.Utils.PropertiesFile;
/**
 * Handle events for Ban System.
 * @author Sharkiller
 */
public class MilkAdminPlayerListener extends PlayerListener {
	String BanListDir, PluginDir = "plugins/milkAdmin";
	Configuration Settings = new Configuration(new File(PluginDir+"settings.yml"));
	String BannedString = Settings.getString("Strings.Banned", "Banned from this server");

	public MilkAdminPlayerListener() {
		Settings.load();
		BanListDir = Settings.getString("Settings.BanListDir", "plugins/milkAdmin");
	}
	
	@Override
	public void onPlayerPreLogin(PlayerPreLoginEvent event) {
		PropertiesFile banListName = new PropertiesFile(BanListDir+"/banlistname.ini");
		PropertiesFile banListIp = new PropertiesFile(BanListDir+"/banlistip.ini");
		try {
			banListName.load();
			banListIp.load();
		} catch (IOException ioe) {}
		
		String pName = event.getName();
		String pIp = event.getAddress().getHostAddress();
		
		if(banListName.keyExists(pName)){
			String Cause = banListName.getString(pName, BannedString);
			event.disallow(Result.KICK_OTHER, Cause);
		}else if(banListIp.keyExists(pIp)){
			String Cause = banListIp.getString(pIp, BannedString);
			event.disallow(Result.KICK_OTHER, Cause);
		}
	}
	
}
