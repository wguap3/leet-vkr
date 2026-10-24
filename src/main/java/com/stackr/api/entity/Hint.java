package com.stackr.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private int orderIndex = 0;

    /** Hint becomes visible after this many wrong submissions on the problem. */
    @Column(name = "unlocks_after_attempts", nullable = false)
    @Builder.Default
    private int unlocksAfterAttempts = 0;
}
