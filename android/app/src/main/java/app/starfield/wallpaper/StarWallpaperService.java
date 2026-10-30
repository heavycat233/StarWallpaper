package app.starfield.wallpaper;

import android.app.KeyguardManager;
import android.app.WallpaperColors;
import android.graphics.Color;
import android.graphics.Rect;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class StarWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine(){return new SkyEngine();}
    final class SkyEngine extends Engine {
        private final HandlerThread thread=new HandlerThread("Starfield-EGL");
        private final Handler renderHandler;
        private final Settings settings=new Settings(StarWallpaperService.this);
        private final LocationTracker location=new LocationTracker(StarWallpaperService.this,settings);
        private final KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        private EGLDisplay display=EGL14.EGL_NO_DISPLAY;
        private EGLContext context=EGL14.EGL_NO_CONTEXT;
        private EGLSurface surface=EGL14.EGL_NO_SURFACE;
        private StarRenderer renderer;
        private volatile boolean visible,surfaceAlive;
        private long lastLockCheck;
        SkyEngine(){thread.start();renderHandler=new Handler(thread.getLooper());}
        private final Runnable draw=new Runnable(){@Override public void run(){
            if(!visible||!surfaceAlive)return;
            try{
                if(renderer==null||surface==EGL14.EGL_NO_SURFACE){
                    SurfaceHolder holder=getSurfaceHolder();
                    if(holder==null)return;
                    init(holder);
                    Rect bounds=holder.getSurfaceFrame();
                    renderer.onSurfaceChanged(null,bounds.width(),bounds.height());
                }
                long now=SystemClock.elapsedRealtime();Settings.Snapshot s=settings.snapshot();
                if(now-lastLockCheck>1000){renderer.allowClock=isPreview()||s.desktopClock||(keyguard!=null&&keyguard.isKeyguardLocked());lastLockCheck=now;}
                renderer.onDrawFrame(null);if(!EGL14.eglSwapBuffers(display,surface)){
                    Log.w("Starfield","Surface swap failed; rebuilding EGL: "+EGL14.eglGetError());
                    release();if(visible&&surfaceAlive)renderHandler.postDelayed(this,1000);return;
                }
                int fps=s.twinkle?s.fps:1;
                if(visible)renderHandler.postDelayed(this,Math.max(10,1000/fps));
            }catch(RuntimeException e){
                Log.e("Starfield","Wallpaper frame failed; retrying",e);
                release();if(visible&&surfaceAlive)renderHandler.postDelayed(this,2000);
            }
        }};
        @Override public void onCreate(SurfaceHolder holder){super.onCreate(holder);setOffsetNotificationsEnabled(false);setTouchEventsEnabled(false);}
        @Override public void onSurfaceCreated(SurfaceHolder holder){super.onSurfaceCreated(holder);surfaceAlive=true;}
        @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int width,int height){
            super.onSurfaceChanged(holder,format,width,height);surfaceAlive=true;
            renderHandler.post(()->{try{if(surface==EGL14.EGL_NO_SURFACE)init(holder);renderer.onSurfaceChanged(null,width,height);renderHandler.removeCallbacks(draw);if(visible)renderHandler.post(draw);}catch(RuntimeException e){Log.e("Starfield","EGL initialization failed; retrying",e);release();if(visible&&surfaceAlive)renderHandler.postDelayed(draw,2000);}});
        }
        @Override public void onSurfaceRedrawNeeded(SurfaceHolder holder){super.onSurfaceRedrawNeeded(holder);renderHandler.removeCallbacks(draw);renderHandler.post(draw);}
        @Override public void onVisibilityChanged(boolean value){visible=value;lastLockCheck=0;renderHandler.removeCallbacks(draw);if(value){location.start(false);renderHandler.post(draw);}else location.stop();}
        @Override public void onWallpaperFlagsChanged(int which){super.onWallpaperFlagsChanged(which);lastLockCheck=0;}
        @Override public WallpaperColors onComputeColors(){return new WallpaperColors(Color.valueOf(0xff0c1018),Color.valueOf(0xff1a2234),null);}
        @Override public void onSurfaceDestroyed(SurfaceHolder holder){surfaceAlive=false;renderHandler.removeCallbacks(draw);CountDownLatch done=new CountDownLatch(1);renderHandler.post(()->{release();done.countDown();});try{done.await(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}super.onSurfaceDestroyed(holder);}
        @Override public void onDestroy(){visible=false;surfaceAlive=false;location.stop();settings.close();renderHandler.removeCallbacksAndMessages(null);renderHandler.post(()->{release();thread.quitSafely();});super.onDestroy();}
        private void init(SurfaceHolder holder){
            display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);int[] versions=new int[2];if(!EGL14.eglInitialize(display,versions,0,versions,1))throw new IllegalStateException("EGL display");
            int[] attributes={EGL14.EGL_RENDERABLE_TYPE,0x0040,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_NONE};EGLConfig[] configs=new EGLConfig[1];int[] count=new int[1];EGL14.eglChooseConfig(display,attributes,0,configs,0,1,count,0);if(count[0]==0)throw new IllegalStateException("OpenGL ES 3 required");
            context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL14.EGL_NONE},0);surface=EGL14.eglCreateWindowSurface(display,configs[0],holder.getSurface(),new int[]{EGL14.EGL_NONE},0);if(surface==EGL14.EGL_NO_SURFACE||!EGL14.eglMakeCurrent(display,surface,surface,context))throw new IllegalStateException("EGL surface");
            EGL14.eglSwapInterval(display,1);renderer=new StarRenderer(StarWallpaperService.this);renderer.onSurfaceCreated(null,null);
        }
        private void release(){
            if(renderer!=null){renderer.close();renderer=null;}
            if(display!=EGL14.EGL_NO_DISPLAY){EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);if(surface!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,surface);if(context!=EGL14.EGL_NO_CONTEXT)EGL14.eglDestroyContext(display,context);EGL14.eglTerminate(display);}
            display=EGL14.EGL_NO_DISPLAY;context=EGL14.EGL_NO_CONTEXT;surface=EGL14.EGL_NO_SURFACE;
        }
    }
}
