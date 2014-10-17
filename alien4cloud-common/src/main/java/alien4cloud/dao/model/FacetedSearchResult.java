package alien4cloud.dao.model;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains results for a search query.
 * 
 */

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class FacetedSearchResult extends GetMultipleDataResult {
    private static final long serialVersionUID = 1L;

    private Map<String, FacetedSearchFacet[]> facets;

    /**
     * Argument constructor.
     * 
     * @param from The start index of the returned elements.
     * @param to The end index of the returned elements.
     * @param queryDuration The duration of the query.
     * @param totalResults The total results for this query.
     * @param types The types of data found.
     * @param data The found data.
     * @param facets The facets if any for the query.
     */
    public FacetedSearchResult(final int from, final int to, final long queryDuration, final long totalResults,
            final String[] types, final Object[] data, final Map<String, FacetedSearchFacet[]> facets) {
        super(types, data, queryDuration, totalResults, from, to);
        this.facets = facets;
    }
}