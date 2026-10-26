package dev.luma.tv;

import java.util.*;
import java.util.regex.*;

/** Reads a YouTube channel's live and recent streams from its public /streams page, or from its RSS feed as a
 *  fallback. Plain Java (no Android classes), so tools/StreamsTest.java checks it without a TV. */
final class Streams {
    static final class Video { String id, title, thumb; boolean live; }

    private static final Pattern ID=Pattern.compile("\"contentId\":\"([\\w-]{11})\"");
    private static final Pattern TITLE=Pattern.compile("\"lockupMetadataViewModel\":\\{\"title\":\\{\"content\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern BADGE=Pattern.compile("\"thumbnailBadgeViewModel\":\\{\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern SOURCES=Pattern.compile("\"thumbnailViewModel\":\\{\"image\":\\{\"sources\":\\[(.*?)\\]");
    private static final Pattern URL=Pattern.compile("\"url\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern ENTRY=Pattern.compile("<entry>.*?<yt:videoId>([\\w-]{11})</yt:videoId>.*?<title>(.*?)</title>",Pattern.DOTALL);

    /** The /streams page: the stream that is live now first, then past streams, newest first. Upcoming
     *  (scheduled) streams are left out: they cannot be watched yet. */
    static List<Video> parsePage(String html,int max){
        ArrayList<Video> live=new ArrayList<>(),past=new ArrayList<>();HashSet<String> seen=new HashSet<>();
        String[] parts=html.split("\"lockupViewModel\":\\{");
        for(int i=1;i<parts.length&&live.size()+past.size()<max;i++){
            String p=parts[i];
            Matcher id=ID.matcher(p),title=TITLE.matcher(p);
            if(!id.find()||!title.find()||!seen.add(id.group(1)))continue;
            Matcher badge=BADGE.matcher(p);String text=badge.find()?unescape(badge.group(1)):"";
            boolean isLive=p.contains("BADGE_STYLE_LIVE")||text.equalsIgnoreCase("LIVE");
            // A past stream shows its length (1:21:40, or 1.21.40 in Indonesian); anything else is upcoming.
            if(!isLive&&!text.matches("[0-9][0-9.:]*"))continue;
            Video v=new Video();v.id=id.group(1);v.title=unescape(title.group(1));v.live=isLive;
            v.thumb="https://i.ytimg.com/vi/"+v.id+"/hqdefault.jpg";
            Matcher sources=SOURCES.matcher(p);
            if(sources.find()){Matcher url=URL.matcher(sources.group(1));while(url.find())v.thumb=unescape(url.group(1));} // the last is the largest
            (isLive?live:past).add(v);
        }
        live.addAll(past);return live;
    }

    /** The channel's RSS feed: its latest uploads, streams and videos alike. Used when the page cannot be read. */
    static List<Video> parseFeed(String xml,int max){
        ArrayList<Video> list=new ArrayList<>();Matcher m=ENTRY.matcher(xml);
        while(m.find()&&list.size()<max){
            Video v=new Video();v.id=m.group(1);v.title=xml(m.group(2));v.thumb="https://i.ytimg.com/vi/"+v.id+"/hqdefault.jpg";list.add(v);
        }
        return list;
    }

    static String unescape(String s){
        StringBuilder b=new StringBuilder(s.length());
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            if(c!='\\'||i+1>=s.length()){b.append(c);continue;}
            char n=s.charAt(++i);
            if(n=='u'&&i+4<s.length()){b.append((char)Integer.parseInt(s.substring(i+1,i+5),16));i+=4;}
            else b.append(n=='n'?'\n':n=='t'?'\t':n);
        }
        return b.toString();
    }
    private static String xml(String s){return s.replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&");}
}
