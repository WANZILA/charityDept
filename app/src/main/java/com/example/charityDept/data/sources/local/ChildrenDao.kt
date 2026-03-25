//package com.example.charityDept.data.sources.local
//
//import androidx.room.*
//import com.example.charityDept.data.model.Child
//
//@Dao
//interface ChildrenDao{
//    @Query("SELECT * FROM children")
//    fun getAllChildren(): List<Child>
//
//    @Insert(onConflict = OnConflictStrategy.ABORT)
//    fun insertChild(kid: Child)
////}
