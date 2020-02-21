package tech.silvermind.demo.retargeter;

import java.util.ArrayList;
import java.util.Arrays;

public class MeshDomain {
    // meshdomain is used for saving boolean information on domain
    private int[][] pointFaceMap;
    private boolean[] faceSelected;
    private int[][] vertices;
    private int[][] faces;

    public MeshDomain(int width, int height){
        pointFaceMap = new int[height][width];
        for (int i = 0; i<pointFaceMap.length ; i++)
            Arrays.fill(pointFaceMap[i], -1);
    }
    public void setFaceVertex(int[][] vertices, int[][] faces) {
        this.vertices = vertices;
        this.faces = faces;
        faceSelected = new boolean[faces.length];
        preparePointFaceMap();
    }
    private void addFaceToMap(int edgeX1, int edgeX2, int y, int f){
        int max = Math.max(edgeX1, edgeX2);
        int min = Math.min(edgeX1, edgeX2);
        if (y >= pointFaceMap.length || y < 0) return;
        for(int x = min; x<= max; x++){
            // check valid point
            if (x >= pointFaceMap[0].length || x < 0) continue;
            pointFaceMap[y][x] = f;
        }
    }

    private void preparePointFaceMap() {
        for(int f = 0; f<faces.length; f++){
            int[] v1 = {vertices[faces[f][0]][0], vertices[faces[f][0]][1]};
            int[] v2 = {vertices[faces[f][1]][0], vertices[faces[f][1]][1]};
            int[] v3 = {vertices[faces[f][2]][0], vertices[faces[f][2]][1]};

            // find upper, middle, lower vertex
            int[] upperYVertex = new int[2];
            int[] middleYVertex = new int[2];
            int[] bottomYVertex = new int[2];

            if (v1[1] >= v2[1] && v1[1] >= v3[1]){
                upperYVertex = v1;
                if (v2[1] >= v3[1]){
                    middleYVertex = v2;
                    bottomYVertex = v3;
                }else{
                    middleYVertex = v3;
                    bottomYVertex = v2;
                }
            }if (v2[1] >= v3[1] && v2[1] >= v1[1]){
                upperYVertex = v2;
                if (v1[1] >= v3[1]){
                    middleYVertex = v1;
                    bottomYVertex = v3;
                }else{
                    middleYVertex = v3;
                    bottomYVertex = v1;
                }
            }if (v3[1] >= v1[1] && v3[1] >= v2[1]){
                upperYVertex = v3;
                if (v2[1] >= v1[1]){
                    middleYVertex = v2;
                    bottomYVertex = v1;
                }else{
                    middleYVertex = v1;
                    bottomYVertex = v2;
                }
            }
            // assume triangle vertices are not collinear
            if (bottomYVertex[1] == middleYVertex[1]){
                for (int y = middleYVertex[1]; y <= upperYVertex[1]; y++){
                    int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                            / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    int edgeX2 = (y - middleYVertex[1]) * (upperYVertex[0] - middleYVertex[0])
                            / (upperYVertex[1] - middleYVertex[1]) + middleYVertex[0];
                    addFaceToMap(edgeX1, edgeX2, y, f);
                }
            }else if (upperYVertex[1] == middleYVertex[1]){
                for (int y = bottomYVertex[1] + 1; y < middleYVertex[1]; y++){
                    int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                            / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    int edgeX2 = (y - bottomYVertex[1]) * (middleYVertex[0] - bottomYVertex[0])
                            / (middleYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    addFaceToMap(edgeX1, edgeX2, y, f);
                }
            }else{
                for (int y = bottomYVertex[1] + 1; y <= middleYVertex[1]; y++){
                    int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                            / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    int edgeX2 = (y - bottomYVertex[1]) * (middleYVertex[0] - bottomYVertex[0])
                            / (middleYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    addFaceToMap(edgeX1, edgeX2, y, f);
                } for (int y = middleYVertex[1] + 1;y <= upperYVertex[1]; y++){
                    int edgeX1 = (y - bottomYVertex[1]) * (upperYVertex[0] - bottomYVertex[0])
                            / (upperYVertex[1] - bottomYVertex[1]) + bottomYVertex[0];
                    int edgeX2 = (y - middleYVertex[1]) * (upperYVertex[0] - middleYVertex[0])
                            / (upperYVertex[1] - middleYVertex[1]) + middleYVertex[0];
                    addFaceToMap(edgeX1, edgeX2, y, f);
                }
            }
        }
    }
    public boolean[] getFaceSelectedBoolean(){
        return faceSelected;
    }
    public boolean isSelected(int x, int y){
        if (x < 0 || x >= pointFaceMap[0].length || y < 0 || y >= pointFaceMap.length) return false;
        int f = pointFaceMap[y][x];
        if (faceSelected == null || f < 0) return false;
        return faceSelected[f];
    }
    public boolean isSelected(int f){
        return faceSelected[f];
    }
    // return centre face
    public int selectFaces(int x, int y){
        if (x < 0 || x >= pointFaceMap[0].length || y < 0 || y >= pointFaceMap.length) return -1;
        int f = pointFaceMap[y][x];
        if (f < 0) return -1;
        if (faceSelected == null) return -2;
        faceSelected[f] = true;
        return f;
    }

    @Deprecated
    public void selectFaces(int[] selectedVertex){
        for (int v: selectedVertex) {
            if (vertices[v][0] < 0 || vertices[v][0] >= pointFaceMap[0].length || vertices[v][1] < 0 || vertices[v][1] >= pointFaceMap.length) continue;
            int f = pointFaceMap[vertices[v][1]][vertices[v][0]];
            if (f < 0) continue;
            if (faceSelected == null) continue;
            faceSelected[f] = true;
        }
    }

    public void selectFaces(ArrayList<Integer> pointIndexSet){
        for (Integer v: pointIndexSet) {
            if (vertices[v][0] < 0 || vertices[v][0] >= pointFaceMap[0].length || vertices[v][1] < 0 || vertices[v][1] >= pointFaceMap.length) continue;
            int f = pointFaceMap[vertices[v][1]][vertices[v][0]];
            if (f < 0) continue;
            if (faceSelected == null) continue;
            faceSelected[f] = true;
        }
    }

    // return centre face
    public int unSelectFaces(int x, int y){
        if (x < 0 || x >= pointFaceMap[0].length || y < 0 || y >= pointFaceMap.length) return -1;
        int f = pointFaceMap[y][x];
        if (f < 0) return -1;
        if (faceSelected == null) return -2;
        faceSelected[f] = false;
        return f;
    }
    public void unSelectAllFaces(){
        faceSelected = new boolean[faceSelected.length];
    }
}