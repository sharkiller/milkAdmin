package com.sectorgamer.sharkiller.milkAdmin.objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;



import com.google.gson.*;
import com.sectorgamer.sharkiller.milkAdmin.util.Configuration;
import com.sectorgamer.sharkiller.milkAdmin.util.MilkAdminLog;

/**
 * Handle milkAdmin White list system.
 * @author Sharkiller
 */
public class Whitelist {
	/**
	 *
	 * sharkale31:
	 *   enabled: true
     *   vip:
     *     active: true
     *     donations:
     *       - '31/12/2000:23'
     *       - '06/08/2011:50'
     *   dates:
     *     register: 999999999
     *     lastlogin: 888888888
     *   referer:
     *     referedby: 'iL_nono'
     *     referers:
     *       - 'Geroo0'
     *       - 'Alexander'
     * warnings: 0
     * comments: 'groso'
     * likes: 50
	 *
	 **/
	static final String PluginDir = "plugins" + File.separator + "milkAdmin";
	private Configuration whitelist;
	private File file = new File(PluginDir + File.separator + "whitelist.yml");
	
	public Whitelist(){
		whitelist = new Configuration(file);
		
		if (file.exists()) {
			whitelist.load();
		} else {
			whitelist.save();
		}
	}
	
	public String removePlayer(String player){
		String result;
		if(keyExists(player)){
			whitelist.setProperty(player+".enabled", false);
			whitelist.save();
			result = "Jugador eliminado de la whitelist";
		}else{
			result = "El jugador no existe";
		}
		return result;
	}
	
	public String addDefaultPlayer(String player){
		String result;
		if(!keyExists(player)){
			Date today = Calendar.getInstance().getTime();
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yy");
			String register = formatter.format(today);
			result = setPlayer(player, true, null, null, register, null, null, null, null, null, null);
		}else{
			result = "El jugador ya existe y su cuenta está "+(whitelist.getBoolean(player+".enabled", false)?"activada":"desactivada");
		}
		return result;
	}
	
	public String setPlayer(String player, boolean enabled, String vip, List<String> donations, String register, String lastlogin, String refererby, String referers, String warnings, String comments, String likes){
		String result;

		whitelist.setProperty(player+".enabled", enabled);
		if(vip != null)
			whitelist.setProperty(player+".vip.active", vip);
		if(donations != null)
			whitelist.setProperty(player+".vip.donations", vip);
		
		if(register != null)
			whitelist.setProperty(player+".dates.register", register);
		if(lastlogin != null)
			whitelist.setProperty(player+".dates.lastlogin", lastlogin);
		
		if(refererby != null)
			whitelist.setProperty(player+".referer.refererby", refererby);
		if(referers != null)
			whitelist.setProperty(player+".referer.referers", referers);
		
		if(warnings != null)
			whitelist.setProperty(player+".warnings", warnings);
		if(comments != null)
			whitelist.setProperty(player+".comments", comments);
		if(likes != null)
			whitelist.setProperty(player+".likes", likes);

		whitelist.save();
		
		result = "ok";
		return result;
	}
	
	public void updateLastLogin(String player){
		Date today = Calendar.getInstance().getTime();
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yy");
		String lastlogin = Calendar.getInstance().getTimeInMillis()+","+formatter.format(today);
		whitelist.setProperty(player+".dates.lastlogin", lastlogin);
		whitelist.save();
	}
	
	private boolean keyExists(String key){
		if(whitelist.getKeys(key) != null)
			return true;
		else
			return false;
	}
	
	public boolean inWhitelist(String player){
		reload();
		if(keyExists(player)){
			return whitelist.getBoolean(player+".enabled", false);
		}else{
			return false;
		}
	}
	
	public void reload(){
		whitelist = new Configuration(file);
		whitelist.load();
	}
	
	public String getPlayer(String player){
		String result;
		Gson gson = new Gson();
		
		result = gson.toJson(player);
		return result;
	}
	
	public String exportDefault(){
		String result;
		
		result = "ok";
		return result;
	}
	
	public String importDefault(){
		String result;
		List<String> def = loadDefaultWhitelist();
		for(String player:def){
			addDefaultPlayer(player);
			MilkAdminLog.info("Imported: "+player);
		}
		result = "ok";
		return result;
	}
	
	public String count(){
		return String.valueOf(whitelist.getKeys(null).size());
	}
	
	private List<String> loadDefaultWhitelist() {
		String line;
		BufferedReader fin;
		List<String> players = new ArrayList<String>();

		try {
			fin = new BufferedReader(new FileReader("white-list.txt"));
		} catch (FileNotFoundException e) {
			MilkAdminLog.warning("ERROR in loadDefaultWhitelist()", e);
			return new ArrayList<String>();
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					players.add(line.trim());
			
		} catch (Exception e) {
			MilkAdminLog.warning("ERROR in loadDefaultWhitelist()", e);
			return new ArrayList<String>();
		} finally {
			try{
				fin.close();
			} catch (IOException e){
				MilkAdminLog.warning("ERROR in loadDefaultWhitelist()", e);
			}
		}
		return players;
	}
}
