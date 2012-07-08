package com.sectorgamer.sharkiller.milkAdmin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileMgmt {
	public static void checkFolders(String[] folders) {
		for (String folder : folders) {
			File f = new File(folder);
			if (!(f.exists() && f.isDirectory()))
				f.mkdir();
		}
	}
	
	public static void checkFiles(String[] files) throws IOException {
		for (String file : files) {
			File f = new File(file);
			if (!(f.exists() && f.isFile()))
				f.createNewFile();
		}
	}
	
	public static void copyDirectory(File sourceLocation , File targetLocation) throws IOException {
		if (sourceLocation.isDirectory()) {
			if (!targetLocation.exists())
				targetLocation.mkdir();
			            
			String[] children = sourceLocation.list();
			for (int i=0; i<children.length; i++)
				copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
		} else {      
			InputStream in = new FileInputStream(sourceLocation);
			OutputStream out = new FileOutputStream(targetLocation);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
				out.write(buf, 0, len);
			in.close();
			out.close();
		}
	}
	
	public static void zipDirectory(File sourceFolder, File destination) throws IOException {
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination));
		recursiveZipDirectory(sourceFolder, output);
		output.close();
	}

	public static void zipDirectories(File[] sourceFolders, File destination) throws IOException {
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination));
		for (File sourceFolder : sourceFolders)
			recursiveZipDirectory(sourceFolder, output);
		output.close();
	}
	
	public static void recursiveZipDirectory(File sourceFolder, ZipOutputStream zipStream) throws IOException {
		String[] dirList = sourceFolder.list();
		byte[] readBuffer = new byte[2156];
		int bytesIn = 0;
		for (int i = 0; i < dirList.length; i++) {
			File f = new File(sourceFolder, dirList[i]);
			if (f.isDirectory()) {
				recursiveZipDirectory(f, zipStream);
				continue;
			} else {
				FileInputStream input = new FileInputStream(f);
				ZipEntry anEntry = new ZipEntry(f.getPath());
				zipStream.putNextEntry(anEntry);
				while((bytesIn = input.read(readBuffer)) != -1)
					zipStream.write(readBuffer, 0, bytesIn);
				input.close();
			}
		}
	}
	
	public static boolean unziptodir(File zipdir, File dest){
		ZipFile zip;
		try {
			zip = new ZipFile(zipdir);
			return unzipFileIntoDirectory(zip, dest);
		} catch (ZipException e) {
			MilkAdminLog.warning("Failed to unzip!", e);
			return false;
		} catch (IOException e) {
			MilkAdminLog.warning("Failed to unzip!", e);
			return false;
		}
	}
	
	public static boolean unzipFileIntoDirectory(ZipFile zipFile, File jiniHomeParentDir) {
		Enumeration<?> files = zipFile.entries();
		File f = null;
		FileOutputStream fos = null;

		while (files.hasMoreElements()) {
			try {
				ZipEntry entry = (ZipEntry) files.nextElement();
				InputStream eis = zipFile.getInputStream(entry);
				byte[] buffer = new byte[1024];
				int bytesRead = 0;

				f = new File(jiniHomeParentDir.getAbsolutePath() + File.separator + entry.getName());

				if (entry.isDirectory()) {
					f.mkdirs();
					continue;
				} else {
					f.getParentFile().mkdirs();
					f.createNewFile();
				}

				fos = new FileOutputStream(f);

				while ((bytesRead = eis.read(buffer)) != -1) {
					fos.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
					}
				}	
			}
		}
		return true;
	}
	
	public static void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
        	MilkAdminLog.severe("Failed to copy resource!", e);
        }
    }

}
