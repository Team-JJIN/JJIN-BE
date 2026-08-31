package com.JJIN.domain.mission.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.JJIN.domain.mission.dto.internal.MissionSearchCondition;
import com.JJIN.domain.mission.dto.internal.MissionSearchResult;
import com.JJIN.domain.mission.entity.enums.MissionSortOption;
import com.JJIN.domain.mission.entity.enums.MissionStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MissionSearchRepositoryImpl implements MissionSearchRepository {

	private final EntityManager entityManager;

	@Override
	public Page<MissionSearchResult> search(
		final MissionSearchCondition condition,
		final Pageable pageable
	) {
		long totalCount = count(condition);
		if (totalCount == 0) {
			return new PageImpl<>(List.of(), pageable, totalCount);
		}

		List<MissionSearchResult> results = fetchResults(condition, pageable);
		return new PageImpl<>(results, pageable, totalCount);
	}

	private List<MissionSearchResult> fetchResults(
		final MissionSearchCondition condition,
		final Pageable pageable
	) {
		StringBuilder jpql = new StringBuilder("""
			select m.id, count(distinct um.id)
			from Mission m
		left join UserMission um on um.mission = m
		where m.status = :status
		""");
		appendFilters(jpql, condition);
		jpql.append(" group by m.id, m.createdAt ");
		appendOrder(jpql, condition.sortOption());

		Query query = entityManager.createQuery(jpql.toString());
		bindParameters(query, condition);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		@SuppressWarnings("unchecked")
		List<Object[]> rows = query.getResultList();
		return rows.stream()
			.map(row -> new MissionSearchResult(
				(Long)row[0],
				((Number)row[1]).longValue()
			))
			.toList();
	}

	private long count(final MissionSearchCondition condition) {
		StringBuilder jpql = new StringBuilder("""
			select count(m.id)
			from Mission m
			where m.status = :status
			""");
		appendFilters(jpql, condition);

		TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);
		bindParameters(query, condition);
		return query.getSingleResult();
	}

	private void appendFilters(
		final StringBuilder jpql,
		final MissionSearchCondition condition
	) {
		if (condition.keyword() != null) {
			jpql.append("""
				 and (
				 lower(m.title) like :keyword
				 or exists (
				 	select 1
				 	from MissionTagMapping mtm
				 	join mtm.tag t
				 	where mtm.mission = m
				 	and lower(t.name) like :keyword
				 )
				 )
				""");
		}
		if (!condition.categories().isEmpty()) {
			jpql.append(" and m.category in :categories ");
		}
		if (!condition.difficulties().isEmpty()) {
			jpql.append(" and m.difficulty in :difficulties ");
		}
		if (condition.sourceType() != null) {
			jpql.append(" and m.sourceType = :sourceType ");
		}
	}

	private void appendOrder(
		final StringBuilder jpql,
		final MissionSortOption sortOption
	) {
		if (sortOption == MissionSortOption.LATEST) {
			jpql.append(" order by m.createdAt desc, m.id desc ");
			return;
		}
		jpql.append("""
			 order by
			 count(distinct um.id) desc,
			 m.createdAt desc,
			 m.id desc
			""");
	}

	private void bindParameters(
		final Query query,
		final MissionSearchCondition condition
	) {
		query.setParameter("status", MissionStatus.ACTIVE);
		if (condition.keyword() != null) {
			query.setParameter("keyword", "%" + condition.keyword() + "%");
		}
		if (!condition.categories().isEmpty()) {
			query.setParameter("categories", condition.categories());
		}
		if (!condition.difficulties().isEmpty()) {
			query.setParameter("difficulties", condition.difficulties());
		}
		if (condition.sourceType() != null) {
			query.setParameter("sourceType", condition.sourceType());
		}
	}
}
