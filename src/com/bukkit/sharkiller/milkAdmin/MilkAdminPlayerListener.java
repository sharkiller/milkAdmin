package com.bukkit.sharkiller.milkAdmin;

import java.io.File;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.util.config.Configuration;

import com.bukkit.sharkiller.milkAdmin.Utils.PropertiesFile;
/**
 * Handle events for all Player related events
 * @author Snowl
 */
public class MilkAdminPlayerListener extends PlayerListener {
	//private final milkBukkit plugin;
	String BanListDir, PluginDir = "plugins/milkAdmin";
	Configuration Settings = new Configuration(new File(PluginDir+"settings.yml"));
	String BannedString = Settings.getString("Strings.Banned", "Banned from this server");

	public MilkAdminPlayerListener(MilkAdmin instance) {
		//plugin = instance;
		Settings.load();
		BanListDir = Settings.getString("Settings.BanListDir", "plugins/milkAdmin");
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		PropertiesFile banListName = new PropertiesFile(BanListDir+"/banlistname.ini");
		PropertiesFile banListIp = new PropertiesFile(BanListDir+"/banlistip.ini");
		try {
			banListName.load();
			banListIp.load();
		} catch (IOException ioe) {}
		
		Player player = event.getPlayer();
		String pName = player.getName();
		String pIp = player.getAddress().getAddress().getHostAddress();
		
		if(banListName.keyExists(pName)){
			String PlayerBanned = banListName.getString(pName, BannedString);
			player.kickPlayer(PlayerBanned);
		}else if(banListIp.keyExists(pIp)){
			String PlayerBannedIP = banListIp.getString(pIp, BannedString);
			player.kickPlayer(PlayerBannedIP);
		}
	}
}
