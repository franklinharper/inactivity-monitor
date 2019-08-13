package com.franklinharper.inactivitymonitor

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MyFirebaseDb(
  val db:FirebaseFirestore = FirebaseFirestore.getInstance()
) {

  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  fun writeDailyActivities(dateTime: ZonedDateTime, activities: Collection<UserActivity>) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
      throw IllegalStateException("The Firebase user is null")
    }
    Timber.d("writeDailyActivities, activities: $activities")

    val yyyymmdd = dateTime.format(dateFormatter)

    val data = hashMapOf( "dailyActivities" to activities )
    Timber.d("user.uid:${user.uid}, data: $data")
    db.collection("users")
      .document(user.uid)
      .collection("dailyActivities")
      .document(yyyymmdd)
      .set(data)
      .addOnSuccessListener { Timber.d("DocumentSnapshot successfully written!") }
      .addOnFailureListener { e -> Timber.e(e, "Error adding document") }
  }

}