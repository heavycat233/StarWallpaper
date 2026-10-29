package app.starfield.wallpaper.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import app.starfield.wallpaper.MainActivity;
import app.starfield.wallpaper.Settings;
import app.starfield.wallpaper.SkyMath;
import app.starfield.wallpaper.SkyPreview;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Test the real controls and visible GLES pixels on Android 17. */
public final class AnnotationInstrumentation extends Instrumentation {
    private final StringBuilder report=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean yes,String name){if(!yes)throw new AssertionError(name);report.append("PASS ").append(name).append('\n');}
    private Switch find(View view,String title){
        if(view instanceof Switch&&title.equals(((Switch)view).getText().toString()))return (Switch)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Switch found=find(group.getChildAt(i),title);if(found!=null)return found;}}
        return null;
    }
    private Bitmap capture(SkyPreview view)throws Exception{
        int old=view.renderer.renderedFrames;view.requestRender();long deadline=SystemClock.elapsedRealtime()+15000;
        while(view.renderer.renderedFrames<old+2&&view.renderer.error==null&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(50);
        check(view.renderer.error==null&&view.renderer.renderedFrames>=old+2,"GLES frame rendered");
        Bitmap image=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        CountDownLatch latch=new CountDownLatch(1);int[] status={-1};
        PixelCopy.request(view,image,result->{status[0]=result;latch.countDown();},new Handler(Looper.getMainLooper()));
        check(latch.await(15,TimeUnit.SECONDS)&&status[0]==PixelCopy.SUCCESS,"GPU frame captured");return image;
    }
    private long difference(Bitmap first,Bitmap second){
        int width=first.getWidth(),height=first.getHeight();int[] a=new int[width*height],b=new int[a.length];
        first.getPixels(a,0,width,0,0,width,height);second.getPixels(b,0,width,0,0,width,height);long difference=0;
        for(int i=0;i<a.length;i++)for(int shift=0;shift<=16;shift+=8)difference+=Math.abs(((a[i]>>shift)&255)-((b[i]>>shift)&255));
        return difference;
    }
    private void save(Bitmap image,String name)throws Exception{
        try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
    private void restore(SharedPreferences prefs,Map<String,?> original){
        SharedPreferences.Editor edit=prefs.edit().clear();
        for(Map.Entry<String,?> item:original.entrySet()){Object value=item.getValue();String key=item.getKey();
            if(value instanceof String)edit.putString(key,(String)value);else if(value instanceof Integer)edit.putInt(key,(Integer)value);else if(value instanceof Long)edit.putLong(key,(Long)value);else if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof Float)edit.putFloat(key,(Float)value);
        }edit.commit();
    }
    @Override public void onStart(){
        Bundle result=new Bundle();Settings settings=new Settings(getTargetContext());SharedPreferences prefs=settings.prefs;Map<String,?> original=new HashMap<>(prefs.getAll());Activity activity=null;
        try{
            check(getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).getLongVersionCode()==5,"Version code 5 installed");
            long epoch=java.time.Instant.parse("2026-10-28T17:30:00Z").toEpochMilli();double[] sir=SkyMath.equatorial(6.752481,-16.716116);double[] horizontal=null;
            for(int hour=0;hour<24;hour++){long candidate=epoch+hour*3600000L;double[] ray=SkyMath.mv(SkyMath.camera(candidate,22.3193,114.1694,0,90,180,320,680).local,sir);if(ray[2]>.45){epoch=candidate;horizontal=ray;break;}}
            check(horizontal!=null,"Sirius visible in fixed test sky");
            int az=(int)Math.round((Math.atan2(horizontal[0],horizontal[1])/SkyMath.D+360)%360),alt=(int)Math.round(Math.asin(horizontal[2])/SkyMath.D);
            final long fixedEpoch=epoch;
            prefs.edit().clear().putString("uiMode","minimal").putString("clock","none").putBoolean("systemTime",false).putLong("fixedEpoch",fixedEpoch).putInt("azimuth",az).putInt("altitude",alt).putBoolean("moon",false).putBoolean("nebulae",false).putBoolean("twinkle",false).putBoolean("labels",false).putBoolean("constellations",false).commit();
            activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));waitForIdleSync();
            Switch names=find(activity.getWindow().getDecorView(),"亮星名称"),figures=find(activity.getWindow().getDecorView(),"星座连线与名称");
            check(names!=null&&names.isEnabled()&&figures!=null&&figures.isEnabled(),"Both annotation switches are usable in minimal mode");
            runOnMainSync(names::performClick);waitForIdleSync();
            check(settings.snapshot().labels&&settings.snapshot().uiMode.equals("standard")&&names.isChecked(),"Bright-star switch enables names and standard mode");
            runOnMainSync(figures::performClick);waitForIdleSync();
            check(settings.snapshot().constellations&&figures.isChecked(),"Constellation switch enables figures");
            runOnMainSync(()->prefs.edit().putString("uiMode","minimal").apply());
            check(!settings.snapshot().labels&&!settings.snapshot().constellations,"Minimal mode hides annotation text");
            runOnMainSync(()->prefs.edit().putString("uiMode","standard").apply());
            check(settings.snapshot().labels&&settings.snapshot().constellations,"Standard mode restores annotation choices");
            Field field=MainActivity.class.getDeclaredField("preview");field.setAccessible(true);SkyPreview sky=(SkyPreview)field.get(activity);
            runOnMainSync(()->prefs.edit().putBoolean("labels",false).putBoolean("constellations",false).apply());Bitmap baseline=capture(sky);
            runOnMainSync(()->prefs.edit().putBoolean("labels",true).apply());Bitmap labelled=capture(sky);
            check(difference(baseline,labelled)>100,"Bright-star names change actual rendered pixels");save(labelled,"qa-star-names.png");labelled.recycle();
            runOnMainSync(()->prefs.edit().putBoolean("labels",false).putBoolean("constellations",true).apply());Bitmap lined=capture(sky);
            check(difference(baseline,lined)>1000,"Constellation lines and names change actual rendered pixels");save(lined,"qa-constellations.png");baseline.recycle();lined.recycle();
            runOnMainSync(()->prefs.edit().putBoolean("constellations",false).putBoolean("twinkle",true).putInt("twinkleStrength",0).apply());Bitmap still=capture(sky);
            runOnMainSync(()->prefs.edit().putInt("twinkleStrength",200).apply());Bitmap strong=capture(sky);
            check(settings.snapshot().twinkleStrength==200&&difference(still,strong)>100,"Twinkle strength changes actual star pixels");still.recycle();strong.recycle();
            Activity done=activity;runOnMainSync(done::finish);activity=null;result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}finally{restore(prefs,original);settings.close();}
    }
}
