package de.sschermer.kugelspiel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * CI-generated top-down 2D tile maps for visual level design QA.
 * Pure build-time tool: java.desktop/JDK17; not part of the Android app.
 */
public final class LevelPreview {
    private static final int SIZE=16,MARGIN=26;
    private static final float SCALE=SIZE/(float)LevelData.TILE;
    private static int px(float v){return MARGIN+Math.round(v*SCALE);}
    private static int color(int value){
        switch(value){
            case LevelData.BLUE:return 0xff00dbff;
            case LevelData.ORANGE:return 0xffffae30;
            case LevelData.VIOLET:return 0xffce6bff;
            default:return 0xffcbd8e0;
        }
    }
    private static void draw(LevelData.Level w,int i,String dir)throws Exception{
        int ww=w.columns*SIZE+2*MARGIN,hh=w.rows*SIZE+2*MARGIN+32;
        BufferedImage im=new BufferedImage(ww,hh,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x112b39));g.fillRect(0,0,ww,hh);
        g.setColor(new Color(0x29495b));
        for(int r=0;r<w.rows;r++)for(int c=0;c<w.columns;c++){
            int xx=MARGIN+c*SIZE,yy=MARGIN+r*SIZE;
            char tile=w.tiles[r][c];
            if(tile!='#'&&tile!='X')continue;
            if(tile=='X'){
                g.setColor(new Color(0x7294a5));g.fillRect(xx,yy,SIZE,SIZE);
                g.setColor(new Color(0xa9c6d2));g.drawRect(xx,yy,SIZE-1,SIZE-1);
            }else {
                boolean topCeiling=r==1;
                int top=topCeiling? yy+SIZE-Math.round(LevelData.DIVIDER_THICKNESS*SCALE):yy;
                g.setColor(new Color(0x638699));
                g.fillRect(xx,top,SIZE,Math.max(1,Math.round(LevelData.DIVIDER_THICKNESS*SCALE)));
            }
        }
        for(LevelData.Shaft s:w.shafts){
            int left=px(s.x1),right=px(s.x2),y=px(s.top);
            g.setStroke(new BasicStroke(2.2f));
            g.setColor(new Color(s.safe?0xff4eeccf:0xffff5260));
            g.drawLine(left,y-2,left,y+8);
            g.drawLine(right,y-2,right,y+8);
            if(!s.safe){
                g.drawLine(left,y+1,right,y+1);
                for(int x=left;x<right;x+=7)g.drawLine(x,y+1,x+3,y+5);
            }
        }
        for(LevelData.Gate d:w.gates){
            g.setColor(new Color(color(d.color)));
            g.setStroke(new BasicStroke(4f));
            g.drawLine(px(d.x),px(d.top),px(d.x),px(d.bottom));
        }
        for(LevelData.ScoreGate d:w.scoreGates){
            g.setColor(new Color(0xfff4cc64));
            g.setStroke(new BasicStroke(4f));
            g.drawLine(px(d.x),px(d.top),px(d.x),px(d.bottom));
            g.setFont(new Font("SansSerif",Font.BOLD,10));
            g.drawString(Integer.toString(d.threshold),px(d.x)+4,
                    (px(d.top)+px(d.bottom))/2);
        }
        for(LevelData.Item item:w.items){
            int x=px(item.x),y=px(item.y);
            int t=item.type;
            g.setColor(new Color(t==LevelData.COLOR?color(item.value):
                    t==LevelData.GRAVITY?0xffb3e69c:
                    t==LevelData.HELIUM_FILL?0xff2de6ee:
                    t==LevelData.HELIUM_DRAIN?0xffffc374:
                    0xff050709));
            g.fillOval(x-6,y-6,12,12);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif",Font.BOLD,9));
            if(t==LevelData.POINT)g.drawString(""+item.value,x-3,y+3);
            else if(t==LevelData.GRAVITY)g.drawString(item.value==LevelData.UP?"U":"D",x-3,y+3);
            else if(t==LevelData.HELIUM_FILL||t==LevelData.HELIUM_DRAIN)
                g.drawString(t==LevelData.HELIUM_FILL?"+":"-",x-3,y+3);
        }
        g.setColor(new Color(0xfffff5dc));
        g.fillOval(px(w.startX)-6,px(w.startY)-6,12,12);
        g.setColor(new Color(0xff63efb7));
        g.fillOval(px(w.exitX)-8,px(w.exitY)-8,16,16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif",Font.BOLD,15));
        g.drawString("LEVEL "+(i+1)+" / "+w.name+"  ·  ALLE "+w.collectibleCount()+
                " MARKEN ("+w.target+" PUNKTE)",MARGIN,20);
        g.dispose();
        File output=new File(dir,String.format("level_%02d.png",i+1));
        ImageIO.write(im,"png",output);
        System.out.println(output.getAbsolutePath());
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("output directory");
        File dir=new File(args[0]);if(!dir.exists()&&!dir.mkdirs())
            throw new IllegalStateException("Cannot create preview directory");
        for(int i=0;i<LevelData.LEVELS.size();i++)
            draw(LevelData.LEVELS.get(i),i,dir.getAbsolutePath());
    }
}
