package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreChatRepository
import com.example.model.CustomStickerItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel dédié à la gestion des stickers personnalisés et présets.
 *
 * Extrait de ChatViewModel (P1-3c) pour séparer les responsabilités :
 * ChatViewModel gère les conversations/messages, StickerViewModel gère les stickers.
 */
class StickerViewModel(
    application: Application,
    private val firestoreRepository: FirestoreChatRepository
) : AndroidViewModel(application) {

    // Stickers personnalisés (état local, synchronisé avec Firestore)
    private val _customStickers = MutableStateFlow<List<CustomStickerItem>>(emptyList())
    val customStickers: StateFlow<List<CustomStickerItem>> = _customStickers.asStateFlow()

    /**
     * Initialise les stickers depuis Firestore et les combine avec les présets locaux.
     * À appeler depuis ChatViewModel au démarrage.
     */
    fun initStickers() {
        viewModelScope.launch {
            firestoreRepository.observeCustomStickers().collect { firestoreStickers ->
                val presets = listOf(
                    CustomStickerItem(
                        id = "stk_preset_1",
                        title = "KRIS FIRE",
                        author = "Kris",
                        imageUri = "preset://🔥",
                        emojiBadge = "🔥",
                        borderColorHex = "#FF5500",
                        isVideo = false
                    ),
                    CustomStickerItem(
                        id = "stk_preset_2",
                        title = "HER LOVE",
                        author = "Her",
                        imageUri = "preset://❤️",
                        emojiBadge = "❤️",
                        borderColorHex = "#FF1493",
                        isVideo = false
                    ),
                    CustomStickerItem(
                        id = "stk_preset_3",
                        title = "NEW SCHOOL",
                        author = "kris&her",
                        imageUri = "preset://✨",
                        emojiBadge = "✨",
                        borderColorHex = "#00F0FF",
                        isVideo = false
                    )
                )
                val existingIds = firestoreStickers.map { it.id }.toSet()
                val newPresets = presets.filter { it.id !in existingIds }
                _customStickers.value = firestoreStickers + newPresets
            }
        }
    }

    /** Ajoute un sticker personnalisé (localement et dans Firestore). */
    fun saveCustomSticker(item: CustomStickerItem) {
        _customStickers.value = listOf(item) + _customStickers.value
        viewModelScope.launch {
            firestoreRepository.createCustomSticker(
                title = item.title,
                imageUri = item.imageUri,
                emojiBadge = item.emojiBadge,
                borderColorHex = item.borderColorHex,
                isVideo = item.isVideo,
                frameTimestampMs = item.frameTimestampMs
            )
        }
    }

    /** Supprime un sticker personnalisé (localement et dans Firestore). */
    fun deleteCustomSticker(id: String) {
        _customStickers.value = _customStickers.value.filter { it.id != id }
        viewModelScope.launch {
            firestoreRepository.deleteCustomSticker(id)
        }
    }
}
