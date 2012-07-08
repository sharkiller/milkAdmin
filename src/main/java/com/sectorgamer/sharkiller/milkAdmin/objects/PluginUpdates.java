package com.sectorgamer.sharkiller.milkAdmin.objects;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;

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
		
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		String date;
		try {
			date = format.parse(getElementValue(element,"pubDate")).toString();
		} catch (ParseException e) {
			date = "????";
		}
		
		String json = "[";
		json = json + "{\"title\":\""+getElementValue(element,"title")+"\"," +
					  "\"link\":\""+getElementValue(element,"link")+"\"," +
					  "\"pubDate\":"+date+"}";
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
