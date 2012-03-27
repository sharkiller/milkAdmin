package com.sectorgamer.sharkiller.milkAdmin.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

public class MilkAdminLog {
	private static final Logger logger = Bukkit.getLogger();

	public static void info(String msg) {
		logger.log(Level.INFO, "[milkAdmin] " + msg);
	}

	public static void warning(String msg) {
		logger.log(Level.WARNING, "[milkAdmin] " + msg);
	}

	public static void severe(String msg) {
		logger.log(Level.SEVERE, "[milkAdmin] " + msg);
	}
	
	public static void debug(String msg) {
		logger.log(Level.INFO, "[milkAdminDebug] " + msg);
	}

	public static void info(String msg, Throwable e) {
		logger.log(Level.INFO, "[milkAdmin] " + msg, e);
	}

	public static void warning(String msg, Throwable e) {
		logger.log(Level.WARNING, "[milkAdmin] " + msg, e);
	}

	public static void severe(String msg, Throwable e) {
		logger.log(Level.SEVERE, "[milkAdmin] " + msg, e);
	}
}
