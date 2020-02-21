package tech.silvermind.Jdt.generator.point;

import tech.silvermind.Jdt.domain.Point;

import java.util.Vector;

/**
 * Point generator
 *
 * @author manolovn
 */
public interface PointGenerator {

    Vector<Point> generatePoints(int width, int height);

    void setBleedX(int bleedX);

    void setBleedY(int bleedY);
}
