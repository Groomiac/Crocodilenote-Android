package groomiac.crocodilenote;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Startup extends Base {
	private LinearLayout rl;
	private ArrayList<FolderItem> l = new ArrayList<FolderItem>();
	
	
	private OnItemClickListener mode_normal = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
				long arg3) {
			
			startMain(getNewfile(l.get(pos).getReal()), l.get(pos).getShow());
		}
	};
	
	private OnItemClickListener mode_delete = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
				long arg3) {
			
			final int thepos = pos;
			
			makeYesnoDialog("Really delete '" + l.get(pos).getShow() + "'?", new Base.StringResult() {
				
				@Override
				void receive(String ret) {
					new Remover(getNewfile(l.get(thepos).getReal())).removeDir();
					l.remove(thepos);
					ba.notifyDataSetChanged();
				}
			});
		}
	};
	
	private BaseAdapter ba;
	private ListView lv;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(isP(_P.secrecy) && !ENC){
			setTitle("Initializing " + Main.name + "...");
			return;
		}

		rl = new LinearLayout(this);
		setContentView(rl);
		
        BitmapDrawable bd = new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.back_base));
        bd.setTileModeXY(TileMode.MIRROR, TileMode.REPEAT);
        rl.setBackgroundDrawable(bd);
		
		RelativeLayout rlbuttons = new RelativeLayout(me);
		rlbuttons.setGravity(Gravity.RIGHT);
		
		rl.setOrientation(LinearLayout.VERTICAL);

		Button b = new Button(this);
		b.setId(556688);
		Button b_del = new Button(this);

		Button b_logout = new Button(this);
		b_logout.setText("  Logout  ");
		
		if(!ENC){
			b_logout.setVisibility(View.GONE);
		}
		
		b_logout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				logout();
				deinit();
				
				finish();
			}
		});

		rlbuttons.addView(b);
		rlbuttons.addView(b_logout);
		rlbuttons.addView(b_del);
		
		RelativeLayout.LayoutParams lpr = (RelativeLayout.LayoutParams)b.getLayoutParams();
		lpr.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		b.setLayoutParams(lpr);
		
		lpr = (RelativeLayout.LayoutParams)b_logout.getLayoutParams();
		lpr.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		b_logout.setLayoutParams(lpr);
		
		lpr = (RelativeLayout.LayoutParams)b_del.getLayoutParams();
		lpr.addRule(RelativeLayout.RIGHT_OF, b.getId());
		b_del.setLayoutParams(lpr);
		

		rl.addView(rlbuttons);
		
		
		lv = new ListView(me);
		rl.addView(lv);
		
		lv.setBackgroundColor(Color.TRANSPARENT);
		lv.setCacheColorHint(Color.TRANSPARENT);

		LayoutParams lp;

		lp = (LayoutParams) lv.getLayoutParams();
		lp.height = LayoutParams.MATCH_PARENT;
		lp.width = LayoutParams.MATCH_PARENT;
		lv.setLayoutParams(lp);


		b.setText("     Add     ");
		b_del.setText("Delete");
		
		b_del.setOnClickListener(new OnClickListener() {
			boolean del = false;
			
			@Override
			public void onClick(View v) {
				if(del){
					((Button)v).setTextColor(Color.BLACK);
					lv.setOnItemClickListener(mode_normal);
				}
				else{
					((Button)v).setTextColor(Color.RED);
					lv.setOnItemClickListener(mode_delete);
				}
				
				del = !del;
			}
		});

		b.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				makeStringDialog("Create folder", "folder name", true, new Base.StringResult() {
					
					@Override
					void receive(String ret) {
						try {
							if(!ENC) getNewfile(ret).mkdir();
						} catch (Exception e) {
							Toast.makeText(me, "Error using this name", Toast.LENGTH_SHORT).show();
							return;
						}

						FolderItem fi = createnew(ret);

						startMain(getNewfile(fi.getReal()), fi.getShow());
					}
				});
				
			}
		});

		lv.setPadding(scalemex(5), scalemex(5), scalemex(5), scalemex(5));

		lv.setAdapter(new BaseAdapter() {
			private final String main = "___Main___";

			{
				getFolderfile().listFiles(new FileFilter() {

					@Override
					public boolean accept(File pathname) {
						if (!pathname.isDirectory())
							return false;

						l.add(new FolderItem(pathname.getName(), null));
						return true;
					}
				});

				if (l.size() == 0){
					if(!ENC) getNewfile(main).mkdirs();
					l.add(createnew(main));
				}

				Collections.sort(l, new Comparator<FolderItem>() {

					@Override
					public int compare(FolderItem lfi, FolderItem rfi) {
						if (lfi == null || rfi == null)
							return 0;

						String lhs = lfi.getShow();
						String rhs = rfi.getShow();
						
						if (main.equals(lhs))
							return -1;
						if (lhs == rhs)
							return 0;
						if (lhs == null && rhs == null)
							return 0;
						if (lhs == null)
							return -1;
						if (rhs == null)
							return -1;

						lhs = lhs.toLowerCase();
						rhs = rhs.toLowerCase();

						if (lhs.startsWith("_") && lhs.startsWith("_"))
							return lhs.compareTo(rhs);

						if (lhs.startsWith("_"))
							return -1;
						if (rhs.startsWith("_"))
							return 1;

						return lhs.compareTo(rhs);
					}

				});

			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView tv;

				if (convertView != null)
					tv = (TextView) convertView;
				else {
					tv = new TextView(me);
					tv.setPadding(scalemex(15), scalemex(25), scalemex(15), scalemex(25));
					tv.setTextColor(Color.BLACK);
					tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
				}

				tv.setText(l.get(position).getShow());

				return tv;
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public Object getItem(int position) {
				return position;
			}

			@Override
			public int getCount() {
				return l.size();
			}
		});
		
		ba = (BaseAdapter) lv.getAdapter();

		lv.setOnItemClickListener(mode_normal);

	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu_startup, menu);
		
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(!ENC){
			menu.removeItem(R.id.timeout);
			menu.removeItem(R.id.expkey);
		}
		else{
			for(int i=0; i<menu.size(); i++){
				MenuItem mi = menu.getItem(i);
				if(mi.getItemId() == R.id.timeout){
					if(isP(_P.timeout)){
						mi.setTitle(getResources().getString(R.string.m_s_timeout_on));
					}
					else{
						mi.setTitle(getResources().getString(R.string.m_s_timeout_off));
					}
				}
			}
		}
		
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.archall:
			export(getFolder());

			return true;
			
		case R.id.help:
			dialog_intro(true);
			
			return true;
			
		case R.id.expkey:
			makeYesnoDialog("Backup key file to SD card?", new StringResult() {
				
				@Override
				void receive(String ret) {
					File tmp_ext = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), file_secret);
					
					if(Utils.copyFile(secretpropFile, tmp_ext)){
						makeHelloDialog(true, "Key backup", "Your personal secrets file is secured by your password. Nevertheless, you should remove the backup copy after you have copied it to a safe place.\n" +
								"\nThe secrets file copy is:\n" + tmp_ext.getAbsolutePath() + "\n", null);
					}
					else{
						Toast.makeText(me, "Unknown error copying key file", Toast.LENGTH_SHORT).show();
					}
					
				}
			});
			
			return true;
		
		case R.id.timeout:
			if(isP(_P.timeout)){
				savePropFalse(_P.timeout);
				cancelLogoutTimer();
			}
			else{
				savePropTrue(_P.timeout);
			}
			
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void startMain(File real, String show){
		Intent inte = new Intent(me, Main.class);
		inte.putExtra(_I.Main_setAbsoluteFolder.name(), real.getAbsolutePath());
		inte.putExtra(_I.Main_setFolderShowname.name(), show);
		
		startActivity(inte);
		me.finish();
	}
	
	static class FolderItem{
		
		private String showname, realname;
		
		public FolderItem(String path, String nickname){
			realname = path;
			showname = nickname;
		}
		
		public String getShow(){
			if(ENC){
				if(showname == null){
					showname = loadInfo(getNewfile(realname).getAbsolutePath(), realname);
				}

				if(showname == null) showname = "Unknown";
				
				return showname;
			}
			return realname;
		}

		public String getReal(){
			return realname;
		}
	}
	
	public static FolderItem createnew(String nickname){
		if(ENC){
			String path = UUID.randomUUID().toString();
			File tmp = getNewfile(path);
			while(tmp.exists()){
				path = UUID.randomUUID().toString();
				tmp = getNewfile(path);
			}
			
			tmp.mkdirs();
			
			storeInfo(getNewfile(path).getAbsolutePath(), path, nickname);
			
			return new FolderItem(path, nickname);
		}
		else{
			return new FolderItem(nickname, null);
		}
	}

	private static void storeInfo(String folder, String salt, String showname) {
		try {
			Cipher c = Cipher.getInstance(cbc);
			IvParameterSpec ivSpec = new IvParameterSpec(genIV(salt));
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kcipher.doFinal(tmp_esk), aes), ivSpec);
			
			CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(new File(folder, ".info")), c);
			Utils.writeFile(showname, cos);
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
	
	private static String loadInfo(String folder, String salt) {
		try {
			Cipher c = Cipher.getInstance(cbc);
			IvParameterSpec ivSpec = new IvParameterSpec(genIV(salt));
			c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(kcipher.doFinal(tmp_esk), aes), ivSpec);
			
			CipherInputStream cis = new CipherInputStream(new FileInputStream(new File(folder, ".info")), c);
			return Utils.readFile(cis);
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	static byte[] genIV(String salt){
		byte[] ret = new byte[16];
		System.arraycopy(ivMac.doFinal(salt.getBytes()), 0, ret, 0, 16);
		return ret;
	}
}
