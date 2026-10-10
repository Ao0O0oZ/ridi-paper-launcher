package com.ridi.calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public final class TodoStore extends SQLiteOpenHelper {
  public static final class Item {
    public long id; public String date, time, title; public boolean done;
  }
  public TodoStore(Context c) { super(c,"todo.db",null,1); }
  public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE todo (_id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, time TEXT NOT NULL, title TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0)"); db.execSQL("CREATE INDEX todo_date ON todo(date)"); }
  public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {}
  public List<Item> forDate(String date) {
    List<Item> result=new ArrayList<Item>();
    Cursor c=getReadableDatabase().query("todo",null,"date=?",new String[]{date},null,null,"done ASC, time ASC, _id DESC");
    try { while(c.moveToNext()) { Item i=new Item(); i.id=c.getLong(c.getColumnIndexOrThrow("_id")); i.date=c.getString(c.getColumnIndexOrThrow("date")); i.time=c.getString(c.getColumnIndexOrThrow("time")); i.title=c.getString(c.getColumnIndexOrThrow("title")); i.done=c.getInt(c.getColumnIndexOrThrow("done"))!=0; result.add(i); } } finally { c.close(); }
    return result;
  }
  public boolean hasItems(String date) { Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM todo WHERE date=? LIMIT 1",new String[]{date}); try { return c.moveToFirst(); } finally {c.close();} }
  public long save(Item i) { ContentValues v=new ContentValues(); v.put("date",i.date); v.put("time",i.time==null?"":i.time); v.put("title",i.title); v.put("done",i.done?1:0); if(i.id==0) return getWritableDatabase().insertOrThrow("todo",null,v); getWritableDatabase().update("todo",v,"_id=?",new String[]{Long.toString(i.id)}); return i.id; }
  public void delete(long id) { getWritableDatabase().delete("todo","_id=?",new String[]{Long.toString(id)}); }
}
