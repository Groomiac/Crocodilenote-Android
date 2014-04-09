package groomiac.crocodilenote;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class Utils {
	
	public static String readFile(String file){
		FileInputStream fis = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 

		try {
			fis = new FileInputStream(new File(file));
			
			byte[] thearray = new byte[1024];
			int b = 0;
			
			while (true){
				try {
					b = fis.read(thearray);
					if (b>=0){
						baos.write(thearray, 0, b);
					}
					else{
						break;
					}
				} catch (Exception e) {
					break;
				}			
			}

		} catch (IOException e) {
			return null;
		} finally{
			try {
				if(fis!=null) fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return new String(baos.toString().trim()).replace("\r", "");
	}
	
	public static void writeFile(String string, String file){
		try {
			File outFile = new File(file);
			FileWriter out = new FileWriter(outFile);
			out.write(string);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public static String readFile(InputStream in){
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 

		try {
			byte[] thearray = new byte[1024];
			int b = 0;
			
			while (true){
				try {
					b = in.read(thearray);
					if (b>=0){
						baos.write(thearray, 0, b);
					}
					else{
						break;
					}
				} catch (Exception e) {
					break;
				}			
			}

		} catch (Exception e) {
			return null;
		} finally{
			try {
				if(in != null) in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return new String(baos.toString().trim()).replace("\r", "");
	}
	
	public static void writeFile(String string, OutputStream fos){
		try {
			fos.write(string.getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public static boolean copyFile(String src, String dest) {
		return copyFile(new File(src), new File(dest));
	}

	public static boolean copyFile(File src, File dest) {
		if(src == null || dest == null || !src.exists() || !src.isFile() || (dest.exists() && dest.isDirectory())) return false;
		
		FileInputStream fis = null;
		FileOutputStream fos = null;

		try {
			fis = new FileInputStream(src);
			fos = new FileOutputStream(dest);
			byte buffer[] = new byte[1024 * 4];
			int bytes;
			while (true) {
				bytes = fis.read(buffer);
				if (bytes <= -1) break;
				fos.write(buffer, 0, bytes);
			}
		} catch (Exception e) {
			System.err.println("error reading or writing: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (Exception e) {}
			if (fos != null)
				try {
					fos.flush();
					fos.close();
				} catch (Exception e) {}
		}
		
		if(dest.exists() && dest.isFile()) return true;
		return false;
	}
	
}
