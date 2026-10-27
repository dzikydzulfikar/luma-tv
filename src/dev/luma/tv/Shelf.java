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

/** A tvOS-style row of YouTube videos below Home's first screen, from a Source: Windah Basudara's live and recent
 *  streams (his public /streams page, RSS as a fallback), or Recommended for You (Recommended). Read without an
 *  API key, cached on disk and refetched when the Source's freshness runs out. OK opens the video in the YouTube
 *  app. Offline and before the first fetch, Windah's row shows the channel and Recommended's row is hidden. */
final class Shelf extends FrameLayout {
    interface Host { void up(); void down(); boolean light(); int px(float value); }
    interface Fetch { List<Streams.Video> fetch(Context context) throws Exception; }

    /** Where a row's videos come from, and what is known of them; it outlives the rows Home rebuilds. */
    static final class Source {
        final String key;final long fresh;final Fetch fetch;final boolean channelLine;
        List<Streams.Video> videos=new ArrayList<>();long fetched;boolean fetching,loaded;
        Source(String key,long fresh,boolean channelLine,Fetch fetch){this.key=key;this.fresh=fresh;this.channelLine=channelLine;this.fetch=fetch;}
    }
    static final String YOUTUBE="com.google.android.youtube.tv";
    static final String CHANNEL="https://www.youtube.com/@windahbasudara";
    private static final String PAGE="https://www.youtube.com/@WindahBasudara/streams";
    private static final String FEED="https://www.youtube.com/feeds/videos.xml?channel_id=UCoIiiHof6BJ85PLuLkuxuhw";
    private static final int MAX=12;
    static final Source WINDAH=new Source("windah-streams",15*60*1000L,false,c->{
        List<Streams.Video> list=new ArrayList<>();
        try{list=Streams.parsePage(get(PAGE),MAX);}catch(Exception ignored){}
        if(list.isEmpty())list=Streams.parseFeed(get(FEED),MAX);
        return list;});
    static final Source RECOMMENDED=new Source("recommended",Recommended.FRESH,true,Recommended::fetch);
    private static final LruCache<String,Bitmap> thumbs=new LruCache<>(40);
    private static final ExecutorService net=Executors.newSingleThreadExecutor();
    private static final Handler main=new Handler(Looper.getMainLooper());

    private final Host host;private final Source source;
    private final LinearLayout row;
    private final HorizontalScrollView scroll;
    private final TextView heading;
    private boolean attached;

    Shelf(Context context,Host host,Source source){
        super(context);this.host=host;this.source=source;setClipChildren(false);setClipToPadding(false);
        heading=new TextView(context);heading.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,host.px(15));
        heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));heading.setIncludeFontPadding(false);
        LayoutParams hp=new LayoutParams(-2,-2);hp.leftMargin=host.px(50);addView(heading,hp);
        // The row scrolls itself (see glide): the platform's jump-to-focus is turned off.
        scroll=new HorizontalScrollView(context){@Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect rect){return 0;}};scroll.setHorizontalScrollBarEnabled(false);scroll.setClipChildren(false);scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setPadding(host.px(48),host.px(14),host.px(48),0);
        row=new LinearLayout(context);row.setClipChildren(false);row.setClipToPadding(false);
        scroll.addView(row,new HorizontalScrollView.LayoutParams(-2,-1));
        LayoutParams sp=new LayoutParams(-1,-1);sp.topMargin=host.px(20);addView(scroll,sp);
        if(!source.loaded){source.loaded=true;load(context,source);}
        bind();
    }

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;refresh();}
    @Override protected void onDetachedFromWindow(){attached=false;super.onDetachedFromWindow();}

    /** Fetches again when the list is older than the Source allows (Home calls this on resume). */
    void refresh(){
        Source s=source;
        if(s.fetching||System.currentTimeMillis()-s.fetched<s.fresh)return;
        s.fetching=true;Context app=getContext().getApplicationContext();
        net.execute(()->{
            List<Streams.Video> list=new ArrayList<>();
            try{list=s.fetch.fetch(app);}catch(Exception e){android.util.Log.w("LumaShelf",s.key+": "+e);}
            final List<Streams.Video> found=list;
            main.post(()->{
                s.fetching=false;s.fetched=System.currentTimeMillis(); // also after a failure: offline, try again later
                if(found.isEmpty())return;
                s.videos=found;save(app,s);if(attached)bind();
            });
        });
    }
    /** Nothing to show yet (Recommended before its first fetch): Home leaves the row out of the way. */
    boolean empty(){return source!=WINDAH&&source.videos.isEmpty();}

    View first(){return row.getChildCount()>0?row.getChildAt(0):null;}
    boolean focusCurrent(){View f=first();if(f==null)return false;f.requestFocus();return true;}

    private void bind(){
        boolean hadFocus=row.hasFocus();row.removeAllViews();List<Streams.Video> videos=source.videos;
        if(source==WINDAH){boolean live=!videos.isEmpty()&&videos.get(0).live;heading.setText(live?"Windah Basudara · Live now":"Windah Basudara · Recent streams");}
        else heading.setText("Recommended for You");
        heading.setTextColor(host.light()?0xCC1D1D1F:0xDDFFFFFF);
        setVisibility(empty()?INVISIBLE:VISIBLE);
        if(videos.isEmpty()){if(source==WINDAH)addCard(null,0);}
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
        if(source.channelLine&&video!=null&&video.channel!=null){ // the channel under the title, quieter
            title.setMaxLines(1);TextView channel=new TextView(c);channel.setText(video.channel);channel.setSingleLine(true);channel.setEllipsize(android.text.TextUtils.TruncateAt.END);
            channel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,host.px(11));channel.setTextColor(host.light()?0x991D1D1F:0x99FFFFFF);channel.setIncludeFontPadding(false);
            LayoutParams chp=new LayoutParams(w,-2);chp.topMargin=h+host.px(27);card.addView(channel,chp);
        }
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
            // Decoded at about the card's size: a 1280x720 thumbnail on a 544x306 card cost texture memory and upload time.
            try{HttpURLConnection con=connect(video.thumb);BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredConfig=Bitmap.Config.RGB_565;
                byte[] data;try(InputStream in=con.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[16384];int n;while((n=in.read(buf))>0)out.write(buf,0,n);data=out.toByteArray();}
                o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);
                int want=host.px(272);o.inSampleSize=1;while(o.outWidth/(o.inSampleSize*2)>=want)o.inSampleSize*=2;o.inJustDecodeBounds=false;
                b=BitmapFactory.decodeByteArray(data,0,data.length,o);
                if(b!=null&&b.getWidth()>want*1.25f){Bitmap s=Bitmap.createScaledBitmap(b,want,Math.round(b.getHeight()*(want/(float)b.getWidth())),true);b.recycle();b=s;}}catch(Exception ignored){}
            final Bitmap done=b;
            main.post(()->{if(done==null)return;thumbs.put(video.id,done);if(video.id.equals(image.getTag()))image.setImageBitmap(done);});
        });
    }

    static HttpURLConnection connect(String url)throws IOException{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36");
        c.setRequestProperty("Accept-Language","id-ID,id;q=0.9");c.setRequestProperty("Cookie","SOCS=CAI; CONSENT=YES+1");
        return c;
    }
    static String get(String url)throws IOException{
        HttpURLConnection c=connect(url);
        try(InputStream in=c.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream(1<<20);byte[] buf=new byte[16384];int n;
            while((n=in.read(buf))>0)out.write(buf,0,n);return out.toString("UTF-8");}
        finally{c.disconnect();}
    }

    private static File cache(Context c,Source s){return new File(c.getCacheDir(),s.key+".json");}
    private static void save(Context c,Source s){
        try{JSONArray a=new JSONArray();for(Streams.Video v:s.videos)a.put(new JSONObject().put("id",v.id).put("title",v.title).put("thumb",v.thumb).put("live",v.live).put("channel",v.channel==null?"":v.channel).put("published",v.published));
            try(FileOutputStream out=new FileOutputStream(cache(c,s))){out.write(new JSONObject().put("time",s.fetched).put("videos",a).toString().getBytes("UTF-8"));}}catch(Exception ignored){}
    }
    /** The last list saved, so Home shows the row at once (and offline); a fresh fetch follows when it is old. */
    private static void load(Context c,Source s){
        File f=cache(c,s);
        try(FileInputStream in=new FileInputStream(f)){byte[] b=new byte[(int)f.length()];int read=0;while(read<b.length){int n=in.read(b,read,b.length-read);if(n<0)break;read+=n;}
            JSONObject o=new JSONObject(new String(b,0,read,"UTF-8"));JSONArray a=o.getJSONArray("videos");ArrayList<Streams.Video> list=new ArrayList<>();
            for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);Streams.Video v=new Streams.Video();v.id=j.getString("id");v.title=j.getString("title");v.thumb=j.getString("thumb");v.live=j.optBoolean("live");
                String ch=j.optString("channel");v.channel=ch.isEmpty()?null:ch;v.published=j.optLong("published");list.add(v);}
            s.videos=list;s.fetched=o.optLong("time");}catch(Exception ignored){}
    }
}
