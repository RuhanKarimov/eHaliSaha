package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.MatchVideo;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Reservation;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.ReservationStatus;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.VideoStatus;
import com.ornek.ehalisaha.ehalisahabackend.repository.MatchVideoRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class MatchCompletionScheduler {

    private final ReservationRepository reservationRepo;
    private final MatchVideoRepository videoRepo;

    public MatchCompletionScheduler(ReservationRepository reservationRepo, MatchVideoRepository videoRepo) {
        this.reservationRepo = reservationRepo;
        this.videoRepo = videoRepo;
    }

    @Scheduled(fixedDelay = 60_000) // 1 dk
    @Transactional
    public void completeAndPublish() {
        Instant now = Instant.now();

        // ✅ sadece gerekli kayıtları çek
        List<Reservation> targets = reservationRepo.findByStatusAndEndTimeBefore(ReservationStatus.CONFIRMED, now);

        for (Reservation r : targets) {
            r.setStatus(ReservationStatus.COMPLETED);
            reservationRepo.save(r);

            videoRepo.findByReservationId(r.getId()).orElseGet(() -> {
                MatchVideo v = new MatchVideo();
                v.setReservationId(r.getId());
                v.setStatus(VideoStatus.PUBLISHED);
                v.setStorageUrl("https://cdn.ehalisaha.local/videos/" + r.getId() + ".mp4");
                return videoRepo.save(v);
            });
        }
    }
}
