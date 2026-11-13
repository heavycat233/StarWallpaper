package app.starfield.wallpaper;

/** Low-precision Sun direction for visual ecliptic effects and annual shower timing. */
public final class SkyEvents {
    private SkyEvents(){}
    public static final double OBLIQUITY=23.439291*SkyMath.D;
    public static final double[] ECLIPTIC_NORMAL={0,-Math.sin(OBLIQUITY),Math.cos(OBLIQUITY)};
    public static double solarLongitude(long epoch){
        double days=epoch/86400000.0+2440587.5-2451545.0;
        double mean=(280.460+0.9856474*days)*SkyMath.D;
        double anomaly=(357.528+0.9856003*days)*SkyMath.D;
        double longitude=(mean/SkyMath.D+1.915*Math.sin(anomaly)+0.020*Math.sin(2*anomaly))%360;
        return (longitude+360)%360;
    }
    public static double[] sunRay(long epoch){
        double longitude=solarLongitude(epoch)*SkyMath.D;
        return new double[]{Math.cos(longitude),Math.cos(OBLIQUITY)*Math.sin(longitude),Math.sin(OBLIQUITY)*Math.sin(longitude)};
    }
    public static double solarDistance(double a,double b){return (a-b+540)%360-180;}
    public static double zodiacalPhase(SkyMath.Camera camera,long epoch){
        double altitude=Math.asin(SkyMath.clamp(SkyMath.mv(camera.local,sunRay(epoch))[2],-1,1))/SkyMath.D;
        return smoothstep(-28,-15,altitude)*(1-smoothstep(-9,-3,altitude));
    }
    private static double smoothstep(double a,double b,double x){double t=SkyMath.clamp((x-a)/(b-a),0,1);return t*t*(3-2*t);}
}
