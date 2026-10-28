package app.starfield.wallpaper.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import app.starfield.wallpaper.LocationTracker;
import app.starfield.wallpaper.MainActivity;
import app.starfield.wallpaper.Settings;

/** Only used against the disposable emulator with synthetic GPS coordinates. */
public final class LocationInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();Settings settings=new Settings(getTargetContext());LocationTracker tracker=new LocationTracker(getTargetContext(),settings);Activity activity=null;
        try{
            if(!LocationTracker.foregroundAllowed(getTargetContext()))throw new AssertionError("Fixture location permission missing");
            settings.flag("followLocation",true);activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(()->tracker.start(true));Bundle ready=new Bundle();ready.putString("stream","READY synthetic GPS; waiting for 114.1694,22.3193\n");sendStatus(1,ready);
            long start=SystemClock.elapsedRealtime();boolean matched=false;while(SystemClock.elapsedRealtime()-start<40000){Settings.Snapshot s=settings.snapshot();if(Math.abs(s.lat-22.3193)<.001&&Math.abs(s.lon-114.1694)<.001&&s.place.equals("当前位置")){matched=true;break;}SystemClock.sleep(200);}
            if(!matched)throw new AssertionError("GPS callback did not update coordinates: "+tracker.status);
            tracker.stop();settings.flag("followLocation",false);settings.position(29.652,91.172,"拉萨","Asia/Shanghai");settings.prefs.edit().commit();
            Activity done=activity;runOnMainSync(done::finish);result.putString("stream","PASS runtime LocationManager receives emulator GPS and persists real coordinates.\nPASS switching follow off preserves manual location fallback.\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}finally{tracker.stop();settings.close();}
    }
}
