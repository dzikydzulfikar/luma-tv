package dev.luma.tv;

import java.util.*;
import java.util.regex.*;

/** Reads a YouTube channel's live and recent streams from its public /streams page, or from its RSS feed as a
 *  fallback. Plain Java (no Android classes), so tools/StreamsTest.java checks it without a TV. */
final class Streams {
    static final class Video { String id, title, thumb, channel; long published; boolean live; }

    private static final Pattern ID=Pattern.compile("\"contentId\":\"([\\w-]{11})\"");
    private static final Pattern TITLE=Pattern.compile("\"lockupMetadataViewModel\":\\{\"title\":\\{\"content\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern BADGE=Pattern.compile("\"thumbnailBadgeViewModel\":\\{\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern SOURCES=Pattern.compile("\"thumbnailViewModel\":\\{\"image\":\\{\"sources\":\\[(.*?)\\]");
    private static final Pattern URL=Pattern.compile("\"url\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern ENTRY=Pattern.compile("<entry>(.*?)</entry>",Pattern.DOTALL);
    private static final Pattern FEED_ID=Pattern.compile("<yt:videoId>([\\w-]{11})</yt:videoId>"),FEED_TITLE=Pattern.compile("<title>(.*?)</title>",Pattern.DOTALL),
        FEED_AUTHOR=Pattern.compile("<author>\\s*<name>(.*?)</name>",Pattern.DOTALL),FEED_DATE=Pattern.compile("<published>(.*?)</published>");

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

    /** The channel's RSS feed: its latest uploads, streams and videos alike, with the channel's name and the time
     *  each was published. Shorts (linked as /shorts/) are left out: they are made for phones. */
    static List<Video> parseFeed(String xml,int max){
        ArrayList<Video> list=new ArrayList<>();Matcher m=ENTRY.matcher(xml);
        while(m.find()&&list.size()<max){
            String e=m.group(1);if(e.contains("/shorts/"))continue;
            Matcher id=FEED_ID.matcher(e),title=FEED_TITLE.matcher(e),author=FEED_AUTHOR.matcher(e),date=FEED_DATE.matcher(e);
            if(!id.find()||!title.find())continue;
            Video v=new Video();v.id=id.group(1);v.title=xml(title.group(1));v.thumb="https://i.ytimg.com/vi/"+v.id+"/hqdefault.jpg";
            if(author.find())v.channel=xml(author.group(1));
            if(date.find())v.published=time(date.group(1));
            list.add(v);
        }
        return list;
    }
    /** An RSS timestamp such as 2026-10-20T16:00:06+00:00, in epoch milliseconds (0 if unreadable). */
    static long time(String iso){
        try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",java.util.Locale.ROOT);return f.parse(iso.trim()).getTime();}catch(Exception e){return 0;}
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
