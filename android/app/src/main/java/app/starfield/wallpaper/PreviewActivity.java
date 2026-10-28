package app.starfield.wallpaper;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

public final class PreviewActivity extends Activity {
    private SkyPreview sky;
    @Override public void onCreate(Bundle state){super.onCreate(state);FrameLayout root=new FrameLayout(this);root.setContentDescription("全屏预览，可使用系统返回手势退出");sky=new SkyPreview(this);root.addView(sky,new FrameLayout.LayoutParams(-1,-1));setContentView(root);getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
    @Override protected void onResume(){super.onResume();sky.onResume();}
    @Override protected void onPause(){sky.onPause();super.onPause();}
    @Override protected void onDestroy(){sky.close();super.onDestroy();}
}
