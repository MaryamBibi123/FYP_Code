package com.example.signuplogina.mvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.signuplogina.Item
import com.example.signuplogina.User
import com.example.signuplogina.mvvm.UsersRepo
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UserViewModel : ViewModel() {

    private val usersRepo = UsersRepo()

    private val _userItems = MutableLiveData<List<Item>>()
    val userItems: LiveData<List<Item>> get() = _userItems

    // Fetch user items via repository and update LiveData
    fun fetchUserItems(userId: String) {
        usersRepo.getUserItems(userId) { items ->
            _userItems.postValue(items)
        }
    }


    private val userList = MutableLiveData<List<User>>()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users")
    fun getUserList(): LiveData<List<User>> = userList


    fun fetchUserById(userId: String, callback: (User?) -> Unit) {

        database.child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            callback(user)
        }.addOnFailureListener { exception ->
            callback(null)
        }
    }


    fun fetchUsersFromFirebase() {
        database.get().addOnSuccessListener { snapshot ->
            val users = mutableListOf<User>()
            for (userSnapshot in snapshot.children) {
                val user = userSnapshot.getValue(User::class.java)
                user?.let { users.add(it) }
            }
            userList.value = users
        }.addOnFailureListener { exception ->
            // Optional: Log the error or show message
            userList.value = emptyList() // Or keep previous state
        }

    }
}
