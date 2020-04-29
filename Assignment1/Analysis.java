import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.MouseInfo;

import javax.imageio.*;
import java.math.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

class Analysis {

    static int height = 1080;
    static int width = 1920;
    Set<Point> points;
    Set<Point> knownPoints;
    BufferedImage originalImage;
    BufferedImage referenceImage;
    

    public BufferedImage readImage(String imagePath, BufferedImage img) {

        height = img.getHeight();
		width = img.getWidth();

        try {
            int frameLength = width*height*3;

			File file = new File(imagePath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

            raf.read(bytes);
            
			int ind = 0;
			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    img.setRGB(x,y,pix);
                    //originalImage[x][y][0] = pix;
					ind++;
				}
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
        }
        
        return img;

    }


    public void showImage(BufferedImage image)  {
        JFrame frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
        frame.setPreferredSize(new Dimension(2000, 2000));
		JLabel lbIm1 = new JLabel(new ImageIcon(image));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);


    }

    public static void main(String [] args) {
        String imagePath = args[0];
        //BufferedImage imageFile;
        Analysis object = new Analysis();
        object.originalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        object.originalImage = object.readImage(imagePath, object.originalImage);
        int numSamples = 0;
        double [] vals = {0.05, 0.1, 0.15, 0.2, 0.25, 0.30, 0.35, 0.40, 0.45, 0.5};
        for(int i=0; i<vals.length; i++) {
            BufferedImage imageFile = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            imageFile = object.readImage(imagePath, imageFile);
            numSamples = (int) Math.floor(vals[i] * height * width);
            System.out.println("_____________________________");
            System.out.println("Samples removed: " + numSamples);
            object.getPointsToRemove(imageFile, numSamples);
            object.gaussianFilter(imageFile, imagePath);
        }

    }

    public void getPointsToRemove(BufferedImage imageFile, int numSamples) {
        points = new HashSet<>();
        int i=0;
        while(i<numSamples) {
             int x = (int) Math.floor(Math.random() * (width - 1));
             int y = (int) Math.floor(Math.random() * (height - 1));

             while(points.contains(new Point(x,y))) {
                 x = (int) Math.floor(Math.random() * (width - 1));
                 y = (int) Math.floor(Math.random() * (height - 1));
               }

               points.add(new Point(x,y));
               imageFile.setRGB(x,y,0);
            i+=1;

           }
    }

    

    public double computeError(BufferedImage reconstructed) {
        //showImage(originalImage);
        //showImage(reconstructed);
        double sumOfSquares = 0.0;
        int redO = 0, greenO = 0, blueO = 0;
        int redC = 0, greenC =  0, blueC = 0;

        for(int i=0; i< width; i++) {
            for(int j=0; j<height; j++) {
                int pixelOriginal = originalImage.getRGB(i,j);
                int pixelReconstructed = reconstructed.getRGB(i,j);

                sumOfSquares +=  Math.pow((pixelOriginal - pixelReconstructed), 2.0);

                // redO += ((pixelOriginal & 0x00ff0000) >> 16);
                // greenO += ((pixelOriginal & 0x00ff00) >> 8);
                // blueO += (pixelOriginal & 0x000000ff);

                // redC += ((pixelReconstructed & 0x00ff0000) >> 16);
                // greenC += ((pixelReconstructed & 0x00ff00) >> 8);
                // blueC += (pixelReconstructed & 0x000000ff);
                
                // sumOfSquares += Math.pow((redO - redC), 2) + Math.pow((greenO - greenC), 2) + Math.pow((blueO - blueC), 2);


            }
        }

        return sumOfSquares;

    }

    public void gaussianFilter(BufferedImage image, String path) {
        //BufferedImage newImage = new BufferedImage(width, height, image.getType());
        //readImage(path, newImage);


        //showImage(image);
        //Thread.sleep(1000);
        


        double red = 0;
        double green = 0;
        double blue = 0;
        int pixel = 0;

        for(Point point: points) {
            int i = point.x;
            int j = point.y;

            
            if(i+1 <  width) {
                pixel = image.getRGB(i+1,j);
                red += 2 * ((pixel & 0x00ff0000) >> 16);
                green += 2 * ((pixel & 0x00ff00) >> 8);
                blue += 2 * (pixel & 0x000000ff);
            }

            if(i-1 >=0) {
                pixel = image.getRGB(i-1,j);
                red += 2 * ((pixel & 0x00ff0000) >> 16);
                green += 2 * ((pixel & 0x00ff00) >> 8);
                blue += 2 * (pixel & 0x000000ff);
            }

            if(j+1 < height) {
                pixel = image.getRGB(i,j+1);
                red += 2 * ((pixel & 0x00ff0000) >> 16);
                green += 2 * ((pixel & 0x00ff00) >> 8);
                blue += 2 * (pixel & 0x000000ff);
            }

            if(j-1 >= 0) {
                pixel = image.getRGB(i,j-1);
                red += 2 * ((pixel & 0x00ff0000) >> 16);
                green += 2 * ((pixel & 0x00ff00) >> 8);
                blue += 2 * (pixel & 0x000000ff);
            }

            if((i+1  < width) && (j+1 < height)) {
                pixel = image.getRGB(i+1,j+1);
                red += ((pixel & 0x00ff0000) >> 16);
                green += ((pixel & 0x00ff00) >> 8);
                blue += (pixel & 0x000000ff);
            }

            if((i+1  < width) && (j-1 >=0)) {
                pixel = image.getRGB(i+1,j-1);
                red += ((pixel & 0x00ff0000) >> 16);
                green += ((pixel & 0x00ff00) >> 8);
                blue += (pixel & 0x000000ff);
            }

            if((i-1  >= 0) && (j+1 < height)) {
                pixel = image.getRGB(i-1,j+1);
                red += ((pixel & 0x00ff0000) >> 16);
                green += ((pixel & 0x00ff00) >> 8);
                blue += (pixel & 0x000000ff);
            }

            if((i-1  >= 0) && (j-1 >=0)) {
                pixel = image.getRGB(i-1,j-1);
                red += ((pixel & 0x00ff0000) >> 16);
                green += ((pixel & 0x00ff00) >> 8);
                blue += (pixel & 0x000000ff);
            }
            
            int r = (int) Math.floor(red/16);
            int g = (int) Math.floor(green/16);
            int b = (int) Math.floor(blue/16);

            pixel = (r << 16) | (g << 8) | (b);

            image.setRGB(i,j,pixel);
        }
    
        showImage(image);

        double result = computeError(image);
        System.out.println("Error" + result);
    }



    


// public void meanFilter(BufferedImage image, String path) {
//     BufferedImage newImage = new BufferedImage(width, height, image.getType());
//     readImage(path, newImage);

//     for(Point point: points) {
//         newImage.setRGB(point.x, point.y, 0);
//     }



//     double red = 0;
//     double green = 0;
//     double blue = 0;
//     int pixel = 0;

//     for(Point point: points) {
//         int i = point.x;
//         int j = point.y;

//         if(i+1 <  width && (!points.contains(new Point(i+1,j)))) {
//             pixel = newImage.getRGB(i+1,j);
//             red += ((pixel & 0x00ff0000) >> 16);
//             green += ((pixel & 0x00ff00) >> 8);
//             blue += (pixel & 0x000000ff);
//         }

//         if(i-1 >=0 && (!points.contains(new Point(i-1,j)))) {
//             pixel = newImage.getRGB(i-1,j);
//             red += ((pixel & 0x00ff0000) >> 16);
//             green +=  ((pixel & 0x00ff00) >> 8);
//             blue +=  (pixel & 0x000000ff);
//         }

//         if(j+1 < height && (!points.contains(new Point(i,j+1)))) {
//             pixel = newImage.getRGB(i,j+1);
//             red += ((pixel & 0x00ff0000) >> 16);
//             green +=  ((pixel & 0x00ff00) >> 8);
//             blue +=  (pixel & 0x000000ff);
//         }

//         if(j-1 >= 0 && (!points.contains(new Point(i,j-1)))) {
//             pixel = newImage.getRGB(i,j-1);
//             red += ((pixel & 0x00ff0000) >> 16);
//             green +=  ((pixel & 0x00ff00) >> 8);
//             blue +=  (pixel & 0x000000ff);
//         }

//         if((i+1  < width) && (j+1 < height) && (!points.contains(new Point(i+1,j+1)))) {
//             pixel = newImage.getRGB(i+1,j+1);
//             red += ((pixel & 0x00ff0000) >> 16);
//             green += ((pixel & 0x00ff00) >> 8);
//             blue += (pixel & 0x000000ff);
//         }

//         if((i+1  < width) && (j-1 >=0) && (!points.contains(new Point(i+1,j-1)))) {
//             pixel = newImage.getRGB(i+1,j-1);
//             red += ((pixel & 0x00ff0000) >> 16);
//             green += ((pixel & 0x00ff00) >> 8);
//             blue += (pixel & 0x000000ff);
//         }

//         if((i-1  >= 0) && (j+1 < height) && (!points.contains(new Point(i-1,j+1)))) {
//             pixel = newImage.getRGB(i-1,j+1);
//             red += ((pixel & 0x00ff0000) >> 16);
//             green += ((pixel & 0x00ff00) >> 8);
//             blue += (pixel & 0x000000ff);
//         }

//         if((i-1  >= 0) && (j-1 >=0) && (!points.contains(new Point(i-1,j-1)))) {
//             pixel = newImage.getRGB(i-1,j-1);
//             red += ((pixel & 0x00ff0000) >> 16);
//             green += ((pixel & 0x00ff00) >> 8);
//             blue += (pixel & 0x000000ff);
//         }
        
//         int r = (int) Math.floor(red/8);
//         int g = (int) Math.floor(green/8);
//         int b = (int) Math.floor(blue/8);

//         pixel = (r << 16) | (g << 8) | (b);

//         newImage.setRGB(i,j,pixel);
//     }

//     showImage(newImage);

// }



// }



class Point {
    int x;
    int y;
  
    public Point(int x, int y) {
      this.x = x;
      this.y = y;
    }
  
    public boolean equals (Object object) {
      if (!(object instanceof Point)) return false;
      if (((Point) object).x != x) return false;
      if (((Point) object).y != y) return false;
      return true;
    }
  
    public int hashCode() {
      return (x << 16) + y;
    }
  
  }

//   public void nearestNeighborInterpolation(BufferedImage image, String path) {
//     BufferedImage newImage = new BufferedImage(width, height,image.getType());
//     readImage(path, newImage);
//     for(Point point: points) {
//         newImage.setRGB(point.x, point.y, 0);
//     }

//     showImage(newImage);

//     for(Point point: points) {
//         Point n = getNearest(point.x, point.y);
//         newImage.setRGB(point.x, point.y, newImage.getRGB(n.x, n.y));
//     }

//     showImage(newImage);

    
// }


// public Point getNearest(int x, int y) {
//     double minDistance  = 999;
//     Point minPoint = null;
//     for(int i = x-10; i<=x+10; i++) {
//         for(int j = y-10; j<=y+10; j++) {
//             if(i>=0 && i<width && j>=0 && j<height) {
//                 if( (i!=x && j!=y) && ((i-x)*(i-x) + (j-y)*(j-y) <= minDistance * minDistance) && (!points.contains(new Point(i-x, j-y)))) {
//                     minDistance = Math.pow((i-x)*(i-x) + (j-y)*(j-y), 2);
//                     minPoint = new Point(i,j);
//                 }
//             }
//         }
//     }

//     return minPoint;
}