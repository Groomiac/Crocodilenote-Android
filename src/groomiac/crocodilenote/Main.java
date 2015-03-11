package groomiac.crocodilenote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class Main extends Base {
	public static final String name = "CrocodileNote";

	private int max = 1;

	private EditText curfocus = null;

	private static String thefolder = null;
	private static String foldername = null;
	private static String foldershowname = null;
	
	//Folder from folder list(view)
	protected static void setAbsoluteFolder(String s) {
		setAbsoluteFolder(new File(s));
	}

	protected static void setAbsoluteFolder(File f) {
		thefolder = f.getAbsolutePath();
		foldername = f.getName();
	}
	
	protected static String getAbsoluteFolder(){
		return thefolder;
	}
	
	protected static void setFolderShowname(String s) {
		foldershowname = s;
	}


	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu_main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.removeall:
			makeYesnoDialog("Delete all texts here?", new StringResult() {
				
				@Override
				void receive(String ret) {
					for (int i = 0; i < max; i++) {
						file(i).delete();
					}
					
					if(getIntent() != null) startActivity(getIntent());
					else startActivity(new Intent(me, Main.class));
					finish();
				}
			});
			return true;
			
		case R.id.linki:
			if (curfocus != null) {
				dialogie(curfocus.getText().toString());
			}
			return true;
			
		case R.id.archoption:
			export(getAbsoluteFolder());
			
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private RelativeLayout l1;
	private LinearLayout l2;
	private Vector<InnerEdit> thetexts;

	private OnEditorActionListener one = new OnEditorActionListener() {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				int ctr = 0;
				for (int i = 0; i < max; i++) {
					if (thetexts.get(i).changed || !checkFiles(i)) {
						storeFiles(i, thetexts.get(i).edit.getText().toString());
						ctr++;
						thetexts.get(i).changed = false;
					}
				}
				Toast.makeText(me, "Saved (" + ctr + ")", Toast.LENGTH_SHORT)
						.show();

				return true;
			}
			return false;
		}
	};

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(isP(_P.secrecy) && !ENC){
			setTitle("Initializing " + Main.name + "...");
			return;
		}
		
		Intent caller = getIntent();
		if(caller != null){
			try {
				String tmp = caller.getStringExtra(_I.Main_setAbsoluteFolder.name());
				if(tmp == null){
					finish();
					return;
				}
				setAbsoluteFolder(tmp);
				
				tmp = caller.getStringExtra(_I.Main_setFolderShowname.name());
				if(tmp == null){
					finish();
					return;
				}
				setFolderShowname(tmp);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		
		WatchImpl.enable = false;

		l1 = new RelativeLayout(this);

		int xi = 0;
		while (true) {
			if (!checkFiles(xi))
				break;
			xi++;
		}
		if (xi > max)
			max = xi;
		
		thetexts = new Vector<InnerEdit>(max + 5);

		ScrollView sv = new ScrollView(this);
		l1.addView(sv);
		
		RelativeLayout.LayoutParams sv_lp = (RelativeLayout.LayoutParams) sv.getLayoutParams();
		sv_lp.width = RelativeLayout.LayoutParams.MATCH_PARENT;
		sv_lp.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		sv_lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		sv.setLayoutParams(sv_lp);
		
		l2 = new LinearLayout(this);
		l2.setOrientation(LinearLayout.VERTICAL);
		l2.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		sv.addView(l2);

		
		ArrayList<String> vals = null;
		ArrayList<Integer> ids = null;
		int newone = -1;
		int focusone = -1;
		
		if(caller != null){
			if(caller.getBooleanExtra("OKAY", false)){
				vals = caller.getStringArrayListExtra("vals");
				ids  = caller.getIntegerArrayListExtra("ids");
				newone = caller.getIntExtra("newone", -1);
				focusone = caller.getIntExtra("curfocus", -1);
			}
		}
		
		if(newone > 0) max = newone + 1;
		
		setTitle(title(max));
		
		for (int i = 0; i < max; i++) {
			EditText t2 = new EditText(this);
			t2.setId(i);
			t2.setOnFocusChangeListener(new OnFocusChangeListener() {

				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus)
						curfocus = (EditText) v;
				}
			});
			t2.setImeOptions(EditorInfo.IME_ACTION_DONE);
			t2.setOnEditorActionListener(one);
			InnerEdit in = new InnerEdit(t2);
			thetexts.add(in);
			t2.addTextChangedListener(new WatchImpl(in));
			l2.addView(t2, 0);
			if(ids != null && ids.contains(i)){
				String tmp = vals.get(ids.indexOf(i));
				if(tmp != null) t2.setText(tmp);
				else t2.setText("err");
				in.changed = true;
			}
			else{
				if (checkFiles(i)) {
					String x = loadFiles(i);
					t2.setText(x);
				} else {
					t2.setText("");

				}
			}

			if(focusone >= 0){
				if (i == focusone)
					t2.requestFocus();
			}
			else{
				if (i == max - 1)
					t2.requestFocus();
			}
		}

		WatchImpl.enable = true;

		Button b1 = new Button(this);
		b1.setText("  Save  ");

		b1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int ctr = 0;
				for (int i = 0; i < max; i++) {
					if (thetexts.get(i).changed || !checkFiles(i)) {
						storeFiles(i, thetexts.get(i).edit.getText().toString());
						ctr++;
						thetexts.get(i).changed = false;
					}
				}
				Toast.makeText(me, "Saved (" + ctr + ")", Toast.LENGTH_SHORT)
						.show();

			}
		});

		Button b2 = new Button(this);
		b2.setText("  Add  ");

		b2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText t2 = new EditText(me);
				t2.setOnFocusChangeListener(new OnFocusChangeListener() {

					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus)
							curfocus = (EditText) v;
					}
				});
				t2.setImeOptions(EditorInfo.IME_ACTION_DONE);
				t2.setOnEditorActionListener(one);

				l2.addView(t2, 0);
				t2.setId(max);
				max++;
				InnerEdit in = new InnerEdit(t2, true);
				thetexts.add(in);
				t2.addTextChangedListener(new WatchImpl(in));
				setTitle(title(max));
				t2.requestFocus();
			}
		});

		Button b3 = new Button(this);
		b3.setText("Save & Exit");
		b3.setOnClickListener(new OnClickListener() {
			//TODO: reduce redundancy
			
			@Override
			public void onClick(View v) {
				int ctr = 0;
				for (int i = 0; i < max; i++) {
					if (thetexts.get(i).changed || !checkFiles(i)) {
						storeFiles(i, thetexts.get(i).edit.getText().toString());
						ctr++;
						thetexts.get(i).changed = false;
					}
				}
				if (ctr > 0)
					Toast.makeText(me, "Saved (" + ctr + ")",
							Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(me, "Quick Exit", Toast.LENGTH_SHORT).show();

				finish();
			}
		});

		Button b4 = new Button(this);
		b4.setText("Return");

		b4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				for (int i = 0; i < max; i++) {
					if (thetexts.get(i).changed || !checkFiles(i)) {
						Toast.makeText(me, "unsaved data", Toast.LENGTH_SHORT)
								.show();
						return;
					}
				}

				startActivity(new Intent(me, Startup.class));
				finish();
			}
		});

		HorizontalScrollView hori = new HorizontalScrollView(this);
		LinearLayout ll_hori = new LinearLayout(this);
		ll_hori.setOrientation(LinearLayout.HORIZONTAL);

		ll_hori.addView(b1);
		ll_hori.addView(b3);
		ll_hori.addView(b2);
		ll_hori.addView(b4);

		hori.addView(ll_hori);
		l1.addView(hori);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		hori.setLayoutParams(lp);
		hori.setId(121232);
		
		sv_lp = (RelativeLayout.LayoutParams) sv.getLayoutParams();
		sv_lp.addRule(RelativeLayout.ABOVE, hori.getId());
		sv.setLayoutParams(sv_lp);

        BitmapDrawable bd = new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.back_base));
        bd.setTileModeXY(TileMode.MIRROR, TileMode.REPEAT);
        l1.setBackgroundDrawable(bd);

		setContentView(l1);
	}

	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		ArrayList<String> vals = new ArrayList<String>();
		ArrayList<Integer> ids = new ArrayList<Integer>();
		int newone = 0;
		
		for (int i = 0; i < max; i++) {
			if(!checkFiles(i)){
				newone = i;
				ids.add(i);
				vals.add(thetexts.get(i).edit.getText().toString());
			}
			else if(thetexts.get(i).changed) {
				ids.add(i);
				vals.add(thetexts.get(i).edit.getText().toString());
			}
		}
		
		if(newone > 0) outState.putInt("newone", newone);
		outState.putStringArrayList("vals", vals);
		outState.putIntegerArrayList("ids", ids);
		if(curfocus != null) outState.putInt("curfocus", curfocus.getId());
		outState.putBoolean("OKAY", true);
	}

	static class WatchImpl implements TextWatcher {
		static boolean enable = true;

		InnerEdit in;

		public WatchImpl(InnerEdit in) {
			this.in = in;
		}

		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if (in != null && enable)
				in.changed = true;
		}
	}

	static class InnerEdit {
		public InnerEdit(EditText edit) {
			this(edit, false);
		}

		public InnerEdit(EditText edit, boolean changed) {
			super();
			this.edit = edit;
			this.changed = changed;
		}

		private EditText edit;
		private boolean changed;
	}

	@Override
	public void onBackPressed() {
		AlertDialog ad = new AlertDialog.Builder(this).create();
		ad.setCancelable(false);
		ad.setMessage("Do you really want to quit?");
		ad.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						me.finish();
					}
				});
		ad.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						dialog.dismiss();
					}
				});
		ad.setButton(AlertDialog.BUTTON_NEUTRAL, "Return",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						me.finish();
						startActivity(new Intent(me, Startup.class));
					}
				});
		ad.show();
	}

	//File (notes) operations
	static void storeEncFiles(int i, String s) {
		try {
			Cipher c = Cipher.getInstance(cbc);
			IvParameterSpec ivSpec = new IvParameterSpec(genIV(filename(i)));
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kcipher.doFinal(tmp_esk), aes), ivSpec);
			
			CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(file(i)), c);
			Utils.writeFile(s, cos);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static void storeFiles(int i, String s) {
		new File(getAbsoluteFolder()).mkdirs();
		
		if(ENC)
			storeEncFiles(i, s);
		else
			storePlainFiles(i, s);
	}

	static void storePlainFiles(int i, String s) {
		//Reading on Android by filtering \r, but this enables best practice for viewing with Windows notepad
		Utils.writeFile(s.replace("\r", "").replace("\n", "\r\n"), file(i).getAbsolutePath());
	}

	static boolean checkFiles(int i) {
		return file(i).exists();
	}
	
	static byte[] genIV(String filename){
		String salt = foldername.toLowerCase() + "/" + filename;
		byte[] ret = new byte[16];
		System.arraycopy(ivMac.doFinal(salt.getBytes()), 0, ret, 0, 16);
		return ret;
	}
	
	static String loadEncFiles(int i) {
		try {
			Cipher c = Cipher.getInstance(cbc);
			IvParameterSpec ivSpec = new IvParameterSpec(genIV(filename(i)));
			c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(kcipher.doFinal(tmp_esk), aes), ivSpec);
			
			CipherInputStream cis = new CipherInputStream(new FileInputStream(file(i)), c);
			return Utils.readFile(cis);
		} catch (Exception e) {
			return null;
		}
	}
	
	static String loadPlainFiles(int i) {
		return Utils.readFile(file(i).getAbsolutePath());
	}
	
	static String loadFiles(int i) {
		if(ENC)
			return loadEncFiles(i);
		else
			return loadPlainFiles(i);
	}
	
	static File file(int i){
		return new File(getAbsoluteFolder(), filename(i));
	}

	private void dialogie(String s) {
		if (s == null || s.length() == 0)
			return;

		AlertDialog ad = new AlertDialog.Builder(me).create();
		ad.setCancelable(true);
		ad.setCanceledOnTouchOutside(true);
		ad.setTitle("Linkification view");

		LinearLayout rl = new LinearLayout(me);
		rl.setBackgroundColor(Color.WHITE);
		rl.setPadding(scalemex(5), scalemex(25), scalemex(5), scalemex(25));

		TextView tv;

		tv = new TextView(me);
		rl.addView(tv);
		tv.setBackgroundColor(Color.WHITE);
		tv.setTextColor(Color.BLACK);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f);
		tv.setText(s);
		Linkify.addLinks(tv, Linkify.ALL);

		ScrollView ho = new ScrollView(me);
		ho.addView(rl);

		ad.setView(ho);
		ad.show();

	}

	private final static String filename(int i) {
		return "data_" + i + ".dat";
	}

	private final static String title(int i) {
		return name + ": " + foldershowname + " (" + i + ")";
	}

}
