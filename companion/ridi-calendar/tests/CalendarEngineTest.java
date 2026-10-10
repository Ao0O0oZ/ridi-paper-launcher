import com.ridi.calendar.CalendarEngine;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.LunarMonth;
import java.util.Calendar;

public final class CalendarEngineTest {
  private static void eq(Object expected,Object actual){if(!expected.equals(actual))throw new AssertionError("expected="+expected+" actual="+actual);}
  private static void yes(boolean v){if(!v)throw new AssertionError("condition failed");}
  public static void main(String[] args){
    Calendar leap=CalendarEngine.date(2024,2,29);leap.add(Calendar.DAY_OF_MONTH,1);eq("2024-03-01",CalendarEngine.key(leap));
    Calendar month=CalendarEngine.date(2026,4,30);month.add(Calendar.DAY_OF_MONTH,1);eq("2026-05-01",CalendarEngine.key(month));
    Calendar year=CalendarEngine.date(2026,12,31);year.add(Calendar.DAY_OF_MONTH,1);eq("2027-01-01",CalendarEngine.key(year));
    Calendar spring=CalendarEngine.date(2026,2,17);eq("正月初一",CalendarEngine.dayLabel(spring));yes(CalendarEngine.annotation(spring).contains("春节"));
    Calendar mid=CalendarEngine.date(2026,9,25);yes(CalendarEngine.annotation(mid).contains("中秋"));
    Calendar qing=CalendarEngine.date(2026,4,5);yes(CalendarEngine.annotation(qing).contains("清明"));
    Calendar winter=CalendarEngine.date(2026,12,22);yes(CalendarEngine.annotation(winter).contains("冬至"));
    Calendar small=CalendarEngine.date(2025,1,29);Lunar l=CalendarEngine.lunar(small);eq(1,l.getDay());
    Calendar leapMonth=CalendarEngine.date(2025,7,25);yes(CalendarEngine.lunar(leapMonth).getMonth()<0);yes(CalendarEngine.dayLabel(leapMonth).startsWith("闰"));
    boolean foundSmall=false,foundLarge=false;for(int m=1;m<=12;m++){int days=LunarMonth.fromYm(2025,m).getDayCount();if(days==29)foundSmall=true;if(days==30)foundLarge=true;yes(days==29||days==30);}
    yes(foundSmall&&foundLarge);
    eq(0,CalendarEngine.mondayOffset(CalendarEngine.date(2026,10,5)));
    System.out.println("CalendarEngine tests passed");
  }
}
