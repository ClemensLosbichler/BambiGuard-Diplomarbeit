#define _USE_MATH_DEFINES

#include <iostream>
#include <math.h>
#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;

#pragma once
class BambiGuardRecognition
{
private:
	const bool DEBUG = false;

	const int HORIZONTAL_FIELD_OF_VIEW = 57;
	const int HORIZONTAL_PIXELS = 640;
	const int VERTICAL_PIXELS = 360;

	// Constants effecting algorithm
	const double MINIMAL_BAMBI_WIDTH = 0.1;
	const double FACTOR_TO_MAX_BAMBI_WIDTH = 20;
	const int MINIMAL_DISTANCE_BETWEEN_BAMBIS = 3;
	const double RADIUS_ARROUND_BAMBIS = 1;
	const double MAXIMAL_SURROUNDING_AVERAGE_VALUE = 100;

	enum class ThresholdType
	{
		Binary = 0, Otsu = 1
	};

	const Mat kernel = Mat::ones(10, 10, CV_8UC1);
	const int thresholdNumber = 127;
	const ThresholdType thresholdType = ThresholdType::Binary;

	double flightHeight;
	double minimalBambiArea;
	double maximalBambiArea;
	double imageWidth;
	double imageHeight;
	double pixelsPerMeter;

	Mat imageForDebugging;

public:
	BambiGuardRecognition(int flightHeight)
	{
		BambiGuardRecognition::flightHeight = flightHeight;
		calculateBambiArea();
	}

	vector<Point> detectBambisInImage(Mat image)
	{
		imageForDebugging = image;
		Mat imageProcessed = processImage(image);

		vector<vector<Point>> contours = findContoursInImage(imageProcessed);
		vector<Point> contourCenters;

		for (int i = 0; i < contours.size(); i++)
		{
			if (!isContourAreaInRange(contours[i]) ||
				!isEnclosingCircleAreaInRange(contours[i]) ||
				!isContourSurroundedByGrass(contours[i], image))
			{
				if (DEBUG)
				{
					cout << "contour is bad" << endl;
					if (!isContourAreaInRange(contours[i]))
						cout << "Contour area NOT in range" << endl;
					if (!isEnclosingCircleAreaInRange(contours[i]))
						cout << "Enclosing circle area NOT in range" << endl;
					if (!isContourSurroundedByGrass(contours[i], image))
						cout << "Contour is NOT surrounded by grass" << endl;
				}
				continue;
			}

			contourCenters.push_back(getContourCenter(contours[i]));
		}

		eliminateClosePoints(contourCenters);

		if (DEBUG)
		{
			imshow("Image Processed", imageProcessed);
			showMiddlepoints(image, contourCenters);
			showBambiArea(image);
		}

		for (Point point : contourCenters)
			point = getPointRelativeToImageCenterInMeter(point);

		return contourCenters;
	}

	Point getPointCoordinates(Point point, double longitudeDrone, double altitudeDrone)
	{

	}

	bool checkBambiCoordinates(Point point, vector<vector<double, double>> scannableArea, vector<double, double> helperCoordinates)
	{
		
	}

private:
	void calculateBambiArea()
	{
		imageWidth = tan(HORIZONTAL_FIELD_OF_VIEW) * flightHeight;
		imageHeight = imageWidth * ((double)VERTICAL_PIXELS / (double)HORIZONTAL_PIXELS);
		pixelsPerMeter = HORIZONTAL_PIXELS / imageWidth;

		double minBambiPixelDiameter = pixelsPerMeter * MINIMAL_BAMBI_WIDTH;
		minimalBambiArea = pow((minBambiPixelDiameter / 2), 2) * M_PI;
		maximalBambiArea = minimalBambiArea * FACTOR_TO_MAX_BAMBI_WIDTH;

		if (DEBUG)
		{
			cout << "\nBambiArea:" << endl;
			cout << "minimalBambiArea " << minimalBambiArea << endl;
			cout << "maximalBambiArea " << maximalBambiArea << endl;
			cout << "pixelsPerMeter " << pixelsPerMeter << endl;
			cout << "imageWidth " << imageWidth << endl;
			cout << "imageHeight " << imageHeight << endl;
		}
	}

	Mat processImage(Mat image)
	{
		Mat imageProcessed = image.clone();

		blurImage(imageProcessed);
		thresholdImage(imageProcessed);
		transformMorphological(imageProcessed);

		return imageProcessed;
	}

	void blurImage(Mat image)
	{
		GaussianBlur(image, image, Size(5, 5), 0);
	}

	void thresholdImage(Mat image)
	{
		switch (thresholdType)
		{
		case ThresholdType::Otsu:
			threshold(image, image, 0, 255, THRESH_OTSU);
		default:
		case ThresholdType::Binary:
			threshold(image, image, thresholdNumber, 255, THRESH_BINARY);
		}
	}

	void transformMorphological(Mat image)
	{
		morphologyEx(image, image, MORPH_OPEN, kernel);
		morphologyEx(image, image, MORPH_CLOSE, kernel);
	}

	vector<vector<Point>> findContoursInImage(Mat image)
	{
		vector<vector<Point>> contours;
		findContours(image, contours, RETR_TREE, CHAIN_APPROX_NONE);

		if (DEBUG)
			drawContours(image, contours, -1, Scalar(255, 255, 255), 3);
		return contours;
	}

	Point getContourCenter(vector<Point> contour)
	{
		Point sum = Point(0, 0);
		for (int i = 0; i < contour.size(); i++)
		{
			sum.x += contour[i].x;
			sum.y += contour[i].y;
		}

		return Point(sum.x / contour.size(), sum.y / contour.size());
	}

	bool isContourAreaInRange(vector<Point> contour)
	{
		double area = contourArea(contour);
		if (DEBUG)
		{
			cout << "contour area: " << area << endl;
		}
		return (area > minimalBambiArea || area < maximalBambiArea);
	}

	bool isEnclosingCircleAreaInRange(vector<Point> contour)
	{
		Point2f center;
		float radius = 0.0;
		minEnclosingCircle(contour, center, radius);

		return (radius * 2 > MINIMAL_BAMBI_WIDTH) || (radius * 2 < MINIMAL_BAMBI_WIDTH * FACTOR_TO_MAX_BAMBI_WIDTH);
	}

	bool isContourSurroundedByGrass(vector<Point> contour, Mat image)
	{
		Point2f center;
		float radius = 0.0;
		minEnclosingCircle(contour, center, radius);

		float surroundingRadius = radius + RADIUS_ARROUND_BAMBIS * pixelsPerMeter;
		float averageValue = getAverageValueOfCircleSegment(center, radius, surroundingRadius, image);

		if (DEBUG)
		{
			//showCircle(center, (int)radius);
			//showCircle(center, (int)surroundingRadius);
		}

		return averageValue < MAXIMAL_SURROUNDING_AVERAGE_VALUE;
	}

	void eliminateClosePoints(vector<Point>& points)
	{
		points.erase(
			remove_if(
				points.begin(),
				points.end(),
				[=](Point const& p) { return isCloseToOtherPoints(p, points); }
			),
			points.end()
		);
	}

	bool isCloseToOtherPoints(Point point, vector<Point> otherPoints)
	{
		for (Point otherPoint : otherPoints)
		{
			if (point == otherPoint)
				continue;
			if (getDistanceBetweenPoints(point, otherPoint) <= pixelsPerMeter * MINIMAL_DISTANCE_BETWEEN_BAMBIS)
				return true;
		}
		return false;
	}

	double getAverageValueOfCircleSegment(Point center, float innerRadius, float outerRadius, Mat image)
	{
		int leftBoundary = (center.x - outerRadius) > 0 ? (center.x - outerRadius) : 0;
		int rightBoundary = center.x + outerRadius;
		int upperBoundary = (center.y - outerRadius) > 0 ? (center.y - outerRadius) : 0;
		int lowerBoundary = center.y + outerRadius;
		Size imageSize = image.size();

		int valueSum = 0;
		int valueCount = 0;

		for (int pixelX = leftBoundary; pixelX <= rightBoundary && pixelX < imageSize.width; pixelX++)
		{
			for (int pixelY = upperBoundary; pixelY <= lowerBoundary && pixelY < imageSize.height; pixelY++)
			{
				double distanceToCenter = getDistanceBetweenPoints(center, Point(pixelX, pixelY));

				if (distanceToCenter > outerRadius || distanceToCenter < innerRadius)
					continue;

				valueSum += (int)image.at<uchar>(pixelY, pixelX);
				valueCount++;
			}
		}

		return (double)valueSum / valueCount;
	}

	Point getPointRelativeToImageCenterInMeter(Point point)
	{
		return Point(point.x - HORIZONTAL_PIXELS / 2, point.y - VERTICAL_PIXELS / 2) / pixelsPerMeter;
	}

	double getDistanceBetweenPoints(Point point1, Point point2)
	{
		return sqrt(pow((point1.x - point2.x), 2) + pow((point1.y - point2.y), 2));
	}

	void showCircle(Point center, int radius)
	{
		circle(imageForDebugging, center, radius, (255, 255, 255), 1);
	}

	void showMiddlepoints(Mat image, vector<Point> points)
	{
		for (int i = 0; i < points.size(); i++)
		{
			if (DEBUG)
			{
				cout << points[i].x << "|"
					<< points[i].y << endl;
			}
			circle(image, points[i], 1, Scalar(255, 255, 255), 3);
		}
	}

	void showBambiArea(Mat image)
	{
		double minBambiPixelDiameter = sqrt(minimalBambiArea / M_PI) * 2;
		double maxBambiPixelDiameter = sqrt(maximalBambiArea / M_PI) * 2;

		circle(image, Point(320, 180), minBambiPixelDiameter, (255, 255, 255), 3);
		circle(image, Point(320, 180), maxBambiPixelDiameter, (255, 255, 255), 3);
	}
};