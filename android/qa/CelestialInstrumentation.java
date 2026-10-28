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
public final class CelestialInstrumentation extends Instrumentation {
    private final StringBuilder report=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean condition,String name){if(!condition)throw new AssertionError(name);report.append("PASS ").append(name).append('\n');}
    private SkyPreview sky(Activity a)throws Exception{Field f=a.getClass().getDeclaredField(a instanceof MainActivity?"preview":"sky");f.setAccessible(true);return (SkyPreview)f.get(a);}
    private void frames(SkyPreview view)throws Exception{int previous=view.renderer.renderedFrames;long deadline=SystemClock.elapsedRealtime()+15000;view.requestRender();while(view.renderer.renderedFrames<previous+3&&view.renderer.error==null&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(50);check(view.renderer.error==null&&view.renderer.renderedFrames>=previous+3,"GLES shader and frame update");}
    private Bitmap capture(SkyPreview view,String filename)throws Exception{frames(view);Bitmap b=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);CountDownLatch latch=new CountDownLatch(1);int[] result={-1};PixelCopy.request(view,b,r->{result[0]=r;latch.countDown();},new Handler(Looper.getMainLooper()));check(latch.await(15,TimeUnit.SECONDS)&&result[0]==PixelCopy.SUCCESS,"GPU pixel readback "+filename);if(filename!=null)try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),filename))){b.compress(Bitmap.CompressFormat.PNG,100,out);}return b;}
    private long[] delta(Bitmap a,Bitmap b){int count=a.getWidth()*a.getHeight();int[] x=new int[count],y=new int[count];a.getPixels(x,0,a.getWidth(),0,0,a.getWidth(),a.getHeight());b.getPixels(y,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());long signed=0,absolute=0;for(int i=0;i<count;i++)for(int shift=0;shift<=16;shift+=8){int difference=((y[i]>>shift)&255)-((x[i]>>shift)&255);signed+=difference;absolute+=Math.abs(difference);}return new long[]{signed,absolute};}
    private void restore(SharedPreferences p,Map<String,?> values){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?> item:values.entrySet()){Object v=item.getValue();String key=item.getKey();if(v instanceof String)e.putString(key,(String)v);else if(v instanceof Integer)e.putInt(key,(Integer)v);else if(v instanceof Long)e.putLong(key,(Long)v);else if(v instanceof Boolean)e.putBoolean(key,(Boolean)v);else if(v instanceof Float)e.putFloat(key,(Float)v);}e.commit();}
    private android.widget.Button findButton(android.view.View v){if(v instanceof android.widget.Button&&((android.widget.Button)v).getText().toString().equals("载入对话预览参数"))return (android.widget.Button)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){android.widget.Button b=findButton(g.getChildAt(i));if(b!=null)return b;}}return null;}
    @Override public void onStart(){Bundle result=new Bundle();Settings s=new Settings(getTargetContext());SharedPreferences p=s.prefs;Map<String,?> original=new HashMap<>(p.getAll());Activity preview=null;try{
        check(getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),0).getLongVersionCode()==4,"Version code 4 installed as upgrade");
        Activity main=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));frames(sky(main));waitForIdleSync();
        runOnMainSync(()->{android.widget.Button button=findButton(main.getWindow().getDecorView());if(button==null)throw new AssertionError("Preview preset action exists");button.performClick();});waitForIdleSync();
        Settings.Snapshot matched=s.snapshot();check(matched.place.equals("香港")&&Math.abs(matched.lat-22.3193)<.00001&&matched.magnitude==9&&matched.fov==180&&matched.faintBrightness==120&&matched.brightSize==70&&matched.azimuth==94&&matched.altitude==79&&!matched.systemTime&&matched.fixedEpoch==java.time.Instant.parse("2026-10-28T17:30:00Z").toEpochMilli(),"One-tap action restores exact conversation date, location, view and brightness");runOnMainSync(main::finish);waitForIdleSync();
        runOnMainSync(()->p.edit().clear().putString("clock","digital").putString("uiMode","minimal").putBoolean("twinkle",false).putBoolean("systemTime",false).putLong("fixedEpoch",java.time.Instant.parse("2026-10-28T17:30:00Z").toEpochMilli()).putInt("azimuth",94).putInt("altitude",79).apply());
        check(s.snapshot().faintBrightness==120&&s.snapshot().fov==180&&s.snapshot().brightSize==70,"New install defaults match selected preview");
        preview=startActivitySync(new Intent(getTargetContext(),PreviewActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));SkyPreview view=sky(preview);
        check(app.starfield.wallpaper.StarCatalog.get(getTargetContext()).count==115570,"Exact preview HYG v4.1 catalogue loaded");
        Bitmap all=capture(view,"qa-celestial-night.png");
        double[] horizontal=app.starfield.wallpaper.CelestialLayer.horizontal(java.time.Instant.parse("2026-10-28T17:30:00Z").toEpochMilli(),29.652,91.172);
        check(horizontal[1]>56&&horizontal[1]<58,"Topocentric lunar altitude agrees with preview");
        runOnMainSync(()->p.edit().putBoolean("moon",false).apply());Bitmap noMoon=capture(view,null);long[] moonDelta=delta(noMoon,all);check(moonDelta[0]>50000,"Moon adds visible illuminated surface pixels: "+moonDelta[0]);all.recycle();
        runOnMainSync(()->p.edit().putBoolean("nebulae",false).apply());Bitmap neither=capture(view,null);long[] nebulaDelta=delta(neither,noMoon);check(nebulaDelta[0]>10000,"Real-position nebulae add visible emission pixels: "+nebulaDelta[0]);noMoon.recycle();
        runOnMainSync(()->p.edit().putInt("brightSize",100).apply());Bitmap large=capture(view,null);long[] sizeDelta=delta(neither,large);check(sizeDelta[0]>10000,"70 percent bright-star cores have less pixel area than 100 percent: "+sizeDelta[0]);neither.recycle();large.recycle();
        runOnMainSync(()->p.edit().putBoolean("moon",true).putBoolean("nebulae",true).putInt("brightSize",70).putString("clock","analog").apply());Bitmap analog=capture(view,"qa-celestial-analog.png");analog.recycle();
        runOnMainSync(()->p.edit().putString("clock","digital").putLong("fixedEpoch",java.time.Instant.parse("2026-10-28T04:00:00Z").toEpochMilli()).apply());Bitmap below=capture(view,null);
        runOnMainSync(()->p.edit().putBoolean("moon",false).apply());Bitmap hidden=capture(view,null);check(delta(below,hidden)[1]==0,"Moon below horizon leaves no disk or glow");below.recycle();hidden.recycle();
        Activity done=preview;runOnMainSync(done::finish);preview=null;result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}finally{restore(p,original);s.close();}}
}
