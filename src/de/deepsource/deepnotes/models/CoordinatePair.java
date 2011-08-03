/**
 * 
 */
package de.deepsource.deepnotes.models;

/**
 * @author Sebastian Ullrich (sebastian.ullrich@deepsource.de)
 *
 */
public class CoordinatePair {
	
	private float x;
	private float y;
	
	/**
	 * Constructor.
	 * @param x x-coordinate.
	 * @param y y-coordinate.
	 */
	public CoordinatePair(float x, float y){
		setX(x);
		setY(y);
	}

	/**
	 * @param x the x to set
	 */
	public void setX(float x) {
		this.x = x;
	}

	/**
	 * @return the x
	 */
	public float getX() {
		return x;
	}

	/**
	 * @param y the y to set
	 */
	public void setY(float y) {
		this.y = y;
	}

	/**
	 * @return the y
	 */
	public float getY() {
		return y;
	}
	
	
	public boolean isValid(){
		if (x >= 0f)
			return true;
		else
			return false;
	}
	
}
