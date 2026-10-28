package app.starfield.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.json.JSONArray;

/** Shared OpenGL ES renderer for settings preview and the system WallpaperService. */
public final class StarRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    public final Settings settings;
    public volatile String error;
    public volatile boolean allowClock=true;
    public volatile int renderedFrames=0;
    private Settings.Snapshot scene;
    private int seenVersion=-1,width=1,height=1,quadVao,starVao,quadVbo,starVbo,galaxyProgram,starsProgram,textureProgram,galaxyTexture,overlayTexture,framebuffer;
    private int galaxyW,galaxyH;
    private StarCatalog catalog;
    private CelestialLayer celestial;private int cloudTexture;private Bitmap cloudOverlay;
    private long galaxyTime=Long.MIN_VALUE,overlayTime=Long.MIN_VALUE;
    private int galaxyVersion=-1,overlayVersion=-1;
    private boolean overlayClock;
    private Bitmap overlay;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
    private final HashMap<String,Integer> uniformCache=new HashMap<>();
    private final DateTimeFormatter dateFormat=DateTimeFormatter.ofPattern("M月d日EEEE",Locale.SIMPLIFIED_CHINESE);
    private static final HashMap<String,String> NAMES=new HashMap<>();
    static {String[][] values={{"Vega","织女星"},{"Altair","牛郎星"},{"Deneb","天津四"},{"Sirius","天狼星"},{"Capella","五车二"},{"Arcturus","大角星"},{"Rigel","参宿七"},{"Betelgeuse","参宿四"},{"Antares","心宿二"},{"Spica","角宿一"},{"Fomalhaut","北落师门"},{"Aldebaran","毕宿五"},{"Polaris","北极星"},{"Canopus","老人星"},{"Procyon","南河三"},{"Pollux","北河三"},{"Regulus","轩辕十四"}};for(String[] v:values)NAMES.put(v[0],v[1]);}
    public StarRenderer(Context context){this.context=context.getApplicationContext();settings=new Settings(context);}
    @Override public void onSurfaceCreated(GL10 ignored,EGLConfig config){
        error=null;uniformCache.clear();galaxyTime=Long.MIN_VALUE;overlayTime=Long.MIN_VALUE;seenVersion=-1;
        try{
            catalog=StarCatalog.get(context);celestial=new CelestialLayer(context);
            int[] vaos=new int[2],buffers=new int[2],textures=new int[3],fbos=new int[1];
            GLES30.glGenVertexArrays(2,vaos,0);quadVao=vaos[0];starVao=vaos[1];GLES30.glGenBuffers(2,buffers,0);quadVbo=buffers[0];starVbo=buffers[1];
            FloatBuffer quad=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer();quad.put(new float[]{-1,-1,1,-1,-1,1,1,1}).position(0);
            GLES30.glBindVertexArray(quadVao);GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,quadVbo);GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,32,quad,GLES30.GL_STATIC_DRAW);GLES30.glEnableVertexAttribArray(0);GLES30.glVertexAttribPointer(0,2,GLES30.GL_FLOAT,false,8,0);
            GLES30.glBindVertexArray(starVao);GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,starVbo);
            FloatBuffer stars=catalog.vertices.duplicate();stars.position(0);GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,catalog.count*20,stars,GLES30.GL_STATIC_DRAW);
            GLES30.glEnableVertexAttribArray(0);GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,20,0);
            GLES30.glEnableVertexAttribArray(1);GLES30.glVertexAttribPointer(1,1,GLES30.GL_FLOAT,false,20,12);
            GLES30.glEnableVertexAttribArray(2);GLES30.glVertexAttribPointer(2,1,GLES30.GL_FLOAT,false,20,16);
            galaxyProgram=program("shaders/quad.vert","shaders/galaxy.frag");starsProgram=program("shaders/stars.vert","shaders/stars.frag");textureProgram=program("shaders/quad.vert","shaders/texture.frag");
            GLES30.glGenTextures(3,textures,0);galaxyTexture=textures[0];overlayTexture=textures[1];cloudTexture=textures[2];
            for(int texture:textures){GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,texture);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);}
            GLES30.glGenFramebuffers(1,fbos,0);framebuffer=fbos[0];GLES30.glBindVertexArray(0);
            GLES30.glClearColor(.02f,.04f,.09f,1);
        }catch(Exception e){error="图形初始化失败："+e.getMessage();Log.e("Starfield",error,e);}
    }
    @Override public void onSurfaceChanged(GL10 ignored,int width,int height){
        this.width=Math.max(1,width);this.height=Math.max(1,height);galaxyW=Math.min(280,this.width);galaxyH=Math.max(1,Math.round((float)galaxyW*this.height/this.width));
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,galaxyTexture);GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,0,GLES30.GL_RGBA,galaxyW,galaxyH,0,GLES30.GL_RGBA,GLES30.GL_UNSIGNED_BYTE,null);
        int overlayWidth=Math.min(this.width,768);if(overlay!=null)overlay.recycle();overlay=Bitmap.createBitmap(overlayWidth,Math.max(1,Math.round((float)overlayWidth*this.height/this.width)),Bitmap.Config.ARGB_8888);
        if(cloudOverlay!=null)cloudOverlay.recycle();cloudOverlay=Bitmap.createBitmap(overlay.getWidth(),overlay.getHeight(),Bitmap.Config.ARGB_8888);
        galaxyTime=Long.MIN_VALUE;overlayTime=Long.MIN_VALUE;
    }
    @Override public void onDrawFrame(GL10 ignored){
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);GLES30.glViewport(0,0,width,height);GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        if(error!=null||catalog==null||overlay==null)return;
        int version=settings.version.get();if(scene==null||seenVersion!=version){scene=settings.snapshot();seenVersion=version;}
        long epoch=scene.epoch();SkyMath.Camera camera=SkyMath.camera(epoch,scene.lat,scene.lon,scene.azimuth,scene.altitude,scene.fov,width,height);
        if(galaxyTime!=epoch/10000||galaxyVersion!=version){
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,framebuffer);GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,GLES30.GL_COLOR_ATTACHMENT0,GLES30.GL_TEXTURE_2D,galaxyTexture,0);GLES30.glViewport(0,0,galaxyW,galaxyH);GLES30.glDisable(GLES30.GL_BLEND);
            GLES30.glUseProgram(galaxyProgram);cameraUniforms(galaxyProgram,camera);GLES30.glUniformMatrix3fv(uniform(galaxyProgram,"uInverse"),1,false,SkyMath.glMatrix(camera.inverse),0);GLES30.glUniform1f(uniform(galaxyProgram,"uGlow"),scene.glow/100f);GLES30.glUniform1f(uniform(galaxyProgram,"uDust"),scene.dust/100f);quad();
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);GLES30.glViewport(0,0,width,height);galaxyTime=epoch/10000;galaxyVersion=version;
            Canvas clouds=new Canvas(cloudOverlay);clouds.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);celestial.drawClouds(clouds,SkyMath.camera(epoch,scene.lat,scene.lon,scene.azimuth,scene.altitude,scene.fov,cloudOverlay.getWidth(),cloudOverlay.getHeight()),scene);GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,cloudTexture);GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,cloudOverlay,0);
        }
        GLES30.glDisable(GLES30.GL_BLEND);texture(galaxyTexture,false);GLES30.glEnable(GLES30.GL_BLEND);GLES30.glBlendFunc(GLES30.GL_ONE,GLES30.GL_ONE_MINUS_SRC_ALPHA);texture(cloudTexture,true);
        GLES30.glEnable(GLES30.GL_BLEND);GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA,GLES30.GL_ONE_MINUS_SRC_ALPHA);GLES30.glUseProgram(starsProgram);cameraUniforms(starsProgram,camera);GLES30.glUniformMatrix3fv(uniform(starsProgram,"uLocal"),1,false,SkyMath.glMatrix(camera.local),0);GLES30.glUniform1f(uniform(starsProgram,"uTime"),(SystemClock.elapsedRealtime()%600000)/1000f);GLES30.glUniform1f(uniform(starsProgram,"uPixelScale"),Math.max(.85f,width/320f));GLES30.glUniform1f(uniform(starsProgram,"uFaintBrightness"),scene.faintBrightness/100f);GLES30.glUniform1f(uniform(starsProgram,"uBrightSize"),scene.brightSize/100f);GLES30.glUniform1i(uniform(starsProgram,"uTwinkle"),scene.twinkle?1:0);GLES30.glBindVertexArray(starVao);int total=catalog.counts[scene.magnitude],bright=Math.min(total,catalog.dynamicCount);if(total>bright)GLES30.glDrawArrays(GLES30.GL_POINTS,bright,total-bright);GLES30.glDrawArrays(GLES30.GL_POINTS,0,bright);
        long overlayBucket=epoch/(scene.clock.equals("analog")?1000:60000);
        if(overlayTime!=overlayBucket||overlayVersion!=version||overlayClock!=allowClock){
            drawOverlay(epoch);GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,overlayTexture);GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,overlay,0);overlayTime=overlayBucket;overlayVersion=version;overlayClock=allowClock;
        }
        GLES30.glBlendFunc(GLES30.GL_ONE,GLES30.GL_ONE_MINUS_SRC_ALPHA);texture(overlayTexture,true);GLES30.glDisable(GLES30.GL_BLEND);renderedFrames++;
    }
    private void texture(int texture,boolean flip){GLES30.glUseProgram(textureProgram);GLES30.glActiveTexture(GLES30.GL_TEXTURE0);GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,texture);GLES30.glUniform1i(uniform(textureProgram,"uTexture"),0);GLES30.glUniform1i(uniform(textureProgram,"uFlip"),flip?1:0);quad();}
    private void quad(){GLES30.glBindVertexArray(quadVao);GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,4);}
    private void cameraUniforms(int program,SkyMath.Camera c){GLES30.glUniform2f(uniform(program,"uSize"),width,height);GLES30.glUniform1f(uniform(program,"uScale"),(float)c.scale);vector(program,"uRight",c.right);vector(program,"uUp",c.up);vector(program,"uCenter",c.center);}
    private void vector(int program,String name,double[] v){GLES30.glUniform3f(uniform(program,name),(float)v[0],(float)v[1],(float)v[2]);}
    private int uniform(int program,String name){String key=program+":"+name;Integer value=uniformCache.get(key);if(value==null){value=GLES30.glGetUniformLocation(program,name);uniformCache.put(key,value);}return value;}
    private int program(String vertexPath,String fragmentPath)throws Exception{
        int vertex=shader(GLES30.GL_VERTEX_SHADER,StarCatalog.readText(context,vertexPath)),fragment=shader(GLES30.GL_FRAGMENT_SHADER,StarCatalog.readText(context,fragmentPath)),program=GLES30.glCreateProgram();GLES30.glAttachShader(program,vertex);GLES30.glAttachShader(program,fragment);GLES30.glLinkProgram(program);int[] status=new int[1];GLES30.glGetProgramiv(program,GLES30.GL_LINK_STATUS,status,0);String info=GLES30.glGetProgramInfoLog(program);GLES30.glDeleteShader(vertex);GLES30.glDeleteShader(fragment);if(status[0]==0)throw new IllegalStateException(info);return program;
    }
    private int shader(int type,String source){int shader=GLES30.glCreateShader(type);GLES30.glShaderSource(shader,source);GLES30.glCompileShader(shader);int[] status=new int[1];GLES30.glGetShaderiv(shader,GLES30.GL_COMPILE_STATUS,status,0);if(status[0]==0)throw new IllegalStateException(GLES30.glGetShaderInfoLog(shader));return shader;}
    private void text(Canvas c,String text,float x,float y,float size,int color,Typeface typeface){paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTextSize(size);paint.setTypeface(typeface);paint.setTextAlign(Paint.Align.CENTER);c.drawText(text,x,y,paint);}
    private void drawOverlay(long epoch){
        Canvas c=new Canvas(overlay);c.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);float scale=overlay.getWidth()/320f;c.scale(scale,scale);float h=overlay.getHeight()/scale,top=h*scene.clockTop/100f;boolean minimal=scene.uiMode.equals("minimal");int ink=Color.rgb(242,237,223),muted=Color.rgb(169,174,190);Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL),light=Typeface.create("sans-serif-light",Typeface.NORMAL);
        celestial.drawMoon(c,SkyMath.camera(epoch,scene.lat,scene.lon,scene.azimuth,scene.altitude,scene.fov,320,Math.round(h)),scene,epoch);
        paint.setShader(new android.graphics.LinearGradient(0,0,0,h,new int[]{0x88030613,0x10030613,0x00030613,0x08030613,0xb0030613},new float[]{0,.24f,.51f,.65f,1},android.graphics.Shader.TileMode.CLAMP));paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);c.drawRect(0,0,320,h,paint);paint.setShader(null);
        ZonedDateTime local=Instant.ofEpochMilli(epoch).atZone(ZoneId.of(scene.zone));
        if(allowClock&&!scene.clock.equals("none")){
            text(c,dateFormat.format(local),160,top+12,12,ink,regular);
            if(scene.clock.equals("digital")){text(c,String.format(Locale.ROOT,"%02d",local.getHour()),160,top+112,100,ink,light);text(c,String.format(Locale.ROOT,"%02d",local.getMinute()),160,top+205,100,Color.rgb(221,211,190),light);}
            else drawAnalog(c,local,160,top+138,106,ink,regular);
            if(!minimal)text(c,"此刻，仰望同一片星空",160,top+249,9,muted,regular);
        }
        if(!minimal){
            String view=scene.altitude==90?"天顶 · 90°":SkyMath.direction(scene.azimuth)+" · "+scene.azimuth+"° / 仰角 "+scene.altitude+"°";
            text(c,view,160,h-126,9,muted,regular);
            text(c,String.format(Locale.ROOT,"%.2f° %s / %.2f° %s",Math.abs(scene.lat),scene.lat<0?"S":"N",Math.abs(scene.lon),scene.lon<0?"W":"E"),160,h-108,8,muted,regular);
            text(c,scene.place,160,h-59,14,ink,regular);text(c,scene.systemTime?"当地星空 · 实时":"当地星空 · 指定时间",160,h-40,9,muted,regular);
        }
        if(scene.labels){
            SkyMath.Camera camera=SkyMath.camera(epoch,scene.lat,scene.lon,scene.azimuth,scene.altitude,scene.fov,320,Math.round(h));double lastX=-1000,lastY=-1000;
            for(int i=0;i<catalog.named.length();i++)try{JSONArray n=catalog.named.getJSONArray(i);String name=NAMES.get(n.getString(0));if(name==null||n.getDouble(3)>scene.magnitude)continue;double[] p=camera.project(SkyMath.equatorial(n.getDouble(1),n.getDouble(2)));if(p!=null&&p[0]>20&&p[0]<270&&p[1]>h*.5&&p[1]<h-150&&Math.hypot(p[0]-lastX,p[1]-lastY)>50){text(c,name,(float)p[0]+19,(float)p[1]-7,8,muted,regular);lastX=p[0];lastY=p[1];}}catch(Exception ignored){}
        }
    }
    private void drawAnalog(Canvas c,ZonedDateTime time,float x,float y,float r,int ink,Typeface font){
        c.save();c.translate(x,y);paint.setColor(ink);paint.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<60;i++){double a=i*Math.PI/30;paint.setAlpha(i%5==0?225:75);paint.setStrokeWidth(i%5==0?1.2f:.65f);float in=r-(i%5==0?7:3);c.drawLine((float)Math.sin(a)*r,-(float)Math.cos(a)*r,(float)Math.sin(a)*in,-(float)Math.cos(a)*in,paint);}
        for(int i=1;i<=12;i++){double a=i*Math.PI/6;text(c,Integer.toString(i),(float)Math.sin(a)*(r-20),-(float)Math.cos(a)*(r-20)+3,9,ink,font);}
        hand(c,(time.getHour()%12+time.getMinute()/60.0)*Math.PI/6,r*.52f,3f,ink);hand(c,(time.getMinute()+time.getSecond()/60.0)*Math.PI/30,r*.77f,1.8f,ink);hand(c,time.getSecond()*Math.PI/30,r*.86f,.8f,Color.rgb(212,184,139));paint.setColor(ink);c.drawCircle(0,0,2.8f,paint);c.restore();
    }
    private void hand(Canvas c,double angle,float length,float stroke,int color){paint.setColor(color);paint.setStrokeWidth(stroke);paint.setStrokeCap(Paint.Cap.ROUND);c.drawLine(-(float)Math.sin(angle)*7,(float)Math.cos(angle)*7,(float)Math.sin(angle)*length,-(float)Math.cos(angle)*length,paint);}
    public void close(){settings.close();if(cloudOverlay!=null){cloudOverlay.recycle();cloudOverlay=null;}if(overlay!=null){overlay.recycle();overlay=null;}}
}
