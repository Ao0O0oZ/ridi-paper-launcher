package com.ridi.calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.content.pm.ActivityInfo;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Calendar;
import java.util.List;

public final class MainActivity extends Activity implements CalendarScreen.Listener {
  private TodoStore todos; private HolidaySchedule holidays; private Weather weather; private Weather.Data weatherData; private CalendarScreen screen;
  private Calendar shown,selected; private Handler handler=new Handler(); private boolean loading; private long lastTap; private String lastToday;
  private int orientationMode;
  @Override public void onCreate(Bundle state){super.onCreate(state);todos=new TodoStore(this);holidays=new HolidaySchedule(this);weather=new Weather(this);weatherData=weather.cached();
    orientationMode=getPreferences(MODE_PRIVATE).getInt("orientation",0);applyOrientation();
    Calendar now=CalendarEngine.today();lastToday=CalendarEngine.key(now);shown=(Calendar)now.clone();selected=(Calendar)now.clone();if(state!=null){shown.setTimeInMillis(state.getLong("shown",now.getTimeInMillis()));selected.setTimeInMillis(state.getLong("selected",now.getTimeInMillis()));lastToday=state.getString("lastToday",lastToday);}
    screen=new CalendarScreen(this,this,holidays,todos,weather);setContentView(screen);render();refresh(false);
  }
  @Override protected void onSaveInstanceState(Bundle b){b.putLong("shown",shown.getTimeInMillis());b.putLong("selected",selected.getTimeInMillis());b.putString("lastToday",lastToday);super.onSaveInstanceState(b);}
  @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);screen.requestLayout();render();}
  @Override protected void onResume(){super.onResume();rollDate();render();if(weather.stale())refresh(false);scheduleMidnight();}
  @Override protected void onPause(){handler.removeCallbacksAndMessages(null);super.onPause();}
  private void rollDate(){Calendar now=CalendarEngine.today();String key=CalendarEngine.key(now);if(!key.equals(lastToday)){if(CalendarEngine.key(selected).equals(lastToday)){selected=(Calendar)now.clone();shown=(Calendar)now.clone();}lastToday=key;}}
  private void scheduleMidnight(){handler.removeCallbacksAndMessages(null);Calendar next=CalendarEngine.today();next.add(Calendar.DAY_OF_MONTH,1);next.set(Calendar.HOUR_OF_DAY,0);next.set(Calendar.MINUTE,0);next.set(Calendar.SECOND,1);handler.postDelayed(new Runnable(){public void run(){rollDate();render();scheduleMidnight();}},Math.max(1000,next.getTimeInMillis()-System.currentTimeMillis()));}
  private void render(){if(screen!=null)screen.update(shown,selected,weatherData);}
  private void applyOrientation(){setRequestedOrientation(orientationMode==1?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:orientationMode==2?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);}
  private String orientationName(int mode){return mode==1?"竖屏":mode==2?"横屏":"自动旋转";}
  private boolean guard(){long now=System.currentTimeMillis();if(now-lastTap<230)return false;lastTap=now;return true;}
  public void previous(){if(!guard())return;shown.add(Calendar.MONTH,-1);render();}
  public void next(){if(!guard())return;shown.add(Calendar.MONTH,1);render();}
  public void today(){if(!guard())return;shown=CalendarEngine.today();selected=(Calendar)shown.clone();render();}
  public void select(Calendar day){if(!guard())return;selected=(Calendar)day.clone();shown=(Calendar)day.clone();render();showDay();}
  public void toggle(TodoStore.Item i){i.done=!i.done;todos.save(i);screen.refreshTodos();}
  public void edit(TodoStore.Item i){editTodo(i);}
  public void add(){TodoStore.Item i=new TodoStore.Item();i.date=CalendarEngine.key(selected);i.time="";i.title="";editTodo(i);}
  private TextView label(String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(Color.BLACK);t.setBackgroundColor(Color.WHITE);t.setPadding(16,12,16,12);return t;}
  private EditText field(String hint,String value){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(21);e.setTextColor(Color.BLACK);e.setHintTextColor(Color.BLACK);e.setBackgroundColor(Color.WHITE);e.setHint(hint);e.setText(value);e.setPadding(16,12,16,12);return e;}
  private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(Color.WHITE);l.setPadding(18,10,18,10);return l;}
  private AlertDialog show(AlertDialog.Builder b){AlertDialog d=b.create();d.setOnShowListener(new DialogInterface.OnShowListener(){public void onShow(DialogInterface v){AlertDialog a=(AlertDialog)v;a.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));a.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);for(int id:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL})if(a.getButton(id)!=null){a.getButton(id).setTextColor(Color.BLACK);a.getButton(id).setBackgroundColor(Color.WHITE);}}});d.show();return d;}
  private ScrollView scroll(LinearLayout body){ScrollView s=new ScrollView(this);s.setBackgroundColor(Color.WHITE);s.addView(body);body.setFocusableInTouchMode(true);body.requestFocus();return s;}
  private void showDay(){
    final String date=CalendarEngine.key(selected);List<TodoStore.Item> list=todos.forDate(date);StringBuilder msg=new StringBuilder(CalendarEngine.week(selected)+"  "+CalendarEngine.lunarLabel(selected));String note=CalendarEngine.annotation(selected);if(note.length()>0)msg.append("\n").append(note);String badge=holidays.badge(selected);if(badge.length()>0)msg.append("  ").append(badge);
    msg.append("\n\n");if(list.isEmpty())msg.append("这一天没有待办事项");else msg.append(list.size()).append(" 项待办，点击条目可编辑");
    final TodoStore.Item[] options=list.toArray(new TodoStore.Item[list.size()]);
    LinearLayout body=column();body.addView(label(msg.toString(),22));for(final TodoStore.Item i:options){TextView row=label((i.done?"☑ ":"□ ")+(i.time.length()>0?i.time+" ":"")+i.title+"  ›",20);body.addView(row);row.setOnClickListener(new View.OnClickListener(){public void onClick(View v){editTodo(i);}});}
    show(new AlertDialog.Builder(this).setTitle(date).setView(scroll(body)).setPositiveButton("新增待办",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){add();}}).setNegativeButton("关闭",null));
  }
  private void editTodo(final TodoStore.Item item){
    LinearLayout l=column();l.addView(label(item.id==0?"新增待办":"编辑待办",25));final EditText title=field("待办内容",item.title);l.addView(title);
    final EditText date=field("日期 yyyy-MM-dd",item.date);l.addView(date);final EditText time=field("时间 HH:mm（可选）",item.time==null?"":item.time);l.addView(time);
    AlertDialog.Builder builder=new AlertDialog.Builder(this).setView(scroll(l)).setPositiveButton("保存",null).setNegativeButton("取消",null);
    if(item.id!=0)builder.setNeutralButton("删除",null);
    final AlertDialog dialog=show(builder);
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){public void onClick(View v){String d=date.getText().toString().trim(),t=time.getText().toString().trim(),s=title.getText().toString().trim();if(s.length()==0){title.setError("请输入内容");return;}if(!d.matches("\\d{4}-\\d{2}-\\d{2}")){date.setError("请输入 yyyy-MM-dd");return;}try{String[] a=d.split("-");CalendarEngine.date(Integer.parseInt(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2]));}catch(Exception e){date.setError("日期无效");return;}if(t.length()>0&&!t.matches("([01]\\d|2[0-3]):[0-5]\\d")){time.setError("请输入 HH:mm");return;}item.title=s;item.date=d;item.time=t;todos.save(item);dialog.dismiss();render();}});
    if(item.id!=0)dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener(){public void onClick(View v){show(new AlertDialog.Builder(MainActivity.this).setMessage("确定删除这条待办？").setPositiveButton("删除",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){todos.delete(item.id);dialog.dismiss();render();}}).setNegativeButton("取消",null));}});
  }
  public void settings(){
    LinearLayout l=column();final int[] display={orientationMode};final TextView orientation=label("屏幕方向："+orientationName(display[0])+"  ›",22);l.addView(orientation);orientation.setOnClickListener(new View.OnClickListener(){public void onClick(View v){display[0]=(display[0]+1)%3;orientation.setText("屏幕方向："+orientationName(display[0])+"  ›");}});
    l.addView(label("点按切换：自动旋转、竖屏、横屏",17));l.addView(label("天气城市",24));final EditText city=field("城市名称，例如 Beijing",weather.city());l.addView(city);
    l.addView(label("中文搜索失败时可输入拼音或英文名称",17));final boolean[] fahrenheit={weather.fahrenheit()},auto={weather.autoLocation()};
    final TextView unit=label((fahrenheit[0]?"☑":"□")+" 使用华氏度 °F",21);l.addView(unit);unit.setOnClickListener(new View.OnClickListener(){public void onClick(View v){fahrenheit[0]=!fahrenheit[0];unit.setText((fahrenheit[0]?"☑":"□")+" 使用华氏度 °F");}});
    final TextView location=label((auto[0]?"☑":"□")+" 自动定位（使用近期网络定位）",21);l.addView(location);location.setOnClickListener(new View.OnClickListener(){public void onClick(View v){auto[0]=!auto[0];location.setText((auto[0]?"☑":"□")+" 自动定位（使用近期网络定位）");}});
    l.addView(label("定位由设备提供；获取不到时可关闭并手动选择城市",17));l.addView(label("版本 1.0 · 天气数据 Open-Meteo",17));
    show(new AlertDialog.Builder(this).setTitle("设置").setView(scroll(l)).setPositiveButton("保存",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){String newCity=city.getText().toString().trim();boolean weatherChanged=false;if(newCity.length()>0&&!newCity.equals(weather.city())){weather.setCity(newCity);weatherData=null;weatherChanged=true;}if(auto[0]!=weather.autoLocation()){weather.setAutoLocation(auto[0]);weatherData=null;weatherChanged=true;}weather.setFahrenheit(fahrenheit[0]);if(orientationMode!=display[0]){orientationMode=display[0];getPreferences(MODE_PRIVATE).edit().putInt("orientation",orientationMode).apply();applyOrientation();}render();if(weatherChanged)refresh(true);}}).setNeutralButton("刷新天气",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){refresh(true);}}).setNegativeButton("关闭",null));
  }
  private void refresh(final boolean manual){if(loading||(!manual&&!weather.stale()))return;loading=true;new Thread(new Runnable(){public void run(){try{final Weather.Data d=weather.refresh();runOnUiThread(new Runnable(){public void run(){weatherData=d;loading=false;screen.refreshWeather(d);}});}catch(final Exception e){android.util.Log.e("RidiWeather","Weather refresh failed",e);runOnUiThread(new Runnable(){public void run(){loading=false;if(manual)show(new AlertDialog.Builder(MainActivity.this).setMessage(e.getMessage()+"\n将继续显示上次有效天气（如有）。").setPositiveButton("知道了",null));screen.refreshWeather(weatherData);}});}}}).start();}
}
