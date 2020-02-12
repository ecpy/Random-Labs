package tech.silvermind.demo.retargeter;

import java.util.ArrayList;

/**
 * Created by edward on 2/26/16.
 */
public class MeshDomain_old {
    // meshdomain is used for saving boolean information on domain
    private int[][] pointFaceMap;
    private boolean[] faceSelected;
    private float[][] vertices;
    private int[][] faces;

    public MeshDomain_old(int width, int height){
        pointFaceMap = new int[height][width];
    }
    private void setFaceVertex(float[][] vertices, int[][] faces) {
        this.vertices = vertices;
        this.faces = faces;
        preparePointFaceMap();
    }

    private void preparePointFaceMap() {
        for(int f = 0; f<faces.length; f++){
            float[] v1 = {vertices[faces[f][0]][0], vertices[faces[f][0]][1]};
            float[] v2 = {vertices[faces[f][1]][0], vertices[faces[f][1]][1]};
            float[] v3 = {vertices[faces[f][2]][0], vertices[faces[f][2]][1]};

            // find upper, middle, lower vertex
            float[] upperYVertex = new float[2];
            float[] middleYVertex = new float[2];
            float[] bottomYVertex = new float[2];

            if (v1[1] >= v2[1] && v1[1] >= v3[1]){
                upperYVertex = v1;
                if (v2[1] >= v3[2]){
                    middleYVertex = v2;
                    bottomYVertex = v3;
                }else{
                    middleYVertex = v3;
                    bottomYVertex = v2;
                }
            }if (v2[1] >= v3[1] && v2[1] >= v1[1]){
                upperYVertex = v2;
                if (v1[1] >= v3[2]){
                    middleYVertex = v1;
                    bottomYVertex = v3;
                }else{
                    middleYVertex = v3;
                    bottomYVertex = v1;
                }
            }if (v3[1] >= v1[1] && v3[1] >= v2[1]){
                upperYVertex = v3;
                if (v2[1] >= v1[2]){
                    middleYVertex = v2;
                    bottomYVertex = v1;
                }else{
                    middleYVertex = v1;
                    bottomYVertex = v2;
                }
            }
            ArrayList<int[]> verticesInFace = new ArrayList<>();

            // assume triangle vertices are not collinear
            if (Math.round(bottomYVertex[1]) == Math.round(middleYVertex[1])){
                for (int y = Math.round(middleYVertex[1]) + 1; y <= Math.round(upperYVertex[1]); y++){
                    int edgeX1 = Math.round((y-bottomYVertex[1])*(upperYVertex[0]-bottomYVertex[0])/(upperYVertex[1]-bottomYVertex[1])+bottomYVertex[0]);
                    int edgeX2 = Math.round((y-middleYVertex[1])*(upperYVertex[0]-middleYVertex[0])/(upperYVertex[1]-middleYVertex[1])+middleYVertex[0]);
                    if (edgeX1 >= edgeX2){
                        for(int x = edgeX2; x<edgeX1; x++){
                            verticesInFace.add(new int[]{x, y});
                        }
                    }else{
                        for(int x = edgeX1; x<edgeX2; x++){
                            verticesInFace.add(new int[]{x, y});
                        }
                    }
                }
            }else if (Math.round(upperYVertex[1]) == Math.round(middleYVertex[1])){
                for (int y = Math.round(bottomYVertex[1]) + 1; y <= Math.round(middleYVertex[1]); y++){
                    int edgeX1 = Math.round((y-bottomYVertex[1])*(upperYVertex[0]-bottomYVertex[0])/(upperYVertex[1]-bottomYVertex[1])+bottomYVertex[0]);
                    int edgeX2 = Math.round((y-bottomYVertex[1])*(middleYVertex[0]-bottomYVertex[0])/(middleYVertex[1]-bottomYVertex[1])+bottomYVertex[0]);
                    if (edgeX1 >= edgeX2){
                        for(int x = edgeX2; x<edgeX1; x++){
                            verticesInFace.add(new int[]{x, y});
                        }
                    }else{
                        for(int x = edgeX1; x<edgeX2; x++){
                            verticesInFace.add(new int[]{x, y});
                        }
                    }
                }
            }else{
                for (int y = Math.round(bottomYVertex[1]) + 1; y <= Math.round(middleYVertex[1]); y++){
                    int edgeX1 = Math.round((y-bottomYVertex[1])*(upperYVertex[0]-bottomYVertex[0])/(upperYVertex[1]-bottomYVertex[1])+bottomYVertex[0]);
                    int edgeX2 = Math.round((y-bottomYVertex[1])*(middleYVertex[0]-bottomYVertex[0])/(middleYVertex[1]-bottomYVertex[1])+bottomYVertex[0]);
                    if (edgeX1 >= edgeX2){
                        for(int x = edgeX2; x<edgeX1; x++){
                            verticesInFace.add(new int[]{x, y});
                        }
                    }else{
                        for(int x = edgeX1; x<edgeX2; x++){
                            verticesInFace.add(new int[]{x, y});
                        }
                    }

                } for (int y = Math.round(middleYVertex[1]) + 1;y <= Math.round(upperYVertex[1]); y++){
                    int edgeX1 = Math.round((y-bottomYVertex[1])*(upperYVertex[0]-bottomYVertex[0])/(upperYVertex[1]-bottomYVertex[1])+bottomYVertex[0]);
                    int edgeX2 = Math.round((y-middleYVertex[1])*(upperYVertex[0]-middleYVertex[0])/(upperYVertex[1]-middleYVertex[1])+middleYVertex[0]);
                    if (edgeX1 >= edgeX2){
                        for(int x = edgeX2; x<edgeX1; x++){
                            verticesInFace.add(new int[]{x, y});
                        }
                    }else{
                        for(int x = edgeX1; x<edgeX2; x++){
                            verticesInFace.add(new int[]{x, y});
                        }
                    }
                }
            }

            for(int[] XY : verticesInFace){
                int[] currentVertex={XY[0], XY[1]};
                // check valid point
                if (currentVertex[0] > pointFaceMap[0].length || currentVertex[0] < 0 || currentVertex[1] > pointFaceMap.length || currentVertex[1] < 0) {
                    continue;
                }
                pointFaceMap[currentVertex[1]][currentVertex[0]] = f;
            }
        }
    }

    /*
    public void updateDomain(int domainTopLeftX, int domainTopLeftY){
        //domainTopLeft = new int[]{domainTopLeftX, domainTopLeftY};
    }
    */

    public void selectFaces(int x, int y){
        faceSelected[pointFaceMap[y][x]] = true;
    }
    public void unSelectFaces(int x, int y){
        faceSelected[pointFaceMap[y][x]] = false;
    }
}