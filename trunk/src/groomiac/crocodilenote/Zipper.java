package groomiac.crocodilenote;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Zipper {
	private String base, src, main;
	
	public Zipper(String base, String src){
		if(!src.endsWith(File.separator)) src += File.separator;

		this.base = base;
		this.src = src;
		
		main = new File(src).getName() + "/";
	}
	
	public void zip() throws Exception{
		ArrayList<File> list = new ArrayList<File>();
		list.addAll(Arrays.asList(new File(src).listFiles()));
		
		if(list.size() == 0) return;
		
		FileOutputStream dest = new FileOutputStream(base);
		ZipOutputStream out = new ZipOutputStream((dest));

		byte data[] = new byte[1024 * 16];
		
		ArrayList<File> list_walker = new ArrayList<File>();
		list_walker.addAll(list);

		while(list_walker.size() > 0){
			for (File f: list_walker) {
				if(f.isDirectory()) {
					list.addAll(Arrays.asList(f.listFiles()));
					continue;
				}
				
				FileInputStream fi = new FileInputStream(f);
				ZipEntry entry = new ZipEntry(saniZipPath(f.getAbsolutePath()));
				entry.setTime(f.lastModified());
				out.putNextEntry(entry);
				int count;
				while ((count = fi.read(data)) != -1) {
					out.write(data, 0, count);
				}
				out.flush();
				fi.close();
			}
			
			list.removeAll(list_walker);
			list_walker.clear();
			list_walker.addAll(list);
		}
		
		
		out.flush();
		out.close();
		dest.flush();
		dest.close();

	}
	
	private final String saniZipPath(String tmp){
		return main + tmp.replace(src, "").replace('\\', '/');
	}
}
