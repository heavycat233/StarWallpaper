package app.starfield.wallpaper;

/** J2000 -> mean equator of date -> local ENU -> equidistant fisheye. */
public final class SkyMath {
    public static final double D = Math.PI / 180.0;
    public static final double[] GALACTIC = {-.0548755604,-.8734370902,-.4838350155,.4941094279,-.4448296300,.7469822445,-.8676661490,-.1980763734,.4559837762};
    private SkyMath() {}
    public static double clamp(double x,double lo,double hi) { return Math.max(lo,Math.min(hi,x)); }
    public static double[] mul(double[] a,double[] b) {
        double[] out=new double[9];
        for(int i=0;i<3;i++) for(int j=0;j<3;j++) for(int k=0;k<3;k++) out[i*3+j]+=a[i*3+k]*b[k*3+j];
        return out;
    }
    public static double[] mv(double[] a,double[] v) { return new double[]{a[0]*v[0]+a[1]*v[1]+a[2]*v[2],a[3]*v[0]+a[4]*v[1]+a[5]*v[2],a[6]*v[0]+a[7]*v[1]+a[8]*v[2]}; }
    public static double[] transpose(double[] m) { return new double[]{m[0],m[3],m[6],m[1],m[4],m[7],m[2],m[5],m[8]}; }
    static double[] rz(double a) { double c=Math.cos(a),s=Math.sin(a); return new double[]{c,-s,0,s,c,0,0,0,1}; }
    public static double[] equatorial(double hours,double degrees) { double a=hours*15*D,d=degrees*D; return new double[]{Math.cos(d)*Math.cos(a),Math.cos(d)*Math.sin(a),Math.sin(d)}; }
    public static double sidereal(long epoch,double lon) {
        double days=epoch/86400000.0+2440587.5-2451545,t=days/36525;
        double value=280.46061837+360.98564736629*days+.000387933*t*t-t*t*t/38710000+lon;
        return ((value%360+360)%360)*D;
    }
    public static double[] precession(long epoch) {
        double t=(epoch/86400000.0+2440587.5-2451545)/36525;
        double zeta=(2306.2181*t+.30188*t*t+.017998*t*t*t)/3600*D,z=(2306.2181*t+1.09468*t*t+.018203*t*t*t)/3600*D,theta=(2004.3109*t-.42665*t*t-.041833*t*t*t)/3600*D;
        double c=Math.cos(theta),s=Math.sin(theta);
        return mul(mul(rz(z),new double[]{c,0,-s,0,1,0,s,0,c}),rz(zeta));
    }
    public static Camera camera(long epoch,double lat,double lon,double azimuth,double altitude,double fov,int width,int height) { return new Camera(epoch,lat,lon,azimuth,altitude,fov,width,height); }
    public static final class Camera {
        public final double[] local,inverse,right,up,center;
        public final int width,height;
        public final double scale;
        Camera(long epoch,double lat,double lon,double azimuth,double altitude,double fov,int width,int height) {
            this.width=width;this.height=height;double lst=sidereal(epoch,lon),p=lat*D,az=azimuth*D,alt=altitude*D;
            double[] horizontal={-Math.sin(lst),Math.cos(lst),0,-Math.sin(p)*Math.cos(lst),-Math.sin(p)*Math.sin(lst),Math.cos(p),Math.cos(p)*Math.cos(lst),Math.cos(p)*Math.sin(lst),Math.sin(p)};
            local=mul(horizontal,precession(epoch));inverse=transpose(local);
            right=new double[]{Math.cos(az),-Math.sin(az),0};up=new double[]{-Math.sin(alt)*Math.sin(az),-Math.sin(alt)*Math.cos(az),Math.cos(alt)};center=new double[]{Math.cos(alt)*Math.sin(az),Math.cos(alt)*Math.cos(az),Math.sin(alt)};
            scale=Math.hypot(width,height)/(fov*D);
        }
        public double[] project(double[] eq) {
            double[] v=mv(local,eq);if(v[2]<0)return null;
            double x=dot(v,right),y=dot(v,up),z=dot(v,center);if(z<-.001)return null;
            double r=Math.hypot(x,y),factor=r<1e-10?scale:scale*Math.acos(clamp(z,-1,1))/r;
            return new double[]{width*.5+x*factor,height*.5-y*factor,v[2]};
        }
        public double[] unproject(double x,double y) {
            double dx=(x-width*.5)/scale,dy=(height*.5-y)/scale,t=Math.hypot(dx,dy),s=t<1e-10?1:Math.sin(t)/t;
            return mv(inverse,new double[]{center[0]*Math.cos(t)+(right[0]*dx+up[0]*dy)*s,center[1]*Math.cos(t)+(right[1]*dx+up[1]*dy)*s,center[2]*Math.cos(t)+(right[2]*dx+up[2]*dy)*s});
        }
    }
    public static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    public static float[] glMatrix(double[] matrix) { double[] t=transpose(matrix);float[] f=new float[9];for(int i=0;i<9;i++)f[i]=(float)t[i];return f; }
    public static String direction(double az){return new String[]{"北","东北","东","东南","南","西南","西","西北"}[(int)Math.round(az/45)%8];}
    public static int[] galaxyFraming(long epoch,double lat,double lon) {
        Camera c=camera(epoch,lat,lon,0,90,150,320,680);double highest=-90,az=0;double[] inverse=transpose(GALACTIC);
        for(int l=0;l<360;l+=3){double[] v=mv(c.local,mv(inverse,new double[]{Math.cos(l*D),Math.sin(l*D),0}));double alt=Math.asin(clamp(v[2],-1,1))/D;if(alt>highest){highest=alt;az=(Math.atan2(v[0],v[1])/D+360)%360;}}
        return new int[]{(int)Math.round((az-20+360)%360),(int)Math.round(clamp(highest+20,25,80))};
    }
}
