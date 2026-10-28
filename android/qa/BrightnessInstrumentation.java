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
import android.widget.SeekBar;
import app.starfield.wallpaper.MainActivity;
import app.starfield.wallpaper.PreviewActivity;
import app.starfield.wallpaper.Settings;
import app.starfield.wallpaper.SkyPreview;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Pixel-based verification of the actual GLES implementation, with time and twinkle fixed. */
public final class BrightnessInstrumentation extends Instrumentation {
    private final StringBuilder report=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean condition,String name){if(!condition)throw new AssertionError(name);report.append("PASS ").append(name).append('\n');}
    private SkyPreview sky(Activity a)throws Exception{Field f=a.getClass().getDeclaredField(a instanceof MainActivity?"preview":"sky");f.setAccessible(true);return (SkyPreview)f.get(a);}
    private void frames(SkyPreview view)throws Exception{int previous=view.renderer.renderedFrames;long deadline=SystemClock.elapsedRealtime()+15000;view.requestRender();while(view.renderer.renderedFrames<previous+3&&view.renderer.error==null&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(50);check(view.renderer.error==null&&view.renderer.renderedFrames>=previous+3,"GLES shader and frame update");}
    private Bitmap capture(SkyPreview view,String filename)throws Exception{frames(view);Bitmap b=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);CountDownLatch latch=new CountDownLatch(1);int[] result={-1};PixelCopy.request(view,b,r->{result[0]=r;latch.countDown();},new Handler(Looper.getMainLooper()));check(latch.await(15,TimeUnit.SECONDS)&&result[0]==PixelCopy.SUCCESS,"GPU pixel readback "+filename);if(filename!=null)try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),filename))){b.compress(Bitmap.CompressFormat.PNG,100,out);}return b;}
    private long[] delta(Bitmap a,Bitmap b){int count=a.getWidth()*a.getHeight();int[] x=new int[count],y=new int[count];a.getPixels(x,0,a.getWidth(),0,0,a.getWidth(),a.getHeight());b.getPixels(y,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());long signed=0,absolute=0;for(int i=0;i<count;i++)for(int shift=0;shift<=16;shift+=8){int difference=((y[i]>>shift)&255)-((x[i]>>shift)&255);signed+=difference;absolute+=Math.abs(difference);}return new long[]{signed,absolute};}
    private void restore(SharedPreferences p,Map<String,?> values){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?> item:values.entrySet()){Object v=item.getValue();String key=item.getKey();if(v instanceof String)e.putString(key,(String)v);else if(v instanceof Integer)e.putInt(key,(Integer)v);else if(v instanceof Long)e.putLong(key,(Long)v);else if(v instanceof Boolean)e.putBoolean(key,(Boolean)v);else if(v instanceof Float)e.putFloat(key,(Float)v);}e.putInt("faintBrightness",180).commit();}
    @Override public void onStart(){Bundle result=new Bundle();Settings s=new Settings(getTargetContext());SharedPreferences p=s.prefs;Map<String,?> original=new HashMap<>(p.getAll());Activity main=null,preview=null;try{
        Settings.Snapshot initial=s.snapshot();check(initial.faintBrightness==180,"Upgrade defaults dark stars to 180 percent");check(initial.magnitude==8&&initial.fov==147&&initial.azimuth==270&&initial.altitude==90&&initial.uiMode.equals("minimal")&&initial.place.equals("拉萨"),"Upgrade preserves 8th magnitude, location, field of view, direction and minimal mode");check(getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).getLongVersionCode()==2,"Version code 2 installed in place");
        main=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));Field sliders=MainActivity.class.getDeclaredField("sliders");sliders.setAccessible(true);SeekBar slider=(SeekBar)((HashMap<?,?>)sliders.get(main)).get("faintBrightness");check(slider!=null&&slider.getMin()==50&&slider.getMax()==400&&slider.getProgress()==180,"Native settings expose the persisted 50–400 percent control");
        runOnMainSync(()->p.edit().putString("clock","none").putString("uiMode","minimal").putBoolean("twinkle",false).putBoolean("systemTime",false).putLong("fixedEpoch",System.currentTimeMillis()/60000*60000).putInt("magnitude",8).putInt("faintBrightness",100).apply());
        preview=startActivitySync(new Intent(getTargetContext(),PreviewActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));SkyPreview view=sky(preview);Bitmap originalPixels=capture(view,"qa-faint-100.png");
        runOnMainSync(()->p.edit().putInt("faintBrightness",180).apply());Bitmap enhanced=capture(view,"qa-faint-180.png");long[] raised=delta(originalPixels,enhanced);check(raised[0]>10000,"180 percent visibly increases faint-star RGB energy (delta="+raised[0]+")");enhanced.recycle();
        runOnMainSync(()->p.edit().putInt("faintBrightness",50).apply());Bitmap reduced=capture(view,null);long[] lowered=delta(originalPixels,reduced);check(lowered[0]<-10000,"50 percent reduces faint-star RGB energy (delta="+lowered[0]+")");reduced.recycle();originalPixels.recycle();
        runOnMainSync(()->p.edit().putInt("faintBrightness",100).putInt("magnitude",3).apply());Bitmap brightBefore=capture(view,null);
        runOnMainSync(()->p.edit().putInt("faintBrightness",400).apply());Bitmap brightAfter=capture(view,null);long[] bright=delta(brightBefore,brightAfter);check(bright[1]==0,"Bright stars and sky background remain pixel-identical at 100 vs 400 percent");brightBefore.recycle();brightAfter.recycle();
        runOnMainSync(()->p.edit().putInt("faintBrightness",999).apply());check(s.snapshot().faintBrightness==400,"Stored brightness clamps safely to control range");
        runOnMainSync(()->p.edit().putString("clock","digital").putInt("magnitude",8).putInt("faintBrightness",180).apply());Bitmap demo=capture(view,"qa-faint-default-180.png");demo.recycle();
        Activity last=preview;runOnMainSync(last::finish);preview=null;waitForIdleSync();restore(p,original);Activity first=main;runOnMainSync(first::finish);main=null;result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}finally{restore(p,original);s.close();}}
}
