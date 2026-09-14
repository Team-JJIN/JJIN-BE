package com.JJIN.domain.recommendation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.JJIN.domain.recommendation.dto.ScoredCandidate;
import com.JJIN.global.geo.GeoPoint;
import com.JJIN.global.geo.GeoUtils;

/**
 * 스코어링된 후보를 좌표 기반으로 여행 일수(k)만큼 지리적 클러스터로 묶는다.
 * 클러스터 = 하루로 배정해 LLM이 하루 안에서 순서만 정하도록 하고, 일자 간 원거리 점프를 막는다.
 *
 * k-means(결정론적 초기화: 서로 가장 먼 k개 시드)로 안정적인 배정을 보장한다.
 */
@Component
public class CourseClusterer {

	private static final int MAX_ITERATIONS = 20;

	/**
	 * @return placeId → dayGroup(1..k). 후보가 없거나 k<=1이면 모두 1일차로 배정.
	 */
	public Map<Long, Integer> clusterByDay(final List<ScoredCandidate> scored, final int tripDays) {
		Map<Long, Integer> dayByPlaceId = new HashMap<>();
		int k = Math.max(1, tripDays);
		if (scored.isEmpty()) {
			return dayByPlaceId;
		}
		if (k == 1 || scored.size() <= k) {
			// 후보가 일수 이하면 굳이 나누지 않고 1일차부터 순차 배정
			int day = 1;
			for (ScoredCandidate s : scored) {
				dayByPlaceId.put(s.candidate().placeId(), Math.min(day++, k));
			}
			return dayByPlaceId;
		}

		List<GeoPoint> points = scored.stream().map(s -> s.candidate().location()).toList();
		List<GeoPoint> centroids = initialCentroids(points, k);
		int[] assignment = new int[points.size()];

		for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
			boolean changed = assignPoints(points, centroids, assignment);
			recomputeCentroids(points, assignment, centroids, k);
			if (!changed) {
				break;
			}
		}

		// 용량 균형 재배정: 특정 날에 후보가 몰려 다른 날이 지나치게 얇아지는 것을 막는다.
		// 후보가 모두 좁은 지역(선정 시군구)에 모여 있으므로 균형을 맞춰도 거리 손해가 작다.
		balanceAssignment(points, centroids, assignment, k);

		for (int i = 0; i < scored.size(); i++) {
			dayByPlaceId.put(scored.get(i).candidate().placeId(), assignment[i] + 1);
		}
		return dayByPlaceId;
	}

	/**
	 * 각 클러스터 크기를 ⌈N/k⌉ 이하로 제한해 균형을 맞춘다.
	 * 정원 초과 클러스터에서 중심으로부터 가장 먼 후보를, 여유가 있는 가장 가까운 클러스터로 옮긴다.
	 */
	private void balanceAssignment(
		final List<GeoPoint> points,
		final List<GeoPoint> centroids,
		final int[] assignment,
		final int k
	) {
		int capacity = (int) Math.ceil((double) points.size() / k);

		for (int pass = 0; pass < points.size(); pass++) {
			int[] counts = new int[k];
			for (int a : assignment) {
				counts[a]++;
			}
			int over = -1;
			for (int c = 0; c < k; c++) {
				if (counts[c] > capacity) {
					over = c;
					break;
				}
			}
			if (over == -1) {
				return;
			}

			// 초과 클러스터에서 중심 대비 가장 먼 후보 하나를, 여유 있는 가장 가까운 클러스터로 이동
			int moveIdx = -1;
			double worstDist = -1;
			for (int i = 0; i < points.size(); i++) {
				if (assignment[i] == over) {
					double dist = GeoUtils.haversineKm(points.get(i), centroids.get(over));
					if (dist > worstDist) {
						worstDist = dist;
						moveIdx = i;
					}
				}
			}
			int target = nearestClusterWithRoom(points.get(moveIdx), centroids, counts, capacity, over);
			assignment[moveIdx] = target == -1 ? over : target;
			if (target == -1) {
				return;
			}
		}
	}

	private int nearestClusterWithRoom(
		final GeoPoint point,
		final List<GeoPoint> centroids,
		final int[] counts,
		final int capacity,
		final int exclude
	) {
		int best = -1;
		double bestDist = Double.MAX_VALUE;
		for (int c = 0; c < centroids.size(); c++) {
			if (c == exclude || counts[c] >= capacity) {
				continue;
			}
			double dist = GeoUtils.haversineKm(point, centroids.get(c));
			if (dist < bestDist) {
				bestDist = dist;
				best = c;
			}
		}
		return best;
	}

	/** 서로 가장 멀리 떨어진 k개 지점을 초기 중심으로 선택(결정론적). */
	private List<GeoPoint> initialCentroids(final List<GeoPoint> points, final int k) {
		List<GeoPoint> centroids = new ArrayList<>();
		centroids.add(points.get(0));
		while (centroids.size() < k) {
			GeoPoint farthest = null;
			double maxMinDist = -1;
			for (GeoPoint p : points) {
				double minDist = centroids.stream()
					.mapToDouble(c -> GeoUtils.haversineKm(p, c))
					.min().orElse(0);
				if (minDist > maxMinDist) {
					maxMinDist = minDist;
					farthest = p;
				}
			}
			centroids.add(farthest);
		}
		return centroids;
	}

	private boolean assignPoints(
		final List<GeoPoint> points,
		final List<GeoPoint> centroids,
		final int[] assignment
	) {
		boolean changed = false;
		for (int i = 0; i < points.size(); i++) {
			int best = 0;
			double bestDist = Double.MAX_VALUE;
			for (int c = 0; c < centroids.size(); c++) {
				double dist = GeoUtils.haversineKm(points.get(i), centroids.get(c));
				if (dist < bestDist) {
					bestDist = dist;
					best = c;
				}
			}
			if (assignment[i] != best) {
				assignment[i] = best;
				changed = true;
			}
		}
		return changed;
	}

	private void recomputeCentroids(
		final List<GeoPoint> points,
		final int[] assignment,
		final List<GeoPoint> centroids,
		final int k
	) {
		double[] sumLat = new double[k];
		double[] sumLng = new double[k];
		int[] count = new int[k];
		for (int i = 0; i < points.size(); i++) {
			int c = assignment[i];
			sumLat[c] += points.get(i).latitude();
			sumLng[c] += points.get(i).longitude();
			count[c]++;
		}
		for (int c = 0; c < k; c++) {
			if (count[c] > 0) {
				centroids.set(c, new GeoPoint(sumLat[c] / count[c], sumLng[c] / count[c]));
			}
		}
	}
}
