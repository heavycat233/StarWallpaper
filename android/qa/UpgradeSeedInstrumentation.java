package app.starfield.wallpaper.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Sets only legacy keys in the disposable emulator before the APK upgrade. */
public final class UpgradeSeedInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        SharedPreferences p=getTargetContext().getSharedPreferences("starfield",0);
        p.edit().putString("clock","digital").putString("uiMode","minimal").putInt("magnitude",8).putInt("fov",147).putInt("glow",148).putInt("dust",50).putInt("azimuth",270).putInt("altitude",90).putBoolean("followLocation",false).putBoolean("systemTime",true).putBoolean("twinkle",true).putBoolean("labels",false).putLong("lat",Double.doubleToLongBits(29.652)).putLong("lon",Double.doubleToLongBits(91.172)).putString("place","拉萨").putString("zone","Asia/Shanghai").remove("faintBrightness").commit();
        String version=getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).versionName;
        result.putString("stream","PASS seeded existing "+version+" settings for an in-place upgrade.\n");finish(Activity.RESULT_OK,result);
    }catch(Exception e){result.putString("stream",e.toString());finish(Activity.RESULT_CANCELED,result);}}
}
