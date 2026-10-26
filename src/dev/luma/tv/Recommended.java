package dev.luma.tv;

import android.content.Context;
import java.io.*;
import java.util.*;
import org.json.*;

/** Recommended for You: AI and tech videos from a curated list (assets/recommended.json). Its pinned videos come
 *  first, then the newest videos of its channels from their public RSS feeds (no account, no API key), without
 *  duplicates, newest first. Shelf caches the list on the TV and refetches it every few hours. */
final class Recommended {
    static final long FRESH=6*60*60*1000L;

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
            try{latest.addAll(Streams.parseFeed(Shelf.get("https://www.youtube.com/feeds/videos.xml?channel_id="+channels.getJSONObject(i).getString("id")),per));}
            catch(Exception e){android.util.Log.w("LumaRecommended","feed "+i+": "+e);} // one channel offline or gone does not empty the row
        }
        Collections.sort(latest,(a,b)->Long.compare(b.published,a.published));
        for(Streams.Video v:latest)if(list.size()<max&&seen.add(v.id))list.add(v);
        return list.size()>(pins==null?0:pins.length())?list:new ArrayList<>(); // pins alone mean the feeds failed: keep the old list
    }
    private static String read(InputStream in)throws IOException{
        try(InputStream s=in){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=s.read(b))>0)out.write(b,0,n);return out.toString("UTF-8");}
    }
}
