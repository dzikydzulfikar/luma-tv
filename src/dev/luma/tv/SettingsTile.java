package dev.luma.tv;

import android.graphics.*;

/** The Settings tile, drawn here rather than taken from an app: a machined metal gear in the spirit of a TV settings
 *  icon (an original drawing, not anyone's asset). A fine-toothed silver outer gear casts a soft shadow on a pale
 *  grey tile; inside it a grooved ring, a graphite inner gear and a polished hub sit on the same centre. */
final class SettingsTile {
    static Bitmap draw(int w,int h){
        Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
        p.setShader(new LinearGradient(0,0,0,h,0xFFF5F5F7,0xFFD7D8DC,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
        float cx=w/2f,cy=h/2f,r=h*.4f;
        Path outer=gear(cx,cy,r,r*.9f,36,.5f);
        // Shadow under the gear, then the gear in brushed silver: light from above, darker towards the rim.
        p.setShader(null);p.setColor(0x47000000);p.setMaskFilter(new BlurMaskFilter(r*.07f,BlurMaskFilter.Blur.NORMAL));
        c.save();c.translate(0,r*.045f);c.drawPath(outer,p);c.restore();p.setMaskFilter(null);
        p.setShader(new LinearGradient(0,cy-r,0,cy+r,new int[]{0xFFFAFAFB,0xFFBFC2C8,0xFF6F737B},new float[]{0,.55f,1},Shader.TileMode.CLAMP));c.drawPath(outer,p);
        p.setShader(new RadialGradient(cx,cy,r,new int[]{0x00000000,0x00000000,0x2E000000},new float[]{0,.8f,1},Shader.TileMode.CLAMP));c.drawPath(outer,p);
        // A dark groove, then the face inside it, with a fine inner shadow along its top edge.
        ring(c,p,cx,cy,r*.8f,r*.06f,0xFF4A4D54,0xFF1F2125);
        float face=r*.76f;
        p.setShader(new LinearGradient(0,cy-face,0,cy+face,0xFFC9CBD0,0xFFF3F4F6,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,face,p);
        innerShadow(c,p,cx,cy,face,r*.05f);
        // The inner gear in graphite, its own shadow, a light ring and the hub.
        Path inner=gear(cx,cy,r*.58f,r*.49f,12,.46f);
        p.setShader(null);p.setColor(0x40000000);p.setMaskFilter(new BlurMaskFilter(r*.04f,BlurMaskFilter.Blur.NORMAL));
        c.save();c.translate(0,r*.03f);c.drawPath(inner,p);c.restore();p.setMaskFilter(null);
        p.setShader(new LinearGradient(0,cy-r*.58f,0,cy+r*.58f,0xFF6D7078,0xFF24262A,Shader.TileMode.CLAMP));c.drawPath(inner,p);
        ring(c,p,cx,cy,r*.33f,r*.035f,0xFFE9EAED,0xFFA4A7AD);
        p.setShader(new LinearGradient(0,cy-r*.3f,0,cy+r*.3f,0xFF4A4D54,0xFF1C1E22,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,r*.3f,p);
        p.setShader(new RadialGradient(cx-r*.06f,cy-r*.08f,r*.18f,0xFFF2F3F5,0xFF5C5F66,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,r*.16f,p);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,r*.008f));p.setColor(0x4D000000);c.drawPath(outer,p);p.setStyle(Paint.Style.FILL);
        return b;
    }
    /** A gear outline: n teeth between radius ro (tips) and ri (roots); duty is the share of each pitch a tip takes. */
    private static Path gear(float cx,float cy,float ro,float ri,int n,float duty){
        Path path=new Path();double pitch=2*Math.PI/n,slope=pitch*.12;
        for(int i=0;i<n;i++){
            double a=i*pitch-Math.PI/2,tip=pitch*duty;
            double[] angles={a,a+slope,a+tip-slope,a+tip};float[] radii={ri,ro,ro,ri};
            for(int k=0;k<4;k++){float x=cx+(float)(Math.cos(angles[k])*radii[k]),y=cy+(float)(Math.sin(angles[k])*radii[k]);if(i==0&&k==0)path.moveTo(x,y);else path.lineTo(x,y);}
            path.arcTo(new RectF(cx-ri,cy-ri,cx+ri,cy+ri),(float)Math.toDegrees(a+tip),(float)Math.toDegrees(pitch-tip),false);
        }
        path.close();return path;
    }
    private static void ring(Canvas c,Paint p,float cx,float cy,float radius,float width,int top,int bottom){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setShader(new LinearGradient(0,cy-radius,0,cy+radius,top,bottom,Shader.TileMode.CLAMP));
        c.drawCircle(cx,cy,radius,p);p.setStyle(Paint.Style.FILL);
    }
    /** A soft shadow just inside a disc, heavier at the top, as if the face were set into the gear. */
    private static void innerShadow(Canvas c,Paint p,float cx,float cy,float radius,float depth){
        c.save();Path clip=new Path();clip.addCircle(cx,cy,radius,Path.Direction.CW);c.clipPath(clip);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(depth*2);p.setColor(0x40000000);p.setMaskFilter(new BlurMaskFilter(depth,BlurMaskFilter.Blur.NORMAL));
        c.drawCircle(cx,cy-depth*.6f,radius+depth*.4f,p);p.setMaskFilter(null);p.setStyle(Paint.Style.FILL);c.restore();
    }
}
