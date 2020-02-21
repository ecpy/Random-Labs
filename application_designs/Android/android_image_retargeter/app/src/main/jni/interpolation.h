/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   Interpolation.h
 * Author: edward
 *
 * Created on April 2, 2016, 11:32 AM
 */ 
#ifndef INTERPOLATION_H
#define INTERPOLATION_H 

#include "lbs/lbs.h"     
#include <string>
//#include <android/log.h>
#include <sstream>

#ifndef TO_STRING
#define TO_STRING
template <typename T>           
std::string to_string(T value)
{
    std::ostringstream os ;
    os << value ;
    return os.str() ;
}
#endif 
float dot(float a[2], float b[2]){
    return a[0]*b[0] + a[1]*b[1];
}  
float norm(float a[2]){
    return std::sqrt(a[0]*a[0]+a[1]*a[1]);
}
void getYVertexOrder(float* &upperYVertex, float* &middleYVertex, float* &bottomYVertex, float v1[2], float v2[2], float v3[2]){
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
}  
/*
template <class T>
void _2DLangrange(float v1[2],float v2[2],float v3[2],float vMap1[2],float vMap2[2],float vMap3[2],int y,float x_bounds[2], T* interpolation2dMapping, int maxBoundXOnValues, int maxBoundYOnValues){
    for(int x = round(std::min(x_bounds[0], x_bounds[1])); x <= round(std::max(x_bounds[0], x_bounds[1])); x++){     
        // check valid point
        if (x > maxBoundXOnValues || x < 0 || y > maxBoundYOnValues || y < 0){ 
            continue;			
        }    
        float diff_cv1[2] = {x- v1[0], y- v1[1]};
        float diff_cv2[2] = {x- v2[0], y- v2[1]};
        float diff_cv3[2] = {x- v3[0], y- v3[1]};
        
        float diff_v1v2[2] = {v1[0]- v2[0], v1[1]- v2[1]};
        float diff_v2v3[2] = {v2[0]- v3[0], v2[1]- v3[1]};
        float diff_v3v1[2] = {v3[0]- v1[0], v3[1]- v1[1]};  
        
        if (std::is_same<T, int>::value){ 
            interpolation2dMapping[2*(maxBoundXOnValues + 1)*y + 2*x] = (int) round(
                    vMap3[0]*norm(diff_cv2)*norm(diff_cv1)/norm(diff_v2v3)/norm(diff_v3v1)
                    + vMap1[0]*norm(diff_cv2)*norm(diff_cv3)/norm(diff_v1v2)/norm(diff_v3v1)
                    + vMap2[0]*norm(diff_cv1)*norm(diff_cv3)/norm(diff_v1v2)/norm(diff_v2v3));
            interpolation2dMapping[2*(maxBoundXOnValues + 1)*y + 2*x + 1] = (int) round(
                    vMap3[1]*norm(diff_cv2)*norm(diff_cv1)/norm(diff_v2v3)/norm(diff_v3v1)
                    + vMap1[1]*norm(diff_cv2)*norm(diff_cv3)/norm(diff_v1v2)/norm(diff_v3v1)
                    + vMap2[1]*norm(diff_cv1)*norm(diff_cv3)/norm(diff_v1v2)/norm(diff_v2v3));
        }else{ 
            interpolation2dMapping[2*(maxBoundXOnValues + 1)*y + 2*x] =  
                    vMap3[0]*norm(diff_cv2)*norm(diff_cv1)/norm(diff_v2v3)/norm(diff_v3v1)
                    + vMap1[0]*norm(diff_cv2)*norm(diff_cv3)/norm(diff_v1v2)/norm(diff_v3v1)
                    + vMap2[0]*norm(diff_cv1)*norm(diff_cv3)/norm(diff_v1v2)/norm(diff_v2v3);
            interpolation2dMapping[2*(maxBoundXOnValues + 1)*y + 2*x + 1] = 
                    vMap3[1]*norm(diff_cv2)*norm(diff_cv1)/norm(diff_v2v3)/norm(diff_v3v1)
                    + vMap1[1]*norm(diff_cv2)*norm(diff_cv3)/norm(diff_v1v2)/norm(diff_v3v1)
                    + vMap2[1]*norm(diff_cv1)*norm(diff_cv3)/norm(diff_v1v2)/norm(diff_v2v3);
        }
        
        //DEBUG  
         if (interpolation2dMapping[2*(maxBoundXOnValues + 1)*y + 2*x] < 0 || interpolation2dMapping[2*(maxBoundXOnValues + 1)*y + 2*x + 1] < 0 ){
         std::cout<<x<<" "<<y<<" "<<std::endl;
         std::cout<<v1[0]<<" "<<v1[1]<<" "<<std::endl;
         std::cout<<v2[0]<<" "<<v2[1]<<" "<<std::endl;
         std::cout<<v3[0]<<" "<<v3[1]<<" "<<std::endl;
         std::cout<<vMap1[0]<<" "<<vMap1[1]<<" "<<std::endl;
         std::cout<<vMap2[0]<<" "<<vMap2[1]<<" "<<std::endl;
         std::cout<<vMap3[0]<<" "<<vMap3[1]<<" "<<std::endl;  
         std::cout<<interpolation2dMapping[2*(maxBoundXOnValues + 1)*y + 2*x]<<" "
         <<interpolation2dMapping[2*(maxBoundXOnValues + 1)*y + 2*x + 1]<<" "<<std::endl;
         std::cout<<std::endl; 
         std::string line;
         std::getline(std::cin, line);   
         }; 
        //interpolation2dMapping[2*(maxBoundX + 1)*y + 2*x + 1] = y;
    }  
}
*/
struct _2DLinear_InterpolationParas{
    MatrixXf A = MatrixXf(2,2);
    VectorXf t = VectorXf(2);
};
void _2DLinear_getInterpolationParas(_2DLinear_InterpolationParas& paras, float v1[2],float v2[2],float v3[2],float vMap1[2],float vMap2[2],float vMap3[2]){ 
    MatrixXf A(6,6);
    VectorXf m(6);
    A <<
            v1[0], v1[1], 0, 0, 1, 0,
            0, 0, v1[0], v1[1], 0, 1,
            v2[0], v2[1], 0, 0, 1, 0,
            0, 0, v2[0], v2[1], 0, 1,
            v3[0], v3[1], 0, 0, 1, 0,
            0, 0, v3[0], v3[1], 0, 1;
    m(0) = vMap1[0];
    m(1) = vMap1[1];
    m(2) = vMap2[0];
    m(3) = vMap2[1];
    m(4) = vMap3[0];
    m(5) = vMap3[1];
    
    VectorXf sol = A.colPivHouseholderQr().solve(m);  
    paras.A(0,0) = sol(0);
    paras.A(0,1) = sol(1);
    paras.A(1,0) = sol(2);
    paras.A(1,1) = sol(3);
    paras.t(0) = sol(4);
    paras.t(1) = sol(5); 
    //std:: cout << sol(0) <<" "<< sol(1) <<" "<< sol(2) <<" "<< sol(3) <<" "<< sol(4) <<" "<< sol(5) << std::endl;  
}   
float sign (float p1[2], float p2[2], float p3[2])
{
    return (p1[0] - p3[0]) * (p2[1] - p3[1]) - (p2[0] - p3[0]) * (p1[1] - p3[1]);
} 
bool PointInTriangle(float pt[2], float v1[2], float v2[2], float v3[2])
{
    bool b1, b2, b3;
    
    b1 = sign(pt, v1, v2) < 0.0f;
    b2 = sign(pt, v2, v3) < 0.0f;
    b3 = sign(pt, v3, v1) < 0.0f;
    
    return ((b1 == b2) && (b2 == b3));
} 
bool isIntArrayEqual(int a[], int b[], int size){
    for (int i = 0; i<size; i++){
        if (a[i] != b[i]) return false;
    }
    return true;
}
bool is3IntPtCollinear(int a[2], int b[2], int c[2]){ 
    if (0.5*(a[0]*(b[1]-c[1])+b[0]*(c[1]-a[1])+c[0]*(a[1]-b[1])) == 0){
        return true;
    }   
    return false; 
}
void getRectBoundaryValues(float vertices[][2], int vertexNum, float boundary[4]){
    float x_min = vertices[0][0];
    float x_max = vertices[0][0];
    float y_min = vertices[0][1];
    float y_max = vertices[0][1];
    for (int i = 1; i < vertexNum; i++){
        if (x_min > vertices[i][0]){
            x_min = vertices[i][0];
        }
        if (x_max < vertices[i][0]){
            x_max = vertices[i][0];
        }
        if (y_min > vertices[i][1]){
            y_min = vertices[i][1];
        }
        if (y_max < vertices[i][1]){
            y_max = vertices[i][1];
        }
    }
    boundary[0] = x_min;
    boundary[1] = x_max;
    boundary[2] = y_min;
    boundary[3] = y_max;
}
 

static const int _2DLangrangeInterpolation = 1; 
static const int _2DLinearInterpolation = 2; 

template <class FloatOrInt>   
void new3DArray(FloatOrInt***& array_ptr, int h, int w, int d){
    array_ptr = new FloatOrInt**[h];
    for(int i = 0;i < h; i++){
        array_ptr[i] = new FloatOrInt*[w];
        for (int j = 0;j < w; j++){
            array_ptr[i][j] = new FloatOrInt[d]();
        }
    }   
}   
template <class FloatOrInt>   
void delete3DArray(FloatOrInt***& array_ptr, int h, int w){   
    for(int i = 0;i < h; i++){
        for (int j = 0;j < w; j++){
            delete[] array_ptr[i][j];
        }
        delete[] array_ptr[i];
    }
    delete[] array_ptr; 
}

template <class FloatOrInt>
void _2DLinear(_2DLinear_InterpolationParas& _paras,int y,float x_bounds[2], FloatOrInt*** Map_ptr, int maxBoundXOnDomain, int maxBoundYOnDomain, int maxBoundXOnValues, int maxBoundYOnValues){ 
     
    int min = round(std::min(x_bounds[0], x_bounds[1]));
    int max = round(std::max(x_bounds[0], x_bounds[1]));
    if (y > maxBoundYOnDomain || y < 0){   
        return;
    }      
    //__android_log_write(ANDROID_LOG_DEBUG, "debugging", (to_string(min)+ " " + to_string(max)).c_str()); 
    for(int x = min; x <= max; x++){   
         
        // check valid point
        if (x > maxBoundXOnDomain || x < 0){  
            //__android_log_write(ANDROID_LOG_DEBUG, "debugging", to_string(x).c_str()); 
            continue;			
        }    
        /*
        VectorXf pt(2);
        pt(0) = x;
        pt(1) = y;
        */
        
        float value[2];
        value[0] = _paras.A(0,0)*x + _paras.A(0,1)*y + _paras.t(0);
        value[1] = _paras.A(1,0)*x + _paras.A(1,1)*y + _paras.t(1); 
  
        if (value[0] > maxBoundXOnValues){ 
            value[0] = maxBoundXOnValues;			
        }if (value[0] < 0 ){
            value[0] = 0;
        }if (value[1] > maxBoundYOnValues){
            value[1] = maxBoundYOnValues;			
        }if (value[1] < 0){
            value[1] = 0;
        }       
        
        Map_ptr[y][x][0] = (FloatOrInt) round(value[0]);
        Map_ptr[y][x][1] =  (FloatOrInt) round(value[1]); 
            
        //__android_log_write(ANDROID_LOG_DEBUG, "debugging", ("TMap: "+  to_string(TMap[2*(maxBoundXOnDomain + 1)*y + 2*x])).c_str()); 
 
       /*
        if (std::is_same<FloatOrInt, int>::value){ 
            Map_ptr[y][x][0] = (int) round(value[0]);
            Map_ptr[y][x][1] = (int) round(value[1]);
        }else{ 
            Map_ptr[y][x][0] = value[0];
            Map_ptr[y][x][1] = value[1];
        } 
        VectorXf value = _paras.A*pt + _paras.t;   
  
        if (value(0) > maxBoundXOnValues){ 
            value(0) = maxBoundXOnValues;			
        }if (value(0) < 0 ){
            value(0) = 0;
        }if (value(1) > maxBoundYOnValues){
            value(1) = maxBoundYOnValues;			
        }if (value(1) < 0){
            value(1) = 0;
        }      
        if (std::is_same<FloatOrInt, int>::value){ 
            TMap[2*(maxBoundXOnDomain + 1)*y + 2*x] = (int) round(value(0));
            TMap[2*(maxBoundXOnDomain + 1)*y + 2*x + 1] = (int) round(value(1));
        }else{ 
            TMap[2*(maxBoundXOnDomain + 1)*y + 2*x] = value(0);
            TMap[2*(maxBoundXOnDomain + 1)*y + 2*x + 1] = value(1);
        }  
        
     * */
    }   
} 

// interpolation2dMapping is used as a pointer type as it has 3 dimensions.
template <class FloatOrInt> 
void singleFace2DIntepolation(float vertices[][2], int vertexNum, int faces[][3], int faceNum, float mapping[][2], FloatOrInt***& intepolation2dMapping_ptr, int maxBoundXOnDomain, int maxBoundYOnDomain, const int method){
        
   new3DArray(intepolation2dMapping_ptr, maxBoundYOnDomain + 1, maxBoundXOnDomain + 1, 2); 
            
    float mappingBoundaries[4];
    getRectBoundaryValues(mapping, vertexNum, mappingBoundaries);  
    
    float domainBoundaries[4];
    getRectBoundaryValues(vertices, vertexNum, domainBoundaries);  
    // for linear interpolation storage
    _2DLinear_InterpolationParas paras;
    for (int f = 0; f<faceNum; f++){    
        float v1[2] = {vertices[faces[f][0]][0] - domainBoundaries[0], vertices[faces[f][0]][1]};
        float v2[2] = {vertices[faces[f][1]][0] - domainBoundaries[0], vertices[faces[f][1]][1]};
        float v3[2] = {vertices[faces[f][2]][0] - domainBoundaries[0], vertices[faces[f][2]][1]};  
        
        float vMap1[2] = {mapping[faces[f][0]][0], mapping[faces[f][0]][1]};
        float vMap2[2] = {mapping[faces[f][1]][0], mapping[faces[f][1]][1]};
        float vMap3[2] = {mapping[faces[f][2]][0], mapping[faces[f][2]][1]}; 
        // find upper, middle, lower vertex   
        float* upper = 0;
        float* middle = 0;
        float* bottom = 0;   
        
        getYVertexOrder(upper, middle, bottom, v1, v2, v3); 
        int upperYVertex[2] = {(int) round(upper[0]), (int) round(upper[1])};
        int middleYVertex[2] = {(int) round(middle[0]), (int) round(middle[1])};
        int bottomYVertex[2] = {(int) round(bottom[0]), (int) round(bottom[1])};  
        /*
         std::cout << f<< std::endl;  
         std::cout << upperYVertex[0] <<" "<<upperYVertex[1] << std::endl;  
         std::cout << middleYVertex[0] <<" "<<middleYVertex[1] << std::endl;  
         std::cout << bottomYVertex[0] <<" "<<bottomYVertex[1] << std::endl;  
         std::string line;
         std::getline(std::cin, line);   
         */ 
        switch (method){  
            default:
            case _2DLinearInterpolation:
                _2DLinear_getInterpolationParas(paras,v1,v2,v3,vMap1,vMap2,vMap3);  
        }     
        // assume triangle vertices are not collinear and coincide
        if (isIntArrayEqual(upperYVertex, middleYVertex, 2) || 
                isIntArrayEqual(upperYVertex, bottomYVertex, 2) ||
                isIntArrayEqual(middleYVertex, bottomYVertex, 2) ||
                is3IntPtCollinear(upperYVertex, middleYVertex, bottomYVertex)){
            continue;
        }
        if (bottomYVertex[1] == middleYVertex[1]){
            for (int y = bottomYVertex[1];y <= upperYVertex[1]; y++){  
                
                // TODO: Here fixes the mapping problem
                float temp[2] = {(float)(y-bottomYVertex[1])*(upperYVertex[0]-bottomYVertex[0])
                        /(upperYVertex[1]-bottomYVertex[1])+bottomYVertex[0],
                (float) (y-bottomYVertex[1])*(upperYVertex[0]-middleYVertex[0])
                        /(upperYVertex[1]-bottomYVertex[1])+middleYVertex[0]};  
                switch (method){ 
                    case _2DLangrangeInterpolation:
                        /*
                        _2DLangrange(v1,v2,v3,vMap1,vMap2,vMap3,y,temp,intepolation2dMapping_ptr,
                                (int) round(boundaries[1]-boundaries[0]),(int) round(boundaries[3]-boundaries[2]));                     
                        */
                        break;
                    default:
                    case _2DLinearInterpolation:  
                        _2DLinear(paras,y,temp,intepolation2dMapping_ptr,
                                maxBoundXOnDomain, maxBoundYOnDomain,
                                (int) round(mappingBoundaries[1]-mappingBoundaries[0]),(int) round(mappingBoundaries[3]-mappingBoundaries[2]));                     
                        break; 
                }   
            }	  
        }else if (upperYVertex[1] == middleYVertex[1]){
            for (int y = bottomYVertex[1]; y <= upperYVertex[1]; y++){ 
                
                // TODO: Here fixes the mapping problem
                float temp[2] = {(float)(y-bottomYVertex[1])*(upperYVertex[0]-bottomYVertex[0])
                        /(upperYVertex[1]-bottomYVertex[1])+bottomYVertex[0],
                (float)(y-bottomYVertex[1])*(middleYVertex[0]-bottomYVertex[0])
                        /(upperYVertex[1]-bottomYVertex[1])+bottomYVertex[0]};   
                switch (method){ 
                    case _2DLangrangeInterpolation:
                        /*
                        _2DLangrange(v1,v2,v3,vMap1,vMap2,vMap3,y,temp,intepolation2dMapping_ptr,
                                (int) round(boundaries[1]-boundaries[0]),(int) round(boundaries[3]-boundaries[2]));                     
                        */                   
                        break;
                    default:
                    case _2DLinearInterpolation:  
                        _2DLinear(paras,y,temp,intepolation2dMapping_ptr,
                                maxBoundXOnDomain, maxBoundYOnDomain,
                                (int) round(mappingBoundaries[1]-mappingBoundaries[0]),(int) round(mappingBoundaries[3]-mappingBoundaries[2]));                     
                        break; 
                }   
            }  
        }else{
            for (int y = bottomYVertex[1]; y <= middleYVertex[1]; y++){
                
                // TODO: Here fixes the mapping problem
                float temp[2] = {(float)(y-bottomYVertex[1])*(upperYVertex[0]-bottomYVertex[0])
                        /(upperYVertex[1]-bottomYVertex[1])+bottomYVertex[0],
                (float)(y-bottomYVertex[1])*(middleYVertex[0]-bottomYVertex[0])
                        /(middleYVertex[1]-bottomYVertex[1])+bottomYVertex[0]}; 
                switch (method){ 
                    case _2DLangrangeInterpolation:
                        /*
                        _2DLangrange(v1,v2,v3,vMap1,vMap2,vMap3,y,temp,intepolation2dMapping_ptr,
                                (int) round(boundaries[1]-boundaries[0]),(int) round(boundaries[3]-boundaries[2]));                     
                        */             
                        break;
                    default:
                    case _2DLinearInterpolation:  
                        _2DLinear(paras,y,temp,intepolation2dMapping_ptr,
                                maxBoundXOnDomain, maxBoundYOnDomain,
                                (int) round(mappingBoundaries[1]-mappingBoundaries[0]),(int) round(mappingBoundaries[3]-mappingBoundaries[2]));                     
                        break; 
                }   
            } for (int y = middleYVertex[1] + 1;y <= upperYVertex[1]; y++){ 
                // TODO: Here fixes the mapping problem
                float temp[2] = {(float)(y-bottomYVertex[1])*(upperYVertex[0]-bottomYVertex[0])
                        /(upperYVertex[1]-bottomYVertex[1])+bottomYVertex[0],
                (float)(y-middleYVertex[1])*(upperYVertex[0]-middleYVertex[0])
                        /(upperYVertex[1]-middleYVertex[1])+middleYVertex[0]};   
                switch (method){ 
                    case _2DLangrangeInterpolation:
                        /*
                        _2DLangrange(v1,v2,v3,vMap1,vMap2,vMap3,y,temp,intepolation2dMapping_ptr,
                                (int) round(boundaries[1]-boundaries[0]),(int) round(boundaries[3]-boundaries[2]));                     
                        */                 
                        break;
                    default:
                    case _2DLinearInterpolation:  
                        _2DLinear(paras,y,temp,intepolation2dMapping_ptr,
                                maxBoundXOnDomain, maxBoundYOnDomain,
                                (int) round(mappingBoundaries[1]-mappingBoundaries[0]),(int) round(mappingBoundaries[3]-mappingBoundaries[2]));                     
                        break; 
                }   
            }	 
        }   
    }
}   
#endif /* LAGRANGE2D_H */

