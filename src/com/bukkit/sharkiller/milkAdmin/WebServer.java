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
	static Logger log = Logger.getLogger("Minecraft");
	String Lang;
	boolean Debug;
	int Port;
	int consoleLines;
	String BannedString;
	String KickedString;
	String GiveItemString;
	String TakeAwayString;
	String levelname;
	Configuration Settings = new Configuration(new File("milkAdmin/settings.yml"));
	PropertiesFile BukkitProperties = new PropertiesFile("server.properties");
	PropertiesFile banList = new PropertiesFile("milkAdmin/banlist.ini");
	String bannedplayers = "banned-players.txt";
	ArrayList<String> bannedPlayers = new ArrayList<String>();
	String bannedips = "banned-ips.txt";
	ArrayList<String> bannedIps = new ArrayList<String>();
	NoSavePropertiesFile adminList = new NoSavePropertiesFile("milkAdmin/admins.ini");
	PropertiesFile saveAdminList = new PropertiesFile("milkAdmin/admins.ini");
	NoSavePropertiesFile noSaveLoggedIn = new NoSavePropertiesFile("milkAdmin/loggedin.ini");
	PropertiesFile LoggedIn = new PropertiesFile("milkAdmin/loggedin.ini");

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
			if(Debug)
				System.out.println("[milkAdmin] ERROR in readFileAsString(): " + e.getMessage());
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
			if(Debug)
				System.out.println("[milkAdmin] ERROR in readFileAsBinary(): " + e.getMessage());
		}
	}

	public void onRTKStringReceived(String s){
		System.out.println("From wrapper: "+s);
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
		if ( (!mcs.g) && (MinecraftServer.a(mcs)) )
			mcs.a(cmd, mcs);
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
			if(Debug)
				System.out.println("[milkAdmin] ERROR in readConsole(): " + e.getMessage());
		}
		return console;	
	}
	
	public String infoData(){
		String data = "";
		try{
			String version = milkAdminInstance.getServer().getVersion();
			String totmem = String.valueOf(Runtime.getRuntime().totalMemory() / 1024 / 1024);
			String maxmem = String.valueOf(Runtime.getRuntime().maxMemory() / 1024 / 1024);
			String freemem = String.valueOf(Runtime.getRuntime().freeMemory() / 1024 / 1024);
			String usedmem = String.valueOf((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/ 1024 / 1024);
			String users = (Lang.equals("en") ? "\"No players online\"":"\"No hay jugadores conectados\"");
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
			data = "{\"version\":\""+version+"\",\"totmem\":\""+totmem+"\",\"maxmem\":\""+maxmem+"\",\"freemem\":\""+freemem+"\",\"usedmem\":\""+usedmem+"\",\"amountusers\":\""+amountusers+"\",\"users\":"+users+"}";
		}
		catch (Exception e) {
			if(Debug)
				System.out.println("[milkAdmin] ERROR in infoData(): " + e.getMessage());
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
			if(Debug)
				System.out.println("[milkAdmin] ERROR in readLine(): " + e.getMessage());
		} 
	}
	
	public void listBans(){
		//[{"players":[{"name":"pepito"},{"name":"sharkale31"}]},{"ips":[{"ip":"127.0.0.1"},{"ip":"127.0.0.2"}]}]
		String listban = "";
		Iterator<String> i;
		try {
			if(Debug) System.out.println("[milkAdmin] Writing listbans.");
			// names
			readLine(bannedplayers, bannedPlayers);
			i = bannedPlayers.iterator();
			listban = "[{\"players\":[";
			while(i.hasNext()) {
				
				listban = listban + "{\"name\":\"" + i.next() + "\"}";
				if(i.hasNext()) listban = listban + ",";
			}
			listban = listban + "]},";
			// ips
			readLine(bannedips, bannedIps);
			i = bannedIps.iterator();
			listban = listban + "{\"ips\":[";
			while(i.hasNext()) {
				listban = listban + "{\"ip\":\"" + i.next() + "\"}";
				if(i.hasNext()) listban = listban + ",";
			}
			listban = listban + "]}]";
		} catch (Exception e) {
			if(Debug)
				System.out.println("[milkAdmin] ERROR in listBans(): " + e.getMessage());
		}
		if(Debug) System.out.println("[milkAdmin] Banlist - Sending JSON lenght: "+listban.length());
		print(listban, "application/json");
	}

	public static void copyFolder(File src, File dest)
	throws IOException{

		if(src.isDirectory()){
			if(!dest.exists()){
				dest.mkdir();
			}

			if(!src.exists()){
				System.out.println("Directory does not exist.");
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
			if(Debug)
				System.out.println("[milkAdmin] ERROR in sha512me(): " + e.getMessage());
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
			if(Debug)
				System.out.println("[milkAdmin] ERROR in print(): " + e.getMessage());
		}
	}

	public void load_settings(){
		Settings.load();
		Lang = Settings.getString("Settings.Language", "english").toLowerCase();
		Debug = Settings.getBoolean("Settings.Debug", false);
		Port = Settings.getInt("Settings.Port", 64712);
		consoleLines = Settings.getInt("Settings.ConsoleLines", 13);
		BannedString = Settings.getString("Strings.Banned", "Banned from this server");
		KickedString = Settings.getString("Strings.Kicked", "Kicked!");
		GiveItemString = Settings.getString("Strings.GiveItem", "You have recieved a");
		TakeAwayString = Settings.getString("Strings.TakeAwayItem", "has been taken away from you :(");
		NoSavePropertiesFile serverProperties = new NoSavePropertiesFile("server.properties");
		levelname = serverProperties.getString("level-name");
	}

	public void run(){
		load_settings();
		if (Lang.equals("spanish")){
			Lang = "es";
		}else if(Lang.equals("english")){
			Lang = "en";
		}else{
			Lang = "en";
		}
		try{
			if ( WebServerMode == 0 ){
				ServerSocket s = new ServerSocket(Port);
				System.out.println("[milkAdmin] Listening on localhost:" + Port);
				for (;;)
					new WebServer(milkAdminInstance, s.accept());
			} else {
				BufferedReader in = new BufferedReader(new InputStreamReader(WebServerSocket.getInputStream()));
				try{
					String l, g, json;
					while ( (l = in.readLine()).length() > 0 ){
						if ( l.startsWith("GET") ){
							g = (l.split(" "))[1];
							if ( g.startsWith("/server/login") ){
								String[] parts = g.replace("&password", "").split("=");
								if(parts.length == 3){
									if(adminList.containsKey(parts[1])){
										String login = adminList.getString(parts[1], parts[2]);
										if(login.contentEquals(sha512me(parts[2]))){
											LoggedIn.setString(WebServerSocket.getInetAddress().getHostAddress(), WebServerSocket.getInetAddress().getCanonicalHostName());
											LoggedIn.setString(WebServerSocket.getInetAddress().getCanonicalHostName(), WebServerSocket.getInetAddress().getHostAddress());
											json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=/\"></head></html>";
										}else{
											json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=../invalidlogin.html\"></head></html>";
										}
									}else{
										json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=../invalidlogin.html\"></head></html>";
									}
								}else{
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=../invalidlogin.html\"></head></html>";
								}
								print(json, "text/html");
							}
							else if (!noSaveLoggedIn.containsKey(WebServerSocket.getInetAddress().getCanonicalHostName()) && !noSaveLoggedIn.containsKey(WebServerSocket.getInetAddress().getHostAddress())){
								if( g.equals("/")){
									readFileAsBinary("./milkAdmin/html/login."+Lang+".html");
								}
								else if( g.equals("/invalidlogin.html")){
									readFileAsBinary("./milkAdmin/html/invalidlogin."+Lang+".html");
								}
								else if( g.equals("/style.css")){
									readFileAsBinary("./milkAdmin/html/style.css");
								}
								//OTHERWISE LOAD PAGES
								else{
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=/\"></head></html>";
									print(json, "text/html");
								}
							}else{
								if(adminList.containsKey("admin")){
									if( g.equals("/register.html")){
										readFileAsBinary("./milkAdmin/html/register."+Lang+".html");
									}
									else if( g.equals("/style.css")){
										readFileAsBinary("./milkAdmin/html/style.css");
									}
									else if ( g.startsWith("/server/account_create") ){
										String[] parts = g.replace("&password", "").split("=");
										saveAdminList.setString(parts[1], sha512me(parts[2]));
										saveAdminList.removeKey("admin");
										json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=/\"></head></html>";
										print(json, "text/html");
									}else{
										readFileAsBinary("./milkAdmin/html/register."+Lang+".html");
									}
								}
								//FINISHED LOGIN

								//SERVER
								//AREA
								else if ( g.startsWith("/server/account_create") ){
									String[] parts = g.replace("&password", "").split("=");
									saveAdminList.setString(parts[1], sha512me(parts[2]));
									json = "ok:"+(Lang.equals("en") ? "Account created.":"Cuenta Creada.");
									print(json, "text/plain");
								}
								else if ( g.equals("/server/logout") ){
									LoggedIn.removeKey(WebServerSocket.getInetAddress().getCanonicalHostName());
									LoggedIn.removeKey(WebServerSocket.getInetAddress().getHostAddress());
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"0; url=/\"></head></html>";
									print(json, "text/html");
								}
								else if ( g.equals("/server/save") ){
									consoleCommand("save-all");
									json = "ok:"+(Lang.equals("en") ? "World Saved":"Mundo Guardado");
									print(json, "text/plain");
								}
								else if ( g.startsWith("/server/say") ){
									String[] parts =  g.replace("+", " ").replace("%21", "!").replace("%40", "@").replace("%23", "#").replace("%24", "$").replace("%25", "%").replace("%5E", "^").replace("%26", "&").replace("%28", "(").replace("%29", ")").replace("%2B", "+").replace("%7B", "{").replace("%5B", "[").replace("%7D", "}").replace("%5D", "]").replace("%7E", "~").replace("%60", "`").replace("%5C", "\\").replace("%7C", "|").replace("%3A", ":").replace("%3B", ";").replace("%22", "\"").replace("%27", "'").replace("%3C", "<").replace("%2C", ",").replace("%3E", ">").replace("%3F", "?").replace("%2F", "/").split("=");
									if(parts[1].startsWith("/")){
										String command = parts[1].replace("/", "");
										consoleCommand(command);
									}else{
										consoleCommand("say " + parts[1]);
									}
									json = "ok:"+(Lang.equals("en") ? "Console Message Sent":"Mensaje de Consola Enviado");
									print(json, "text/plain");
								}
								else if ( g.startsWith("/server/broadcast_message") ){
									String[] parts = g.replace("+", " ").replace("%21", "!").replace("%40", "@").replace("%23", "#").replace("%24", "$").replace("%25", "%").replace("%5E", "^").replace("%26", "&").replace("%28", "(").replace("%29", ")").replace("%2B", "+").replace("%7B", "{").replace("%5B", "[").replace("%7D", "}").replace("%5D", "]").replace("%7E", "~").replace("%60", "`").replace("%5C", "\\").replace("%7C", "|").replace("%3A", ":").replace("%3B", ";").replace("%22", "\"").replace("%27", "'").replace("%3C", "<").replace("%2C", ",").replace("%3E", ">").replace("%3F", "?").replace("%2F", "/").split("=");

									if(parts.length == 2){
										milkAdminInstance.getServer().broadcastMessage(parts[1].replace("%3D", "="));
										json = "ok:"+(Lang.equals("en") ? "Broadcasted Message":"Mensaje Emitido");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.equals("/server/stop") ){
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"20; url=/\">" + readFileAsString("./milkAdmin/html/wait."+Lang+".html");
									print(json, "text/html");
									milkAdminInstance.api.executeCommand(RTKInterface.CommandType.HOLD_SERVER);
								}
								else if ( g.equals("/server/reload_server") ){
									milkAdminInstance.getServer().reload();
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"20; url=/\">" + readFileAsString("./milkAdmin/html/wait."+Lang+".html");
									print(json, "text/html");
								}
								else if ( g.equals("/server/restart_server") ){
									try{
										milkAdminInstance.api.executeCommand(RTKInterface.CommandType.RESTART);
									}catch(IOException e){
										if(Debug)
											System.out.println("[milkAdmin] ERROR in restart_server: " + e.getMessage());
									}
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"20; url=/\">" + readFileAsString("./milkAdmin/html/wait."+Lang+".html");
									print(json, "text/html");
								}
								else if ( g.equals("/server/force_stop") ){
									json = "ok:"+(Lang.equals("en") ? "Stopped server":"Server Detenido");
									print(json, "text/plain");
									System.exit(1);
								}
								else if ( g.equals("/server/get_plugins.json") ){
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
								else if ( g.startsWith("/server/disable_plugin") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										milkAdminInstance.getServer().getPluginManager().disablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(parts[1]));
										json = "ok:"+(Lang.equals("en") ? parts[1]+" Plugin disabled":"Plugin "+parts[1]+" desactivado");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/server/enable_plugin") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										milkAdminInstance.getServer().getPluginManager().enablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(parts[1]));
										json = "ok:"+(Lang.equals("en") ? parts[1]+" Plugin enabled":"Plugin "+parts[1]+" activado");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if(g.equals("/server/console")) {
									print(readConsole(), "text/plain");
								}
								else if(g.startsWith("/server/properties_edit")) {
									String[] parts = g.replace("?to", "").split("=");
									if(parts.length == 3){
										BukkitProperties.setString(parts[1], parts[2]);
										json = "ok:"+(Lang.equals("en") ? "Edited Property":"Propiedad Editada");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
											
									print(json, "text/plain");
								}
								else if(g.equals("/server/backup")){
									consoleCommand("save-all");
									DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH-mm-ss");
									Date date = new Date();
									String datez = dateFormat.format(date);
									new File("backups").mkdir();
									new File("backups/"+datez).mkdir();
									File srcFolder = new File(levelname);
									File destFolder = new File("backups/" + datez + "/" + levelname);
									try{
										copyFolder(srcFolder,destFolder);
										json = "ok:"+(Lang.equals("en") ? "Backed up!":"Mundo Respaldado");
										print(json, "text/plain");
									}catch(IOException e){
										if(Debug)
											System.out.println("[milkAdmin] ERROR in backup: " + e.getMessage());
										return;
									}
								}
								else if(g.startsWith("/server/restore")){
									String[] parts = g.split("=");
									json = "<html><head><meta HTTP-EQUIV=\"REFRESH\" content=\"20; url=/server/restore=" + parts[1] + "\">" + readFileAsString("./milkAdmin/html/wait."+Lang+".html");
									print(json, "text/html");
									milkAdminInstance.api.executeCommand(RTKInterface.CommandType.HOLD_SERVER);
								}
								else if(g.startsWith("/server/delete")){
									String[] parts = g.split("=");
									deleteDirectory(new File("backups/"+parts[1]));
									json = "ok:"+(Lang.equals("en") ? "Backup Deleted!":"Respaldo Eliminado");
									print(json, "text/plain");
								}
								else if(g.equals("/info/list_backups.json")){
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
								else if ( g.equals("/info/data.json") ){
									print(infoData(), "application/json");
								}
								////////////////
								//PLAYER AREA
								////////////////
								else if ( g.startsWith("/player/kick_user") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											milkAdminInstance.getServer().getPlayer(parts[1]).kickPlayer(KickedString);
											json = "ok:"+(Lang.equals("en") ? parts[1]+" kicked!":parts[1]+" fue expulsado de la partida");
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/give_item") ){
									String[] parts = g.replace("&item", "").replace("&amount", "").split("=");
									if(parts.length == 4){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											milkAdminInstance.getServer().getPlayer(parts[1]).getInventory().addItem(new ItemStack(Material.getMaterial(Integer.valueOf(parts[2])), Integer.valueOf(parts[3])));
											json = "ok:"+(Lang.equals("en") ? "Items given":"Items entregados");
											milkAdminInstance.getServer().getPlayer(parts[1]).sendMessage(GiveItemString + Material.getMaterial(Integer.valueOf(parts[2])));
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										} 
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}

									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/remove_item") ){
									String[] parts = g.replace("&item", "").replace("&amount", "").split("=");
									if(parts.length == 4){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											milkAdminInstance.getServer().getPlayer(parts[1]).getInventory().removeItem(new ItemStack(Material.getMaterial(Integer.valueOf(parts[2])), Integer.valueOf(parts[3])));
											json = "ok:"+(Lang.equals("en") ? "Items removed from the inventory":"Items eliminado del invetario");
											milkAdminInstance.getServer().getPlayer(parts[1]).sendMessage(Material.getMaterial(Integer.valueOf(parts[2])) + TakeAwayString);
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/get_health") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											json = "ok:"+String.valueOf(milkAdminInstance.getServer().getPlayer(parts[1]).getHealth());
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/set_health") ){
									String[] parts = g.replace("&amount", "").split("=");
									if(parts.length == 3){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											milkAdminInstance.getServer().getPlayer(parts[1]).setHealth(Integer.valueOf(parts[2]));
											if(parts[2].equals("0")){
												json = "ok:"+(Lang.equals("en") ? "Player Killed":"Jugador Asesinado");
											}else{
												json = "ok:"+(Lang.equals("en") ? "Health changed to ":"Vida cambiada a ")+parts[2]+"/20";
											}
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/shoot_arrow") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											milkAdminInstance.getServer().getPlayer(parts[1]).shootArrow();
											json = "ok:"+(Lang.equals("en") ? "Arrow Shoot!":"Flecha disparada!");
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.equals("/player/banlist.json") ){
									listBans();
								}
								else if ( g.startsWith("/player/ban_player") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										banList.setString(milkAdminInstance.getServer().getPlayer(parts[1]).getName().toString(), "true");
										milkAdminInstance.getServer().getPlayer(parts[1]).kickPlayer(BannedString);
										json = "ok:"+(Lang.equals("en") ? "Player Banned!":"Jugador Baneado!");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/ban_ip") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											banList.setString(String.valueOf(milkAdminInstance.getServer().getPlayer(parts[1]).getAddress()).split("/")[1].split(":")[0], "true");
											milkAdminInstance.getServer().getPlayer(parts[1]).kickPlayer(BannedString);
										}else{
											banList.setString(parts[1], "true");
										}
										json = "ok:"+(Lang.equals("en") ? "IP Banned!":"IP Baneada!");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/unban_player") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										banList.setString(milkAdminInstance.getServer().getPlayer(parts[1]).getName(), "false");
										json = "ok:"+(Lang.equals("en") ? "Player Unbanned":"Jugador Desbaneado!");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/unban_ip") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										banList.setString(parts[1], "false");
										json = "ok:"+(Lang.equals("en") ? "IP Unbanned":"IP Desbaneada!");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/ban") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										banList.setString(parts[1], "true");
										json = "ok:"+(Lang.equals("en") ? "Player Banned":"Jugador Baneado!");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/unban") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										banList.setString(parts[1], "false");
										json = "ok:"+(Lang.equals("en") ? "Player Unbanned":"Jugador Desbaneado!");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/delete") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										banList.removeKey(parts[1]);
										json = "ok:"+(Lang.equals("en") ? "Registry Deleted":"Registro Eliminado!");
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/throw_snowball") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											milkAdminInstance.getServer().getPlayer(parts[1]).throwSnowball();
											json = "ok:"+(Lang.equals("en") ? "Snowball thrown!":"Bola de nieve lanzada!");
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/throw_egg") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											milkAdminInstance.getServer().getPlayer(parts[1]).throwEgg();
											json = "ok:"+(Lang.equals("en") ? "Egg thrown!":"Huevo Lanzado!");
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/change_display_name") ){
									String[] parts = g.replace("&name", "").split("=");
									if(parts.length == 3){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											milkAdminInstance.getServer().getPlayer(parts[1]).setDisplayName(parts[2]);
											json = "ok:"+(Lang.equals("en") ? "A "+parts[1]+" se le cambio el nombre a "+parts[2]:parts[1]+"'s name changed to "+parts[2]);
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/teleport_to_player") ){
									String[] parts = g.replace("&to_user", "").split("=");
									if(parts.length == 3){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() && milkAdminInstance.getServer().getPlayer(parts[2]).isOnline()){
											milkAdminInstance.getServer().getPlayer(parts[1]).teleport(milkAdminInstance.getServer().getPlayer(parts[2]));
											json = "ok:"+(Lang.equals("en") ? "Player Teleported":"Jugador Teletransportado");
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/teleport_to_location") ){
									String[] parts = g.replace("&x", "").replace("&y", "").replace("&z", "").split("=");
									if(parts.length == 5){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											milkAdminInstance.getServer().getPlayer(parts[1]).teleport(new Location(milkAdminInstance.getServer().getPlayer(parts[1]).getWorld(), Integer.valueOf(parts[2]), Integer.valueOf(parts[3]), Integer.valueOf(parts[4])));
											json = "ok:"+(Lang.equals("en") ? "Player Teleported":"Jugador Teletransportado");
										}else{
											json = "error:"+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado");
										}
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/is_online") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										json = "ok:"+String.valueOf(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline());
									}else{
										json = "error:"+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos");
									}
									print(json, "text/plain");
								}
								else if ( g.startsWith("/player/get_ip_port.json") ){
									String[] parts = g.split("=");
									if(parts.length == 2){
										if(milkAdminInstance.getServer().getPlayer(parts[1]).isOnline() == true){
											String ip_port = String.valueOf(milkAdminInstance.getServer().getPlayer(parts[1]).getAddress()).split("/")[1];
											json = "{\"status\":\"ok\",\"ip\":\""+ip_port.split(":")[0]+"\",\"port\":\""+ip_port.split(":")[1]+"\"}";
										}else{
											json = "{\"status\":\"error\", \"error\":\""+(Lang.equals("en") ? "The player is not connected":"El jugador no esta conectado")+"\"}";
										}
									}else{
										json = "{\"status\":\"error\", \"error\":\""+(Lang.equals("en") ? "Bad Parameters":"Parametros erroneos")+"\"}";
									}
									print(json, "application/json");
								}
								else if( g.equals("/")){
									readFileAsBinary("./milkAdmin/html/index."+Lang+".html");
								}
								else{
									readFileAsBinary("./milkAdmin/html/" + g);
								}
							}
						}
					}
				} catch (Exception e) {
					if(Debug)
						System.out.println("[milkAdmin] ERROR in ServerParser: " + e);
				}
			}
		} catch (Exception e){
			if(Debug)
				System.out.println("[milkAdmin] ERROR in ServerInitialize: " + e.getMessage());
		}
	}
}