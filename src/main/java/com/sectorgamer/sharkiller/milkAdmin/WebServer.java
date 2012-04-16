package com.sectorgamer.sharkiller.milkAdmin;

import java.net.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.io.*;
import java.util.regex.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;

import com.sectorgamer.sharkiller.milkAdmin.rtk.*;
import com.sectorgamer.sharkiller.milkAdmin.util.*;

/**
 * Simple <code>WebServer</code> All-In-One.
 * @author Sharkiller
 */
public class WebServer extends Thread implements RTKListener{
	int WebServerMode;
	MilkAdmin milkAdminInstance;
	Socket WebServerSocket;
	ServerSocket rootSocket = null;
	static Logger log = Logger.getLogger("Minecraft");
	String Lang;
	boolean Debug;
	static InetAddress Ip = null;
	int Port;
	int consoleLines;
	String BannedString;
	String KickedString;
	String levelname;
	String PluginDir = "plugins/milkAdmin/";
	String BackupPath = "Backups [milkAdmin]";
	String ExternalUrl = "";
	String BanListDir;
	Configuration Settings = new Configuration(new File(PluginDir+"settings.yml"));
	Configuration Worlds = new Configuration(new File(PluginDir+"worlds.yml"));
	PropertiesFile BukkitProperties = new PropertiesFile("server.properties");
	String bannedplayers = "banned-players.txt";
	ArrayList<String> bannedPlayers = new ArrayList<String>();
	String bannedips = "banned-ips.txt";
	ArrayList<String> bannedIps = new ArrayList<String>();
	NoSavePropertiesFile adminList = new NoSavePropertiesFile(PluginDir+"admins.ini");
	PropertiesFile saveAdminList = new PropertiesFile(PluginDir+"admins.ini");
	PropertiesFile LoggedIn = new PropertiesFile(PluginDir+"loggedin.ini");

	/**
	 * Create the socket and listens for a connection.
	 * 
	 * @param i milkAdmin instance.
	 */
	public WebServer(MilkAdmin i){
		WebServerMode = 0;
		milkAdminInstance = i;
		start();
	}
	
	/**
	 * Process the GET request.
	 * 
	 * @param i milkAdmin instance.
	 * @param s Socket with the request.
	 */
	public WebServer(MilkAdmin i, Socket s){
		WebServerMode = 1;
		milkAdminInstance = i;
		WebServerSocket = s;
		start();
	}
	
	public void debug(String text){
		if(Debug)
			MilkAdminLog.debug(text);
	}

	public String readFileAsString(String filePath)
	throws java.io.IOException{
		StringBuffer fileData = new StringBuffer(65536);
		try{
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[65536];
			int length;
			
			while((length = reader.read(buf)) > -1){
				fileData.append(String.valueOf(buf, 0, length).replaceAll("_ExternalUrl_", ExternalUrl));
			}
			reader.close();
		}
		catch (Exception e) {
			debug("ERROR in readFileAsString(): " + e.getMessage());
		}
		return fileData.toString();
	}
	
	public void readFileAsBinary(String path, String type)
	throws java.io.IOException{
		readFileAsBinary(path, type, false);
	}
	
	public void readFileAsBinary(String path, String type, boolean replace)
	throws java.io.IOException{
		try{
			File archivo = new File(path);
			String StringData = new String("");
			long lengthData;
			if(archivo.exists()){
				FileInputStream file = new FileInputStream(archivo);
				byte[] fileData = new byte[65536];
				int length;
			
				if(replace){
					while ((length = file.read(fileData)) > 0){
						String aux = new String(fileData, 0, length);
						StringData = StringData + aux.replaceAll("_ExternalUrl_", ExternalUrl);
					}
					lengthData = StringData.length();
				}else{
					lengthData = archivo.length();
				}
				
				DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
				out.writeBytes("HTTP/1.1 200 OK\r\n");
				if(type != null)
					out.writeBytes("Content-Type: "+type+"; charset=utf-8\r\n");
				out.writeBytes("Content-Length: "+lengthData+"\r\n");
				out.writeBytes("Cache-Control: no-cache, must-revalidate\r\n");
				out.writeBytes("Server: milkAdmin Webserver\r\n");
				out.writeBytes("Connection: Close\r\n\r\n");
				
				if(replace){
					out.writeBytes(StringData);
				}else{
					while ((length = file.read(fileData)) > 0)
						out.write(fileData, 0, length);
				}
				out.flush();
	
	            file.close();
	            out.close();
			}else{
				httperror("404 Not Found");
			}
		}
		catch (Exception e) {
			debug("ERROR in readFileAsBinary(): " + e.getMessage());
		}
	}

	public void onRTKStringReceived(String s){
		debug("From wrapper: "+s);
	}

	public void consoleCommand(String cmd){
		milkAdminInstance.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
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
			debug("ERROR in readConsole(): " + e.getMessage());
		}
		return console;	
	}
	
	public String lastConsoleLine(){
		String console = ""; 
		String lastline = "";
		try{
			File f = new File("server.log");
			RandomAccessFile randomFile = new RandomAccessFile(f, "r");
			long fileLength = randomFile.length();
			long startPosition = fileLength - 200;
			if(startPosition < 0)
				startPosition = 0;
			randomFile.seek(startPosition);
			while( ( lastline = randomFile.readLine() ) != null ){
				console = lastline;
			}
		}
		catch (Exception e) {
			debug("ERROR in lastConsoleLine(): " + e.getMessage());
		}
		return console;
	}
	
	public String infoProperties() throws IOException{
		BukkitProperties.load();
		String ip = BukkitProperties.getString("server-ip", "");
		String port = BukkitProperties.getString("server-port", "25565");
		String maxplayers = BukkitProperties.getString("max-players", "10");
		String viewdistance = BukkitProperties.getString("view-distance", "10");
		String holdmessage = BukkitProperties.getString("hold-message", "");
		boolean allownether  = BukkitProperties.getBoolean("allow-nether", true);
		boolean spawnmonsters = BukkitProperties.getBoolean("spawn-monsters", false);
		boolean spawnanimals = BukkitProperties.getBoolean("spawn-animals", false);
		boolean onlinemode = BukkitProperties.getBoolean("online-mode", false);
		boolean pvp = BukkitProperties.getBoolean("pvp", false);
		boolean flight = BukkitProperties.getBoolean("allow-flight", false);
		boolean whitelist = BukkitProperties.getBoolean("white-list", false);
		
		String json = "{\"ip\":\""+ip+"\"," +
		"\"port\":\""+port+"\"," +
		"\"maxplayers\":\""+maxplayers+"\"," +
		"\"viewdistance\":\""+viewdistance+"\"," +
		"\"holdmessage\":\""+holdmessage+"\"," +
		"\"levelname\":\""+levelname+"\"," +
		"\"allownether\":\""+allownether+"\"," +
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
		String build = "???";
		String freespace = "1";
		String totalspace = "1";
		String usedspace = "0";
		try{
			String version = milkAdminInstance.getServer().getVersion();
			Matcher result = Pattern.compile("b([0-9]+)jnks").matcher(version);
			result.find();
			try{
				build = result.group(1);
			}catch(IllegalStateException e){}
			String totmem = String.valueOf(Runtime.getRuntime().totalMemory() / 1024 / 1024);
			String maxmem = String.valueOf(Runtime.getRuntime().maxMemory() / 1024 / 1024);
			String freemem = String.valueOf(Runtime.getRuntime().freeMemory() / 1024 / 1024);
			String usedmem = String.valueOf((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/ 1024 / 1024);
			File Disk = new File(BackupPath);
			try{
				if(!Disk.exists())
					Disk.mkdir();
				double fs = (double)(Disk.getFreeSpace() / 1024 / 1024) / 1024;
				double ts = (double)(Disk.getTotalSpace() / 1024 / 1024) / 1024;
				double us = ts - fs;
				freespace = String.format("%.2f%n", fs).trim();
				totalspace = String.format("%.2f%n", ts).trim();
				usedspace = String.format("%.2f%n", us).trim();
			}catch(SecurityException e){
				debug("Security Exception in Space Data");
			}
			
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
			boolean usingrtk = milkAdminInstance.UsingRTK;
			data = "{\"lastrestart\":\""+MilkAdmin.initTime+"\"," +
					"\"version\":\""+version+"\"," +
					"\"build\":\""+build+"\"," +
					"\"totmem\":\""+totmem+"\"," +
					"\"maxmem\":\""+maxmem+"\"," +
					"\"freemem\":\""+freemem+"\"," +
					"\"usedmem\":\""+usedmem+"\"," +
					"\"freespace\":\""+freespace+"\"," +
					"\"totalspace\":\""+totalspace+"\"," +
					"\"usedspace\":\""+usedspace+"\"," +
					"\"amountusers\":\""+amountusers+"\"," +
					"\"users\":"+users+"," +
					"\"usingrtk\":"+usingrtk+"," +
					"\"properties\":"+infoProperties()+"}";
		}
		catch (Exception e) {
			debug("ERROR in infoData(): " + e.getMessage());
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
			debug("ERROR in readLine(): " + e.getMessage());
		} 
	}
	
	public List<String> loadWhitelist() {
		String line;
		BufferedReader fin;
		List<String> players = new ArrayList<String>();

		try {
			fin = new BufferedReader(new FileReader("white-list.txt"));
		} catch (FileNotFoundException e) {
			debug("ERROR in loadWhitelist(): "+e.getMessage());
			return new ArrayList<String>();
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					players.add(line.trim());
			
		} catch (Exception e) {
			debug("ERROR in loadWhitelist(): "+e.getMessage());
			return new ArrayList<String>();
		} finally {
			try{
				fin.close();
			} catch (IOException e){
				debug("ERROR in loadWhitelist(): "+e.getMessage());
			}
		}
		return players;
	}
	
	public boolean saveWhitelist(List<String> players) {
		final String newLine = System.getProperty("line.separator");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("white-list.txt"));
			for (String player : players)
				writer.write(player + newLine);
			writer.flush();
			writer.close();
			return true;
		} catch (Exception e) {
			debug("ERROR in saveWhitelist(): "+e.getMessage());
			return false;
		}
	}
	
	public void addToWhitelist(String user) {
		File file = new File("white-list.txt");
		try {
			FileWriter writer = new FileWriter(file, true);
			writer.write(user + System.getProperty("line.separator"));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			debug("ERROR in saveWhitelist(): "+e.getMessage());
		}
	}
	
	public void listBans(){
		//[{"players":[{"name":"pepito"},{"name":"sharkale31"}]},{"ips":[{"ip":"127.0.0.1"},{"ip":"127.0.0.2"}]}]
		String listban = "";
		Iterator<Map.Entry<String, String>> i;
		Map.Entry<String, String> e;
		try {
			debug("Writing listbans.");
			// names
			Map<String,String> banNames = milkAdminInstance.BL.banListName.returnMap();
			i = banNames.entrySet().iterator();
			listban = "[{\"players\":[";
			while(i.hasNext()) {
				e = i.next();
				listban = listban + "{\"name\":\"" + e.getKey() + "\",\"cause\":\"" + e.getValue() + "\"}";
				if(i.hasNext()) listban = listban + ",";
			}
			listban = listban + "]},";
			// ips
			Map<String,String> banIps = milkAdminInstance.BL.banListIp.returnMap();
			i = banIps.entrySet().iterator();
			listban = listban + "{\"ips\":[";
			while(i.hasNext()) {
				e = i.next();
				listban = listban + "{\"ip\":\"" + e.getKey() + "\",\"cause\":\"" + e.getValue() + "\"}";
				if(i.hasNext()) listban = listban + ",";
			}
			listban = listban + "]}]";
		} catch (Exception err) {
			debug("ERROR in listBans(): " + err.getMessage());
		}
		debug("Banlist - Sending JSON lenght: "+listban.length());
		print(listban, "application/json");
	}

	public static void copyFolder(File src, File dest)
	throws IOException{

		if(src.isDirectory()){
			if(!dest.exists()){
				dest.mkdir();
			}

			if(!src.exists()){
				MilkAdminLog.info("Directory does not exist.");
				return;
			}
			String files[] = src.list();

			for (String file : files) {
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				copyFolder(srcFile,destFile);
			}
		}else{
			
			if(!dest.exists()) {
				dest.createNewFile();
			}
			
			FileChannel source = null;
			FileChannel destination = null;
			try {
				source = new FileInputStream(src).getChannel();
				destination = new FileOutputStream(dest).getChannel();
				destination.transferFrom(source, 0, source.size());
			}
			finally {
				if(source != null) {
					source.close();
				}
				if(destination != null) {
					destination.close();
				}
			}
		}
	}
	
	static public boolean deleteDirectory(File path) {
		if(path.exists()) {
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
			debug("ERROR in sha512me(): " + e.getMessage());
		}
		return message;
	}
	
	public void print(String data, String MimeType){
		try{ 
			DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 200 OK\r\n");
			out.writeBytes("Content-Type: "+MimeType+"; charset=utf-8\r\n");
			out.writeBytes("Cache-Control: no-cache, must-revalidate\r\n");
			out.writeBytes("Content-Length: "+data.length()+"\r\n");
			out.writeBytes("Server: milkAdmin Server\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			out.writeBytes(data);
			out.flush();
			out.close();
		} catch (Exception e) { 
			debug("ERROR in print(): " + e.getMessage());
		}
	}
	
	public void httperror(String error){
		
		try{
			DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
			out.writeBytes("HTTP/1.1 "+error+"\r\n");
			out.writeBytes("Server: milkAdmin Server\r\n");
			out.writeBytes("Connection: Close\r\n\r\n");
			out.flush();
			out.close();
		} catch (Exception e) { 
			debug("ERROR in httperror(): " + e.getMessage());
		}
	}

	public void load_settings() throws IOException{
		Settings.load();
		LoggedIn.load();
		BackupPath = Settings.getString("Backup.Path", "Backups [milkAdmin]");
		BanListDir = Settings.getString("Settings.BanListDir", "plugins/milkAdmin");
		ExternalUrl = Settings.getString("Settings.ExternalUrl", "http://www.sharkale.com.ar/milkAdmin");
		Debug = Settings.getBoolean("Settings.Debug", false);
		String ipaux = Settings.getString("Settings.Ip", null);
		if(ipaux != null && !ipaux.equals("")){
			try {
				Ip = InetAddress.getByName(ipaux);
			} catch (UnknownHostException e) {
				debug("ERROR UnknownHostException - Ip: "+ ipaux + " - Message: " + e.getMessage());
			}
		}
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
				if(param != "password") debug(" - getParam: "+param+" - Value: "+resdec);
				return resdec;
			}catch (UnsupportedEncodingException e){
				debug("ERROR in getParam(): " + e.getMessage());
				return "";
			}
		}else
			return "";
	}
	
	public void run(){
		try{
			load_settings();
			if ( WebServerMode == 0 ){
				if(Ip == null){
					rootSocket = new ServerSocket(Port);
					MilkAdminLog.info("WebServer listening on port "+Port);
				}else{
					rootSocket = new ServerSocket(Port, 50, Ip);
					MilkAdminLog.info("WebServer listening on "+Ip+":"+Port);
				}
				while(!rootSocket.isClosed()){
					Socket requestSocket = rootSocket.accept();
					new WebServer(milkAdminInstance, requestSocket);
				}
			} else {
				long timeDebug = System.currentTimeMillis();
				String urlDebug = "";
				BufferedReader in = new BufferedReader(new InputStreamReader(WebServerSocket.getInputStream()));
				try{
					String l, g, url="", param="", json, htmlDir = "./plugins/milkAdmin/html";
					boolean flag = true;
					while ( (l = in.readLine()) != null && flag){
						if ( l.startsWith("GET") ){
							flag = false;
							g = (l.split(" "))[1];
							Pattern regex = Pattern.compile("([^\\?]*)([^#]*)");
							Matcher result = regex.matcher(g);
							if(result.find()){
								url = result.group(1);
								param = result.group(2);
							}
							String HostAddress = WebServerSocket.getInetAddress().getHostAddress();
							debug(HostAddress+" - "+url);
							urlDebug = url;
							debug(" - ContainsKey: "+String.valueOf(LoggedIn.containsKey(HostAddress)) + " - keyExists: "+String.valueOf(LoggedIn.keyExists(HostAddress)));
							if ( url.startsWith("/ping") ){
								if (!LoggedIn.containsKey(HostAddress)){
									json = "login";
								}else{
									json = "pong";
								}
								print(json, "text/plain");
							}
							else if ( url.startsWith("/server/login") ){
								String username = getParam("username", param);
								String password = getParam("password", param);
	                        	if(username.length() > 0 && password.length() > 0){
									if(adminList.containsKey(username)){
										String login = adminList.getString(username, password);
										if(login.contentEquals(sha512me(password))){
											debug(" - "+username+" logged in from "+HostAddress);
											LoggedIn.setString(HostAddress, username);
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
	                        	print(json, "text/plain");
							}
							else if (!LoggedIn.containsKey(HostAddress)){
								debug(" - No logged.");
								if( url.equals("/") || url.equals("/login.html")){
									readFileAsBinary(htmlDir+"/login.html", "text/html", true);
								}
								else if(url.startsWith("/js/lang/")){
									readFileAsBinary(htmlDir + url, "text/javascript");
								}
								else if(url.startsWith("/js/")){
									readFileAsBinary(htmlDir + url, "text/javascript", true);
								}
								else if(url.startsWith("/css/")){
									readFileAsBinary(htmlDir + url, "text/css", true);
								}
								else if( url.startsWith("/images/")){
									readFileAsBinary(htmlDir + url, null);
								}
								//OTHERWISE LOAD PAGES
								else{
									httperror("403 Access Denied");
								}
							}else{
								if(adminList.containsKey("admin")){
									if( url.equals("/register.html")){
										readFileAsBinary(htmlDir+"/register.html", "text/html");
									}
									else if(url.startsWith("/js/lang/")){
										readFileAsBinary(htmlDir + url, "text/javascript");
									}
									else if(url.startsWith("/js/")){
										readFileAsBinary(htmlDir + url, "text/javascript", true);
									}
									else if(url.startsWith("/css/")){
										readFileAsBinary(htmlDir + url, "text/css", true);
									}
									else if( url.startsWith("/images/")){
										readFileAsBinary(htmlDir + url, null);
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
										readFileAsBinary(htmlDir+"/register.html", "text/html");
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
									LoggedIn.removeKey(HostAddress);
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
									json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
									json += "<head><script type=\"text/javascript\">tourl = './';</script>" + readFileAsString(htmlDir+"/wait.html");
									print(json, "text/html");
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										debug("ERROR in Stop: " + e.getMessage());
									}
									milkAdminInstance.RTKapi.executeCommand(RTKInterface.CommandType.HOLD_SERVER,null);
								}
								else if ( url.equals("/reload_server") ){
									milkAdminInstance.getServer().reload();
									json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
									json += "<head><script type=\"text/javascript\">tourl = './';</script>" + readFileAsString(htmlDir+"/wait.html");
									print(json, "text/html");
								}
								else if ( url.equals("/restart_server") ){
									try{
										milkAdminInstance.RTKapi.executeCommand(RTKInterface.CommandType.RESTART,null);
									}catch(IOException e){
										debug("ERROR in restart_server: " + e.getMessage());
									}
									json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
									json += "<head><script type=\"text/javascript\">tourl = './';</script>" + readFileAsString(htmlDir+"/wait.html");
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
		                        		if(milkAdminInstance.getServer().getPluginManager().isPluginEnabled(plugin)){
		                        			milkAdminInstance.getServer().getPluginManager().disablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(plugin));
		                        			json = "ok:plugindisabled:_NAME_,"+plugin;
		                        		}else{
		                        			json = "ok:pluginnotenabled";
		                        		}
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
								else if ( url.startsWith("/server/reload_plugin") ){
									String plugin = getParam("plugin", param);
		                        	if(plugin.length() > 0){
		                        		if(milkAdminInstance.getServer().getPluginManager().isPluginEnabled(plugin)){
		                        			milkAdminInstance.getServer().getPluginManager().disablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(plugin));
		                        			milkAdminInstance.getServer().getPluginManager().enablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(plugin));
		                        			json = "ok:pluginreloaded:_NAME_,"+plugin;
		                        		}else{
		                        			json = "ok:pluginnotenabled";
		                        		}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/server/load_plugin") ){
									String plugin = getParam("plugin", param);
		                        	if(plugin.length() > 0){
		                                File pluginFile = new File(new File("plugins"), plugin + ".jar");
		                                if (pluginFile.isFile()) {
		                                    try {
		                                        Plugin newPlugin = milkAdminInstance.getServer().getPluginManager().loadPlugin(pluginFile);
		                                        if (newPlugin != null) {
		                                            String pluginName = newPlugin.getDescription().getName();
		                                            milkAdminInstance.getServer().getPluginManager().enablePlugin(newPlugin);
		                                            if (newPlugin.isEnabled()) {
		                                                MilkAdminLog.info("Plugin loaded and enabled [" + pluginName + "]");
		                                                json = "ok:pluginloaded:_NAME_,"+pluginName;
		                                            } else {
		                                                json = "error:pluginloadfailed";
		                                            }
		                                        } else {
		                                        	json = "error:pluginloadfailed";
		                                        }
		                                    } catch (UnknownDependencyException ex) {
		                                        json = "error:pluginnotplugin";
		                                    } catch (InvalidPluginException ex) {
		                                        json = "error:pluginnotplugin";
		                                    } catch (InvalidDescriptionException ex) {
		                                        json = "error:plugininvalid";
		                                    }
		                                } else {
		                                    json = "error:pluginnotexist";
		                                }
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
								else if( url.startsWith("/page/change_lang")) {
									String lang = getParam("lang", param);
		                        	if(lang.length() > 0){
		                        		if(new File(htmlDir+"/js/lang/"+lang, "default.js").exists()){
		                        			File src = new File(htmlDir+"/js/lang/"+lang, "default.js");
		                        			File dest = new File(htmlDir+"/js/lang", "default.js");
		                        			copyFolder(src, dest);
		                        			json = "ok:langchanged";
		                        		}else
		                        			json = "error:langnotfound";
									}else{
										json = "error:badparameters";
									}
											
									print(json, "text/plain");
								}
								else if( url.equals("/backup")){
									Worlds.load();
									//Worlds.setHeader("# milkAdmin - INTERNAL USE DO NOT MODIFY");
									List<World> worlds = milkAdminInstance.getServer().getWorlds();
									List<String> wstr = new ArrayList<String>();
									if ( worlds.size() > 0 ){
										for (World world: worlds){
											wstr.add(world.getName());
										}
									}
									Worlds.setProperty("Worlds", wstr);
									Worlds.save();
									json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
									json += "<head><script type=\"text/javascript\">tourl = '/backup';</script>" + readFileAsString(htmlDir+"/wait.html");
									print(json, "text/html");
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										debug("ERROR in backup: " + e.getMessage());
									}
									milkAdminInstance.RTKapi.executeCommand(RTKInterface.CommandType.HOLD_SERVER,null);
								}
								else if( url.startsWith("/restore")){
									String id = getParam("id", param);
									String clear = getParam("clear", param);
									if(id.length() > 0){
										json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
										json += "<head><script type=\"text/javascript\">tourl = '/restore?id="+id+"&clear="+clear+"';</script>" + readFileAsString(htmlDir+"/wait.html");
										print(json, "text/html");
									}else
										readFileAsBinary(htmlDir+"/index.html", "text/html");
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										debug("ERROR in backup: " + e.getMessage());
									}
									milkAdminInstance.RTKapi.executeCommand(RTKInterface.CommandType.HOLD_SERVER,null);
								}
								else if( url.startsWith("/delete")){
									String id = getParam("id", param);
									if(id.length() > 0){
										deleteDirectory(new File(BackupPath+"/"+id));
										json = "ok:deletebackup";
									}else
										json = "error:badparameters";
									print(json, "text/plain");
								}
								else if( url.equals("/info/list_backups.json")){
									File dir = new File(BackupPath);
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
								/////////////////////////
								//CUSTOM WHITELIST AREA
								/////////////////////////
								/*else if ( url.equals("/customwl/get.json") ){
									List<String> players = loadWhitelist();
									String wl = "[";
									for(String p: players){
										if(wl.length() > 1)
											wl+= ",";
										wl+= "\""+p+"\"";
									}
									wl+= "]";
									print(wl, "application/json");
								}*/
								else if ( url.equals("/customwl/add.php") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										json = milkAdminInstance.WL.addDefaultPlayer(user);
									}else{
										json = "Invalid User";
									}
									print(json, "text/plain");
								}
								else if ( url.equals("/customwl/remove.php") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										json = milkAdminInstance.WL.removePlayer(user);
									}else{
										json = "Invalid User";
									}
									print(json, "text/plain");
								}
								//////////////////
								//WHITELIST AREA
								//////////////////
								else if ( url.equals("/whitelist/get.json") ){
									List<String> players = loadWhitelist();
									String wl = "[";
									for(String p: players){
										if(wl.length() > 1)
											wl+= ",";
										wl+= "\""+p+"\"";
									}
									wl+= "]";
									print(wl, "application/json");
								}
								else if ( url.equals("/whitelist/add") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										addToWhitelist(user);
										json = "ok";
									}else{
										json = "error";
									}
									print(json, "text/plain");
								}
								else if ( url.equals("/whitelist/save") ){
									String usersP = getParam("users", param);
									List<String> usersL = new ArrayList<String>();
									if(usersP.length() > 0){
										String[] users = usersP.split(",");
										for(String user:users){
											usersL.add(user);
										}
									}
									if(saveWhitelist(usersL)){
										json = "ok";
									}else{
										json = "error";
									}
									print(json, "text/plain");
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
											milkAdminInstance.BL.banListName.setString(p.getName(), banstring);
											p.kickPlayer(banstring);
											MilkAdminLog.info(p.getName()+" banned for: "+banstring);
										}else{
											milkAdminInstance.BL.banListName.setString(user, banstring);
										}
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
											milkAdminInstance.BL.banListIp.setString(String.valueOf(p.getAddress()).split("/")[1].split(":")[0], banstring);
											p.kickPlayer(banstring);
											MilkAdminLog.info(p.getName()+" banned for: "+banstring);
										}else{
											milkAdminInstance.BL.banListIp.setString(ip, banstring);
										}
										json = "ok:ipbanned:_IP_,"+ip;
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/unban_player") ){
									String user = getParam("user", param);
									if(user.length() > 0){
										if(milkAdminInstance.BL.banListName.keyExists(user)){
											milkAdminInstance.BL.banListName.removeKey(user);
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
										if(milkAdminInstance.BL.banListIp.keyExists(ip)){
											milkAdminInstance.BL.banListIp.removeKey(ip);
											json = "ok:ipunbanned:_IP_,"+ip;
										}else{
											json = "error:ipnotbanned";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.equals("/player/banlist.json") ){
									listBans();
								}
								else if ( url.startsWith("/player/shoot_arrow") ){
									String user = getParam("user", param);
									int amount = Integer.parseInt(getParam("amount", param));
									if(user.length() > 0 && amount > 0 && amount < 1000){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											for(int i=0;i<amount;i++)
												p.launchProjectile(org.bukkit.entity.Arrow.class);
											json = "ok:arrowshooted";
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/shoot_fireball") ){
									String user = getParam("user", param);
									int amount = Integer.parseInt(getParam("amount", param));
									if(user.length() > 0 && amount > 0 && amount < 1000){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											for(int i=0;i<amount;i++)
												p.launchProjectile(org.bukkit.entity.Fireball.class);
											
											json = "ok:fireballshooted";
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/throw_snowball") ){
									String user = getParam("user", param);
									int amount = Integer.parseInt(getParam("amount", param));
									if(user.length() > 0 && amount > 0 && amount < 1000){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											for(int i=0;i<amount;i++)
												p.launchProjectile(org.bukkit.entity.Snowball.class);
											
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
									int amount = Integer.parseInt(getParam("amount", param));
									if(user.length() > 0 && amount > 0 && amount < 1000){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											for(int i=0;i<amount;i++)
												p.launchProjectile(org.bukkit.entity.Egg.class);
											
											json = "ok:throwegg";
										}else{
											json = "error:playernotconnected";
										}
									}else{
										json = "error:badparameters";
									}
									print(json, "text/plain");
								}
								else if ( url.startsWith("/player/throw_bomb") ){
									String user = getParam("user", param);
									int amount = Integer.parseInt(getParam("amount", param));
									if(user.length() > 0 && amount > 0 && amount < 1000){
										Player p = milkAdminInstance.getServer().getPlayer(user);
										if(p != null && p.isOnline()){
											for(int i=0;i<amount;i++)
												p.launchProjectile(org.bukkit.entity.SmallFireball.class);
											
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
									if(user.length() > 0 && x.length() > 0 && y.length() > 1 && y.length() < 128 && z.length() > 0){
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
									readFileAsBinary(htmlDir+"/index.html", "text/html", true);
								}
								else if(url.startsWith("/js/lang/")){
									readFileAsBinary(htmlDir + url, "text/javascript");
								}
								else if(url.startsWith("/js/")){
									readFileAsBinary(htmlDir + url, "text/javascript", true);
								}
								else if(url.startsWith("/css/")){
									readFileAsBinary(htmlDir + url, "text/css", true);
								}
								else if( url.startsWith("/images/")){
									readFileAsBinary(htmlDir + url, null);
								}
								else
									readFileAsBinary(htmlDir+"/index.html", "text/html", true);
							}
						}
					}
				} catch (IOException e){
				} catch (Exception e) {
					debug("ERROR in ServerParser: " + e);
				}
				timeDebug = System.currentTimeMillis() - timeDebug;
				debug(" - Took " + timeDebug + "ms to process: " + urlDebug);
			}
		} catch (IOException e){
			debug("ERROR in ServerInitialize: " + e.getMessage());
		}
	}
	public void stopServer()throws IOException{
		if(rootSocket != null)
			rootSocket.close();
	}
}