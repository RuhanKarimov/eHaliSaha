package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.AuditLog;
import com.ornek.ehalisaha.ehalisahabackend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void log(Long actorId, String action, String entityType, Long entityId, String detail) {
        AuditLog a = new AuditLog();
        a.setActorUserId(actorId);
        a.setAction(trim(action, 60));
        a.setEntityType(trim(entityType, 60));
        a.setEntityId(entityId);
        a.setDetail(trim(detail, 2000));
        repo.save(a);
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        s = s.trim();
        return s.length() <= max ? s : s.substring(0, max);
    }
}
