package tech.silvermind.Jdt.triangulator;

import tech.silvermind.Jdt.domain.Edge;
import tech.silvermind.Jdt.domain.Point;
import tech.silvermind.Jdt.domain.Triangle;
import tech.silvermind.Jdt.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Delaunay triangulator
 *
 * @author Edward
 */
public class DelaunayTriangulator extends Triangulator {

    private Triangulation triangulation;

    public DelaunayTriangulator() {

    }

    // TODO: Here fixes a bug that triangulator cannot handle zero points.
    private static float vectorTransformToNonZeroBase[] = new float[]{0, 0};
    private ArrayList<int[]> getBackOriginalUniqueVertexSet(ArrayList<int[]> vertices){
        for (int[] pt : vertices) {
            pt[0] += vectorTransformToNonZeroBase[0];
            pt[1] += vectorTransformToNonZeroBase[1];
        }
        return vertices;
    }

    private ArrayList<int[]> getNonZeroVertexSet(ArrayList<int[]> vertices) {
        ArrayList<int[]> vertices_copy = new ArrayList<>(vertices);
        float x_min = vertices_copy.get(0)[0];
        float y_min = vertices_copy.get(0)[1];
        for (int[] pt : vertices_copy) {
            if (x_min > pt[0]) {
                x_min = pt[0];
            }
            if (y_min > pt[1]) {
                y_min = pt[1];
            }
        }
        vectorTransformToNonZeroBase[0] = x_min - 1;
        vectorTransformToNonZeroBase[1] = y_min - 1;

        // Transform to non-zero base
        for (int[] pt : vertices_copy) {
            pt[0] -= x_min - 1;
            pt[1] -= y_min - 1;
        }
        return vertices_copy;
    }
    public static class DelaunayRestult {
        public int[][] faces;
        public int[][] vertices;
    }
    public static DelaunayRestult getResultContainer(){
        return new DelaunayRestult();
    }
    // TODO: Currently only support int type triangulation, while float type can be similarly implemented by below.
    public boolean triangulte(ArrayList<int[]> vertices, int verticesXRangeSize, int verticesYRangeSize, DelaunayRestult resultContainer){

        boolean isVerticesUnique = true;
        vertices = getNonZeroVertexSet(vertices);

        int[][] tempVertexIndexMap = new int[verticesYRangeSize + 1][verticesXRangeSize + 1];
        for (int[] row : tempVertexIndexMap) {
            Arrays.fill(row, -1);
        }
        int currentVertexIndex = 0;
        for (Iterator<int[]> iterator = vertices.iterator(); iterator.hasNext(); ) {
            int[] vertex = iterator.next();
            if (tempVertexIndexMap[vertex[1]][vertex[0]] == -1) {
                tempVertexIndexMap[vertex[1]][vertex[0]] = currentVertexIndex;
                currentVertexIndex++;
            } else {
                iterator.remove();
                isVerticesUnique = false;
            }
        }

        Vector<Triangle> triangles = triangulate(vertexList2PointVectors(getNonZeroVertexSet(vertices)));
        List<int[]> faceList = new ArrayList<>();
        for(Triangle triangle: triangles){
            int[] vertexIndexInFace = new int[3];
            int[] faceVertices = new int[]{
                    triangle.a.x,
                    triangle.a.y,
                    triangle.b.x,
                    triangle.b.y,
                    triangle.c.x,
                    triangle.c.y
            };
            for (int m = 0; m < 6; m += 2) {
                vertexIndexInFace[m / 2] = tempVertexIndexMap[faceVertices[m + 1]][faceVertices[m]];
            }
            faceList.add(vertexIndexInFace);
        }
        resultContainer.vertices = getBackOriginalUniqueVertexSet(vertices).toArray(new int[vertices.size()][2]);
        checkValidFace(resultContainer.vertices, faceList);
        resultContainer.faces = faceList.toArray(new int[faceList.size()][3]);

        return isVerticesUnique;
    }
    // remove triangle vertices which are collinear
    private boolean checkValidFace(int[][] vertices, List<int[]> faceList){
        boolean isValid = true;
        for (Iterator<int[]> iterator = faceList.iterator(); iterator.hasNext(); ) {
            int[] vertexIndexInFace = iterator.next();
            int[] a = vertices[vertexIndexInFace[0]];
            int[] b = vertices[vertexIndexInFace[1]];
            int[] c = vertices[vertexIndexInFace[2]];
            if (0 == (a[0]*(b[1]-c[1])+b[0]*(c[1]-a[1])+c[0]*(a[1]-b[1]))){
                iterator.remove();
                isValid = false;
            }
        }
        return isValid;
    }

    private Vector<Point> vertexList2PointVectors(List<int[]> list) {
        Vector<Point> pointVector = new Vector<>();
        for (int i = 0; i < list.size(); i++) {
            pointVector.add(new Point(list.get(i)[0], list.get(i)[1]));
        }
        return pointVector;
    }


    @Override
    protected Vector<Triangle> triangulate(Vector<Point> pointSet) {

        Preconditions.checkNotNull(pointSet);
        Preconditions.checkArgument(pointSet.size() >= 3, "Can't triangulate less than 3 points");
        triangulation = new Triangulation();

        Triangle superTriangle = generateSuperTriangle(pointSet);
        triangulation.add(superTriangle);

        for (int i = 0; i < pointSet.size(); i++) {
            Triangle triangle = triangulation.findContainingTriangle(pointSet.get(i));

            if (triangle == null) {
                Edge edge = triangulation.findNearestEdge(pointSet.get(i));

                Triangle first = triangulation.findOneTriangleSharing(edge);
                Triangle second = triangulation.findNeighbour(first, edge);

                if (first == null || second == null) {
                    continue;
                }

                Point firstNoneEdgeVertex = first.getNoneEdgeVertex(edge);
                Point secondNoneEdgeVertex = second.getNoneEdgeVertex(edge);

                triangulation.remove(first);
                triangulation.remove(second);

                Triangle triangle1 = new Triangle(edge.a, firstNoneEdgeVertex, pointSet.get(i));
                Triangle triangle2 = new Triangle(edge.b, firstNoneEdgeVertex, pointSet.get(i));
                Triangle triangle3 = new Triangle(edge.a, secondNoneEdgeVertex, pointSet.get(i));
                Triangle triangle4 = new Triangle(edge.b, secondNoneEdgeVertex, pointSet.get(i));

                triangulation.add(triangle1);
                triangulation.add(triangle2);
                triangulation.add(triangle3);
                triangulation.add(triangle4);

                legalizeEdge(triangle1, new Edge(edge.a, firstNoneEdgeVertex), pointSet.get(i));
                legalizeEdge(triangle2, new Edge(edge.b, firstNoneEdgeVertex), pointSet.get(i));
                legalizeEdge(triangle3, new Edge(edge.a, secondNoneEdgeVertex), pointSet.get(i));
                legalizeEdge(triangle4, new Edge(edge.b, secondNoneEdgeVertex), pointSet.get(i));
            } else {
                Point a = triangle.a;
                Point b = triangle.b;
                Point c = triangle.c;

                triangulation.remove(triangle);

                Triangle first = new Triangle(a, b, pointSet.get(i));
                Triangle second = new Triangle(b, c, pointSet.get(i));
                Triangle third = new Triangle(c, a, pointSet.get(i));

                triangulation.add(first);
                triangulation.add(second);
                triangulation.add(third);

                legalizeEdge(first, new Edge(a, b), pointSet.get(i));
                legalizeEdge(second, new Edge(b, c), pointSet.get(i));
                legalizeEdge(third, new Edge(c, a), pointSet.get(i));
            }
        }

        // remove super triangle
        triangulation.removeTrianglesUsing(superTriangle.a);
        triangulation.removeTrianglesUsing(superTriangle.b);
        triangulation.removeTrianglesUsing(superTriangle.c);

        return triangulation.getTriangles();
    }

    private void legalizeEdge(Triangle triangle, Edge edge, Point vertex) {
        Triangle neighbourTriangle = triangulation.findNeighbour(triangle, edge);

        if (neighbourTriangle != null) {
            if (neighbourTriangle.isPointInCircumcircle(vertex)) {
                triangulation.remove(triangle);
                triangulation.remove(neighbourTriangle);

                Point noneEdgeVertex = neighbourTriangle.getNoneEdgeVertex(edge);

                Triangle firstTriangle = new Triangle(noneEdgeVertex, edge.a, vertex);
                Triangle secondTriangle = new Triangle(noneEdgeVertex, edge.b, vertex);

                triangulation.add(firstTriangle);
                triangulation.add(secondTriangle);

                legalizeEdge(firstTriangle, new Edge(noneEdgeVertex, edge.a), vertex);
                legalizeEdge(secondTriangle, new Edge(noneEdgeVertex, edge.b), vertex);
            }
        }
    }

    private Triangle generateSuperTriangle(Collection<Point> pointSet) {
        final int factor = 3;
        int maxCoordinate = 0;
        int minCoordinate = 0;

        for (Point point : pointSet) {
            maxCoordinate = Math.max(Math.max(point.x, point.y), maxCoordinate);
            minCoordinate = Math.min(Math.min(point.x, point.y), minCoordinate);
        }

        Point p1 = new Point(minCoordinate, factor * maxCoordinate);
        Point p2 = new Point(factor * maxCoordinate, minCoordinate);
        Point p3 = new Point(-factor * maxCoordinate, -factor * maxCoordinate);

        return new Triangle(p1, p2, p3);
    }
}
