package com.jotape.data.local.mapper

import com.jotape.data.local.model.InteractionEntity
import com.jotape.domain.model.Interaction
import com.jotape.domain.model.MessageStatus
import com.jotape.domain.model.Sender
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Converte um InteractionEntity (modelo de dados local) para um Interaction (modelo de domínio).
 */
fun InteractionEntity.toDomainModel(): Interaction {
    val isFromUser = try {
        Sender.valueOf(this.sender) == Sender.USER
    } catch (e: IllegalArgumentException) {
        false // Considerar como não sendo do usuário se o valor for inválido
    }
    val timestampInstant = try {
        Instant.ofEpochMilli(this.timestamp)
    } catch (e: Exception) {
        Instant.now() // Usar tempo atual se o timestamp for inválido
    }

    return Interaction(
        id = this.id,
        isFromUser = isFromUser,
        text = this.text,
        timestamp = timestampInstant
    )
}

/**
 * Converte uma lista de InteractionEntity para uma lista de Interaction.
 */
fun List<InteractionEntity>.toDomainModelList(): List<Interaction> {
    return this.map { it.toDomainModel() }
}

/**
 * Converte um Interaction (modelo de domínio) para um InteractionEntity (modelo de dados local).
 * Útil para salvar interações (especialmente a do usuário) no banco local.
 */
fun Interaction.toEntity(userId: String, status: String = "PENDING"): InteractionEntity {
    // O ID pode já existir se for uma atualização, mas se for novo, pode gerar um.
    // A lógica exata de ID pode depender do fluxo (ex: gerar no repo antes de salvar).
    // Aqui assumimos que o ID do Interaction é o que deve ser usado.
    val senderString = if (this.isFromUser) Sender.USER.name else Sender.ASSISTANT.name

    return InteractionEntity(
        id = this.id, // Usar o ID do modelo de domínio
        text = this.text,
        sender = senderString,
        timestamp = this.timestamp.toEpochMilli(),
        status = status, // Status inicial geralmente é PENDING
        userId = userId // Precisa do ID do usuário para associar a interação
    )
} 