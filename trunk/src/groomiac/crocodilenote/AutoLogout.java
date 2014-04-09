package groomiac.crocodilenote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoLogout extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
		if(!Base.isInit()) return;
		
	    try {
	    	if(!Base.actives()){
				Base.logout();
				Base.deinit();
	    	}
	    	else{
	    		return;
	    	}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	    
	    try {
			for(Base b: Base.active_del){
				if(!b.isFinishing()){
					b.finish();
				}
			}
			Base.active_del.clear();
			Base.active.clear();
	    } catch (Throwable e) {
	    	e.printStackTrace();
	    }
	}
}
