import com.ridi.calendar.Weather;
import com.ridi.calendar.ChineseCities;

public final class WeatherCodeTest {
  private static void eq(String expected,String actual){if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
  public static void main(String[] args){eq("晴",Weather.description(0));eq("晴间多云",Weather.description(1));eq("多云",Weather.description(2));eq("阴",Weather.description(3));eq("雾",Weather.description(45));eq("小雨",Weather.description(61));eq("中雨",Weather.description(63));eq("大雨",Weather.description(65));eq("雪",Weather.description(75));eq("雷雨",Weather.description(95));if(Weather.iconGlyph(2)==Weather.iconGlyph(3)||Weather.iconGlyph(0)==Weather.iconGlyph(2)||Weather.iconGlyph(61)==Weather.iconGlyph(75))throw new AssertionError("distinct weather icons");if(ChineseCities.find("北京")==null||ChineseCities.find("Beijing")==null||ChineseCities.find("不存在")!=null)throw new AssertionError("city lookup");System.out.println("Weather code tests passed");}
}
