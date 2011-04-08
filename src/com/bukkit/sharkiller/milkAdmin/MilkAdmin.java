package com.bukkit.sharkiller.milkAdmin;

import java.io.*;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;

import com.bukkit.sharkiller.milkAdmin.McRKit.RTKInterface;
import com.bukkit.sharkiller.milkAdmin.McRKit.RTKInterfaceException;
import com.bukkit.sharkiller.milkAdmin.McRKit.RTKListener;

public class MilkAdmin extends org.bukkit.plugin.java.JavaPlugin implements RTKListener{
	private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
	private final MilkAdminPlayerListener playerListener = new MilkAdminPlayerListener(this);
	RTKInterface api = null;
	Configuration Settings = new Configuration(new File("milkAdmin/settings.yml"));

	public MilkAdmin(org.bukkit.plugin.PluginLoader pluginLoader, org.bukkit.Server serverInstance, org.bukkit.plugin.PluginDescriptionFile descFile, java.io.File folder, java.io.File pluginFile, java.lang.ClassLoader cLoader) {

	}
	public MilkAdmin() {

	}
	public void setup() {
		try{
			new File("milkAdmin").mkdir();
			new File("milkAdmin", "banlist.ini").createNewFile();
			new File("milkAdmin", "settings.yml").createNewFile();
		} catch (IOException ex) {
			System.out.println("Could not create milkAdmin files.");
		}
		eraseLoggedIn();
		try{
			Settings.load();
			String username = Settings.getString("RTK.Username", "user");
			String password = Settings.getString("RTK.Password", "pass");
			int port = Settings.getInt("RTK.Port", 25561);
			api = RTKInterface.createRTKInterface(port,"localhost",username,password);
		}catch(RTKInterfaceException e){
			e.printStackTrace();
		}
		api.registerRTKListener(this);
	}

	public void onRTKStringReceived(String s){
		System.out.println("From wrapper: "+s);
	}

	public void eraseLoggedIn(){
		File myFile = new File("milkAdmin/loggedin.ini");
		myFile.delete();
		try{
			new File("milkAdmin/loggedin.ini").createNewFile();
		} catch (IOException ex) {
			System.out.println("Could not create milkAdmin files!");
		}
	}

	@SuppressWarnings("unused")
	public void onEnable() {
		Logger logger = Logger.getLogger("Minecraft");
		setup();
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);

		PluginDescriptionFile pdfFile = this.getDescription();
		System.out.println("[milkAdmin] v"+pdfFile.getVersion()+" is enabled!" );
		System.out.println("[milkAdmin] Developed by: "+pdfFile.getAuthors());
		new WebServer(this);
	}

	public void onDisable() {
		System.out.println("[milkAdmin] Disabled!");
	}

	public boolean isDebugging(final Player player) {
		if (debugees.containsKey(player)) {
			return debugees.get(player);
		} else {
			return false;
		}
	}

	public void setDebugging(final Player player, final boolean value) {
		debugees.put(player, value);
	}
}