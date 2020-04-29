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

To compile:
javac ImageDisplay.java

To run:

java ImageDisplay [path\to\image] [quantization_level] [delivery_mode] [latency]

e.g.

java ImageDisplay .\miamibeach.rgb 7 3 1