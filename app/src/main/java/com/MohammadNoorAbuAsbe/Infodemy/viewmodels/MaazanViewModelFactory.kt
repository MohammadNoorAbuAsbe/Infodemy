package com.MohammadNoorAbuAsbe.Infodemy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.MaazanRepository

class MaazanViewModelFactory(
    private val repository: MaazanRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MaazanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MaazanViewModel(repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}