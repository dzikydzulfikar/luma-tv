package dev.luma.tv;

import java.util.*;

/** Prayer times for one place, worked out on the TV with no network, by the criteria of Muhammadiyah's Majelis
 *  Tarjih: Subuh at the sun 18° below the horizon (Munas Tarjih XXXI, 2020; it was 20°), Isya at 18°, Ashar when a
 *  shadow is its object's length plus the noon shadow, Maghrib at sunset, and 2 minutes of ihtiyath added to each,
 *  with seconds dropped. Checked against the Majelis Tarjih's Imsakiyah 1447 H for Jakarta: 146 of its 150 times
 *  match to the minute, the rest are a minute apart (tools/PrayerTest.java). */
final class Prayer {
    static final String[] NAMES={"Subuh","Dzuhur","Ashar","Maghrib","Isya"};
    /** The kos in Setiabudi, Jakarta Selatan. */
    static final double LAT=-6.214,LON=106.823;
    static final TimeZone ZONE=TimeZone.getTimeZone("Asia/Jakarta");
    static final double IHTIYATH=2;

    /** Minutes after local midnight of the five prayers on a day (Calendar fields of that day), then sunrise and
     *  sunset (sunrise, like the Majelis Tarjih's "terbit", 2 minutes early; sunset as it is). */
    static int[] times(int year,int month,int day,double lat,double lon){
        Calendar c=Calendar.getInstance(ZONE);c.clear();c.set(year,month-1,day);
        double tz=ZONE.getOffset(c.getTimeInMillis())/3600000.0;
        double jd0=c.getTimeInMillis()/86400000.0+2440587.5,p=Math.toRadians(lat);
        double noonDec=sun(jd0+(12+tz-lon/15)/24)[0];
        double asr=Math.toDegrees(Math.atan(1/(1+Math.tan(Math.abs(p-noonDec)))));
        double[] h={event(jd0,5,-18,-1,p,lat,lon,tz),event(jd0,12,Double.NaN,0,p,lat,lon,tz),event(jd0,15,asr,1,p,lat,lon,tz),
            event(jd0,18,-.8333,1,p,lat,lon,tz),event(jd0,19,-18,1,p,lat,lon,tz)};
        int[] out=new int[7];for(int i=0;i<5;i++)out[i]=(int)Math.floor(h[i]*60+IHTIYATH);
        out[5]=(int)Math.floor(event(jd0,6,-.8333,-1,p,lat,lon,tz)*60-IHTIYATH+1);out[6]=(int)Math.floor(h[3]*60);
        return out;
    }
    /** Local hour when the sun reaches an altitude (NaN: noon), before (-1) or after (+1) noon, refined with the
     *  sun's place at that hour. */
    private static double event(double jd0,double guess,double alt,int side,double p,double lat,double lon,double tz){
        double t=guess;
        for(int i=0;i<4;i++){
            double[] s=sun(jd0+t/24);double noon=12+tz-lon/15-s[1];
            if(Double.isNaN(alt)){t=noon;continue;}
            double cos=(Math.sin(Math.toRadians(alt))-Math.sin(p)*Math.sin(s[0]))/(Math.cos(p)*Math.cos(s[0]));
            t=noon+side*Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,cos))))/15;
        }
        return t;
    }
    /** The sun's declination (radians) and the equation of time (hours) on a Julian day (USNO's approximation). */
    private static double[] sun(double jd){
        double d=jd-2451545,g=Math.toRadians((357.529+.98560028*d)%360),q=(280.459+.98564736*d)%360;
        double l=Math.toRadians((q+1.915*Math.sin(g)+.020*Math.sin(2*g))%360),e=Math.toRadians(23.439-.00000036*d);
        double ra=Math.toDegrees(Math.atan2(Math.cos(e)*Math.sin(l),Math.cos(l)))/15;
        double eqt=q/15-((ra%24+24)%24);eqt=((eqt+12)%24+24)%24-12;
        return new double[]{Math.asin(Math.sin(e)*Math.sin(l)),eqt};
    }
    /** The times to show at a moment (as times() gives them) and, last, the index of the next prayer: today's,
     *  or after Isya tomorrow's with Subuh next. */
    static int[] schedule(long now){
        Calendar c=Calendar.getInstance(ZONE);c.setTimeInMillis(now);
        int minute=c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE),next=-1;
        int[] t=times(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),LAT,LON);
        for(int i=4;i>=0;i--)if(t[i]>minute)next=i;
        if(next<0){c.add(Calendar.DAY_OF_MONTH,1);t=times(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),LAT,LON);next=0;}
        int[] out=Arrays.copyOf(t,8);out[7]=next;return out;
    }
    /** Minutes from a moment to the next prayer in schedule(). */
    static int minutesTo(long now,int[] schedule){
        Calendar c=Calendar.getInstance(ZONE);c.setTimeInMillis(now);
        int d=schedule[schedule[7]]-(c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE));return d<=0?d+1440:d;
    }
    static String hm(int m){return String.format(Locale.ROOT,"%02d:%02d",m/60,m%60);}
}
