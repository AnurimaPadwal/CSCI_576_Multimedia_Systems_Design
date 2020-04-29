/*******************************************************
 * Name: DisplayImage.java
 * Description: Interactive program to scale, magnify 
 * and anti-alias given image
 * Input parameters: [Path/to/image] type: string
 *                   [Mode] type: int - [1 or 2]
 *                   [scale factor] type: float
 *                   [alias] type: int
 *                          0 - output as is
 *                          1 - image should be anti-aliased
 * Output: Image with specified operation performed
 * Author: Anurima Anil Padwal 
 * For CSCI 576, HW Assignment 1 (Programmimg question)
 ******************************************************/
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
import java.util.List;
import java.util.Random;

class Point {
    int x;
    int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

class DisplayImage {

    static int width = 1920;
    static int height = 1080;
    static int [][][] newImage;
    static int [][][] originalImage = new int [width][height][1];

    public BufferedImage scaleImage(BufferedImage image, double scale, int isAlias) {
        int scaledWidth = (int) Math.floor(scale * width);
        int scaledHeight = (int) Math.floor(scale * height);

        newImage = new int[scaledWidth][scaledHeight][1];

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, image.getType());

        for(int j = 0; j < scaledWidth; j++) {
            for(int i = 0; i < scaledHeight; i++) {
                int pixel = image.getRGB((int)Math.floor(j/scale),(int) Math.floor(i/scale));

                newImage[j][i][0] = pixel;

                scaledImage.setRGB(j, i, pixel);
            }
        }

        if(isAlias == 1) {
            scaledImage = aliasImage(scaledImage);
        }



        return scaledImage;

    }
    
    public void readImage(String imagePath, BufferedImage img) {

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
                    originalImage[x][y][0] = pix;
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

    }
    
    public void showImage(BufferedImage image)  {
        JFrame frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
        frame.setPreferredSize(new Dimension(5000, 5000));
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

    public BufferedImage aliasImage(BufferedImage image) {
        int pixel;
        double red = 0;
        double green = 0;
        double blue = 0;

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage aliasedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for(int i = 0; i < width; i++) {
            for(int j = 0; j < height; j++) {
                pixel = image.getRGB(i,j);

                red = 4 * ((pixel & 0x00ff0000) >> 16);
                green = 4 * ((pixel & 0x00ff00) >> 8);
                blue = 4 * (pixel & 0x00000000ff);

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

                aliasedImage.setRGB(i, j, pixel);
            }
        }

        return aliasedImage;
    }

    public BufferedImage rgbToYUV(BufferedImage image) {
        Random rand = new Random();
        BufferedImage rgbImage = new BufferedImage(width, height, image.getType());
        double red, green, blue;
        //double lum = 0.01 + (0.5 - 0.01) *rand.nextDouble();

        for(int i=0; i<width; i++) {
            for(int j=0; j<height; j++) {
                int pixel = image.getRGB(i,j);
                red = ((pixel & 0x00ff0000) >> 16);
                green = ((pixel & 0x00ff00) >> 8);
                blue = (pixel & 0x000000ff);

                red = red/255;
                green = green/255;
                blue = blue/255;

                double y = 0.299 * red + 0.587 * green + 0.114 * blue;
                double u = -0.147 * red - 0.289 * green + 0.436 * blue;
                double v = 0.615 * red - 0.515 * green - 0.100 * blue;

                y = y - 0.35;

                if (y>1) y=1;
                if(y<0) y=0;

                int r, g, b;
                r = (int) Math.floor((y + 0.000 * u + 1.140 * v) * 255);
                g = (int) Math.floor((y - 0.396 * u - 0.581 * v) * 255);
                b = (int) Math.floor((y + 2.029 * u + 0.000 * v) * 255);

                if(r>255) r = 255;
                else if(r<0) r = 0;

                if(g > 255) g = 255;
                else if(g < 0) g = 0;

                if(b > 255) b = 255;
                else if(b < 0) b = 0;


                pixel = (r << 16) | (g << 8) | (b);

                rgbImage.setRGB(i,j,pixel);
            
                


             }
         }

        //showImage(rgbImage);
        return rgbImage;
    }
    public static void main(String [] args) throws InterruptedException {
        String imagePath = args[0];
        int mode = Integer.parseInt(args[1]);
        double scale = Double.parseDouble(args[2]);
        int isAlias = Integer.parseInt(args[3]);

        BufferedImage imageFile = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        DisplayImage displayImage = new DisplayImage();
        displayImage.readImage(imagePath, imageFile);
        BufferedImage resultImage = null;
          
        switch(mode) {
            case 1: resultImage = displayImage.scaleImage(imageFile, scale, isAlias);
                    
                    displayImage.showImage(resultImage);
                    break;
            case 2: resultImage = displayImage.scaleImage(imageFile, scale, isAlias);
            
                    imageFile = displayImage.rgbToYUV(imageFile);
                    for(int i = 0; i< width; i++) {
                        for (int j = 0; j<height; j++) {
                            originalImage[i][j][0] = imageFile.getRGB(i,j);
                        }
                    }
                    for(int i = 0; i< scale * width; i++) {
                        for(int j=0; j< scale * height; j++) {
                            newImage[i][j][0] =  resultImage.getRGB(i,j);
                        }
                    }
                    DisplayImage2 displayImage2 = new DisplayImage2();
                    displayImage2.displayImageMode2(imageFile, scale, newImage, originalImage, isAlias);
                    break;
            default: System.out.println("Invalid value for mode. Please enter values 1 or 2 only for mode.");
                    break;
                
        }

    }
}

class DisplayImage2 extends Frame implements MouseMotionListener{

    static int width = 1920;
    static int height = 1080;

    DisplayImage displayImage = new DisplayImage();
    JLabel lbIm1;
    BufferedImage image;
    JFrame frame;
    double scale;
    int [][][] newImage;
    int [][][] originalImage;
    int isAlias;
    GridBagLayout gLayout;
    GridBagConstraints c;
    List<Point> updateBuffer;

    public DisplayImage2(){

    }
    public void displayImageMode2(BufferedImage image, double scale, int [][][] newImage, int [][][] originalImage, int isAlias) throws InterruptedException{
        this.updateBuffer = new ArrayList<>();
        this.image = image;
        this.scale = scale;
        this.newImage = newImage;
        this.originalImage = originalImage;
        this.isAlias = isAlias;
        frame = new JFrame();
		gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
        frame.setPreferredSize(new Dimension(1920, 1080));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        lbIm1 = new JLabel(new ImageIcon(image));
    
        lbIm1.addMouseMotionListener(this);
               
        c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
        frame.getContentPane().add(lbIm1, c);
        frame.setPreferredSize(new Dimension(1920, 1080));
        
		frame.pack();
        frame.setVisible(true);

    }
    
    
    public void mouseMoved(MouseEvent e) {
        int xMouse = e.getX();
        int yMouse = e.getY();
        
        updateBufferedPixels();
        
        magnifyImage(xMouse, yMouse);

        showImageFrame();
    }

    public void mouseDragged(MouseEvent e) {}

    public void showImageFrame() {
        lbIm1.setIcon(new ImageIcon(this.image));        
        frame.getContentPane().add(lbIm1, c);   
        frame.pack();
        frame.setVisible(true);
    }

    public void updateBufferedPixels() {
        if(updateBuffer.size()>0) {
            for(Point point: updateBuffer) {
                int x = point.x;
                int y = point.y;
                this.image.setRGB(x, y, this.originalImage[x][y][0]);
            }
        }
    }

    public void magnifyImage(int xMouse, int yMouse) {
        int radius = 100;

        double anchorX = scale * xMouse;
        double anchorY = scale * yMouse;
        // double offsetX = xMouse - radius;
        // double offestY = yMouse - radius;

        updateBuffer = new ArrayList<>();

        for(int x = xMouse - radius; x <= xMouse + radius; x++){
            if(x < 0 || x>= width) continue;
            for(int y = yMouse - radius; y <= yMouse + radius; y++) {
                if(y < 0 || y >= height) continue;
                double distance = (x - xMouse) * (x - xMouse) + (y - yMouse) * (y - yMouse);
                if(distance <= radius * radius) {
                    // int xNew = (int) Math.floor(anchorX + (x - offsetX));                   
                    // int yNew = (int) Math.floor(anchorY + (y - offestY));  
                    int xNew = 0;
                    int yNew = 0;
                    if(x<=xMouse) {
                        xNew = (int) Math.floor(anchorX - (xMouse - x));
                    }
                    if(y<=yMouse) {
                        yNew = (int) Math.floor(anchorY - (yMouse - y));
                    }
                    if(x>xMouse) {
                        xNew = (int) Math.floor(anchorX + (x - xMouse));
                    }
                    if(y>yMouse) {
                        yNew = (int) Math.floor(anchorY + (y - yMouse));
                    }
                    
                    if(xNew >=0 && xNew < (scale*width) && (yNew>=0) && yNew < scale*height) {
                        updateBuffer.add(new Point(x, y));
                        int pixel = this.newImage[xNew][yNew][0];
                        this.image.setRGB(x,y,pixel);
                    }
                }
            }
        }
    }

}