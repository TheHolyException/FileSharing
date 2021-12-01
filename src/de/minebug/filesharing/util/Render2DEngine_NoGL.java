package de.minebug.filesharing.util;


import java.awt.image.BufferedImage;

public class Render2DEngine_NoGL {
	//private BufferedImage bimage;
	private int[] image;
	private final int w, h;
	private boolean alphaSupport = true;
	public boolean updated = true;

	public Render2DEngine_NoGL(int w, int h){
		//image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		this.w = w;
		this.h = h;
		image = new int[w * h];
	}
	
	public Render2DEngine_NoGL(int w, int h, int[] img){
		//image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		this.w = w;
		this.h = h;
		image = img.clone();
	}
	
	public Render2DEngine_NoGL(BufferedImage sourceImage){
		//image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		this.w = sourceImage.getWidth();
		this.h = sourceImage.getHeight();
		image = new int[w * h];
		sourceImage.getRGB(0, 0, w, h, image, 0, w);
	}
	
	public void setEnableAlphaSupport(boolean alphaSupport) {
		if(this.alphaSupport && !alphaSupport) for(int i=0; i<image.length; i++) image[i] |= 0xFF000000;
		this.alphaSupport = alphaSupport;
		
	}
	
	public boolean getSupportsAlpha() {
		return alphaSupport;
	}
	
	public int[] getRaw(){
		return image;
	}
	
	public void drawImage(BufferedImage img, float px, float py) {
		int x, y;
		int minX = Math.max(0, (int)px);
		int maxX = Math.min(w, img.getWidth() + (int)px);
		int minY = Math.max(0, (int)py);
		int maxY = Math.min(h, img.getHeight() + (int)py);
		if(alphaSupport) {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					//image.setRGB(x + (int)px, y + (int)py, mixColors(img.getRGB(x, y), image.getRGB(x + (int)px, y + (int)py)));
					drawPixel(x, y, img.getRGB(x - (int)px, y - (int)py));
				}
			}
		} else {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					this.image[y * this.w + x] = img.getRGB(x - (int)px, y - (int)py);
				}
			}
		}
		updated = true;
	}

	public void drawImage(BufferedImage img, float px, float py, float sx, float sy) {
		if(sx >= img.getWidth() && sy >= img.getHeight()) {
			drawImage_fast(img, px, py, sx, sy);
			//System.out.println("drawImage_fast");
			return;
		}
//		for(int y=0; y<(int)sy; y++){
//			for(int x=0; x<(int)sx; x++){
		int x, y;
		int minX = Math.max(0, (int)px);
		int maxX = Math.min(w, (int)(sx + px));
		int minY = Math.max(0, (int)py);
		int maxY = Math.min(h, (int)(sy + py));
		if(alphaSupport) {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					//System.out.println(x + " " + y + " " + sx + " " + sy + " " + image.getWidth());
					//image.setRGB(x + (int)px, y + (int)py, mixColors(Utils.getPixel(x / sx, y / sy, img), image.getRGB(x + (int)px, y + (int)py)));
					//drawPixel(x + px, y + py, PixelUtils.getPixel(x / sx, y / sy, img));
					drawPixel(x, y, PixelUtils.getPixel((x-px) / sx, (y-py) / sy, img));
				}
			}
		} else {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					this.image[y * this.w + x] = PixelUtils.getPixel((x-px) / sx, (y-py) / sy, img);
				}
			}
		}
		updated = true;
	}
	
	private void drawImage_fast(BufferedImage img, float px, float py, float sx, float sy) {
		int x, y;
		int minX = Math.max(0, (int)px);
		int maxX = Math.min(w, (int)(sx + px));
		int minY = Math.max(0, (int)py);
		int maxY = Math.min(h, (int)(sy + py));
		//for(y=0; y<(int)sy; y++){
		//	for(x=0; x<(int)sx; x++){
		if(alphaSupport) {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					//System.out.println(x + " " + y + " " + sx + " " + sy + " " + image.getWidth());
					//image.setRGB(x + (int)px, y + (int)py, mixColors(Utils.getPixelFast(x / sx, y / sy, img), image.getRGB(x + (int)px, y + (int)py)));
					//drawPixel(x, y, Utils.getPixelFast(x / sx, y / sy, img));
					//drawPixel(x + px, y + py, img.getRGB((int)(x / sx * img.getWidth()), (int)((float)y / sy * (float)img.getHeight())));
					drawPixel(x, y, img.getRGB((int)((x-px) / sx * img.getWidth() ), (int)((float)(y-py) / sy * img.getHeight() )));
					
				}
			}
		} else {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					this.image[y * this.w + x] = img.getRGB((int)((x-px) / sx * img.getWidth() ), (int)((float)(y-py) / sy * img.getHeight() )) | 0xFF000000;
				}
			}
		}
	}
	
	public void drawImage(Render2DEngine_NoGL img, float px, float py) {
		int x;
		int minX = Math.max(0, (int)px);
		int maxX = Math.min(w, img.getWidth() + (int)px);
		int y = Math.max(0, (int)py);
		int maxY = Math.min(h, img.getHeight() + (int)py);
		if(alphaSupport && img.alphaSupport) {
			while(y<maxY){
				for(x=minX; x<maxX; x++){
					//image.setRGB(x + (int)px, y + (int)py, mixColors(img.getRGB(x, y), image.getRGB(x + (int)px, y + (int)py)));
					drawPixel(x, y, img.getPixel(x - (int)px, y - (int)py));
				}
				y++;
			}
		} else {
			//int yy = minY - (int)py;
			//int xx = minX - (int)px;
			//maxX -= xx;
			quickDraw(0, y, maxX, maxY, minX, img, (int)px, (int)py);
		}
		updated = true;
	}
	
	private void quickDraw(int x, int y, int maxX, int maxY, int minX, Render2DEngine_NoGL img, int px, int py){
		while(y<maxY){
			for(x=minX; x<maxX; x++){
				this.image[y * this.w + x] = img.image[(y - py) * this.w + (x - px)];//.getPixel(x - (int)px, y - (int)py);
			}
			y++;
			//System.arraycopy(img.getRaw(), y * this.w + minX, this.image, yy * img.getWidth() + xx, maxX);
			//yy++;
		}
	}
	
	public void drawImage(Render2DEngine_NoGL img, float px, float py, float sx, float sy) {
		if(sx >= img.getWidth() && sy >= img.getHeight()) {
			drawImage_fast(img, px, py, sx, sy);
			//System.out.println("drawImage_fast");
			return;
		}
		int x, y;
		int minX = Math.max(0, (int)px);
		int maxX = Math.min(w, (int)(sx + px));
		int minY = Math.max(0, (int)py);
		int maxY = Math.min(h, (int)(sy + py));
		if(alphaSupport && img.alphaSupport) {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					drawPixel(x, y, PixelUtils.getPixel((x-px) / sx, (y-py) / sy, img));
				}
			}
		} else {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					this.image[y * this.w + x] = PixelUtils.getPixel((x-px) / sx, (y-py) / sy, img);
				}
			}
		}
		updated = true;
	}
	
	private void drawImage_fast(Render2DEngine_NoGL img, float px, float py, float sx, float sy) {
		int x, y;
		int minX = Math.max(0, (int)px);
		int maxX = Math.min(w, (int)(sx + px));
		int minY = Math.max(0, (int)py);
		int maxY = Math.min(h, (int)(sy + py));
		if(alphaSupport && img.alphaSupport) {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					drawPixel(x, y, img.getPixel((int)((x-px) / sx * img.getWidth() ), (int)((float)(y-py) / sy * img.getHeight() )));
				}
			}
		} else {
			for(y=minY; y<maxY; y++){
				for(x=minX; x<maxX; x++){
					this.image[y * this.w + x] = img.getPixel((int)((x-px) / sx * img.getWidth() ), (int)((float)(y-py) / sy * img.getHeight() ));
				}
			}
		}
	}

	public void drawFill(float x1, float y1, float x2, float y2, int color) {
		if(x1 > x2) {
			float a = x1;
			x1 = x2;
			x2 = a;
		}
		if(y1 > y2) {
			float a = y1;
			y1 = y2;
			y2 = a;
		}
		if(alphaSupport) {
			for(int y=(int)y1; y<y2; y++){
				for(int x=(int)x1; x<x2; x++){
					//image.setRGB(x, y, color);
					drawPixel(x, y, color);
				}
			}
		} else {
			for(int y=(int)y1; y<y2; y++){
				int yy = y*w;
				for(int x=(int)x1; x<x2; x++){
					image[x + yy] = color;
				}
			}
			
		}
		updated = true;
	}

	public void drawLine(float startX, float startY, float endX, float endY, int color) {
		  try {
			float a = Math.max(startY, endY) - Math.min(startY, endY);
			float step = a == 0 ? 2 : (float)(endX - startX) / a;
			float pointer=0;
			int start, end, b=0;
			if(step <= 1){
				pointer = startX;
				start = (int)startY;
				end = (int)endY;
				
				if(startY > endY){
					pointer = endX;
					step *= -1f;
					b = end;
					end = start;
					start = b;
				}
				
				if(start < 0) {
					pointer += step * (0 - start);
					start = 0;
				}
				//if(startY == endY) System.out.println("==");
				if(end > h) end = h;
				//if(step == 0) System.out.println(start + " -> " + end);
				for (int i=start; i<end; i++) {
				  drawPixel(pointer, i, color);
				  pointer+=step;
				}
				return;
			}
			a = Math.max(startX, endX) - Math.min(startX, endX);
			step=a == 0 ? 0 : (float)(endY - startY) / a;
			pointer = startY;
			start = (int)startX;
			end = (int)endX;
			if(startX > endX){
				pointer = endY;
				step *= -1f;
				a = end;
				end = start;
				start = b;
			}
			
			if(start < 0) {
				pointer += step * (0 - start);
				start = 0;
			}
			
			if(end > w) end = w;
			
			for (int i=start; i<end; i++) {
			  drawPixel(i, pointer, color);
			  pointer+=step;
			}
		  } 
		  catch(Exception e) {
			//println(e);
		  }
		updated = true;
	}
	
	public int getPixel(int x, int y){
		return image[x + y*w];
	}

	public void drawPixel(float px, float py, int color) {
		if(px >= 0 && py >= 0 && px < w && py < h) {//image.setRGB((int)px, (int)py, mixColors(color, image.getRGB((int)px, (int)py)));
			int p = (int)px + (int)py*w;
			image[p] = mixColors(color, image[p]);
		}
		updated = true;
	}
	public void onBeginUpdate() {}

	public void onEndUpdate() {}

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}
	
	public static int mixColors(int a, int b){
		int a1 = (a >> 24) & 0xFF;
		//return 0xFF000000 | 
		return 	((((b >> 24) & 0xFF) + (a1 * (255 - ((b >> 24) & 0xFF))/255)) << 24) |
				((fastLerp((a >> 16) & 0xFF, (b >> 16) & 0xFF, a1)) << 16) |
				((fastLerp((a >> 8) & 0xFF, (b >> 8) & 0xFF, a1)) << 8) |
				(fastLerp(a & 0xFF, b & 0xFF, a1));
		//((((b >> 24) & 0xFF) + (((a >> 24) & 0xFF) * (255 - ((b >> 24) & 0xFF))/255)) << 24);
		//((fastLerp((a >> 24) & 0xFF, (b >> 24) & 0xFF, (a >> 24) & 0xFF)) << 24);
	}
	
	private static int fastLerp(int b, int a, int x){
		return a + (x * (b - a) / 0xFF);
	}

}