package com.krishagni.catissueplus.core.common;

public class Pair<T, U> {
	private T first;
	
	private U second;
	
	public T first() {
		return first;
	}
	
	public U second() {
		return second;
	}
	
	public static <T, U> Pair<T, U> make(T first, U second) {
		Pair<T, U> pair = new Pair<T, U>();
		pair.first = first;
		pair.second = second;
		return pair;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null) {
			return false;
		}
		
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		Pair other = (Pair) obj;
		if (first == null) {
			if (other.first != null) {
				return false;
			}
		} else if (!first.equals(other.first)) {
			return false;
		}
		
		if (second == null) {
			return other.second == null;
		} else {
			return second.equals(other.second);
		}
	}
}
