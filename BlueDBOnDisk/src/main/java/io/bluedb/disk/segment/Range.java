package io.bluedb.disk.segment;

import java.io.File;
import java.util.Collection;

import io.bluedb.disk.Blutils;

public final class Range implements Comparable<Range> {

	private final long start;
	private final long end;
	
	public Range(long start, long end) {
		this.start = start;
		this.end = end;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public long length() {
		return getEnd() - getStart() + 1;
	}

	public boolean containsInclusive(long point) {
		return point >= start && point <= end;
	}

	public boolean encloses(Range other) {
		return getStart() <= other.getStart() && getEnd() >= other.getEnd();
	}

	public boolean overlaps(Range otherRange) {
		return start <= otherRange.end && end >= otherRange.start;
	}

	public boolean overlapsAny(Collection<Range> otherRanges) {
		for (Range otherRange: otherRanges) {
			if (overlaps(otherRange)) {
				return true;
			}
		}
		return false;
	}

	public String toUnderscoreDelimitedString() {
		return start + "_" + end;
	}

	public static Range fromFileWithUnderscoreDelmimitedName(File file) {
		String fileName = file.getName();
		return fromUnderscoreDelmimitedString(fileName);
	}

	public static Range fromUnderscoreDelmimitedString(String string) {
		try {
			String[] parts = string.split("_");
			long start = Long.valueOf(parts[0]);
			long end = Long.valueOf(parts[1]);
			return new Range(start, end);
		} catch (Throwable t) {
			return null;
		}
	}

	public static Range forValueAndRangeSize(long value, long rangeSize) {
		long low = Blutils.roundDownToMultiple(value, rangeSize);
		long high = Math.min(Long.MAX_VALUE - rangeSize + 1, low) + rangeSize - 1;  // prevent overflow
		return new Range(low, high);
	}

	@Override
	public String toString() {
		return "Range [start=" + start + ", end=" + end + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (end ^ (end >>> 32));
		result = prime * result + (int) (start ^ (start >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Range) {
			Range other = (Range) obj;
			return (end == other.end) && (start == other.start); 
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(Range other) {
		if (this.start > other.start) {
			return 1;
		} else if (this.start < other.start) {
			return -1;
		} else {
			if (this.end > other.end) {
				return 1;
			} else if (this.end < other.end){
				return -1;
			} else {
				return 0;
			}
		}
	}
}
