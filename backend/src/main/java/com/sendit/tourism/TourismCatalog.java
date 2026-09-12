package com.sendit.tourism;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TourismCatalog {
    private final JdbcTemplate jdbc;
    public TourismCatalog(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Marker(String id, String name, String category, double latitude, double longitude, long count) {}
    public record Snapshot(List<Marker> points, long total, String updatedAt, boolean stale, boolean failed) {}

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Snapshot viewport(String mode, double west, double south, double east, double north, int level) {
        LocalDate month = LocalDate.now(ZoneId.of("Asia/Seoul")).withDayOfMonth(1);
        var status = jdbc.queryForMap("SELECT last_success, last_error, period, last_success < now() - interval '48 hours' AS stale FROM tourism_catalog_sync WHERE mode=?", mode);
        // Coarser cells bound the response even when a client requests a large area at a close zoom.
        double cell = Math.max(level >= 6 ? 0.003 * Math.pow(2, level - 3) : 0,
                Math.max(east - west, north - south) / 18);
        String filter = " FROM tourism_catalog WHERE mode=? AND geom && ST_MakeEnvelope(?,?,?, ?,4326)"
                + " AND (mode <> 'festival' OR (event_start_date <= ? AND COALESCE(event_end_date,event_start_date) >= ?))";
        Object[] args = {mode, west, south, east, north, month.plusMonths(1).minusDays(1), month};
        Long total = jdbc.queryForObject("SELECT count(*)" + filter, Long.class, args);
        List<Marker> points;
        if (level < 6 && total != null && total <= 400) {
            points = jdbc.query("SELECT content_id, name, category, latitude, longitude" + filter + " ORDER BY content_id",
                    (rs, row) -> new Marker(rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getDouble(5), 1), args);
        } else {
            String sql = "SELECT min(content_id), CASE WHEN count(*)=1 THEN min(name) ELSE '' END, min(category),"
                    + " avg(latitude), avg(longitude), count(*)" + filter
                    + " GROUP BY floor(longitude / " + cell + "), floor(latitude / " + cell + ") ORDER BY min(content_id)";
            points = jdbc.query(sql, (rs, row) -> new Marker(rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getDouble(4), rs.getDouble(5), rs.getLong(6)), args);
        }
        boolean stale = Boolean.TRUE.equals(status.get("stale")) || status.get("period") == null
                || (mode.equals("festival") && !month.toString().equals(status.get("period").toString()));
        return new Snapshot(points, total == null ? 0 : total,
                status.get("last_success") == null ? null : status.get("last_success").toString(), stale,
                Boolean.TRUE.equals(status.get("last_error")));
    }

    public TourApiClient.MapPlace detail(String mode, String id) {
        return jdbc.query("SELECT * FROM tourism_catalog WHERE mode=? AND content_id=?", (rs, row) ->
                new TourApiClient.MapPlace(rs.getString("content_id"), rs.getString("content_type_id"), rs.getString("name"),
                        rs.getString("category"), rs.getString("address"), rs.getDouble("latitude"), rs.getDouble("longitude"),
                        rs.getString("image_url"), rs.getObject("event_start_date", LocalDate.class), rs.getObject("event_end_date", LocalDate.class)),
                mode, id).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void replace(String mode, LocalDate period, List<TourApiClient.MapPlace> places) {
        // Only the public catalog snapshot is replaced; user-saved places are separate and untouched.
        jdbc.update("DELETE FROM tourism_catalog WHERE mode=?", mode);
        jdbc.batchUpdate("""
                INSERT INTO tourism_catalog(mode,content_id,content_type_id,name,category,address,latitude,longitude,
                image_url,event_start_date,event_end_date) VALUES (?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT (mode,content_id) DO NOTHING
                """, places, 200, (ps, p) -> {
            ps.setString(1, mode); ps.setString(2, p.contentId()); ps.setString(3, p.contentTypeId());
            ps.setString(4, p.name()); ps.setString(5, p.category()); ps.setString(6, p.address());
            ps.setDouble(7, p.latitude()); ps.setDouble(8, p.longitude()); ps.setString(9, p.imageUrl());
            ps.setObject(10, p.eventStartDate()); ps.setObject(11, p.eventEndDate());
        });
        jdbc.update("UPDATE tourism_catalog_sync SET last_success=now(), period=?, last_error=false WHERE mode=?", period, mode);
    }
}
