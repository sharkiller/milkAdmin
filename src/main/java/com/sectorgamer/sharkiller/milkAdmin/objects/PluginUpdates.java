package com.sectorgamer.sharkiller.milkAdmin.objects;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.*;
import javax.xml.parsers.*;

import org.bukkit.plugin.*;

/**
 * Handle milkAdmin Ban list system.
 * @author Sharkiller
 */
public class PluginUpdates {
	private PluginManager PM;
	
	public PluginUpdates(PluginManager pm){
		this.PM = pm;
	}
	
	public String getLatest(String plugin){
		Plugin p = PM.getPlugin(plugin);
		if(p != null){
			NodeList rss = getRSS(getName(plugin));
			String json;
			if(rss != null)
				json = RSS2JSON(rss);
			else
				json = "[{\"error\":\"plugindevpagenotfound\"}]";
			return json;
		}else{
			return "[{\"error\":\"pluginnotexist\"}]";
		}
	}
	
	public String RSS2JSON(NodeList rss){
		Element element = (Element)rss.item(0);
		
		SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");//Sun, 08 Jul 2012 10:11:31 +0000
		SimpleDateFormat format2 = new SimpleDateFormat("dd/MM/yyyy");
		Date dateparse;
		String date="";
		try {
			format.setLenient(true);
			date = getElementValue(element,"pubDate").substring(5,16);
			dateparse = format.parse(date);
			date = format2.format(dateparse);
		} catch (ParseException e) {
			date = "????";
		}
		
		String json = "[";
		json = json + "{\"title\":\""+getElementValue(element,"title")+"\"," +
					  "\"link\":\""+getElementValue(element,"link")+"\"," +
					  "\"pubDate\":\""+date+"\"}";
		json = json + "]";
		
		return json;
	}
	private String getCharacterDataFromElement(Element e) {
		try {
			Node child = e.getFirstChild();
			if(child instanceof CharacterData) {
				CharacterData cd = (CharacterData) child;
				return cd.getData();
			}
		}
		catch(Exception ex) {}
		return "";
	}
	protected float getFloat(String value) {
		if(value != null && !value.equals("")) {
			return Float.parseFloat(value);
		}
		return 0;
	}
	protected String getElementValue(Element parent,String label) {
		return getCharacterDataFromElement((Element)parent.getElementsByTagName(label).item(0));
	}
	
	public NodeList getRSS(String pluginNameURL){
		String url = "http://dev.bukkit.org/server-mods/"+pluginNameURL+"/files.rss";
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			URL u = new URL(url);
			Document doc = builder.parse(u.openStream());
			NodeList nodes = doc.getElementsByTagName("item");
			return nodes;
		} catch(Exception ex) {
			return null;
		}
	}
	
	public String getName(String pluginName){
		String pluginNameURL = pluginName.toLowerCase();
		
		int i = pluginsName.length / 2;
		for(int j = 0;j < i-1; j=j+2){
			if(pluginsName[j] == pluginName){
				pluginNameURL = pluginsName[j+1];
				break;
			}
		}
		
		return pluginNameURL;
	}
	
	public static final String pluginsName[] = {
    	"EssentialsChat", 			"essentials",
    	"EssentialsGeoIP", 			"essentials",
    	"EssentialsProtect", 		"essentials",
    	"EssentialsSpawn", 			"essentials",
    	"EssentialsXMPP", 			"essentials",
    	"Towny", 					"towny-advanced",
    	"VanishNoPacket", 			"vanish",
    	
    	"-NOT-",		 			"-COPY-"
    };  
}
