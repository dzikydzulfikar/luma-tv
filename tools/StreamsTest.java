package dev.luma.tv;

import java.util.List;

/** Checks Streams against the shapes YouTube's /streams page and RSS feed use. Run from the repository root:
 *    javac -d build/test src/dev/luma/tv/Streams.java tools/StreamsTest.java && java -cp build/test dev.luma.tv.StreamsTest */
public final class StreamsTest {
    static String lockup(String id,String title,String badge,String style){
        return "\"lockupViewModel\":{\"contentImage\":{\"thumbnailViewModel\":{\"image\":{\"sources\":[{\"url\":\"https://i.ytimg.com/vi/"+id+"/hq720.jpg?a=1\\u0026b=2\",\"width\":360},{\"url\":\"https://i.ytimg.com/vi/"+id+"/hq720.jpg?big\",\"width\":720}]},"
            +"\"overlays\":[{\"thumbnailBottomOverlayViewModel\":{\"badges\":[{\"thumbnailBadgeViewModel\":{\"text\":\""+badge+"\",\"badgeStyle\":\"THUMBNAIL_OVERLAY_BADGE_STYLE_"+style+"\"}}]}}]}},"
            +"\"metadata\":{\"lockupMetadataViewModel\":{\"title\":{\"content\":\""+title+"\"}}},\"contentId\":\""+id+"\",\"contentType\":\"LOCKUP_CONTENT_TYPE_VIDEO\"}";
    }
    public static void main(String[] args){
        String page="x"+lockup("UPCOMING001","Soon","Mendatang","DEFAULT")+lockup("PASTVIDEO01","Past \\\"one\\\" \\u00e9","1.21.40","DEFAULT")
            +lockup("LIVEVIDEO01","Live now","LIVE","LIVE")+lockup("PASTVIDEO02","Past two","39:21","DEFAULT");
        List<Streams.Video> v=Streams.parsePage(page,10);
        check(v.size()==3,"upcoming left out: "+v.size());
        check(v.get(0).id.equals("LIVEVIDEO01")&&v.get(0).live,"live first");
        check(v.get(1).title.equals("Past \"one\" é"),"title unescaped: "+v.get(1).title);
        check(v.get(1).thumb.endsWith("hq720.jpg?big"),"largest thumbnail: "+v.get(1).thumb);
        check(Streams.parsePage(page,1).size()==1,"max");
        String feed="<feed><title>Windah</title><entry><yt:videoId>FEEDVIDEO01</yt:videoId><title>A &amp; B</title></entry></feed>";
        List<Streams.Video> f=Streams.parseFeed(feed,10);
        check(f.size()==1&&f.get(0).title.equals("A & B")&&f.get(0).thumb.contains("FEEDVIDEO01"),"feed");
        String rich="<feed><entry><yt:videoId>FEEDVIDEO02</yt:videoId><title>Talk</title><link rel=\"alternate\" href=\"https://www.youtube.com/watch?v=FEEDVIDEO02\"/><author><name>Cursor</name></author><published>2026-09-03T17:00:00+00:00</published></entry>"
            +"<entry><yt:videoId>SHORTVIDEO1</yt:videoId><title>Short</title><link rel=\"alternate\" href=\"https://www.youtube.com/shorts/SHORTVIDEO1\"/></entry></feed>";
        List<Streams.Video> r=Streams.parseFeed(rich,10);
        check(r.size()==1&&"Cursor".equals(r.get(0).channel),"channel name, shorts left out: "+r.size());
        check(r.get(0).published==1788454800000L,"published time: "+r.get(0).published);
        System.out.println(failures==0?"all ok":failures+" failed");
        if(failures>0)System.exit(1);
    }
    static int failures;
    static void check(boolean ok,String what){if(!ok)failures++;System.out.println((ok?"ok    ":"FAIL  ")+what);}
}
