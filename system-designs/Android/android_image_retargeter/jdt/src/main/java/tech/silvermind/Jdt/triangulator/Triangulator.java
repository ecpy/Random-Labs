package tech.silvermind.Jdt.triangulator;

import tech.silvermind.Jdt.domain.Point;
import tech.silvermind.Jdt.domain.Triangle;

import java.util.Vector;

/**
 * Triangulator behavior
 *
 * @author manolovn
 */
public abstract class Triangulator {

    protected abstract Vector<Triangle> triangulate(Vector<Point> pointSet);
}
