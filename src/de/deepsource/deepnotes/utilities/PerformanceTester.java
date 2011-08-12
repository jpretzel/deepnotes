package de.deepsource.deepnotes.utilities;

import android.util.Log;

public final class PerformanceTester {

	private static int ppsCounter = 0;
	private static long startMilliseconds = 0;
	private static long stopMilliseconds = 0;
	
	public static void start(){
		startMilliseconds = System.currentTimeMillis();
	}
	
	public static void stop(){
		stopMilliseconds = System.currentTimeMillis();
	}
	
	public static void hit(){
		ppsCounter++;
	}
	
	public static void printMessurement(){
		double time = (double)(stopMilliseconds - startMilliseconds) / 1000d;
		double avePps = (double)ppsCounter / (double)time;
		Log.i("PerformanceTester:", "-----------------------------------");
		Log.i("Test Duration:", String.valueOf(time));
		Log.i("Hits:", String.valueOf(ppsCounter));
		Log.i("Average Points per Second:", String.valueOf(avePps));
		Log.i("PerformanceTester:", "-----------------------------------");
		reset();
	}
	
	private static void reset(){
		ppsCounter = 0;
		startMilliseconds = 0;
		stopMilliseconds = 0;
	}
}
