//package com.example.charityDept.data.mappers
//
///// ADDED
//import com.example.charityDept.data.model.Child
//import com.example.charityDept.data.model.Reply
//import com.example.charityDept.data.model.RegistrationStatus
//import com.example.charityDept.data.model.EducationPreference
//import com.example.charityDept.data.model.EducationLevel
//
///// ADDED — map exactly to your ChildEntity in package data.local
//fun Child.toEntity(): ChildEntity = ChildEntity(
//    childId = childId,
//    fName = fName,
//    lName = lName,
//    oName = oName,
//    gender = gender,
//    age = age,
//    street = street,
//    registrationStatus = registrationStatus,                // uses DbConverters
//    graduated = graduated,                                   // uses DbConverters
//    nameSearch = (listOf(fName, oName, lName)
//        .map { it.trim() }.filter { it.isNotEmpty() }
//        .joinToString(" ")
//        .lowercase()),
//    createdAt = createdAt,
//    updatedAt = updatedAt,
//    educationPreference = educationPreference,
//    educationLevel = educationLevel,
//    technicalSkills = technicalSkills,
//    resettled = resettled,
//    resettlementDate = resettlementDate,
//    sponsoredForEducation = sponsoredForEducation,
//    sponsorFName = sponsorFName,
//    sponsorLName = sponsorLName
//)

