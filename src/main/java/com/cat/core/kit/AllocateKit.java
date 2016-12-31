package com.cat.core.kit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AllocateKit {

	public static int allocate(int min, Collection<Integer> collection) {
		if (ValidateKit.isEmpty(collection)) {
			return min;
		}
		List<Integer> list = new ArrayList<>(collection);
		Collections.sort(list);

		if (list.get(0) > min) {
			return min;
		}

		int lastIndex = list.size() - 1;

		for (int i = 0; i < lastIndex; i++) {
			int current = list.get(i);
			if (current + 1 < list.get(i + 1)) {
				return current + 1;
			}
		}

		return list.get(lastIndex) + 1;
	}
}
