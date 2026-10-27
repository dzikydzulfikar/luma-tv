package dev.luma.tv;

import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;

/** Calm, bright backdrops for the pale wallpaper, made once on the TV (off the UI thread) as still bitmaps, so they
 *  cost nothing per frame. Chosen with a hidden switch while the look settles:
 *    adb shell am start -n dev.luma.tv/.MainActivity --es luma.bg mesh|aerial|pearl|silk
 *  satin  Satin Glow, white satin folds (an image we generated), the default
 *  mesh   warm white melting into cool light blue and lilac, a few wide colour blobs; it drifts imperceptibly
 *  aerial a frame of a downloaded Apple TV aerial (Hawaii), heavily blurred and lifted towards white
 *  pearl  a near-white pearl gradient with one warm glow from the top and a cool shade low on the right
 *  silk   the Silver silk photo, blurred (the previous look) */
final class Backdrops {
    static final String[] NAMES={"satin","mesh","aerial","pearl","silk"};

    /** Satin Glow, decoded once straight to the screen's size: a still bitmap, no extra layer per frame. */
    static Bitmap satin(Context c,int w,int h){
        BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredConfig=Bitmap.Config.ARGB_8888;o.inScaled=false;
        Bitmap b=BitmapFactory.decodeResource(c.getResources(),R.drawable.bg_satin,o);if(b==null)return null;
        if(b.getWidth()==w&&b.getHeight()==h)return b;Bitmap s=Bitmap.createScaledBitmap(b,w,h,true);b.recycle();return s;
    }
    static Bitmap mesh(int w,int h){
        Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
        c.drawColor(0xFFF8F6F3);
        blob(c,p,w*.08f,h*.05f,w*.55f,0xFFFADFCD);  // peach, top left
        blob(c,p,w*.92f,h*.12f,w*.62f,0xFFC9DCF6);  // cool light blue, top right
        blob(c,p,w*.18f,h*1.0f,w*.6f,0xFFDFD2F3);   // lilac, bottom left
        blob(c,p,w*.78f,h*.95f,w*.5f,0xFFDDEAF4);   // pale ice, bottom right
        blob(c,p,w*.5f,h*.45f,w*.45f,0xFFFFFFFF);   // a bright centre keeps the cards and text clean
        return b;
    }
    static Bitmap pearl(int w,int h){
        Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
        p.setShader(new LinearGradient(0,0,0,h,0xFFEFF1F5,0xFFF9F8F6,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
        blob(c,p,w*.42f,-h*.15f,w*.6f,0xFFFFF4E8);
        blob(c,p,w*1.02f,h*.9f,w*.5f,0xFFE2E7EF);
        return b;
    }
    /** A frame of Hawaii (else any cached aerial), blurred and lifted towards white. null if none is on the TV yet. */
    static Bitmap aerial(Context context,int w,int h){
        Aerials.Film film=null;
        for(Aerials.Film f:Aerials.cached(context)){if(f.name.equals("Hawaii")){film=f;break;}if(film==null)film=f;}
        if(film==null)return null;
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try{
            r.setDataSource(Aerials.file(context,film).getPath());
            Bitmap frame=r.getFrameAtTime(40_000_000L,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);if(frame==null)return null;
            Bitmap soft=soften(frame),b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
            p.setColorFilter(new ColorMatrixColorFilter(saturation(.7f)));c.drawBitmap(soft,null,new RectF(0,0,w,h),p);soft.recycle();
            c.drawColor(0x9EF7F7F8); // lifted: about 62% white over the picture
            return b;
        }catch(RuntimeException e){return null;}
        finally{try{r.release();}catch(Exception ignored){}}
    }
    /** A wide blur: shrink in steps, then grow back with filtering. */
    static Bitmap soften(Bitmap photo){
        int w=photo.getWidth(),h=photo.getHeight();Bitmap b=photo;
        for(int k:new int[]{4,16,48}){Bitmap n=Bitmap.createScaledBitmap(b,Math.max(1,w/k),Math.max(1,h/k),true);if(b!=photo)b.recycle();b=n;}
        for(int k:new int[]{16,4}){Bitmap n=Bitmap.createScaledBitmap(b,w/k,h/k,true);b.recycle();b=n;}
        photo.recycle();return b;
    }
    private static void blob(Canvas c,Paint p,float x,float y,float radius,int color){
        p.setShader(new RadialGradient(x,y,radius,new int[]{color,color&0x00FFFFFF|0x80000000,color&0x00FFFFFF},new float[]{0,.45f,1},Shader.TileMode.CLAMP));
        c.drawCircle(x,y,radius,p);
    }
    private static float[] saturation(float s){ColorMatrix m=new ColorMatrix();m.setSaturation(s);return m.getArray();}
}
