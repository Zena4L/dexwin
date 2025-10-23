package com.clement.dexwin.domain.services.implementations;

import com.clement.dexwin.domain.models.notes.Note;
import com.clement.dexwin.domain.models.users.User;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class NoteSpecification {

    private NoteSpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Note> filterNotes(User user, String search, List<String> filterTags) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (user != null) {
                predicates.add(criteriaBuilder.equal(root.get("user"), user));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";

                Predicate titlePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        searchPattern
                );

                Expression<String> contentAsText = criteriaBuilder.function(
                        "text",
                        String.class,
                        root.get("content")
                );

                Predicate contentPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(contentAsText),
                        searchPattern
                );

                predicates.add(criteriaBuilder.or(titlePredicate, contentPredicate));
            }

            if (filterTags != null && !filterTags.isEmpty()) {
                List<Predicate> tagPredicates = new ArrayList<>();

                for (String tag : filterTags) {
                    tagPredicates.add(
                            criteriaBuilder.isTrue(
                                    criteriaBuilder.function(
                                            "jsonb_exists",
                                            Boolean.class,
                                            root.get("tags"),
                                            criteriaBuilder.literal(tag)
                                    )
                            )
                    );
                }

                predicates.add(criteriaBuilder.or(tagPredicates.toArray(new Predicate[0])));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}