import app.starfield.wallpaper.SkyMath;
import java.time.Instant;

public final class SkyMathNativeTest {
    static void close(double actual,double expected,double tolerance){if(Math.abs(actual-expected)>tolerance)throw new AssertionError(actual+" != "+expected);}
    public static void main(String[] args){
        long epoch=Instant.parse("2000-01-01T12:00:00Z").toEpochMilli();
        close(SkyMath.sidereal(epoch,0)/SkyMath.D,280.46061837,1e-8);
        close(SkyMath.sidereal(epoch+86400000,0)/SkyMath.D,281.44626573629,1e-8);
        int samples=0;
        for(double lat:new double[]{-67,-33.8688,0,22.3193,65}){
            SkyMath.Camera c=SkyMath.camera(epoch,lat,114,0,90,150,320,680);
            close(Math.asin(SkyMath.mv(c.local,new double[]{0,0,1})[2])/SkyMath.D,lat,1e-8);
            double[] star=SkyMath.equatorial(SkyMath.sidereal(epoch,114)/SkyMath.D/15,lat),p=c.project(star);
            close(p[0],160,1e-5);close(p[1],340,1e-5);close(p[2],1,1e-8);
            if(c.project(new double[]{-star[0],-star[1],-star[2]})!=null)throw new AssertionError("Nadir visible");
        }
        for(long date:new long[]{epoch,Instant.parse("2026-10-28T14:00:00Z").toEpochMilli(),Instant.parse("2050-06-01T02:00:00Z").toEpochMilli()})for(int fov:new int[]{120,147,180})for(int alt:new int[]{0,45,90}){
            SkyMath.Camera c=SkyMath.camera(date,-33.8688,151.2093,237,alt,fov,320,680);
            for(int x:new int[]{0,40,160,280,320})for(int y:new int[]{0,80,340,600,680}){double[] eq=c.unproject(x,y),local=SkyMath.mv(c.local,eq);close(SkyMath.dot(eq,eq),1,1e-8);if(local[2]>1e-8){double[] projected=c.project(eq);if(projected==null)throw new AssertionError("Projection missing");close(projected[0],x,1e-5);close(projected[1],y,1e-5);samples++;}}
            double[] corner=SkyMath.mv(c.local,c.unproject(0,0));close(Math.acos(SkyMath.dot(corner,c.center))/SkyMath.D,fov/2.0,1e-5);
        }
        System.out.println("PASS native astronomy: sidereal reference, both hemispheres, horizon, zenith, "+samples+" projection samples.");
    }
}
