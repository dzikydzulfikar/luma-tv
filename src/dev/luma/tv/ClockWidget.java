package dev.luma.tv;

import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.*;

/** The left card on Home: the time, large and light, over today's date, in the quiet type of tvOS. It is not
 *  focusable and does nothing but tell the time; it ticks once a minute, on the minute, while Home is visible. */
final class ClockWidget extends Widget {
    private final TextView time,date;
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;paint();long now=System.currentTimeMillis();postDelayed(this,60000-now%60000+50);}};

    ClockWidget(Context context,WidgetBoard.Host host){
        super(context,host,WidgetBoard.width("clock"));
        time=text("",84,ink(0xFF),"sans-serif-thin");time.setLetterSpacing(-.02f);time.setGravity(Gravity.BOTTOM);
        addView(time,pos(w-56,96,28,26));
        date=text("",17,ink(0xB3),"sans-serif-medium");date.setLetterSpacing(.005f);
        addView(date,pos(w-56,22,30,130));
        paint();
    }
    @Override void setActive(boolean value){super.setActive(value);removeCallbacks(tick);if(value){paint();post(tick);}}
    @Override void refresh(){paint();}
    private void paint(){
        Date now=new Date();
        time.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(now)); // 24-hour, like the clock on the other pages
        date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(now));
    }
}
