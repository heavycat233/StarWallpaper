package app.starfield.wallpaper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/** Animated decorative meteors using IMO annual shower activity and population indices. */
public final class MeteorLayer {
    private static final double VISUAL_TIME_SCALE=20.0;
    private static final double SPORADIC_ZHR=2.5;
    private static final class Shower {
        final String name;final int start,end;final double peakSolar,ra,dec,zhr,r,widthDays;
        Shower(JSONObject object)throws Exception{
            name=object.getString("name");start=object.getInt("start");end=object.getInt("end");peakSolar=object.getDouble("peakSolar");
            ra=object.getDouble("ra");dec=object.getDouble("dec");zhr=object.getDouble("zhr");r=object.getDouble("r");widthDays=object.getDouble("widthDays");
        }
        boolean active(int day){return start<=end?day>=start&&day<=end:day>=start||day<=end;}
        double weight(int day,double solar,SkyMath.Camera camera){
            if(!active(day))return 0;
            double altitude=SkyMath.mv(camera.local,SkyMath.equatorial(ra/15,dec))[2];
            if(altitude<=0)return 0;
            double delta=SkyEvents.solarDistance(solar,peakSolar)/.9856474;
            return zhr*(.02+.98*Math.exp(-.5*Math.pow(delta/widthDays,2)))*altitude;
        }
    }
    private static final class Meteor {
        final double x,y,dx,dy,length,width,alpha,duration;final long started;
        Meteor(double x,double y,double dx,double dy,double length,double width,double alpha,double duration,long started){
            this.x=x;this.y=y;this.dx=dx;this.dy=dy;this.length=length;this.width=width;this.alpha=alpha;this.duration=duration;this.started=started;
        }
    }
    private final Shower[] showers;private final Random random=new Random();private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Meteor> active=new ArrayList<>();private long nextSpawn,lastTick;private int lastVersion=-1;
    public MeteorLayer(Context context)throws Exception{
        JSONArray data=new JSONObject(StarCatalog.readText(context,"meteor-showers.json")).getJSONArray("showers");
        showers=new Shower[data.length()];for(int i=0;i<showers.length;i++)showers[i]=new Shower(data.getJSONObject(i));
        paint.setStrokeCap(Paint.Cap.ROUND);
    }
    private double interval(double eventsPerSecond){return -Math.log(Math.max(1e-9,random.nextDouble()))/Math.max(1e-9,eventsPerSecond)*1000;}
    private int magnitude(double r){
        double total=0;for(int m=-3;m<=5;m++)total+=Math.pow(r,m);
        double value=random.nextDouble()*total;for(int m=-3;m<=5;m++){value-=Math.pow(r,m);if(value<=0)return m;}return 5;
    }
    public boolean draw(Canvas canvas,SkyMath.Camera camera,Settings.Snapshot scene,long epoch,int version){
        long now=SystemClock.elapsedRealtime();
        if(!scene.meteors||scene.meteorDensity==0){active.clear();nextSpawn=0;lastVersion=version;return false;}
        if(lastVersion!=version||now-lastTick>15000){active.clear();nextSpawn=0;lastVersion=version;}
        lastTick=now;
        LocalDate date=Instant.ofEpochMilli(epoch).atZone(ZoneOffset.UTC).toLocalDate();int day=date.getMonthValue()*100+date.getDayOfMonth();
        double solar=SkyEvents.solarLongitude(epoch),total=SPORADIC_ZHR;double[] weights=new double[showers.length];
        for(int i=0;i<showers.length;i++){weights[i]=showers[i].weight(day,solar,camera);total+=weights[i];}
        double fraction=.30*Math.pow(scene.fov/180.0,2);
        double rate=total*fraction*VISUAL_TIME_SCALE*scene.meteorDensity/100.0/3600.0;
        if(nextSpawn==0)nextSpawn=now+Math.max(200,(long)interval(rate));
        int spawned=0;
        while(now>=nextSpawn&&spawned++<4){
            double choice=random.nextDouble()*total;Shower shower=null;
            for(int i=0;i<showers.length;i++){choice-=weights[i];if(choice<=0){shower=showers[i];break;}}
            spawn(camera,shower,now);
            nextSpawn+=Math.max(200,(long)interval(rate));
        }
        if(now>=nextSpawn)nextSpawn=now+Math.max(200,(long)interval(rate));
        boolean drawn=false;
        for(Iterator<Meteor> iterator=active.iterator();iterator.hasNext();){
            Meteor meteor=iterator.next();double progress=(now-meteor.started)/meteor.duration;
            if(progress>=1){iterator.remove();continue;}
            if(progress<0)continue;
            double hx=meteor.x+meteor.dx*meteor.length*(progress-.5),hy=meteor.y+meteor.dy*meteor.length*(progress-.5);
            double tail=meteor.length*.62*Math.min(1,progress*6),tx=hx-meteor.dx*tail,ty=hy-meteor.dy*tail;
            double fade=meteor.alpha*Math.min(1,progress*7)*Math.pow(1-progress,.6);
            int head=Color.argb((int)(255*fade),245,242,229);
            paint.setColor(Color.WHITE);
            paint.setShader(new LinearGradient((float)tx,(float)ty,(float)hx,(float)hy,Color.TRANSPARENT,head,Shader.TileMode.CLAMP));
            paint.setStrokeWidth((float)meteor.width);canvas.drawLine((float)tx,(float)ty,(float)hx,(float)hy,paint);paint.setShader(null);
            if(meteor.width>1.6){paint.setColor(Color.argb((int)(75*fade),172,200,255));paint.setStrokeWidth((float)(meteor.width*3));canvas.drawLine((float)tx,(float)ty,(float)hx,(float)hy,paint);}
            paint.setColor(Color.argb((int)(210*fade),255,251,241));canvas.drawCircle((float)hx,(float)hy,(float)(meteor.width*.75),paint);
            drawn=true;
        }
        return drawn;
    }
    private void spawn(SkyMath.Camera camera,Shower shower,long now){
        for(int attempt=0;attempt<10;attempt++){
            double x=random.nextDouble()*camera.width,y=random.nextDouble()*camera.height;
            double[] ray=camera.unproject(x,y),local=SkyMath.mv(camera.local,ray);
            if(local[2]<.18)continue;
            double dx,dy;
            if(shower==null){double angle=random.nextDouble()*Math.PI*2;dx=Math.cos(angle);dy=Math.sin(angle);}
            else{
                double[] radiant=SkyMath.equatorial(shower.ra/15,shower.dec);double dot=SkyMath.dot(ray,radiant);
                double[] away={ray[0]*dot-radiant[0],ray[1]*dot-radiant[1],ray[2]*dot-radiant[2]};
                double norm=Math.sqrt(SkyMath.dot(away,away));if(norm<.05)continue;
                double angle=.02/norm;double[] shifted={ray[0]*Math.cos(.02)+away[0]*angle,ray[1]*Math.cos(.02)+away[1]*angle,ray[2]*Math.cos(.02)+away[2]*angle};
                double[] projected=camera.project(shifted);if(projected==null)continue;
                dx=projected[0]-x;dy=projected[1]-y;norm=Math.hypot(dx,dy);if(norm<.01)continue;dx/=norm;dy/=norm;
            }
            int mag=magnitude(shower==null?2.7:shower.r);double scale=Math.max(.85,camera.width/320.0);
            double length=(14+(5-mag)*6+random.nextDouble()*12)*scale,width=(.55+(5-mag)*.19)*scale;
            double alpha=Math.min(1,.30+(5-mag)*.09),duration=420+random.nextDouble()*380;
            active.add(new Meteor(x,y,dx,dy,length,width,alpha,duration,now));return;
        }
    }
}
