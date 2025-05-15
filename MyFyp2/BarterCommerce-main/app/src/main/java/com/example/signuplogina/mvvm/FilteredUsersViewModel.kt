package com.example.signuplogina.mvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.signuplogina.User
import com.example.signuplogina.modal.FeedbackTags
import kotlin.collections.mapNotNull

// --- ViewModel ---
class FilteredUsersViewModel : ViewModel() {

    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> get() = _allUsers
    val userRepo: UsersRepo = UsersRepo()
    private val _filteredUsers = MutableLiveData<List<User>>()
    val filteredUsers: LiveData<List<User>> get() = _filteredUsers

    private val _availableTags = MutableLiveData<Set<FeedbackTags>>()
    val availableTags: LiveData<Set<FeedbackTags>> get() = _availableTags

    fun loadUsers() {
        userRepo.getAllUsersFromFirebase { users ->
            _allUsers.value = users
            _availableTags.value = extractTags(users)
            _filteredUsers.value = users
        }
    }

    fun applyFilters(selectedTags: Set<FeedbackTags>) {
        val users = _allUsers.value ?: return

        if (selectedTags.isEmpty()) {
            _filteredUsers.value = users
        } else {
            _filteredUsers.value = users.filter { user ->
                val userTags = user.ratings?.feedbackList?.values
                    ?.flatMap { it.tags }
                    ?.toSet() ?: emptySet()

                userTags.containsAll(selectedTags)
//                userTags.intersect(selectedTags).isNotEmpty()

            }
        }
    }

//    fun applyFilters(selectedTags: Set<FeedbackTags>) {
//        val users = _allUsers.value ?: return
//        if (selectedTags.isEmpty()) {
//            _filteredUsers.value = users
//        } else {
//            _filteredUsers.value = users.filter { user ->
//                selectedTags.any { tag -> user.status?.contains(tag, ignoreCase = true) == true }
//            }
//        }
//    }

    private fun extractTags(users: List<User>?): Set<FeedbackTags> {
        val tagSet = mutableSetOf<FeedbackTags>()
        users?.forEach { user ->
            user.ratings?.feedbackList?.forEach { bidId,feedback ->
                tagSet.addAll(feedback.tags)
            }
        }
        return tagSet
    }
}