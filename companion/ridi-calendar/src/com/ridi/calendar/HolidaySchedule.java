package com.ridi.calendar;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/** Only explicitly published schedule dates get an OFF/WORK badge. */
public final class HolidaySchedule {
  private final Map<String,String> values = new HashMap<String,String>();
  public HolidaySchedule(Context context) {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("holidays.csv"), "UTF-8"));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.startsWith("#") || line.trim().length()==0) continue;
        String[] p=line.split(",");
        String[] a=p[0].split("-"); String[] b=p[1].split("-");
        Calendar from=CalendarEngine.date(Integer.parseInt(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2]));
        Calendar to=CalendarEngine.date(Integer.parseInt(b[0]),Integer.parseInt(b[1]),Integer.parseInt(b[2]));
        while (!from.after(to)) { values.put(CalendarEngine.key(from),p[2]); from.add(Calendar.DAY_OF_MONTH,1); }
      }
      br.close();
    } catch(Exception ignored) { /* No badges if schedule cannot load. */ }
  }
  public String badge(Calendar day) { String v=values.get(CalendarEngine.key(day)); return "OFF".equals(v)?"休":("WORK".equals(v)?"工":""); }
}
