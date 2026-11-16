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

/** Native Canvas renderer and platform physics for layered tile mazes. */
final class GameView extends View {
    private static final float TILE=LevelData.TILE, R=15f, G=950f;
    private static final float JUMP=490f, SPEED=266f, STEP=1f/120f;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final HashSet<Integer> left=new HashSet<>(),right=new HashSet<>(),jump=new HashSet<>();
    private final android.content.SharedPreferences prefs;
    private float x,y,vx,vy,camX,camY,scale=1,lastDt=0;
    private float jumpBuffer,coyote,tipTime,messageTime,exitWarning,finishDelay,resetHold;
    private float checkpointX,checkpointY;
    private int checkpointColor,checkpointGravity,checkpointScore;
    private int color=LevelData.NONE,gravity=LevelData.DOWN,score=0,level=0;
    private boolean surface=true,paused=false,complete=false,hasCheckpoint=false;
    private boolean[] collected=new boolean[0],snapshot=new boolean[0];
    private String message="";
    long last=System.nanoTime();

    GameView(Context context) {
        super(context);
        prefs=context.getSharedPreferences("kugelspiel_progress",Context.MODE_PRIVATE);
        level=Math.max(0,Math.min(9,prefs.getInt("level_004",0)));
        begin(false);
        setFocusable(true);
    }
    private LevelData.Level world(){return LevelData.LEVELS.get(level);}
    void setPaused(boolean pause) {
        paused=pause;last=System.nanoTime();
        if(pause){left.clear();right.clear();jump.clear();resetHold=0;}
    }
    private void begin(boolean first) {
        if(first)level=0;
        LevelData.Level w=world();
        x=w.startX;y=w.startY;vx=vy=0;gravity=LevelData.DOWN;color=LevelData.NONE;
        score=0;hasCheckpoint=false;checkpointScore=0;
        checkpointX=x;checkpointY=y;checkpointColor=color;checkpointGravity=gravity;
        collected=new boolean[w.items.size()];snapshot=collected.clone();
        surface=true;complete=false;finishDelay=0;resetHold=0;jumpBuffer=0;coyote=.14f;
        tipTime=4.5f;messageTime=0;exitWarning=0;
        camX=0;camY=0;
        left.clear();right.clear();jump.clear();
        prefs.edit().putInt("level_004",level).apply();
    }
    private void msg(String t) {message=t;messageTime=1.65f;}
    private void checkpoint(float px,float py) {
        if(hasCheckpoint && Math.abs(checkpointX-px)<2)return;
        hasCheckpoint=true;
        checkpointX=px;checkpointY=py;
        checkpointColor=color;checkpointGravity=gravity;checkpointScore=score;
        snapshot=collected.clone();
        msg("Checkpoint gespeichert");
    }
    private void death() {
        x=checkpointX;y=checkpointY;vx=vy=0;
        gravity=checkpointGravity;color=checkpointColor;score=checkpointScore;
        collected=snapshot.clone();surface=true;jumpBuffer=0;coyote=.14f;
        msg(hasCheckpoint?"Zurück zum Checkpoint":"Zurück zum Start");
    }
    private void next() {
        int old=prefs.getInt("best_004_"+level,0);
        if(score>old)prefs.edit().putInt("best_004_"+level,score).apply();
        if(level==9){complete=true;finishDelay=0;msg("Alle Labyrinthe geschafft!");}
        else{level++;begin(false);}
    }
    private static float clamp(float n,float low,float high){return Math.max(low,Math.min(high,n));}
    private boolean touching(float ax,float ay,float bx,float by,float rad){
        return (ax-bx)*(ax-bx)+(ay-by)*(ay-by)<rad*rad;
    }

    private void moveHorizontal(float d) {
        if(Math.abs(d)<.0001f)return;
        x+=d;
        int leftCol=(int)Math.floor((x-R)/TILE),rightCol=(int)Math.floor((x+R)/TILE);
        int upRow=(int)Math.floor((y-R)/TILE),lowRow=(int)Math.floor((y+R)/TILE);
        for(int r=upRow;r<=lowRow;r++)for(int c=leftCol;c<=rightCol;c++) {
            if(!world().solid(c,r))continue;
            float a=c*TILE,b=(c+1)*TILE;
            if(x+R>a && x-R<b && y+R>r*TILE && y-R<(r+1)*TILE) {
                if(d>0)x=a-R;else x=b+R;
                vx=0;
            }
        }
        for(LevelData.Gate gate:world().gates) {
            if(gate.color==color)continue;
            if(y+R<=gate.top || y-R>=gate.bottom)continue;
            float a=gate.x-12,b=gate.x+12;
            if(x+R>a&&x-R<b) {
                if(d>0)x=a-R;else x=b+R;
                vx=0;
            }
        }
    }
    private void moveVertical(float d) {
        surface=false;
        y+=d;
        int leftCol=(int)Math.floor((x-R)/TILE),rightCol=(int)Math.floor((x+R)/TILE);
        int upRow=(int)Math.floor((y-R)/TILE),lowRow=(int)Math.floor((y+R)/TILE);
        for(int r=upRow;r<=lowRow;r++)for(int c=leftCol;c<=rightCol;c++) {
            if(!world().solid(c,r))continue;
            float top=r*TILE,bottom=(r+1)*TILE,leftEdge=c*TILE,rightEdge=(c+1)*TILE;
            if(x+R<=leftEdge||x-R>=rightEdge||y+R<=top||y-R>=bottom)continue;
            if(d>=0) {
                y=top-R;
                if(gravity==LevelData.DOWN)surface=true;
            } else {
                y=bottom+R;
                if(gravity==LevelData.UP)surface=true;
            }
            vy=0;
        }
    }
    private void tick(float dt) {
        if(paused||complete)return;
        if(finishDelay>0){finishDelay-=dt;if(finishDelay<=0)next();return;}
        tipTime=Math.max(0,tipTime-dt);
        messageTime=Math.max(0,messageTime-dt);
        exitWarning=Math.max(0,exitWarning-dt);
        if(!left.isEmpty()&&!right.isEmpty()){
            resetHold+=dt;
            if(resetHold>=2.5f){begin(false);msg("Level neu gestartet");return;}
        }else resetHold=0;

        int command=(right.isEmpty()?0:1)-(left.isEmpty()?0:1);
        if(command!=0)vx+=command*(surface?1320f:860f)*dt;
        else vx*=Math.max(0,1-(surface?10f:3f)*dt);
        vx=clamp(vx,-SPEED,SPEED);
        jumpBuffer=Math.max(0,jumpBuffer-dt);
        if(surface)coyote=.15f; else coyote=Math.max(0,coyote-dt);
        if(jumpBuffer>0&&coyote>0){
            vy=-gravity*JUMP;surface=false;coyote=0;jumpBuffer=0;
        }
        moveHorizontal(vx*dt);
        vy=clamp(vy+gravity*G*dt,-750,750);
        moveVertical(vy*dt);
        // Explicitly dangerous gaps are separate from turquoise traversal shafts.
        for(LevelData.Shaft shaft:world().shafts) {
            if(shaft.safe)continue;
            if(x>shaft.x1+R*.35f&&x<shaft.x2-R*.35f&&
                y+R>shaft.top+19&&y-R<shaft.bottom) {death();return;}
        }
        if(y>world().rows*TILE+80){death();return;}

        for(int i=0;i<world().items.size();i++){
            LevelData.Item item=world().items.get(i);
            float rad=item.type==LevelData.COLOR?R+27:R+25;
            if(!touching(x,y,item.x,item.y,rad))continue;
            if(item.type==LevelData.POINT) {
                if(!collected[i]){collected[i]=true;score+=item.value;msg("+"+item.value+" Punkte");}
            }else if(item.type==LevelData.COLOR&&color!=item.value) {
                color=item.value;msg("Farbe: "+colorName(color));
            }else if(item.type==LevelData.GRAVITY&&gravity!=item.value) {
                gravity=item.value;surface=false;coyote=0;vy=0;
                msg(gravity==LevelData.UP?"Schwerkraft nach oben":"Schwerkraft nach unten");
            }
        }
        for(float[] cp:world().checkpoints) {
            if(touching(x,y,cp[0],cp[1],R+36) && (!hasCheckpoint||Math.abs(checkpointX-cp[0])>2)){
                checkpoint(cp[0],cp[1]);
            }
        }
        float ex=world().exitX,ey=world().exitY;
        if(Math.abs(x-ex)<35 && Math.abs(y-ey)<115) {
            if(score>=world().target){finishDelay=.8f;vx=vy=0;msg("Ausgang geöffnet!");}
            else if(exitWarning<=0) {
                msg("Noch "+(world().target-score)+" Punkte nötig");
                exitWarning=2;
            }
        }
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if(getWidth()<2||getHeight()<2)return;
        scale=Math.min(getWidth()/1180f,getHeight()/770f);
        float vw=getWidth()/scale,vh=getHeight()/scale;
        long now=System.nanoTime();float frame=clamp((now-last)*1e-9f,0,.04f);last=now;
        for(float time=frame;time>0;){float dt=Math.min(time,STEP);tick(dt);time-=dt;}
        float maxX=Math.max(0,world().columns*TILE-vw);
        float maxY=Math.max(0,world().rows*TILE-vh);
        float wantX=clamp(x-vw*.46f,0,maxX);
        float wantY=clamp(y-vh*.43f,0,maxY);
        camX+=(wantX-camX)*Math.min(1,frame*6);
        camY+=(wantY-camY)*Math.min(1,frame*5);
        c.save();c.scale(scale,scale);
        backdrop(c,vw,vh);
        c.save();c.translate(-camX,-camY);
        shafts(c);
        blocks(c,vw,vh);
        for(LevelData.Gate gate:world().gates)drawGate(c,gate);
        for(int i=0;i<world().items.size();i++)drawItem(c,world().items.get(i),collected[i]);
        drawCheckpoints(c);
        exit(c);
        ball(c);
        c.restore();
        headsUp(c,vw,vh);
        c.restore();
        postInvalidateOnAnimation();
    }

    private void backdrop(Canvas c,float vw,float vh) {
        p.setShader(new LinearGradient(0,0,0,vh,0xff0b1c2a,0xff274555,Shader.TileMode.CLAMP));
        c.drawRect(0,0,vw,vh,p);p.setShader(null);
        p.setColor(0x266c98aa);p.setStrokeWidth(1);
        for(int i=-2;i<(int)(vw/82)+4;i++) {
            float line=((i*82-camX*.10f)%(vw+82));
            c.drawLine(line,0,line,vh,p);
        }
        for(int i=0;i<(int)(vh/82)+3;i++) {
            float line=((i*82-camY*.10f)%(vh+82));
            c.drawLine(0,line,vw,line,p);
        }
    }
    private void blocks(Canvas c,float vw,float vh) {
        int x0=Math.max(0,(int)Math.floor(camX/TILE)-2),x1=Math.min(world().columns-1,(int)Math.ceil((camX+vw)/TILE)+2);
        int y0=Math.max(0,(int)Math.floor(camY/TILE)-2),y1=Math.min(world().rows-1,(int)Math.ceil((camY+vh)/TILE)+2);
        for(int row=y0;row<=y1;row++)for(int col=x0;col<=x1;col++){
            if(world().tiles[row][col]!='#')continue;
            float a=col*TILE,b=row*TILE;
            p.setShader(new LinearGradient(a,b,a,b+TILE,0xff839ba9,0xff304b5b,Shader.TileMode.CLAMP));
            c.drawRect(a,b,a+TILE,b+TILE,p);p.setShader(null);
            p.setColor(0xff91b0ba);
            c.drawRect(a,b,a+TILE,b+3,p);
            p.setColor(0x66597889);
            c.drawRect(a+TILE-3,b,a+TILE,b+TILE,p);
            p.setColor(0x6650707b);
            c.drawRect(a+10,b+TILE-12,a+TILE-10,b+TILE-10,p);
        }
    }
    private void shafts(Canvas c){
        for(LevelData.Shaft s:world().shafts){
            float center=(s.x1+s.x2)*.5f;
            if(s.safe){
                p.setColor(0x552de2d0);
                c.drawRoundRect(new RectF(s.x1+5,s.top-13,s.x2-5,s.bottom+12),12,12,p);
                p.setColor(0xff55ffda);p.setStrokeWidth(5);
                c.drawLine(s.x1+6,s.top-5,s.x1+6,s.bottom+9,p);
                c.drawLine(s.x2-6,s.top-5,s.x2-6,s.bottom+9,p);
                p.setTextAlign(Paint.Align.CENTER);p.setTextSize(28);
                p.setTypeface(Typeface.DEFAULT_BOLD);p.setColor(0xffd9fff7);
                c.drawText("↓",center,(s.top+s.bottom)*.5f+9,p);
                p.setTextSize(13);c.drawText("DURCHGANG",center,s.top-19,p);
                p.setTextAlign(Paint.Align.LEFT);
            }else{
                p.setColor(0xe2140910);
                c.drawRect(s.x1,s.top,s.x2,s.bottom,p);
                p.setColor(0xfff2484c);p.setStrokeWidth(6);
                c.drawLine(s.x1,s.top,s.x2,s.top,p);
                // Repeated red teeth make a lethal gap unmistakable.
                Path teeth=new Path();
                for(float xx=s.x1;xx<s.x2;xx+=18){
                    teeth.moveTo(xx,s.top+6);
                    teeth.lineTo(xx+9,s.top+28);
                    teeth.lineTo(Math.min(s.x2,xx+18),s.top+6);
                }
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
                c.drawPath(teeth,p);p.setStyle(Paint.Style.FILL);
                p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);
                p.setTextSize(15);p.setColor(0xffffe3d7);
                c.drawText("GEFAHR",center,s.top-17,p);
                p.setTextAlign(Paint.Align.LEFT);
            }
        }
    }
    private static int vivid(int color){
        if(color==LevelData.BLUE)return 0xff00dbff;
        if(color==LevelData.ORANGE)return 0xffff9c00;
        if(color==LevelData.VIOLET)return 0xffd368ff;
        return 0xffd5e9f1;
    }
    private static int dark(int color){
        if(color==LevelData.BLUE)return 0xff095d9c;
        if(color==LevelData.ORANGE)return 0xff8b3c13;
        if(color==LevelData.VIOLET)return 0xff54298c;
        return 0xff293f51;
    }
    private static String colorName(int col){
        if(col==LevelData.BLUE)return "BLAU";
        if(col==LevelData.ORANGE)return "ORANGE";
        if(col==LevelData.VIOLET)return "VIOLETT";
        return "NEUTRAL";
    }
    private static String colorLetter(int col) {
        if(col==LevelData.BLUE)return "B";
        if(col==LevelData.ORANGE)return "O";
        if(col==LevelData.VIOLET)return "V";
        return "-";
    }
    private void drawGate(Canvas c,LevelData.Gate g){
        boolean open=color==g.color;
        float a=g.x-12,b=g.x+12;
        p.setShader(new LinearGradient(a,g.top,b,g.bottom,vivid(g.color),dark(g.color),Shader.TileMode.CLAMP));
        p.setAlpha(open?40:242);
        c.drawRoundRect(new RectF(a,g.top,b,g.bottom),6,6,p);
        p.setShader(null);p.setAlpha(255);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(open?2:3);
        p.setColor(open?0x9962b5ba:vivid(g.color));
        c.drawRoundRect(new RectF(a,g.top,b,g.bottom),6,6,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xffffffff);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(21);
        p.setTextAlign(Paint.Align.CENTER);
        for(float yy=g.top+33;yy<g.bottom-8;yy+=64)c.drawText(colorLetter(g.color),g.x,yy,p);
        p.setTextAlign(Paint.Align.LEFT);
    }
    private void drawItem(Canvas c,LevelData.Item item,boolean collected){
        float xx=item.x,yy=item.y;
        if(item.type==LevelData.COLOR) {
            p.setColor(0x6640f1d8);c.drawCircle(xx,yy,34,p);
            p.setColor(0xff102333);c.drawCircle(xx,yy,29,p);
            Path d=new Path();
            d.moveTo(xx,yy-27);d.lineTo(xx+26,yy);d.lineTo(xx,yy+27);d.lineTo(xx-26,yy);d.close();
            p.setColor(vivid(item.value));c.drawPath(d,p);
            p.setColor(0xffffffff);p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);c.drawPath(d,p);p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(22);p.setColor(0xff112033);
            c.drawText(colorLetter(item.value),xx,yy+8,p);
            p.setTextAlign(Paint.Align.LEFT);
        }else if(item.type==LevelData.GRAVITY){
            p.setColor(0x9966e6d3);c.drawCircle(xx,yy,30,p);
            p.setColor(0xff173c4a);c.drawRoundRect(new RectF(xx-26,yy-26,xx+26,yy+26),9,9,p);
            p.setColor(0xffb5ffe7);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
            c.drawRoundRect(new RectF(xx-26,yy-26,xx+26,yy+26),9,9,p);
            p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);
            p.setColor(0xffffffff);p.setTextSize(35);p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText(item.value==LevelData.UP?"↑":"↓",xx,yy+12,p);
            p.setTextAlign(Paint.Align.LEFT);
        } else {
            // Not collected: black with white number.
            // Collected: semi-transparent white with black number, never awarded twice.
            p.setColor(collected?0x88ffffff:0xff05070a);
            c.drawCircle(xx,yy,23,p);
            p.setColor(collected?0xff000000:0xffffffff);
            p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(item.value==10?20:22);
            c.drawText(Integer.toString(item.value),xx,yy+7.5f,p);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }
    private void drawCheckpoints(Canvas c){
        for(float[] cp:world().checkpoints){
            p.setColor(hasCheckpoint&&checkpointX>=cp[0]-2?0xff65e8c4:0xfff2d38a);
            c.drawRoundRect(new RectF(cp[0]-5,cp[1]-53,cp[0]+5,cp[1]+16),4,4,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
            c.drawCircle(cp[0],cp[1]-56,12,p);p.setStyle(Paint.Style.FILL);
        }
    }
    private void exit(Canvas c) {
        float xx=world().exitX, yy=world().exitY;
        boolean open=score>=world().target;
        p.setColor(open?0xff46e7b6:0xffea6b57);
        c.drawRoundRect(new RectF(xx-14,yy-60,xx+14,yy+62),9,9,p);
        p.setColor(0xff18313e);
        c.drawRoundRect(new RectF(xx-9,yy-52,xx+9,yy+54),5,5,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);
        p.setColor(open?0xffbcffe5:0xffffc9b2);
        c.drawCircle(xx,yy,28,p);p.setStyle(Paint.Style.FILL);
        String label=open?"OFFEN":"NOCH "+(world().target-score);
        String label2=open?"":((world().target-score)==1?"PUNKT":"PUNKTE");
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(18);
        float yLabel=yy-79;
        p.setColor(0xe0122533);
        c.drawRoundRect(new RectF(xx-80,yLabel-24,xx+80,yLabel+(open?7:25)),8,8,p);
        p.setColor(open?0xffaaffd7:0xffffd0bd);
        c.drawText(label,xx,yLabel,p);
        if(!open)c.drawText(label2,xx,yLabel+20,p);
        p.setTextAlign(Paint.Align.LEFT);
    }
    private void ball(Canvas c) {
        int bright=vivid(color),base=dark(color);
        p.setShader(new RadialGradient(x-6,y-8,R*2.1f,
                new int[]{0xffffffff,0xffe8faff,bright,base},
                new float[]{0,.27f,.49f,1},Shader.TileMode.CLAMP));
        c.drawCircle(x,y,R,p);p.setShader(null);
        c.save();c.rotate(x*180f/(float)Math.PI/R,x,y);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.6f);p.setColor(0xb1f4ffff);
        c.drawArc(new RectF(x-11,y-11,x+11,y+11),28,135,false,p);
        p.setStyle(Paint.Style.FILL);c.restore();
        p.setColor(0xffffffff);c.drawCircle(x-7,y-8,3.4f,p);
    }

    private void headsUp(Canvas c,float vw,float vh){
        p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        p.setTextAlign(Paint.Align.LEFT);p.setTextSize(22);p.setColor(0xfff1ffff);
        c.drawText(String.format(Locale.GERMANY,"LEVEL %02d / 10",level+1),24,34,p);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setColor(score>=world().target?0xff8bffd7:0xffffe098);
        c.drawText("PUNKTE "+score+" / "+world().target,vw-25,34,p);
        p.setTextAlign(Paint.Align.LEFT);p.setTextSize(15);p.setColor(0xffd0e5e9);
        String subtitle=world().name+"  |  "+world().tip;
        while(subtitle.length()>10&&p.measureText(subtitle)>vw-50)
            subtitle=subtitle.substring(0,subtitle.length()-2);
        c.drawText(subtitle,24,58,p);
        float leftMargin=28,rightMargin=vw-28;
        p.setColor(0x99567785);c.drawRoundRect(new RectF(leftMargin,76,rightMargin,83),4,4,p);
        float factor=clamp(x/(world().columns*TILE),0,1);
        p.setColor(0xff84ddcb);
        c.drawRoundRect(new RectF(leftMargin,76,leftMargin+(rightMargin-leftMargin)*factor,83),4,4,p);
        p.setColor(0xffedfef9);c.drawCircle(leftMargin+(rightMargin-leftMargin)*factor,79,7,p);
        if(messageTime>0||tipTime>0){
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(23);p.setColor(0xffd4fff3);
            c.drawText(messageTime>0?message:world().name,vw/2,116,p);
            p.setTextAlign(Paint.Align.LEFT);
        }
        button(c,105,vh-104,70,1,!left.isEmpty());
        button(c,268,vh-104,70,2,!right.isEmpty());
        button(c,vw-107,vh-108,78,3,!jump.isEmpty());
        p.setTextSize(15);p.setColor(0xffbdd5df);
        c.drawText("FARBE "+colorName(color)+"  |  GRAVITATION "+(gravity==LevelData.UP?"↑":"↓"),26,vh-16,p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("← + → halten: Neustart",vw-26,vh-16,p);
        p.setTextAlign(Paint.Align.LEFT);
        if(complete) {
            p.setColor(0xe20b2632);
            c.drawRoundRect(new RectF(vw*.15f,vh*.26f,vw*.85f,vh*.66f),20,20,p);
            p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(30);p.setColor(0xffc1ffdf);
            c.drawText("ALLE LABYRINTHE GELÖST",vw*.5f,vh*.43f,p);
            p.setTextSize(20);p.setColor(0xffffffff);
            c.drawText("SPRINGEN: Neue Runde",vw*.5f,vh*.53f,p);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }
    private void button(Canvas c,float xx,float yy,float rad,int type,boolean pressed){
        p.setColor(pressed?0xd56ee7cc:0x99516d7e);c.drawCircle(xx,yy,rad,p);
        p.setColor(0xffc5e5e8);p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3);c.drawCircle(xx,yy,rad,p);p.setStyle(Paint.Style.FILL);
        Path t=new Path();float size=29;
        if(type==1){
            t.moveTo(xx-size,yy);t.lineTo(xx+size*.65f,yy-size);t.lineTo(xx+size*.65f,yy+size);
        }else if(type==2){
            t.moveTo(xx+size,yy);t.lineTo(xx-size*.65f,yy-size);t.lineTo(xx-size*.65f,yy+size);
        }else{
            t.moveTo(xx,yy-size);t.lineTo(xx-size,yy+size*.65f);t.lineTo(xx+size,yy+size*.65f);
        }
        t.close();p.setColor(0xffffffff);c.drawPath(t,p);
    }
    private int control(float rawX,float rawY){
        float xx=rawX/scale,yy=rawY/scale,vw=getWidth()/scale,vh=getHeight()/scale;
        if(Math.hypot(xx-105,yy-(vh-104))<90)return 1;
        if(Math.hypot(xx-268,yy-(vh-104))<90)return 2;
        if(Math.hypot(xx-(vw-107),yy-(vh-108))<98)return 3;
        return 0;
    }
    private void assign(int pointer,int type){
        boolean previous=!jump.isEmpty();
        left.remove(pointer);right.remove(pointer);jump.remove(pointer);
        if(type==1)left.add(pointer);
        if(type==2)right.add(pointer);
        if(type==3){
            jump.add(pointer);
            if(!previous){
                if(complete)begin(true);
                else jumpBuffer=.16f;
            }
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        int action=e.getActionMasked(),idx=e.getActionIndex();
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
            assign(e.getPointerId(idx),control(e.getX(idx),e.getY(idx)));
        }else if(action==MotionEvent.ACTION_MOVE){
            for(int i=0;i<e.getPointerCount();i++)
                assign(e.getPointerId(i),control(e.getX(i),e.getY(i)));
        }else if(action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_UP){
            assign(e.getPointerId(idx),0);
        }else if(action==MotionEvent.ACTION_CANCEL){
            left.clear();right.clear();jump.clear();resetHold=0;
        }
        return true;
    }
}
