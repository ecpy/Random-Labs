package tech.silvermind.demo.retargeter;

/**
 * Created by edward on 2/26/16.
 */
public class Rect2DMap {
    private int[] domainLeftTop;
    private int domainWidth;
    private int domainHeight;
    private int[][][] full2DMap;

    public Rect2DMap(int domainWidth, int domainHeight, int domainTopLeftX, int domainTopLeftY, boolean identityMapInitialization){
        this.domainHeight = domainHeight;
        this.domainWidth = domainWidth;
        domainLeftTop = new int[]{domainTopLeftX, domainTopLeftY};
        /*
        values_X = new int[domainHeight][domainWidth];
        values_Y = new int[domainHeight][domainWidth];
        */

        full2DMap = new int[domainHeight][domainWidth][2];
        if (identityMapInitialization) {
            init();
        }
    }

    public int getDomainWidth(){
        return domainWidth;
    }
    public int getDomainHeight(){
        return domainHeight;
    }
    public int[] getDomainLeftTop(){
        return domainLeftTop;
    }
    public int[][][] getFull2DMapStartFromZero(){
        return full2DMap;
    }

    private void init() {
        for (int x = 0; x < domainWidth; x++){
            for (int y = 0; y < domainHeight; y++){
                /*
                values_X[y][x] = x;
                values_Y[y][x] = y;
                */

                full2DMap[y][x][0] = x;
                full2DMap[y][x][1] = y;
            }
        }


    }

    public int[] getValues(int domainX, int domainY){
        domainX -= domainLeftTop[0];
        domainY -= domainLeftTop[1];
        return full2DMap[domainY][domainX];

        //return new int[]{values_X[domainY][domainX], values_Y[domainY][domainX]};
    }
    /*
    public void renew(int[] domainLeftTop, int[][] rangeX, int[][] rangeY){
        this.domainLeftTop = domainLeftTop;
        values_X = rangeX;
        values_Y = rangeY;
        domainWidth = rangeX[0].length;
        domainHeight = rangeX.length;
    }
    */

    public void renew(int[] domainTopLeft, int[][][] full2DMap){
        this.domainLeftTop = domainTopLeft;
        domainWidth = full2DMap[0].length;
        domainHeight = full2DMap.length;
        this.full2DMap = full2DMap;
    }

    public void setValues(int domainX, int domainY, int rangeX, int rangeY){
        domainX -= domainLeftTop[0];
        domainY -= domainLeftTop[1];
        if(domainX < 0 || domainX >= full2DMap[0].length || domainY < 0 || domainY >= full2DMap.length){
            return;
        }
        full2DMap[domainY][domainX][0] = rangeX;
        full2DMap[domainY][domainX][1] = rangeY;

        //values_X[domainY][domainX] = rangeX;
        //values_Y[domainY][domainX] = rangeY;
    }

}
