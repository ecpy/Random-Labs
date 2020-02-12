#include <iostream> 
#include <fstream>
#include <cstdio>
#include <ctime> 
#include <vector>  
#include "lbs/lbs.h" 
#include "lbs/lbs_retargeting.h"    
#include "interpolation.h"
using namespace Eigen;  
int main(int argc,char *argv[]){ 
    if (argc < 5) { 
        std::cerr << "Usage:   " << argv[0] << " <vertices> <faces> <mu> <bounds>" << std::endl;
        std::cerr << "Example: " << argv[0] << " v f mu bd" << std::endl; 
        std::cerr << "File input format:  <vertices> vx2" << std::endl; 
        std::cerr << "                    <faces> fx3" << std::endl; 
        std::cerr << "                    <mu> fx2" << std::endl; 
        std::cerr << "                    <bounds> 1x4: x_min x_max y_min y_max" << std::endl; 
        return 1;
    } 
    // vertex
    std::ifstream vertexFile(argv[1]);
    if (!vertexFile){ std::cout << "Error: Cannot open vertex file.\n"; return 1;}
    std::vector<std::array<float,2>> vertex;      
    float x, y;
    while (vertexFile >> x >> y)
    { 
        std::array<float,2> temp = {x, y};
        vertex.push_back(temp); 
    } 
    vertexFile.close();
    // face
    std::ifstream faceFile(argv[2]);
    if (!faceFile){ std::cout << "Error: Cannot open face file.\n"; return 1;}
    std::vector<std::array<int,3>> face;    
    int i, j, k;
    while (faceFile >> i >> j >> k)
    {  
        std::array<int,3> temp = {i, j, k};
        face.push_back(temp); 
    } 
    faceFile.close();
    // mu
    std::ifstream muFile(argv[3]);
    if (!muFile){ std::cout << "Error: Cannot open mu file.\n"; return 1;}
    std::vector<std::array<float, 2>> mu;  
    float u,v;
    while (muFile >> u >> v)
    {  
        std::array<float, 2> temp = {u, v};
        mu.push_back(temp); 
    } 
    muFile.close();
    // bounds
    std::ifstream bdFile(argv[4]);
    if (!bdFile){ std::cout << "Error: Cannot open bounds file.\n"; return 1;}
    float bounds[4];   
    float a,b,c,d;
    while (bdFile >> a >> b >> c >> d){  bounds[0]=a;bounds[1]=b;bounds[2]=c;bounds[3]=d;} 
    bdFile.close(); 
    // check if file inputs are valid 
    const int vertexNum = vertex.size();
    const int faceNum = face.size();
    const int muNum = mu.size(); 
    if (vertexNum == 0 || faceNum== 0 || faceNum != muNum || bounds[0] >= bounds[1] || bounds[2] >= bounds[3]  ){ std::cout << "Error:Data input is not valid. Please check file data input.\n"; return 1;}     
    
    
    float solution[vertexNum][2];
    float _vertex[vertexNum][2];
    int _face[faceNum][3];
    float _mu[vertexNum][2];    
    for (int i = 0; i < vertexNum; i++){
        _vertex[i][0] = vertex[i][0];
        _vertex[i][1] = vertex[i][1];
    }for (int i = 0; i < faceNum; i++){
        _face[i][0] = face[i][0];
        _face[i][1] = face[i][1];
        _face[i][2] = face[i][2];
    }for (int i = 0; i < vertexNum; i++){
        _mu[i][0] = mu[i][0];
        _mu[i][1] = mu[i][1];
    }
    std::clock_t start = std::clock(); 
    //lbs::solveOnRect(_vertex, vertexNum, _face, faceNum, _mu, bounds, solution); 
    lbs_retargeting::solveOnRect(_vertex, vertexNum, _face, faceNum, _mu, bounds, solution); 
    
        float intSolution[vertexNum][2];
        for (int i = 0; i < vertexNum; i++){    
            intSolution[i][0] = (float) round(solution[i][0]); 
            intSolution[i][1] = (float) round(solution[i][1]);
        }       
        
    std::ofstream outputFile;
    outputFile.open("sol.m", std::ofstream::out | std::ofstream::trunc); 
    
    outputFile << "sol = [..." << std::endl;
    for (int i = 0; i < vertexNum; i++){  
            outputFile << intSolution[i][0] <<", " << intSolution[i][1] <<";\n"; 
    }    
    outputFile << "];" << "\n";   
    outputFile.close(); 
    
    int maxBoundXOnDomain = (int) round(bounds[1] - bounds[0]);
    int maxBoundYOnDomain = (int) round(bounds[3] - bounds[2]); 
    int*** intepolation2dMapping_ptr = 0;  
    
    // change solution to use y fix schemes
    /*
    for (int i = 0; i < vertexNum; i++){
        solution[i][1] = _vertex[i][1];
    }
   */ 
     
    singleFace2DIntepolation(intSolution, vertexNum, _face, faceNum, _vertex, intepolation2dMapping_ptr, maxBoundXOnDomain, maxBoundYOnDomain, _2DLinearInterpolation);

    std::cout << "Computation time: " << (std::clock() - start)/(double) CLOCKS_PER_SEC <<"s"<< std::endl; 
    std::cout << "Writing to file..." << std::endl; 
     
    outputFile.open("b.m", std::ofstream::out | std::ofstream::trunc); 
    
    outputFile << "sol_y = [..." << std::endl;
    for (int y = 0; y< maxBoundYOnDomain + 1; y++){  
        for (int x = 0 ; x< maxBoundXOnDomain; x++){
            //std::cout << "(" << y << "," << x << ")" << intepolation2dMapping_ptr[y][x][1]<< std::endl;
            outputFile << intepolation2dMapping_ptr[y][x][1] <<", "; 
        }     
        //std::cout << "(" << y << "," << maxBoundXOnDomain << ")" << intepolation2dMapping_ptr[y][maxBoundXOnDomain][1]<< std::endl; 
        outputFile <<  intepolation2dMapping_ptr[y][maxBoundXOnDomain][1] <<";" <<std::endl; 
    }    
    outputFile << "];" << "\n";   
    outputFile.close();
    
    outputFile.open("a.m", std::ofstream::out | std::ofstream::trunc); 
    outputFile << "sol_x = [..." << std::endl;
    for (int y = 0; y< maxBoundYOnDomain + 1; y++){  
        for (int x = 0 ; x< maxBoundXOnDomain; x++){
        outputFile <<  intepolation2dMapping_ptr[y][x][0] <<", ";
        }        
        outputFile <<  intepolation2dMapping_ptr[y][maxBoundXOnDomain][0] <<";" <<std::endl;
    }    
    outputFile << "];" << "\n";   
    outputFile.close();
    
    delete3DArray(intepolation2dMapping_ptr, maxBoundYOnDomain + 1, maxBoundXOnDomain + 1); 

    return 0;   
}
