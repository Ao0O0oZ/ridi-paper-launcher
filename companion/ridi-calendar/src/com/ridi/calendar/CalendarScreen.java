package com.ridi.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Fixed reference proportions, scaled to the actual content area in either orientation. */
public final class CalendarScreen extends View {
  public interface Listener { void previous(); void next(); void today(); void select(Calendar day); void toggle(TodoStore.Item item); void edit(TodoStore.Item item); void add(); void settings(); }
  private static final int BLACK=Color.BLACK, WHITE=Color.WHITE, ADJACENT=Color.rgb(128,128,128);
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final List<Hit> hits=new ArrayList<Hit>();
  private final android.graphics.Typeface weatherIcons;
  private final Listener listener; private final HolidaySchedule holidays; private final TodoStore todos;
  private Calendar shown, selected; private Weather.Data weather; private Weather weatherStore; private List<TodoStore.Item> selectedTodos=new ArrayList<TodoStore.Item>();
  private final List<Cell> monthCells=new ArrayList<Cell>();
  private float sx,sy; private boolean landscape;
  private static final class Hit { RectF r; Runnable action; Hit(float l,float t,float r,float b,Runnable a){this.r=new RectF(l,t,r,b);action=a;} }
  private static final class Cell { Calendar day; String lunar,note,badge; boolean hasTodo,inMonth; }
  public CalendarScreen(Context context,Listener listener,HolidaySchedule holidays,TodoStore todos,Weather weatherStore) { super(context); this.listener=listener;this.holidays=holidays;this.todos=todos;this.weatherStore=weatherStore;weatherIcons=android.graphics.Typeface.createFromAsset(context.getAssets(),"qweather-icons.ttf");p.setColor(BLACK);setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
  public void update(Calendar shown,Calendar selected,Weather.Data data){this.shown=(Calendar)shown.clone();this.selected=(Calendar)selected.clone();this.weather=data;selectedTodos=todos.forDate(CalendarEngine.key(selected));prepareMonth();invalidate();}
  private void prepareMonth(){monthCells.clear();Calendar first=CalendarEngine.date(shown.get(Calendar.YEAR),shown.get(Calendar.MONTH)+1,1);int offset=CalendarEngine.mondayOffset(first);int rows=(offset+first.getActualMaximum(Calendar.DAY_OF_MONTH)+6)/7;Calendar day=(Calendar)first.clone();day.add(Calendar.DAY_OF_MONTH,-offset);for(int i=0;i<rows*7;i++){Cell cell=new Cell();cell.day=(Calendar)day.clone();CalendarEngine.Info info=CalendarEngine.describe(day);cell.lunar=info.lunarDay;cell.note=info.annotation;cell.badge=holidays.badge(day);cell.hasTodo=todos.hasItems(CalendarEngine.key(day));cell.inMonth=day.get(Calendar.MONTH)==shown.get(Calendar.MONTH);monthCells.add(cell);day.add(Calendar.DAY_OF_MONTH,1);}}
  public void refreshTodos(){selectedTodos=todos.forDate(CalendarEngine.key(selected));prepareMonth();if(landscape)invalidate(0,(int)(615*sy),getWidth(),getHeight());else invalidate(0,(int)(605*sy),getWidth(),getHeight());}
  public void refreshWeather(Weather.Data data){weather=data;if(landscape)invalidate((int)(405*sx),0,getWidth(),(int)(325*sy));else invalidate((int)(545*sx),0,getWidth(),(int)(465*sy));}
  private void line(Canvas c,float x1,float y1,float x2,float y2,float width){p.setColor(BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);}
  private void rect(Canvas c,float l,float t,float r,float b,boolean fill){p.setColor(BLACK);p.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);p.setStrokeWidth(1.5f);c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.FILL);}
  private void txt(Canvas c,String s,float x,float baseline,float size,boolean bold,boolean serif,int color){p.setColor(color);p.setTypeface(serif?android.graphics.Typeface.create("serif",bold?1:0):android.graphics.Typeface.create("sans-serif",bold?1:0));p.setTextSize(size);p.setStyle(Paint.Style.FILL);c.drawText(s,x,baseline,p);}
  private void centered(Canvas c,String s,float cx,float baseline,float size,boolean bold,int color){p.setTypeface(android.graphics.Typeface.create("serif",bold?1:0));p.setTextSize(size);txt(c,s,cx-p.measureText(s)/2,baseline,size,bold,true,color);}
  private void hit(float l,float t,float r,float b,Runnable a){hits.add(new Hit(l,t,r,b,a));}
  @Override protected void onDraw(Canvas actual) {
    super.onDraw(actual); actual.drawColor(WHITE); hits.clear(); landscape=getWidth()>getHeight(); float w=landscape?1448:1072,h=landscape?1072:1448; sx=getWidth()/w;sy=getHeight()/h;
    Canvas c=actual; c.save();c.scale(sx,sy); if(landscape)drawLandscape(c);else drawPortrait(c);c.restore();
  }
  private void drawPortrait(Canvas c){
    line(c,540,27,540,457,1.5f);drawDate(c,35,18,505,456,false);drawWeather(c,567,18,1038,455,false);
    line(c,29,478,1043,478,1.5f);drawMonth(c,29,490,1043,1084);line(c,29,1097,1043,1097,1.5f);drawTodos(c,29,1108,1043,1360,false);
  }
  private void drawLandscape(Canvas c){
    line(c,391,20,391,970,1.5f);drawDate(c,40,20,370,605,true);line(c,27,615,368,615,1.5f);drawTodos(c,30,627,368,970,true);
    drawWeather(c,415,20,1420,306,true);line(c,414,324,1420,324,1.5f);drawMonth(c,414,337,1420,968);
  }
  private void drawDate(Canvas c,float l,float t,float r,float b,boolean wide){
    Calendar now=CalendarEngine.today();float big=214;
    txt(c,String.format(java.util.Locale.CHINA,"%02d",now.get(Calendar.DAY_OF_MONTH)),l,wide?t+big:t+190,big,true,true,BLACK);
    float metaY=wide?t+303:t+268;
    txt(c,now.get(Calendar.YEAR)+"年"+(now.get(Calendar.MONTH)+1)+"月",l,metaY,wide?45:45,true,true,BLACK);
    txt(c,CalendarEngine.week(now),l,metaY+(wide?68:60),wide?43:43,true,true,BLACK);
    float divider=wide?t+395:t+347;line(c,l,divider,wide?r:r-145,divider,1.5f);
    float lunarY=divider+(wide?72:54);
    txt(c,CalendarEngine.lunarLabel(now),l,lunarY,wide?36:34,true,true,BLACK);
    String note=CalendarEngine.annotation(now),badge=holidays.badge(now);if(badge.length()>0)note+=(note.length()>0?" · ":"")+("休".equals(badge)?"放假":"调休工作");
    if(note.length()>0)txt(c,note,l,lunarY+(wide?59:46),wide?35:32,true,true,BLACK);
    hit(l,t,r,b,new Runnable(){public void run(){listener.today();}});
  }
  private void drawWeather(Canvas c,float l,float t,float r,float b,boolean wide){
    float split=wide?780:0;float forecastTop=wide?t+6:t+247;float forecastL=wide?split+17:l;float forecastR=r;
    txt(c,weather==null?weatherStore.city():weather.city,l+7,t+41,wide?39:37,true,true,BLACK);
    drawPin(c,wide?split-32:r-26,t+28);hit(wide?l:r-75,t,wide?split:r,t+68,new Runnable(){public void run(){listener.settings();}});
    if(weather==null){txt(c,"天气暂无数据",l+15,t+150,wide?30:30,true,true,BLACK);txt(c,"点按位置图标设置城市或刷新",l+10,t+196,20,false,true,BLACK);}
    else {
      drawWeatherIcon(c,l+(wide?80:83),t+(wide?153:126),weather.code,wide?45:45);
      float textX=l+(wide?167:186);
      txt(c,Weather.description(weather.code),textX,t+(wide?137:106),wide?39:39,true,true,BLACK);
      Weather.Forecast today=weather.days[0];
      txt(c,weatherStore.temp(today.high)+" / "+weatherStore.temp(today.low),textX,t+(wide?198:163),wide?40:38,false,true,BLACK);
      txt(c,"风 "+Math.round(weather.wind)+" km/h",textX,t+(wide?238:202),wide?23:23,false,true,BLACK);
      txt(c,"当前 "+weatherStore.temp(weather.current)+"  ·  更新 "+weather.updated,l+5,t+(wide?279:227),wide?17:17,false,true,BLACK);
    }
    if(wide)line(c,split,t+7,split,b-5,1.5f);else line(c,l,t+238,r,t+238,1.5f);
    txt(c,"未来5天",forecastL,forecastTop+29,wide?29:28,true,true,BLACK);
    if(weather==null){txt(c,"预报不可用",forecastL,forecastTop+91,22,false,true,BLACK);return;}
    float cell=(forecastR-forecastL)/5;
    for(int i=1;i<=5;i++){
      Weather.Forecast f=weather.days[i];float x=forecastL+(i-1)*cell,cx=x+cell/2;
      if(i>1)line(c,x,forecastTop+54,x,b-4,1);
      centered(c,f.label,cx,forecastTop+67,wide?22:20,false,BLACK);
      drawWeatherIcon(c,cx,forecastTop+(wide?115:110),f.code,wide?23:21);
      centered(c,Weather.description(f.code),cx,forecastTop+(wide?158:153),wide?22:19,false,BLACK);
      centered(c,Math.round(f.high)+"/"+Math.round(f.low),cx,forecastTop+(wide?192:183),wide?21:19,false,BLACK);
    }
  }
  private void drawPin(Canvas c,float x,float y){p.setColor(BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.5f);android.graphics.Path pin=new android.graphics.Path();pin.moveTo(x,y+14);pin.cubicTo(x-20,y-13,x+20,y-13,x,y+14);c.drawPath(pin,p);c.drawCircle(x,y-2,3.5f,p);p.setStyle(Paint.Style.FILL);}
  private void drawWeatherIcon(Canvas c,float x,float y,int code,float radius){
    String glyph=String.valueOf((char)Weather.iconGlyph(code));
    p.setColor(BLACK);p.setStyle(Paint.Style.FILL);p.setTypeface(weatherIcons);p.setTextSize(radius*2.4f);
    c.drawText(glyph,x-p.measureText(glyph)/2f,y-(p.ascent()+p.descent())/2f,p);
  }
  private void drawMonth(Canvas c,float l,float t,float r,float b){
    int year=shown.get(Calendar.YEAR),month=shown.get(Calendar.MONTH)+1;float width=r-l;float titleH=74,weekdayH=53;
    txt(c,year+"年"+month+"月",l+10,t+46,42,true,true,BLACK);
    drawChevron(c,r-146,t+25,false);drawChevron(c,r-48,t+25,true);
    hit(r-195,t,r-100,t+70,new Runnable(){public void run(){listener.previous();}});hit(r-95,t,r,t+70,new Runnable(){public void run(){listener.next();}});
    hit(l,t,l+width*.55f,t+65,new Runnable(){public void run(){listener.today();}});
    for(int col=0;col<7;col++)centered(c,"一二三四五六日".substring(col,col+1),l+(col+.5f)*width/7,t+titleH+38,27,true,BLACK);
    line(c,l,t+titleH+weekdayH,r,t+titleH+weekdayH,1.5f);
    int rows=monthCells.size()/7;float top=t+titleH+weekdayH;float ch=(b-top)/rows;float cw=width/7;
    Calendar today=CalendarEngine.today();
    for(int row=0;row<rows;row++)for(int col=0;col<7;col++){
      Cell cell=monthCells.get(row*7+col);final Calendar day=cell.day;float x=l+col*cw,y=top+row*ch;boolean inMonth=cell.inMonth;boolean isToday=inMonth&&CalendarEngine.sameDay(day,today);
      if(isToday){p.setColor(BLACK);c.drawRoundRect(new RectF(x+12,y+5,x+cw-12,y+ch-6),7,7,p);}
      int ink=isToday?WHITE:(inMonth?BLACK:ADJACENT);float small=Math.max(20,Math.min(25,ch*.22f));
      centered(c,Integer.toString(day.get(Calendar.DAY_OF_MONTH)),x+cw/2,y+ch*.36f,inMonth?Math.min(40,ch*.36f):Math.min(32,ch*.31f),inMonth,ink);
      centered(c,cell.lunar,x+cw/2,y+ch*.64f,small,false,ink);
      String annotation=cell.note;if(annotation.length()>4)annotation=annotation.substring(0,4);
      if(annotation.length()>0)centered(c,annotation,x+cw/2,y+ch*.88f,Math.max(17,small*.82f),false,ink);
      String badge=cell.badge;if(badge.length()>0)txt(c,badge,x+(isToday?19:11),y+22,16,true,true,ink);
      if(cell.hasTodo){p.setColor(ink);c.drawCircle(x+cw-17,y+16,4.5f,p);}
      hit(x,y,x+cw,y+ch,new Runnable(){public void run(){listener.select(day);}});
    }
  }
  private void drawChevron(Canvas c,float x,float y,boolean right){float direction=right?1:-1;line(c,x-direction*10,y,x+direction*7,y+18,3);line(c,x+direction*7,y+18,x-direction*10,y+36,3);}
  private void drawTodos(Canvas c,float l,float t,float r,float b,boolean side){
    drawMemo(c,l+15,t+25,side?34:36);txt(c,"待办事项",l+(side?76:76),t+43,side?36:36,true,true,BLACK);
    String count=selectedTodos.size()+" 项";p.setTypeface(android.graphics.Typeface.create("serif",0));p.setTextSize(23);float countWidth=p.measureText(count);txt(c,count,r-countWidth-(side?58:66),t+40,23,false,true,BLACK);
    txt(c,"＋",r-43,t+42,side?37:37,false,true,BLACK);hit(r-70,t,r,t+60,new Runnable(){public void run(){listener.add();}});
    if(side)line(c,l,t+61,r,t+61,1.5f);
    String date=CalendarEngine.key(selected);if(side)txt(c,date,l+5,t+91,18,false,true,BLACK);
    int max=side?5:4;float rowTop=t+(side?103:64);float rowH=side?51:44;
    if(selectedTodos.isEmpty())txt(c,"暂无待办，点击 ＋ 新建",l+12,rowTop+39,side?24:23,false,true,BLACK);
    for(int i=0;i<Math.min(max,selectedTodos.size());i++){
      final TodoStore.Item item=selectedTodos.get(i);float y=rowTop+i*rowH;rect(c,l+15,y+7,l+47,y+39,item.done);if(item.done){lineColor(c,l+21,y+23,l+29,y+31,3,WHITE);lineColor(c,l+29,y+31,l+43,y+12,3,WHITE);}
      String label=(item.time.length()>0?item.time+"  ":"")+item.title;int maxChars=side?11:36;if(label.length()>maxChars)label=label.substring(0,maxChars-1)+"…";
      txt(c,label,l+73,y+36,side?25:25,!item.done,true,BLACK);if(item.done)line(c,l+73,y+28,Math.min(r-35,l+73+label.length()*(side?24:23)),y+28,1.4f);
      line(c,l+8,y+rowH,r-5,y+rowH,1);
      hit(l,y,l+62,y+rowH,new Runnable(){public void run(){listener.toggle(item);}});hit(l+62,y,r,y+rowH,new Runnable(){public void run(){listener.edit(item);}});
    }
    if(selectedTodos.size()>max){txt(c,"查看全部 ›",r-150,b-13,21,false,true,BLACK);hit(r-180,b-55,r,b,new Runnable(){public void run(){listener.select((Calendar)selected.clone());}});}
  }
  private void lineColor(Canvas c,float x1,float y1,float x2,float y2,float width,int color){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);}
  private void drawMemo(Canvas c,float x,float y,float size){p.setColor(BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);c.drawRect(x-size*.44f,y-size*.55f,x+size*.44f,y+size*.55f,p);c.drawLine(x-size*.24f,y-size*.22f,x+size*.24f,y-size*.22f,p);c.drawLine(x-size*.24f,y,x+size*.24f,y,p);c.drawLine(x-size*.24f,y+size*.22f,x+size*.1f,y+size*.22f,p);p.setStyle(Paint.Style.FILL);}
  @Override public boolean onTouchEvent(MotionEvent event){if(event.getAction()!=MotionEvent.ACTION_UP)return true;float x=event.getX()/sx,y=event.getY()/sy;for(int i=hits.size()-1;i>=0;i--)if(hits.get(i).r.contains(x,y)){hits.get(i).action.run();return true;}return true;}
}
