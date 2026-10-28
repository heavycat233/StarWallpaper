package app.starfield.wallpaper;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Packed sorted catalogue. Only one direct vertex buffer is held per app process. */
public final class StarCatalog {
    private static StarCatalog instance;
    public final FloatBuffer vertices;
    public final int count,dynamicCount;
    public final int[] counts=new int[12];
    public final String source,notice;
    public final JSONArray named;
    public static synchronized StarCatalog get(Context context) throws Exception {if(instance==null)instance=new StarCatalog(context);return instance;}
    private StarCatalog(Context context)throws Exception{
        JSONObject meta=new JSONObject(readText(context,"catalogue.json"));count=meta.getInt("count");source=meta.getString("source");notice=meta.getString("notice");named=meta.getJSONArray("named");
        for(int m=3;m<=11;m++)counts[m]=meta.getJSONObject("counts").getInt(Integer.toString(m));
        ByteBuffer packed;
        try(InputStream in=new GZIPInputStream(context.getAssets().open("stars.bin.gz"));ByteArrayOutputStream out=new ByteArrayOutputStream()){in.transferTo(out);packed=ByteBuffer.wrap(out.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);}
        if(packed.remaining()!=count*6)throw new IllegalStateException("星表校验失败");
        vertices=ByteBuffer.allocateDirect(count*5*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        int bright=0;
        for(int i=0;i<count;i++){
            double ra=(packed.getShort()&65535)/65535.0*24,dec=packed.getShort()/32767.0*90;float mag=(packed.get()&255)/16f-2,bv=(packed.get()&255)/64f-.5f;
            if(mag<6.5f)bright++;
            double[] v=SkyMath.equatorial(ra,dec);vertices.put((float)v[0]).put((float)v[1]).put((float)v[2]).put(mag).put(bv);
        }
        dynamicCount=bright;vertices.position(0);
    }
    public static String readText(Context context,String path)throws Exception{try(InputStream in=context.getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){in.transferTo(out);return out.toString(java.nio.charset.StandardCharsets.UTF_8);}}
}
