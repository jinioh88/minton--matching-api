package org.app.mintonmatchapi.chat.repository;

import org.app.mintonmatchapi.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomIdAndIdGreaterThanOrderByIdAsc(Long roomId, Long id);

    Page<ChatMessage> findByRoomIdOrderByIdAsc(Long roomId, Pageable pageable);

    @Query("""
            SELECT m FROM ChatMessage m JOIN FETCH m.sender
            WHERE m.room.id = :roomId AND m.deletedAt IS NULL ORDER BY m.id DESC""")
    Page<ChatMessage> findVisibleByRoomIdOrderByIdDesc(@Param("roomId") Long roomId, Pageable pageable);

    @Query("""
            SELECT m FROM ChatMessage m JOIN FETCH m.sender
            WHERE m.room.id = :roomId AND m.deletedAt IS NULL AND m.id < :cursor ORDER BY m.id DESC""")
    Page<ChatMessage> findVisibleByRoomIdAndIdLessThan(
            @Param("roomId") Long roomId, @Param("cursor") Long cursor, Pageable pageable);

    Optional<ChatMessage> findByIdAndRoom_Id(Long messageId, Long roomId);

    @Query("""
            SELECT m FROM ChatMessage m JOIN FETCH m.sender
            WHERE m.room.id = :roomId AND m.deletedAt IS NULL AND m.id > :afterId ORDER BY m.id ASC""")
    List<ChatMessage> findVisibleByRoomIdAndIdGreaterThan(
            @Param("roomId") Long roomId, @Param("afterId") Long afterId, Pageable limit);

    @Query("""
            SELECT m FROM ChatMessage m JOIN FETCH m.sender
            WHERE m.deletedAt IS NULL AND m.id IN (
                SELECT MAX(m2.id) FROM ChatMessage m2
                WHERE m2.deletedAt IS NULL AND m2.room.id IN :roomIds GROUP BY m2.room.id)
            """)
    List<ChatMessage> findLatestVisibleByRoomIdsWithSender(@Param("roomIds") List<Long> roomIds);
}
