package app.starfield.wallpaper.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import app.starfield.wallpaper.MainActivity;
import app.starfield.wallpaper.StarWallpaperService;

/** Integration fixture for the Android system's own live-wallpaper picker. */
public final class WallpaperInstrumentation extends Instrumentation {
    private boolean apply;
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);apply="true".equals(arguments.getString("apply"));start();}
    private void dump(AccessibilityNodeInfo node,StringBuilder out,int depth){if(node==null)return;out.append(" ".repeat(Math.min(16,depth))).append(node.getClassName()).append(" text=").append(node.getText()).append(" description=").append(node.getContentDescription()).append(" clickable=").append(node.isClickable()).append('\n');for(int i=0;i<node.getChildCount();i++)dump(node.getChild(i),out,depth+1);}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node,String exact){if(node==null)return null;if(node.getText()!=null&&node.getText().toString().equalsIgnoreCase(exact))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo result=find(node.getChild(i),exact);if(result!=null)return result;}return null;}
    private boolean click(String... labels){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();for(String label:labels){AccessibilityNodeInfo node=find(root,label);if(node==null)continue;while(node!=null&&!node.isClickable())node=node.getParent();if(node!=null)return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);}return false;}
    @Override public void onStart(){Bundle result=new Bundle();StringBuilder report=new StringBuilder();try{
        Intent main=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);Activity activity=startActivitySync(main);SystemClock.sleep(1500);click("Got it");SystemClock.sleep(300);Bitmap settings=getUiAutomation().takeScreenshot();if(settings!=null)try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"qa-settings-ui.png"))){settings.compress(Bitmap.CompressFormat.PNG,100,out);settings.recycle();}
        Intent picker=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(getTargetContext(),StarWallpaperService.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getTargetContext().startActivity(picker);SystemClock.sleep(3500);dump(getUiAutomation().getRootInActiveWindow(),report,0);
        Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap!=null)try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"qa-system-picker.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);bitmap.recycle();}
        if(apply){report.append("\nAPPLY FIRST: ").append(click("Set wallpaper","Apply","Set","设置壁纸","应用")).append('\n');SystemClock.sleep(1200);dump(getUiAutomation().getRootInActiveWindow(),report,0);report.append("\nAPPLY TARGET: ").append(click("Home screen and lock screen","Home and lock screens","Set wallpaper","Apply","设置壁纸","应用")).append('\n');SystemClock.sleep(2000);android.app.WallpaperInfo info=WallpaperManager.getInstance(getTargetContext()).getWallpaperInfo();boolean matched=info!=null&&info.getComponent().getClassName().equals(StarWallpaperService.class.getName());report.append("\nACTIVE WALLPAPER: ").append(info==null?"none":info.getComponent()).append("\nPASS_APPLIED=").append(matched).append('\n');result.putBoolean("applied",matched);}
        try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"qa-wallpaper-report.txt"))){out.write(report.toString().getBytes(StandardCharsets.UTF_8));}
        result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
