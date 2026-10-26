package dev.luma.tv;

import android.content.Context;
import android.view.*;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.*;

/** The left card on Home: the time, large and light, over today's date, in the quiet type of tvOS, with the next
 *  prayer for Jakarta on the right (Prayer, Muhammadiyah's criteria, worked out offline). A slot above it is kept
 *  for the air conditioner and the room's temperature, hidden until something fills it. Nothing here acts, so
 *  nothing takes focus. It ticks once a minute, on the minute, while Home is visible. */
final class ClockWidget extends Widget {
    static final float SIDE_W=170;
    private final TextView time,date,prayerName,prayerTime,climate;
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;paint();long now=System.currentTimeMillis();postDelayed(this,60000-now%60000+50);}};

    ClockWidget(Context context,WidgetBoard.Host host){
        super(context,host,WidgetBoard.width("clock"));
        float side=w-28-SIDE_W;
        time=text("",84,ink(0xFF),"sans-serif-thin");time.setLetterSpacing(-.02f);time.setGravity(Gravity.BOTTOM);
        addView(time,pos(side-40,96,28,26));
        date=text("",17,ink(0xB3),"sans-serif-medium");date.setLetterSpacing(.005f);
        addView(date,pos(side-40,22,30,130));
        climate=text("",13,ink(0x99),"sans-serif-medium");climate.setGravity(Gravity.RIGHT);climate.setVisibility(GONE);
        addView(climate,pos(SIDE_W,18,side,30));
        prayerName=text("",17,ink(0xB3),"sans-serif-medium");prayerName.setGravity(Gravity.RIGHT);
        addView(prayerName,pos(SIDE_W,22,side,62));
        prayerTime=text("",44,ink(0xF2),"sans-serif-thin");prayerTime.setGravity(Gravity.RIGHT|Gravity.BOTTOM);prayerTime.setLetterSpacing(-.01f);
        addView(prayerTime,pos(SIDE_W,50,side,82));
        TextView method=text("Muhammadiyah",12,ink(0x73),"sans-serif");method.setGravity(Gravity.RIGHT);method.setLetterSpacing(.02f);
        addView(method,pos(SIDE_W,16,side,135));
        paint();
    }
    @Override void setActive(boolean value){super.setActive(value);removeCallbacks(tick);if(value){paint();post(tick);}}
    @Override void refresh(){paint();}
    private void paint(){
        Date now=new Date();
        time.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(now)); // 24-hour, like the clock on the other pages
        date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(now));
        Object[] next=Prayer.next(now.getTime());
        prayerName.setText(Prayer.NAMES[(Integer)next[0]]);prayerTime.setText((String)next[1]);
    }
    /** The air conditioner and the room, for example "AC 24° · Kamar 27°", once Homebridge reports them; null hides it. */
    void climate(String line){climate.setText(line==null?"":line);climate.setVisibility(line==null?GONE:VISIBLE);}
}
