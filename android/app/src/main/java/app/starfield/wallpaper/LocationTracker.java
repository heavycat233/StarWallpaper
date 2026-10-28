package app.starfield.wallpaper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.TimeZone;

/** Foreground updates, or low-frequency visible-wallpaper updates with explicit background grant. */
public final class LocationTracker implements LocationListener {
    private final Context context;private final Settings settings;private final LocationManager manager;
    private final Handler handler=new Handler(Looper.getMainLooper());private CancellationSignal current;
    public volatile String status="手动位置";
    public LocationTracker(Context context,Settings settings){this.context=context;this.settings=settings;manager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);}
    public static boolean foregroundAllowed(Context c){return c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED||c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    public static boolean backgroundAllowed(Context c){return c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    public void start(boolean activityVisible){
        stop();if(!settings.snapshot().followLocation||!foregroundAllowed(context)){status="使用手动位置";return;}
        if(!activityVisible&&!backgroundAllowed(context)){status="打开 App 时更新位置";return;}
        status="正在获取位置…";
        boolean fine=context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
        Location latest=null;String currentProvider=null;
        for(String provider:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER})try{
            if(provider.equals(LocationManager.GPS_PROVIDER)&&!fine)continue;
            if(!manager.isProviderEnabled(provider))continue;
            if(currentProvider==null)currentProvider=provider;
            Location known=manager.getLastKnownLocation(provider);if(known!=null&&(latest==null||known.getElapsedRealtimeNanos()>latest.getElapsedRealtimeNanos()))latest=known;
            manager.requestLocationUpdates(provider,activityVisible?60000:600000,1000,this,Looper.getMainLooper());
        }catch(SecurityException|IllegalArgumentException ignored){}
        if(latest!=null&&SystemClock.elapsedRealtimeNanos()-latest.getElapsedRealtimeNanos()<6L*3600*1000000000L)onLocationChanged(latest);
        if(currentProvider!=null){try{current=new CancellationSignal();manager.getCurrentLocation(currentProvider,current,context.getMainExecutor(),location->{if(location!=null)onLocationChanged(location);else status="暂未定位，保留最近位置";});CancellationSignal request=current;handler.postDelayed(()->{if(current==request&&request!=null)request.cancel();},30000);}catch(SecurityException|IllegalArgumentException ignored){status="暂未定位，保留最近位置";}}
        else status="系统定位未开启，保留手动位置";
    }
    @Override public void onLocationChanged(Location location){
        if(!settings.snapshot().followLocation)return;
        double lat=location.getLatitude(),lon=location.getLongitude();if(!Double.isFinite(lat)||!Double.isFinite(lon))return;
        settings.position(lat,lon,"当前位置",TimeZone.getDefault().getID());settings.prefs.edit().putLong("locationUpdated",System.currentTimeMillis()).apply();status="位置已更新 · 精度约 "+Math.round(location.getAccuracy())+" 米";
    }
    @Override public void onProviderDisabled(String provider){status="定位已关闭，保留最近位置";}
    public void stop(){handler.removeCallbacksAndMessages(null);if(current!=null){current.cancel();current=null;}if(manager!=null)manager.removeUpdates(this);}
}
