package ru.infernoproject.worldd.utils;

import ru.infernoproject.worldd.constants.WorldSize;
import ru.infernoproject.worldd.world.movement.WorldPosition;

import java.util.List;

public final class MathUtils {

    /*
    Original Algorithm (https://habr.com/post/125356/)

    template bool pt_in_polygon2(const T &test,const std::vector &polygon) {
        static const int q_patt[2][2]= { {0,1}, {3,2} };

        if (polygon.size()<3) return false;

        std::vector::const_iterator end=polygon.end();

        T pred_pt=polygon.back();

        pred_pt.x-=test.x;
        pred_pt.y-=test.y;

        int pred_q=q_patt[pred_pt.y<0][pred_pt.x<0];

        int w=0;

        for(std::vector::const_iterator iter=polygon.begin(); iter!=end; ++iter) {
            T cur_pt = *iter;

            cur_pt.x-=test.x;
            cur_pt.y-=test.y;

            int q=q_patt[cur_pt.y<0][cur_pt.x<0];

            switch (q-pred_q) {
                case -3:++w;break;
                case 3:--w;break;
                case -2:if(pred_pt.x*cur_pt.y>=pred_pt.y*cur_pt.x) ++w;break;
                case 2:if(!(pred_pt.x*cur_pt.y>=pred_pt.y*cur_pt.x)) --w;break;
            }

            pred_pt = cur_pt;
            pred_q = q;

        }

        return w!=0;
    }
     */

    public static boolean isPointInPolygon(WorldPosition point, List<WorldPosition> polygon) {
        if (polygon.size() < 3)
            return false;

        final int qPattern[][] = {{0, 1}, {2, 3}};

        WorldPosition previousPoint = polygon.get(0);

        previousPoint = new WorldPosition(
            previousPoint.getLocation(),
            previousPoint.getX() - point.getX(),
            previousPoint.getY(),
            previousPoint.getZ() - point.getZ(),
            previousPoint.getOrientation()
        );

        int previousQ = qPattern[previousPoint.getZ() < 0 ? 1 : 0][previousPoint.getX() < 0 ? 1 : 0];
        int w = 0;

        for (WorldPosition polygonPoint : polygon) {
            WorldPosition currentPoint = new WorldPosition(
                polygonPoint.getLocation(),
                polygonPoint.getX() - point.getX(),
                polygonPoint.getY(),
                polygonPoint.getZ() - point.getZ(),
                polygonPoint.getOrientation()
            );

            int q = qPattern[currentPoint.getZ() < 0 ? 1 : 0][currentPoint.getX() < 0 ? 1 : 0];

            switch (q - previousQ) {
                case -3:
                    ++w;
                    break;
                case 3:
                    --w;
                    break;
                case -2:
                    if (previousPoint.getX() * currentPoint.getZ() >= previousPoint.getZ() * currentPoint.getX())
                        ++w;
                    break;
                case 2:
                    if (!(previousPoint.getX() * currentPoint.getZ() >= previousPoint.getZ() * currentPoint.getX()))
                        --w;
                    break;
            }

            previousPoint = currentPoint;
            previousQ = q;
        }

        return w != 0;
    }

    public static WorldPosition getLinePoint(WorldPosition p1, WorldPosition p2, float x) {
        float z = ((p1.getZ() - p2.getZ()) * x + (p1.getX() * p2.getZ() - p2.getX() * p1.getX())) / (p1.getX() - p2.getX());

        return new WorldPosition(p1.getLocation(), x, 0f, z, 0f);
    }

    public static boolean isPathInPolygon(WorldPosition source, WorldPosition destination, List<WorldPosition> polygon) {
        float step = WorldSize.MAX_SPEED * 0.1f;

        float minX = Math.min(source.getX(), destination.getX());
        float maxX = Math.max(source.getX(), destination.getX());

        for (float x = minX; x <= maxX; x += step) {
            WorldPosition point = MathUtils.getLinePoint(source, destination, x);

            if (isPointInPolygon(point, polygon)) {
                return true;
            }
        }

        return false;
    }

    public static float calculateDistance(WorldPosition a, WorldPosition b) {
        float distanceX = a.getX() - b.getX();
        float distanceY = a.getY() - b.getY();
        float distanceZ = a.getZ() - b.getZ();

        return (float) Math.sqrt(
                Math.pow(distanceX, 2.0f) + Math.pow(distanceY, 2.0) + Math.pow(distanceZ, 2.0)
        );
    }
}
