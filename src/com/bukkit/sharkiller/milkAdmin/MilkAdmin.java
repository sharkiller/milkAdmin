package com.bukkit.sharkiller.milkAdmin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;

import com.bukkit.sharkiller.milkAdmin.RTK.RTKInterface;
import com.bukkit.sharkiller.milkAdmin.RTK.RTKInterfaceException;
import com.bukkit.sharkiller.milkAdmin.RTK.RTKListener;
import com.bukkit.sharkiller.milkAdmin.Utils.PropertiesFile;
import com.bukkit.sharkiller.milkAdmin.WebServer;

public class MilkAdmin extends JavaPlugin implements RTKListener{
	static Logger Log = Logger.getLogger("Minecraft");
	private final MilkAdminPlayerListener playerListener = new MilkAdminPlayerListener();
	public static String initTime = "";
	boolean UsingRTK;
	RTKInterface api = null;
	String PluginDir = "plugins/milkAdmin";
	String userRTK, passRTK;
	int portRTK;
	Configuration Settings = new Configuration(new File(PluginDir+"/settings.yml"));
	private WebServer server = null;

	public MilkAdmin(org.bukkit.plugin.PluginLoader pluginLoader, org.bukkit.Server serverInstance, org.bukkit.plugin.PluginDescriptionFile descFile, java.io.File folder, java.io.File pluginFile, java.lang.ClassLoader cLoader) {

	}
	public MilkAdmin() {

	}
	public boolean setup() {
		try{
			if(!new File(PluginDir).exists()){
				Log.warning("[milkAdmin] milkAdmin folder not found. Wrong installation!");
				return false;
			}
			if(!new File(PluginDir+"/html").exists()){
				Log.warning("[milkAdmin] html folder not found. Wrong installation!");
				return false;
			}
			if(!new File(PluginDir, "settings.yml").exists()){
				new File(PluginDir, "settings.yml").createNewFile();
				Log.warning("[milkAdmin] settings.yml not found. Using default settings.");
			}
			
			if(!new File(PluginDir, "admins.ini").exists()){
				Log.warning("[milkAdmin] admins.ini not found.");
				new File(PluginDir, "admins.ini").createNewFile();
				PropertiesFile Admin = new PropertiesFile(PluginDir+"/admins.ini");
				Admin.load();
				Admin.setString("admin", "c7ad44cbad762a5da0a452f9e854fdc1e0e7a52a38015f23f3eab1d80b931dd472634dfac71cd34ebc35d16ab7fb8a90c81f975113d6c7538dc69dd8de9077ec");
				Log.info("[milkAdmin] Default admins.ini created successfully.");
			}

			eraseLoggedIn();

			Settings.load();
			String BanListDir = Settings.getString("Settings.BanListDir", "plugins/milkAdmin");
			UsingRTK = Settings.getBoolean("RTK.UsingRTK", false);
			
			if(!new File(BanListDir, "banlistip.ini").exists() || !new File(BanListDir, "banlistname.ini").exists()){
				Log.warning("[milkAdmin] Banlist files not found in '"+BanListDir+"'. Creating...");
				new File(BanListDir, "banlistip.ini").createNewFile();
				new File(BanListDir, "banlistname.ini").createNewFile();
				Log.info("[milkAdmin] Banlist files created successfully.");
			}
			
			if(UsingRTK){
				userRTK = Settings.getString("RTK.Username", "user");
				passRTK = Settings.getString("RTK.Password", "pass");
				portRTK = Settings.getInt("RTK.Port", 25000);
				api = RTKInterface.createRTKInterface(portRTK,"localhost",userRTK,passRTK);
				api.registerRTKListener(this);
			}else{
				Log.warning("[milkAdmin] Not using RTK. Required to Start/Stop/Restart.");
			}
		}catch(IOException e){
			Log.severe("[milkAdmin] Could not create milkAdmin files.");
			return false;
		}catch(RTKInterfaceException e){
			Log.severe("[milkAdmin] Could not create RTK Interface.");
			return false;
		}
		return true;
	}

	public void onRTKStringReceived(String s){
		if(s.equals("RTK_TIMEOUT")){
			Log.warning("[milkAdmin] RTK not response to the user '"+userRTK+"' in the port '"+portRTK+"' bad configuration?");
		} else
			Log.info("[milkAdmin] From RTK: "+s);
	}

	public void eraseLoggedIn(){
		File myFile = new File(PluginDir+"/loggedin.ini");
		myFile.delete();
		try{
			new File(PluginDir+"/loggedin.ini").createNewFile();
		} catch (IOException ex) {
			Log.severe("[milkAdmin] Could not create milkAdmin files!");
		}
	}

	public void onEnable() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	    initTime = sdf.format(cal.getTime());
		boolean init = setup();
		if(init){
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvent(Event.Type.PLAYER_PRELOGIN, playerListener, Priority.High, this);

			PluginDescriptionFile pdfFile = this.getDescription();
			Log.info("[milkAdmin] v"+pdfFile.getVersion()+" is enabled!" );
			Log.info("[milkAdmin] Developed by: "+pdfFile.getAuthors());
			server = new WebServer(this);
		}else{
			Log.severe("[milkAdmin] Failed to initialized!");
		}
	}

	public void onDisable() {
		try{
			if(server != null)
				server.stopServer();
			Log.info("[milkAdmin] milkAdmin disabled successfully!");
		}catch(IOException e){
			Log.severe("[milkAdmin] Error closing WebServer");
			e.printStackTrace();
		}
	}
}