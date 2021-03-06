/*
 * Performance Tester
 */
package de.deepsource.deepnotes.utilities;

import android.util.Log;

public final class PerformanceTester {

	private static int ppsCounter = 0;
	private static long startMilliseconds = 0;
	private static long stopMilliseconds = 0;
	private static String[] logList = new String[20];


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
		double time = (stopMilliseconds - startMilliseconds) / 1000d;
		double avePps = ppsCounter / time;
		/*Log.i("PerformanceTester:", "-----------------------------------");
		Log.i("Test Duration:", String.valueOf(time));
		Log.i("Hits:", String.valueOf(ppsCounter));
		Log.i("Average Points per Second:", String.valueOf(avePps));
		*/
		Log.e("Copy stats:", time + "\t"+ avePps);
		/*Log.i("PerformanceTester:", "-----------------------------------");*/

		//logList[logCounter++] = String.valueOf(time) + String.valueOf(avePps);

		reset();
	}

	public static void printLog(){
		for (String l : logList){
			Log.i("LogList", l);
		}
	}

	private static void reset(){
		ppsCounter = 0;
		startMilliseconds = 0;
		stopMilliseconds = 0;
	}
}
