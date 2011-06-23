package com.bukkit.sharkiller.milkAdmin;

//imports
import java.lang.reflect.Field;
import net.minecraft.server.MinecraftServer;

import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.io.*;
import java.util.regex.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;
import org.bukkit.util.config.*;

import com.bukkit.sharkiller.milkAdmin.McRKit.*;

public class WebServer extends Thread implements RTKListener{
	int WebServerMode;
	MilkAdmin milkAdminInstance;
	Socket WebServerSocket;
	ServerSocket rootSocket = null;
	static Logger log = Logger.getLogger("Minecraft");
	String Lang;
	boolean Debug;
	int Port;
	int consoleLines;
	String BannedString;
	String KickedString;
	String levelname;
	String PluginDir = "plugins/milkAdmin/";
	Configuration Settings = new Configuration(new File(PluginDir+"settings.yml"));
	PropertiesFile BukkitProperties = new PropertiesFile("server.properties");
	PropertiesFile banListName = new PropertiesFile(PluginDir+"banlistname.ini");
	PropertiesFile banListIp = new PropertiesFile(PluginDir+"banlistip.ini");
	String bannedplayers = "banned-players.txt";
	ArrayList<String> bannedPlayers = new ArrayList<String>();
	String bannedips = "banned-ips.txt";
	ArrayList<String> bannedIps = new ArrayList<String>();
	NoSavePropertiesFile adminList = new NoSavePropertiesFile(PluginDir+"admins.ini");
	PropertiesFile saveAdminList = new PropertiesFile(PluginDir+"admins.ini");
	NoSavePropertiesFile noSaveLoggedIn = new NoSavePropertiesFile(PluginDir+"loggedin.ini");
	PropertiesFile LoggedIn = new PropertiesFile(PluginDir+"loggedin.ini");

	public WebServer(MilkAdmin i){
		WebServerMode = 0;
		milkAdminInstance = i;
		start();
	}
	public WebServer(MilkAdmin i, Socket s){
		WebServerMode = 1;
		milkAdminInstance = i;
		WebServerSocket = s;
		start();
	}
	
	public void debug(String text){
		if(Debug)
			System.out.println(text);
	}

	public String readFileAsString(String filePath)
	throws java.io.IOException{
		StringBuffer fileData = new StringBuffer(1000);
		try{
			BufferedReader reader = new BufferedReader(
					new FileReader(filePath));
			char[] buf = new char[1024];
			int numRead=0;
			while((numRead=reader.read(buf)) != -1){
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
		}
		catch (Exception e) {
			debug("[milkAdmin] ERROR in readFileAsString(): " + e.getMessage());
		}
		return fileData.toString();
	}
	
	public void readFileAsBinary(String path)
	throws java.io.IOException{
		try{
			File archivo = new File(path);
			
			DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
			out.writeBytes("Content-Length: "+archivo.length()+"\r\n");
			out.writeBytes("Cache-Control: no-cache\r\n");
			out.writeBytes("Server: milkAdmin Webserver\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			
            FileInputStream file = new FileInputStream(archivo);
            byte[] fileData = new byte[8192];
            for(int i = 0; (long)i < archivo.length(); i += 8192){
                int bytesRead = file.read(fileData);
                out.write(fileData, 0, bytesRead);
            }
            file.close();
            out.close();
		}
		catch (Exception e) {
			debug("[milkAdmin] ERROR in readFileAsBinary(): " + e.getMessage());
		}
	}

	public void onRTKStringReceived(String s){
		debug("From wrapper: "+s);
	}

	// Add to main class
	public void consoleCommand(String cmd){
		CraftServer craftserver = (CraftServer)milkAdminInstance.getServer();
		Field field;
		try { field = CraftServer.class.getDeclaredField("console"); }
		catch (NoSuchFieldException ex) {return; }
		catch (SecurityException ex) {return; }
		MinecraftServer mcs;
		try { field.setAccessible(true); mcs = (MinecraftServer) field.get(craftserver); }
		catch (IllegalArgumentException ex) {return; }
		catch (IllegalAccessException ex) {return; }
		if ( (!mcs.isStopped) && (MinecraftServer.isRunning(mcs)) )
			mcs.issueCommand(cmd, mcs);
	}
	
	public String readConsole(){
		String console = ""; 
		String line = "";
		try{
			File f = new File("server.log");
			RandomAccessFile randomFile = new RandomAccessFile(f, "r");
			long numberOfLines = Long.valueOf(consoleLines).longValue();
			long fileLength = randomFile.length();
			long startPosition = fileLength - (numberOfLines * 100);
			if(startPosition < 0)
				startPosition = 0;
			randomFile.seek(startPosition);
			while( ( line = randomFile.readLine() ) != null ){
				console = console + (line + "\n");
			}
		}
		catch (Exception e) {
			debug("[milkAdmin] ERROR in readConsole(): " + e.getMessage());
		}
		return console;	
	}
	
	public String infoProperties() throws IOException{
		BukkitProperties.load();
		String ip = BukkitProperties.getString("server-ip", "");
		String port = BukkitProperties.getString("server-port", "25565");
		String maxplayers = BukkitProperties.getString("max-players", "10");
		String spawnradio = BukkitProperties.getString("spawn-protection", "16");
		String levelname = BukkitProperties.getString("level-name", "world");
		String levelseed = BukkitProperties.getString("level-seed", "");
		boolean nether = BukkitProperties.getBoolean("hellworld", false);
		boolean spawnmonsters = BukkitProperties.getBoolean("spawn-monsters", false);
		boolean spawnanimals = BukkitProperties.getBoolean("spawn-animals", false);
		boolean onlinemode = BukkitProperties.getBoolean("online-mode", false);
		boolean pvp = BukkitProperties.getBoolean("pvp", false);
		boolean flight = BukkitProperties.getBoolean("allow-flight", false);
		boolean whitelist = BukkitProperties.getBoolean("white-list", false);
		
		String json = "{\"ip\":\""+ip+"\"," +
		"\"port\":\""+port+"\"," +
		"\"maxplayers\":\""+maxplayers+"\"," +
		"\"spawnradio\":\""+spawnradio+"\"," +
		"\"levelname\":\""+levelname+"\"," +
		"\"levelseed\":\""+levelseed+"\"," +
		"\"nether\":\""+nether+"\"," +
		"\"spawnmonsters\":\""+spawnmonsters+"\"," +
		"\"spawnanimals\":\""+spawnanimals+"\"," +
		"\"onlinemode\":\""+onlinemode+"\"," +
		"\"pvp\":\""+pvp+"\"," +
		"\"flight\":\""+flight+"\"," +
		"\"whitelist\":\""+whitelist+"\"}";
		
		return json;
	}
	
	public String infoData(){
		String data = "";
		try{
			String version = milkAdminInstance.getServer().getVersion();
			Matcher result = Pattern.compile("b([0-9]+)jnks").matcher(version);
			result.find();
			String build = result.group(1);
			String totmem = String.valueOf(Runtime.getRuntime().totalMemory() / 1024 / 1024);
			String maxmem = String.valueOf(Runtime.getRuntime().maxMemory() / 1024 / 1024);
			String freemem = String.valueOf(Runtime.getRuntime().freeMemory() / 1024 / 1024);
			String usedmem = String.valueOf((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/ 1024 / 1024);
			String users = "[]";
			String amountusers = String.valueOf(milkAdminInstance.getServer().getOnlinePlayers().length);
			if ( milkAdminInstance.getServer().getOnlinePlayers().length > 0 ){
				users = "[";
				Player[] p = milkAdminInstance.getServer().getOnlinePlayers();
				for (int i = 0; i < p.length; i++ ){
					users = users + "\"" + p[i].getName()+ "\"";
					if (i < p.length - 1) {
						users = users + ", ";
					}
				}
				users = users + "]";
			}
			data = "{\"lastrestart\":\""+MilkAdmin.initTime+"\"," +
					"\"version\":\""+version+"\"," +
					"\"build\":\""+build+"\"," +
					"\"totmem\":\""+totmem+"\"," +
					"\"maxmem\":\""+maxmem+"\"," +
					"\"freemem\":\""+freemem+"\"," +
					"\"usedmem\":\""+usedmem+"\"," +
					"\"amountusers\":\""+amountusers+"\"," +
					"\"users\":"+users+"," +
					"\"properties\":"+infoProperties()+"}";
		}
		catch (Exception e) {
			debug("[milkAdmin] ERROR in infoData(): " + e.getMessage());
		}
		return data;
	}
	
	public void readLine(String path, ArrayList<String> save){
		try {
			save.clear();
			File banlist = new File(path);
			if (banlist.exists()){
				BufferedReader in = new BufferedReader(new FileReader(banlist));
				String data = null;
				while ((data = in.readLine()) != null){
					//Checking for blank lines
					if (data.length()>0){
						save.add(data);
					}
				}
				in.close();
			}
		}
		catch (IOException e) {
			debug("[milkAdmin] ERROR in readLine(): " + e.getMessage());
		} 
	}
	
	public void listBans(){
		//[{"players":[{"name":"pepito"},{"name":"sharkale31"}]},{"ips":[{"ip":"127.0.0.1"},{"ip":"127.0.0.2"}]}]
		String listban = "";
		Iterator<Map.Entry<String, String>> i;
		Map.Entry<String, String> e;
		try {
			debug("[milkAdmin] Writing listbans.");
			// names
			Map<String,String> banNames = banListName.returnMap();
			i = banNames.entrySet().iterator();
			listban = "[{\"players\":[";
			while(i.hasNext()) {
				e = i.next();
				listban = listban + "{\"name\":\"" + e.getKey() + "\",\"cause\":\"" + e.getValue() + "\"}";
				if(i.hasNext()) listban = listban + ",";
			}
			listban = listban + "]},";
			// ips
			Map<String,String> banIps = banListIp.returnMap();
			i = banIps.entrySet().iterator();
			listban = listban + "{\"ips\":[";
			while(i.hasNext()) {
				e = i.next();
				listban = listban + "{\"ip\":\"" + e.getKey() + "\",\"cause\":\"" + e.getValue() + "\"}";
				if(i.hasNext()) listban = listban + ",";
			}
			listban = listban + "]}]";
		} catch (Exception err) {
			debug("[milkAdmin] ERROR in listBans(): " + err.getMessage());
		}
		debug("[milkAdmin] Banlist - Sending JSON lenght: "+listban.length());
		print(listban, "application/json");
	}

	public static void copyFolder(File src, File dest)
	throws IOException{

		if(src.isDirectory()){
			if(!dest.exists()){
				dest.mkdir();
			}

			if(!src.exists()){
				System.out.println("[milkAdmin] Directory does not exist.");
				return;
			}
			String files[] = src.list();

			for (String file : files) {
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				copyFolder(srcFile,destFile);
			}
		}else{
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest); 
			
			// byte fileData[] = new byte[8192];
			byte[] buffer = new byte[8192];

			int length;
			while ((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}
			in.close();
			out.close();
		}
	}

	public String sha512me(String message){
		MessageDigest md;
		try {
			md= MessageDigest.getInstance("SHA-512");
			md.update(message.getBytes());
			byte[] mb = md.digest();
			String out = "";
			for (int i = 0; i < mb.length; i++) {
				byte temp = mb[i];
				String s = Integer.toHexString(new Byte(temp));
				while (s.length() < 2) {
					s = "0" + s;
				}
				s = s.substring(s.length() - 2);
				out += s;
			}
			message = out;

		} catch (NoSuchAlgorithmException e) {
			debug("[milkAdmin] ERROR in sha512me(): " + e.getMessage());
		}
		return message;
	}

	static public boolean deleteDirectory(File path) {
		if(path.exists() ) {
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					deleteDirectory(files[i]);
				}else{
					files[i].delete();
				}
			}
		}
		return( path.delete() );
	}
	
	public void print(String data, String MimeType){
		try{ 
			DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
			out.writeBytes("Content-Type: "+MimeType+"\r\n");
			out.writeBytes("Cache-Control: no-cache, must-revalidate\r\n");
			out.writeBytes("Content-Length: "+data.length()+"\r\n");
			out.writeBytes("Server: milkAdmin Server\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			out.writeBytes(data);
			out.close();
		} catch (Exception e) { 
			debug("[milkAdmin] ERROR in print(): " + e.getMessage());
		}
	}

	public void load_settings(){
		Settings.load();
		Debug = Settings.getBoolean("Settings.Debug", false);
		Port = Settings.getInt("Settings.Port", 64712);
		consoleLines = Settings.getInt("Settings.ConsoleLines", 13);
		BannedString = Settings.getString("Strings.Banned", "Banned from this server");
		KickedString = Settings.getString("Strings.Kicked", "Kicked!");
		NoSavePropertiesFile serverProperties = new NoSavePropertiesFile("server.properties");
		levelname = serverProperties.getString("level-name");
	}
	
	public String getParam(String param, String URL)
	{
		Pattern regex = Pattern.compile("[\\?&]"+param+"=([^&#]*)");
		Matcher result = regex.matcher(URL);
		if(result.find()){
			try{
				String resdec = URLDecoder.decode(result.group(1),"UTF-8");
				if(param != "password") debug("getParam: "+param+" - Value: "+resdec);
				return resdec;
			}catch (UnsupportedEncodingException e){
				debug("[milkAdmin] ERROR in getParam(): " + e.getMessage());
				return "";
			}
		}else
			return "";
	}
	
	public void run(){
		load_settings();
		try{
			if ( WebServerMode == 0 ){
				rootSocket = new ServerSocket(Port);
				System.out.println("[milkAdmin] Listening on localhost:"+Port);
				for (;;)
					new WebServer(milkAdminInstance, rootSocket.accept());
			} else {
				BufferedReader in = new BufferedReader(new InputStreamReader(WebServerSocket.getInputStream()));
				try{
					String l, g, url="", param="", json, htmlDir = "./plugins/milkAdmin/html";
					while ( (l = in.readLine()).length() > 0 ){
						if ( l.startsWith("GET") ){
							g = (l.split(" "))[1];
							Pattern regex = Pattern.compile("([^\\?]*)([^#]*)");
							Matcher result = regex.matcher(g);
							if(result.find()){
								url = result.group(1);
								param = result.group(2);
							}
							
							if ( url.startsWith("/server/login") ){
								String username = getParam("username", param);
								String password = getParam("password", param);
	                        	if(username.length() > 0 && password.length() > 0){
									if(adminList.containsKey(username)){
										String login = adminList.getString(username, password);
										if(login.contentEquals(sha512me(password))){
											LoggedIn.setString(WebServerSocket.getInetAddress().getHostAddress(), WebServerSocket.getInetAddress().getCanonicalHostName());
											LoggedIn.setString(WebServerSocket.getInetAddress().getCanonicalHostName(), WebServerSocket.getInetAddress().getHostAddress());
											json = "ok";
										}else{
											json = "error";
										}
									}else{
										json = "error";
									}
								}else{
									json = "error";
								}
	                        	print(json, "text/html");
							}
							else if (!noSaveLoggedIn.containsKey(WebServerSocket.getInetAddress().getCanonicalHostName()) || !noSaveLoggedIn.containsKey(WebServerSocket.getInetAddress().getHostAddress())){
								if( url.equals("/") || url.equals("/index.html")){
									readFileAsBinary(htmlDir+"/login.html");
								}
								else if( url.startsWith("/images/") || url.startsWith("/js/") || url.startsWith("/css/")){
									readFileAsBinary(htmlDir + url);
								}
								//OTHERWISE LOAD PAGES
								else{
									readFileAsBinary(htmlDir+"/login.html");
								}
							}else{
								if(adminList.containsKey("admin")){
									if( url.equals("/register.html")){
										readFileAsBinary(htmlDir+"/register.html");
									}
									else if( url.startsWith("/images/") || url.startsWith("/js/") || url.startsWith("/css/")){
										readFileAsBinary(htmlDir + url);
									}
									else if ( url.startsWith("/server/account_create") ){
										String username = getParam("username", param);
										String password = getParam("password", param);
			                        	if(username.length() > 0 && password.length() > 0){
			                        		saveAdminList.setString(username, sha512me(password));
			                        		saveAdminList.removeKey("admin");
			                        		json = "ok:accountcreated";
			                        	}else
			                        		json = "error:badparameters";
										print(json, "text/html");
									}else{
										readFileAsBinary(htmlDir+"/register.html");
									}
								}
								//FINISHED LOGIN

								//SERVER
								//AREA
								else if ( url.startsWith("/server/account_create") ){
									String username = getParam("username", param);
									String password = getParam("password", param);
		                        	if(username.length() > 0 && password.length() > 0){
		                        		saveAdminList.setString(username, sha512me(password));
		                        		json = "ok:accountcreated";
		                        	}else
		                        		json = "error:badparameters";
									print(json, "text/plain");
								}
								else if ( url.equals("/server/logout") ){
									LoggedIn.removeKey(WebServerSocket.getInetAddress().getCanonicalHostName());
									LoggedIn.removeKey(WebServerSocket.getInetAddress().getHostAddress());
									json = "ok";
									print(json, "text/plain");
								}
								else if ( url.equals("/save") ){
									consoleCommand("save-all");
									json = "ok:worldsaved";
									print(json, "text/plain");
								}
								else if ( url.startsWith("/server/say") ){
									String text = getParam("message", param);
		                        	if(text.length() > 0){
										if(text.startsWith("/")){
											String command = text.replace("/", "");
											consoleCommand(command);
										}else{
											consoleCommand("say " + text);
										}
										json = "ok:messagesent";
		                        	}else
		                        		json = "error:messageempty";
									print(json, "text/plain");
								}
								else if ( url.startsWith("/server/broadcast_message") ){
									String text = getParam("message", param);
		                        	if(text.length() > 0){
										milkAdminInstance.getServer().broadcastMessage(text);
										json = "ok:broadcastedmessage";
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.equals("/stop") ){
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"20; url=/\">" + readFileAsString(htmlDir+"/wait.html");
									print(json, "text/html");
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										debug("[milkAdmin] ERROR in Stop: " + e.getMessage());
									}
									milkAdminInstance.api.executeCommand(RTKInterface.CommandType.HOLD_SERVER,null);
								}
								else if ( url.equals("/reload_server") ){
									milkAdminInstance.getServer().reload();
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"20; url=/\">" + readFileAsString(htmlDir+"/wait.html");
									print(json, "text/html");
								}
								else if ( url.equals("/restart_server") ){
									try{
										milkAdminInstance.api.executeCommand(RTKInterface.CommandType.RESTART,null);
									}catch(IOException e){
										debug("[milkAdmin] ERROR in restart_server: " + e.getMessage());
									}
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"20; url=/\">" + readFileAsString(htmlDir+"/wait.html");
									print(json, "text/html");
								}
								else if ( url.equals("/force_stop") ){
									json = "ok:forcestop";
									print(json, "text/plain");
									System.exit(1);
								}
								else if ( url.equals("/server/get_plugins.json") ){
									Plugin[] p = milkAdminInstance.getServer().getPluginManager().getPlugins();
									json = "[";
									int cant = p.length;
									for(int i = 0; i < cant; i++){
										PluginDescriptionFile pdf = p[i].getDescription();
										json = json + "{\"name\":\"" + pdf.getName() + "\", \"version\":\"" + pdf.getVersion() + "\",\"enabled\":"+p[i].isEnabled()+"}";
										if (i < (cant) - 1) json = json + ",";
									}
									json = json + "]";
									print(json, "application/json");
								}
								else if ( url.startsWith("/server/disable_plugin") ){
									String plugin = getParam("plugin", param);
		                        	if(plugin.length() > 0){
										milkAdminInstance.getServer().getPluginManager().disablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(plugin));
										json = "ok:plugindisabled:_NAME_,"+plugin;
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/server/enable_plugin") ){
									String plugin = getParam("plugin", param);
		                        	if(plugin.length() > 0){
										milkAdminInstance.getServer().getPluginManager().enablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(plugin));
										json = "ok:pluginenabled:_NAME_,"+plugin;
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if( url.equals("/server/console")) {
									print(readConsole(), "text/plain");
								}
								else if( url.startsWith("/server/properties_edit")) {
									String property = getParam("property", param);
									String value = getParam("value", param);
		                        	if(property.length() > 0 && value.length() > 0){
										BukkitProperties.setString(property, value);
										json = "ok:editedproperty";
									}else{
										json = "error:badparameters";
									}
											
									print(json, "text/plain");
								}
								else if( url.equals("/backup")){
									consoleCommand("save-all");
									DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH-mm-ss");
									Date date = new Date();
									String datez = dateFormat.format(date);
									new File("plugins/milkAdmin/backups").mkdir();
									new File("plugins/milkAdmin/backups/"+datez).mkdir();
									File srcFolder = new File(levelname);
									File destFolder = new File("plugins/milkAdmin/backups/" + datez + "/" + levelname);
									try{
										copyFolder(srcFolder,destFolder);
										json = "ok:worldbackedup";
										print(json, "text/plain");
									}catch(IOException e){
										debug("[milkAdmin] ERROR in backup: " + e.getMessage());
										return;
									}
								}
								else if( url.startsWith("/restore")){
									String id = getParam("id", param);
									if(id.length() > 0){
										json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"20; url=/server/restore?id="+id+"\">" + readFileAsString(htmlDir+"/wait.html");
										print(json, "text/html");
									}else
										readFileAsBinary(htmlDir+"/index.html");
									milkAdminInstance.api.executeCommand(RTKInterface.CommandType.HOLD_SERVER,null);
								}
								else if( url.startsWith("/delete")){
									String id = getParam("id", param);
									if(id.length() > 0){
										deleteDirectory(new File("plugins/milkAdmin/backups/"+id));
										json = "ok:deletebackup";
									}else
										json = "error:badparameters";
									print(json, "text/plain");
								}
								else if( url.equals("/info/list_backups.json")){
									File dir = new File("backups");
									String[] children = dir.list();
									String listbu = "[";
									if (children == null) {
										listbu = "[]";
									} else {
										listbu = "[";
										int i=0; 
										while (i < (children.length)) {
											String filename = children[i];
											String filenamed = filename;
											String filenamechanged = filenamed.replace(".", "/").replace("_", " ").replace("-", ":");
											listbu = listbu + ("{\"optionValue\":\"" + filename + "\", \"optionDisplay\":\"" + filenamechanged + "\"}");
											if (i < (children.length) - 1) {
												listbu = listbu + (",");
											}
											i++;
										}
										listbu = listbu + ("]");
									}
									print(listbu, "application/json");
								}
								/////////////
								//INFO AREA
								/////////////
								else if ( url.equals("/info/data.json") ){
									print(infoData(), "application/json");
								}
								////////////////
								//PLAYER AREA
								////////////////
								else if ( url.startsWith("/player/kick") ){
									String user = getParam("user", param);
									String cause = getParam("cause", param);
									if(user.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											String kickString = KickedString;
											if(cause.length() > 0)
												kickString = cause;
											p.kickPlayer(kickString);
											json = "ok:kickplayer:_NAME_,"+user;
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/give_item") ){
									String user = getParam("user", param);
									String item = getParam("item", param);
									String amount = getParam("amount", param);
									if(user.length() > 0 && amount.length() > 0 && item.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											p.getInventory().addItem(new ItemStack(Material.getMaterial(Integer.valueOf(item)), Integer.valueOf(amount)));
											json = "ok:itemsgiven:_NAME_,"+user+",_AMOUNT_,"+amount+",_ITEM_,"+Material.getMaterial(Integer.valueOf(item));
										}else{
											json = "error:playernotconnected";
										} 
									}else{
										json = "error:badparameters";
									}

									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/remove_item") ){
									String user = getParam("user", param);
									String item = getParam("item", param);
									String amount = getParam("amount", param);
									if(user.length() > 0 && amount.length() > 0 && item.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											p.getInventory().removeItem(new ItemStack(Material.getMaterial(Integer.valueOf(item)), Integer.valueOf(amount)));
											json = "ok:itemsremoved:_NAME_,"+user+",_AMOUNT_,"+amount+",_ITEM_,"+Material.getMaterial(Integer.valueOf(item));
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/get_health") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											json = "ok:"+String.valueOf(p.getHealth());
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/set_health") ){
									String user = getParam("user", param);
									String amount = getParam("amount", param);
									if(user.length() > 0 && amount.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											try{
												int health = Integer.parseInt(amount,10);
												if(health >= 0 && health <=20){
													p.setHealth(health);
													if(health == 0){
														json = "ok:playerkilled:_NAME_,"+user;
													}else{
														json = "ok:healthchanged:_NAME_,"+user+",_AMOUNT_,"+amount;
													}
												}else{
													json = "error:badparameters";
												}
											}catch(NumberFormatException err){
												json = "error:badparameters";
											}
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/ban_player") ){
									String user = getParam("user", param);
									String cause = getParam("cause", param);
									if(user.length() > 0){
										String banstring = BannedString;
										if(cause.length() > 0)
											banstring = cause;
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											banListName.setString(p.getName(), banstring);
											p.kickPlayer(banstring);
										}else{
											banListName.setString(user, banstring);
										}
										banListName.save();
										json = "ok:playerbanned:_NAME_,"+user;
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/ban_ip") ){
									String ip = getParam("ip", param);
									String cause = getParam("cause", param);
									if(ip.length() > 0){
										String banstring = BannedString;
										if(cause.length() > 0)
											banstring = cause;
										Player p = milkAdminInstance.getServer().getPlayer(ip);
										if(p != null && p.isOnline()){
											banListIp.setString(String.valueOf(p.getAddress()).split("/")[1].split(":")[0], banstring);
											p.kickPlayer(banstring);
										}else{
											banListIp.setString(ip, banstring);
										}
										banListIp.save();
										json = "ok:ipbanned:_IP_,"+ip;
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/unban_player") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										if(banListName.keyExists(user)){
											banListName.removeKey(user);
											banListName.save();
											json = "ok:playerunbanned:_NAME_,"+user;
										}else{
											json = "error:playernotbanned";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/unban_ip") ){
									String ip = getParam("user", param);
									if(ip.length() > 0){
										if(banListIp.keyExists(ip)){
											banListIp.removeKey(ip);
											banListIp.save();
											json = "ok:ipunbanned:_IP_,"+ip;
										}else{
											json = "error:ipnotbanned";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/shoot_arrow") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											p.shootArrow();
											json = "ok:arrowshooted";
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.equals("/player/banlist.json") ){
									listBans();
								}
								else if ( url.startsWith("/player/throw_snowball") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											p.throwSnowball();
											json = "ok:throwsnowball";
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/throw_egg") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											p.throwEgg();
											json = "ok:throwegg";
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/change_display_name") ){
									String user = getParam("user", param);
									String name = getParam("name", param);
									if(user.length() > 0 && name.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											p.setDisplayName(name);
											json = "ok:changename:_OLD_,"+user+",_NEW_,"+name;
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/teleport_to_player") ){
									String user = getParam("user", param);
									String touser = getParam("to_user", param);
									if(user.length() > 0 && touser.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										Player p2 = milkAdminInstance.getServer().getPlayer(touser);
										if(p != null && p2!= null && p.isOnline() && p2.isOnline()){
											p.teleport(p2);
											json = "ok:playerteleported";
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/teleport_to_location") ){
									String user = getParam("user", param);
									String x = getParam("x", param);
									String y = getParam("y", param);
									String z = getParam("z", param);
									if(user.length() > 0 && x.length() > 0 && y.length() > 0 && z.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											p.teleport(new Location(p.getWorld(), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z)));
											json = "ok:playerteleported";
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/is_online") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline())
											json = "ok:"+String.valueOf(p.isOnline());
										else
											json = "ok:false";
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/get_ip_port.json") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											String ip_port = String.valueOf(p.getAddress()).split("/")[1];
											json = "{\"status\":\"ok\",\"ip\":\""+ip_port.split(":")[0]+"\",\"port\":\""+ip_port.split(":")[1]+"\"}";
										}else{
											json = "{\"status\":\"error\", \"error\":\"playernotconnected\"}";
										}
									}else{
										json = "{\"status\":\"error\", \"error\":\"badparameters\"}";
									}
									print(json, "application/json");
								}
								else if( url.equals("/") || url.equals("/index.html")){
									readFileAsBinary(htmlDir+"/index.html");
								}
								else if( url.startsWith("/images/") || url.startsWith("/js/") || url.startsWith("/css/")){
									readFileAsBinary(htmlDir + url);
								}
								else
									readFileAsBinary(htmlDir+"/index.html");
							}
						}
					}
				} catch (Exception e) {
					debug("[milkAdmin] ERROR in ServerParser: " + e.getLocalizedMessage());
				}
			}
		} catch (IOException e){
			debug("[milkAdmin] ERROR in ServerInitialize: " + e.getMessage());
		}
	}
	public void stopServer()throws IOException{
		rootSocket.close();
	}
}