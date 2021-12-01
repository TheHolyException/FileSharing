package de.minebug.filesharing.util;


import java.awt.image.BufferedImage;

public class PixelUtils {
	public static int getPixel(float x, float y, BufferedImage img){
		int width = img.getWidth();
		int height = img.getHeight();
		x *= width;
		y *= height;
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x >= width) x = width-1;
		if(y >= height) y = height-1;
		int x1 = (int)Math.floor(x);
		int x2 = (int)Math.ceil(x);
		int y1 = (int)Math.floor(y);
		int y2 = (int)Math.ceil(y);
		int[] c11 = getRawPixel(x1, y1, img);
		int[] c12 = getRawPixel(x1, y2, img);
		int[] c21 = getRawPixel(x2, y1, img);
		int[] c22 = getRawPixel(x2, y2, img);
		float xRel = x - x1;
		float a = lerp(xRel, c11[0], c12[0]);
		float r = lerp(xRel, c11[1], c12[1]);
		float g = lerp(xRel, c11[2], c12[2]);
		float b = lerp(xRel, c11[3], c12[3]);
		float a2 = lerp(xRel, c21[0], c22[0]);
		float r2 = lerp(xRel, c21[1], c22[1]);
		float g2 = lerp(xRel, c21[2], c22[2]);
		float b2 = lerp(xRel, c21[3], c22[3]);
		float yRel = y - y1;
		a = lerp(yRel, a, a2);
		r = lerp(yRel, r, r2);
		g = lerp(yRel, g, g2);
		b = lerp(yRel, b, b2);
		return ((int)a << 24) | ((int)r << 16) | ((int)g << 8) | ((int)b);
	}
	
	private static int[] getRawPixel(int x, int y, BufferedImage img) {
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x >= img.getWidth()) x = img.getWidth() - 1;
		if(y >= img.getHeight()) y = img.getHeight() - 1;
		int argb = img.getRGB(x, y);
		return new int[]{(argb >> 24) & 0xFF, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
	}

	public static int getPixel(float x, float y, Render2DEngine_NoGL img){
		int width = img.getWidth();
		int height = img.getHeight();
		x *= width;
		y *= height;
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x >= width) x = width-1;
		if(y >= height) y = height-1;
		int x1 = (int)Math.floor(x);
		int x2 = (int)Math.ceil(x);
		int y1 = (int)Math.floor(y);
		int y2 = (int)Math.ceil(y);
		int[] c11 = getRawPixel(x1, y1, img);
		int[] c12 = getRawPixel(x1, y2, img);
		int[] c21 = getRawPixel(x2, y1, img);
		int[] c22 = getRawPixel(x2, y2, img);
		float xRel = x - x1;
		float a = lerp(xRel, c11[0], c12[0]);
		float r = lerp(xRel, c11[1], c12[1]);
		float g = lerp(xRel, c11[2], c12[2]);
		float b = lerp(xRel, c11[3], c12[3]);
		float a2 = lerp(xRel, c21[0], c22[0]);
		float r2 = lerp(xRel, c21[1], c22[1]);
		float g2 = lerp(xRel, c21[2], c22[2]);
		float b2 = lerp(xRel, c21[3], c22[3]);
		float yRel = y - y1;
		a = lerp(yRel, a, a2);
		r = lerp(yRel, r, r2);
		g = lerp(yRel, g, g2);
		b = lerp(yRel, b, b2);
		return ((int)a << 24) | ((int)r << 16) | ((int)g << 8) | ((int)b);
	}
	
	private static int[] getRawPixel(int x, int y, Render2DEngine_NoGL img) {
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x >= img.getWidth()) x = img.getWidth() - 1;
		if(y >= img.getHeight()) y = img.getHeight() - 1;
		int argb = img.getPixel(x, y);
		return new int[]{(argb >> 24) & 0xFF, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
	}

	public static float lerp(float t, float a, float b) {
		return a + t * (b - a);
	}

	public static int getPixelFast(float x, float y, BufferedImage img) {
		int[] c = getRawPixel((int)(x * img.getWidth()), (int)(y * img.getHeight()), img);
		return 0xFF000000 | (c[1] << 16) | (c[2] << 8) | (c[3]);
	}
}

