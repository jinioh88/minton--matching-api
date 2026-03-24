package org.app.mintonmatchapi.chat.repository;

import org.app.mintonmatchapi.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {

    Optional<ChatRoom> findByMatchId(Long matchId);

    boolean existsByMatchId(Long matchId);

    @Query("SELECT cr FROM ChatRoom cr JOIN FETCH cr.match m JOIN FETCH m.host WHERE cr.id = :roomId")
    Optional<ChatRoom> findByIdWithMatchAndHost(@Param("roomId") Long roomId);
}
