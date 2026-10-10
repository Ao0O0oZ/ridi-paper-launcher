package com.ridi.calendar;

/** Offline coordinates for commonly selected Chinese cities when old Android cannot use geocoding HTTPS. */
public final class ChineseCities {
  private ChineseCities() {}
  private static final String[][] CITIES={
    {"北京","Beijing","39.9075","116.39723"},{"上海","Shanghai","31.2304","121.4737"},{"天津","Tianjin","39.3434","117.3616"},{"重庆","Chongqing","29.563","106.5516"},
    {"广州","Guangzhou","23.1291","113.2644"},{"深圳","Shenzhen","22.5431","114.0579"},{"成都","Chengdu","30.5728","104.0668"},{"杭州","Hangzhou","30.2741","120.1551"},
    {"南京","Nanjing","32.0603","118.7969"},{"武汉","Wuhan","30.5928","114.3055"},{"西安","Xi'an","34.3416","108.9398"},{"苏州","Suzhou","31.2989","120.5853"},
    {"郑州","Zhengzhou","34.7466","113.6254"},{"长沙","Changsha","28.2282","112.9388"},{"合肥","Hefei","31.8206","117.2272"},{"济南","Jinan","36.6512","117.1201"},
    {"青岛","Qingdao","36.0671","120.3826"},{"福州","Fuzhou","26.0745","119.2965"},{"厦门","Xiamen","24.4798","118.0894"},{"昆明","Kunming","25.0389","102.7183"},
    {"沈阳","Shenyang","41.8057","123.4315"},{"大连","Dalian","38.914","121.6147"},{"哈尔滨","Harbin","45.8038","126.5349"},{"长春","Changchun","43.8171","125.3235"},
    {"石家庄","Shijiazhuang","38.0428","114.5149"},{"太原","Taiyuan","37.8706","112.5489"},{"南昌","Nanchang","28.682","115.8582"},{"南宁","Nanning","22.817","108.3669"},
    {"贵阳","Guiyang","26.647","106.6302"},{"兰州","Lanzhou","36.0611","103.8343"},{"银川","Yinchuan","38.4872","106.2309"},{"西宁","Xining","36.6171","101.7782"},
    {"乌鲁木齐","Urumqi","43.8256","87.6168"},{"拉萨","Lhasa","29.652","91.1721"},{"海口","Haikou","20.044","110.1999"},{"三亚","Sanya","18.2528","109.5119"}
  };
  public static double[] find(String city){String name=city.trim();for(String[] c:CITIES)if(name.equals(c[0])||name.equalsIgnoreCase(c[1]))return new double[]{Double.parseDouble(c[2]),Double.parseDouble(c[3])};return null;}
}
