package app.starfield.wallpaper.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.PixelCopy;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import app.starfield.wallpaper.MainActivity;
import app.starfield.wallpaper.PreviewActivity;
import app.starfield.wallpaper.Settings;
import app.starfield.wallpaper.SkyPreview;
import app.starfield.wallpaper.StarCatalog;

public final class SmokeInstrumentation extends Instrumentation {
    private StringBuilder checks=new StringBuilder();
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    void check(boolean condition,String name){if(!condition)throw new AssertionError(name);checks.append("PASS ").append(name).append('\n');}
    SkyPreview sky(Activity activity)throws Exception{Field field=activity.getClass().getDeclaredField(activity instanceof MainActivity?"preview":"sky");field.setAccessible(true);return (SkyPreview)field.get(activity);}
    void rendered(SkyPreview view)throws Exception{long start=SystemClock.elapsedRealtime();while(view.renderer.renderedFrames<3&&view.renderer.error==null&&SystemClock.elapsedRealtime()-start<60000)SystemClock.sleep(200);check(view.renderer.error==null,"GLES shaders initialize");check(view.renderer.renderedFrames>=3,"Native renderer produces frames");}
    Activity open(Class<? extends Activity> type){Intent intent=new Intent();intent.setComponent(new ComponentName(getTargetContext(),type));intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);return startActivitySync(intent);}
    void screenshot(SkyPreview view,String name)throws Exception{
        CountDownLatch ready=new CountDownLatch(1);Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);int[] status={-1};
        PixelCopy.request(view,bitmap,result->{status[0]=result;ready.countDown();},new Handler(Looper.getMainLooper()));check(ready.await(20,TimeUnit.SECONDS)&&status[0]==PixelCopy.SUCCESS,"PixelCopy "+name);
        int min=255,max=0,colored=0;for(int y=bitmap.getHeight()/2;y<bitmap.getHeight()-50;y+=9)for(int x=20;x<bitmap.getWidth()-20;x+=9){int p=bitmap.getPixel(x,y),r=(p>>16)&255,g=(p>>8)&255,b=p&255;min=Math.min(min,b);max=Math.max(max,b);if(b>r+3&&b>g)colored++;}
        check(max-min>12&&colored>100,"Sky pixels contain stars and blue galactic background "+name);
        File file=new File(getTargetContext().getExternalFilesDir(null),name);try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    @Override public void onStart(){Bundle result=new Bundle();Activity activity=null;Settings settings=null;
        try{
            settings=new Settings(getTargetContext());StarCatalog catalogue=StarCatalog.get(getTargetContext());check(catalogue.count>900000,"Deep catalogue contains more than 900,000 stars");check(catalogue.counts[11]==catalogue.count&&catalogue.counts[3]<200,"Magnitude limits and complete packed catalogue");check(!app.starfield.wallpaper.LocationTracker.foregroundAllowed(getTargetContext()),"App starts without requesting location access");
            settings.prefs.edit().clear().putInt("fps",5).apply();
            activity=open(MainActivity.class);rendered(sky(activity));screenshot(sky(activity),"qa-settings-sky.png");
            Activity main=activity;Method refresh=MainActivity.class.getDeclaredMethod("refresh");refresh.setAccessible(true);SharedPreferences preferences=settings.prefs;
            runOnMainSync(()->{preferences.edit().putInt("magnitude",11).putInt("fov",180).putInt("azimuth",90).putInt("altitude",0).putBoolean("labels",true).putString("uiMode","minimal").apply();try{refresh.invoke(main);}catch(Exception e){throw new RuntimeException(e);}});
            check(!settings.snapshot().labels&&settings.snapshot().altitude==0,"Minimal mode suppresses names and preserves horizon angle");
            runOnMainSync(()->preferences.edit().putString("uiMode","standard").apply());check(settings.snapshot().labels,"Standard mode restores label preference");
            runOnMainSync(()->{preferences.edit().putString("clock","digital").putString("uiMode","minimal").putInt("magnitude",9).putInt("fov",147).putInt("azimuth",270).putInt("altitude",90).putBoolean("labels",false).apply();});
            activity=open(PreviewActivity.class);rendered(sky(activity));screenshot(sky(activity),"qa-minimal-digital.png");SkyPreview paused=sky(activity);Activity finished=activity;runOnMainSync(finished::finish);waitForIdleSync();int before=paused.renderer.renderedFrames;SystemClock.sleep(650);check(paused.renderer.renderedFrames==before,"Paused preview stops drawing");
            runOnMainSync(()->preferences.edit().putString("clock","analog").apply());activity=open(PreviewActivity.class);rendered(sky(activity));screenshot(sky(activity),"qa-minimal-analog.png");Activity analog=activity;runOnMainSync(analog::finish);waitForIdleSync();
            runOnMainSync(()->preferences.edit().putString("clock","digital").putString("uiMode","standard").putInt("magnitude",11).putInt("fov",180).putInt("azimuth",235).putInt("altitude",43).putLong("lat",Double.doubleToLongBits(-33.8688)).putLong("lon",Double.doubleToLongBits(151.2093)).putString("place","悉尼").putString("zone","Australia/Sydney").putBoolean("systemTime",false).putLong("fixedEpoch",java.time.Instant.parse("2026-06-21T12:30:00Z").toEpochMilli()).apply());
            activity=open(PreviewActivity.class);rendered(sky(activity));screenshot(sky(activity),"qa-standard-sydney.png");
            Activity last=activity;runOnMainSync(last::finish);waitForIdleSync();settings.prefs.edit().clear().commit();
            activity=main;runOnMainSync(()->{try{refresh.invoke(main);}catch(Exception e){throw new RuntimeException(e);}});SystemClock.sleep(500);rendered(sky(activity));Bitmap app=getUiAutomation().takeScreenshot();if(app!=null){try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"qa-settings-ui.png"))){app.compress(Bitmap.CompressFormat.PNG,100,out);}app.recycle();}
            result.putString("screenshots",getTargetContext().getExternalFilesDir(null).getAbsolutePath());result.putString("stream",checks.toString());result.putBoolean("passed",true);finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream",checks+"FAIL "+android.util.Log.getStackTraceString(e));result.putBoolean("passed",false);finish(Activity.RESULT_CANCELED,result);}
        finally{if(activity!=null){Activity finalActivity=activity;runOnMainSync(finalActivity::finish);}if(settings!=null){settings.prefs.edit().clear().apply();settings.close();}}
    }
}
