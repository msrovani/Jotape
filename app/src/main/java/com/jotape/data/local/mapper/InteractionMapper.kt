package com.jotape.data.local.mapper

import com.jotape.data.local.model.InteractionEntity
import com.jotape.domain.model.Interaction

/**
 * Maps InteractionEntity (data layer) to Interaction (domain layer).
 */
fun InteractionEntity.toDomainModel(): Interaction {
    return Interaction(
        id = this.id,
        isFromUser = this.isFromUser,
        text = this.text,
        timestamp = this.timestamp,
        isSynced = this.isSynced
    )
}

/**
 * Maps a list of InteractionEntity to a list of Interaction.
 */
fun List<InteractionEntity>.toDomainModelList(): List<Interaction> {
    return this.map { it.toDomainModel() }
}

/**
 * Maps Interaction (domain layer) to InteractionEntity (data layer).
 * Note: ID might be ignored if Room uses autoGenerate.
 */
fun Interaction.toEntity(): InteractionEntity {
    return InteractionEntity(
        id = this.id,
        isFromUser = this.isFromUser,
        text = this.text,
        timestamp = this.timestamp,
        isSynced = this.isSynced
    )
} 