package com.urlshortener.repository;

import com.urlshortener.domain.ShortUrl;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID>, JpaSpecificationExecutor<ShortUrl> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);
}
