package dev.luma.tv;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** A tvOS-style top shelf on Home: Windah Basudara's live stream (when there is one) and recent streams, read from
 *  the channel's public /streams page without an API key (RSS as a fallback), cached on disk and refreshed at most
 *  every 15 minutes. OK opens the video in the YouTube app. Offline and before the first fetch it shows the channel. */
final class Shelf extends FrameLayout {
    interface Host { void up(); void down(); boolean light(); int px(float value); }

    static final String YOUTUBE="com.google.android.youtube.tv";
    static final String CHANNEL="https://www.youtube.com/@windahbasudara";
    private static final String PAGE="https://www.youtube.com/@WindahBasudara/streams";
    private static final String FEED="https://www.youtube.com/feeds/videos.xml?channel_id=UCoIiiHof6BJ85PLuLkuxuhw";
    private static final long FRESH=15*60*1000L;
    private static final int MAX=12;
    private static List<Streams.Video> videos=new ArrayList<>();
    private static long fetched;
    private static boolean fetching;
    private static final LruCache<String,Bitmap> thumbs=new LruCache<>(MAX);
    private static final ExecutorService net=Executors.newSingleThreadExecutor();
    private static final Handler main=new Handler(Looper.getMainLooper());

    private final Host host;
    private final LinearLayout row;
    private final HorizontalScrollView scroll;
    private final TextView heading;
    private boolean attached;

    Shelf(Context context,Host host){
        super(context);this.host=host;setClipChildren(false);setClipToPadding(false);
        heading=new TextView(context);heading.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,host.px(15));
        heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));heading.setIncludeFontPadding(false);
        LayoutParams hp=new LayoutParams(-2,-2);hp.leftMargin=host.px(50);addView(heading,hp);
        // The row scrolls itself (see glide): the platform's jump-to-focus is turned off.
        scroll=new HorizontalScrollView(context){@Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect rect){return 0;}};scroll.setHorizontalScrollBarEnabled(false);scroll.setClipChildren(false);scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setPadding(host.px(48),host.px(14),host.px(48),0);
        row=new LinearLayout(context);row.setClipChildren(false);row.setClipToPadding(false);
        scroll.addView(row,new HorizontalScrollView.LayoutParams(-2,-1));
        LayoutParams sp=new LayoutParams(-1,-1);sp.topMargin=host.px(20);addView(scroll,sp);
        if(videos.isEmpty())load(context);
        bind();
    }

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;refresh();}
    @Override protected void onDetachedFromWindow(){attached=false;super.onDetachedFromWindow();}

    /** Fetches again when the list is older than 15 minutes (Home calls this on resume). */
    void refresh(){
        if(fetching||System.currentTimeMillis()-fetched<FRESH)return;
        fetching=true;Context app=getContext().getApplicationContext();
        net.execute(()->{
            List<Streams.Video> list=new ArrayList<>();
            try{list=Streams.parsePage(get(PAGE),MAX);}catch(Exception ignored){}
            if(list.isEmpty())try{list=Streams.parseFeed(get(FEED),MAX);}catch(Exception ignored){}
            final List<Streams.Video> found=list;
            main.post(()->{
                fetching=false;fetched=System.currentTimeMillis(); // also after a failure: offline, try again in 15 minutes
                if(found.isEmpty())return;
                videos=found;save(app);if(attached)bind();
            });
        });
    }

    View first(){return row.getChildCount()>0?row.getChildAt(0):null;}
    boolean focusCurrent(){View f=first();if(f==null)return false;f.requestFocus();return true;}

    private void bind(){
        boolean hadFocus=row.hasFocus();row.removeAllViews();
        boolean live=!videos.isEmpty()&&videos.get(0).live;
        heading.setText(live?"Windah Basudara · Live now":"Windah Basudara · Recent streams");
        heading.setTextColor(host.light()?0xCC1D1D1F:0xDDFFFFFF);
        if(videos.isEmpty()){addCard(null,0);}
        else for(int i=0;i<videos.size();i++)addCard(videos.get(i),i);
        if(hadFocus)focusCurrent();
    }

    private void addCard(Streams.Video video,int index){
        Context c=getContext();int w=host.px(272),h=host.px(153);
        FrameLayout card=new FrameLayout(c);card.setFocusable(true);card.setFocusableInTouchMode(true);card.setClipChildren(false);
        FrameLayout face=new FrameLayout(c);GradientDrawable bg=new GradientDrawable();bg.setColor(0xFF2A2A2E);bg.setCornerRadius(host.px(12)); // the radius of the app tiles
        face.setBackground(bg);face.setClipToOutline(true);face.setLayerType(View.LAYER_TYPE_HARDWARE,null);face.setElevation(host.px(2));
        ImageView image=new ImageView(c);image.setScaleType(ImageView.ScaleType.CENTER_CROP);face.addView(image,new LayoutParams(-1,-1));
        card.addView(face,new LayoutParams(w,h));
        if(video!=null&&video.live){
            TextView badge=new TextView(c);badge.setText("LIVE");badge.setTextColor(Color.WHITE);badge.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,host.px(10));
            badge.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));badge.setPadding(host.px(7),host.px(3),host.px(7),host.px(3));
            GradientDrawable red=new GradientDrawable();red.setColor(0xFFE62117);red.setCornerRadius(host.px(4));badge.setBackground(red);
            LayoutParams bp=new LayoutParams(-2,-2);bp.leftMargin=host.px(10);bp.topMargin=host.px(10);face.addView(badge,bp);
        }
        TextView title=new TextView(c);title.setText(video==null?"Windah Basudara on YouTube":video.title);title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,host.px(12));
        title.setTextColor(host.light()?0xFF1D1D1F:0xF2FFFFFF);title.setIncludeFontPadding(false);title.setLineSpacing(host.px(2),1);
        LayoutParams tp=new LayoutParams(w,-2);tp.topMargin=h+host.px(9);card.addView(title,tp);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(w,-1);if(index>0)cp.leftMargin=host.px(18);row.addView(card,cp);
        if(video==null)image.setImageResource(R.drawable.windah_banner);else thumb(video,image);
        card.setOnFocusChangeListener((v,f)->{
            float s=f?1.08f:1f;
            face.animate().cancel();
            face.animate().scaleX(s).scaleY(s).translationY(f?-host.px(3):0).translationZ(f?host.px(10):0).setDuration(f?260:200).setInterpolator(EASE).withLayer().start();
            if(f)glide(v);
        });
        card.setOnClickListener(v->open(video==null?CHANNEL+"/streams":"https://www.youtube.com/watch?v="+video.id));
        card.setOnKeyListener((v,key,e)->{
            if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;
            if(key==KeyEvent.KEYCODE_DPAD_UP){host.up();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_DOWN){host.down();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_LEFT&&index==0)return true;
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&index==row.getChildCount()-1)return true;
            return false;
        });
    }

    private static final android.view.animation.Interpolator EASE=new android.view.animation.DecelerateInterpolator(1.6f);
    private android.animation.ObjectAnimator slide;
    /** tvOS keeps the focused card in place at the left and moves the row under it, with one eased glide. */
    private void glide(View card){
        int target=Math.max(0,Math.min(card.getLeft(),row.getWidth()-scroll.getWidth()+scroll.getPaddingLeft()+scroll.getPaddingRight()));
        if(slide!=null)slide.cancel();
        if(target==scroll.getScrollX())return;
        slide=android.animation.ObjectAnimator.ofInt(scroll,"scrollX",scroll.getScrollX(),target);
        slide.setDuration(320);slide.setInterpolator(EASE);slide.start();
    }

    /** Opens a YouTube link in the official YouTube app; another handler only if that app cannot take it. */
    static void open(Context c,String url){
        Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(url)).setPackage(YOUTUBE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{c.startActivity(intent);}catch(ActivityNotFoundException e){
            try{c.startActivity(intent.setPackage(null));}catch(ActivityNotFoundException ignored){Toast.makeText(c,"YouTube is not installed.",Toast.LENGTH_SHORT).show();}
        }
    }
    private void open(String url){open(getContext(),url);}

    private void thumb(Streams.Video video,ImageView image){
        Bitmap cached=thumbs.get(video.id);if(cached!=null){image.setImageBitmap(cached);return;}
        image.setTag(video.id);
        net.execute(()->{
            Bitmap b=null;
            try{HttpURLConnection con=connect(video.thumb);BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredConfig=Bitmap.Config.RGB_565;
                try(InputStream in=con.getInputStream()){b=BitmapFactory.decodeStream(in,null,o);}}catch(Exception ignored){}
            final Bitmap done=b;
            main.post(()->{if(done==null)return;thumbs.put(video.id,done);if(video.id.equals(image.getTag()))image.setImageBitmap(done);});
        });
    }

    private static HttpURLConnection connect(String url)throws IOException{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36");
        c.setRequestProperty("Accept-Language","id-ID,id;q=0.9");c.setRequestProperty("Cookie","SOCS=CAI; CONSENT=YES+1");
        return c;
    }
    private static String get(String url)throws IOException{
        HttpURLConnection c=connect(url);
        try(InputStream in=c.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream(1<<20);byte[] buf=new byte[16384];int n;
            while((n=in.read(buf))>0)out.write(buf,0,n);return out.toString("UTF-8");}
        finally{c.disconnect();}
    }

    private static File cache(Context c){return new File(c.getCacheDir(),"windah-streams.json");}
    private static void save(Context c){
        try{JSONArray a=new JSONArray();for(Streams.Video v:videos)a.put(new JSONObject().put("id",v.id).put("title",v.title).put("thumb",v.thumb).put("live",v.live));
            try(FileOutputStream out=new FileOutputStream(cache(c))){out.write(new JSONObject().put("time",fetched).put("videos",a).toString().getBytes("UTF-8"));}}catch(Exception ignored){}
    }
    /** The last list saved, so Home shows streams at once (and offline); a fresh fetch follows when it is old. */
    private static void load(Context c){
        try(FileInputStream in=new FileInputStream(cache(c))){byte[] b=new byte[(int)cache(c).length()];int read=0;while(read<b.length){int n=in.read(b,read,b.length-read);if(n<0)break;read+=n;}
            JSONObject o=new JSONObject(new String(b,0,read,"UTF-8"));JSONArray a=o.getJSONArray("videos");ArrayList<Streams.Video> list=new ArrayList<>();
            for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);Streams.Video v=new Streams.Video();v.id=j.getString("id");v.title=j.getString("title");v.thumb=j.getString("thumb");v.live=j.optBoolean("live");list.add(v);}
            videos=list;fetched=o.optLong("time");}catch(Exception ignored){}
    }
}
