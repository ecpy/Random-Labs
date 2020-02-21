package tech.silvermind.demo.retargeter;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by edward on 5/25/16.
 */
public class ObjectPointsCounter {
    private ObjectPointsCounter(){
        // not allow constructor
    }
    private static ArrayList<ArrayList<Point>> objectSet = new ArrayList<>();
    public static void addPoint(int x, int y){
        if (objectSet.size() == 0){
            prepareNewObject();
        }   objectSet.get(objectSet.size() - 1).add(new Point(x, y));
    }

    public static void addPointSet(ArrayList<Point> pointSet){
        for (Point point:pointSet) {
            addPoint(point.x ,point.y);
        }
    }
    public static void reset(){
        objectSet.clear();
    }
    public static void prepareNewObject(){
        objectSet.add(new ArrayList<Point>());
    }

    public static ArrayList<ArrayList<Point>> getObjectSet(){
        // TODO: here merges objects with duplicate points into one
        HashMap<Point, Integer> pointObjectMap = new HashMap<>();
        Set<Map.Entry<Point, Integer>> set = pointObjectMap.entrySet();
        int currentObjectLabel = 0;
        for (ArrayList<Point> points:objectSet){
            for(Point point: points){
                if (pointObjectMap.containsKey(point) && pointObjectMap.get(point) != currentObjectLabel){
                    int pendingDeleteLabel = pointObjectMap.get(point);
                    for(Map.Entry<Point, Integer> entry: set){
                        if (entry.getValue() == pendingDeleteLabel){
                            entry.setValue(currentObjectLabel);
                        }
                    }
                }else{
                    pointObjectMap.put(point, currentObjectLabel);
                }
            }
            currentObjectLabel++;
        }
        // get total number of values
        HashSet<Integer> values = new HashSet<>();
        for(Map.Entry<Point, Integer> entry: set){
            values.add(entry.getValue());
        }
        HashMap<Integer, Integer> valuesIndex = new HashMap<>();
        int i = 0;
        for(Integer value:values){
            valuesIndex.put(value, i);
            i++;
        }
        ArrayList<ArrayList<Point>> finalObjectSet = new ArrayList<>();
        // initialize
        while(finalObjectSet.size() < values.size()) finalObjectSet.add(new ArrayList<Point>());

        // put back to result
        Iterator iterator = set.iterator();
        while(iterator.hasNext()){
            HashMap.Entry entry = (HashMap.Entry)iterator.next();
            finalObjectSet.get(valuesIndex.get(entry.getValue())).add((Point) entry.getKey());
            iterator.remove();
        }
        // update
        objectSet = finalObjectSet;
        return finalObjectSet;
    }
}
