package com.sendit.tourism;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TourismCatalogSync {
    private final JdbcTemplate jdbc;
    private final TourApiClient api;
    private final TourismCatalog catalog;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TourismCatalogSync.class);
    public TourismCatalogSync(JdbcTemplate jdbc, TourApiClient api, TourismCatalog catalog,
            @Value("${app.tour-api.catalog-sync-enabled:true}") boolean enabled) {
        this.jdbc = jdbc; this.api = api; this.catalog = catalog; this.enabled = enabled;
    }
    @Scheduled(initialDelay = 15000, fixedDelay = 3600000)
    public void schedule() {
        // Network collection must not block the application's other scheduled jobs.
        if (!enabled || !running.compareAndSet(false, true)) return;
        Thread.ofVirtual().name("tourism-catalog-sync").start(() -> {
            try { for (String mode : List.of("nearby", "festival")) sync(mode); }
            finally { running.set(false); }
        });
    }
    void sync(String mode) {
        LocalDate period = LocalDate.now(ZoneId.of("Asia/Seoul")).withDayOfMonth(1);
        // A database advisory lock prevents duplicate collection across server instances.
        try (var connection = jdbc.getDataSource().getConnection();
             var lock = connection.prepareStatement("SELECT pg_try_advisory_lock(72851, ?)");
             var unlock = connection.prepareStatement("SELECT pg_advisory_unlock(72851, ?)")) {
            int key = mode.equals("nearby") ? 1 : 2;
            lock.setInt(1, key); unlock.setInt(1, key);
            try (var result = lock.executeQuery()) { if (!result.next() || !result.getBoolean(1)) return; }
            try {
                Integer due = jdbc.queryForObject("""
                        SELECT count(*) FROM tourism_catalog_sync WHERE mode=? AND
                        (last_success IS NULL OR last_success < now()-interval '24 hours' OR period<>?)
                        """, Integer.class, mode, period);
                if (due == null || due == 0) return;
                var places = new ArrayList<TourApiClient.MapPlace>();
                boolean complete = false;
                for (int page = 1; page <= 200; page++) {
                    var response = api.discover(mode, period, page);
                    places.addAll(response.places());
                    if (!response.hasMore()) { complete = true; break; }
                }
                if (!complete || (mode.equals("nearby") && places.isEmpty()))
                    throw new IllegalStateException("Incomplete tourism snapshot");
                catalog.replace(mode, period, places);
                log.info("Tourism catalog synced: mode={}, places={}", mode, places.size());
            } catch (Exception ex) {
                jdbc.update("UPDATE tourism_catalog_sync SET last_error=true WHERE mode=?", mode);
                log.warn("Tourism catalog sync failed; retaining previous snapshot: mode={}", mode);
            } finally { unlock.execute(); }
        } catch (Exception ex) {
            log.warn("Tourism catalog sync unavailable: mode={}", mode);
        }
    }
}
