package dev.luma.tv;

import android.content.Context;
import android.os.StatFs;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/** Apple TV aerials for the screensaver, downloaded at run time from Apple's own server and kept in the app's private
 *  storage: a few 1080p H.264 films, at most CAP bytes in all, and only while the TV has room to spare. Nothing is
 *  bundled in the app or the repository. Only nature films (Landscapes, Underwater); Hawaii comes first. */
final class Aerials {
    static final class Film {
        final String name,file;final long size;
        Film(String name,String file,long size){this.name=name;this.file=file;this.size=size;}
        String url(){return "https://sylvan.apple.com/Videos/"+file;}
    }
    /** In play order. Sizes are Apple's, so a partial file is never mistaken for a whole one. */
    static final Film[] FILMS={ // nature only (Apple's Landscapes and Underwater sets): no Space / Earth-from-orbit films
        new Film("Hawaii","comp_H004_C009_PS_v01_SDR_PS_20180925_SDR_2K_AVC.mov",181314313L),
        new Film("Yosemite","Y004_C015_SDR_2K_AVC.mov",288869148L),
        new Film("Palau Coral","PA_A004_C003_SDR_20190719_SDR_2K_AVC.mov",258857850L),
        new Film("Kelp","KP_A010_C002_SDR_20190717_SDR_2K_AVC.mov",304281782L),
        new Film("Iceland","I003_C008_SDR_2K_AVC.mov",405727585L),
    };
    /** At most this much on disk for aerials, and always this much left free for the TV itself. */
    static final long CAP=1500L*1024*1024,RESERVE=2L*1024*1024*1024;
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Luma aerials");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});
    private static volatile boolean busy;

    static File dir(Context c){File d=new File(c.getFilesDir(),"aerials");d.mkdirs();return d;}
    static File file(Context c,Film f){return new File(dir(c),f.file);}
    static boolean ready(Context c,Film f){return file(c,f).length()==f.size;}

    /** The films already on the TV, in play order. */
    static List<Film> cached(Context c){ArrayList<Film> list=new ArrayList<>();for(Film f:FILMS)if(ready(c,f))list.add(f);return list;}

    /** Fetches the missing films one after another, in the background, while they fit under CAP and the reserve. */
    static void ensure(Context context){
        if(busy)return;busy=true;Context c=context.getApplicationContext();
        IO.execute(()->{
            try{
                // films no longer in the list (the old Space ones) are removed so they never play or take room
                HashSet<String> keep=new HashSet<>();for(Film f:FILMS){keep.add(f.file);keep.add(f.file+".part");}
                File[] old=dir(c).listFiles();if(old!=null)for(File o:old)if(!keep.contains(o.getName()))o.delete();
                long used=0;for(Film f:FILMS)if(ready(c,f))used+=f.size;
                for(Film f:FILMS){
                    if(ready(c,f))continue;
                    long free=new StatFs(dir(c).getPath()).getAvailableBytes();
                    if(used+f.size>CAP||free-f.size<RESERVE)break;
                    if(download(c,f))used+=f.size;else break; // offline or refused: try again next time
                }
            }finally{busy=false;}
        });
    }

    /** Resumes a partial download (Range), and keeps the file only when it has Apple's exact size. */
    private static boolean download(Context c,Film f){
        File part=new File(dir(c),f.file+".part"),done=file(c,f);
        try{
            long have=part.length();
            HttpURLConnection con=(HttpURLConnection)new URL(f.url()).openConnection();
            con.setConnectTimeout(15000);con.setReadTimeout(30000);
            if(have>0)con.setRequestProperty("Range","bytes="+have+"-");
            int code=con.getResponseCode();
            if(code!=200&&code!=206){con.disconnect();return false;}
            boolean append=code==206;
            try(InputStream in=con.getInputStream();OutputStream out=new FileOutputStream(part,append)){
                byte[] buf=new byte[64*1024];int n;while((n=in.read(buf))>0)out.write(buf,0,n);
            }finally{con.disconnect();}
            if(part.length()!=f.size){if(part.length()>f.size)part.delete();return false;}
            return part.renameTo(done);
        }catch(IOException e){return false;}
    }
}
