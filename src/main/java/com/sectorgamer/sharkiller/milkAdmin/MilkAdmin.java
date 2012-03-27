package com.sectorgamer.sharkiller.milkAdmin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

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
import com.sectorgamer.sharkiller.milkAdmin.listeners.BanlistListener;
import com.sectorgamer.sharkiller.milkAdmin.listeners.WhitelistListener;
import com.sectorgamer.sharkiller.milkAdmin.objects.Banlist;
import com.sectorgamer.sharkiller.milkAdmin.objects.Whitelist;
import com.sectorgamer.sharkiller.milkAdmin.rtk.RTKInterface;
import com.sectorgamer.sharkiller.milkAdmin.rtk.RTKInterfaceException;
import com.sectorgamer.sharkiller.milkAdmin.rtk.RTKListener;
import com.sectorgamer.sharkiller.milkAdmin.util.FileMgmt;
import com.sectorgamer.sharkiller.milkAdmin.util.MilkAdminLog;
import com.sectorgamer.sharkiller.milkAdmin.util.PropertiesFile;

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
	/* RTK variables */
	boolean UsingRTK;
	RTKInterface api = null;
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

	public MilkAdmin(org.bukkit.plugin.PluginLoader pluginLoader, org.bukkit.Server serverInstance, org.bukkit.plugin.PluginDescriptionFile descFile, java.io.File folder, java.io.File pluginFile, java.lang.ClassLoader cLoader) {

	}
	public MilkAdmin() {

	}
	public boolean setup() {
		try{
			/* Check and Create plugin folder */
			File dir = new File(PluginDir);
			if(!dir.exists()){
				dir.mkdirs();
			}
			
			/* Check and Create web files folder */
			dir = new File(PluginDir+ File.separator + "html");
			if(!dir.exists()){
				MilkAdminLog.info("Copying default HTML ZIP...");
				dir = new File(PluginDir + File.separator + "milkAdmin.zip");
				FileMgmt.copy(getResource("milkAdmin.zip"), dir);
				MilkAdminLog.info("Done! Unzipping...");
				FileMgmt.unziptodir(dir, new File(PluginDir));
				MilkAdminLog.info("Done! Deleting zip.");
				dir.delete();
			}
			
			/* Check and Copy default config file */
			dir = new File(PluginDir+ File.separator + "settings.yml");
			if(!dir.exists()){
				MilkAdminLog.info("Copying default settings.yml file.");
				FileMgmt.copy(getResource("settings.yml"), dir);
			}
			/* Init config */
			Settings = YamlConfiguration.loadConfiguration(dir);
			
			dir = new File(PluginDir+ File.separator + "admins.ini");
			if(!dir.exists()){
				MilkAdminLog.info("Copying default admins.ini file.");
				FileMgmt.copy(getResource("admins.ini"), dir);
			}
			/* Init loggedin system */
			eraseLoggedIn();

			ServerProperties.load();
			BLDir = Settings.getString("Settings.BanListDir", "plugins" + File.separator + "milkAdmin");
			BLMessage = Settings.getString("Strings.Banned", "Banned from the server!");
			UsingRTK = Settings.getBoolean("RTK.UsingRTK", false);
			WLCustom = Settings.getBoolean("Whitelist.Custom", false);
			boolean MCWhitelist = ServerProperties.getBoolean("white-list", false);
			OnlineMode = ServerProperties.getBoolean("online-mode", true);
			
			if(MCWhitelist && WLCustom){
				MilkAdminLog.warning("Minecraft Whitelist is actitivated. Shutting down custom Whitelist.");
				WLCustom = false;
			}else if(WLCustom){
				WL = new Whitelist();
				WLAlert = Settings.getBoolean("Whitelist.Alert", true);
				WLAlertMessage = Settings.getString("Whitelist.AlertMessage", "&6%s trying to join but is not in whitelist.");
				WLKickMessage = Settings.getString("Whitelist.KickMessage", "You are not in whitelist. Register on the forum!");
				MilkAdminLog.info("Using Custom Whitelist. ("+WL.count()+" users)");
			}
			
			boolean perm = Settings.getBoolean("Settings.UsingPermissions", true);
	    	if(perm)
	    		enablePermissions();
	    	else
	    		MilkAdminLog.warning("No permission system enabled!");
			
			BL = new Banlist(this);
			
			if(UsingRTK){
				userRTK = Settings.getString("RTK.Username", "user");
				passRTK = Settings.getString("RTK.Password", "pass");
				portRTK = Settings.getInt("RTK.Port", 25000);
				api = RTKInterface.createRTKInterface(portRTK,"localhost",userRTK,passRTK);
				api.registerRTKListener(this);
			}else{
				MilkAdminLog.warning("Not using RTK. Required to Start/Stop/Restart/Backup.");
			}
		}catch(IOException e){
			MilkAdminLog.severe("Could not create milkAdmin files.");
			return false;
		}catch(RTKInterfaceException e){
			MilkAdminLog.severe("Could not create RTK Interface.");
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
			/* Init listeners */
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvents(new BanlistListener(this), this);
			if(WLCustom)
				pm.registerEvents(new WhitelistListener(this), this);
			/* Welcome messages */
			PluginDescriptionFile pdfFile = this.getDescription();
			MilkAdminLog.info("v"+pdfFile.getVersion()+" is enabled!" );
			MilkAdminLog.info("Developed by: "+pdfFile.getAuthors());
			/* Init webserver class */
			server = new WebServer(this);
		}else{
			MilkAdminLog.severe("Failed to initialized!");
		}
	}
	
	@Override
	public void onDisable() {
		try{
			/* Stop webserver */
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
	    				sender.sendMessage("§2[milkAdmin] §4Whitelist default importada.");
	    			}else if(args[1].equalsIgnoreCase("reload")){
	    				WL.reload();
	    				sender.sendMessage("§2[milkAdmin] §4Whitelist recargada.");
	    			}
	    		}else if(args.length == 3){
	    			if(args[1].equalsIgnoreCase("add")){
	    				String res = WL.addDefaultPlayer(args[2]);
	    				if(res == "ok")
	    					sender.sendMessage("§6[milkAdmin] §a"+args[2]+" fue agregado a la whitelist.");
	    				else
	    					sender.sendMessage("§6[milkAdmin] §a"+res);
	    			}else if(args[1].equalsIgnoreCase("remove")){
	    				String res = WL.removePlayer(args[2]);
	    				if(res == "ok")
	    					sender.sendMessage("§6[milkAdmin] §a"+args[2]+" fue sacado de la whitelist.");
	    				else
	    					sender.sendMessage("§6[milkAdmin] §a"+res);
	    			}
	    		}
    		}else
    			sender.sendMessage("§4La custom whitelist no está actualizada.");
    	}else{
    		sender.sendMessage("§4No tienes acceso a ese comando.");
    	}
    }
}