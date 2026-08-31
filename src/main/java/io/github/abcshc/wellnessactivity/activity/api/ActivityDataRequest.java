package io.github.abcshc.wellnessactivity.activity.api;

import java.util.List;

public record ActivityDataRequest(
	List<ActivityEntryRequest> entries,
	ActivitySourceRequest source
) {
}
