package de.sschermer.kugelspiel;

public final class HalfScreenControlsTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        HalfScreenControls touch=new HalfScreenControls();
        check(!touch.press(5,HalfScreenControls.RIGHT,1000),"first touch must only move");
        check(touch.direction()==HalfScreenControls.RIGHT,"right touch moves right");
        check(touch.press(8,HalfScreenControls.LEFT,1030),"opposite second finger jumps");
        check(touch.direction()==HalfScreenControls.RIGHT,"lead side remains movement direction");
        touch.move(8,HalfScreenControls.LEFT);
        check(touch.direction()==HalfScreenControls.RIGHT,"moving second finger cannot steer");
        touch.release(8,1050);
        check(touch.press(9,HalfScreenControls.LEFT,1080),"repeated opposite touch jumps");
        touch.release(5,1100);
        check(touch.direction()==HalfScreenControls.LEFT,"remaining finger takes over");
        touch.release(9,1110);
        check(touch.direction()==0,"release stops input");
        check(touch.press(12,HalfScreenControls.LEFT,1300),"quick retap jumps");
        touch.release(12,1310);
        check(!touch.press(13,HalfScreenControls.LEFT,1610),"slow retap must not jump");
        touch.move(13,HalfScreenControls.RIGHT);
        check(touch.direction()==HalfScreenControls.RIGHT,"drag changes direction");
        touch.reset();
        check(touch.direction()==0,"reset clears pointers");
        check(!touch.press(14,HalfScreenControls.RIGHT,1650),"reset clears retap history");
        check(!touch.press(15,HalfScreenControls.RIGHT,1660),"same-side extra touch is not jump");
        System.out.println("HalfScreenControlsTest OK");
    }
}
