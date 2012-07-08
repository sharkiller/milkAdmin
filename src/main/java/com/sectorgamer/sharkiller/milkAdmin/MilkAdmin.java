package com.sectorgamer.sharkiller.milkAdmin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.YamlConfiguration;

import com.sectorgamer.sharkiller.milkAdmin.WebServer;
import com.sectorgamer.sharkiller.milkAdmin.listeners.*;
import com.sectorgamer.sharkiller.milkAdmin.objects.*;
import com.sectorgamer.sharkiller.milkAdmin.rtk.*;
import com.sectorgamer.sharkiller.milkAdmin.util.*;

import net.milkbowl.vault.permission.Permission;

public class MilkAdmin extends JavaPlugin implements RTKListener{
	/* Ban list variables */
	public Banlist BL;
	public String BLDir;
	public String BLMessage;
	/* White list variables */
	public Whitelist WL;
	public boolean WLCustom = false;
	public boolean WLAlert = false;
	public String WLAlertMessage;
	public String WLKickMessage;
	public ArrayList<String> kickedPlayers = new ArrayList<String>();
	/* PluginUpdates variables */
	public PluginUpdates PU;
	/* RTK variables */
	boolean UsingRTK;
	RTKInterface RTKapi = null;
	String userRTK, passRTK;
	int portRTK;
	/* Server variables */
	String PluginDir = "plugins" + File.separator + "milkAdmin";
	PropertiesFile ServerProperties = new PropertiesFile("server.properties");
	YamlConfiguration Settings = null;
	private WebServer server = null;
	public static String initTime = "";
	public boolean OnlineMode = true;
	public static Permission Permissions = null;
    private boolean permissionsEnabled = false;

	public boolean setup() {
		try{
			File dir;
			/* Check and Create plugin folder */
			dir = new File(PluginDir);
			if(!dir.exists()) dir.mkdirs();
			
			/* Check and Copy web files folder */
			dir = new File(PluginDir+ File.separator + "html");
			if(!dir.exists()){
				MilkAdminLog.info("Copying default HTML ZIP...");
				dir = new File(PluginDir + File.separator + "milkAdmin.zip");
				FileMgmt.copy(getResource("milkAdmin.zip"), dir);
				MilkAdminLog.info("Done! Unzipping...");
				if(FileMgmt.unziptodir(dir, new File(PluginDir))){
					MilkAdminLog.info("Done! Deleting zip.");
					dir.deleteOnExit();
				} else
					MilkAdminLog.info("Unzip failed!");
				
			}
			
			/* Check and Copy default config files */
			dir = new File(PluginDir+ File.separator + "settings.yml");
			if(!dir.exists()){
				MilkAdminLog.info("Copying default settings.yml file.");
				FileMgmt.copy(getResource("settings.yml"), dir);
			}
			
			/* Init configs */
			Settings = YamlConfiguration.loadConfiguration(dir);
			ServerProperties.load();
			
			dir = new File(PluginDir+ File.separator + "admins.ini");
			if(!dir.exists()){
				MilkAdminLog.info("Copying default admins.ini file.");
				FileMgmt.copy(getResource("admins.ini"), dir);
			}
			
			/* Init loggedin system */
			eraseLoggedIn();
			
			/* Get configs */
			BLDir = Settings.getString("Settings.BanListDir", "plugins" + File.separator + "milkAdmin");
			BLMessage = Settings.getString("Strings.Banned", "Banned from the server!");
			UsingRTK = Settings.getBoolean("RTK.UsingRTK", false);
			WLCustom = Settings.getBoolean("Whitelist.Custom", false);
			boolean MCWhitelist = ServerProperties.getBoolean("white-list", false);
			OnlineMode = ServerProperties.getBoolean("online-mode", true);
			
			/* Setup permissions */
			boolean perm = Settings.getBoolean("Settings.UsingPermissions", true);
	    	if(perm)
	    		enablePermissions();
	    	else
	    		MilkAdminLog.warning("No permission system enabled!");
	    	
	    	/* Init whitelist */
	    	if(MCWhitelist && WLCustom){
				MilkAdminLog.warning("Minecraft Whitelist is actitivated. Shutting down custom Whitelist.");
				WLCustom = false;
			}else if(WLCustom){
				WL = new Whitelist();
				WLAlert = Settings.getBoolean("Whitelist.Alert", true);
				WLAlertMessage = Settings.getString("Whitelist.AlertMessage", "&6%s trying to join but is not in whitelist.");
				WLKickMessage = Settings.getString("Whitelist.KickMessage", "You are not in whitelist. Register on the forum!");
				MilkAdminLog.info("Using Custom Whitelist ("+WL.count()+" users)");
			}
			
	    	/* Init banlist */
			BL = new Banlist(this);
			MilkAdminLog.info("Banlist Files Loaded");
			MilkAdminLog.info("Banned Nicknames: ("+BL.count(true)+" nicks)");
			MilkAdminLog.info("Banned IPs: ("+BL.count(false)+" IPs)");
			
			/* Init RTK Listener */
			if(UsingRTK){
				userRTK = Settings.getString("RTK.Username", "user");
				passRTK = Settings.getString("RTK.Password", "pass");
				portRTK = Settings.getInt("RTK.Port", 25000);
				RTKapi = RTKInterface.createRTKInterface(portRTK,"localhost",userRTK,passRTK);
				RTKapi.registerRTKListener(this);
			}else{
				MilkAdminLog.warning("Not using RTK. Required to Start/Stop/Restart/Backup.");
			}
		}catch(IOException e){
			MilkAdminLog.severe("Could not create milkAdmin files.", e);
			return false;
		}catch(RTKInterfaceException e){
			MilkAdminLog.severe("Could not create RTK Interface.");
			return false;
		}catch(Exception e){
			MilkAdminLog.severe("Something went wrong!", e);
			return false;
		}
		return true;
	}
	
	public void enablePermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            Permissions = permissionProvider.getProvider();
            permissionsEnabled = true;
            MilkAdminLog.info("Permission support enabled!");
        } else
        	MilkAdminLog.warning("Permission system not found!");
    }

	public void onRTKStringReceived(String s){
		if(s.equals("RTK_TIMEOUT")){
			MilkAdminLog.warning("RTK not response to the user '"+userRTK+"' in the port '"+portRTK+"' bad configuration?");
		} else
			MilkAdminLog.info("From RTK: "+s);
	}

	public void eraseLoggedIn(){
		try{
			File loggedin = new File(PluginDir+ File.separator + "loggedin.ini");
			loggedin.delete();
			loggedin.createNewFile();
		} catch (IOException ex) {
			MilkAdminLog.severe("Failed to create loggedin.ini file.", ex);
		}
	}
	
	@Override
	public void onEnable() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	    initTime = sdf.format(cal.getTime());
	    /* Init configs */
		boolean init = setup();
		if(init){
			/* Start listeners */
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvents(new BanlistListener(this), this);
			if(WLCustom)
				pm.registerEvents(new WhitelistListener(this), this);
			/* Welcome messages */
			PluginDescriptionFile pdfFile = this.getDescription();
			MilkAdminLog.info("v"+pdfFile.getVersion()+" is enabled!" );
			MilkAdminLog.info("Developed by: "+pdfFile.getAuthors());
			/* Init PluginUpdates */
			PU = new PluginUpdates(pm);
			/* Init web server class */
			server = new WebServer(this);
		}else{
			MilkAdminLog.severe("Failed to initialized!");
		}
	}
	
	@Override
	public void onDisable() {
		try{
			/* Stop web server */
			if(server != null)
				server.stopServer();
			MilkAdminLog.info("milkAdmin disabled successfully!");
		}catch(IOException e){
			MilkAdminLog.severe("Error closing WebServer", e);
		}
	}
	
	
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
    	String commandName = cmd.getName();
    	
    	if(commandName.equalsIgnoreCase("milkadmin")) {
    		try {
    			if(args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?"))
    				sender.sendMessage("=D");
    			else if(args[0].equalsIgnoreCase("whitelist") || args[0].equalsIgnoreCase("wl"))
    				whitelistProccess(sender, args);
    			else if(args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("b"))
    				sender.sendMessage("=D");
    			return true;
    		} catch(ArrayIndexOutOfBoundsException ex) {
    			return false;
    		}
    	}
    	return false;
    }
    
    public void whitelistProccess(CommandSender sender, String[] args){
    	if(sender instanceof ConsoleCommandSender || !(permissionsEnabled) || Permissions.has((Player)sender,"milkadmin.whitelist")) {
    		if(WLCustom){
	    		if(args.length == 2){
	    			if(args[1].equalsIgnoreCase("importdefault") || args[1].equalsIgnoreCase("impdef")){
	    				WL.importDefault();
	    				sender.sendMessage(ChatColor.GOLD+"[milkAdmin] "+ChatColor.GREEN+"Default Whitelist imported.");
	    			}else if(args[1].equalsIgnoreCase("reload")){
	    				WL.reload();
	    				sender.sendMessage(ChatColor.GOLD+"[milkAdmin] "+ChatColor.GREEN+"Whitelist reloaded.");
	    			}
	    		}else if(args.length == 3){
	    			if(args[1].equalsIgnoreCase("add")){
	    				String res = WL.addDefaultPlayer(args[2]);
	    				if(res == "ok")
	    					sender.sendMessage(ChatColor.GOLD+"[milkAdmin] "+ChatColor.GREEN+args[2]+" fue agregado a la whitelist.");
	    				else
	    					sender.sendMessage(ChatColor.GOLD+"[milkAdmin] "+ChatColor.GREEN+res);
	    			}else if(args[1].equalsIgnoreCase("remove")){
	    				String res = WL.removePlayer(args[2]);
	    				if(res == "ok")
	    					sender.sendMessage(ChatColor.GOLD+"[milkAdmin] "+ChatColor.GREEN+args[2]+" fue sacado de la whitelist.");
	    				else
	    					sender.sendMessage(ChatColor.GOLD+"[milkAdmin] "+ChatColor.GREEN+res);
	    			}
	    		}
    		}else
    			sender.sendMessage(ChatColor.RED+"The custom whitelist is not activated.");
    	}else{
    		sender.sendMessage(ChatColor.RED+"You do not have access to this command.");
    	}
    }
}