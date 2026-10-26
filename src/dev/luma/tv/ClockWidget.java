package dev.luma.tv;

import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** The left card on Home: the time, large and light, over today's date, in the quiet type of tvOS, with a small
 *  Control Center beside it: Wi-Fi, Picture & Sound, Accessories and Screensaver as four pills. Each pill shows its
 *  state and opens the matching page of the TV's own Settings (Screensaver starts it now). The time ticks once a
 *  minute, on the minute, while Home is visible; the pills refresh with it. */
final class ClockWidget extends Widget {
    static final float PILL_W=118,PILL_H=64,GAP=8,PILLS_X=560-18-PILL_W*2-GAP;
    private final TextView time,date;
    private final Pill network,picture,accessories,screensaver;
    private final DecelerateInterpolator decelerate=new DecelerateInterpolator(1.6f);
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;paint();update();long now=System.currentTimeMillis();postDelayed(this,60000-now%60000+50);}};

    ClockWidget(Context context,WidgetBoard.Host host){
        super(context,host,WidgetBoard.width("clock"));
        time=text("",84,ink(0xFF),"sans-serif-thin");time.setLetterSpacing(-.02f);time.setGravity(Gravity.BOTTOM);
        addView(time,pos(PILLS_X-40,96,28,26));
        date=text("",17,ink(0xB3),"sans-serif-medium");date.setLetterSpacing(.005f);
        addView(date,pos(PILLS_X-40,22,30,130));
        float top=(WidgetBoard.ROW_H-PILL_H*2-GAP)/2;
        network=pill("wifi","Wi-Fi",0,0,top,()->open(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        picture=pill("tv","Picture & Sound",1,1,top,()->open(new Intent(Settings.ACTION_SOUND_SETTINGS)));
        accessories=pill("remote","Accessories",2,0,top+PILL_H+GAP,()->open(new Intent("com.google.android.intent.action.CONNECT_INPUT")));
        screensaver=pill("moon","Screensaver",3,1,top+PILL_H+GAP,()->open(new Intent("com.google.android.pano.action.SLEEP")));
        accessories.set("Remote, Bluetooth",false);
        screensaver.set("Start now",false);
        paint();update();
    }
    @Override void setActive(boolean value){super.setActive(value);removeCallbacks(tick);if(value){paint();update();post(tick);}}
    @Override void refresh(){paint();update();}
    private void paint(){
        Date now=new Date();
        time.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(now)); // 24-hour, like the clock on the other pages
        date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(now));
    }
    private Pill pill(String glyph,String title,int index,int col,float y,Runnable action){
        Pill p=new Pill(glyph,title,index,action);addView(p,pos(PILL_W,PILL_H,PILLS_X+col*(PILL_W+GAP),y));return p;
    }
    private void open(Intent intent){try{host.start(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException e){host.start(new Intent(Settings.ACTION_SETTINGS));}}

    /** The network and the picture, as Control would show them: signal strength (not the network's name) and the
     *  output mode. */
    @SuppressWarnings("deprecation")
    private void update(){
        Context c=getContext();
        try{
            ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities caps=cm.getNetworkCapabilities(cm.getActiveNetwork());
            if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){network.glyph.kind("ethernet");network.name.setText("Ethernet");network.set("Connected",true);}
            else if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                WifiInfo info=((WifiManager)c.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
                int level=info==null?0:WifiManager.calculateSignalLevel(info.getRssi(),4);
                boolean online=caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                network.glyph.kind("wifi");network.name.setText("Wi-Fi");
                network.set(online?new String[]{"Weak","Fair","Good","Excellent"}[Math.max(0,Math.min(3,level))]:"No internet",true);
            } else {network.glyph.kind("offline");network.name.setText("Wi-Fi");network.set("Not connected",false);}
        }catch(RuntimeException e){network.set("Settings",false);}
        try{
            Display.Mode mode=host.activity().getWindowManager().getDefaultDisplay().getMode();
            int h=Math.min(mode.getPhysicalHeight(),mode.getPhysicalWidth());
            picture.set((h>=2160?"4K":h>=1080?"1080p":h>=720?"720p":h+"p")+" · "+Math.round(mode.getRefreshRate())+" Hz",false);
        }catch(RuntimeException e){picture.set("Display, audio",false);}
    }

    /** A Control Center pill: a round glyph over a name and a state. Focus lifts it like an app icon (a decelerated
     *  scale with a soft shadow, no bounce) and turns it white; a connection that is up tints the glyph blue. */
    final class Pill extends FrameLayout {
        final View disc;final Glyph glyph;final TextView name,state;boolean focused,on;
        Pill(String kind,String title,int index,Runnable action){
            super(ClockWidget.this.getContext());
            disc=new View(getContext());addView(disc,pos(26,26,10,9));
            glyph=new Glyph(getContext()).kind(kind);addView(glyph,pos(14,14,16,15));
            name=text(title,12,ink(0xF2),"sans-serif-medium");addView(name,pos(PILL_W-20,15,11,39-1));
            state=text("",10.5f,ink(0x8C),"sans-serif");addView(state,pos(PILL_W-20,13,11,52-1));
            setPivotX(px(PILL_W/2));setPivotY(px(PILL_H/2));setFocusable(true);setFocusableInTouchMode(true);setSoundEffectsEnabled(false);
            setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            setOnFocusChangeListener((v,f)->{
                focused=f;paintPill();animate().cancel();float s=f?1.08f:1f;
                if(motion())animate().scaleX(s).scaleY(s).translationZ(f?px(10):0).setDuration(f?240:180).setInterpolator(decelerate).start();
                else{setScaleX(s);setScaleY(s);}
                if(f)host.step(index);
            });
            setOnClickListener(v->{host.open();press(this,1.08f);action.run();});
            paintPill();
        }
        void set(String status,boolean active){if(!status.contentEquals(state.getText()))state.setText(status);if(active!=on){on=active;paintPill();}}
        void paintPill(){
            GradientDrawable bg=round(focused?0xF7FFFFFF:(light()?0x73FFFFFF:0x1AFFFFFF),14);
            if(!focused)bg.setStroke(Math.max(1,px(.6f)),light()?0x0F000000:0x14FFFFFF);
            setBackground(bg);
            disc.setBackground(oval(on?0xFF0A84FF:(light()||focused?0x14000000:0x24FFFFFF)));
            int text=focused?0xFF1D1D1F:ink(0xF2);
            glyph.color(on?Color.WHITE:text);name.setTextColor(text);state.setTextColor(focused?0x991D1D1F:ink(0x8C));
        }
    }
}
