package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreChatRepository
import com.example.model.StatusStory
import com.example.model.StatusStoryItem
import com.example.util.AccountEmailService
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel dédié aux notifications, stories et emails de bienvenue.
 *
 * Extrait de ChatViewModel (P1-3d) pour séparer les responsabilités :
 * ChatViewModel gère les conversations/messages, NotificationViewModel gère
 * les notifications utilisateur et le cycle d'onboarding.
 */
class NotificationViewModel(
    application: Application,
    private val firestoreRepository: FirestoreChatRepository
) : AndroidViewModel(application) {

    // ------------------------------------------------------------------
    //  Bannière de création de compte
    // ------------------------------------------------------------------

    private val _accountCreationBanner = MutableStateFlow<String?>(null)
    val accountCreationBanner: StateFlow<String?> = _accountCreationBanner.asStateFlow()

    fun dismissAccountCreationBanner() {
        _accountCreationBanner.value = null
    }

    // ------------------------------------------------------------------
    //  Stories (statuts)
    // ------------------------------------------------------------------

    private val _stories = MutableStateFlow<List<StatusStoryItem>>(emptyList())
    val stories: StateFlow<List<StatusStoryItem>> = _stories.asStateFlow()

    fun observeStories() {
        viewModelScope.launch {
            firestoreRepository.observeStories().collect { storyItems ->
                _stories.value = storyItems
            }
        }
    }

    fun postNewStatus(caption: String, emoji: String) {
        viewModelScope.launch {
            firestoreRepository.postStory(caption, emoji)
        }
    }

    // ------------------------------------------------------------------
    //  Email de confirmation de compte
    // ------------------------------------------------------------------

    /**
     * Envoie l'email de confirmation de création de compte via Firestore
     * (extension Trigger Email). Ne s'envoie que si un email est disponible.
     */
    fun sendAccountCreationEmail(
        email: String,
        displayName: String,
        phoneNumber: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (email.isBlank()) {
            onComplete?.invoke(false)
            return
        }
        viewModelScope.launch {
            val success = AccountEmailService.dispatchAccountCreationEmail(
                email = email,
                displayName = displayName,
                phoneNumber = phoneNumber,
                firestore = firestoreRepository.getFirestore()
            )
            if (success) {
                NotificationHelper.showAccountCreatedNotification(
                    applicationContext = application.applicationContext,
                    googleEmail = email
                )
                _accountCreationBanner.value = "Un message de confirmation a été envoyé dans la boîte mail de votre compte Google ($email) !"
            }
            onComplete?.invoke(success)
        }
    }
}
