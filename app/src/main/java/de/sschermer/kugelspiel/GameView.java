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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

/**
 * Iteration 003: a side-scrolling, score-gated, branching platform puzzle.
 * Canvas artwork is drawn at runtime; no external images or libraries.
 */
final class GameView extends View {
    private static final float FLOOR=LevelData.BOTTOM, CEILING=LevelData.TOP;
    private static final float R=22f, GATE_HALF=15f, G=1150f;
    private static final float JUMP_SPEED=566f, MAX_SPEED=318f, STEP=1f/120f;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final HashSet<Integer> left=new HashSet<>(), right=new HashSet<>(), jump=new HashSet<>();
    private final android.content.SharedPreferences prefs;

    private float x,y,vx,vy,camX,camY,scale=1f;
    private float jumpBuffer,coyote,toastTime,titleTime,goalToastCooldown,restartHold,finishTime;
    private int gravity=LevelData.DOWN,color=LevelData.NONE,level=0;
    private int score=0, checkpointScore=0;
    private float respawnX=110;
    private int respawnColor=LevelData.NONE, respawnGravity=LevelData.DOWN;
    private boolean onSurface=true,paused=false,complete=false,checkpointActive=false;
    private boolean[] collected=new boolean[0], savedCollected=new boolean[0];
    private boolean[] checkpointUsed=new boolean[0], savedCheckpoints=new boolean[0];
    private String toast="";
    private long last=System.nanoTime();

    GameView(Context context) {
        super(context);
        prefs=context.getSharedPreferences("kugelspiel_progress",Context.MODE_PRIVATE);
        level=Math.max(0,Math.min(LevelData.LEVELS.size()-1,prefs.getInt("level_003",0)));
        beginLevel(false);
        setFocusable(true);
    }

    private LevelData.Level data(){return LevelData.LEVELS.get(level);}
    void setPaused(boolean value) {
        paused=value; last=System.nanoTime();
        if(value){left.clear();right.clear();jump.clear();restartHold=0;}
    }
    private void beginLevel(boolean fromFirst) {
        if(fromFirst)level=0;
        score=0;checkpointScore=0;color=LevelData.NONE;gravity=LevelData.DOWN;
        respawnX=110;respawnColor=color;respawnGravity=gravity;
        collected=new boolean[data().items.size()];
        savedCollected=collected.clone();
        checkpointUsed=new boolean[data().checkpoints.size()];
        savedCheckpoints=checkpointUsed.clone();
        checkpointActive=false;
        x=110;y=FLOOR-R;vx=vy=0;onSurface=true;
        jumpBuffer=0;coyote=.13f;finishTime=0;restartHold=0;
        complete=false;titleTime=3;goalToastCooldown=0;toastTime=0;
        camX=-90;camY=0;
        left.clear();right.clear();jump.clear();
        prefs.edit().putInt("level_003",level).apply();
    }
    private void atCheckpoint(int i) {
        if(checkpointUsed[i])return;
        checkpointUsed[i]=true;respawnX=data().checkpoints.get(i)+25;
        checkpointScore=score;respawnColor=color;respawnGravity=LevelData.DOWN;
        savedCollected=collected.clone();savedCheckpoints=checkpointUsed.clone();
        checkpointActive=true;message("Checkpoint gesichert");
    }
    private void die() {
        x=respawnX;y=FLOOR-R;vx=vy=0;
        score=checkpointScore;color=respawnColor;gravity=respawnGravity;
        collected=savedCollected.clone();
        checkpointUsed=savedCheckpoints.clone();
        onSurface=true;jumpBuffer=0;coyote=.13f;finishTime=0;
        message(checkpointActive?"Zurück zum Checkpoint":"Zurück zum Start");
    }
    private void message(String msg){toast=msg;toastTime=1.65f;}
    private void finishLevel() {
        int old=prefs.getInt("best_003_"+level,0);
        if(score>old)prefs.edit().putInt("best_003_"+level,score).apply();
        if(level==LevelData.LEVELS.size()-1){
            complete=true;finishTime=0;message("Alle zehn Level geschafft!");
        } else {
            level++;beginLevel(false);
        }
    }
    private static float clamp(float v,float low,float high) {return Math.max(low,Math.min(high,v));}
    private boolean floorAt(float px) {
        for(LevelData.Segment gap:data().gaps)
            if(px>gap.from+R*.45f && px<gap.to-R*.45f)return false;
        return true;
    }
    private float gateTop(LevelData.Gate g) {
        return g.lane==LevelData.FLOOR ? FLOOR-184 : CEILING+4;
    }
    private float gateBottom(LevelData.Gate g) {
        return g.lane==LevelData.CEILING ? CEILING+184 : FLOOR-4;
    }

    private void tick(float dt) {
        if(paused || complete)return;
        if(finishTime>0) {
            finishTime-=dt;if(finishTime<=0)finishLevel();return;
        }
        titleTime=Math.max(0,titleTime-dt);
        toastTime=Math.max(0,toastTime-dt);
        goalToastCooldown=Math.max(0,goalToastCooldown-dt);

        if(!left.isEmpty()&&!right.isEmpty()) {
            restartHold+=dt;
            if(restartHold>=2.5f){beginLevel(false);message("Level neu gestartet");return;}
        } else restartHold=0;

        float direction=(right.isEmpty()?0:1)-(left.isEmpty()?0:1);
        float accel=onSurface?1530f:930f;
        if(direction!=0)vx+=direction*accel*dt;
        else vx*=Math.max(0,1-(onSurface?9f:3f)*dt);
        vx=clamp(vx,-MAX_SPEED,MAX_SPEED);

        jumpBuffer=Math.max(0,jumpBuffer-dt);
        if(onSurface)coyote=.14f;else coyote=Math.max(0,coyote-dt);
        if(jumpBuffer>0 && coyote>0){
            vy=-gravity*JUMP_SPEED;onSurface=false;coyote=0;jumpBuffer=0;
        }
        float oldX=x,oldY=y;
        x+=vx*dt;
        for(LevelData.Gate gate:data().gates) {
            if(!gate.blocks(color))continue;
            if(y+R<gateTop(gate)||y-R>gateBottom(gate))continue;
            float leftEdge=gate.x-GATE_HALF-R,rightEdge=gate.x+GATE_HALF+R;
            if(vx>0 && oldX<=leftEdge && x>leftEdge){x=leftEdge;vx=0;}
            else if(vx<0 && oldX>=rightEdge && x<rightEdge){x=rightEdge;vx=0;}
        }
        x=clamp(x,R,data().finish+R);

        vy=clamp(vy+gravity*G*dt,-940,940);
        y+=vy*dt;
        boolean standing=false;

        // One-way metal shelves are traversable from below in normal gravity.
        if(gravity==LevelData.DOWN && vy>=0) {
            for(LevelData.Segment shelf:data().ledges) {
                if(x+R<=shelf.from || x-R>=shelf.to)continue;
                if(oldY+R<=shelf.y+5 && y+R>=shelf.y) {
                    y=shelf.y-R;vy=0;standing=true;break;
                }
            }
        } else if(gravity==LevelData.UP && vy<=0) {
            for(LevelData.Segment shelf:data().ledges) {
                if(x+R<=shelf.from || x-R>=shelf.to)continue;
                float under=shelf.y+18;
                if(oldY-R>=under-5 && y-R<=under) {
                    y=under+R;vy=0;standing=true;break;
                }
            }
        }
        if(gravity==LevelData.DOWN && floorAt(x) && y>=FLOOR-R && oldY<=FLOOR+55) {
            y=FLOOR-R;if(vy>0)vy=0;standing=true;
        }
        if(gravity==LevelData.UP && y<=CEILING+R) {
            y=CEILING+R;if(vy<0)vy=0;standing=true;
        }
        // After gravity switches, never tunnel into a floor or ceiling.
        if(gravity==LevelData.DOWN && y<CEILING+R)y=CEILING+R;
        if(gravity==LevelData.UP && y>FLOOR-R && floorAt(x))y=FLOOR-R;
        onSurface=standing;

        // Persistent switches + single-award points.
        for(int i=0;i<data().items.size();i++){
            LevelData.Item item=data().items.get(i);
            if(Math.hypot(x-item.x,y-item.y)>R+(item.type==LevelData.COLOR?39:34))continue;
            if(item.type==LevelData.POINT){
                if(!collected[i]){
                    collected[i]=true;score+=item.value;message("+"+item.value+" Punkte");
                }
            } else if(item.type==LevelData.COLOR && color!=item.value) {
                color=item.value;message("Farbe: "+colorName(color));
            } else if(item.type==LevelData.GRAVITY && gravity!=item.value) {
                gravity=item.value;onSurface=false;coyote=0;vy=0;
                message("Schwerkraft: "+(gravity==LevelData.UP?"nach oben":"nach unten"));
            }
        }
        for(int i=0;i<data().checkpoints.size();i++) {
            // Secure checkpoints when the player reaches the floor after a route.
            float cp=data().checkpoints.get(i);
            if(!checkpointUsed[i] && x>cp && gravity==LevelData.DOWN && y>FLOOR-60)
                atCheckpoint(i);
        }
        for(LevelData.Hazard spike:data().spikes){
            if(Math.abs(x-spike.x)<R+19 && y+R>spike.base-32 && y-R<spike.base)
            {die();return;}
        }
        if(y>FLOOR+230 || y<CEILING-200){die();return;}

        if(x>=data().finish-29) {
            if(score<data().target){
                x=data().finish-30;if(vx>0)vx=0;
                if(goalToastCooldown<=0){
                    message("Noch "+(data().target-score)+" Punkte bis zum Ausgang!");
                    goalToastCooldown=2f;
                }
            } else {
                finishTime=.72f;vx=vy=0;message("Ausgang geöffnet!");
            }
        }
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(getWidth()<=0||getHeight()<=0)return;
        scale=Math.min(getWidth()/1130f,getHeight()/755f);
        float ww=getWidth()/scale,hh=getHeight()/scale;
        long now=System.nanoTime();
        float elapsed=clamp((now-last)/1_000_000_000f,0,.04f);
        last=now;
        for(float remain=elapsed;remain>0;){
            float delta=Math.min(STEP,remain);tick(delta);remain-=delta;
        }
        float targetX=clamp(x-ww*.46f,-85f,Math.max(-85f,data().finish+115-ww));
        float targetY=clamp(y-hh*.58f,-35f,Math.max(-35f,FLOOR+175-hh));
        camX+=(targetX-camX)*Math.min(1,elapsed*6.5f);
        camY+=(targetY-camY)*Math.min(1,elapsed*4f);

        canvas.save();canvas.scale(scale,scale);
        background(canvas,ww,hh);
        canvas.save();canvas.translate(-camX,-camY);
        drawGround(canvas);
        for(LevelData.Segment shelf:data().ledges)drawShelf(canvas,shelf);
        for(LevelData.Hazard spike:data().spikes)drawSpike(canvas,spike);
        for(LevelData.Gate gate:data().gates)drawGate(canvas,gate);
        for(int i=0;i<data().items.size();i++)drawItem(canvas,data().items.get(i),collected[i]);
        drawMarkers(canvas);
        drawSphere(canvas);
        canvas.restore();
        drawHud(canvas,ww,hh);
        canvas.restore();
        postInvalidateOnAnimation();
    }
    private void background(Canvas c,float ww,float hh) {
        paint.setShader(new LinearGradient(0,0,0,hh,
                0xff0b1c2c,0xff284251,Shader.TileMode.CLAMP));
        c.drawRect(0,0,ww,hh,paint);paint.setShader(null);
        paint.setStrokeWidth(1);
        paint.setColor(0x264e7b8b);
        for(int i=-2;i<32;i++) {
            float x=(i*122-camX*.14f)%(ww+122);
            c.drawLine(x,0,x,hh,paint);
        }
        paint.setColor(0x1d6e9caa);
        for(int i=0;i<13;i++) {
            float y=(i*94-camY*.12f)%(hh+94);
            c.drawLine(0,y,ww,y,paint);
        }
    }
    private void drawGround(Canvas c){
        drawStone(c,-200,CEILING-125,data().finish+200,CEILING,true);
        float from=-200;
        for(LevelData.Segment gap:data().gaps){
            if(from<gap.from)drawStone(c,from,FLOOR,gap.from,FLOOR+210,false);
            // Shaded shaft really marks missing collision geometry.
            paint.setColor(0x99203241);
            c.drawRect(gap.from,FLOOR,gap.to,FLOOR+320,paint);
            paint.setColor(0xffd59667);
            c.drawRect(gap.from-5,FLOOR,gap.from+5,FLOOR+28,paint);
            c.drawRect(gap.to-5,FLOOR,gap.to+5,FLOOR+28,paint);
            from=gap.to;
        }
        drawStone(c,from,FLOOR,data().finish+200,FLOOR+210,false);
    }
    private void drawStone(Canvas c,float a,float top,float b,float bottom,boolean ceiling){
        if(b<=a)return;
        paint.setShader(new LinearGradient(0,top,0,bottom,
                ceiling?0xff27404c:0xff96aab3,
                ceiling?0xff889ca6:0xff283f4b,Shader.TileMode.CLAMP));
        c.drawRect(a,top,b,bottom,paint);paint.setShader(null);
        float edge=ceiling?bottom-8:top;
        paint.setColor(0xffbdd1d3);c.drawRect(a,edge,b,edge+8,paint);
        paint.setStrokeWidth(2);paint.setColor(0x66334751);
        for(float tx=Math.max(a,0)+20;tx<b;tx+=84){
            c.drawLine(tx,top+17,tx,bottom-15,paint);
            c.drawLine(tx+7,ceiling?bottom-31:top+30,
                    Math.min(b,tx+61),ceiling?bottom-31:top+30,paint);
        }
        paint.setColor(0x88d5e5e8);
        for(float tx=Math.max(a,0)+31;tx<b;tx+=84)
            c.drawCircle(tx,edge+(ceiling?1:14),2.3f,paint);
    }
    private void drawShelf(Canvas c,LevelData.Segment shelf) {
        RectF box=new RectF(shelf.from,shelf.y,shelf.to,shelf.y+19);
        paint.setShader(new LinearGradient(0,shelf.y,0,shelf.y+19,
                0xffc4d3d7,0xff465e69,Shader.TileMode.CLAMP));
        c.drawRoundRect(box,6,6,paint);paint.setShader(null);
        paint.setColor(0xffe2f4f2);
        c.drawRoundRect(new RectF(shelf.from,shelf.y,shelf.to,shelf.y+4),3,3,paint);
    }
    private void drawSpike(Canvas c,LevelData.Hazard spike){
        float xx=spike.x,yy=spike.base;
        Path triangles=new Path();
        triangles.moveTo(xx-27,yy);triangles.lineTo(xx-15,yy-32);
        triangles.lineTo(xx-4,yy);triangles.lineTo(xx+8,yy-36);
        triangles.lineTo(xx+27,yy);triangles.close();
        paint.setShader(new LinearGradient(xx,yy-34,xx,yy,
                0xffffe0b0,0xffc45135,Shader.TileMode.CLAMP));
        c.drawPath(triangles,paint);paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);paint.setColor(0xfff9b784);
        paint.setStrokeWidth(2);c.drawPath(triangles,paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private static int vivid(int color) {
        if(color==LevelData.BLUE)return 0xff08d7ff;
        if(color==LevelData.AMBER)return 0xffffa500;
        if(color==LevelData.VIOLET)return 0xffbe63ff;
        return 0xffd7ebf2;
    }
    private static int shadow(int color) {
        if(color==LevelData.BLUE)return 0xff056997;
        if(color==LevelData.AMBER)return 0xffa64a0f;
        if(color==LevelData.VIOLET)return 0xff6332a0;
        return 0xff263e4d;
    }
    private static String colorName(int col){
        if(col==LevelData.BLUE)return "BLAU";
        if(col==LevelData.AMBER)return "ORANGE";
        if(col==LevelData.VIOLET)return "VIOLETT";
        return "NEUTRAL";
    }
    private static String colorMark(int col){
        if(col==LevelData.BLUE)return "B";
        if(col==LevelData.AMBER)return "O";
        if(col==LevelData.VIOLET)return "V";
        return "–";
    }
    private void drawGate(Canvas c,LevelData.Gate gate){
        float gx=gate.x,top=gateTop(gate),bottom=gateBottom(gate);
        boolean open=!gate.blocks(color);
        paint.setShader(new LinearGradient(gx-15,0,gx+15,0,
                vivid(gate.color),shadow(gate.color),Shader.TileMode.CLAMP));
        paint.setAlpha(open?44:238);
        c.drawRoundRect(new RectF(gx-GATE_HALF,top,gx+GATE_HALF,bottom),7,7,paint);
        paint.setShader(null);paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(open?2:4);
        paint.setColor(open?0x806fc6db:vivid(gate.color));
        c.drawRoundRect(new RectF(gx-GATE_HALF,top,gx+GATE_HALF,bottom),7,7,paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(open?0x99ffffff:0xffffffff);
        paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(22);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        for(float yy=top+39;yy<bottom-6;yy+=75)
            c.drawText(colorMark(gate.color),gx,yy,paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }
    private void drawItem(Canvas c,LevelData.Item item,boolean used) {
        float xx=item.x,yy=item.y;
        if(item.type==LevelData.COLOR) {
            // Big high-saturation swatch: obvious even when zoomed far out.
            int bright=vivid(item.value);
            paint.setColor(0x8840dce6);c.drawCircle(xx,yy,48,paint);
            paint.setColor(0xff0c2535);c.drawCircle(xx,yy,43,paint);
            Path diamond=new Path();
            diamond.moveTo(xx,yy-39);diamond.lineTo(xx+36,yy);
            diamond.lineTo(xx,yy+39);diamond.lineTo(xx-36,yy);diamond.close();
            paint.setColor(bright);c.drawPath(diamond,paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xfff5ffff);paint.setStrokeWidth(4);c.drawPath(diamond,paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff10263a);paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(28);
            c.drawText(colorMark(item.value),xx,yy+10,paint);
            paint.setTextAlign(Paint.Align.LEFT);
        } else if(item.type==LevelData.GRAVITY) {
            paint.setColor(0x8882f1c1);c.drawCircle(xx,yy,43,paint);
            paint.setColor(0xff123b45);c.drawRoundRect(new RectF(xx-35,yy-35,xx+35,yy+35),11,11,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);
            paint.setColor(0xffa0ffd8);
            c.drawRoundRect(new RectF(xx-35,yy-35,xx+35,yy+35),11,11,paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffffffff);paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(49);
            c.drawText(item.value==LevelData.UP?"↑":"↓",xx,yy+17,paint);
            paint.setTextAlign(Paint.Align.LEFT);
        } else {
            // Single-award point token stays in the world after collection.
            // Pending: opaque black with a white number.
            // Collected: semi-transparent white with a black number.
            paint.setColor(used?0x88ffffff:0xff07090c);
            c.drawCircle(xx,yy,35,paint);
            paint.setColor(used?0xff000000:0xffffffff);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(item.value==10?28:31);
            c.drawText(Integer.toString(item.value),xx,yy+11,paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }
    private void drawMarkers(Canvas c) {
        paint.setColor(0xffa0bdc3);
        c.drawRoundRect(new RectF(76,FLOOR-90,87,FLOOR),4,4,paint);
        for(int i=0;i<data().checkpoints.size();i++) {
            float xx=data().checkpoints.get(i);
            paint.setColor(checkpointUsed[i]?0xff80f5c7:0xffe4c878);
            c.drawRoundRect(new RectF(xx-6,FLOOR-82,xx+6,FLOOR),4,4,paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);c.drawCircle(xx,FLOOR-86,16,paint);
            paint.setStyle(Paint.Style.FILL);
        }
        float xx=data().finish;
        boolean open=score>=data().target;
        paint.setColor(open?0xff63eac5:0xffe46e57);
        c.drawRoundRect(new RectF(xx-15,CEILING+10,xx+16,FLOOR-8),9,9,paint);
        paint.setColor(0xff153544);
        c.drawRoundRect(new RectF(xx-9,CEILING+20,xx+10,FLOOR-19),6,6,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5);
        paint.setColor(open?0xffb6ffe9:0xffffbaa2);
        c.drawCircle(xx,(CEILING+FLOOR)*.5f,40,paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(24);paint.setColor(0xffffffff);
        c.drawText(open?"✓":"🔒",xx,(CEILING+FLOOR)*.5f+8,paint);
        if(!open){
            paint.setTextSize(18);
            c.drawText(score+" / "+data().target,xx,(CEILING+FLOOR)*.5f+80,paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }
    private void drawSphere(Canvas c){
        float yy=gravity==LevelData.UP?CEILING:FLOOR;
        paint.setColor(0x68051120);
        c.drawOval(new RectF(x-26,yy-6,x+26,yy+6),paint);
        int bright=color==LevelData.NONE?0xffeefbff:vivid(color);
        int mid=color==LevelData.NONE?0xff9db5c0:bright;
        paint.setShader(new RadialGradient(x-10,y-12,R*2.15f,
                new int[]{0xffffffff,0xfff1fbff,mid,shadow(color)},
                new float[]{0,.19f,.45f,1},Shader.TileMode.CLAMP));
        c.drawCircle(x,y,R,paint);paint.setShader(null);
        c.save();c.rotate(x*180/(float)Math.PI/R,x,y);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3.3f);
        paint.setColor(0x88ffffff);
        c.drawArc(new RectF(x-16,y-16,x+16,y+16),31,140,false,paint);
        paint.setColor(0x66405667);
        c.drawArc(new RectF(x-11,y-11,x+11,y+11),218,112,false,paint);
        paint.setStyle(Paint.Style.FILL);c.restore();
        paint.setColor(0xeaffffff);c.drawCircle(x-10,y-12,4.5f,paint);
    }

    private void drawHud(Canvas c,float ww,float hh) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        paint.setColor(0xfff1fcff);paint.setTextSize(23);
        c.drawText(String.format(Locale.GERMANY,"LEVEL %02d / 10",level+1),26,37,paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(score>=data().target?0xff77ffcd:0xffffd787);
        paint.setTextSize(23);
        c.drawText("PUNKTE  "+score+" / "+data().target,ww-26,37,paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(0xffbddee4);paint.setTextSize(16);
        String tip=data().name+"  ·  "+data().tip;
        float max=ww-52;
        while(tip.length()>8 && paint.measureText(tip)>max)tip=tip.substring(0,tip.length()-2);
        c.drawText(tip,26,64,paint);

        // Functional route progress indicator for the larger scrolling levels.
        float a=30,b=ww-30,progress=clamp(x/data().finish,0,1);
        paint.setColor(0x994d6f80);
        c.drawRoundRect(new RectF(a,80,b,87),4,4,paint);
        paint.setColor(0xff8ee3d7);
        c.drawRoundRect(new RectF(a,80,a+(b-a)*progress,87),4,4,paint);
        for(LevelData.Branch branch:data().branches) {
            float px=a+(b-a)*branch.x/data().finish;
            paint.setColor(0xffffd681);
            c.drawCircle(px,83.5f,5,paint);
        }
        paint.setColor(0xffffffff);
        c.drawCircle(a+(b-a)*progress,83.5f,8,paint);
        if(toastTime>0||titleTime>0){
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(24);
            paint.setColor(0xffdbf9ed);
            c.drawText(toastTime>0?toast:data().name,ww/2,122,paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
        paint.setTextSize(15);paint.setColor(0xffabcad2);
        c.drawText("FARBE: "+colorName(color)+"    GRAVITATION: "+(gravity==LevelData.UP?"↑":"↓"),27,hh-20,paint);
        int best=prefs.getInt("best_003_"+level,0);
        paint.setTextAlign(Paint.Align.RIGHT);
        c.drawText("BESTWERT "+best+"     ← + → 2,5 s: Neustart",ww-26,hh-20,paint);
        paint.setTextAlign(Paint.Align.LEFT);
        button(c,105,hh-106,71,1,!left.isEmpty());
        button(c,273,hh-106,71,2,!right.isEmpty());
        button(c,ww-108,hh-109,78,3,!jump.isEmpty());

        if(complete){
            paint.setColor(0xe40c2936);
            c.drawRoundRect(new RectF(ww*.15f,hh*.23f,ww*.85f,hh*.69f),20,20,paint);
            paint.setColor(0xffb9ffe4);paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(32);
            c.drawText("ALLE ZEHN LEVEL GESCHAFFT",ww*.5f,hh*.41f,paint);
            paint.setColor(0xfff1ffff);paint.setTextSize(20);
            c.drawText("SPRINGEN: Neue Runde",ww*.5f,hh*.52f,paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }
    private void button(Canvas c,float xx,float yy,float r,int type,boolean pressed){
        paint.setColor(pressed?0xd269e4cc:0x99516b7b);
        c.drawCircle(xx,yy,r,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);
        paint.setColor(0xffc3e0e5);c.drawCircle(xx,yy,r,paint);
        paint.setStyle(Paint.Style.FILL);
        Path tri=new Path();float t=29;
        if(type==1){
            tri.moveTo(xx-t,yy);tri.lineTo(xx+t*.60f,yy-t);tri.lineTo(xx+t*.60f,yy+t);
        } else if(type==2){
            tri.moveTo(xx+t,yy);tri.lineTo(xx-t*.60f,yy-t);tri.lineTo(xx-t*.60f,yy+t);
        } else {
            tri.moveTo(xx,yy-t);tri.lineTo(xx-t,yy+t*.60f);tri.lineTo(xx+t,yy+t*.60f);
        }
        tri.close();paint.setColor(0xffffffff);c.drawPath(tri,paint);
    }
    private int control(float rawX,float rawY) {
        float px=rawX/scale,py=rawY/scale;
        float ww=getWidth()/scale,hh=getHeight()/scale;
        if(Math.hypot(px-105,py-(hh-106))<90)return 1;
        if(Math.hypot(px-273,py-(hh-106))<90)return 2;
        if(Math.hypot(px-(ww-108),py-(hh-109))<99)return 3;
        return 0;
    }
    private void assign(int pointer,int target) {
        boolean wasJump=!jump.isEmpty();
        left.remove(pointer);right.remove(pointer);jump.remove(pointer);
        if(target==1)left.add(pointer);
        if(target==2)right.add(pointer);
        if(target==3){
            jump.add(pointer);
            if(!wasJump){
                if(complete)beginLevel(true);
                else jumpBuffer=.17f;
            }
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        int action=e.getActionMasked(),i=e.getActionIndex();
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
            assign(e.getPointerId(i),control(e.getX(i),e.getY(i)));
        } else if(action==MotionEvent.ACTION_MOVE) {
            for(int k=0;k<e.getPointerCount();k++)
                assign(e.getPointerId(k),control(e.getX(k),e.getY(k)));
        } else if(action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_UP){
            assign(e.getPointerId(i),0);
        } else if(action==MotionEvent.ACTION_CANCEL){
            left.clear();right.clear();jump.clear();restartHold=0;
        }
        return true;
    }
}
