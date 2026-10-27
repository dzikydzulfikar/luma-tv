package dev.luma.tv;

import android.content.Context;
import java.io.*;
import java.util.*;
import org.json.*;

/** Recommended for You: AI and tech videos from a curated list (assets/recommended.json). Its pinned videos come
 *  first, then the newest videos of its channels from their public RSS feeds (no account, no API key), without
 *  duplicates, newest first (the channel's /videos page when RSS fails). Shelf caches the list on the TV and refetches it every few hours. */
final class Recommended {
    static final long FRESH=3*60*60*1000L;

    static List<Streams.Video> fetch(Context context)throws Exception{
        JSONObject config=new JSONObject(read(context.getAssets().open("recommended.json")));
        int max=config.optInt("max",15),per=config.optInt("perChannel",4);
        ArrayList<Streams.Video> list=new ArrayList<>(),latest=new ArrayList<>();HashSet<String> seen=new HashSet<>();
        JSONArray pins=config.optJSONArray("pins");
        for(int i=0;pins!=null&&i<pins.length();i++){
            JSONObject p=pins.getJSONObject(i);Streams.Video v=new Streams.Video();
            v.id=p.getString("id");v.title=p.getString("title");v.channel=p.optString("channel",null);v.thumb="https://i.ytimg.com/vi/"+v.id+"/hqdefault.jpg";
            if(seen.add(v.id))list.add(v);
        }
        JSONArray channels=config.optJSONArray("channels");
        for(int i=0;channels!=null&&i<channels.length();i++){
            String id=channels.getJSONObject(i).getString("id");List<Streams.Video> got=new ArrayList<>();
            try{got=Streams.parseFeed(Shelf.get("https://www.youtube.com/feeds/videos.xml?channel_id="+id),per);}catch(Exception e){android.util.Log.w("LumaRecommended","feed "+i+": "+e);}
            if(got.isEmpty())try{got=Streams.parseUploads(Shelf.get("https://www.youtube.com/channel/"+id+"/videos"),per,System.currentTimeMillis());}
                catch(Exception e){android.util.Log.w("LumaRecommended","page "+i+": "+e);} // one channel offline or gone does not empty the row
            latest.addAll(got);
        }
        Collections.sort(latest,(a,b)->Long.compare(b.published,a.published));
        // Newest uploads lead; the hand-picked pins are mixed in every third card, so the row changes as channels post.
        ArrayList<Streams.Video> pinned=list,mixed=new ArrayList<>();int fresh=0,p=0;HashSet<String> used=new HashSet<>();
        for(Streams.Video v:latest){
            if(mixed.size()>=max)break;if(!used.add(v.id))continue;
            mixed.add(v);fresh++;
            if(fresh%2==0&&p<pinned.size()&&mixed.size()<max&&used.add(pinned.get(p).id))mixed.add(pinned.get(p++));
        }
        while(p<pinned.size()&&mixed.size()<max)if(used.add(pinned.get(p).id))mixed.add(pinned.get(p++));else p++;
        android.util.Log.i("LumaRecommended","fetched "+fresh+" new from feeds, "+mixed.size()+" total, newest "+(latest.isEmpty()?0:latest.get(0).published));
        return fresh>0?mixed:new ArrayList<>(); // pins alone mean the feeds failed: keep the old list
    }
    private static String read(InputStream in)throws IOException{
        try(InputStream s=in){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=s.read(b))>0)out.write(b,0,n);return out.toString("UTF-8");}
    }
}
