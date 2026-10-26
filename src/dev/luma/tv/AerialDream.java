package dev.luma.tv;

import android.graphics.*;
import android.media.MediaPlayer;
import android.os.*;
import android.service.dreams.DreamService;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** The screensaver: Apple TV aerial films (Aerials) played full screen one after another, each looping until the
 *  next takes over with a fade through black, under a quiet clock and the place's name, as on tvOS. Before the
 *  first film has downloaded it shows the Silver wallpaper with the clock. Any key wakes the TV. */
public final class AerialDream extends DreamService implements SurfaceHolder.Callback {
    private static final long FILM_MS=10*60*1000L,FADE_MS=900;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SurfaceView surface;
    private View veil;
    private TextView clock,place;
    private MediaPlayer player;
    private List<Aerials.Film> films=Collections.emptyList();
    private int index;
    private final Runnable tick=new Runnable(){public void run(){clock.setText(new SimpleDateFormat("HH:mm",Locale.ENGLISH).format(new Date()));long now=System.currentTimeMillis();handler.postDelayed(this,60000-now%60000+50);}};
    private final Runnable next=()->{veil.animate().alpha(1).setDuration(FADE_MS).withEndAction(()->{index++;play();}).start();};

    @Override public void onAttachedToWindow(){
        super.onAttachedToWindow();
        setInteractive(false);setFullscreen(true);setScreenBright(true);
        float unit=getResources().getDisplayMetrics().widthPixels/960f;
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        ImageView still=new ImageView(this);still.setScaleType(ImageView.ScaleType.CENTER_CROP);still.setImageResource(R.drawable.wp_silver_day);
        root.addView(still,new FrameLayout.LayoutParams(-1,-1));
        surface=new SurfaceView(this);surface.getHolder().addCallback(this);root.addView(surface,new FrameLayout.LayoutParams(-1,-1));
        veil=new View(this);veil.setBackgroundColor(Color.BLACK);veil.setAlpha(0);root.addView(veil,new FrameLayout.LayoutParams(-1,-1));
        clock=label(unit,34,"sans-serif-light");FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);cp.topMargin=Math.round(30*unit);cp.rightMargin=Math.round(48*unit);root.addView(clock,cp);
        place=label(unit,15,"sans-serif-medium");FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.LEFT);pp.bottomMargin=Math.round(36*unit);pp.leftMargin=Math.round(48*unit);root.addView(place,pp);
        setContentView(root);
        films=Aerials.cached(this);
        surface.setVisibility(films.isEmpty()?View.GONE:View.VISIBLE);
        Aerials.ensure(this); // the films still missing come down while the TV rests
    }
    private TextView label(float unit,float size,String family){
        TextView t=new TextView(this);t.setTextColor(0xF2FFFFFF);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,size*unit);
        t.setTypeface(Typeface.create(family,Typeface.NORMAL));t.setShadowLayer(8*unit,0,unit,0x99000000);return t;
    }
    @Override public void onDreamingStarted(){super.onDreamingStarted();handler.post(tick);}
    @Override public void onDreamingStopped(){handler.removeCallbacksAndMessages(null);release();super.onDreamingStopped();}
    @Override public void onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);release();super.onDetachedFromWindow();}

    @Override public void surfaceCreated(SurfaceHolder holder){play();}
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){}
    @Override public void surfaceDestroyed(SurfaceHolder holder){release();}

    /** Plays the current film on a loop, fades in from black, and hands over to the next one after FILM_MS. */
    private void play(){
        release();
        if(films.isEmpty()||!surface.getHolder().getSurface().isValid())return;
        Aerials.Film film=films.get(index%films.size());
        place.setText(film.name);
        try{
            player=new MediaPlayer();player.setDisplay(surface.getHolder());player.setLooping(true);player.setVolume(0,0);
            player.setDataSource(Aerials.file(this,film).getPath());
            player.setOnPreparedListener(p->{p.start();veil.animate().alpha(0).setDuration(FADE_MS).start();});
            player.setOnErrorListener((p,what,extra)->{handler.post(next);return true;});
            player.prepareAsync();
            if(films.size()>1){handler.removeCallbacks(next);handler.postDelayed(next,FILM_MS);}
        }catch(Exception e){release();}
    }
    private void release(){if(player!=null){try{player.release();}catch(RuntimeException ignored){}player=null;}}
}
