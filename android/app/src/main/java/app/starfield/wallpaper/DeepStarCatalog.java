package app.starfield.wallpaper;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.zip.GZIPInputStream;
import org.json.JSONObject;

/** AT-HYG stars absent from the HYG base, in the preview's byte-plane format. */
public final class DeepStarCatalog {
    private static DeepStarCatalog instance;
    public final FloatBuffer vertices;
    public final int count;
    public final int[] counts=new int[12];

    public static synchronized DeepStarCatalog get(Context context)throws Exception {
        if(instance==null)instance=new DeepStarCatalog(context);
        return instance;
    }

    private DeepStarCatalog(Context context)throws Exception {
        JSONObject meta=new JSONObject(StarCatalog.readText(context,"deep-catalogue.json"));
        count=meta.getInt("count");
        for(int m=3;m<=11;m++)counts[m]=meta.getJSONObject("counts").getInt(Integer.toString(m));
        byte[] planes;
        try(InputStream in=new GZIPInputStream(context.getAssets().open("stars-deep.bin.gz"));ByteArrayOutputStream out=new ByteArrayOutputStream(count*6)){
            in.transferTo(out);planes=out.toByteArray();
        }
        if(planes.length!=count*6)throw new IllegalStateException("深星表长度不符");
        vertices=ByteBuffer.allocateDirect(count*5*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for(int i=0;i<count;i++){
            int ra=(planes[i]&255)|((planes[count+i]&255)<<8);
            short dec=(short)((planes[count*2+i]&255)|((planes[count*3+i]&255)<<8));
            float mag=(planes[count*4+i]&255)/16f-2,bv=(planes[count*5+i]&255)/64f-.5f;
            double[] v=SkyMath.equatorial(ra/65535.0*24,dec/32767.0*90);
            vertices.put((float)v[0]).put((float)v[1]).put((float)v[2]).put(mag).put(bv);
        }
        vertices.position(0);
    }
}
