package app.starfield.wallpaper;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.nio.*;
import java.util.zip.GZIPInputStream;
import org.json.*;

/** Offline Astronomy Engine sampled ephemeris, OpenNGC positions and NASA Moon map. */
public final class CelestialLayer {
 private static volatile ByteBuffer table;private static double start,step;private static int count;
 private final JSONArray nebulae;private final Bitmap[] clouds;private final Bitmap moonMap;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 public CelestialLayer(Context c)throws Exception{
  synchronized(CelestialLayer.class){if(table==null){try(InputStream in=new GZIPInputStream(c.getAssets().open("moon-ephemeris.bin.gz"))){ByteBuffer loaded=ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);start=loaded.getDouble(0);step=loaded.getDouble(8);count=loaded.getInt(16);table=loaded;}}}
  nebulae=new JSONArray(StarCatalog.readText(c,"nebulae.json"));clouds=new Bitmap[nebulae.length()];
  for(int i=0;i<clouds.length;i++)try(InputStream in=c.getAssets().open(nebulae.getJSONObject(i).getString("id")+".png")){clouds[i]=BitmapFactory.decodeStream(in);}
  try(InputStream in=c.getAssets().open("moon.png")){moonMap=BitmapFactory.decodeStream(in);}
 }
 public static double[] sample(long epoch){
  double t=SkyMath.clamp((epoch-start)/step,0,count-1.000001);int i=(int)t;double f=t-i;double[] v=new double[11];
  for(int k=0;k<11;k++){double a=table.getFloat(20+i*44+k*4),b=table.getFloat(20+(i+1)*44+k*4);double d=b-a;if(k==10){if(d>180)d-=360;if(d< -180)d+=360;}v[k]=a+f*d;}return v;
 }
 static double[] unit(double[] v){double d=Math.sqrt(SkyMath.dot(v,v));return new double[]{v[0]/d,v[1]/d,v[2]/d};}
 static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
 static double[] offset(double[] v,double[] t,double a){return new double[]{v[0]*Math.cos(a)+t[0]*Math.sin(a),v[1]*Math.cos(a)+t[1]*Math.sin(a),v[2]*Math.cos(a)+t[2]*Math.sin(a)};}
 public static double[] moonRay(double[] v,SkyMath.Camera c,double lat){
  double p=lat*SkyMath.D,N=6378.137/Math.sqrt(1-.00669438*Math.sin(p)*Math.sin(p)),r=N*Math.cos(p),z=N*(1-.00669438)*Math.sin(p);
  double[] observer=SkyMath.mv(c.inverse,new double[]{0,z*Math.cos(p)-r*Math.sin(p),r*Math.cos(p)+z*Math.sin(p)});
  return unit(new double[]{v[0]-observer[0],v[1]-observer[1],v[2]-observer[2]});
 }
 public static double[] horizontal(long epoch,double lat,double lon){SkyMath.Camera c=SkyMath.camera(epoch,lat,lon,0,90,150,320,680);double[] v=SkyMath.mv(c.local,moonRay(sample(epoch),c,lat));return new double[]{(Math.atan2(v[0],v[1])/SkyMath.D+360)%360,Math.asin(SkyMath.clamp(v[2],-1,1))/SkyMath.D};}
 public void drawClouds(Canvas canvas,SkyMath.Camera c,Settings.Snapshot s){
  if(!s.nebulae||s.nebulaBrightness==0)return;
  for(int i=0;i<clouds.length;i++)try{
   JSONObject n=nebulae.getJSONObject(i);double[] ray=SkyMath.equatorial(n.getDouble("ra"),n.getDouble("dec")),p=c.project(ray);if(p==null)continue;
   double[] east=unit(new double[]{-ray[1],ray[0],0}),north=unit(cross(ray,east));double factor=s.nebulaSize/100.0*SkyMath.D/120;
   double[] a=c.project(offset(ray,north,n.getDouble("major")*factor)),b=c.project(offset(ray,east,n.getDouble("minor")*factor));if(a==null||b==null)continue;
   Matrix matrix=new Matrix();matrix.setValues(new float[]{(float)((b[0]-p[0])/64),(float)((a[0]-p[0])/64),(float)(p[0]-(b[0]-p[0])-(a[0]-p[0])),(float)((b[1]-p[1])/64),(float)((a[1]-p[1])/64),(float)(p[1]-(b[1]-p[1])-(a[1]-p[1])),0,0,1});
   paint.setBlendMode(BlendMode.SCREEN);double strength=s.nebulaBrightness/100.0;for(int pass=0;pass<Math.ceil(strength);pass++){paint.setAlpha((int)(255*Math.min(1,strength-pass)));canvas.drawBitmap(clouds[i],matrix,paint);}paint.setBlendMode(null);paint.setAlpha(255);
  }catch(JSONException ignored){}
  // Extended clouds are clipped at the actual horizon too.
  paint.setBlendMode(BlendMode.CLEAR);for(int y=0;y<c.height;y+=3)for(int x=0;x<c.width;x+=3)if(SkyMath.mv(c.local,c.unproject(x+1.5,y+1.5))[2]<0)canvas.drawRect(x,y,x+3,y+3,paint);paint.setBlendMode(null);
 }
 public void drawMoon(Canvas canvas,SkyMath.Camera c,Settings.Snapshot s,long epoch){
  if(!s.moon)return;double[] v=sample(epoch),ray=moonRay(v,c,s.lat),p=c.project(ray);if(p==null)return;
  double radius=c.scale*Math.asin(1737.4/Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]))*s.moonSize;
  if(p[0]+radius<0||p[0]-radius>c.width||p[1]+radius<0||p[1]-radius>c.height)return;
  double[] east=unit(new double[]{-ray[1],ray[0],0}),north=unit(cross(ray,east)),e=c.project(offset(ray,east,.0001)),n=c.project(offset(ray,north,.0001));if(e==null||n==null)return;
  double[] light={v[3],v[4],v[5]},axis={v[6],v[7],v[8]};double se=SkyMath.dot(light,east),sn=SkyMath.dot(light,north),lx=(e[0]-p[0])*se+(n[0]-p[0])*sn,ly=(e[1]-p[1])*se+(n[1]-p[1])*sn,lz=-SkyMath.dot(light,ray),norm=Math.hypot(lx,ly),xy=Math.sqrt(Math.max(0,1-lz*lz));lx=lx/norm*xy;ly=ly/norm*xy;
  se=SkyMath.dot(axis,east);sn=SkyMath.dot(axis,north);double px=(e[0]-p[0])*se+(n[0]-p[0])*sn,py=(e[1]-p[1])*se+(n[1]-p[1])*sn;norm=Math.hypot(px,py);px/=norm;py/=norm;
  double b=v[9]*SkyMath.D,l=v[10]*SkyMath.D;double[] center={Math.cos(b)*Math.cos(l),Math.cos(b)*Math.sin(l),Math.sin(b)},me={-Math.sin(l),Math.cos(l),0},mn={-Math.sin(b)*Math.cos(l),-Math.sin(b)*Math.sin(l),Math.cos(b)};
  int size=128;int[] pixels=new int[size*size];
  for(int y=0;y<size;y++)for(int x=0;x<size;x++){
   double nx=(x+.5-64)/64,ny=(y+.5-64)/64,r2=nx*nx+ny*ny;if(r2>=1)continue;
   if(SkyMath.mv(c.local,c.unproject(p[0]+nx*radius,p[1]+ny*radius))[2]<0)continue;
   double nz=Math.sqrt(1-r2),xe=-nx*py+ny*px,yn=nx*px+ny*py;double[] q=new double[3];for(int k=0;k<3;k++)q[k]=nz*center[k]+xe*me[k]+yn*mn[k];
   int tx=(int)((Math.atan2(q[1],q[0])/(2*Math.PI)+.5)*moonMap.getWidth())%moonMap.getWidth(),ty=(int)((.5-Math.asin(SkyMath.clamp(q[2],-1,1))/Math.PI)*moonMap.getHeight());
   double albedo=Color.red(moonMap.getPixel(Math.max(0,tx),Math.min(moonMap.getHeight()-1,ty)))/255.0,dot=nx*lx+ny*ly+nz*lz,value=(.4+.6*albedo)*(dot>0?.45+.55*Math.sqrt(dot):.035)*245;
   pixels[y*size+x]=Color.argb((int)(255*SkyMath.clamp((1-Math.sqrt(r2))*64,0,1)),(int)value,(int)(value*.98),(int)(value*.92));
  }
  Bitmap disk=Bitmap.createBitmap(pixels,size,size,Bitmap.Config.ARGB_8888);paint.setAlpha(255);canvas.drawBitmap(disk,null,new RectF((float)(p[0]-radius),(float)(p[1]-radius),(float)(p[0]+radius),(float)(p[1]+radius)),paint);disk.recycle();
 }
}
