package com.JJIN.domain.place.tourapi.dto;

import java.util.Collections;
import java.util.List;

public record TourApiPage<T>(
	List<T> items,
	int pageNo,
	int numOfRows,
	int totalCount
) {
	public TourApiPage {
		items = items == null ? Collections.emptyList() : List.copyOf(items);
	}

	public static <T> TourApiPage<T> empty() {
		return new TourApiPage<>(Collections.emptyList(), 1, 0, 0);
	}

	public boolean hasNext() {
		return pageNo > 0 && numOfRows > 0 && pageNo * numOfRows < totalCount;
	}
}
