package com.bukkit.sharkiller.milkAdmin;

import java.io.File;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.util.config.Configuration;
/**
 * Handle events for all Player related events
 * @author Snowl
 */
public class MilkAdminPlayerListener extends PlayerListener {
	//private final milkBukkit plugin;
	Configuration Settings = new Configuration(new File("milkAdmin/settings.yml"));
	PropertiesFile banListName = new PropertiesFile("milkAdmin/banlistname.ini");
	PropertiesFile banListIp = new PropertiesFile("milkAdmin/banlistip.ini");
	String BannedString = Settings.getString("Strings.Banned", "Banned from this server");

	public MilkAdminPlayerListener(MilkAdmin instance) {
		//plugin = instance;
		Settings.load();
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
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
