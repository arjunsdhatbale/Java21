package com.main.shared.pagination.helper;

import com.main.shared.pagination.codec.CursorCodec;
import com.main.shared.pagination.context.CursorContext;
import com.main.shared.pagination.model.CursorPageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// CursorQueryHelper.java  — use this in any repository
@Component
@RequiredArgsConstructor
public class CursorQueryHelper {

    private final EntityManager em;
    private final CursorCodec codec;

    /**
     * Generic cursor query. Call from any repository.
     * Reads CursorContext automatically — no args needed.
     *
     * @param entityClass  the JPA entity
     * @param predicatesFn optional extra WHERE predicates (business filters)
     */
    public <T> List<T> findWithCursor(
            Class<T> entityClass,
            Function<CriteriaBuilder, List<Predicate>> predicatesFn
    ) throws Throwable {
        CursorPageRequest req  = CursorContext.get();
        CriteriaBuilder   cb   = em.getCriteriaBuilder();
        CriteriaQuery<T> cq   = cb.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);

        List<Predicate> predicates = new ArrayList<>();

        // Apply cursor WHERE clause
        if (!req.isFirstPage()) {
            CursorCodec.CursorPayload payload = codec.decode(req.cursor());
            applyAfterCursor(cb, root, req, payload, predicates);
        }

        // Apply caller-supplied business predicates
        if (predicatesFn != null) {
            predicates.addAll(predicatesFn.apply((CriteriaBuilder) root));
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        // Order by sortField, then id (tie-breaker for stable pagination)
        cq.orderBy((Order) buildOrder(cb, root, req));

        return em.createQuery(cq)
                .setMaxResults(req.size())
                .getResultList();
    }

    private <T> void applyAfterCursor(
            CriteriaBuilder cb, Root<T> root,
            CursorPageRequest req, CursorCodec.CursorPayload payload,
            List<Predicate> predicates
    ) {
        // Cursor WHERE: (sortField > cursorValue) OR (sortField = cursorValue AND id > cursorId)
        Path<Comparable> sortPath = root.get(req.sortField());
        Path<Comparable> idPath   = root.get("id");

        boolean asc = "ASC".equalsIgnoreCase(req.sortDir());

        @SuppressWarnings("unchecked")
        Predicate afterSortField = asc
                ? cb.greaterThan(sortPath, (Comparable) payload.sortFieldValue())
                : cb.lessThan(sortPath,    (Comparable) payload.sortFieldValue());

        @SuppressWarnings("unchecked")
        Predicate sameFieldAfterId = cb.and(
                cb.equal(sortPath, (Comparable) payload.sortFieldValue()),
                asc ? cb.greaterThan(idPath, (Comparable) payload.id())
                        : cb.lessThan(idPath,    (Comparable) payload.id())
        );

        predicates.add(cb.or(afterSortField, sameFieldAfterId));
    }

    private <T> List<Order> buildOrder(
            CriteriaBuilder cb, Root<T> root, CursorPageRequest req
    ) {
        boolean asc = "ASC".equalsIgnoreCase(req.sortDir());
        return List.of(
                asc ? cb.asc(root.get(req.sortField()))  : cb.desc(root.get(req.sortField())),
                asc ? cb.asc(root.get("id"))             : cb.desc(root.get("id"))   // tie-breaker
        );
    }
}