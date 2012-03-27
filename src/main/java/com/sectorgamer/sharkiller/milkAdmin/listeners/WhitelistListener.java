package com.sectorgamer.sharkiller.milkAdmin.listeners;


import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import com.sectorgamer.sharkiller.milkAdmin.MilkAdmin;


/**
 * Handle events for White list System.
 * @author Sharkiller
 */
public class WhitelistListener implements Listener {
	static final String PluginDir = "plugins/milkAdmin";
	MilkAdmin plugin;

	public WhitelistListener(MilkAdmin plugin){
		this.plugin = plugin;
	}
	
	/**
	 * Handle events for White list System.
	 * @author Sharkiller
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void whitelistCheckLogin(PlayerLoginEvent event) {
		if (!event.getResult().equals(Result.ALLOWED))
			return;
		
		String pName = event.getPlayer().getName();
		String Alert;
		
		try{
			if(plugin.WLAlert)
				Alert = String.format(plugin.WLAlertMessage, pName);
			else
				Alert = null;
		}catch(Exception e){
			Alert = plugin.WLAlertMessage;
		}
		
		if(!plugin.WL.inWhitelist(pName)){
			event.disallow(Result.KICK_OTHER, plugin.WLKickMessage);
			if(Alert != null)
				plugin.getServer().broadcastMessage(Alert);
		}else
			plugin.WL.updateLastLogin(pName);
	}
}