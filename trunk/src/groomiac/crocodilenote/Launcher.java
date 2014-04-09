package groomiac.crocodilenote;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;

public class Launcher extends Base {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle("Initializing " + Main.name + "...");
		
		LinearLayout ll = new LinearLayout(me);
		ll.setBackgroundColor(Color.BLACK);
		setContentView(ll);
		
		if(skiplauncher){
			startActivity(new Intent(me, Startup.class));
			me.finish();
		}
	}
}
