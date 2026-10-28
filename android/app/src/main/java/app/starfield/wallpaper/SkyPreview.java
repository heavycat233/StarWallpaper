package app.starfield.wallpaper;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;

public final class SkyPreview extends GLSurfaceView {
    public final StarRenderer renderer;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean running;
    private final Runnable tick=new Runnable(){@Override public void run(){if(!running)return;requestRender();handler.postDelayed(this,50);}};
    public SkyPreview(Context context){super(context);setEGLContextClientVersion(3);setEGLConfigChooser(8,8,8,8,0,0);renderer=new StarRenderer(context);setRenderer(renderer);setRenderMode(RENDERMODE_WHEN_DIRTY);setPreserveEGLContextOnPause(true);}
    @Override public void onResume(){super.onResume();running=true;handler.removeCallbacks(tick);handler.post(tick);}
    @Override public void onPause(){running=false;handler.removeCallbacks(tick);super.onPause();}
    public void close(){running=false;handler.removeCallbacks(tick);renderer.settings.close();}
}
