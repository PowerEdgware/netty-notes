package com.netty02;

import java.util.ArrayList;
import java.util.List;

public class IntArrayTest {

	  private static final int[] SIZE_TABLE;

	    static {
	        List<Integer> sizeTable = new ArrayList<Integer>();
	        for (int i = 16; i < 512; i += 16) {
	            sizeTable.add(i);
	        }

	        for (int i = 512; i > 0; i <<= 1) {
	            sizeTable.add(i);
	        }

	        SIZE_TABLE = new int[sizeTable.size()];
	        for (int i = 0; i < SIZE_TABLE.length; i ++) {
	            SIZE_TABLE[i] = sizeTable.get(i);
	        }
	    }
	    
	    private static int getSizeTableIndex(final int size) {
	        for (int low = 0, high = SIZE_TABLE.length - 1;;) {
	            if (high < low) {
	                return low;
	            }
	            if (high == low) {
	                return high;
	            }

	            int mid = low + high >>> 1;
	            int a = SIZE_TABLE[mid];
	            int b = SIZE_TABLE[mid + 1];
	            if (size > b) {
	                low = mid + 1;
	            } else if (size < a) {
	                high = mid - 1;
	            } else if (size == a) {
	                return mid;
	            } else {
	                return mid + 1;
	            }
	        }
	    }
	    
	    public static void main(String[] args) {
//			new IntArrayTest();
	    	int size=IntArrayTest.getSizeTableIndex(1024);
	    	System.out.println(size);
	    	System.out.println(SIZE_TABLE[size]);
		}
}
