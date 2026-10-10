package com.ridi.calendar;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import java.util.Calendar;
import java.util.List;

/** Civil dates use the device's current time zone; lunar and solar terms use lunar-java. */
public final class CalendarEngine {
  private CalendarEngine() {}
  public static final class Info { public final String lunarDay,annotation; public Info(String day,String note){lunarDay=day;annotation=note;} }
  public static Calendar date(int year, int month, int day) {
    Calendar c = Calendar.getInstance();
    c.clear(); c.setLenient(false); c.set(year, month - 1, day);
    c.getTime();
    return c;
  }
  public static Calendar today() { return Calendar.getInstance(); }
  public static String key(Calendar c) { return String.format(java.util.Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH)); }
  public static String dayLabel(Calendar c) {
    return describe(c).lunarDay;
  }
  public static String lunarLabel(Calendar c) {
    Lunar l = lunar(c);
    return "农历 " + l.getMonthInChinese() + "月" + l.getDayInChinese();
  }
  public static Lunar lunar(Calendar c) { return Solar.fromYmd(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH)).getLunar(); }
  public static String annotation(Calendar c) {
    return describe(c).annotation;
  }
  public static Info describe(Calendar c) {
    Solar s = Solar.fromYmd(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
    Lunar l = s.getLunar();
    String day=l.getDay() == 1 ? l.getMonthInChinese() + "月初一" : l.getDayInChinese();
    List<String> lunarFestivals = l.getFestivals();
    if (!lunarFestivals.isEmpty()) return new Info(day,lunarFestivals.get(0));
    List<String> solarFestivals = s.getFestivals();
    if (!solarFestivals.isEmpty()) return new Info(day,solarFestivals.get(0));
    String term = l.getJieQi();
    return new Info(day,term == null ? "" : term);
  }
  public static boolean sameDay(Calendar a, Calendar b) { return key(a).equals(key(b)); }
  public static int mondayOffset(Calendar c) { return (c.get(Calendar.DAY_OF_WEEK)+5)%7; }
  public static String week(Calendar c) { return "星期" + "日一二三四五六".charAt(c.get(Calendar.DAY_OF_WEEK)-1); }
}
