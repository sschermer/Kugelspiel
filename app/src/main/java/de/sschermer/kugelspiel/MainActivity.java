package de.sschermer.kugelspiel;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

public final class MainActivity extends Activity {
  private Game game;
  @Override public void onCreate(Bundle b) { super.onCreate(b); getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512); game=new Game(this); setContentView(game); }
  @Override protected void onPause() { super.onPause(); if(game!=null) game.paused=true; }
  @Override protected void onResume() { super.onResume(); if(game!=null) { game.paused=false; game.last=System.nanoTime(); } }

  static final class Segment { final float a,b,y; Segment(float a,float b,float y){this.a=a;this.b=b;this.y=y;} }
  static final class World {
    final ArrayList<Segment> floors=new ArrayList<>(), ledges=new ArrayList<>();
    final ArrayList<Float> spikes=new ArrayList<>();
    final int index; final float finish; final float checkpoint;
    World(int n) {
      index=n; finish=1860+90*n; checkpoint=finish*0.53f;
      // Progressively more, still short and reachable (maximum single gap 138 units).
      float previous=0;
      for(int k=0;k<n;k++) {
        float a=400+k*230+((n+k)%3)*22;
        if(a>finish-220) break;
        float width=65+(n%4)*18;
        floors.add(new Segment(previous,a,620));
        previous=a+width;
      }
      floors.add(new Segment(previous,finish+260,620));
      for(int k=0;k<n+1;k++) {
        float x=280+k*255+((k+n)%2)*40;
        if(x<finish-110 && safeFloor(x)) spikes.add(x);
      }
      if(n>=4) {
        for(int k=0;k<(n-2)/3;k++) {
          float x=600+k*440;
          ledges.add(new Segment(x,x+135,505-k%2*35));
        }
      }
    }
    boolean safeFloor(float x) {for(Segment s:floors) if(x>s.a+48 && x<s.b-48) return true; return false;}
  }
  static final class Game extends View {
    final Paint p=new Paint(3); final ArrayList<World> levels=new ArrayList<>();
    final HashSet<Integer> leftIds=new HashSet<>(), rightIds=new HashSet<>(), jumpIds=new HashSet<>();
    final float R=23, FLOOR=620;
    float x=110,y=FLOOR-R,vx=0,vy=0,cx=0,cy=280,checkpointX=110,scale=1,screenW,screenH;
    int stage=0; boolean grounded=true,paused=false, finished=false;
    float jumpBuffer=0,coyote=0, messageTime=2.5f; long last=System.nanoTime();
    Game(Context c) {super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);for(int i=0;i<10;i++)levels.add(new World(i));setFocusable(true);}
    private World w(){return levels.get(stage);}
    void restart(boolean atCheckpoint){x=atCheckpoint?checkpointX:110;y=FLOOR-R;vx=vy=0;grounded=true;coyote=0;jumpBuffer=0;finished=false;}
    void advance(){ if(stage<9){stage++;checkpointX=110;restart(false);messageTime=2;}else {finished=true;messageTime=100;} }
    void tick(float dt){
      if(paused||finished)return;
      boolean L=!leftIds.isEmpty(), RR=!rightIds.isEmpty();
      float dir=(RR?1:0)-(L?1:0), accel=grounded?1900:1150;
      if(dir!=0)vx+=dir*accel*dt;else vx*=Math.max(0,1-(grounded?8:2)*dt);
      vx=Math.max(-430,Math.min(430,vx));
      if(!grounded)coyote=Math.max(0,coyote-dt); else coyote=.105f;
      jumpBuffer=Math.max(0,jumpBuffer-dt);
      if(jumpBuffer>0&&coyote>0){vy=-675;grounded=false;coyote=0;jumpBuffer=0;}
      float oldY=y;
      vy=Math.min(vy+1550*dt,1150);
      x+=vx*dt;y+=vy*dt;
      grounded=false;
      if(vy>=0) {
        for(Segment s:w().floors) if(x+R>s.a && x-R<s.b && oldY+R<=s.y+4 && y+R>=s.y) {y=s.y-R;vy=0;grounded=true;break;}
        if(!grounded)for(Segment s:w().ledges)if(x+R>s.a&&x-R<s.b&&oldY+R<=s.y+4&&y+R>=s.y){y=s.y-R;vy=0;grounded=true;break;}
      }
      if(x<R){x=R;vx=0;}
      if(x>=w().checkpoint && checkpointX<300){checkpointX=w().checkpoint;messageTime=1.2f;}
      for(float sx:w().spikes)if(Math.abs(x-sx)<R+14 && y+R>FLOOR-25 && y-R<FLOOR){restart(true);messageTime=1.3f;return;}
      if(y>FLOOR+500){restart(true);messageTime=1.2f;return;}
      if(x>w().finish){advance();return;}
      messageTime=Math.max(0,messageTime-dt);
    }
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);
      screenW=getWidth();screenH=getHeight();scale=Math.min(screenW/1100f,screenH/750f);
      long now=System.nanoTime();float dt=Math.min(.033f,(now-last)*1e-9f);last=now;
      for(float t=dt;t>0;){float step=Math.min(.008f,t);tick(step);t-=step;}
      float visibleW=screenW/scale,visibleH=screenH/scale;
      float targetX=x-visibleW*.50f,targetY=y-visibleH*.46f;
      cx+=(targetX-cx)*Math.min(1,dt*6.5f);cy+=(targetY-cy)*Math.min(1,dt*5f);
      c.save();c.scale(scale,scale);
      paintBackground(c,visibleW,visibleH);
      c.save();c.translate(-cx,-cy);
      drawTerrain(c,visibleW);
      drawSpikes(c);
      drawMarkers(c);
      drawBall(c);
      c.restore();
      drawInterface(c,visibleW,visibleH);
      c.restore();
      postInvalidateOnAnimation();
    }
    void paintBackground(Canvas c,float ww,float hh){
      p.setShader(new LinearGradient(0,0,0,hh,0xff101f33,0xff273c4c,Shader.TileMode.CLAMP));c.drawRect(0,0,ww,hh,p);p.setShader(null);
      // Wide, visually quiet structural grid; no decorative pickups.
      p.setColor(0x1d9db5c7);p.setStrokeWidth(1);
      for(int i=0;i<20;i++){float xx=((i*135-cx*.12f)% (ww+135)+(ww+135))%(ww+135);c.drawLine(xx,0,xx,hh,p);}
      p.setShader(new LinearGradient(0,hh*.7f,0,hh,0x00486372,0x55486372,Shader.TileMode.CLAMP));c.drawRect(0,hh*.7f,ww,hh,p);p.setShader(null);
    }
    void drawTerrain(Canvas c,float ww){
      for(Segment s:w().floors)platform(c,s,150);
      for(Segment s:w().ledges)platform(c,s,23);
    }
    void platform(Canvas c,Segment s,float depth){
      RectF rect=new RectF(s.a,s.y,s.b,s.y+depth);
      p.setShader(new LinearGradient(s.a,s.y,s.a,s.y+depth,0xff7c8e99,0xff253a47,Shader.TileMode.CLAMP));c.drawRoundRect(rect,7,7,p);p.setShader(null);
      p.setColor(0xffa5b7bc);c.drawRoundRect(new RectF(s.a,s.y,s.b,s.y+8),4,4,p);
      p.setColor(0x4b0b1e29);p.setStrokeWidth(2);
      for(float z=s.a+70;z<s.b;z+=95)c.drawLine(z,s.y+12,z,s.y+depth-6,p);
      p.setColor(0x55788d93);for(float z=s.a+24;z<s.b;z+=65)c.drawCircle(z,s.y+18,2.4f,p);
    }
    void drawSpikes(Canvas c) {
      for(float s:w().spikes){Path path=new Path();path.moveTo(s-21,FLOOR);path.lineTo(s-10,FLOOR-29);path.lineTo(s,FLOOR-5);path.lineTo(s+11,FLOOR-33);path.lineTo(s+23,FLOOR);path.close();p.setShader(new LinearGradient(s,FLOOR-32,s,FLOOR,0xffffc38a,0xffb54842,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);}
    }
    void drawMarkers(Canvas c){
      p.setColor(0x99cbeacb);c.drawRoundRect(new RectF(78,FLOOR-110,87,FLOOR),4,4,p);
      p.setColor(0xff62d5ae);c.drawRoundRect(new RectF(w().finish,FLOOR-130,w().finish+13,FLOOR),6,6,p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);c.drawCircle(w().finish+6,FLOOR-137,19,p);p.setStyle(Paint.Style.FILL);
      float cp=w().checkpoint;p.setColor(checkpointX>300?0xff72e6ce:0xffe2c77e);c.drawRoundRect(new RectF(cp-5,FLOOR-76,cp+5,FLOOR),4,4,p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(cp,FLOOR-78,13,p);p.setStyle(Paint.Style.FILL);
    }
    void drawBall(Canvas c){
      p.setColor(0x66000000);c.drawOval(new RectF(x-26,FLOOR-7,x+26,FLOOR+2),p);
      p.setShader(new RadialGradient(x-9,y-12,R*2.1f,new int[]{0xfff8ffff,0xffb7c9d0,0xff647985,0xff172b38},new float[]{0,.26f,.62f,1},Shader.TileMode.CLAMP));c.drawCircle(x,y,R,p);p.setShader(null);
      c.save();c.rotate(x*180f/(float)Math.PI/R,x,y);
      p.setColor(0x9ee7f5ff);p.setStrokeWidth(3);p.setStyle(Paint.Style.STROKE);c.drawArc(new RectF(x-R*.77f,y-R*.77f,x+R*.77f,y+R*.77f),30,125,false,p);
      p.setColor(0x664c6579);c.drawArc(new RectF(x-R*.52f,y-R*.52f,x+R*.52f,y+R*.52f),215,115,false,p);p.setStyle(Paint.Style.FILL);c.restore();
      p.setColor(0xdfffffff);c.drawCircle(x-10,y-12,4,p);p.setColor(0x80ffffff);c.drawCircle(x-12,y-14,7,p);
    }
    void drawInterface(Canvas c,float ww,float hh){
      p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(23);p.setColor(0xffecf6f8);c.drawText("KUGELSPIEL  /  "+String.format(java.util.Locale.ROOT,"%02d",stage+1)+" / 10",25,40,p);
      p.setTextSize(16);p.setColor(0xffb8d0d5);c.drawText("Ziel: grüner Ring   •   Checkpoint: Leuchte",25,65,p);
      drawControl(c,92,hh-98,68,"◀",!leftIds.isEmpty());
      drawControl(c,245,hh-98,68,"▶",!rightIds.isEmpty());
      drawControl(c,ww-108,hh-105,77,"↥",!jumpIds.isEmpty());
      if(messageTime>0){p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(25);p.setColor(0xffeafaff);String msg=finished?"ALLE 10 LEVEL GESCHAFFT!":checkpointX>300?"CHECKPOINT AKTIV":"LEVEL "+(stage+1);c.drawText(msg,ww*.5f-p.measureText(msg)*.5f,100,p);}
      if(finished){p.setTextSize(18);String msg="Tippe auf Springen für einen Neustart";c.drawText(msg,ww*.5f-p.measureText(msg)*.5f,145,p);}
    }
    void drawControl(Canvas c,float x,float y,float r,String symbol,boolean pressed){
      p.setColor(pressed?0xbb7ce4db:0x883d6474);c.drawCircle(x,y,r,p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0xffb7d5dc);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);
      p.setTextSize(48);p.setTypeface(Typeface.DEFAULT_BOLD);p.setColor(0xffeffcff);c.drawText(symbol,x-p.measureText(symbol)/2,y+16,p);
    }
    int control(float sx,float sy){
      float px=sx/scale,py=sy/scale,hh=getHeight()/scale,ww=getWidth()/scale;
      if(Math.hypot(px-92,py-(hh-98))<83)return 1;
      if(Math.hypot(px-245,py-(hh-98))<83)return 2;
      if(Math.hypot(px-(ww-108),py-(hh-105))<95)return 3;
      return 0;
    }
    void assign(int id,int button){leftIds.remove(id);rightIds.remove(id);jumpIds.remove(id);if(button==1)leftIds.add(id);if(button==2)rightIds.add(id);if(button==3){if(jumpIds.isEmpty()){if(finished){stage=0;checkpointX=110;restart(false);}jumpBuffer=.13f;}jumpIds.add(id);}}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
      int a=e.getActionMasked(),idx=e.getActionIndex();
      if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_POINTER_DOWN){assign(e.getPointerId(idx),control(e.getX(idx),e.getY(idx)));}
      else if(a==MotionEvent.ACTION_MOVE){for(int i=0;i<e.getPointerCount();i++)assign(e.getPointerId(i),control(e.getX(i),e.getY(i)));}
      else if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_POINTER_UP){assign(e.getPointerId(idx),0);}
      else if(a==MotionEvent.ACTION_CANCEL){leftIds.clear();rightIds.clear();jumpIds.clear();}
      return true;
    }
  }
}
