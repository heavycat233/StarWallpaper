package app.starfield.wallpaper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.IntConsumer;

public final class MainActivity extends Activity {
    private static final int BG=0xff0c1018,PANEL=0xff181e2a,INK=0xfff1eee8,MUTED=0xffa1a5b5,GOLD=0xffdfcfb0;
    private Settings settings;private LocationTracker location;private SkyPreview preview;private LinearLayout body;private boolean binding;
    private TextView subtitle,locationStatus,dateStatus,viewStatus;
    private Spinner clockSpinner,modeSpinner,placeSpinner;
    private Switch twinkle,labels,constellations,follow,realTime,desktopClock,nebulae,moon;
    private final HashMap<String,SeekBar> sliders=new HashMap<>();private final HashMap<String,TextView> sliderLabels=new HashMap<>();
    private final HashMap<Integer,Button> directionButtons=new HashMap<>();private Button zenith;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final String[] placeNames={"香港","拉萨","丽江","悉尼","阿塔卡马","雷克雅未克","自定义 / 当前定位"};
    private final double[][] positions={{22.3193,114.1694},{29.652,91.172},{26.872,100.230},{-33.8688,151.2093},{-23.023,-67.755},{64.1466,-21.9426}};
    private final String[] zones={"Asia/Hong_Kong","Asia/Shanghai","Asia/Shanghai","Australia/Sydney","America/Santiago","Atlantic/Reykjavik"};
    private final Runnable statusTick=new Runnable(){@Override public void run(){refreshStatus();handler.postDelayed(this,2000);}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);settings=new Settings(this);location=new LocationTracker(this,settings);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(12),dp(22),dp(32));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        scroll.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());view.setPadding(i.left,i.top,i.right,i.bottom);return insets;});setContentView(scroll);
        TextView brand=text("✦  星野",25,INK);brand.setLetterSpacing(.12f);body.addView(brand);TextView tagline=text("STARFIELD  /  此刻的真实星空",11,MUTED);tagline.setLetterSpacing(.12f);add(tagline,4,18);
        FrameLayout frame=new FrameLayout(this);frame.setBackground(shape(0xff242a36,24));frame.setPadding(dp(4),dp(4),dp(4),dp(4));frame.setClipToOutline(true);preview=new SkyPreview(this);frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));LinearLayout.LayoutParams frameParams=new LinearLayout.LayoutParams(dp(200),dp(420));frameParams.gravity=Gravity.CENTER_HORIZONTAL;body.addView(frame,frameParams);
        subtitle=text("正在载入真实星图…",11,MUTED);subtitle.setGravity(Gravity.CENTER);add(subtitle,12,14);
        Button apply=button("应用动态壁纸",true);body.addView(apply,lp(-1,50));apply.setOnClickListener(v->applyWallpaper());
        Button fullscreen=button("全屏预览",false);add(fullscreen,6,18);fullscreen.setOnClickListener(v->startActivity(new Intent(this,PreviewActivity.class)));
        Button matchPreview=button("载入对话预览参数",false);add(matchPreview,2,10);matchPreview.setOnClickListener(v->{location.stop();settings.position(22.3193,114.1694,"香港","Asia/Hong_Kong");settings.prefs.edit().putString("clock","digital").putString("uiMode","minimal").putInt("magnitude",9).putInt("faintBrightness",120).putInt("brightSize",70).putInt("clockTop",13).putInt("fov",180).putInt("azimuth",94).putInt("altitude",79).putInt("glow",148).putInt("dust",50).putBoolean("twinkle",true).putInt("twinkleStrength",140).putBoolean("labels",false).putBoolean("constellations",false).putBoolean("nebulae",true).putInt("nebulaBrightness",140).putInt("nebulaSize",250).putBoolean("moon",true).putInt("moonSize",8).putBoolean("followLocation",false).putBoolean("systemTime",false).putLong("fixedEpoch",java.time.Instant.parse("2026-10-28T17:30:00Z").toEpochMilli()).apply();refresh();toast("已载入香港预览时刻；开启跟随真实日期和时间可恢复实时");});
        heading("时钟与界面");clockSpinner=select("时钟样式",new String[]{"数字时钟","传统指针","使用系统时钟（仅星空）"},position->{if(!binding){settings.text("clock",new String[]{"digital","analog","none"}[position]);refresh();}});
        modeSpinner=select("界面显示",new String[]{"精简版 · 仅时间与日期","标准版 · 日期、位置与朝向"},position->{if(!binding){settings.text("uiMode",position==0?"minimal":"standard");refresh();}});
        range("clockTop","时钟位置",5,65,"%",value->settings.number("clockTop",value));
        desktopClock=toggle("桌面也显示自绘时钟",false,value->settings.flag("desktopClock",value));
        note("系统锁屏时钟由手机系统保留。出现双时钟时，可选择“使用系统时钟”，或调整自绘时钟位置；壁纸无法隐藏系统解锁界面。");
        heading("星空");range("magnitude","星等上限",3,11," 等",value->settings.number("magnitude",value));range("faintBrightness","暗星亮度",50,400,"%",value->settings.number("faintBrightness",value));note("主要增强 6–11 等暗星；100% 为原始亮度，默认 120%。");range("fov","对角线视场",120,180,"°",value->settings.number("fov",value));
        range("brightSize","亮星星点大小",50,100,"%",value->settings.number("brightSize",value));
        heading("星云与月球");nebulae=toggle("真实位置星云",true,value->settings.flag("nebulae",value));moon=toggle("月球与真实月相",true,value->settings.flag("moon",value));range("nebulaBrightness","星云亮度",0,250,"%",value->settings.number("nebulaBrightness",value));range("nebulaSize","星云显示尺寸（100% 为真实角尺寸）",100,400,"%",value->settings.number("nebulaSize",value));range("moonSize","月球显示尺寸",1,12,"×",value->settings.number("moonSize",value));note("星云色彩与纹理为艺术表现。月球位置、月相随日期和地点变化；显示尺寸默认 8×，可调回真实角直径 1×。地平线以下不显示。");Button moonView=button("朝向月球",false);add(moonView,2,6);moonView.setOnClickListener(v->focusMoon(false));Button night=button("夜景示例",false);add(night,2,6);night.setOnClickListener(v->focusMoon(true));
        heading("视角朝向");viewStatus=text("",12,GOLD);add(viewStatus,0,8);GridLayout grid=new GridLayout(this);grid.setColumnCount(3);int[] azimuths={315,0,45,270,-1,90,225,180,135};String[] names={"西北","北","东北","西","天顶","东","西南","南","东南"};
        for(int i=0;i<azimuths.length;i++){final int az=azimuths[i];Button b=button(names[i],false);b.setTextSize(13);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(46);p.columnSpec=GridLayout.spec(i%3,1,1f);p.rowSpec=GridLayout.spec(i/3);p.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(b,p);if(az<0)zenith=b;else directionButtons.put(az,b);b.setOnClickListener(v->{Settings.Snapshot s=settings.snapshot();if(az<0)settings.number("altitude",90);else{settings.number("azimuth",az);if(s.altitude==90)settings.number("altitude",45);}refresh();});}body.addView(grid,lp(-1,-2));
        range("azimuth","方位角",0,359,"°",value->settings.number("azimuth",value));range("altitude","仰角",0,90,"°",value->settings.number("altitude",value));Button galaxy=button("朝向当前可见银河",false);add(galaxy,2,16);galaxy.setOnClickListener(v->{Settings.Snapshot s=settings.snapshot();int[] p=SkyMath.galaxyFraming(s.epoch(),s.lat,s.lon);settings.prefs.edit().putInt("azimuth",p[0]).putInt("altitude",p[1]).apply();refresh();});
        heading("银河与动态");range("glow","银河光晕",0,200,"%",value->settings.number("glow",value));range("dust","暗云层次",0,150,"%",value->settings.number("dust",value));twinkle=toggle("星点闪烁",true,value->settings.flag("twinkle",value));range("twinkleStrength","闪烁强度",0,200,"%",value->settings.number("twinkleStrength",value));labels=toggle("亮星名称",false,value->setAnnotation("labels",value));constellations=toggle("星座连线与名称",false,value->setAnnotation("constellations",value));note("开启亮星名称或星座显示时，自动切换到标准版；精简版只保留时间与日期。");range("fps","动画帧率",5,30," fps",value->settings.number("fps",value));
        heading("真实位置");placeSpinner=select("观测位置",placeNames,position->{if(binding||position>=6)return;Settings.Snapshot s=settings.snapshot();if(s.place.equals(placeNames[position]))return;settings.flag("followLocation",false);location.stop();settings.position(positions[position][0],positions[position][1],placeNames[position],zones[position]);refresh();});
        Button manual=button("自定义经纬度与时区",false);add(manual,4,8);manual.setOnClickListener(v->manualPosition());follow=toggle("跟随真实位置",false,value->{if(value)enableLocation();else{settings.flag("followLocation",false);location.stop();refresh();}});
        Button locate=button("更新当前位置",false);add(locate,2,5);locate.setOnClickListener(v->enableLocation());
        Button background=button("允许旅行时自动更新",false);add(background,2,5);background.setOnClickListener(v->backgroundLocation());locationStatus=text("",12,MUTED);add(locationStatus,2,8);note("位置只在本机使用。后台更新需在权限中选择“始终允许”；关闭定位或拒绝权限时，继续使用最近位置。锁屏可见时低频更新，后台定位可能受系统节电限制。");
        heading("日期与时间");realTime=toggle("跟随真实日期和时间",true,value->{settings.prefs.edit().putLong("fixedEpoch",settings.snapshot().epoch()).putBoolean("systemTime",value).apply();refresh();});dateStatus=text("",12,MUTED);add(dateStatus,4,8);Button customDate=button("选择观测日期与时间",false);add(customDate,2,10);customDate.setOnClickListener(v->pickDate());
        heading("星图与隐私");Button about=button("星图来源、许可与使用说明",false);add(about,0,12);about.setOnClickListener(v->showAbout());note("星野 1.2.2 · Android 17+\n离线星表，无广告、无账号、无分析上传。\n银河光晕和暗云是按真实银道定位的艺术增强；白天仍显示星空。");
        refresh();
    }
    private void focusMoon(boolean example){try{Settings.Snapshot s=settings.snapshot();long epoch=s.epoch();if(example){ZonedDateTime z=Instant.now().atZone(ZoneId.of(s.zone));for(int i=1;i<=30;i++){epoch=z.toLocalDate().plusDays(i).atTime(1,30).atZone(ZoneId.of(s.zone)).toInstant().toEpochMilli();if(CelestialLayer.horizontal(epoch,s.lat,s.lon)[1]>25)break;}settings.prefs.edit().putLong("fixedEpoch",epoch).putBoolean("systemTime",false).apply();}double[] h=CelestialLayer.horizontal(epoch,s.lat,s.lon);if(h[1]<=0){toast("月球当前位于地平线下，请选择其他时刻");return;}settings.prefs.edit().putBoolean("moon",true).putInt("azimuth",(int)Math.round((h[0]+(example?8:0))%360)).putInt("altitude",(int)Math.round(SkyMath.clamp(h[1]+(example?22:0),0,80))).apply();refresh();if(example)toast("夜景示例：指定日期，关闭示例请开启跟随真实日期和时间");}catch(Exception e){toast("星历尚未载入，请稍后重试");}}
    private void applyWallpaper(){
        WallpaperManager manager=WallpaperManager.getInstance(this);if(!manager.isWallpaperSupported()||!manager.isSetWallpaperAllowed()){toast("当前用户或设备不允许设置壁纸");return;}
        Intent intent=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,StarWallpaperService.class));
        try{startActivity(intent);}catch(Exception e){try{startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}catch(Exception ignored){toast("请在系统壁纸设置中选择“星野”动态壁纸");}}
    }
    private void refresh(){
        binding=true;Settings.Snapshot s=settings.snapshot();clockSpinner.setSelection(s.clock.equals("analog")?1:s.clock.equals("none")?2:0);modeSpinner.setSelection(s.uiMode.equals("minimal")?0:1);
        int[] values={s.clockTop,s.magnitude,s.faintBrightness,s.fov,s.azimuth,s.altitude,s.glow,s.dust,s.fps,s.brightSize,s.nebulaBrightness,s.nebulaSize,s.moonSize,s.twinkleStrength};String[] keys={"clockTop","magnitude","faintBrightness","fov","azimuth","altitude","glow","dust","fps","brightSize","nebulaBrightness","nebulaSize","moonSize","twinkleStrength"};for(int i=0;i<keys.length;i++)sliders.get(keys[i]).setProgress(values[i]);
        nebulae.setChecked(s.nebulae);moon.setChecked(s.moon);twinkle.setChecked(s.twinkle);labels.setChecked(s.labels);constellations.setChecked(s.constellations);follow.setChecked(s.followLocation);realTime.setChecked(s.systemTime);desktopClock.setChecked(s.desktopClock);
        int selected=6;for(int i=0;i<6;i++)if(placeNames[i].equals(s.place))selected=i;placeSpinner.setSelection(selected);
        for(Integer az:directionButtons.keySet())styleButton(directionButtons.get(az),s.altitude!=90&&az==s.azimuth);styleButton(zenith,s.altitude==90);
        viewStatus.setText(s.altitude==90?"天顶 · 仰角 90°":SkyMath.direction(s.azimuth)+" "+s.azimuth+"° · 仰角 "+s.altitude+"°");binding=false;refreshStatus();preview.requestRender();
    }
    private void refreshStatus(){if(preview==null)return;Settings.Snapshot s=settings.snapshot();if(s.followLocation&&s.place.equals("当前位置")&&placeSpinner.getSelectedItemPosition()!=6)placeSpinner.setSelection(6);subtitle.setText(preview.renderer.error!=null?preview.renderer.error:"真实星图 · "+s.magnitude+" 等 / "+s.fov+"° · "+(s.uiMode.equals("minimal")?"精简版":"标准版"));locationStatus.setText(String.format(Locale.ROOT,"%s  %.4f°, %.4f°\n%s",s.place,s.lat,s.lon,s.followLocation?location.status:"手动位置"));dateStatus.setText(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm · VV",Locale.ROOT).format(Instant.ofEpochMilli(s.epoch()).atZone(ZoneId.of(s.zone))));}
    private void setAnnotation(String key,boolean enabled){
        android.content.SharedPreferences.Editor edit=settings.prefs.edit().putBoolean(key,enabled);
        if(enabled)edit.putString("uiMode","standard");
        edit.apply();refresh();
    }
    private void enableLocation(){
        if(!LocationTracker.foregroundAllowed(this)){requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},40);return;}
        settings.flag("followLocation",true);location.start(true);refresh();
    }
    private void backgroundLocation(){
        if(!LocationTracker.foregroundAllowed(this)){enableLocation();return;}
        new AlertDialog.Builder(this).setTitle("旅行时更新星空位置").setMessage("在系统应用权限中将位置设为“始终允许”，星野才可在锁屏可见时低频获取位置。坐标只用于本机星图；关闭开关即可停止。设备的节电策略可能延迟更新。").setNegativeButton("取消",null).setPositiveButton("前往权限设置",(d,w)->{settings.flag("followLocation",true);startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}).show();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==40){if(LocationTracker.foregroundAllowed(this))enableLocation();else{settings.flag("followLocation",false);refresh();toast("未获得定位权限，继续使用手动位置");}}}
    private void manualPosition(){
        Settings.Snapshot s=settings.snapshot();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(8),dp(22),dp(8));EditText lat=edit(box,"纬度 −90 至 90",Double.toString(s.lat),true),lon=edit(box,"经度 −180 至 180",Double.toString(s.lon),true),zone=edit(box,"时区，例如 Asia/Shanghai",s.zone,false),name=edit(box,"位置名称",s.place,false);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("自定义位置").setView(box).setNegativeButton("取消",null).setPositiveButton("保存",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{double la=Double.parseDouble(lat.getText().toString().trim()),lo=Double.parseDouble(lon.getText().toString().trim());String zo=zone.getText().toString().trim();ZoneId.of(zo);if(!Double.isFinite(la)||!Double.isFinite(lo)||Math.abs(la)>90||Math.abs(lo)>180)throw new IllegalArgumentException();String place=name.getText().toString().trim();settings.flag("followLocation",false);location.stop();settings.position(la,lo,place.isEmpty()?"自定义位置":place.substring(0,Math.min(30,place.length())),zo);refresh();dialog.dismiss();}catch(Exception e){toast("请检查经纬度范围和有效时区名称");}}));dialog.show();
    }
    private void pickDate(){Settings.Snapshot s=settings.snapshot();ZonedDateTime z=Instant.ofEpochMilli(s.epoch()).atZone(ZoneId.of(s.zone));DatePickerDialog dialog=new DatePickerDialog(this,(picker,year,month,day)->new TimePickerDialog(this,(time,hour,minute)->{try{LocalDateTime local=LocalDateTime.of(year,month+1,day,hour,minute);ZoneId zone=ZoneId.of(s.zone);if(zone.getRules().getValidOffsets(local).isEmpty()){toast("该时间位于夏令时跳转空档，请重新选择");return;}long epoch=local.atZone(zone).toInstant().toEpochMilli();settings.prefs.edit().putLong("fixedEpoch",epoch).putBoolean("systemTime",false).apply();refresh();}catch(Exception e){toast("时间无效");}},z.getHour(),z.getMinute(),true).show(),z.getYear(),z.getMonthValue()-1,z.getDayOfMonth());dialog.getDatePicker().setMinDate(Instant.parse("1900-01-01T00:00:00Z").toEpochMilli());dialog.getDatePicker().setMaxDate(Instant.parse("2100-12-31T23:59:00Z").toEpochMilli());dialog.show();}
    private void showAbout(){new Thread(()->{try{StarCatalog catalog=StarCatalog.get(this);String message=catalog.source+"\n"+StarCatalog.readText(this,"CELESTIAL-LICENSE.txt")+"\n"+StarCatalog.readText(this,"OpenNGC-LICENSE.txt")+"\n"+StarCatalog.readText(this,"BSD-3-Clause-d3-celestial.txt")+"\n"+String.format(Locale.ROOT,"%,d",catalog.count)+" 颗离线真实恒星\n\n"+catalog.notice+"\n\n与对话预览使用相同的 HYG v4.1 数据库和量化记录。暗星端不完备。\n\n数据：David Nash / Astronomy Nexus，CC BY-SA 4.0。\nhttps://github.com/astronexus/HYG-Database\n\n"+StarCatalog.readText(this,"HYG-LICENSE.txt")+"\n\n系统时钟、AOD 与安全解锁界面由设备系统管理。本 App 不上传位置信息，也不请求互联网权限。";runOnUiThread(()->{ScrollView scroll=new ScrollView(this);TextView text=text(message,13,INK);text.setPadding(dp(20),dp(10),dp(20),dp(10));scroll.addView(text);new AlertDialog.Builder(this).setTitle("星图与许可").setView(scroll).setPositiveButton("关闭",null).show();});}catch(Exception e){runOnUiThread(()->toast("星表信息读取失败"));}},"Starfield-about").start();}
    private EditText edit(LinearLayout parent,String hint,String value,boolean numeric){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(INK);e.setTextSize(14);e.setHint(hint);e.setContentDescription(hint);e.setText(value);if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);parent.addView(e,lp(-1,56));return e;}
    private Spinner select(String title,String[] options,IntConsumer selected){label(title);Spinner spinner=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,options);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);spinner.setBackgroundTintList(ColorStateList.valueOf(GOLD));spinner.setContentDescription(title);add(spinner,0,14);spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){selected.accept(pos);}});return spinner;}
    private void range(String key,String title,int min,int max,String suffix,IntConsumer action){TextView value=text("",13,INK);add(value,9,0);SeekBar seek=new SeekBar(this);seek.setMin(min);seek.setMax(max);seek.setContentDescription(title);seek.setProgressTintList(ColorStateList.valueOf(GOLD));seek.setThumbTintList(ColorStateList.valueOf(GOLD));add(seek,0,5);sliders.put(key,seek);sliderLabels.put(key,value);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int progress,boolean fromUser){value.setText(title+"    "+progress+suffix);if(fromUser&&!binding){action.accept(progress);if(key.equals("azimuth")||key.equals("altitude"))refresh();else preview.requestRender();}}});value.setText(title+"    "+min+suffix);}
    private interface ToggleAction{void accept(boolean value);}
    private Switch toggle(String title,boolean checked,ToggleAction action){Switch toggle=new Switch(this);toggle.setText(title);toggle.setTextColor(INK);toggle.setTextSize(13);toggle.setChecked(checked);toggle.setPadding(0,dp(7),0,dp(7));toggle.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{GOLD,MUTED}));add(toggle,3,4);toggle.setOnCheckedChangeListener((b,value)->{if(!binding){action.accept(value);preview.requestRender();}});return toggle;}
    private void heading(String title){View line=new View(this);line.setBackgroundColor(0x20ffffff);LinearLayout.LayoutParams p=lp(-1,1);p.topMargin=dp(20);p.bottomMargin=dp(19);body.addView(line,p);TextView h=text(title,17,INK);h.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));add(h,0,10);}
    private void label(String title){add(text(title,13,MUTED),7,3);}
    private void note(String value){TextView t=text(value,12,MUTED);t.setLineSpacing(dp(3),1f);add(t,4,8);}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private Button button(String title,boolean active){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setMinWidth(0);b.setMinimumHeight(dp(44));styleButton(b,active);return b;}
    private void styleButton(Button b,boolean active){b.setTextColor(active?0xff24252a:INK);b.setBackground(shape(active?GOLD:PANEL,10));}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private LinearLayout.LayoutParams lp(int width,int height){return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height));}
    private void add(View view,int top,int bottom){LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(top);p.bottomMargin=dp(bottom);body.addView(view,p);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override protected void onResume(){super.onResume();preview.onResume();if(settings.snapshot().followLocation)location.start(true);refresh();handler.post(statusTick);}
    @Override protected void onPause(){handler.removeCallbacks(statusTick);location.stop();preview.onPause();super.onPause();}
    @Override protected void onDestroy(){preview.close();settings.close();super.onDestroy();}
}
