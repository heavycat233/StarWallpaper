package app.starfield.wallpaper;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

public final class Settings implements SharedPreferences.OnSharedPreferenceChangeListener {
    public final SharedPreferences prefs;
    public final AtomicInteger version=new AtomicInteger();
    public Settings(Context context){prefs=context.getApplicationContext().getSharedPreferences("starfield",Context.MODE_PRIVATE);prefs.registerOnSharedPreferenceChangeListener(this);}
    public void close(){prefs.unregisterOnSharedPreferenceChangeListener(this);}
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){version.incrementAndGet();}
    public void number(String key,int value){prefs.edit().putInt(key,value).apply();}
    public void flag(String key,boolean value){prefs.edit().putBoolean(key,value).apply();}
    public void text(String key,String value){prefs.edit().putString(key,value).apply();}
    public void position(double lat,double lon,String name,String zone){
        prefs.edit().putLong("lat",Double.doubleToRawLongBits(SkyMath.clamp(lat,-90,90))).putLong("lon",Double.doubleToRawLongBits(SkyMath.clamp(lon,-180,180))).putString("place",name).putString("zone",zone).apply();
    }
    public Snapshot snapshot(){return new Snapshot(prefs);}
    public static final class Snapshot {
        public final String clock,uiMode,place,zone;
        public final int magnitude,faintBrightness,fov,glow,dust,azimuth,altitude,clockTop,fps,brightSize,nebulaBrightness,nebulaSize,moonSize,twinkleStrength;
        public final boolean nebulae,moon;
        public final double lat,lon;
        public final boolean twinkle,labels,constellations,followLocation,systemTime,desktopClock;
        public final long fixedEpoch;
        Snapshot(SharedPreferences p){
            clock=p.getString("clock","digital");uiMode=p.getString("uiMode","minimal");place=p.getString("place","香港");
            String savedZone=p.getString("zone","Asia/Hong_Kong");try{ZoneId.of(savedZone);}catch(Exception e){savedZone="Asia/Shanghai";}zone=savedZone;
            magnitude=bounded(p,"magnitude",9,3,11);faintBrightness=bounded(p,"faintBrightness",120,50,400);fov=bounded(p,"fov",180,120,180);glow=bounded(p,"glow",148,0,200);dust=bounded(p,"dust",50,0,150);
            brightSize=bounded(p,"brightSize",70,50,100);nebulaBrightness=bounded(p,"nebulaBrightness",140,0,250);nebulaSize=bounded(p,"nebulaSize",250,100,400);moonSize=bounded(p,"moonSize",8,1,12);nebulae=p.getBoolean("nebulae",true);moon=p.getBoolean("moon",true);
            azimuth=bounded(p,"azimuth",94,0,359);altitude=bounded(p,"altitude",79,0,90);clockTop=bounded(p,"clockTop",13,5,65);fps=bounded(p,"fps",20,5,30);twinkleStrength=bounded(p,"twinkleStrength",140,0,200);
            double la=Double.longBitsToDouble(p.getLong("lat",Double.doubleToLongBits(22.3193))),lo=Double.longBitsToDouble(p.getLong("lon",Double.doubleToLongBits(114.1694)));
            lat=Double.isFinite(la)?SkyMath.clamp(la,-90,90):22.3193;lon=Double.isFinite(lo)?SkyMath.clamp(lo,-180,180):114.1694;
            twinkle=p.getBoolean("twinkle",true);labels=p.getBoolean("labels",false)&&!uiMode.equals("minimal");constellations=p.getBoolean("constellations",false)&&!uiMode.equals("minimal");followLocation=p.getBoolean("followLocation",false);
            systemTime=p.getBoolean("systemTime",true);fixedEpoch=p.getLong("fixedEpoch",System.currentTimeMillis());desktopClock=p.getBoolean("desktopClock",false);
        }
        static int bounded(SharedPreferences p,String key,int value,int lo,int hi){return (int)SkyMath.clamp(p.getInt(key,value),lo,hi);}
        public long epoch(){return systemTime?System.currentTimeMillis():fixedEpoch;}
    }
}
