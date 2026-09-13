package com.sendit.share;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedContentPlaceService {
    private final SharedContentRepository contents;
    private final SharedContentPlaceRepository places;

    public SharedContentPlaceService(SharedContentRepository contents,
                                     SharedContentPlaceRepository places) {
        this.contents = contents;
        this.places = places;
    }

    @Transactional
    public void replace(Long sharedContentId, List<PageMetadata> candidates) {
        SharedContent content = contents.findForSaving(sharedContentId).orElseThrow();
        places.deleteBySharedContentId(sharedContentId);
        places.flush();
        int order = 1;
        for (PageMetadata candidate : candidates) {
            if (candidate.placeName() == null || candidate.placeName().isBlank()) continue;
            places.save(new SharedContentPlace(content, order++, candidate));
        }
    }

    @Transactional(readOnly = true)
    public List<SharedContentPlace> list(Long sharedContentId) {
        return places.findBySharedContentIdOrderByDisplayOrder(sharedContentId);
    }
}
