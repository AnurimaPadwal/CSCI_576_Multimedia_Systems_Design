/*******************************************************
 * Name: ImageDisplay.java
 * Description: Interactive program to encode and decode 
 * an RGB image using JPEG compression. 
 * Input parameters: [Path/to/image] type: string
 *                   [Quantization level] type: int - [0,7]
 *                   [Delivery Mode] type: int
 * 							1 - Baseline delivery
 * 							2 - Progressive delivery using spectral selection
 * 							3 - Progressive delivery using successive bit approximation
 *                   [Latency] type: int
 *                          This simulates netwwork latency by "sleeping" for the
 * 							given time in milliseconds
 * Output: Decoded Image, per specified mode
 * Author: Anurima Anil Padwal 
 * For CSCI 576, HW Assignment 2 (Programmimg question)
 ******************************************************/
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.*;

class Point {
    int x;
    int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}


public class ImageDisplay {
	ArrayList <Point> diagonalCoords = new ArrayList<>();
	JFrame frame;
	JLabel lbIm1;

	static int width = 1920;
	static int height = 1080;
	static int N = 8;
	int num_blocks = (height * width)/(N * N);
	int num_diagonals = 2 * N - 2;

	GridBagConstraints c;

	static double [][][] originalImage = new double[height][width][3];
	double [][] cosines = new double[N][N];
	double [][][] block = new double[N][N][3];
	double [][][][] blocks = new double[num_blocks][N][N][3];
	double [][][][] encoded = new double[num_blocks][N][N][3];
	double [][][][] scan = new double[num_blocks][N][N][3];
	double [][][][] decoded = new double[num_blocks][N][N][3];
	double [][][][] sign_bits = new double[num_blocks][N][N][3];

	double [][][] dct = new double[N][N][3];
	double [][][] idct = new double[N][N][3];
	double [][][] quant = new double[N][N][3];
	double [][][] dequant = new double[N][N][3];

	private static final double C = 1/Math.sqrt(2);

	double [][][] encodedImageMatrix = new double[height][width][3];
	double [][] decodedImageMatrix = new double[height][width];

	BufferedImage encodedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	BufferedImage decodedImage;

	public int getBit(int n, int k) {
		int mask = 1 << k;
		return ((n & mask) >> k);
	}

	public void computeDCT(double [][][] image, int idx) {
		for(int i = 0; i< N; i++) {
			for(int j = 0; j < N; j++) {
				double sum = 0;
				double coefficient_i = 1;
				double coefficient_j = 1;
				if(i == 0) coefficient_i = C;
				if(j == 0) coefficient_j = C;

				for(int x = 0; x < N; x ++) {
					for(int y = 0; y < N; y++) {
						sum += image[x][y][idx] * cosines[x][i] * cosines[y][j];
					}
				}

				dct[i][j][idx] = Math.floor(0.25 *  coefficient_i * coefficient_j * sum);
			}
		}
	}

	 

	public void computeIDCT(int idx) {
		for(int x = 0; x < N; x++) {
			for(int y = 0; y < N; y++) {

				double sum = 0;
				double coefficient_u = 1, coefficient_v = 1;

				for(int u = 0; u < N; u++){
					for(int v = 0; v < N; v++) {
						if(u == 0) coefficient_u = C;
						if(v == 0) coefficient_v = C;

						sum += coefficient_u * coefficient_v * dequant[u][v][idx] * cosines[x][u] * cosines[y][v];

					}
				}

				idct[x][y][idx] = Math.floor(0.25 * sum);
			}
		}

	}

	public void quantizeBlock(int n, int idx) {
		double factor = Math.pow(2, n);
		for (int r = 0; r < N; r ++) {
			for(int c = 0; c < N; c++) {
				quant[r][c][idx] = dct[r][c][idx]/factor;
			}
		}

	}

	public void dequantizeBlock(int n, int idx) {
		double factor = Math.pow(2, n);
		for (int r = 0; r < N; r ++) {
			for(int c = 0; c < N; c++) {
				dequant[r][c][idx] = quant[r][c][idx] * factor;
			}
		}

	}

	public void dequantizeBlocks(int level) {
		double factor = Math.pow(2,level);
		for(int n = 0; n < num_blocks; n++) {
			for(int i = 0; i< N; i++) {
				for(int j = 0; j < N; j++) {
					scan[n][i][j][0] = encoded[n][i][j][0]  * factor;
					scan[n][i][j][1] = encoded[n][i][j][1] * factor;
					scan[n][i][j][2] = encoded[n][i][j][2] * factor;
				}
			}
		}
	}

	public void computeCosines() {
		for(int i=0; i<N;i++) {
			for(int j = 0; j<N; j++) {
				cosines[i][j] = Math.cos(((2 * i + 1) * j * Math.PI)/16);
			}
		}

	}

	public void computeForMode3(int level, int latency) {
		getAllBlocks();

		encodeAllBlocks(level);

		dequantizeBlocks(level);

		for (int p = 12; p >=0; p--) {
			int cnt = -1;
			for(int i = 0; i < height/N; i++) {
				for(int j = 0; j <width/N; j++) {
					cnt+=1;

					

					for(int r = 0; r < N; r++) {
						for(int c = 0; c < N; c++) {
							if(scan[cnt][r][c][0] < 0) {
								int bit0 = getBit((int)(-scan[cnt][r][c][0]), p);
								sign_bits[cnt][r][c][0] -= bit0 * Math.pow(2,p);

							}
							else{
								int bit0 = getBit((int)(scan[cnt][r][c][0]), p);
								sign_bits[cnt][r][c][0] += bit0 * Math.pow(2,p);
							}
							if(scan[cnt][r][c][1] < 0) {
								int bit1 = getBit((int)(-scan[cnt][r][c][1]), p);
								sign_bits[cnt][r][c][1] -= bit1 * Math.pow(2,p);

							}
							else{
								int bit1 = getBit((int)(scan[cnt][r][c][1]), p);
								sign_bits[cnt][r][c][1] += bit1 * Math.pow(2,p);
							}
							if(scan[cnt][r][c][2] < 0) {
								int bit2 = getBit((int)(-scan[cnt][r][c][2]), p);
								sign_bits[cnt][r][c][2] -= bit2 * Math.pow(2,p);

							}
							else{
								int bit2 = getBit((int)(scan[cnt][r][c][2]), p);
								sign_bits[cnt][r][c][2] += bit2 * Math.pow(2,p);
							}
							
							
						}
					}
					
					for(int x = i*8, r=0; x < i*8 + 8; x++,r++) {
						for(int y = j*8, c = 0; y < j*8 + 8; y++, c++) {
							double sum0 = 0;
							double sum1 = 0;
							double sum2 = 0;
							double coefficient_u = 1, coefficient_v = 1;

							for(int u = 0; u < N; u++){
								for(int v = 0; v < N; v++) {
									if(u == 0) coefficient_u = C;
									if(v == 0) coefficient_v = C;

									// sum0 += coefficient_u * coefficient_v * sign_bits[cnt][u][v][0] * Math.cos(((2 * r + 1)*u*Math.PI)/16) * Math.cos(((2 * c + 1) * v * Math.PI)/16);
									// sum1 += coefficient_u * coefficient_v * sign_bits[cnt][u][v][1] * Math.cos(((2 * r + 1)*u*Math.PI)/16) * Math.cos(((2 * c + 1) * v * Math.PI)/16);
									// sum2 += coefficient_u * coefficient_v * sign_bits[cnt][u][v][2] * Math.cos(((2 * r + 1)*u*Math.PI)/16) * Math.cos(((2 * c + 1) * v * Math.PI)/16);

									sum0 += coefficient_u * coefficient_v * sign_bits[cnt][u][v][0] * cosines[r][u] * cosines[c][v];
									sum1 += coefficient_u * coefficient_v * sign_bits[cnt][u][v][1] * cosines[r][u] * cosines[c][v];
									sum2 += coefficient_u * coefficient_v * sign_bits[cnt][u][v][2] * cosines[r][u] * cosines[c][v];


								}
							}

							int red = (int) Math.floor(0.25 * sum0);
							int green = (int) Math.floor(0.25 * sum1);
							int blue = (int) Math.floor(0.25 * sum2);
							
							if(red < 0) red = 0;
							else if(red > 255) red = 255;
							if(green < 0) green = 0;
							else if(green > 255) green = 255;
							if(blue < 0) blue = 0;
							else if(blue > 255) blue = 255;
							
	 						int pixel = (red << 16) | (green << 8) | (blue);

							this.decodedImage.setRGB(y, x, pixel);
							//System.out.println(x + " " + y + " " + pixel);
							 


						}
					}

				}
			}
			showImageFrame();
			//System.out.println("Display Image");
			try {
				Thread.sleep(latency);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	public void computeForMode2(int level, int latency) {
		getAllBlocks();
		 
		encodeAllBlocks(level);

		getDiagonals(); 

		int red, green, blue, pixel;
	 	for(Point p: diagonalCoords) {
			int row = p.x;
			int col  = p.y;
	 		int ct = -1;
	 		for(int i = 0; i < (height/N); i++) {
	 			for(int j = 0; j < (width/N); j++) {
					ct+=1;
				 
				 	decodeBySpectralSelection(row, col, ct, level);
					int flag = 0;
	 				for(int x = i*8, r = 0; x < i*8 + 8; x++, r++) {
						if(flag == 1) break;
	 					for(int y = j*8, c = 0; y < j*8 + 8; y++, c++) {
	 						if(r == row && c == col) {
								flag = 1;
	 							red = (int) Math.floor(decoded[ct][r][c][0]);
	 							green = (int) Math.floor(decoded[ct][r][c][1]);
								blue = (int) Math.floor(decoded[ct][r][c][2]);

								if(red < 0) red = 0;
								else if(red > 255) red = 255;
								if(green < 0) green = 0;
								else if(green > 255) green = 255;
								if(blue < 0) blue = 0;
								else if(blue > 255) blue = 255;
	 							pixel = (red << 16) | (green << 8) | (blue);

	 							this.decodedImage.setRGB(y, x, pixel);

	 					}
						
	 				}
	 			}

				
	 		}
	 	}


		showImageFrame();
		try {
			Thread.sleep(latency);
		}
		catch(InterruptedException ie) {
			System.out.println(ie.getMessage());
		}
	}


	 	
}

public void decodeBySpectralSelection(int row, int col, int b, int level) {
	double factor = Math.pow(2, level);
	double temp0 = 0.0;
	double temp1 = 0.0;
	double temp2 = 0.0;
	double coefficient_u = 1;
	double coefficient_v = 1;
	scan[b][row][col][0] = encoded[b][row][col][0] * factor;
	scan[b][row][col][1] = encoded[b][row][col][1] * factor;
	scan[b][row][col][2] = encoded[b][row][col][2] * factor;
		 
	for(int u = 0; u < N; u++) {
	 	for(int v = 0; v < N; v++) {
	 		if(u == 0) coefficient_u = C;
	 		if(v == 0) coefficient_v = C;
				
	 		// temp0 += coefficient_u * coefficient_v * scan[b][u][v][0] * Math.cos(((2 * row + 1)*u*Math.PI)/16) * Math.cos(((2 * col + 1) * v * Math.PI)/16);
	 		// temp1 += coefficient_u * coefficient_v * scan[b][u][v][1] * Math.cos(((2 * row + 1)*u*Math.PI)/16) * Math.cos(((2 * col + 1) * v * Math.PI)/16);
			// temp2 += coefficient_u * coefficient_v * scan[b][u][v][2] * Math.cos(((2 * row + 1)*u*Math.PI)/16) * Math.cos(((2 * col + 1) * v * Math.PI)/16);
			 
			temp0 += coefficient_u * coefficient_v * scan[b][u][v][0] * cosines[row][u] * cosines[col][v];
	 		temp1 += coefficient_u * coefficient_v * scan[b][u][v][1] * cosines[row][u] * cosines[col][v];
	 		temp2 += coefficient_u * coefficient_v * scan[b][u][v][2] * cosines[row][u] * cosines[col][v];
	 	
	 	}
	}

	decoded[b][row][col][0] = 0.25 * temp0;
	decoded[b][row][col][1] = 0.25 * temp1;
	decoded[b][row][col][2] = 0.25 * temp2;
}





public void computeForMode1(int mode, int n, int latency){
	for(int i = 0; i < (height/N); i++) {
		for(int j = 0; j < (width/N); j++) {
			block = new double[N][N][3];
			for(int x = i*8, r = 0; x < i*8 + 8; x++, r++) {
				for(int y = j*8, c = 0; y < j*8 + 8; y++, c++) {
					block[r][c][0] = originalImage[x][y][0];
					block[r][c][1] = originalImage[x][y][1];
					block[r][c][2] = originalImage[x][y][2];
	
				}
			}

			computeDCT(block, 0);
			computeDCT(block, 1);
			computeDCT(block, 2);

			quantizeBlock(n, 0);
			quantizeBlock(n, 1);
			quantizeBlock(n, 2);
				
			dequantizeBlock(n, 0);
			dequantizeBlock(n, 1);
			dequantizeBlock(n, 2);

			computeIDCT(0);
			computeIDCT(1);
			computeIDCT(2);

				
			int pixel, red, green, blue;
			for(int x = i*8, r = 0; x < (i*8 + 8); x++, r++) {
				for(int y = j*8, c = 0; y < (j*8 + 8); y++, c++) {
					red = (int) Math.floor(idct[r][c][0]);
					green = (int) Math.floor(idct[r][c][1]);
					blue = (int) Math.floor(idct[r][c][2]);
					if(red < 0) red = 0;
					else if(red > 255) red = 255;
					if(green < 0) green = 0;
					else if(green > 255) green = 255;
					if(blue < 0) blue = 0;
					else if(blue > 255) blue = 255;
					pixel = (red << 16) | (green << 8) | (blue);
					this.decodedImage.setRGB(y,x,pixel);

				}
			}

			showImageFrame();
			try {
				Thread.sleep(latency);
			}
			catch(InterruptedException ie) {
				System.out.println(ie.getMessage());
			}
					
		}
	}

}

	public void getAllBlocks() {
		int count = -1;
		for(int i = 0; i < height/N; i++) {
			for(int j = 0; j < width/N; j++) {
				count+=1;

				for(int x = i*8, r = 0; x < i*8 + 8; x++, r++) {
					for(int y = j*8, c = 0; y < j*8 + 8; y++, c++) {
	 					blocks[count][r][c][0] = originalImage[x][y][0];
	 					blocks[count][r][c][1] = originalImage[x][y][1];
	 					blocks[count][r][c][2] = originalImage[x][y][2];
	 				}

				}
			}
		}
	}

	public void encodeAllBlocks(int level) {
		double factor = Math.pow(2,level);
		for(int n = 0; n < num_blocks; n++) {
			for(int i = 0; i< N; i++) {
				for(int j = 0; j < N; j++) {
					double sum0 = 0, sum1 = 0, sum2 = 0;
					double coefficient_i = 1;
					double coefficient_j = 1;
					if(i == 0) coefficient_i = C;
					if(j == 0) coefficient_j = C;

					for(int x = 0; x < N; x ++) {
						for(int y = 0; y < N; y++) {
							// sum0 += blocks[n][x][y][0] * Math.cos(((2 * x + 1) * i * Math.PI)/16) * Math.cos(((2 * y + 1) * j * Math.PI)/16);
							// sum1 += blocks[n][x][y][1] * Math.cos(((2 * x + 1) * i * Math.PI)/16) * Math.cos(((2 * y + 1) * j * Math.PI)/16);
							// sum2 += blocks[n][x][y][2] * Math.cos(((2 * x + 1) * i * Math.PI)/16) * Math.cos(((2 * y + 1) * j * Math.PI)/16);

							sum0 += blocks[n][x][y][0] * cosines[x][i] * cosines[y][j];
							sum1 += blocks[n][x][y][1] * cosines[x][i] * cosines[y][j];
							sum2 += blocks[n][x][y][2] * cosines[x][i] * cosines[y][j];

							
						}	
					}

					encoded[n][i][j][0] = Math.floor(0.25 *  coefficient_i * coefficient_j * sum0);
					encoded[n][i][j][1] = Math.floor(0.25 *  coefficient_i * coefficient_j * sum1);
					encoded[n][i][j][2] = Math.floor(0.25 *  coefficient_i * coefficient_j * sum2);

					encoded[n][i][j][0] /= factor;
					encoded[n][i][j][1] /= factor;
					encoded[n][i][j][2] /= factor;

				}
			}
	
		}

	}

	public void getDiagonals() {
		for(int n = 0; n <= num_diagonals; n++) {
			for(int r = 0; r < N; r++) {
				for(int c = 0; c < N; c++) {
					if (r+c == n) {
						diagonalCoords.add((new Point(r,c)));
					}
				}
			}
		}
	}

public BufferedImage readImageRGB(String imgPath, BufferedImage img){
	int height = img.getHeight();
	int width = img.getWidth();
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					
					img.setRGB(x,y,pix);
					ind++;
				}
			}

			
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		return img;
	}

	public void showIms(BufferedImage image){
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		frame.setPreferredSize(new Dimension(width, height));
		lbIm1 = new JLabel(new ImageIcon(image));

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
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}


	public void showImageFrame() {
        lbIm1.setIcon(new ImageIcon(this.decodedImage));        
        frame.getContentPane().add(lbIm1, c);   
        frame.pack();
        frame.setVisible(true);
	}


	

	public static void main(String[] args) {
		System.out.println("Running...");
		long start = System.currentTimeMillis();
		ImageDisplay ren = new ImageDisplay();
		ren.computeCosines();

		String imagePath = args[0];
		int quantizationLevel = Integer.parseInt(args[1]);
		int deliveryMode = Integer.parseInt(args[2]);
		int latency = Integer.parseInt(args[3]);


		BufferedImage imageFile = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imageFile = ren.readImageRGB(imagePath, imageFile);
		ren.showIms(imageFile);
		BufferedImage result;

		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				int pixel = imageFile.getRGB(j,i);
				int r = ((pixel & 0x00ff0000) >> 16);
                int g = ((pixel & 0x00ff00) >> 8);
				int b = (pixel & 0x000000ff);
				
				originalImage[i][j][0] = r;
				originalImage[i][j][1] = g;
				originalImage[i][j][2] = b;
			}
		}
		ren.decodedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ren.showIms(ren.decodedImage);

		switch(deliveryMode) {
			case 1: ren.computeForMode1(deliveryMode, quantizationLevel, latency);
					break;
			case 2: ren.computeForMode2(quantizationLevel, latency);
					//ren.computeMode2(quantizationLevel, latency);
					break;

			case 3: ren.computeForMode3(quantizationLevel, latency);
					break;

		}

		long end = System.currentTimeMillis();
		long duration = end - start;
		System.out.println("Image decoded successfully in mode "+deliveryMode+ ". Elapsed Time: "+duration);

	}

}
