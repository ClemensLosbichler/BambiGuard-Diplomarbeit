#include <iostream>
#include <opencv2/opencv.hpp>

#include "BambiGuard.h"

using namespace std;
using namespace cv;

vector<Mat> readVideo(string videoPath)
{
	VideoCapture video = VideoCapture(videoPath);

	if (!video.isOpened())
	{
		cout << "\nno video stream\n" << endl;
		return {};
	}

	cout << "\nVideo \"" << videoPath << "\"" << endl;
	cout << video.get(5) << "Hz" << endl;
	cout << "frame count: " << video.get(7) << endl;

	vector<Mat> images;

	while (video.isOpened())
	{
		Mat frame;

		if (!video.read(frame))
		{
			break;
		}

		Mat image_grayscale;
		cvtColor(frame, image_grayscale, COLOR_BGR2GRAY);

		images.push_back(image_grayscale);
	}

	video.release();

	return images;
}

vector<Mat> readPhoto(string photoPath)
{
	Mat image = imread(photoPath);
	cvtColor(image, image, COLOR_BGR2GRAY);

	if (!image.data) {
		return {};
	}

	vector<Mat> images = { image };
	return images;
}

int main()
{
	string photoPath = "1bambiWithFlaws.png";
	string videoPath = "DJI_0267.MP4";

	bool showImages = false;
	double flightHeight = 25;

	// vector<Mat> images = readPhoto(photoPath);
	vector<Mat> images = readVideo(videoPath);

	if (images.size() == 0)
	{
		cout << "\nno images\n" << endl;
		return 1;
	}

	BambiGuardRecognition recognition = BambiGuardRecognition(flightHeight);

	for (int i = 0; i < images.size(); i++)
	{
		vector<Point> middlePoints = recognition.detectBambisInImage(images[i]);

		for (Point middlePoint : middlePoints)
		{
			cout << middlePoint.x << "|" << middlePoint.y << endl;
		}

		if (showImages)
		{
			imshow("BambiGuard", images[i]);
			waitKey(30);
		}

	}

	if (showImages)
		waitKey(0);
	return 0;
}