package com.bukkit.sharkiller.milkAdmin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;

import com.bukkit.sharkiller.milkAdmin.McRKit.RTKInterface;
import com.bukkit.sharkiller.milkAdmin.McRKit.RTKInterfaceException;
import com.bukkit.sharkiller.milkAdmin.McRKit.RTKListener;
import com.bukkit.sharkiller.milkAdmin.WebServer;

public class MilkAdmin extends org.bukkit.plugin.java.JavaPlugin implements RTKListener{
	private final MilkAdminPlayerListener playerListener = new MilkAdminPlayerListener(this);
	public static String initTime = "";
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
				System.err.println("[milkAdmin] milkAdmin folder not found. Wrong installation!");
				return false;
			}
			if(!new File(PluginDir+"/html").exists()){
				System.err.println("[milkAdmin] html folder not found. Wrong installation!");
				return false;
			}
			new File(PluginDir, "banlistip.ini").createNewFile();
			new File(PluginDir, "banlistname.ini").createNewFile();
			if(!new File(PluginDir, "admins.ini").exists()){
				System.err.println("[milkAdmin] admins.ini not found.");
				new File(PluginDir, "admins.ini").createNewFile();
				PropertiesFile Admin = new PropertiesFile(PluginDir+"/admins.ini");
				Admin.load();
				Admin.setString("admin", "c7ad44cbad762a5da0a452f9e854fdc1e0e7a52a38015f23f3eab1d80b931dd472634dfac71cd34ebc35d16ab7fb8a90c81f975113d6c7538dc69dd8de9077ec");
				System.out.println("[milkAdmin] Default admins.ini created successfully.");
			}
			if(!new File(PluginDir, "settings.yml").exists()){
				new File(PluginDir, "settings.yml").createNewFile();
				System.err.println("[milkAdmin] settings.yml not found. Using default settings.");
			}
		} catch (IOException ex) {
			System.err.println("[milkAdmin] Could not create milkAdmin files.");
			return false;
		}
		eraseLoggedIn();
		try{
			Settings.load();
			userRTK = Settings.getString("RTK.Username", "user");
			passRTK = Settings.getString("RTK.Password", "pass");
			portRTK = Settings.getInt("RTK.Port", 25000);
			api = RTKInterface.createRTKInterface(portRTK,"localhost",userRTK,passRTK);
		}catch(RTKInterfaceException e){
			e.printStackTrace();
			return false;
		}
		api.registerRTKListener(this);
		return true;
	}

	public void onRTKStringReceived(String s){
		if(s.equals("RTK_TIMEOUT")){
			System.out.println("[milkAdmin] RTK not response to the user '"+userRTK+"' in the port '"+portRTK+"' bad configuration?");
		} else
			System.out.println("[milkAdmin] From RTK: "+s);
	}

	public void eraseLoggedIn(){
		File myFile = new File(PluginDir+"/loggedin.ini");
		myFile.delete();
		try{
			new File(PluginDir+"/loggedin.ini").createNewFile();
		} catch (IOException ex) {
			System.out.println("Could not create milkAdmin files!");
		}
	}

	public void onEnable() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	    initTime = sdf.format(cal.getTime());
		boolean init = setup();
		if(init){
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.High, this);

			PluginDescriptionFile pdfFile = this.getDescription();
			System.out.println("[milkAdmin] v"+pdfFile.getVersion()+" is enabled!" );
			System.out.println("[milkAdmin] Developed by: "+pdfFile.getAuthors());
			server = new WebServer(this);
		}else{
			System.err.println("[milkAdmin] Not initialized!");
		}
	}

	public void onDisable() {
		try{
			if(server != null){
				server.stopServer();
				System.out.println("[milkAdmin] WebServer closed successfully!");
				System.out.println("[milkAdmin] Disabled!");
			}
		}catch(IOException e){
			System.err.println("[milkAdmin] Error closing WebServer");
			e.printStackTrace();
		}
	}
}