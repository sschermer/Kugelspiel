package de.sschermer.kugelspiel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.Locale;

/** Single-screen, three-button puzzle game. No non-Android runtime dependencies. */
final class GameView extends View {
    private static final float CEILING=164f, FLOOR=630f, R=22f, GATE_HALF=13f;
    private static final float GRAVITY=1120f, JUMP_V=460f, MAX_SPEED=310f;
    private static final float FIXED_STEP=1f/120f;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final HashSet<Integer> left=new HashSet<>(), right=new HashSet<>(), jump=new HashSet<>();
    private final android.content.SharedPreferences prefs;
    private float x,y,vx,vy,camX=0,camY=0,scale=1,clock=0,buffer=0,coyote=0;
    private float actionTime=0, levelTitleTime=3, finishTime=0;
    private int gravity=LevelData.DOWN, color=LevelData.NONE, level=0;
    private boolean onSurface=true,paused=false,complete=false;
    private String action="";
    long last=System.nanoTime();

    GameView(Context context) {
        super(context);
        prefs=context.getSharedPreferences("kugelspiel_progress",Context.MODE_PRIVATE);
        level=Math.max(0,Math.min(9,prefs.getInt("level",0)));
        reset(false);
        setFocusable(true);
    }

    private LevelData.Level world(){return LevelData.LEVELS.get(level);}
    void setPaused(boolean value) {
        paused=value;
        last=System.nanoTime();
        if(value){left.clear();right.clear();jump.clear();}
    }
    private void reset(boolean newGame) {
        if(newGame)level=0;
        x=100;y=FLOOR-R;vx=vy=0;
        gravity=LevelData.DOWN;color=LevelData.NONE;
        onSurface=true;buffer=0;coyote=0;complete=false;finishTime=0;
        levelTitleTime=3;actionTime=0;
        left.clear();right.clear();jump.clear();
        prefs.edit().putInt("level",level).apply();
    }
    private void next() {
        if(level==LevelData.LEVELS.size()-1) {
            complete=true;finishTime=0; action="Alle zehn Rätsel gelöst!";
        } else {
            level++;reset(false);
        }
    }
    private void status(String text) {action=text;actionTime=1.8f;}

    private static float clamp(float value,float low,float high) {
        return Math.max(low,Math.min(high,value));
    }
    private void tick(float dt) {
        if(paused || complete)return;
        clock+=dt;
        if(finishTime>0) {
            finishTime-=dt;
            if(finishTime<=0)next();
            return;
        }
        levelTitleTime=Math.max(0,levelTitleTime-dt);
        actionTime=Math.max(0,actionTime-dt);
        float direction=(right.isEmpty()?0:1)-(left.isEmpty()?0:1);
        float acceleration=onSurface?1600:980;
        if(direction!=0)vx+=direction*acceleration*dt;
        else vx*=Math.max(0,1-(onSurface?10f:4f)*dt);
        vx=clamp(vx,-MAX_SPEED,MAX_SPEED);
        buffer=Math.max(0,buffer-dt);
        if(onSurface)coyote=0.13f; else coyote=Math.max(0,coyote-dt);
        if(buffer>0 && coyote>0) {
            vy=-gravity*JUMP_V;
            onSurface=false;coyote=0;buffer=0;
        }
        float oldX=x;
        x+=vx*dt;
        for(LevelData.Gate gate:world().gates) {
            if(gate.color==color)continue;
            float upper=gateTop(gate),lower=gateBottom(gate);
            if(y+R<=upper || y-R>=lower)continue;
            float edgeLeft=gate.x-GATE_HALF-R;
            float edgeRight=gate.x+GATE_HALF+R;
            if(vx>0 && oldX<=edgeLeft && x>edgeLeft) {x=edgeLeft;vx=0;}
            if(vx<0 && oldX>=edgeRight && x<edgeRight) {x=edgeRight;vx=0;}
        }
        x=clamp(x,R,world().finish+R);
        vy=clamp(vy+gravity*GRAVITY*dt,-900,900);
        y+=vy*dt;
        if(y>=FLOOR-R){y=FLOOR-R;if(vy>0)vy=0;}
        if(y<=CEILING+R){y=CEILING+R;if(vy<0)vy=0;}
        onSurface=gravity==LevelData.DOWN ? y>=FLOOR-R-.8f : y<=CEILING+R+.8f;
        for(LevelData.Item item:world().items) {
            float iy=item.lane==LevelData.FLOOR?FLOOR-31:CEILING+31;
            if(Math.abs(x-item.x)>R+14 || Math.abs(y-iy)>R+21)continue;
            if(item.type==LevelData.COLOR_ITEM && item.value!=color) {
                color=item.value;
                status("Farbe: "+colorName(color));
            } else if(item.type==LevelData.GRAVITY_ITEM && item.value!=gravity) {
                gravity=item.value;
                onSurface=false;coyote=0;vy=0;
                status("Schwerkraft: "+(gravity==LevelData.UP?"nach oben":"nach unten"));
            }
        }
        // Repeated items are not consumed; touching them again resets the state.
        if(x>=world().finish-7) {
            finishTime=.75f;status("Rätsel gelöst");
            vx=0;vy=0;
        }
    }
    private static float gateTop(LevelData.Gate gate) {
        return gate.lane==LevelData.FLOOR?FLOOR-168:CEILING+4;
    }
    private static float gateBottom(LevelData.Gate gate) {
        return gate.lane==LevelData.CEILING?CEILING+168:FLOOR-4;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(getWidth()==0||getHeight()==0)return;
        float ww,hh;
        scale=Math.min(getWidth()/1120f,getHeight()/735f);
        ww=getWidth()/scale;hh=getHeight()/scale;
        long now=System.nanoTime();
        float elapsed=clamp((now-last)/1_000_000_000f,0,.05f);
        last=now;
        for(float t=elapsed;t>0;) {float dt=Math.min(FIXED_STEP,t);tick(dt);t-=dt;}
        float maxCamX=Math.max(-95f,world().finish+180-ww);
        float desiredX=clamp(x-ww*.49f,-95,maxCamX);
        camX+=(desiredX-camX)*Math.min(1,elapsed*7);
        // Keep both puzzle tracks in view, even during gravity reversals.
        float desiredY=(CEILING+FLOOR)*.5f-hh*.5f;
        camY+=(desiredY-camY)*Math.min(1,elapsed*6);
        canvas.save();
        canvas.scale(scale,scale);
        background(canvas,ww,hh);
        canvas.save();canvas.translate(-camX,-camY);
        drawSurfaces(canvas);
        for(LevelData.Gate g:world().gates)drawGate(canvas,g);
        for(LevelData.Item item:world().items)drawItem(canvas,item);
        drawStartFinish(canvas);
        drawBall(canvas);
        canvas.restore();
        hud(canvas,ww,hh);
        canvas.restore();
        postInvalidateOnAnimation();
    }

    private void background(Canvas c,float ww,float hh) {
        p.setShader(new LinearGradient(0,0,0,hh,0xff0e2030,0xff29414c,Shader.TileMode.CLAMP));
        c.drawRect(0,0,ww,hh,p);p.setShader(null);
        p.setStrokeWidth(1);
        p.setColor(0x264f7586);
        for(int i=-1;i<30;i++) {
            float xx=(i*115-camX*.13f)%(ww+115);
            c.drawLine(xx,0,xx,hh,p);
        }
        p.setColor(0x1d95adba);
        for(int i=0;i<12;i++) {
            float yy=i*93;
            c.drawLine(0,yy,ww,yy,p);
        }
    }
    private void drawSurfaces(Canvas c) {
        float from=-220,to=world().finish+250;
        // The massive rock/metal bands are actual collision surfaces, not decorations.
        surface(c,from,CEILING-120,to,CEILING,true);
        surface(c,from,FLOOR,to,FLOOR+175,false);
    }
    private void surface(Canvas c,float a,float top,float b,float bottom,boolean ceiling) {
        int light=0xff879ba7,mid=0xff445d6a,deep=0xff263c49;
        p.setShader(new LinearGradient(a,top,a,bottom,
                ceiling?deep:light,ceiling?light:deep,Shader.TileMode.CLAMP));
        c.drawRect(a,top,b,bottom,p);p.setShader(null);
        p.setColor(0xffb1c6ca);
        float rim=ceiling?bottom-7:top;
        c.drawRect(a,rim,b,rim+7,p);
        p.setStrokeWidth(2);p.setColor(0x65324654);
        for(float xx=25;xx<b;xx+=94) {
            c.drawLine(xx,top+16,xx,bottom-12,p);
            if(!ceiling)c.drawLine(xx+8,top+33,xx+54,top+33,p);
            else c.drawLine(xx+8,bottom-33,xx+54,bottom-33,p);
        }
        p.setColor(0x70d8ebeb);
        for(float xx=35;xx<b;xx+=94)c.drawCircle(xx,rim+(ceiling?0:10),2.7f,p);
    }
    private static int colorBright(int col) {
        switch(col) {case LevelData.BLUE:return 0xff64ddf2;
            case LevelData.AMBER:return 0xffffc06e;
            case LevelData.VIOLET:return 0xffbe9afa;
            default:return 0xffb8cedb;}
    }
    private static int colorDark(int col) {
        switch(col) {case LevelData.BLUE:return 0xff246d96;
            case LevelData.AMBER:return 0xff8c4c27;
            case LevelData.VIOLET:return 0xff573e87;
            default:return 0xff2f4b58;}
    }
    private static String colorName(int col) {
        switch(col) {case LevelData.BLUE:return "Blau";
            case LevelData.AMBER:return "Orange";
            case LevelData.VIOLET:return "Violett";
            default:return "Neutral";}
    }
    private static String colorSymbol(int col) {
        switch(col) {case LevelData.BLUE:return "B";
            case LevelData.AMBER:return "O";
            case LevelData.VIOLET:return "V";
            default:return "—";}
    }
    private void drawGate(Canvas c,LevelData.Gate gate) {
        float top=gateTop(gate),bottom=gateBottom(gate),gx=gate.x;
        boolean open=gate.color==color;
        p.setShader(new LinearGradient(gx-GATE_HALF,0,gx+GATE_HALF,0,
                colorBright(gate.color),colorDark(gate.color),Shader.TileMode.CLAMP));
        p.setAlpha(open?63:235);
        c.drawRoundRect(new RectF(gx-GATE_HALF,top,gx+GATE_HALF,bottom),7,7,p);
        p.setShader(null);p.setAlpha(255);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(open?2:3);
        p.setColor(open?0x6696e5f5:colorBright(gate.color));
        c.drawRoundRect(new RectF(gx-GATE_HALF,top,gx+GATE_HALF,bottom),7,7,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(open?0x77ffffff:0xfff9ffff);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(19);
        for(float yy=top+35;yy<bottom-12;yy+=90)
            c.drawText(colorSymbol(gate.color),gx,yy,p);
        p.setTextAlign(Paint.Align.LEFT);
        if(open) {
            p.setColor(0x669eebdb);
            c.drawCircle(gx,(top+bottom)*.5f,19,p);
        }
    }
    private void drawItem(Canvas c,LevelData.Item item) {
        float ix=item.x,iy=item.lane==LevelData.FLOOR?FLOOR-31:CEILING+31;
        boolean isColor=item.type==LevelData.COLOR_ITEM;
        int bright=isColor?colorBright(item.value):0xffb1eecb;
        int dark=isColor?colorDark(item.value):0xff29776d;
        p.setColor(0x553b7d8a);c.drawCircle(ix,iy,32,p);
        Path shape=new Path();
        if(isColor) {
            shape.moveTo(ix,iy-27);shape.lineTo(ix+24,iy);shape.lineTo(ix,iy+27);
            shape.lineTo(ix-24,iy);shape.close();
        } else {
            shape.moveTo(ix-24,iy-21);shape.lineTo(ix+24,iy-21);
            shape.lineTo(ix+24,iy+21);shape.lineTo(ix-24,iy+21);shape.close();
        }
        p.setShader(new LinearGradient(ix-25,iy-27,ix+24,iy+25,bright,dark,Shader.TileMode.CLAMP));
        c.drawPath(shape,p);p.setShader(null);
        p.setColor(0xffeaffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.3f);
        c.drawPath(shape,p);p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(20);p.setColor(0xfff7ffff);
        c.drawText(isColor?colorSymbol(item.value):(item.value==LevelData.UP?"↑":"↓"),ix,iy+7,p);
        p.setTextAlign(Paint.Align.LEFT);
    }
    private void drawStartFinish(Canvas c) {
        p.setColor(0xff9cb9bd);
        c.drawRoundRect(new RectF(88,FLOOR-95,98,FLOOR),4,4,p);
        p.setColor(0xff64e4b5);
        c.drawRoundRect(new RectF(world().finish-10,CEILING+10,world().finish+8,FLOOR-8),7,7,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);
        c.drawCircle(world().finish-1,(CEILING+FLOOR)*.5f,35,p);
        p.setStyle(Paint.Style.FILL);
    }
    private void drawBall(Canvas c) {
        p.setColor(0x60070e19);
        if(gravity==LevelData.DOWN)c.drawOval(new RectF(x-28,FLOOR-8,x+28,FLOOR+3),p);
        else c.drawOval(new RectF(x-28,CEILING-4,x+28,CEILING+7),p);
        int bright=color==LevelData.NONE?0xfff7ffff:colorBright(color);
        int main=color==LevelData.NONE?0xffa3bac5:colorBright(color);
        int dark=color==LevelData.NONE?0xff142b39:colorDark(color);
        p.setShader(new RadialGradient(x-10,y-13,R*2.2f,
                new int[]{0xffffffff,bright,main,dark},
                new float[]{0f,.23f,.48f,1f},Shader.TileMode.CLAMP));
        c.drawCircle(x,y,R,p);p.setShader(null);
        c.save();c.rotate(x*180f/(float)Math.PI/R,x,y);
        p.setColor(0xa8edffff);p.setStrokeWidth(2.8f);p.setStyle(Paint.Style.STROKE);
        c.drawArc(new RectF(x-16,y-16,x+16,y+16),40,125,false,p);
        p.setColor(0x55445e69);
        c.drawArc(new RectF(x-12,y-12,x+12,y+12),214,122,false,p);
        p.setStyle(Paint.Style.FILL);c.restore();
        p.setColor(0xaaffffff);c.drawCircle(x-10,y-12,4.6f,p);
    }
    private void hud(Canvas c,float ww,float hh) {
        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        p.setTextSize(23);p.setColor(0xffebf7fb);
        c.drawText(String.format(Locale.ROOT,"KUGELSPIEL   %02d / 10",level+1),26,39,p);
        p.setTextSize(16);p.setColor(0xffb3d2dc);
        c.drawText(world().title+"  ·  "+world().clue,26,69,p);
        String mode="FARBE  "+colorName(color).toUpperCase(Locale.GERMAN)
                +"     GRAVITATION  "+(gravity==LevelData.UP?"↑":"↓");
        p.setTextSize(18);p.setColor(0xffe5f5f7);p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(mode,ww-25,38,p);p.setTextAlign(Paint.Align.LEFT);
        if(levelTitleTime>0 || actionTime>0) {
            String text=actionTime>0?action:"Rätsel "+(level+1)+": "+world().title;
            p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(24);
            p.setTextAlign(Paint.Align.CENTER);p.setColor(0xffd3f8f0);
            c.drawText(text,ww*.5f,113,p);p.setTextAlign(Paint.Align.LEFT);
        }
        button(c,105,hh-98,70,1,!left.isEmpty());
        button(c,275,hh-98,70,2,!right.isEmpty());
        button(c,ww-110,hh-101,76,3,!jump.isEmpty());
        if(complete) {
            p.setColor(0xd5112c37);c.drawRoundRect(new RectF(ww*.16f,hh*.27f,ww*.84f,hh*.66f),20,20,p);
            p.setTextAlign(Paint.Align.CENTER);p.setColor(0xffbdf5db);
            p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(35);
            c.drawText("ALLE ZEHN RÄTSEL GELÖST",ww*.5f,hh*.43f,p);
            p.setTextSize(21);p.setColor(0xfff2ffff);
            c.drawText("Zum Neustart auf SPRINGEN tippen",ww*.5f,hh*.53f,p);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }
    private void button(Canvas c,float bx,float by,float radius,int which,boolean pressed) {
        p.setColor(pressed?0xcc69dcc7:0x99416673);
        c.drawCircle(bx,by,radius,p);
        p.setColor(0xffb8d9e0);p.setStrokeWidth(3);
        p.setStyle(Paint.Style.STROKE);c.drawCircle(bx,by,radius,p);
        p.setStyle(Paint.Style.FILL);
        Path arrow=new Path();float s=26;
        if(which==1) {
            arrow.moveTo(bx-s,by);arrow.lineTo(bx+s*.65f,by-s);arrow.lineTo(bx+s*.65f,by+s);
        } else if(which==2) {
            arrow.moveTo(bx+s,by);arrow.lineTo(bx-s*.65f,by-s);arrow.lineTo(bx-s*.65f,by+s);
        } else {
            arrow.moveTo(bx,by-s);arrow.lineTo(bx-s,by+s*.6f);arrow.lineTo(bx+s,by+s*.6f);
        }
        arrow.close();p.setColor(0xfff1ffff);c.drawPath(arrow,p);
    }
    private int control(float rawX,float rawY) {
        float xx=rawX/scale,yy=rawY/scale;
        float ww=getWidth()/scale,hh=getHeight()/scale;
        if(Math.hypot(xx-105,yy-(hh-98))<88)return 1;
        if(Math.hypot(xx-275,yy-(hh-98))<88)return 2;
        if(Math.hypot(xx-(ww-110),yy-(hh-101))<96)return 3;
        return 0;
    }
    private void assign(int pointer,int target) {
        boolean previousJump=!jump.isEmpty();
        left.remove(pointer);right.remove(pointer);jump.remove(pointer);
        if(target==1)left.add(pointer);
        if(target==2)right.add(pointer);
        if(target==3) {
            jump.add(pointer);
            if(!previousJump) {
                if(complete)reset(true);
                else buffer=.15f;
            }
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action=event.getActionMasked(),at=event.getActionIndex();
        if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
            assign(event.getPointerId(at),control(event.getX(at),event.getY(at)));
        } else if(action==MotionEvent.ACTION_MOVE) {
            for(int i=0;i<event.getPointerCount();i++) {
                assign(event.getPointerId(i),control(event.getX(i),event.getY(i)));
            }
        } else if(action==MotionEvent.ACTION_POINTER_UP || action==MotionEvent.ACTION_UP) {
            assign(event.getPointerId(at),0);
        } else if(action==MotionEvent.ACTION_CANCEL) {
            left.clear();right.clear();jump.clear();
        }
        return true;
    }
}
