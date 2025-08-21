package com.MohammadNoorAbuAsbe.Infodemy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.MohammadNoorAbuAsbe.Infodemy.data.TokenManager
import com.MohammadNoorAbuAsbe.Infodemy.data.repository.StudentCardRepository

class StudentCardViewModelFactory(
    private val repository: StudentCardRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentCardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentCardViewModel(repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
