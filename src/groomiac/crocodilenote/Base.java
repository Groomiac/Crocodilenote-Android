package groomiac.crocodilenote;

import groomiac.encryptor.PBKDF2;
import groomiac.encryptor.PRNGFixes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputType;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class Base extends Activity {
	final static int flags = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;
	final static String aes = "AES";
	final static String ecb = "AES/ECB/NoPadding";
	final static String cbc = "AES/CBC/PKCS5Padding";
	final static String hmac = "HmacSHA256";
	
	final static String file_secret = "secret.conf";
	
	final static String t_cancel = "Cancel";
	final static String t_ok = "Ok";
	final static String t_okay = "Okay";
	final static String t_yes = "Yes";
	final static String t_no = "No";
	final static String t_return = "Return";

	static boolean ENC = false;
	static boolean skiplauncher = false;
	
	private static int IS_INIT = 0;
	private static final int INITIALIZED = 42;

	protected static byte[] tmp_esk;
	protected static Cipher kcipher;
	protected static Mac ivMac;
	
	static void deinit(){
		IS_INIT = -99;
		propFile = null;
		props = null;
		secretpropFile = null;
		keyprops = null;
		
		props = new Properties();
		keyprops = new Properties();
	}
	
	static void logout(){
		new Random(System.nanoTime()).nextBytes(tmp_esk);
		try {
			if(kcipher != null)
				kcipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(tmp_esk, aes));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static boolean isInit(){
		return IS_INIT == INITIALIZED;
	}
	
	static ArrayList<Base> active = new ArrayList<Base>();
	static ArrayList<Base> active_del = new ArrayList<Base>();
	private static AlarmManager alarmMgr;
	private static PendingIntent alarmIntent;
	private static final long alarmTime = 1000 * 60 * 30;
	
	final void createLogoutTimer(){
		cancelLogoutTimer();
		
		if (alarmMgr != null && alarmIntent != null) {
			try {
				alarmMgr.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + (alarmTime), alarmIntent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	final static void cancelLogoutTimer(){
		if (alarmMgr != null && alarmIntent != null) {
			try {
				alarmMgr.cancel(alarmIntent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	Handler h = new Handler();
	Base me;
	
	static boolean actives(){
		ArrayList<Base> buf = new ArrayList<Base>();
		buf.addAll(active);
		for(Base b: buf){
			if(b.isFinishing()) active.remove(b);
		}
		
		buf = new ArrayList<Base>();
		buf.addAll(active_del);
		for(Base b: buf){
			if(b.isFinishing()) active_del.remove(b);
		}
		
		return active.size() != 0;
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if(ENC && isP(_P.timeout) && isInit()){
			actives();
			if(hasFocus){
				cancelLogoutTimer();
				active.add(me);
			}
			else{
				active.remove(me);
				active_del.add(me);
				if(!actives())
					createLogoutTimer();
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		active.remove(me);
		active_del.remove(me);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		me = this;
		
		if(IS_INIT != INITIALIZED){
			PRNGFixes.apply();
			
			ENC = false;
			skiplauncher = false;
			

			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);

			if (metrics != null && metrics.xdpi > 0 && metrics.ydpi > 0) {
				if (metrics.xdpi > metrics.densityDpi - 10
						&& metrics.xdpi < metrics.densityDpi + 5)
					factx = metrics.xdpi / 240f;
				else
					factx = metrics.densityDpi / 240f;

				if (metrics.ydpi > metrics.densityDpi - 10
						&& metrics.ydpi < metrics.densityDpi + 5)
					facty = metrics.ydpi / 240f;
				else
					facty = metrics.densityDpi / 240f;
			} else {
				factx = facty = metrics.densityDpi / 240f;
			}

			if (factx <= 0) {
				factx = 1;
			}

			if (facty <= 0) {
				facty = 1;
			}
			
			
			//TODO: write tmp file to test if storage is really there and writable
			setFolder(new File(Environment.getExternalStorageDirectory(), Main.name));
			getFolderfile().mkdirs();
			
			propFile = getNewfile("settings.conf");
			if(propFile.exists() && propFile.isFile()){
				try {
					FileInputStream fis = new FileInputStream(propFile);
					props.load(fis);
					fis.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if(isSet(_P.secrecy)){
				if(isP(_P.secrecy)){
					initSecretProps(getFilesDir());
					if(secretpropFile.exists() && secretpropFile.isFile()){
						try {
							FileInputStream fis = new FileInputStream(secretpropFile);
							keyprops.load(fis);
							fis.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						dialog_loadpw();
					}
					else{
						dialog_reintro();
					}
					
					try {
						alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
						alarmIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, AutoLogout.class), 0);
						cancelLogoutTimer();
					} catch (Exception e) {}
				}
				else{
					skiplauncher = true;
				}
			}
			else{
				dialog_intro();
			}
				
			IS_INIT = INITIALIZED;
		}
	}
	
	//Crypto
	private void loadpw(String pw){
		try {
			//Check key material
			byte[] tmp_ce, tmp_cd, tmp_ke, s;
			int it = getSecretInt(_P.i);
			tmp_ce = getSecretBytes(_P.ce);
			tmp_cd = getSecretBytes(_P.cd);
			tmp_ke = getSecretBytes(_P.ke);
			s = getSecretBytes(_P.s);

			ivMac = Mac.getInstance(hmac);
			ivMac.init(new SecretKeySpec(s, aes));
			
			Mac mac = Mac.getInstance(hmac);
			PBKDF2 ff = new PBKDF2(mac);
			
			byte[] pwkey = ff.generateDerivedParameters(256, pw.getBytes(), s, it);

			SecretKeySpec seckey = new SecretKeySpec(pwkey, aes);
			Cipher cipher = Cipher.getInstance(ecb);
			cipher.init(Cipher.DECRYPT_MODE, seckey);
			
			tmp_ke = cipher.doFinal(tmp_ke);
			seckey = new SecretKeySpec(tmp_ke, aes);
			cipher.init(Cipher.DECRYPT_MODE, seckey);
			
			tmp_ce = cipher.doFinal(tmp_ce);

			if(Arrays.equals(tmp_cd, tmp_ce)){
				KeyGenerator key_gen = KeyGenerator.getInstance(aes);
				key_gen.init(256);
				long max = System.currentTimeMillis() % 11;
				max++;
				for(int i=0; i < max; i++)
					key_gen.generateKey();
				SecretKey new_key = key_gen.generateKey();
				
				byte[] tmp_k = new_key.getEncoded();
				
				seckey = new SecretKeySpec(tmp_k, aes);
				cipher.init(Cipher.ENCRYPT_MODE, seckey);
				tmp_esk = cipher.doFinal(tmp_ke);
				
				
				kcipher = Cipher.getInstance(ecb);
				kcipher.init(Cipher.DECRYPT_MODE, seckey);
				
				if(me instanceof Launcher){
					startActivity(new Intent(me, Startup.class));
				}
				else{
					if(getIntent() != null)
						startActivity(getIntent());
					else
						startActivity(new Intent(me, getClass()));
				}
				me.finish();
				
				ENC = true;
				skiplauncher = true;
			}
			else{
				Toast.makeText(me, "Password is not correct!", Toast.LENGTH_LONG).show();
				
				deinit();
				finish();
			}
		} catch (Exception e) {
			Toast.makeText(me, "AES encryption not possible on your device!", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	//Properties
	static Properties props = new Properties();
	static Properties keyprops = new Properties();
	static File propFile, secretpropFile;

	static void saveProp(_P key, String val){
		saveProp(key.name(), val);
	}

	static void saveProp(String key, String val){
		try {
			props.setProperty(key, val);
			
			FileOutputStream fos = new FileOutputStream(propFile);
			props.store(fos, Main.name);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	static String getP(String key){
		return props.getProperty(key, null);
	}
	
	static String getP(_P key){
		return props.getProperty(key.name(), null);
	}
	
	static boolean isP(_P key){
		return "true".equals(props.getProperty(key.name()));
	}
	
	static boolean isSet(_P key){
		String tmp = (props.getProperty(key.name()));
		
		return (tmp != null && tmp.length() != 0);
	}
	
	static void savePropTrue(_P key){
		saveProp(key.name(), "true");
	}

	static void savePropFalse(_P key){
		saveProp(key.name(), "false");
	}

	//Properties for secrets
	static void saveSecret(_P key, String val){
		saveSecret(key.name(), val);
	}

	static void saveSecret(_P key, byte[] val){
		saveSecret(key.name(), Base64.encodeToString(val, flags));
	}

	static void saveSecret(_P key, int val){
		saveSecret(key.name(), val + "");
	}

	static void saveSecret(String key, String val){
		try {
			keyprops.setProperty(key, val);
			
			FileOutputStream fos = new FileOutputStream(secretpropFile);
			keyprops.store(fos, Main.name);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	static String getSecret(_P key){
		return keyprops.getProperty(key.name(), null);
	}
	
	static int getSecretInt(_P key){
		try {
			return Integer.parseInt(keyprops.getProperty(key.name(), null));
		} catch (Exception e) {
			return -1;
		}
	}
	
	static byte[] getSecretBytes(_P key){
		return Base64.decode(keyprops.getProperty(key.name(), null), flags);
	}
	
	static void initSecretProps(File path){
		secretpropFile = new File(path, file_secret);
	}
	
	
	private static String mainfolder = null;
	private static File folderfile = null;

	static final File getFolderfile() {
		return folderfile;
	}

	static final String getFolder() {
		return mainfolder;
	}

	static final void setFolder(String folder) {
		if (mainfolder == null) {
			mainfolder = folder;
			folderfile = new File(folder);
		}
	}

	static final void setFolder(File folder) {
		if (mainfolder == null) {
			mainfolder = folder.getAbsolutePath();
			folderfile = folder;
		}
	}

	static final File getNewfile(String filename) {
		return new File(folderfile, filename);
	}
	
	static final File getArchFolder(String folder){
		return new File(folder, Main.name + "_Export");
	}
	
	static final File getArchFolder(File folder){
		return new File(folder, Main.name + "_Export");
	}
	

	//UI related
	private static float factx = -1;
	private static float facty = -1;

	static final int scalemex(int px) {
		return (int) (factx * px);
	}

	static final int scalemey(int px) {
		return (int) (facty * px);
	}

	// UI Widgets
	abstract class StringResult{
		abstract void receive(String ret);
	}

	void makeStringDialog(String title, String hint, final StringResult sr) {
		makeStringDialog(title, hint, false, false, sr, null);
	}

	void makeStringDialog(String title, String hint, StringResult sr, StringResult cancelResult) {
		makeStringDialog(title, hint, false, false, sr, cancelResult);
	}

	void makeStringDialog(String title, final StringResult sr) {
		makeStringDialog(title, null, false, false, sr, null);
	}
	
	void makeYesnoDialog(String title, final StringResult sr) {
		makeStringDialog(title, null, false, true, sr, null);
	}

	void makeYesnoDialog(String title, final StringResult sr, StringResult cancel_sr) {
		makeStringDialog(title, null, false, true, sr, cancel_sr);
	}

	void makeStringDialog(String title, String hint, final boolean sanitize, final StringResult sr) {
		makeStringDialog(title, hint, sanitize, false, sr, null);
	}

	void makeStringDialog(String title, String hint, final boolean sanitize, final boolean yesnoonly, final StringResult sr, final StringResult cancelResult) {
		final AlertDialog ad = new AlertDialog.Builder(me).create();
		ad.setTitle(title);
		
		ad.setCancelable(false);

		LinearLayout rl = new LinearLayout(me);
		rl.setPadding(scalemex(5), scalemex(25), scalemex(5), scalemex(25));
		rl.setGravity(Gravity.CENTER_HORIZONTAL);

		final EditText tv = new EditText(me);
		final OnClickListener ok_action = new OnClickListener() {

			@Override
			public void onClick(View view) {
				if(!yesnoonly){
					Object o = tv.getText();
					if (o == null)
						return;
					String s = o.toString();
					if (s == null || s.length() == 0)
						return;

					if(sanitize)
						s = s.trim()
							.replace("\r", "")
							.replace("\n", "")
							.replace("\\", "_")
							.replace("/", "_")
						;

					if (s.length() == 0)
						return;
					
					if(sr != null) sr.receive(s);
				}
				else{
					if(sr != null) sr.receive(null);
				}
				
				ad.dismiss();
			}
		};

		if(!yesnoonly){
			if(hint != null) tv.setHint("   " + hint + "   ");
			else  tv.setHint("                 ");
			tv.setSingleLine(true);
			tv.setBackgroundColor(Color.LTGRAY);
			tv.setTextColor(Color.BLACK);

			rl.addView(tv);
			
			if(title != null && title.toLowerCase().contains("enter") && title.toLowerCase().contains("password"))
				tv.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			
			tv.setImeOptions(EditorInfo.IME_ACTION_DONE);
			tv.setOnEditorActionListener(new OnEditorActionListener() {
				
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						ok_action.onClick(null);

						return true;
					}
					return false;
				}
			});
			
			tv.requestFocus();
			ad.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		}

		Button bok = new Button(me);
		bok.setText("   Ok   ");
		bok.setTextColor(Color.BLACK);

		Button can = new Button(me);
		can.setText("Cancel");
		can.setTextColor(Color.BLACK);

		rl.addView(bok);
		rl.addView(can);

		can.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(cancelResult != null) cancelResult.receive(null);
				
				ad.dismiss();
			}
		});

		bok.setOnClickListener(ok_action);

		HorizontalScrollView ho = new HorizontalScrollView(me);
		ho.addView(rl);

		LinearLayout llmain = new LinearLayout(me);
		llmain.setGravity(Gravity.CENTER);
		llmain.addView(ho);
		llmain.setBackgroundColor(Color.WHITE);

		ad.setView(llmain);
		ad.show();

	}

	void makeHelloDialog(boolean cancel, String title, String text, final StringResult sr) {
		final AlertDialog ad = new AlertDialog.Builder(me).create();
		ad.setTitle(title);
		
		ad.setCancelable(cancel);

		RelativeLayout rl = new RelativeLayout(me);
		rl.setPadding(scalemex(5), scalemex(25), scalemex(5), scalemex(25));
		rl.setGravity(Gravity.CENTER_HORIZONTAL);
		rl.setBackgroundColor(Color.WHITE);
		
		LinearLayout ll = new LinearLayout(me);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(scalemex(5), 0, scalemex(5), 0);
		
		ScrollView sv = new ScrollView(me);
		TextView tv = new TextView(me);

		tv.setTextColor(Color.BLACK);
		tv.setText(text);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f);
		tv.setPadding(scalemex(10), scalemex(2), scalemex(10), scalemex(10));
		Linkify.addLinks(tv, Linkify.ALL);

		Button bok = new Button(me);
		bok.setText("Okay");
		bok.setTextColor(Color.BLACK);

		ll.addView(tv);
		ll.addView(bok);
		
		sv.addView(ll);
		
		bok.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(sr != null) sr.receive(null);
				ad.dismiss();
			}
		});

		rl.addView(sv);
		
		ad.setView(rl);
		ad.show();
	}

	static class DialogOpt{
		//Buttons
		String posB_label, negB_label, neuB_label;
		StringResult posB_sr, negB_sr, neuB_sr;
		OnClickListener posB_oc, negB_oc, neuB_oc;
		
		String title;
		boolean edittext, sanitize, cancelable;
		String et_hint;
		String et_pre;
		
		public void prepare(){
			if(title == null) title = Main.name;
			if(posB_label == null) posB_label = longer(t_ok);
			if(negB_label == null) negB_label = t_cancel;
			
			if(posB_label == t_yes) posB_label = longer(posB_label);
			if(negB_label == t_no) negB_label = longer(negB_label);
		}
		
		private String longer(String s){
			return "   " + s + "   ";
		}
		
		public static DialogOpt gen(){
			return new DialogOpt();
		}
	}
	
	void makeADialog(final DialogOpt opt) {
		if(opt == null) System.err.println("No dialog options specified!");
		else opt.prepare();
		
		final AlertDialog ad = new AlertDialog.Builder(me).create();
		ad.setTitle(opt.title);
		
		ad.setCancelable(opt.cancelable);
		ad.setCanceledOnTouchOutside(opt.cancelable);
	
		LinearLayout rl = new LinearLayout(me);
		rl.setPadding(scalemex(5), scalemex(25), scalemex(5), scalemex(25));
		rl.setGravity(Gravity.CENTER_HORIZONTAL);
	
		final EditText tv = new EditText(me);
		final OnClickListener ok_action = new OnClickListener() {
	
			@Override
			public void onClick(View v) {
				if(opt.edittext){
					Object o = tv.getText();
					if (o == null)
						return;
					String s = o.toString();
					if (s == null || s.length() == 0)
						return;
	
					if(opt.sanitize)
						s = s.trim()
							.replace("\r", "")
							.replace("\n", "")
							.replace("\\", "_")
							.replace("/", "_")
						;
	
					if (s.length() == 0)
						return;
					
					if(opt.posB_sr != null) opt.posB_sr.receive(s);
				}
				else{
					if(opt.posB_sr != null) opt.posB_sr.receive(null);
				}
				
				ad.dismiss();
			}
		};
	
		if(opt.edittext){
			if(opt.et_pre == null){
				if(opt.et_hint != null) tv.setHint("   " + opt.et_hint + "   ");
				else  tv.setHint("                 ");
			}
			else{
				tv.setText(opt.et_pre);
			}

			tv.setSingleLine(true);
			tv.setBackgroundColor(Color.LTGRAY);
			tv.setTextColor(Color.BLACK);
	
			rl.addView(tv);
			
			if(opt.title.toLowerCase().contains("enter") && opt.title.toLowerCase().contains("password"))
				tv.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

			tv.setImeOptions(EditorInfo.IME_ACTION_DONE);
			tv.setOnEditorActionListener(new OnEditorActionListener() {
				
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						ok_action.onClick(null);

						return true;
					}
					return false;
				}
			});
			
			tv.requestFocus();
			ad.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		}
	
		Button bok = new Button(me);
		bok.setText(opt.posB_label);
		bok.setTextColor(Color.BLACK);
	
		Button can = new Button(me);
		can.setText(opt.negB_label);
		bok.setTextColor(Color.BLACK);
	
		rl.addView(bok);
		rl.addView(can);
	
		if(opt.negB_oc != null)
			can.setOnClickListener(opt.negB_oc);
		else
			can.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(opt.negB_sr != null) opt.negB_sr.receive(null);
					
					ad.dismiss();
				}
			});
	
		if(opt.posB_oc != null)
			bok.setOnClickListener(opt.posB_oc);
		else
			bok.setOnClickListener(ok_action);
	
		HorizontalScrollView ho = new HorizontalScrollView(me);
		ho.addView(rl);
	
		LinearLayout llmain = new LinearLayout(me);
		llmain.setGravity(Gravity.CENTER);
		llmain.addView(ho);
		llmain.setBackgroundColor(Color.WHITE);
	
		ad.setView(llmain);
		ad.show();
	
	}

	void export(final String absoluteFolder){
		String archfolder = getP(_P.archfolder);
		File tmparch = null;
		
		if(archfolder != null){
			tmparch = new File(archfolder);
		}
		
		if(tmparch == null || !tmparch.exists() || !tmparch.isDirectory()){
			dialog_archie(absoluteFolder);
		}
		else{
			makeArchive(archfolder, absoluteFolder);
		}
	}
	
	void makeArchive(final String destinationFolder, final String sourceFolder) {
		new Thread(new Runnable() {
			long l = System.currentTimeMillis();

			@Override
			public void run() {
				try {
					String tmp;
					if(sourceFolder.equals(getFolder())){
						tmp = "Archive_ALL_" + System.currentTimeMillis() + ".zip";
					}
					else{
						String idname = new File(sourceFolder).getName();
						if(ENC) idname = idname.split("-")[0];
						tmp = "Archive_" + idname + "_" + System.currentTimeMillis() + ".zip";
					}
					
					new Zipper(new File(destinationFolder, tmp).getAbsolutePath(), sourceFolder).zip();
				} catch (Exception e) {
					e.printStackTrace();
				}

				h.post(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(
								me,
								"Archived ("
										+ (System.currentTimeMillis() - l)
										+ "ms) to\n" + destinationFolder, Toast.LENGTH_SHORT).show();
					}
				});

			}
		}).start();
	}

	//Dialogs
	void dialog_loadpw(){
		makeStringDialog("Enter your Password", "password", new StringResult() {
			
			@Override
			void receive(String ret) {
				loadpw(ret);
			}
		},
		
		new Base.StringResult() {
			
			@Override
			void receive(String ret) {
				deinit();
				finish();
				
			}
		}
				
		);
	}
	
	void dialog_createpw(){
		makeStringDialog("Create your Password", "password", new StringResult() {
			
			@Override
			void receive(String ret) {
				try {
					KeyGenerator key_gen = KeyGenerator.getInstance(aes);
					key_gen.init(256);
					long max = System.currentTimeMillis() % 11;
					max++;
					
					//Don't know if it is meaningful, but sure it does not hurt!
					for(int i=0; i < max; i++)
						key_gen.generateKey();
					SecretKey new_key = key_gen.generateKey();
					
					int its = new Random(System.currentTimeMillis()).nextInt(1000) + 6000;
					
					byte[] s = new_key.getEncoded();
					
					for(int i=0; i < max; i++)
						key_gen.generateKey();
					new_key = key_gen.generateKey();
					
					byte[] k = new_key.getEncoded();
					
					for(int i=0; i < max; i++)
						key_gen.generateKey();
					new_key = key_gen.generateKey();
					
					byte[] c_dec = new_key.getEncoded();
					
					Mac mac = Mac.getInstance(hmac);
					PBKDF2 ff = new PBKDF2(mac);
					
					byte[] pwkey = ff.generateDerivedParameters(256, ret.getBytes(), s, its);
					
					SecretKeySpec seckey = new SecretKeySpec(pwkey, aes);
					Cipher cipher = Cipher.getInstance(ecb);
					cipher.init(Cipher.ENCRYPT_MODE, seckey);
					
					saveSecret(_P.ke, Base64.encodeToString(cipher.doFinal(k), flags));
					
					seckey = new SecretKeySpec(k, aes);
					cipher.init(Cipher.ENCRYPT_MODE, seckey);

					saveSecret(_P.ce, Base64.encodeToString(cipher.doFinal(c_dec), flags));

					saveSecret(_P.cd, Base64.encodeToString(c_dec, flags));
					saveSecret(_P.s, Base64.encodeToString(s, flags));
					saveSecret(_P.i, its);
					
					props.setProperty(_P.timeout.name(), "true");
					savePropTrue(_P.secrecy);
					loadpw(ret);
					
				} catch (Exception e) {
					Toast.makeText(me, "AES encryption not possible on your device!", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
			}
		},
		
		new Base.StringResult() {
			
			@Override
			void receive(String ret) {
				deinit();
				finish();
			}
		}
				
		);
	}

	void dialog_intro(){
		dialog_intro(false);
	}

	void dialog_intro(final boolean showonly){
		makeHelloDialog(showonly, "Hello to CrocodileNote",
				"CrocodileNote supports two modes of operation - plaintext or encryption. You have to choose a mode to initilize CrocodileNote.\n" +
				"Notes are stored on the SD card in the 'CrocodileNote' folder for easy backup.\n\n" +
				"In encryption mode you have to provide a password which is used for password based encryption. We are using an encryption scheme for encryption-key derivation from the common standard PKCS#5 which is used for instance " +
				"in the famos TrueCrypt disk encryption software and others.\n" +
				"The encryption key is cached in working memory temporarily until Android finishes the app or you logout explicitly.\n" +
				"\nATTENTION: You should write down your password on a paper and keep the paper at a safe place for backup purposes. It is NOT possible to recover any of your data in case you forget " +
				"or loose your password. There is no backdoor or recovery possible to break the encryption (AES-256)!\n" +
				"A secure password contains mixed letters, numbers and special characters of at least 8(!) digits. We suggest you think of a unique sentence.\n",
				new StringResult() {
					
					@Override
					void receive(String ret) {
						if(!showonly) dialog_usepw();
					}
				});
	}
	
	void dialog_reintro(){
		makeHelloDialog(false, "Resetting CrocodileNote",
				"Welcome back to CrocodileNote. CrocodileNote has detected an existing and encrypted notes folder. However, the app seems to be missing your key file.\n" +
				"\nIf you want to reset CrocodileNote completely and delete all(!) existing notes, please remove the folder:\n" + getFolder() + "\n" +
				"\nOtherwise, please provide a key file to import.\n",
				new StringResult() {
					
					@Override
					void receive(String ret) {
						dialog_impkey(true);
					}
				});
	}
	
	void dialog_impkey(){
		dialog_impkey(false);
	}
	
	void dialog_impkey(final boolean cancel){
		DialogOpt opt = DialogOpt.gen();
		opt.title = "Provide the path to the key file";
		opt.edittext = true;
		try {
			opt.et_pre = Environment.getExternalStorageDirectory().getAbsolutePath();
			if(!opt.et_pre.endsWith(File.separator)) opt.et_pre += File.separator;
		} catch (Exception e) {
			opt.et_hint = "absolute path";
		}
		opt.posB_sr =
		new StringResult() {
			
			@Override
			void receive(String ret) {
				boolean valid = true;
				
				if(ret == null || ret.length() == 0) {
					valid = false;
				}
				else{
					File tmp_file = new File(ret);
					
					if(tmp_file.isDirectory() && tmp_file.exists()){
						tmp_file = new File(tmp_file, file_secret);
					}
					
					if(!tmp_file.exists() || !tmp_file.isFile() || !tmp_file.getName().equals(file_secret) || tmp_file.length() < 10){
						valid = false;
					}
					else{
						if(Utils.copyFile(tmp_file, secretpropFile)){
							props.setProperty(_P.timeout.name(), "true");
							savePropTrue(_P.secrecy);

							deinit();
							
							startActivity(new Intent(me, Launcher.class));
							finish();
						}
						else{
							valid = false;
						}
					}
				}

				if(!valid){
					dialog_impkey();
					Toast.makeText(me, "Not a valid key file", Toast.LENGTH_SHORT).show();
				}
			}
		};
		opt.negB_sr = 
		new StringResult() {
			
			@Override
			void receive(String ret) {
				if(!cancel) dialog_usepw();
				else {
					deinit();
					finish();
				}
			}
		};
		makeADialog(opt);
	}
	
	void dialog_chkexkey(){
		DialogOpt opt = DialogOpt.gen();
		opt.title = "Do you have a key file from a previous installation?";
		opt.posB_label = t_yes;
		opt.negB_label = t_no;
		opt.posB_sr =
		new StringResult() {
			
			@Override
			void receive(String ret) {
				initSecretProps(getFilesDir());
				dialog_impkey();
			}
		};
		opt.negB_sr = 
		new StringResult() {
			
			@Override
			void receive(String ret) {
				initSecretProps(getFilesDir());
				dialog_createpw();
			}
		};
		makeADialog(opt);
	}
	
	void dialog_usepw(){
		DialogOpt opt = DialogOpt.gen();
		opt.title = "Do you want to use encryption?";
		opt.posB_label = t_yes;
		opt.negB_label = t_no;
		opt.posB_sr =
		new StringResult() {
			
			@Override
			void receive(String ret) {
				dialog_chkexkey();
			}
		};
		opt.negB_sr = 
		new StringResult() {
			
			@Override
			void receive(String ret) {
				savePropFalse(_P.secrecy);
				skiplauncher = true;
				startActivity(new Intent(me, Launcher.class));
				finish();
			}
		};
		makeADialog(opt);
	}
	
	void dialog_archie(final String absoluteFolder){
		DialogOpt opt = DialogOpt.gen();
		opt.title = "Provide a writable SD folder";
		opt.cancelable = true;
		opt.edittext = true;
		try {
			opt.et_pre = Environment.getExternalStorageDirectory().getAbsolutePath();
			if(!opt.et_pre.endsWith(File.separator)) opt.et_pre += File.separator;
		} catch (Exception e) {
			opt.et_hint = "absolute path";
		}
		opt.posB_sr =
		new StringResult() {
			
			@Override
			void receive(String ret) {
				File tmp_folder = null;
				
				if(ret != null) tmp_folder = new File(ret);
				if(!tmp_folder.exists()) tmp_folder.mkdirs();
				
				File archfolder = getArchFolder(tmp_folder);
				
				try {
					archfolder.mkdir();
				} catch (Exception e) {
					Toast.makeText(me, "Error using this name", Toast.LENGTH_SHORT).show();
					return;
				}
				
				if(!archfolder.exists() || !archfolder.isDirectory()){
					Toast.makeText(me, "Export folder could not be created", Toast.LENGTH_LONG).show();
					return;
				}
				
				saveProp(_P.archfolder, archfolder.getAbsolutePath());
				makeArchive(archfolder.getAbsolutePath(), absoluteFolder);
			}
		};
		makeADialog(opt);
	}
}
