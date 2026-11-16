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
    private static final float JUMP=490f, SPEED=532f, STEP=1f/120f, HOP=157f;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final BounceChain bounce=new BounceChain();
    private final HashSet<Integer> left=new HashSet<>(),right=new HashSet<>(),jump=new HashSet<>();
    private final HalfScreenControls halfControls=new HalfScreenControls();
    private final android.content.SharedPreferences prefs;
    private boolean touchHalves=false,menuOpen=false;
    private float x,y,vx,vy,camX,camY,scale=1,lastDt=0;
    private float twoFingerTakeoffX=0f;
    private float jumpBuffer,coyote,tipTime,messageTime,exitWarning,finishDelay,resetHold;
    private float checkpointX,checkpointY;
    private int checkpointColor,checkpointGravity,checkpointScore;
    private boolean checkpointHelium=false;
    private boolean helium=false;
    private int activeStation=-1;
    private float charge=0;
    private int color=LevelData.NONE,gravity=LevelData.DOWN,score=0,level=0;
    private boolean surface=true,paused=false,complete=false,hasCheckpoint=false;
    private boolean[] collected=new boolean[0],snapshot=new boolean[0];
    private String message="";
    long last=System.nanoTime();

    GameView(Context context) {
        super(context);
        prefs=context.getSharedPreferences("kugelspiel_progress",Context.MODE_PRIVATE);
        touchHalves=prefs.getBoolean("touch_halves",false);
        level=Math.max(0,Math.min(9,prefs.getInt("level_008",0)));
        begin(false);
        setFocusable(true);
    }
    private LevelData.Level world(){return LevelData.LEVELS.get(level);}
    void setPaused(boolean pause) {
        paused=pause;last=System.nanoTime();
        if(pause){clearControls();resetHold=0;}
    }
    private void begin(boolean first) {
        if(first)level=0;
        LevelData.Level w=world();
        x=w.startX;y=w.startY;vx=vy=0;gravity=LevelData.DOWN;color=LevelData.NONE;
        score=0;hasCheckpoint=false;checkpointScore=0;helium=false;checkpointHelium=false;
        activeStation=-1;charge=0;bounce.reset();
        checkpointX=x;checkpointY=y;checkpointColor=color;checkpointGravity=gravity;
        collected=new boolean[w.items.size()];snapshot=collected.clone();
        surface=true;complete=false;finishDelay=0;resetHold=0;jumpBuffer=0;coyote=.14f;
        tipTime=4.5f;messageTime=0;exitWarning=0;
        camX=0;camY=0;
        clearControls();
        prefs.edit().putInt("level_008",level).apply();
    }
    private void msg(String t) {message=t;messageTime=1.65f;}
    /** Completion depends on the individual token flags, not on a lower score target. */
    private int missingTokens() {
        int missing=0;
        for(int i=0;i<world().items.size();i++){
            if(world().items.get(i).type==LevelData.POINT && !collected[i])missing++;
        }
        return missing;
    }
    private boolean allTokensCollected(){return missingTokens()==0;}
    private void checkpoint(float px,float py) {
        if(hasCheckpoint && Math.abs(checkpointX-px)<2)return;
        hasCheckpoint=true;
        checkpointX=px;checkpointY=py;
        checkpointColor=color;checkpointGravity=gravity;checkpointScore=score;
        checkpointHelium=helium;
        snapshot=collected.clone();
        msg("Checkpoint gespeichert");
    }
    private void death() {
        x=checkpointX;y=checkpointY;vx=vy=0;
        gravity=checkpointGravity;color=checkpointColor;score=checkpointScore;helium=checkpointHelium;
        activeStation=-1;charge=0;bounce.reset();
        collected=snapshot.clone();surface=true;jumpBuffer=0;coyote=.14f;
        msg(hasCheckpoint?"Zurück zum Checkpoint":"Zurück zum Start");
    }
    private void next() {
        int old=prefs.getInt("best_008_"+level,0);
        if(score>old)prefs.edit().putInt("best_008_"+level,score).apply();
        if(level==9){complete=true;finishDelay=0;msg("Alle Labyrinthe geschafft!");}
        else{level++;begin(false);}
    }
    private static float clamp(float n,float low,float high){return Math.max(low,Math.min(high,n));}
    private boolean touching(float ax,float ay,float bx,float by,float rad){
        return (ax-bx)*(ax-bx)+(ay-by)*(ay-by)<rad*rad;
    }

    /** Structural floors/ceilings are only 19 units thick (was 58). */
    private float tileTop(int col,int row) {
        if(row<0 || col<2 || col>=world().columns-2) return row*TILE;
        if(world().tiles[row][col]=='X')return row*TILE;
        if(row==1)return (row+1)*TILE-LevelData.DIVIDER_THICKNESS; // topmost ceiling
        return row*TILE;                    // floor or intermediate step
    }
    private float tileBottom(int col,int row) {
        if(row<0 || col<2 || col>=world().columns-2)return (row+1)*TILE;
        if(world().tiles[row][col]=='X')return (row+1)*TILE;
        if(row==1)return (row+1)*TILE;
        return row*TILE+LevelData.DIVIDER_THICKNESS;
    }
    private void moveHorizontal(float d) {
        if(Math.abs(d)<.00001f)return;
        x+=d;
        int leftCol=(int)Math.floor((x-R)/TILE),rightCol=(int)Math.floor((x+R)/TILE);
        int upRow=(int)Math.floor((y-R)/TILE),lowRow=(int)Math.floor((y+R)/TILE);
        for(int row=upRow;row<=lowRow;row++)for(int col=leftCol;col<=rightCol;col++){
            if(!world().solid(col,row))continue;
            float a=col*TILE,b=(col+1)*TILE;
            if(x+R>a && x-R<b && y+R>tileTop(col,row) && y-R<tileBottom(col,row)){
                if(d>0)x=a-R;else x=b+R;
                vx=0;
            }
        }
        for(LevelData.Gate gate:world().gates) {
            if(gate.color==color)continue;
            if(y+R<=gate.top||y-R>=gate.bottom)continue;
            float a=gate.x-12,b=gate.x+12;
            if(x+R>a&&x-R<b){x=d>0?a-R:b+R;vx=0;}
        }
        for(LevelData.ScoreGate gate:world().scoreGates){
            if(!gate.blocks(score))continue;
            if(y+R<=gate.top||y-R>=gate.bottom)continue;
            float a=gate.x-13,b=gate.x+13;
            if(x+R>a&&x-R<b) {
                x=d>0?a-R:b+R;vx=0;
                if(exitWarning<=0){
                    msg("Zwischentür: Noch "+(gate.threshold-score)+" Punkte");
                    exitWarning=2;
                }
            }
        }
    }
    private void moveVertical(float d) {
        boolean wasSurface=surface;
        float impactSpeed=Math.abs(vy);
        surface=false;
        y+=d;
        int leftCol=(int)Math.floor((x-R)/TILE),rightCol=(int)Math.floor((x+R)/TILE);
        int upRow=(int)Math.floor((y-R)/TILE),lowRow=(int)Math.floor((y+R)/TILE);
        boolean onLanding=false,hitHead=false;
        for(int row=upRow;row<=lowRow;row++)for(int col=leftCol;col<=rightCol;col++){
            if(!world().solid(col,row))continue;
            float top=tileTop(col,row),bottom=tileBottom(col,row);
            float a=col*TILE,b=(col+1)*TILE;
            if(x+R<=a||x-R>=b||y+R<=top||y-R>=bottom)continue;
            if(d>=0){
                y=top-R;
                if(gravity==LevelData.DOWN && !helium){surface=true;onLanding=true;}
                else hitHead=true;
            } else {
                y=bottom+R;
                if(gravity==LevelData.UP||helium){surface=true;onLanding=true;}
                else hitHead=true;
            }
            vy=0;
        }
        if(hitHead && !helium){bounce.ceilingContact();} // any head hit -> 100%
        if(onLanding&&!wasSurface&&!helium&&impactSpeed>70){
            if(bounce.landed()){
                // Automatic small intermediate hop after a manual jump.
                vy=-gravity*HOP;surface=false;coyote=0;
            }
        }
    }

    private void tick(float dt) {
        if(paused||complete||menuOpen)return;
        if(finishDelay>0){finishDelay-=dt;if(finishDelay<=0)next();return;}
        tipTime=Math.max(0,tipTime-dt);
        messageTime=Math.max(0,messageTime-dt);
        exitWarning=Math.max(0,exitWarning-dt);
        if(!touchHalves&&!left.isEmpty()&&!right.isEmpty()){
            resetHold+=dt;
            if(resetHold>=2.5f){begin(false);msg("Level neu gestartet");return;}
        }else resetHold=0;

        long inputTime=android.os.SystemClock.uptimeMillis();
        if(touchHalves)syncHalfMovement(inputTime);
        int command=(right.isEmpty()?0:1)-(left.isEmpty()?0:1);
        // Share one monotonic timestamp for steering and the 200ms input gate.
        // A second finger added during running never cancels momentum.
        vx=HorizontalMotion.step(vx,command,surface,
                touchHalves&&halfControls.blocksHorizontal(inputTime),dt,SPEED);
        bounce.update(dt);
        jumpBuffer=Math.max(0,jumpBuffer-dt);
        if(surface&&!helium)coyote=.15f; else coyote=Math.max(0,coyote-dt);
        if(jumpBuffer>0&&!helium) {
            float scale=bounce.jump(surface||coyote>0);
            if(scale>0) {
                vy=-gravity*JUMP*scale; // height is proportional to velocity squared
                surface=false;coyote=0;jumpBuffer=0;
                msg("Sprunghöhe: "+Math.round(bounce.currentHeight()*100)+" %");
            }
        }
        moveHorizontal(vx*dt);
        if(helium)vy=clamp(vy-380f*dt,-170,220);
        else vy=clamp(vy+gravity*G*dt,-900,900);
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
            }else if(item.type==LevelData.GRAVITY&&!helium&&gravity!=item.value) {
                gravity=item.value;surface=false;coyote=0;vy=0;bounce.reset();
                msg(gravity==LevelData.UP?"Schwerkraft nach oben":"Schwerkraft nach unten");
            }
        }
        // Sockets require intentional, near-motionless placement, not collision.
        int station=-1;
        for(int i=0;i<world().items.size();i++){
            LevelData.Item item=world().items.get(i);
            if(item.type!=LevelData.HELIUM_FILL&&item.type!=LevelData.HELIUM_DRAIN)continue;
            boolean modeFits=item.type==LevelData.HELIUM_FILL?!helium:helium;
            if(item.type==LevelData.HELIUM_FILL && score<item.value)modeFits=false;
            if(modeFits && Math.abs(x-item.x)<10 && Math.abs(y-item.y)<19
                    && Math.abs(vx)<27 && Math.abs(vy)<30) {station=i;break;}
        }
        if(station<0){activeStation=-1;charge=0;}
        else{
            if(station!=activeStation){activeStation=station;charge=0;}
            charge+=dt;
            if(charge>=.55f){
                LevelData.Item item=world().items.get(station);
                helium=item.type==LevelData.HELIUM_FILL;
                gravity=helium?LevelData.UP:LevelData.DOWN;
                bounce.reset();surface=false;coyote=0;vy=0;
                charge=0;activeStation=-1;
                msg(helium?"Helium eingefüllt – jetzt aufsteigen":"Helium abgepumpt");
            }
        }
        for(float[] cp:world().checkpoints) {
            if(touching(x,y,cp[0],cp[1],R+36) && (!hasCheckpoint||Math.abs(checkpointX-cp[0])>2)){
                checkpoint(cp[0],cp[1]);
            }
        }
        float ex=world().exitX,ey=world().exitY;
        if(Math.abs(x-ex)<35 && Math.abs(y-ey)<115) {
            if(allTokensCollected()){finishDelay=.8f;vx=vy=0;msg("Alle Marken gesammelt!");}
            else if(exitWarning<=0) {
                msg("Noch "+missingTokens()+" Marken sammeln");
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
        for(LevelData.ScoreGate gate:world().scoreGates)drawScoreGate(c,gate);
        for(int i=0;i<world().items.size();i++)drawItem(c,world().items.get(i),collected[i]);
        drawCheckpoints(c);
        exit(c);
        ball(c);
        c.restore();
        headsUp(c,vw,vh);
        if(menuOpen)drawControlsMenu(c,vw,vh);
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
            if(!world().solid(col,row))continue;
            float a=col*TILE,top=tileTop(col,row),bottom=tileBottom(col,row);
            p.setShader(new LinearGradient(a,top,a,bottom,0xff91a9b7,0xff355262,Shader.TileMode.CLAMP));
            c.drawRect(a,top,a+TILE,bottom,p);p.setShader(null);
            p.setColor(0xffa5c4cc);c.drawRect(a,top,a+TILE,top+Math.min(3,bottom-top),p);
            p.setColor(0x66597889);
            c.drawRect(a+TILE-3,top,a+TILE,bottom,p);
            if(bottom-top>12){
                p.setColor(0x666e8995);
                c.drawRect(a+8,bottom-5,a+TILE-8,bottom-3,p);
            }
        }
    }
    /**
     * Mark only the actual missing divider: a turquoise 19-unit-thin portal
     * (or red lethal break), never a solid-looking block between two floors.
     */
    private void shafts(Canvas c){
        for(LevelData.Shaft shaft:world().shafts){
            float leftEdge=shaft.x1,rightEdge=shaft.x2;
            float top=shaft.top,bottom=shaft.bottom;
            float center=(leftEdge+rightEdge)*.5f;
            if(shaft.safe){
                p.setColor(0x554affd8);
                c.drawRect(leftEdge+3,top+1,rightEdge-3,bottom-1,p);
                p.setColor(0xff54f3d7);
                p.setStrokeWidth(5);
                // Short vertical rails end at the actual surface, not
                // at an artificial 100-unit-high decorative shaft.
                c.drawLine(leftEdge+3,top-7,leftEdge+3,bottom+6,p);
                c.drawLine(rightEdge-3,top-7,rightEdge-3,bottom+6,p);
                // All ordinary safe openings are unlabelled and bidirectional.
                // No direction arrow: either direction is valid with suitable physics.
            }else{
                p.setColor(0xff390e18);
                c.drawRect(leftEdge,top,rightEdge,bottom,p);
                p.setColor(0xffff6464);
                p.setStrokeWidth(4);
                c.drawLine(leftEdge,top,rightEdge,top,p);
                Path teeth=new Path();
                for(float x=leftEdge;x<rightEdge;x+=16){
                    teeth.moveTo(x,top+3);
                    teeth.lineTo(x+8,top+18);
                    teeth.lineTo(Math.min(rightEdge,x+16),top+3);
                }
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2.5f);
                c.drawPath(teeth,p);
                p.setStyle(Paint.Style.FILL);
                // Red hazard teeth convey death; no opening text labels.
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
    private void drawScoreGate(Canvas c,LevelData.ScoreGate g){
        boolean unlocked=!g.blocks(score);
        p.setColor(unlocked?0x334affbf:0xe6d57e52);
        c.drawRoundRect(new RectF(g.x-13,g.top,g.x+13,g.bottom),6,6,p);
        p.setColor(unlocked?0xff63f0bd:0xffffd38f);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(unlocked?2:4);
        c.drawRoundRect(new RectF(g.x-13,g.top,g.x+13,g.bottom),6,6,p);
        p.setStyle(Paint.Style.FILL);
        String text=unlocked?"OFFEN":"NOCH "+(g.threshold-score);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(17);p.setColor(0xfff5ffff);
        c.drawText(text,g.x,(g.top+g.bottom)*.5f,p);
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
        }else if(item.type==LevelData.HELIUM_FILL ||
                item.type==LevelData.HELIUM_DRAIN){
            boolean fill=item.type==LevelData.HELIUM_FILL;
            boolean active=fill?!helium:helium;
            int accent=fill?0xff34e8ff:0xffffce82;
            // Real U-shaped, functional machine recess.
            p.setColor(0xff102d3d);
            c.drawRoundRect(new RectF(xx-34,yy-25,xx+34,yy+25),9,9,p);
            p.setColor(accent);p.setStrokeWidth(5);p.setStyle(Paint.Style.STROKE);
            Path socket=new Path();
            if(fill){
                socket.moveTo(xx-27,yy-22);socket.lineTo(xx-27,yy+21);
                socket.quadTo(xx,yy+31,xx+27,yy+21);
                socket.lineTo(xx+27,yy-22);
            }else{
                socket.moveTo(xx-27,yy+22);socket.lineTo(xx-27,yy-21);
                socket.quadTo(xx,yy-31,xx+27,yy-21);
                socket.lineTo(xx+27,yy+22);
            }
            c.drawPath(socket,p);p.setStyle(Paint.Style.FILL);
            p.setColor(active?0xfff8ffff:0xffa5c2c8);
            p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(16);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(fill?"HE+":"HE-",xx,yy+6,p);
            if(fill && score<item.value){
                p.setColor(0xffffd19d);
                p.setTextSize(14);
                c.drawText("NOCH "+(item.value-score),xx,yy-36,p);
            }
            p.setTextAlign(Paint.Align.LEFT);
            if(activeStation>=0 && world().items.get(activeStation)==item) {
                p.setColor(accent);p.setStrokeWidth(5);
                p.setStyle(Paint.Style.STROKE);
                c.drawArc(new RectF(xx-39,yy-39,xx+39,yy+39),
                        -90,360*clamp(charge/.55f,0,1),false,p);
                p.setStyle(Paint.Style.FILL);
            }
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
        boolean open=allTokensCollected();
        p.setColor(open?0xff46e7b6:0xffea6b57);
        c.drawRoundRect(new RectF(xx-14,yy-60,xx+14,yy+62),9,9,p);
        p.setColor(0xff18313e);
        c.drawRoundRect(new RectF(xx-9,yy-52,xx+9,yy+54),5,5,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);
        p.setColor(open?0xffbcffe5:0xffffc9b2);
        c.drawCircle(xx,yy,28,p);p.setStyle(Paint.Style.FILL);
        String label=open?"OFFEN":"NOCH "+missingTokens();
        String label2=open?"":(missingTokens()==1?"MARKE":"MARKEN");
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
        if(helium) {
            p.setColor(0x554fffea);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
            c.drawCircle(x,y,R+5,p);p.setStyle(Paint.Style.FILL);
        }
        // Four tiny functional pips: baseline + up to three stacked boosts.
        for(int i=0;i<bounce.boosts;i++){
            p.setColor(0xffffe58b);
            c.drawCircle(x+(i-1)*8,y+R+7,2.8f,p);
        }
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
        p.setColor(allTokensCollected()?0xff8bffd7:0xffffe098);
        c.drawText("MARKEN "+(world().collectibleCount()-missingTokens())+
                    "/"+world().collectibleCount()+"   •   PUNKTE "+score+"/"+world().target,
                    vw-25,34,p);
        p.setTextAlign(Paint.Align.LEFT);p.setTextSize(15);p.setColor(0xffd0e5e9);
        String subtitle=world().name+"  |  "+world().tip;
        while(subtitle.length()>10&&p.measureText(subtitle)>vw-50)
            subtitle=subtitle.substring(0,subtitle.length()-2);
        c.drawText(subtitle,24,58,p);
        p.setColor(helium?0xff8effe1:0xffffe6aa);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.RIGHT);p.setTextSize(16);
        c.drawText(helium?"HELIUM ↑":"SPRUNG "+Math.round(bounce.currentHeight()*100)+" %",vw-26,59,p);
        p.setTextAlign(Paint.Align.LEFT);
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
        if(!touchHalves){
            button(c,105,vh-104,70,1,!left.isEmpty());
            button(c,268,vh-104,70,2,!right.isEmpty());
            button(c,vw-107,vh-108,78,3,!jump.isEmpty());
        }
        p.setColor(0xb9143544);
        c.drawRoundRect(new RectF(vw*.5f-82,vh-142,vw*.5f+82,vh-70),15,15,p);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(21);p.setColor(0xffe6fffa);
        c.drawText("MENÜ",vw*.5f,vh-96,p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(15);p.setColor(0xffbdd5df);
        c.drawText("FARBE "+colorName(color)+"  |  GRAVITATION "+(gravity==LevelData.UP?"↑":"↓"),26,vh-16,p);
        p.setTextAlign(Paint.Align.RIGHT);
        c.drawText(touchHalves?"Neustart im Menü":"← + → halten: Neustart",vw-26,vh-16,p);
        p.setTextAlign(Paint.Align.LEFT);
        // Functional diagnostics for this debug APK; removed from release builds.
        if(touchHalves&&(getContext().getApplicationInfo().flags
                & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0){
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(17);p.setColor(0xffb1fff1);
            c.drawText("TOUCH-TEST: "+
                    halfControls.debugStatus(android.os.SystemClock.uptimeMillis()),
                    vw*.5f,147,p);
            p.setTextAlign(Paint.Align.LEFT);
        }
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
    private void clearControls(){
        left.clear();right.clear();jump.clear();
        halfControls.reset();
        resetHold=0;
    }
    private void requestJump(){
        if(complete)begin(true);
        else jumpBuffer=.16f;
    }
    private boolean menuHit(float rawX,float rawY){
        float xx=rawX/scale,yy=rawY/scale,vw=getWidth()/scale,vh=getHeight()/scale;
        return xx>=vw*.5f-90&&xx<=vw*.5f+90&&yy>=vh-150&&yy<=vh-60;
    }
    private void selectTouchMode(boolean enable){
        touchHalves=enable;
        prefs.edit().putBoolean("touch_halves",enable).apply();
        clearControls();
    }
    private void menuOption(Canvas c,float vw,float vh,float top,float bottom,String label,boolean selected){
        float x1=vw*.30f,x2=vw*.70f,y1=vh*top,y2=vh*bottom;
        p.setColor(selected?0xff236357:0xff284352);
        c.drawRoundRect(new RectF(x1,y1,x2,y2),13,13,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(selected?3f:1.5f);
        p.setColor(selected?0xff92ffe0:0xff678a96);
        c.drawRoundRect(new RectF(x1,y1,x2,y2),13,13,p);
        p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(22);p.setColor(0xfff2fffd);
        c.drawText(label,vw*.5f,(y1+y2)*.5f+8,p);
    }
    private void drawControlsMenu(Canvas c,float vw,float vh){
        p.setColor(0xd7081620);c.drawRect(0,0,vw,vh,p);
        p.setColor(0xff112b39);
        c.drawRoundRect(new RectF(vw*.26f,vh*.16f,vw*.74f,vh*.86f),24,24,p);
        p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(31);p.setColor(0xffd5fff4);
        c.drawText("STEUERUNG",vw*.5f,vh*.265f,p);
        p.setTypeface(Typeface.DEFAULT);p.setTextSize(17);p.setColor(0xffadcbd1);
        c.drawText("Steuerungsart auswählen",vw*.5f,vh*.315f,p);
        menuOption(c,vw,vh,.35f,.45f,"DREI TASTEN"+(touchHalves?"":"  ✓"),!touchHalves);
        menuOption(c,vw,vh,.48f,.58f,"ZWEI HÄLFTEN"+(touchHalves?"  ✓":""),touchHalves);
        menuOption(c,vw,vh,.62f,.70f,"LEVEL NEU STARTEN",false);
        menuOption(c,vw,vh,.73f,.82f,"WEITERSPIELEN",false);
    }
    private void handleMenuTap(float rawX,float rawY){
        float xx=rawX/scale,yy=rawY/scale,vw=getWidth()/scale,vh=getHeight()/scale;
        if(xx<vw*.30f||xx>vw*.70f)return;
        if(yy>=vh*.35f&&yy<=vh*.45f)selectTouchMode(false);
        else if(yy>=vh*.48f&&yy<=vh*.58f)selectTouchMode(true);
        else if(yy>=vh*.62f&&yy<=vh*.70f){menuOpen=false;begin(false);}
        else if(yy>=vh*.73f&&yy<=vh*.82f){menuOpen=false;clearControls();}
    }
    private void syncHalfMovement(long now){
        left.clear();right.clear();jump.clear();
        int direction=halfControls.direction(now);
        if(direction==HalfScreenControls.LEFT)left.add(0);
        else if(direction==HalfScreenControls.RIGHT)right.add(0);
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
            if(!previous)requestJump();
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        int action=e.getActionMasked(),idx=e.getActionIndex();
        if(menuOpen){
            if(action==MotionEvent.ACTION_UP)handleMenuTap(e.getX(idx),e.getY(idx));
            return true;
        }
        // Secondary fingers always belong to gameplay, not the menu button.
        if(action==MotionEvent.ACTION_DOWN&&menuHit(e.getX(idx),e.getY(idx))){
            menuOpen=true;clearControls();return true;
        }
        if(touchHalves){
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                int side=e.getX(idx)<getWidth()*.5f?HalfScreenControls.LEFT:HalfScreenControls.RIGHT;
                // A first finger ALWAYS begins a 200ms recognition window.
                // No physics flag (surface/vx/vy) is allowed to bypass it.
                if(action==MotionEvent.ACTION_DOWN){
                    twoFingerTakeoffX=x;
                    vx=0f;
                }
                boolean jumpNow=halfControls.press(e.getPointerId(idx),side,
                        e.getEventTime());
                if(jumpNow){
                    if(halfControls.isVerticalPair()){
                        // Keep the simultaneous start jump at its original takeoff x.
                        x=twoFingerTakeoffX;
                    }
                    // Only the initial near-simultaneous pair is stationary.
                    // Later second-finger jumps retain full horizontal velocity.
                    if(halfControls.isVerticalPair())vx=0f;
                    requestJump();
                }
            }else if(action==MotionEvent.ACTION_MOVE){
                for(int i=0;i<e.getPointerCount();i++){
                    int side=e.getX(i)<getWidth()*.5f?HalfScreenControls.LEFT:HalfScreenControls.RIGHT;
                    halfControls.move(e.getPointerId(i),side);
                }
            }else if(action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_UP){
                halfControls.release(e.getPointerId(idx),e.getEventTime());
            }else if(action==MotionEvent.ACTION_CANCEL){
                clearControls();
            }
            syncHalfMovement(e.getEventTime());
        }else{
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                assign(e.getPointerId(idx),control(e.getX(idx),e.getY(idx)));
            }else if(action==MotionEvent.ACTION_MOVE){
                for(int i=0;i<e.getPointerCount();i++)
                    assign(e.getPointerId(i),control(e.getX(i),e.getY(i)));
            }else if(action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_UP){
                assign(e.getPointerId(idx),0);
            }else if(action==MotionEvent.ACTION_CANCEL){
                clearControls();
            }
        }
        return true;
    }
}
