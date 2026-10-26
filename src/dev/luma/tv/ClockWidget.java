package dev.luma.tv;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** The left card on Home: the time, large and light, over today's date, and the day's prayers for Jakarta (Prayer:
 *  Muhammadiyah's criteria, worked out offline) in one of four looks, chosen with a hidden switch while the design
 *  settles (adb shell am start -n dev.luma.tv/.MainActivity --es luma.clock A|B|C|D):
 *    A  the five prayers as a quiet list past a hairline, the next one lit on a soft pill;
 *    B  the next prayer large, with a countdown, and the five times small beneath;
 *    C  a day glance: sunrise, sunset and the next prayer under thin line icons;
 *    D  no prayers: a narrower card with the time and date centred, balanced against the weather.
 *  A small air-conditioner chip is kept in every look, hidden until Homebridge reports the AC. Nothing here acts,
 *  so nothing takes focus. It ticks once a minute, on the minute, while Home is visible, which also moves the next
 *  prayer on as each one passes and turns the day over at midnight. */
final class ClockWidget extends Widget {
    static String style="B"; // the owner's choice; A, C and D stay as hidden looks
    static final float PAD=24,DIV=296,SIDE=DIV+24,ROW_GAP=2; // PAD: the padding of every Home card
    private final TextView time,date,ac;
    private final ArrayList<TextView> names=new ArrayList<>(),times=new ArrayList<>();
    private final ArrayList<View> rows=new ArrayList<>();
    private TextView heroName,heroTime,countdown;
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;paint();long now=System.currentTimeMillis();postDelayed(this,60000-now%60000+50);}};

    ClockWidget(Context context,WidgetBoard.Host host){
        super(context,host,WidgetBoard.width("clock"));
        boolean centred=style.equals("D");float textW=centred?w-PAD*2:DIV-PAD-12;
        time=text("",84,ink(0xFF),"sans-serif-thin");time.setLetterSpacing(-.02f);time.setGravity(Gravity.BOTTOM|(centred?Gravity.CENTER_HORIZONTAL:0));
        addView(time,pos(textW,96,PAD,26));
        date=text("",17,ink(0xB3),"sans-serif-medium");date.setLetterSpacing(.005f);if(centred)date.setGravity(Gravity.CENTER_HORIZONTAL);
        addView(date,pos(textW,22,centred?PAD:PAD+2,130));
        if(!centred){View divider=new View(context);divider.setBackgroundColor(ink(0x1F));addView(divider,pos(1,WidgetBoard.ROW_H-PAD*2,DIV,PAD));}
        float sideW=w-PAD-SIDE;
        if(style.equals("A"))for(int i=0;i<5;i++){
            FrameLayout row=new FrameLayout(context);
            TextView n=text(Prayer.NAMES[i],14,ink(0x8C),"sans-serif");n.setGravity(Gravity.CENTER_VERTICAL);n.setPadding(px(12),0,0,0);row.addView(n,new FrameLayout.LayoutParams(-1,-1));
            TextView t=text("",14,ink(0x8C),"sans-serif");t.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);t.setPadding(0,0,px(12),0);row.addView(t,new FrameLayout.LayoutParams(-1,-1));
            names.add(n);times.add(t);rows.add(row);addView(row,pos(sideW,24,SIDE,0));
        }
        if(style.equals("B")){
            heroName=text("",15,ink(0xB3),"sans-serif-medium");addView(heroName,pos(140,20,SIDE+2,PAD+8));
            heroTime=text("",44,ink(0xFF),"sans-serif-thin");heroTime.setLetterSpacing(-.01f);heroTime.setGravity(Gravity.BOTTOM);addView(heroTime,pos(sideW,50,SIDE,PAD+26));
            countdown=text("",13,ink(0x8C),"sans-serif");addView(countdown,pos(sideW,17,SIDE+2,PAD+80));
            LinearLayout strip=new LinearLayout(context);addView(strip,pos(sideW-2,31,SIDE+2,130)); // columns spread edge to edge
            for(int i=0;i<5;i++){
                if(i>0)strip.addView(new View(context),new LinearLayout.LayoutParams(0,1,1));
                LinearLayout col=new LinearLayout(context);col.setOrientation(LinearLayout.VERTICAL);strip.addView(col,new LinearLayout.LayoutParams(-2,-1));
                TextView n=text(Prayer.NAMES[i],10.5f,ink(0x73),"sans-serif");col.addView(n,new LinearLayout.LayoutParams(-2,px(16)));
                TextView t=text("",12,ink(0x8C),"sans-serif");col.addView(t,new LinearLayout.LayoutParams(-2,px(15)));
                names.add(n);times.add(t);
            }
        }
        if(style.equals("C")){
            float colW=sideW/3;String[] icons={"sun","moon",null};
            for(int i=0;i<3;i++){
                float x=SIDE+i*colW;
                if(icons[i]!=null){Glyph g=new Glyph(context).kind(icons[i]).color(ink(0x99));addView(g,pos(20,20,x+colW/2-10,56));}
                else{View dot=new View(context);dot.setBackground(oval(ink(0xFF)));addView(dot,pos(7,7,x+colW/2-3.5f,62.5f));}
                TextView t=text("",17,ink(i==2?0xFF:0xB3),i==2?"sans-serif-medium":"sans-serif-light");t.setGravity(Gravity.CENTER_HORIZONTAL);addView(t,pos(colW,22,x,88));
                TextView n=text(i==0?"Sunrise":i==1?"Sunset":"",11.5f,ink(0x80),"sans-serif");n.setGravity(Gravity.CENTER_HORIZONTAL);addView(n,pos(colW,15,x,114));
                times.add(t);names.add(n);
            }
        }
        ac=text("",12,ink(0xB3),"sans-serif-medium");ac.setGravity(Gravity.CENTER);ac.setBackground(round(light()?0x12000000:0x1AFFFFFF,11));ac.setVisibility(GONE);
        addView(ac,centred?pos(96,22,(w-96)/2,WidgetBoard.ROW_H-PAD-12):pos(96,22,w-PAD-96,PAD-2));
        place();paint();
    }
    @Override void setActive(boolean value){super.setActive(value);removeCallbacks(tick);if(value){paint();post(tick);}}
    @Override void refresh(){paint();}
    private void paint(){
        Date now=new Date();
        time.setText(new SimpleDateFormat("HH:mm",Locale.ENGLISH).format(now)); // 24-hour, like the clock on the other pages
        date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.ENGLISH).format(now));
        if(style.equals("D"))return;
        int[] s=Prayer.schedule(now.getTime());int next=s[7];
        if(style.equals("A"))for(int i=0;i<5;i++){
            boolean lit=i==next;times.get(i).setText(Prayer.hm(s[i]));
            for(TextView t:new TextView[]{names.get(i),times.get(i)}){t.setTextColor(ink(lit?0xFF:0x8C));t.setTypeface(Typeface.create(lit?"sans-serif-medium":"sans-serif",Typeface.NORMAL));}
            rows.get(i).setBackground(lit?round(light()?0x12000000:0x17FFFFFF,8):null);
        }
        if(style.equals("B")){
            heroName.setText(Prayer.NAMES[next]);heroTime.setText(Prayer.hm(s[next]));
            int m=Prayer.minutesTo(now.getTime(),s);countdown.setText("in "+(m>=60?m/60+"h "+m%60+"m":m+"m")+(tomorrow(now,s)?" · tomorrow":""));
            for(int i=0;i<5;i++){boolean lit=i==next;times.get(i).setText(Prayer.hm(s[i]));times.get(i).setTextColor(ink(lit?0xFF:0x8C));names.get(i).setTextColor(ink(lit?0xB3:0x73));}
        }
        if(style.equals("C")){
            times.get(0).setText(Prayer.hm(s[5]));times.get(1).setText(Prayer.hm(s[6]));
            times.get(2).setText(Prayer.hm(s[next]));names.get(2).setText(Prayer.NAMES[next]);
        }
    }
    /** After Isya the list is tomorrow's (its Ashar can differ by a minute from today's). */
    private static boolean tomorrow(Date now,int[] s){Calendar c=Calendar.getInstance(Prayer.ZONE);c.setTime(now);return s[7]==0&&c.get(Calendar.HOUR_OF_DAY)>=12;}
    /** Look A: five rows centred in the card, or shorter rows under the AC chip when it shows. Look D: the time and
     *  date lift to make room for the chip below them. */
    private void place(){
        boolean chip=ac.getVisibility()==VISIBLE;
        if(style.equals("D")){float lift=chip?-px(10):0;time.setTranslationY(lift);date.setTranslationY(lift);}
        if(!style.equals("A"))return;
        float h=chip?21:24,top=chip?PAD+26:(WidgetBoard.ROW_H-5*h-4*ROW_GAP)/2;
        for(int i=0;i<rows.size();i++){FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)rows.get(i).getLayoutParams();p.height=px(h);p.topMargin=px(top+i*(h+ROW_GAP));rows.get(i).setLayoutParams(p);}
    }
    /** The AC as the IR blaster last set it, for example ac(true,"24°"): a green dot when on, grey when off. The
     *  blaster has no thermometer, so this is the set temperature, not the room's. null hides the chip. */
    void ac(boolean on,String setting){
        if(setting==null)ac.setVisibility(GONE);
        else{
            SpannableString s=new SpannableString("●  AC · "+setting);
            s.setSpan(new ForegroundColorSpan(on?0xFF34C759:ink(0x59)),0,1,0);
            ac.setText(s);ac.setVisibility(VISIBLE);
        }
        place();
    }
}
