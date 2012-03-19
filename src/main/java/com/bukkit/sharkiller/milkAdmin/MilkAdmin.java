package com.bukkit.sharkiller.milkAdmin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.YamlConfiguration;

import com.bukkit.sharkiller.milkAdmin.WebServer;
import com.bukkit.sharkiller.milkAdmin.listeners.BanlistListener;
import com.bukkit.sharkiller.milkAdmin.listeners.WhitelistListener;
import com.bukkit.sharkiller.milkAdmin.objects.Banlist;
import com.bukkit.sharkiller.milkAdmin.objects.Whitelist;
import com.bukkit.sharkiller.milkAdmin.rtk.RTKInterface;
import com.bukkit.sharkiller.milkAdmin.rtk.RTKInterfaceException;
import com.bukkit.sharkiller.milkAdmin.rtk.RTKListener;
import com.bukkit.sharkiller.milkAdmin.util.PropertiesFile;

import net.milkbowl.vault.permission.Permission;


public class MilkAdmin extends JavaPlugin implements RTKListener{
	static Logger Log = Logger.getLogger("Minecraft");
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
	String PluginDir = "plugins/milkAdmin";
	String userRTK, passRTK;
	int portRTK;
	/* Server variables */
	PropertiesFile ServerProperties = new PropertiesFile("server.properties");
	YamlConfiguration Settings = YamlConfiguration.loadConfiguration(new File(PluginDir+"/settings.yml"));
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

			ServerProperties.load();
			BLDir = Settings.getString("Settings.BanListDir", "plugins/milkAdmin");
			BLMessage = Settings.getString("Strings.Banned", "Banned from the server!");
			UsingRTK = Settings.getBoolean("RTK.UsingRTK", false);
			WLCustom = Settings.getBoolean("Whitelist.Custom", false);
			boolean MCWhitelist = ServerProperties.getBoolean("white-list", false);
			OnlineMode = ServerProperties.getBoolean("online-mode", true);
			
			if(MCWhitelist && WLCustom){
				Log.warning("[milkAdmin] Minecraft Whitelist is actitivated. Shutting down custom Whitelist.");
				WLCustom = false;
			}else if(WLCustom){
				WL = new Whitelist();
				WLAlert = Settings.getBoolean("Whitelist.Alert", true);
				WLAlertMessage = Settings.getString("Whitelist.AlertMessage", "&6%s trying to join but is not in whitelist.");
				WLKickMessage = Settings.getString("Whitelist.KickMessage", "You are not in whitelist. Register on the forum!");
				Log.info("[milkAdmin] Using Custom Whitelist.");
			}
			
			boolean perm = Settings.getBoolean("Settings.UsingPermissions", true);
	    	if(perm)
	    		enablePermissions();
	    	else
	    		Log.warning("[milkAdmin] No permission system enabled!");
			
			if(!new File(BLDir, "banlistip.ini").exists() || !new File(BLDir, "banlistname.ini").exists()){
				Log.warning("[milkAdmin] Banlist files not found in '"+BLDir+"'. Creating...");
				new File(BLDir, "banlistip.ini").createNewFile();
				new File(BLDir, "banlistname.ini").createNewFile();
				Log.info("[milkAdmin] Banlist files created successfully.");
			}
			BL = new Banlist(this);
			
			if(UsingRTK){
				userRTK = Settings.getString("RTK.Username", "user");
				passRTK = Settings.getString("RTK.Password", "pass");
				portRTK = Settings.getInt("RTK.Port", 25000);
				api = RTKInterface.createRTKInterface(portRTK,"localhost",userRTK,passRTK);
				api.registerRTKListener(this);
			}else{
				Log.warning("[milkAdmin] Not using RTK. Required to Start/Stop/Restart/Backup.");
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
	
	public void enablePermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            Permissions = permissionProvider.getProvider();
            permissionsEnabled = true;
		    Log.info("[milkAdmin] Permission support enabled!");
        } else
			Log.warning("[milkAdmin] Permission system not found!");
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
			pm.registerEvents(new BanlistListener(this), this);
			if(WLCustom)
				pm.registerEvents(new WhitelistListener(this), this);

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
	
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
    	String commandName = cmd.getName();
    	
    	if(commandName.equalsIgnoreCase("milkadmin"))
    	{
    		try {
    			if(args[0].equalsIgnoreCase("ayuda") || args[0].equalsIgnoreCase("?"))
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