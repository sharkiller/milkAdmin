package com.bukkit.sharkiller.milkAdmin;

import java.io.File;
import java.io.IOException;

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
	PropertiesFile banList = new PropertiesFile("milkAdmin/banlist.ini");
	String BannedString = Settings.getString("Strings.Banned", "Banned from this server");

	public MilkAdminPlayerListener(MilkAdmin instance) {
		//plugin = instance;
		Settings.load();
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		try {
			banList.load();
		} catch (IOException ioe) {}
		String PlayerBanned = banList.getString(event.getPlayer().getName(), "false");
		String PlayerBannedIP = banList.getString(event.getPlayer().getAddress().getAddress().getHostAddress(), "false");
		if(PlayerBannedIP.contentEquals("true") || PlayerBanned.contentEquals("true")){
			event.getPlayer().kickPlayer(BannedString);
		}
	}
}
