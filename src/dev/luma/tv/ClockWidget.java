package dev.luma.tv;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.*;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.*;

/** The left card on Home: the time, large and light, over today's date and, quieter still, the next two prayers for
 *  Jakarta (Prayer: Muhammadiyah's criteria, worked out offline). A small air-conditioner chip waits in the corner,
 *  hidden until Homebridge reports the AC. Nothing here acts, so nothing takes focus. It ticks once a minute, on the
 *  minute, while Home is visible. */
final class ClockWidget extends Widget {
    private final TextView time,date,prayers,ac;
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;paint();long now=System.currentTimeMillis();postDelayed(this,60000-now%60000+50);}};

    ClockWidget(Context context,WidgetBoard.Host host){
        super(context,host,WidgetBoard.width("clock"));
        time=text("",84,ink(0xFF),"sans-serif-thin");time.setLetterSpacing(-.02f);time.setGravity(Gravity.BOTTOM);
        addView(time,pos(w-56,92,28,18));
        date=text("",17,ink(0xB3),"sans-serif-medium");date.setLetterSpacing(.005f);
        addView(date,pos(w-56,22,30,114));
        prayers=text("",14,ink(0x80),"sans-serif");prayers.setLetterSpacing(.01f);
        addView(prayers,pos(w-56,18,30,143));
        ac=text("",12,ink(0xB3),"sans-serif-medium");ac.setGravity(Gravity.CENTER);ac.setBackground(round(light()?0x14000000:0x1AFFFFFF,12));ac.setVisibility(GONE);
        addView(ac,pos(96,24,w-28-96,26));
        paint();
    }
    @Override void setActive(boolean value){super.setActive(value);removeCallbacks(tick);if(value){paint();post(tick);}}
    @Override void refresh(){paint();}
    private void paint(){
        Date now=new Date();
        time.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(now)); // 24-hour, like the clock on the other pages
        date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(now));
        prayers.setText(Prayer.upcoming(now.getTime()));
    }
    /** The AC as the IR blaster last set it, for example ac(true,"24°"): a green dot when on, grey when off. The
     *  blaster has no thermometer, so this is the set temperature, not the room's. null hides the chip. */
    void ac(boolean on,String setting){
        if(setting==null){ac.setVisibility(GONE);return;}
        SpannableString s=new SpannableString("●  AC · "+setting);
        s.setSpan(new ForegroundColorSpan(on?0xFF34C759:ink(0x59)),0,1,0);
        ac.setText(s);ac.setVisibility(VISIBLE);
    }
}
