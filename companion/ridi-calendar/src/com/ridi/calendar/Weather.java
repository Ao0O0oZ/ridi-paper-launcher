package com.ridi.calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class Weather {
  public static final class Forecast { public String date, label; public int code; public double high,low; }
  public static final class Data {
    public String city, updated,source; public double current,wind; public int code; public Forecast[] days=new Forecast[6]; public long fetchedAt;
  }
  private final SharedPreferences prefs; private final Context context;
  public Weather(Context context) { this.context=context; prefs=context.getSharedPreferences("weather",0); }
  public String city() { return prefs.getString("city","北京"); }
  public void setCity(String city) { prefs.edit().putString("city",city).remove("cache").apply(); }
  public boolean fahrenheit() { return prefs.getBoolean("fahrenheit",false); }
  public void setFahrenheit(boolean v) { prefs.edit().putBoolean("fahrenheit",v).apply(); }
  public boolean autoLocation(){return prefs.getBoolean("auto_location",false);}
  public void setAutoLocation(boolean v){prefs.edit().putBoolean("auto_location",v).remove("cache").apply();}
  public Data cached() { try { String raw=prefs.getString("cache",null); return raw==null?null:parse(new JSONObject(raw)); } catch(Exception e){return null;} }
  public boolean stale() { Data d=cached(); return d==null || System.currentTimeMillis()-d.fetchedAt>3*60*60*1000L; }
  public Data refresh() throws Exception {
    String city=city();
    double latitude=Double.NaN,longitude=Double.NaN;String shownCity=city;
    boolean usedLocation=false;
    if(autoLocation()){
      LocationManager manager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
      Location location=manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      if(location!=null && System.currentTimeMillis()-location.getTime()<=24*60*60*1000L){latitude=location.getLatitude();longitude=location.getLongitude();shownCity="定位 ("+Math.round(latitude*100)/100.0+", "+Math.round(longitude*100)/100.0+")";usedLocation=true;}
    }
    if(!usedLocation){
      try{
        JSONObject geo=get("https://geocoding-api.open-meteo.com/v1/search?name="+URLEncoder.encode(city,"UTF-8")+"&count=1&language=zh&countryCode=CN&format=json");
        JSONArray matches=geo.optJSONArray("results");
        if(matches==null || matches.length()==0) throw new Exception("城市搜索无结果");
        JSONObject found=matches.getJSONObject(0);latitude=found.getDouble("latitude");longitude=found.getDouble("longitude");
      }catch(Exception e){double[] known=ChineseCities.find(city);if(known==null)throw new Exception("无法搜索该城市；旧设备离线坐标表支持主要城市",e);latitude=known[0];longitude=known[1];}
    }
    String url="https://api.open-meteo.com/v1/forecast?latitude="+latitude+"&longitude="+longitude+"&current=temperature_2m,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=6";
    JSONObject raw;try{raw=get(url);}catch(Exception openMeteoError){android.util.Log.w("RidiWeather","Open-Meteo unavailable; using MET Norway");raw=fromMetNorway(latitude,longitude);}
    if(!raw.has("ridi_source"))raw.put("ridi_source","Open-Meteo");raw.put("ridi_city",shownCity); raw.put("ridi_fetched",System.currentTimeMillis());
    Data d=parse(raw); prefs.edit().putString("cache",raw.toString()).apply(); return d;
  }
  private JSONObject get(String url) throws Exception {
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(8000); c.setReadTimeout(8000); c.setRequestProperty("User-Agent","RidiCalendar/1.0 (https://github.com/aozheng/ridi-paper-launcher)");
    if(c instanceof HttpsURLConnection){
      SSLContext tls=SSLContext.getInstance("TLSv1.2");
      if(url.contains("open-meteo.com")){
        KeyStore roots=KeyStore.getInstance(KeyStore.getDefaultType());roots.load(null,null);
        java.io.InputStream cert=context.getAssets().open("isrgrootx1.crt");
        try{roots.setCertificateEntry("isrg-root-x1",CertificateFactory.getInstance("X.509").generateCertificate(cert));}finally{cert.close();}
        TrustManagerFactory trust=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());trust.init(roots);tls.init(null,trust.getTrustManagers(),null);
      }else tls.init(null,null,null);
      ((HttpsURLConnection)c).setSSLSocketFactory(new Tls12Factory(tls.getSocketFactory()));
    }
    try { if(c.getResponseCode()!=200) throw new Exception("天气服务暂不可用（"+c.getResponseCode()+"）"); BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8")); StringBuilder b=new StringBuilder(); String s; while((s=r.readLine())!=null)b.append(s); r.close(); return new JSONObject(b.toString()); } finally {c.disconnect();}
  }
  private JSONObject fromMetNorway(double lat,double lon)throws Exception{
    JSONObject source=get("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat="+lat+"&lon="+lon);
    JSONArray series=source.getJSONObject("properties").getJSONArray("timeseries");if(series.length()==0)throw new Exception("备用天气接口没有预报数据");
    Calendar today=CalendarEngine.today();SimpleDateFormat utc=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);utc.setTimeZone(TimeZone.getTimeZone("UTC"));
    JSONArray dates=new JSONArray(),codes=new JSONArray(),high=new JSONArray(),low=new JSONArray();
    Forecast[] days=new Forecast[6];boolean[] symbols=new boolean[6];
    for(int d=0;d<6;d++){Calendar date=(Calendar)today.clone();date.add(Calendar.DAY_OF_MONTH,d);Forecast f=new Forecast();f.date=CalendarEngine.key(date);f.high=-999;f.low=999;days[d]=f;}
    JSONObject first=series.getJSONObject(0).getJSONObject("data").getJSONObject("instant").getJSONObject("details");
    double current=first.getDouble("air_temperature"),wind=first.getDouble("wind_speed")*3.6;int currentCode=3;
    for(int i=0;i<series.length();i++){
      JSONObject entry=series.getJSONObject(i),data=entry.getJSONObject("data");Calendar date=Calendar.getInstance();date.setTime(utc.parse(entry.getString("time")));String key=CalendarEngine.key(date);
      int index=-1;for(int d=0;d<6;d++)if(days[d].date.equals(key)){index=d;break;}if(index<0)continue;
      double temp=data.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature");Forecast f=days[index];f.high=Math.max(f.high,temp);f.low=Math.min(f.low,temp);
      JSONObject period=data.optJSONObject("next_1_hours");if(period==null)period=data.optJSONObject("next_6_hours");if(period==null)period=data.optJSONObject("next_12_hours");
      if(period!=null){String symbol=period.getJSONObject("summary").getString("symbol_code");int code=metCode(symbol);if(!symbols[index]||(date.get(Calendar.HOUR_OF_DAY)>=10&&date.get(Calendar.HOUR_OF_DAY)<=14)){f.code=code;symbols[index]=true;}if(i==0)currentCode=code;}
    }
    for(int d=0;d<6;d++){Forecast f=days[d];if(f.high < -100 || !symbols[d])throw new Exception("备用天气接口缺少未来五天数据");dates.put(f.date);codes.put(f.code);high.put(f.high);low.put(f.low);}
    JSONObject daily=new JSONObject();daily.put("time",dates);daily.put("weather_code",codes);daily.put("temperature_2m_max",high);daily.put("temperature_2m_min",low);
    JSONObject now=new JSONObject();now.put("temperature_2m",current);now.put("weather_code",currentCode);now.put("wind_speed_10m",wind);
    JSONObject normalized=new JSONObject();normalized.put("current",now);normalized.put("daily",daily);normalized.put("ridi_source","MET Norway");return normalized;
  }
  private static int metCode(String symbol){if(symbol.contains("thunder"))return 95;if(symbol.contains("snow")||symbol.contains("sleet"))return 75;if(symbol.contains("heavyrain"))return 65;if(symbol.contains("rain"))return 61;if(symbol.contains("fog"))return 45;if(symbol.contains("cloudy"))return symbol.contains("partly")?2:3;if(symbol.contains("fair"))return 1;if(symbol.contains("clearsky"))return 0;return 3;}
  /** KitKat supports TLS 1.2 but does not enable it on sockets by default. */
  private static final class Tls12Factory extends SSLSocketFactory {
    private final SSLSocketFactory delegate;
    Tls12Factory(SSLSocketFactory delegate){this.delegate=delegate;}
    private Socket enable(Socket socket,String host){if(socket instanceof SSLSocket){SSLSocket ssl=(SSLSocket)socket;ssl.setEnabledProtocols(new String[]{"TLSv1.2"});try{ssl.getClass().getMethod("setHostname",String.class).invoke(ssl,host);}catch(Exception ignored){}}return socket;}
    public String[] getDefaultCipherSuites(){return delegate.getDefaultCipherSuites();}
    public String[] getSupportedCipherSuites(){return delegate.getSupportedCipherSuites();}
    public Socket createSocket(Socket s,String host,int port,boolean close)throws IOException{return enable(delegate.createSocket(s,host,port,close),host);}
    public Socket createSocket(String host,int port)throws IOException{return enable(delegate.createSocket(host,port),host);}
    public Socket createSocket(String host,int port,InetAddress local,int localPort)throws IOException{return enable(delegate.createSocket(host,port,local,localPort),host);}
    public Socket createSocket(InetAddress host,int port)throws IOException{return enable(delegate.createSocket(host,port),host.getHostName());}
    public Socket createSocket(InetAddress host,int port,InetAddress local,int localPort)throws IOException{return enable(delegate.createSocket(host,port,local,localPort),host.getHostName());}
  }
  private static Data parse(JSONObject o) throws Exception {
    Data d=new Data(); d.city=o.getString("ridi_city"); if(d.city.endsWith("（手动）"))d.city=d.city.substring(0,d.city.length()-4); d.source=o.optString("ridi_source","Open-Meteo"); d.fetchedAt=o.getLong("ridi_fetched"); d.updated=new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(d.fetchedAt));
    JSONObject now=o.getJSONObject("current"); d.current=now.getDouble("temperature_2m"); d.code=now.getInt("weather_code"); d.wind=now.getDouble("wind_speed_10m");
    JSONObject daily=o.getJSONObject("daily"); JSONArray dates=daily.getJSONArray("time"),codes=daily.getJSONArray("weather_code"),high=daily.getJSONArray("temperature_2m_max"),low=daily.getJSONArray("temperature_2m_min");
    for(int i=0;i<6;i++){Forecast f=new Forecast(); f.date=dates.getString(i); f.label=f.date.substring(5).replace('-','/'); f.code=codes.getInt(i); f.high=high.getDouble(i); f.low=low.getDouble(i);d.days[i]=f;}
    return d;
  }
  public static String description(int code) { if(code==0)return "晴"; if(code<=2)return "多云"; if(code==3)return "阴"; if(code==45||code==48)return "雾"; if(code>=95)return "雷雨"; if(code>=71&&code<=86)return "雪"; if(code>=61&&code<=67)return code<=61?"小雨":code<=63?"中雨":"大雨"; if(code>=80&&code<=82)return code==80?"小雨":code==81?"中雨":"大雨"; if(code>=51&&code<=57)return "小雨"; return "未知"; }
  public String temp(double c){double t=fahrenheit()?c*9/5+32:c;return Math.round(t)+(fahrenheit()?"℉":"℃");}
}
