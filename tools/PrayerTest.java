package dev.luma.tv;

/** Checks Prayer against the Majelis Tarjih's Imsakiyah 1447 H for Jakarta (Oman Fathurohman SW, Majelis Tarjih dan
 *  Tajdid PP Muhammadiyah; web.suaramuhammadiyah.id/wp-content/uploads/2026/01/Imsakiyah-1447-H_jakarta.pdf, at its
 *  place: 6°12'19" S, 106°49'27" E). Run from the repository root:
 *    javac -d build/test src/dev/luma/tv/Prayer.java tools/PrayerTest.java && java -cp build/test dev.luma.tv.PrayerTest */
public final class PrayerTest {
    // month, day, Subuh, Zuhur, Ashar, Maghrib, Isya (2026)
    static final Object[][] TABLE={
        {2,18,"04:49","12:08","15:20","18:17","19:27"},
        {2,19,"04:49","12:08","15:19","18:16","19:27"},
        {2,20,"04:49","12:08","15:18","18:16","19:27"},
        {2,21,"04:49","12:08","15:18","18:16","19:26"},
        {2,22,"04:49","12:08","15:17","18:15","19:26"},
        {2,23,"04:49","12:08","15:16","18:15","19:26"},
        {2,24,"04:50","12:07","15:15","18:15","19:25"},
        {2,25,"04:50","12:07","15:14","18:15","19:25"},
        {2,26,"04:50","12:07","15:13","18:14","19:24"},
        {2,27,"04:50","12:07","15:13","18:14","19:24"},
        {2,28,"04:50","12:07","15:12","18:14","19:23"},
        {3,1,"04:50","12:07","15:11","18:13","19:23"},
        {3,2,"04:50","12:06","15:10","18:13","19:23"},
        {3,3,"04:50","12:06","15:09","18:12","19:22"},
        {3,4,"04:50","12:06","15:08","18:12","19:22"},
        {3,5,"04:50","12:06","15:07","18:12","19:21"},
        {3,6,"04:50","12:05","15:08","18:11","19:21"},
        {3,7,"04:50","12:05","15:08","18:11","19:20"},
        {3,8,"04:50","12:05","15:08","18:10","19:20"},
        {3,9,"04:50","12:05","15:09","18:10","19:19"},
        {3,10,"04:50","12:05","15:09","18:10","19:19"},
        {3,11,"04:50","12:04","15:10","18:09","19:18"},
        {3,12,"04:50","12:04","15:10","18:09","19:18"},
        {3,13,"04:50","12:04","15:10","18:08","19:17"},
        {3,14,"04:50","12:03","15:11","18:08","19:17"},
        {3,15,"04:50","12:03","15:11","18:07","19:16"},
        {3,16,"04:50","12:03","15:11","18:07","19:16"},
        {3,17,"04:50","12:03","15:11","18:06","19:16"},
        {3,18,"04:49","12:02","15:12","18:06","19:15"},
        {3,19,"04:49","12:02","15:12","18:05","19:15"},
    };
    public static void main(String[] args){
        double lat=-(6+12/60.0+19/3600.0),lon=106+49/60.0+27/3600.0;int exact=0,n=0;
        for(Object[] row:TABLE){
            int[] t=Prayer.times(2026,(Integer)row[0],(Integer)row[1],lat,lon);
            for(int i=0;i<5;i++){
                String[] hm=((String)row[2+i]).split(":");int want=Integer.parseInt(hm[0])*60+Integer.parseInt(hm[1]);n++;
                if(Math.abs(t[i]-want)>1)throw new AssertionError(row[0]+"/"+row[1]+" "+Prayer.NAMES[i]+": "+Prayer.hm(t[i])+" not "+row[2+i]);
                if(t[i]==want)exact++;
            }
        }
        if(exact<n-6)throw new AssertionError(exact+" of "+n+" exact");
        StringBuilder today=new StringBuilder();int[] t=Prayer.times(2026,10,26,Prayer.LAT,Prayer.LON);
        for(int i=0;i<5;i++)today.append(Prayer.NAMES[i]).append(' ').append(Prayer.hm(t[i])).append(i<4?", ":"");
        if(!today.toString().equals("Subuh 04:18, Dzuhur 11:38, Ashar 14:51, Maghrib 17:47, Isya 18:58"))throw new AssertionError(today);
        // 20:00 WIB on 26 Oct 2026 is after Isya: next is Subuh on the 27th.
        if(!"0 04:18".equals(Prayer.next(1793019600000L)[0]+" "+Prayer.next(1793019600000L)[1]))throw new AssertionError("next "+Prayer.next(1793019600000L)[1]);
        System.out.println("ok: "+exact+" of "+n+" times exact; 26 Oct 2026 Setiabudi: "+today);
    }
}
