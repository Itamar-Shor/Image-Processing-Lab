
package il.ac.tau.adviplab.myimageproc;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"ConstantConditions", "SameParameterValue"})
public class MyImageProc {

    public static void detectAndReplaceChessboard(Mat sourceImage, Mat replacementImage) {
        int bwThreshold = 100;
        Mat bwImage = new Mat();
        Mat dilated = new Mat();
        Mat eroded = new Mat();
        List<MatOfPoint> convex_hull;


        im2BW(sourceImage, bwImage, bwThreshold, Imgproc.THRESH_BINARY_INV);
        imDilate(bwImage, dilated, new Size(5.0, 5.0));
        imErode(bwImage, eroded, new Size(5.0, 5.0));
        convex_hull = findClutterOfConnectedComponents(sourceImage, dilated, eroded, 30,35, true);
        Imgproc.drawContours(sourceImage, convex_hull, 0, new Scalar(0, 255, 0), 5);

        bwImage.release();
        dilated.release();
        eroded.release();
    }

    private static void im2BW(Mat image, Mat bwImage, double threshold, int type) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(gray, bwImage, threshold, 500, type);

        gray.release();
    }

    private static void imDilate(Mat image, Mat dilatedImage, Size strelSize) {
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, strelSize);
        Imgproc.dilate(image, dilatedImage, kernel);
        kernel.release();
    }
    private static void imErode(Mat image, Mat erodedImage, Size strelSize) {
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, strelSize);
        Imgproc.erode(image, erodedImage, kernel);
        kernel.release();
    }

    private static List<MatOfPoint> findClutterOfConnectedComponents(Mat sourceImage, Mat dilatedImage,
                                                                     Mat erodedImage, int tmin, int tmax, boolean showFlag) {
        List<MatOfPoint> hullList = new ArrayList<>();
        Mat labels = new Mat(dilatedImage.size(), dilatedImage.type());
        Mat stats = Mat.zeros(new Size(0,0), 0);
        Mat centroids = Mat.zeros(new Size(0,0), 0);
        Mat blob = new Mat();
        Mat blob_nzero = new Mat();
        MatOfPoint points;
        MatOfPoint hull = new MatOfPoint();

        Imgproc.connectedComponentsWithStats(dilatedImage, labels, stats, centroids);

        int img_w = sourceImage.width();
        int img_h = sourceImage.height();

        int[] rectangleInfo = new int[5];
        int blob_idx = 0, max_nofCC = -1, max_nofCC_idx = 0;

        // loop over all dilated blobs
        for(int i = 1; i < stats.rows(); i++) {
            // Extract bounding box
            stats.row(i).get(0, 0, rectangleInfo);
            int x = (rectangleInfo[0] >= img_w) ? (img_w-1) : rectangleInfo[0] ;
            int y = (rectangleInfo[1] >= img_h) ? (img_h-1) : rectangleInfo[1];
            int w = (rectangleInfo[2] + x > img_w) ? (img_w-1-x) : rectangleInfo[2] ;
            int h = (rectangleInfo[3] + y > img_h) ? (img_h-1-y) : rectangleInfo[3] ;

            Rect rectangle = new Rect(x, y, w, h);
            Mat subM = erodedImage.submat(rectangle);

            int nofCC = Imgproc.connectedComponents(subM, labels);
            if (nofCC > max_nofCC){
                max_nofCC = nofCC;
                max_nofCC_idx = i;
            }
            if ((tmin <= nofCC) && (nofCC <= tmax)) {
                blob_idx = i;
            }
            if (showFlag){
                Imgproc.rectangle(sourceImage, rectangle, new Scalar(1));
                Imgproc.putText(sourceImage, Integer.toString(nofCC), new Point(x+w*0.5, y+h*0.5), Imgproc.FONT_HERSHEY_COMPLEX, 1, new Scalar(255, 255, 255));
            }

        }
        // TODO: dispaly

        // in case that no blob_idx satisfied the condition
        blob_idx = (blob_idx == 0) ? max_nofCC_idx : blob_idx;

        Core.compare(labels, new Scalar(blob_idx), blob, Core.CMP_EQ);
        Core.findNonZero(blob, blob_nzero);

        points = new MatOfPoint(blob_nzero);
        convexHull(points, hull);
        hullList.add(hull);

        blob.release();
        blob_nzero.release();
        labels.release();
        stats.release();
        centroids.release();
        points.release();

        return hullList;
    }

    private static void convexHull(MatOfPoint points, MatOfPoint hull) {
        MatOfInt hullMatOfInt = new MatOfInt();
        List<Point> pointsList = new LinkedList<>();
        Imgproc.convexHull(points, hullMatOfInt);
        for (int i = 0; i < hullMatOfInt.height(); i++) {
            pointsList.add(points.toList().get(hullMatOfInt.toList().get(i)));
        }
        hull.fromList(pointsList);
        hullMatOfInt.release();
    }

    private static List<Point> approxCurve(Mat sourceImage, List<MatOfPoint> pointsOnCurveList, boolean drawFlag) {
        MatOfPoint2f hullOfChessBoard2f;
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        List<MatOfPoint> approxCurveList = new ArrayList<>();
        List<Point> listOfPointsOnApproxCurve = new ArrayList<>();
        if ((pointsOnCurveList != null) && (pointsOnCurveList.size() != 0)) {
            hullOfChessBoard2f = new MatOfPoint2f(pointsOnCurveList.get(0).toArray());
            Imgproc.approxPolyDP(hullOfChessBoard2f, approxCurve,
                    Imgproc.arcLength(hullOfChessBoard2f, true) * 0.02, true);
            approxCurveList.add(new MatOfPoint(approxCurve.toArray()));
            if (drawFlag) {
                Imgproc.drawContours(sourceImage, approxCurveList, 0, new Scalar(0, 255, 0), 2);
            }
            listOfPointsOnApproxCurve = approxCurveList.get(0).toList();
        }
        return listOfPointsOnApproxCurve;
    }





    private static int comparePoints(Point center, Point p1, Point p2){
        double angle1, angle2, d1, d2;
        angle1 = getAngle(center, p1);
        angle2 = getAngle(center, p2);

        if (angle1 < angle2){
            return -1;
        }

        d1 = getDistance(center, p1);
        d2 = getDistance(center, p2);

        if ((angle1 == angle2) && (d1 < d2)){
            return -1;
        }
        return 1;
    }


    private static double getAngle(Point center, Point p){
        double x, y, angle;
        x = p.x - center.x;
        y = p.y - center.y;
        angle = Math.atan2(y, x);
        if (angle <= 0){
            angle += 2 * Math.PI;
        }
        return angle;
    }

    private static double getDistance(Point p1, Point p2){
        double x, y;
        x = p1.x - p2.x;
        y = p1.y - p2.y;
        return Math.sqrt(x*x + y*y);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static List<Point> sortPoints(List<Point> listOfPoints, Mat image, boolean drawFlag) {
        Point center = new Point(0,0);
        List<Point> sorted = new ArrayList<Point>();

        for (int i=0; i<listOfPoints.size(); i++){
            center.x += listOfPoints.get(i).x;
            center.y += listOfPoints.get(i).y;
        }
        center.x /= listOfPoints.size();
        center.y /= listOfPoints.size();

        for (int i=0; i<listOfPoints.size(); i++){
            Point new_p = new Point(listOfPoints.get(i).x - center.x, listOfPoints.get(i).y - center.y);
            listOfPoints.set(i,new_p);
        }

        listOfPoints.sort((Point p1, Point p2) -> comparePoints(center, p1, p2));

        for (int i=listOfPoints.size()-1; i>=0; i--){
            sorted.add(listOfPoints.get(i));
        }

        return sorted;
    }

}
