package com.sectorgamer.sharkiller.milkAdmin.listeners;


import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import com.sectorgamer.sharkiller.milkAdmin.MilkAdmin;

/**
 * Handle events for Ban list System.
 * @author Sharkiller
 */
public class BanlistListener implements Listener {
	private MilkAdmin plugin;

	public BanlistListener(MilkAdmin i) {
		this.plugin = i;
	}
	
	/**
	 * Handle events for Banlist System.
	 * @author Sharkiller
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void banCheckLogin(PlayerLoginEvent event) {
		if (!event.getResult().equals(Result.ALLOWED))
			return;
		
		String pName = event.getPlayer().getName();
		String pIp = "";
		
		if (event.getResult().equals(Result.ALLOWED)) {
			pIp = event.getKickMessage();
        }
		
		String Cause = plugin.BL.isBanned(pName, pIp);
		
		if(Cause != null){
			event.disallow(Result.KICK_OTHER, Cause);
		}
	}
}
