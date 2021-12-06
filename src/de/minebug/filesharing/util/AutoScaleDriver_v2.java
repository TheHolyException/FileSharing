package de.minebug.filesharing.util;


public class AutoScaleDriver_v2 {

	private Render2DEngine_NoGL texture1;
	private int w, h;
	private double zoom, maxZoom;

	private int zoomPosX = 0, zoomPosY = 0;
	
	private boolean needsCleanBackground = true;
	public int mode = 1;
	
	public AutoScaleDriver_v2(Render2DEngine_NoGL texture) {
		this.texture1 = texture;
	}
	
	public void onRender(Render2DEngine_NoGL graphics) {
		if(mode == 0){
			//graphics.drawImage(texture1, 0, 0, w, h);
			return;
		}
		if(mode == 1) {
			float scaleY = getScale();
			float scaleX = scaleY;
			if(needsCleanBackground) {
				//graphics.drawFill(0, 0, w, h, 0xFF000000);
				needsCleanBackground = false;
			}
			if(texture1 != null){
				graphics.drawImage(texture1, -zoomPosX, -zoomPosY, getTexWidth() * scaleX, getTexHeight() * scaleY);
			}
		}
		if(mode == 2) {
			if(texture1 != null){
				double scaleX = (double)texture1.getWidth() / (double)w;
				double scaleY = (double)texture1.getHeight() / (double)h;
				scaleX = Math.min(scaleX, scaleY);//crop outer (fill entire screen)
				int w2 = (int)((double)texture1.getWidth() / (double)scaleX);
				int h2 = (int)((double)texture1.getHeight() / (double)scaleX);
				graphics.drawImage(texture1, (w - w2) / 2, (h - h2) / 2, w2, h2);
			}
		}
	}
	
	
	public float getScale(){
		if(zoom == 0) return 1;
		return (float)(1f / (zoom / 10f));
	}
	
	public void onMouseScroll(boolean downwards, int mouseX, int mouseY) {
		double scale_old = getScale();
		if(downwards){
			if(zoom > 2) {
				zoom--;
			} else {
				zoom = 1;
			}
		} else {
			if(zoom < maxZoom) {
				zoom++;
			} else {
				zoom = maxZoom;
			}
		}
		double scale_new = getScale();
		//  -zoomPosX ... mouse ... -zoomPosX + (int)(getTexHeight() * scale)
		System.out.println("wh: " + w + " " + h);
		System.out.println("img-wh: " + (int)(getTexWidth() * scale_old) + " " + (int)(getTexHeight() * scale_old));
		System.out.println("zoomPos: " + zoomPosX + " " + zoomPosY);
		int mousePosOnImageX = (int)(mouseX/* / scale_old*/ + /*(int)(getTexWidth() * scale_old) -*/ zoomPosX);
		int mousePosOnImageY = (int)(mouseY/* / scale_old*/ + /*(int)(getTexHeight() * scale_old) -*/ zoomPosY);
		zoomPosX = (int)((zoomPosX + mouseX) / scale_old * scale_new) - mouseX;
		zoomPosY = (int)((zoomPosY + mouseY) / scale_old * scale_new) - mouseY;
		System.out.println("mousePosOnImage: " + mousePosOnImageX + " " + mousePosOnImageY);
		zoomPosX += 0;
		zoomPosY += 0;
		autoFitIntoBounds();
	}
	
	public void onMouseMoved(int x1, int y1, int x2, int y2) {
		zoomPosX -= x2 - x1;
		zoomPosY -= y2 - y1;
		autoFitIntoBounds();
	}
	
	public void onResize(int width, int height) {
		w = width;
		h = height;
		double scaleX = (double)getTexWidth() / (double)w;
		double scaleY = (double)getTexHeight() / (double)h;
		boolean autoResize = zoom == maxZoom;
		maxZoom = Math.max(scaleX, scaleY) * 10;
		if(zoom == 0 || autoResize) zoom = maxZoom;
		autoFitIntoBounds();
	}
	
	private void autoFitIntoBounds(){
		double scale = getScale();
		int w2 = (int)((double)getTexWidth() * scale);
		int h2 = (int)((double)getTexHeight() * scale);
		if(zoomPosX > w2 - w) zoomPosX = w2 - w;
		if(zoomPosY > h2 - h) zoomPosY = h2 - h;
		if(zoomPosX < 0) {
			zoomPosX = w2 < w ? (w2 / 2) - (w / 2) : 0;
		}
		if(zoomPosY < 0) {
			zoomPosY = h2 < h ? (h2 / 2) - (h / 2) : 0;
		}
		needsCleanBackground = true;
	}
	
	public void updateTexture(Render2DEngine_NoGL texture){
		this.texture1 = texture;
	}
	
	private int getTexHeight(){
		if(texture1 != null) return texture1.getHeight();
		return 0;
	}
	
	private int getTexWidth(){
		if(texture1 != null) return texture1.getWidth();
		return 0;
	}
}
