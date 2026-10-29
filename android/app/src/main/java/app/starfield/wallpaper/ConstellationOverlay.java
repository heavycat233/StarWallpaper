package app.starfield.wallpaper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;

/** J2000 stick figures and Chinese names, projected by the same camera as the stars. */
final class ConstellationOverlay {
    private final ArrayList<Figure> figures=new ArrayList<>();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);

    private static final class Figure {
        final String code,name;
        final double[] label;
        final ArrayList<double[][]> paths=new ArrayList<>();
        Figure(JSONArray data)throws Exception {
            code=data.getString(0);name=data.getString(1);
            label=SkyMath.equatorial(data.getDouble(2)/15,data.getDouble(3));
            JSONArray lines=data.getJSONArray(4);
            for(int i=0;i<lines.length();i++){
                JSONArray points=lines.getJSONArray(i);double[][] path=new double[points.length()][];
                for(int j=0;j<points.length();j++){
                    JSONArray point=points.getJSONArray(j);
                    path[j]=SkyMath.equatorial(point.getDouble(0)/15,point.getDouble(1));
                }
                paths.add(path);
            }
        }
    }

    ConstellationOverlay(Context context)throws Exception {
        JSONObject data=new JSONObject(StarCatalog.readText(context,"constellations.json"));
        JSONArray items=data.getJSONArray("figures");
        for(int i=0;i<items.length();i++)figures.add(new Figure(items.getJSONArray(i)));
        if(figures.size()!=89)throw new IllegalStateException("星座数据不完整");
    }

    void draw(Canvas canvas,SkyMath.Camera camera){
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.75f);paint.setColor(0x8497a9c8);
        for(Figure figure:figures)for(double[][] path:figure.paths){
            double[] previous=null;
            for(double[] point:path){
                double[] current=camera.project(point);
                if(previous!=null&&current!=null)canvas.drawLine((float)previous[0],(float)previous[1],(float)current[0],(float)current[1],paint);
                previous=current;
            }
        }
        paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(10);
        paint.setColor(0xc7e0d9c7);
        ArrayList<double[]> placed=new ArrayList<>();HashSet<String> named=new HashSet<>();
        for(Figure figure:figures){
            if(!named.add(figure.code))continue;
            double[] point=camera.project(figure.label);
            if(point==null||point[0]<25||point[0]>camera.width-25||point[1]<25||point[1]>camera.height-25)continue;
            boolean crowded=false;
            for(double[] other:placed)if(Math.hypot(point[0]-other[0],point[1]-other[1])<52){crowded=true;break;}
            if(!crowded){canvas.drawText(figure.name,(float)point[0],(float)point[1],paint);placed.add(point);}
        }
    }
}
